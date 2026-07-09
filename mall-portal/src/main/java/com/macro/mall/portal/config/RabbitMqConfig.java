package com.macro.mall.portal.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.macro.mall.portal.domain.QueueEnum;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    DirectExchange orderDirect() {
        return ExchangeBuilder
                .directExchange(QueueEnum.QUEUE_ORDER_CANCEL.getExchange())
                .durable(true)
                .build();
    }

    @Bean
    DirectExchange orderTtlDirect() {
        return ExchangeBuilder
                .directExchange(QueueEnum.QUEUE_TTL_ORDER_CANCEL.getExchange())
                .durable(true)
                .build();
    }

    @Bean
    DirectExchange seckillDirect() {
        return ExchangeBuilder
                .directExchange(QueueEnum.QUEUE_SECKILL_ORDER.getExchange())
                .durable(true)
                .build();
    }

    @Bean
    public Queue orderQueue() {
        return QueueBuilder
                .durable(QueueEnum.QUEUE_ORDER_CANCEL.getName())
                .build();
    }

    @Bean
    public Queue orderTtlQueue() {
        return QueueBuilder
                .durable(QueueEnum.QUEUE_TTL_ORDER_CANCEL.getName())
                .withArgument("x-dead-letter-exchange", QueueEnum.QUEUE_ORDER_CANCEL.getExchange())
                .withArgument("x-dead-letter-routing-key", QueueEnum.QUEUE_ORDER_CANCEL.getRouteKey())
                .build();
    }

    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder
                .durable(QueueEnum.QUEUE_SECKILL_ORDER.getName())
                .build();
    }

    @Bean
    Binding orderBinding(DirectExchange orderDirect, Queue orderQueue) {
        return BindingBuilder
                .bind(orderQueue)
                .to(orderDirect)
                .with(QueueEnum.QUEUE_ORDER_CANCEL.getRouteKey());
    }

    @Bean
    Binding orderTtlBinding(DirectExchange orderTtlDirect, Queue orderTtlQueue) {
        return BindingBuilder
                .bind(orderTtlQueue)
                .to(orderTtlDirect)
                .with(QueueEnum.QUEUE_TTL_ORDER_CANCEL.getRouteKey());
    }

    @Bean
    Binding seckillOrderBinding(DirectExchange seckillDirect, Queue seckillOrderQueue) {
        return BindingBuilder
                .bind(seckillOrderQueue)
                .to(seckillDirect)
                .with(QueueEnum.QUEUE_SECKILL_ORDER.getRouteKey());
    }
}
