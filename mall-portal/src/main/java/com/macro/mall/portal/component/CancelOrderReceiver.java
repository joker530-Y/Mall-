package com.macro.mall.portal.component;

import com.macro.mall.portal.service.OmsPortalOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *  取消订单消息的接收者
 * Created by macro on 2018/9/14.
 */
@Component
@RabbitListener(queues = "mall.order.cancel")
public class CancelOrderReceiver {
    /** 日志记录器 */
    private static final Logger LOGGER = LoggerFactory.getLogger(CancelOrderReceiver.class);
    /** 前台订单服务 */
    @Autowired
    private OmsPortalOrderService portalOrderService;

    /**
     * 接收延迟队列中的取消订单消息，执行取消订单操作
     * @param orderId 订单ID
     */
    @RabbitHandler
    public void handle(Long orderId){
        portalOrderService.cancelOrder(orderId);
        LOGGER.info("process orderId:{}",orderId);
    }
}
