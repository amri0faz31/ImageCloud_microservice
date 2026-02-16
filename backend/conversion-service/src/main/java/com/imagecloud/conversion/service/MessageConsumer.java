package main.java.com.imagecloud.conversion.service;

import com.imagecloud.conversion.dto.ConversionRequest;
import com.imagecloud.conversion.dto.ConversionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageConsumer {

    private final ImageConversionService conversionService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.image}")
    private String imageExchange;

    @Value("${rabbitmq.routing-key.conversion-response}")
    private String conversionResponseRoutingKey;

    @RabbitListener(queues = "${rabbitmq.queue.conversion-request}")
    public void consumeConversionRequest(ConversionRequest request) {
        log.info("Received conversion request for image ID: {}", request.getImageId());

        ConversionResponse response = new ConversionResponse();
        response.setImageId(request.getImageId());

        try {
            // Perform image conversion
            byte[] convertedData = conversionService.convertImage(
                    request.getImageData(),
                    request.getOriginalFormat(),
                    request.getTargetFormat()
            );

            response.setConvertedImageData(convertedData);
            response.setSuccess(true);
            log.info("Successfully converted image ID: {}", request.getImageId());

        } catch (Exception e) {
            log.error("Error converting image ID: {}", request.getImageId(), e);
            response.setSuccess(false);
            response.setErrorMessage("Conversion failed: " + e.getMessage());
        }

        // Send response back via RabbitMQ
        rabbitTemplate.convertAndSend(imageExchange, conversionResponseRoutingKey, response);
        log.info("Sent conversion response for image ID: {}", request.getImageId());
    }
}
