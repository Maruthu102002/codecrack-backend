package com.codecrack.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ Configuration with DLQ, Retry, and Priority Queue
 * Only active when RabbitMQ is not excluded (i.e., prod profile)
 */
@Configuration
@ConditionalOnBean(org.springframework.amqp.rabbit.connection.ConnectionFactory.class)
public class EnhancedRabbitMQConfig {

    public static final String CODE_SUBMISSIONS_QUEUE = "code.submissions.queue";
    public static final String CODE_SUBMISSIONS_EXCHANGE = "code.submissions.exchange";
    public static final String CODE_SUBMISSIONS_ROUTING_KEY = "code.submissions";

    public static final String CODE_SUBMISSIONS_PRIORITY_QUEUE = "code.submissions.priority.queue";
    public static final String CODE_SUBMISSIONS_PRIORITY_ROUTING_KEY = "code.submissions.priority";

    public static final String CODE_SUBMISSIONS_DLQ = "code.submissions.dlq";
    public static final String CODE_SUBMISSIONS_DLX = "code.submissions.dlx";
    public static final String CODE_SUBMISSIONS_DLQ_ROUTING_KEY = "code.submissions.dlq";

    public static final String CODE_SUBMISSIONS_RETRY_QUEUE = "code.submissions.retry.queue";
    public static final String CODE_SUBMISSIONS_RETRY_ROUTING_KEY = "code.submissions.retry";

    // ========== NORMAL QUEUE ==========

    @Bean
    public Queue codeSubmissionsQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", CODE_SUBMISSIONS_DLX);
        args.put("x-dead-letter-routing-key", CODE_SUBMISSIONS_DLQ_ROUTING_KEY);
        args.put("x-message-ttl", 300000);
        args.put("x-max-priority", 10);
        args.put("x-max-length", 10000);
        args.put("x-overflow", "drop-head");
        return new Queue(CODE_SUBMISSIONS_QUEUE, true, false, false, args);
    }

    @Bean
    public DirectExchange codeSubmissionsExchange() {
        return new DirectExchange(CODE_SUBMISSIONS_EXCHANGE, true, false);
    }

    @Bean
    public Binding codeSubmissionsBinding() {
        return BindingBuilder
                .bind(codeSubmissionsQueue())
                .to(codeSubmissionsExchange())
                .with(CODE_SUBMISSIONS_ROUTING_KEY);
    }

    // ========== PRIORITY QUEUE ==========

    @Bean
    public Queue codeSubmissionsPriorityQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", CODE_SUBMISSIONS_DLX);
        args.put("x-dead-letter-routing-key", CODE_SUBMISSIONS_DLQ_ROUTING_KEY);
        args.put("x-max-priority", 10);
        args.put("x-message-ttl", 120000);
        return new Queue(CODE_SUBMISSIONS_PRIORITY_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding codeSubmissionsPriorityBinding() {
        return BindingBuilder
                .bind(codeSubmissionsPriorityQueue())
                .to(codeSubmissionsExchange())
                .with(CODE_SUBMISSIONS_PRIORITY_ROUTING_KEY);
    }

    // ========== DLQ ==========

    @Bean
    public Queue codeSubmissionsDLQ() {
        return QueueBuilder.durable(CODE_SUBMISSIONS_DLQ).build();
    }

    @Bean
    public DirectExchange codeSubmissionsDLX() {
        return new DirectExchange(CODE_SUBMISSIONS_DLX, true, false);
    }

    @Bean
    public Binding codeSubmissionsDLQBinding() {
        return BindingBuilder
                .bind(codeSubmissionsDLQ())
                .to(codeSubmissionsDLX())
                .with(CODE_SUBMISSIONS_DLQ_ROUTING_KEY);
    }

    // ========== RETRY QUEUE ==========

    @Bean
    public Queue codeSubmissionsRetryQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", CODE_SUBMISSIONS_EXCHANGE);
        args.put("x-dead-letter-routing-key", CODE_SUBMISSIONS_ROUTING_KEY);
        args.put("x-message-ttl", 30000);
        return new Queue(CODE_SUBMISSIONS_RETRY_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding codeSubmissionsRetryBinding() {
        return BindingBuilder
                .bind(codeSubmissionsRetryQueue())
                .to(codeSubmissionsExchange())
                .with(CODE_SUBMISSIONS_RETRY_ROUTING_KEY);
    }

    // ========== CONVERTERS & TEMPLATE ==========

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);

        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.err.println("Message not confirmed: " + cause);
            }
        });

        template.setReturnsCallback(returned ->
                System.err.println("Message returned: " + returned.getMessage()));

        return template;
    }

    // ========== RETRY & LISTENER ==========

    @Bean
    public RetryOperationsInterceptor retryInterceptor() {
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000);

        return RetryInterceptorBuilder
                .stateless()
                .retryPolicy(retryPolicy)
                .backOffPolicy(backOffPolicy)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter,
            RetryOperationsInterceptor retryInterceptor
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(1);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setAdviceChain(retryInterceptor);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
