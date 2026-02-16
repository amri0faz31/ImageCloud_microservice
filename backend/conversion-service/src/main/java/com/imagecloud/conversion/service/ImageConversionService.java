package main.java.com.imagecloud.conversion.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageConversionService {

    private final MeterRegistry meterRegistry;

    public byte[] convertImage(byte[] imageData, String sourceFormat, String targetFormat) throws IOException {
        log.info("Converting image from {} to {}", sourceFormat, targetFormat);
        
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // Read the input image
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
            BufferedImage bufferedImage = ImageIO.read(inputStream);

            if (bufferedImage == null) {
                throw new IOException("Failed to read image data");
            }

            // Convert using Thumbnailator (maintains quality, handles transparency)
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            Thumbnails.of(bufferedImage)
                    .scale(1.0) // Keep original size
                    .outputFormat(targetFormat)
                    .toOutputStream(outputStream);

            byte[] result = outputStream.toByteArray();
            log.info("Image conversion successful. Output size: {} bytes", result.length);
            
            // Record successful conversion time
            sample.stop(Timer.builder("imagecloud.image.conversion.duration")
                    .tag("source_format", sourceFormat)
                    .tag("target_format", targetFormat)
                    .tag("status", "success")
                    .description("Image conversion processing time")
                    .register(meterRegistry));

            return result;
        } catch (IOException e) {
            // Record failed conversion time
            sample.stop(Timer.builder("imagecloud.image.conversion.duration")
                    .tag("source_format", sourceFormat)
                    .tag("target_format", targetFormat)
                    .tag("status", "failed")
                    .description("Image conversion processing time")
                    .register(meterRegistry));
            throw e;
        }
    }
}
