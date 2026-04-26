package com.springAi.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitConfig {

    public static final String DOC_INGEST_EXCHANGE = "doc.ingest.exchange";
    public static final String DOC_INGEST_QUEUE = "doc.ingest.queue";
    public static final String DOC_INGEST_ROUTING_KEY = "doc.ingest";

    public static final String DOC_INGEST_DLX_EXCHANGE = "doc.ingest.dlx.exchange";
    public static final String DOC_INGEST_DLX_QUEUE = "doc.ingest.dlx.queue";
    public static final String DOC_INGEST_DLX_ROUTING_KEY = "doc.ingest.dlx";

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }

    @Bean
    public DirectExchange docIngestExchange() {
        return new DirectExchange(DOC_INGEST_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange docIngestDlxExchange() {
        return new DirectExchange(DOC_INGEST_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue docIngestQueue() {
        return new Queue(DOC_INGEST_QUEUE, true, false, false, Map.of(
                "x-dead-letter-exchange", DOC_INGEST_DLX_EXCHANGE,
                "x-dead-letter-routing-key", DOC_INGEST_DLX_ROUTING_KEY
        ));
    }

    @Bean
    public Queue docIngestDlxQueue() {
        return new Queue(DOC_INGEST_DLX_QUEUE, true);
    }

    @Bean
    public Binding docIngestBinding(
            @Qualifier("docIngestExchange") DirectExchange docIngestExchange,
            @Qualifier("docIngestQueue") Queue docIngestQueue) {
        return BindingBuilder.bind(docIngestQueue)
                .to(docIngestExchange)
                .with(DOC_INGEST_ROUTING_KEY);
    }

    @Bean
    public Binding docIngestDlxBinding(
            @Qualifier("docIngestDlxExchange") DirectExchange docIngestDlxExchange,
            @Qualifier("docIngestDlxQueue") Queue docIngestDlxQueue) {
        return BindingBuilder.bind(docIngestDlxQueue)
                .to(docIngestDlxExchange)
                .with(DOC_INGEST_DLX_ROUTING_KEY);
    }
}
