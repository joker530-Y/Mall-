package com.macro.mall.portal.component;

import com.macro.mall.portal.domain.QueueEnum;
import com.macro.mall.portal.domain.SeckillOrderMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SeckillOrderSenderTest {

    @InjectMocks
    private SeckillOrderSender seckillOrderSender;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(seckillOrderSender, "confirmTimeoutMs", 1000L);
    }

    @Test
    void send_shouldWaitForAck() {
        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setRequestId("req-1");
        message.setRelationId(1L);
        message.setMemberId(2L);

        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(3);
            correlationData.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                eq(QueueEnum.QUEUE_SECKILL_ORDER.getExchange()),
                eq(QueueEnum.QUEUE_SECKILL_ORDER.getRouteKey()),
                eq(message),
                any(CorrelationData.class)
        );

        seckillOrderSender.send(message);

        ArgumentCaptor<CorrelationData> captor = ArgumentCaptor.forClass(CorrelationData.class);
        verify(rabbitTemplate).convertAndSend(
                eq(QueueEnum.QUEUE_SECKILL_ORDER.getExchange()),
                eq(QueueEnum.QUEUE_SECKILL_ORDER.getRouteKey()),
                eq(message),
                captor.capture()
        );
        assertEquals("req-1", captor.getValue().getId());
    }

    @Test
    void send_shouldFailOnNack() {
        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setRequestId("req-nack");

        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(3);
            correlationData.getFuture().complete(new CorrelationData.Confirm(false, "broker nack"));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                eq(QueueEnum.QUEUE_SECKILL_ORDER.getExchange()),
                eq(QueueEnum.QUEUE_SECKILL_ORDER.getRouteKey()),
                eq(message),
                any(CorrelationData.class)
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> seckillOrderSender.send(message));
        assertTrue(ex.getMessage().contains("nack"));
    }

    @Test
    void send_shouldFailWhenConfirmFutureErrors() {
        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setRequestId("req-err");

        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(3);
            CompletableFuture<CorrelationData.Confirm> future = correlationData.getFuture();
            future.completeExceptionally(new RuntimeException("returned"));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                eq(QueueEnum.QUEUE_SECKILL_ORDER.getExchange()),
                eq(QueueEnum.QUEUE_SECKILL_ORDER.getRouteKey()),
                eq(message),
                any(CorrelationData.class)
        );

        assertThrows(IllegalStateException.class, () -> seckillOrderSender.send(message));
    }
}
