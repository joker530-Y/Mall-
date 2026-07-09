package com.macro.mall.portal.component;

import com.macro.mall.portal.domain.QueueEnum;
import com.macro.mall.portal.domain.SeckillOrderMessage;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SeckillOrderSender {
    @Autowired
    private AmqpTemplate amqpTemplate;

    public void send(SeckillOrderMessage message) {
        amqpTemplate.convertAndSend(
                QueueEnum.QUEUE_SECKILL_ORDER.getExchange(),
                QueueEnum.QUEUE_SECKILL_ORDER.getRouteKey(),
                message
        );
    }
}
