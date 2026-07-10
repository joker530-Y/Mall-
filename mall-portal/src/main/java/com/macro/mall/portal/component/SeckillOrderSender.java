package com.macro.mall.portal.component;

import com.macro.mall.portal.domain.QueueEnum;
import com.macro.mall.portal.domain.SeckillOrderMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 秒杀下单消息发送：等待 publisher confirm，失败时抛出异常以便上层回补 Redis。
 */
@Component
public class SeckillOrderSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeckillOrderSender.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${mall.seckill.mq-confirm-timeout-ms:3000}")
    private long confirmTimeoutMs;

    public void send(SeckillOrderMessage message) {
        String correlationId = message.getRequestId() != null
                ? message.getRequestId()
                : java.util.UUID.randomUUID().toString();
        CorrelationData correlationData = new CorrelationData(correlationId);
        rabbitTemplate.convertAndSend(
                QueueEnum.QUEUE_SECKILL_ORDER.getExchange(),
                QueueEnum.QUEUE_SECKILL_ORDER.getRouteKey(),
                message,
                correlationData
        );
        try {
            CorrelationData.Confirm confirm = correlationData.getFuture()
                    .get(confirmTimeoutMs, TimeUnit.MILLISECONDS);
            if (confirm == null || !confirm.isAck()) {
                String reason = confirm == null ? "null confirm" : confirm.getReason();
                throw new IllegalStateException("seckill MQ publish nack: " + reason);
            }
        } catch (TimeoutException e) {
            LOGGER.error("seckill MQ publish confirm timeout, requestId={}", correlationId);
            throw new IllegalStateException("seckill MQ publish confirm timeout", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("seckill MQ publish confirm interrupted", e);
        } catch (ExecutionException e) {
            LOGGER.error("seckill MQ publish confirm failed, requestId={}", correlationId, e);
            throw new IllegalStateException("seckill MQ publish confirm failed", e);
        }
    }
}
