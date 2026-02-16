package main.java.com.imagecloud.main.service;

import com.imagecloud.main.dto.*;
import com.imagecloud.main.model.ConversionStatus;
import com.imagecloud.main.model.Image;
import com.imagecloud.main.repository.ImageRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    private final ImageRepository imageRepository;
    private final RabbitTemplate rabbitTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${rabbitmq.exchange.image}")
    private String imageExchange;

    @Value("${rabbitmq.routing-key.conversion-request}")
    private String conversionRequestRoutingKey;

    public ImageUploadResponse uploadAndConvert(MultipartFile file, String targetFormat, String userId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // Extract original format from filename
            String originalFileName = file.getOriginalFilename();
            String originalFormat = getFileExtension(originalFileName);

            // Save image to database
            Image image = new Image();
            image.setUserId(userId);
            image.setOriginalFileName(originalFileName);
            image.setOriginalFormat(originalFormat);
            image.setTargetFormat(targetFormat);
            image.setStatus(ConversionStatus.PENDING);
            image.setOriginalImage(file.getBytes());

            // Measure database save operation
            Timer.Sample dbSample = Timer.start(meterRegistry);
            Image savedImage = imageRepository.save(image);
            dbSample.stop(Timer.builder("imagecloud.database.query.duration")
                    .tag("operation", "save")
                    .tag("entity", "image")
                    .description("Database query execution time")
                    .register(meterRegistry));
            
            log.info("Image saved with ID: {}", savedImage.getId());

            // Send to RabbitMQ for conversion
            ConversionRequest request = new ConversionRequest(
                    savedImage.getId(),
                    savedImage.getOriginalImage(),
                    originalFormat,
                    targetFormat
            );

            // Measure queue send time
            Timer.Sample queueSample = Timer.start(meterRegistry);
            rabbitTemplate.convertAndSend(imageExchange, conversionRequestRoutingKey, request);
            queueSample.stop(Timer.builder("imagecloud.queue.send.duration")
                    .tag("queue", "conversion-request")
                    .description("Time to send message to RabbitMQ")
                    .register(meterRegistry));
            
            log.info("Conversion request sent for image ID: {}", savedImage.getId());

            // Update status to PROCESSING
            savedImage.setStatus(ConversionStatus.PROCESSING);
            imageRepository.save(savedImage);
            
            // Track conversion request
            Counter.builder("imagecloud.conversion.requests.total")
                    .tag("status", "initiated")
                    .tag("target_format", targetFormat)
                    .description("Total conversion requests initiated")
                    .register(meterRegistry)
                    .increment();

            return new ImageUploadResponse(
                    savedImage.getId(),
                    "PROCESSING",
                    "Image uploaded successfully and conversion started"
            );

        } catch (IOException e) {
            log.error("Error uploading image", e);
            // Track failed uploads
            Counter.builder("imagecloud.conversion.requests.total")
                    .tag("status", "upload_failed")
                    .tag("target_format", targetFormat != null ? targetFormat : "unknown")
                    .description("Total conversion requests initiated")
                    .register(meterRegistry)
                    .increment();
            throw new RuntimeException("Failed to upload image: " + e.getMessage());
        }
    }

    public void handleConversionResponse(ConversionResponse response) {
        log.info("Received conversion response for image ID: {}", response.getImageId());

        Timer.Sample dbSample = Timer.start(meterRegistry);
        Image image = imageRepository.findById(response.getImageId())
                .orElseThrow(() -> new RuntimeException("Image not found with ID: " + response.getImageId()));
        dbSample.stop(Timer.builder("imagecloud.database.query.duration")
                .tag("operation", "find")
                .tag("entity", "image")
                .description("Database query execution time")
                .register(meterRegistry));

        if (response.isSuccess()) {
            image.setConvertedImage(response.getConvertedImageData());
            image.setStatus(ConversionStatus.COMPLETED);
            image.setConvertedAt(LocalDateTime.now());
            log.info("Image conversion completed for ID: {}", response.getImageId());
            
            // Track successful conversion
            Counter.builder("imagecloud.conversion.requests.total")
                    .tag("status", "success")
                    .tag("target_format", image.getTargetFormat())
                    .description("Total conversion requests initiated")
                    .register(meterRegistry)
                    .increment();
        } else {
            image.setStatus(ConversionStatus.FAILED);
            image.setErrorMessage(response.getErrorMessage());
            log.error("Image conversion failed for ID: {}. Error: {}", response.getImageId(), response.getErrorMessage());
            
            // Track failed conversion
            Counter.builder("imagecloud.conversion.requests.total")
                    .tag("status", "failed")
                    .tag("target_format", image.getTargetFormat())
                    .description("Total conversion requests initiated")
                    .register(meterRegistry)
                    .increment();
        }

        imageRepository.save(image);
    }

    public List<ImageHistoryResponse> getUserHistory(String userId) {
        Timer.Sample dbSample = Timer.start(meterRegistry);
        List<Image> images = imageRepository.findByUserIdOrderByUploadedAtDesc(userId);
        dbSample.stop(Timer.builder("imagecloud.database.query.duration")
                .tag("operation", "findByUser")
                .tag("entity", "image")
                .description("Database query execution time")
                .register(meterRegistry));

        return images.stream()
                .map(image -> new ImageHistoryResponse(
                        image.getId(),
                        image.getOriginalFileName(),
                        image.getOriginalFormat(),
                        image.getTargetFormat(),
                        image.getStatus(),
                        image.getUploadedAt(),
                        image.getConvertedAt(),
                        image.getErrorMessage()
                ))
                .collect(Collectors.toList());
    }

    public byte[] getConvertedImage(Long imageId, String userId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        if (!image.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        if (image.getStatus() != ConversionStatus.COMPLETED) {
            throw new RuntimeException("Image conversion not completed yet");
        }

        return image.getConvertedImage();
    }

    public Image getImageStatus(Long imageId, String userId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        if (!image.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        return image;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
