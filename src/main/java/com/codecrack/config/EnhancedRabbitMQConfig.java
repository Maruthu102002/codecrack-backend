package com.codecrack.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnhancedRabbitMQConfig {

    @Value("${app.queue.submissions}")
    private String submissionsQueue;

    @Value("${app.queue.submissions-priority}")
    private String priorityQueue;

    @Value("${app.queue.submissions-dlq}")
    private String dlqQueue;

    // ===== EXCHANGES =====
    @Bean
    public DirectExchange submissionsExchange() {
        return new DirectExchange("codecrack.exchange", true, false);
    }

    @Bean
    public DirectExchange dlqExchange() {
        return new DirectExchange("codecrack.dlq.exchange", true, false);
    }

    // ===== QUEUES =====
    @Bean
    public Queue submissionsQueue() {
        return QueueBuilder.durable(submissionsQueue)
                .withArgument("x-dead-letter-exchange", "codecrack.dlq.exchange")
                .withArgument("x-dead-letter-routing-key", dlqQueue)
                .withArgument("x-message-ttl", 300000)
                .build();
    }

    @Bean
    public Queue priorityQueue() {
        return QueueBuilder.durable(priorityQueue)
                .withArgument("x-dead-letter-exchange", "codecrack.dlq.exchange")
                .withArgument("x-max-priority", 10)
                .build();
    }

    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(dlqQueue).build();
    }

    // ===== BINDINGS =====
    @Bean
    public Binding submissionsBinding() {
        return BindingBuilder
                .bind(submissionsQueue())
                .to(submissionsExchange())
                .with(submissionsQueue);
    }

    @Bean
    public Binding priorityBinding() {
        return BindingBuilder
                .bind(priorityQueue())
                .to(submissionsExchange())
                .with(priorityQueue);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder
                .bind(dlqQueue())
                .to(dlqExchange())
                .with(dlqQueue);
    }

    // ===== MESSAGE CONVERTER =====
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

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(1);
        return factory;
    }
}