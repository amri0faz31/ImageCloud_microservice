package main.java.com.imagecloud.main.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.conversion-request}")
    private String conversionRequestQueue;

    @Value("${rabbitmq.queue.conversion-response}")
    private String conversionResponseQueue;

    @Value("${rabbitmq.exchange.image}")
    private String imageExchange;

    @Value("${rabbitmq.routing-key.conversion-request}")
    private String conversionRequestRoutingKey;

    @Value("${rabbitmq.routing-key.conversion-response}")
    private String conversionResponseRoutingKey;

    @Bean
    public Queue conversionRequestQueue() {
        return new Queue(conversionRequestQueue, true);
    }

    @Bean
    public Queue conversionResponseQueue() {
        return new Queue(conversionResponseQueue, true);
    }

    @Bean
    public TopicExchange imageExchange() {
        return new TopicExchange(imageExchange);
    }

    @Bean
    public Binding conversionRequestBinding() {
        return BindingBuilder
                .bind(conversionRequestQueue())
                .to(imageExchange())
                .with(conversionRequestRoutingKey);
    }

    @Bean
    public Binding conversionResponseBinding() {
        return BindingBuilder
                .bind(conversionResponseQueue())
                .to(imageExchange())
                .with(conversionResponseRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
