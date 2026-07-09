package com.macro.mall.portal.component;

import com.macro.mall.portal.domain.SeckillOrderMessage;
import com.macro.mall.portal.service.SeckillRedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = "mall.seckill.order")
public class SeckillOrderReceiver {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeckillOrderReceiver.class);

    @Autowired
    private SeckillRedisService seckillRedisService;

    @RabbitHandler
    public void handle(SeckillOrderMessage message) {
        try {
            seckillRedisService.consumeOrder(message);
        } catch (Exception e) {
            LOGGER.error("process seckill order failed, requestId={}", message.getRequestId(), e);
            seckillRedisService.markConsumeFailed(message, e.getMessage());
        }
    }
}
