package main.java.com.imagecloud.main.service;

import com.imagecloud.main.dto.ConversionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageConsumer {

    private final ImageService imageService;

    @RabbitListener(queues = "${rabbitmq.queue.conversion-response}")
    public void consumeConversionResponse(ConversionResponse response) {
        log.info("Consuming conversion response for image ID: {}", response.getImageId());
        imageService.handleConversionResponse(response);
    }
}
