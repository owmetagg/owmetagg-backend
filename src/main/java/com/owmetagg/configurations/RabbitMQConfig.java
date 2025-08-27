package com.owmetagg.configurations;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import org.springframework.web.client.RestTemplate;

@Configuration
@Getter
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.player-data:player.data.queue}")
    private String playerDataQueue;

    @Value("${rabbitmq.exchange.player-data:player.data.exchange}")
    private String playerDataExchange;

    @Value("${rabbitmq.routing-key.player-data:player.data}")
    private String playerDataRoutingKey;

    // Dead letter queue configuration
    @Value("${rabbitmq.queue.player-data-dlq:player.data.dlq}")
    private String playerDataDlq;

    @Value("${rabbitmq.exchange.player-data-dlx:player.data.dlx}")
    private String playerDataDlx;

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    // Main exchange for player data
    @Bean
    public DirectExchange playerDataExchange() {
        return new DirectExchange(playerDataExchange, true, false);
    }

    // Dead letter exchange
    @Bean
    public DirectExchange playerDataDeadLetterExchange() {
        return new DirectExchange(playerDataDlx, true, false);
    }

    // Main queue with DLQ configuration
    @Bean
    public Queue playerDataQueue() {
        return QueueBuilder.durable(playerDataQueue)
                .withArgument("x-dead-letter-exchange", playerDataDlx)
                .withArgument("x-dead-letter-routing-key", "dead.letter")
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    // Dead letter queue
    @Bean
    public Queue playerDataDeadLetterQueue() {
        return QueueBuilder.durable(playerDataDlq).build();
    }

    // Binding for main queue
    @Bean
    public Binding playerDataBinding() {
        return BindingBuilder
                .bind(playerDataQueue())
                .to(playerDataExchange())
                .with(playerDataRoutingKey);
    }

    // Binding for dead letter queue
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(playerDataDeadLetterQueue())
                .to(playerDataDeadLetterExchange())
                .with("dead.letter");
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}