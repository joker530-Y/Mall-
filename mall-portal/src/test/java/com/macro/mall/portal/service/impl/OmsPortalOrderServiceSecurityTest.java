package com.macro.mall.portal.service.impl;

import cn.hutool.json.JSONUtil;
import com.macro.mall.common.exception.ApiException;
import com.macro.mall.common.service.RedisService;
import com.macro.mall.mapper.OmsOrderItemMapper;
import com.macro.mall.mapper.OmsOrderMapper;
import com.macro.mall.model.OmsOrder;
import com.macro.mall.model.OmsOrderExample;
import com.macro.mall.model.UmsMember;
import com.macro.mall.portal.dao.PortalOrderDao;
import com.macro.mall.portal.domain.OmsOrderDetail;
import com.macro.mall.portal.domain.OrderIdempotentSnapshot;
import com.macro.mall.portal.domain.OrderParam;
import com.macro.mall.portal.service.UmsMemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OmsPortalOrderServiceSecurityTest {

    @InjectMocks
    private OmsPortalOrderServiceImpl orderService;

    @Mock
    private UmsMemberService memberService;
    @Mock
    private OmsOrderMapper orderMapper;
    @Mock
    private OmsOrderItemMapper orderItemMapper;
    @Mock
    private PortalOrderDao portalOrderDao;
    @Mock
    private RedisService redisService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void generateOrder_shouldReturnCachedResultWhenRequestIdExists() {
        UmsMember member = member(1L, "memberA");
        when(memberService.getCurrentMember()).thenReturn(member);
        String requestId = "req-123";
        OrderIdempotentSnapshot snapshot = new OrderIdempotentSnapshot();
        OmsOrder cachedOrder = order(99L, 1L, 0);
        cachedOrder.setOrderSn("SN99");
        snapshot.setOrder(cachedOrder);
        snapshot.setOrderItemList(Collections.emptyList());
        when(redisService.get("mall:portal:order:idempotent:1:req-123")).thenReturn(JSONUtil.toJsonStr(snapshot));

        Map<String, Object> result = orderService.generateOrder(new OrderParam(), requestId);

        assertEquals(99L, ((OmsOrder) result.get("order")).getId());
        verify(orderMapper, never()).insert(any());
    }

    @Test
    void generateOrder_shouldRejectDuplicateProcessingRequest() {
        UmsMember member = member(1L, "memberA");
        when(memberService.getCurrentMember()).thenReturn(member);
        when(redisService.get(anyString())).thenReturn(null);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class,
                () -> orderService.generateOrder(new OrderParam(), "req-duplicate"));

        assertTrue(exception.getMessage().contains("订单处理中"));
    }

    @Test
    void mockPay_shouldRejectOrderOwnedByAnotherMember() {
        when(memberService.getCurrentMember()).thenReturn(member(1L, "memberA"));
        when(orderMapper.selectByPrimaryKey(100L)).thenReturn(order(100L, 2L, 0));

        ApiException exception = assertThrows(ApiException.class, () -> orderService.mockPay(100L, 1));

        assertTrue(exception.getMessage().contains("无权支付"));
    }

    @Test
    void detail_shouldRejectOrderOwnedByAnotherMember() {
        when(memberService.getCurrentMember()).thenReturn(member(1L, "memberA"));
        when(orderMapper.selectByPrimaryKey(100L)).thenReturn(order(100L, 2L, 0));

        ApiException exception = assertThrows(ApiException.class, () -> orderService.detail(100L));

        assertTrue(exception.getMessage().contains("无权查看"));
        verify(orderItemMapper, never()).selectByExample(any());
    }

    @Test
    void paySuccess_shouldBeIdempotentForAlreadyPaidOrder() {
        OmsOrder paidOrder = order(100L, 1L, 1);
        when(orderMapper.updateByExampleSelective(any(OmsOrder.class), any(OmsOrderExample.class))).thenReturn(0);
        when(orderMapper.selectByPrimaryKey(100L)).thenReturn(paidOrder);

        Integer result = orderService.paySuccess(100L, 1);

        assertEquals(0, result);
        verify(portalOrderDao, never()).updateSkuStock(any());
    }

    @Test
    void mockPay_shouldReturnZeroWhenOrderAlreadyPaid() {
        when(memberService.getCurrentMember()).thenReturn(member(1L, "memberA"));
        when(orderMapper.selectByPrimaryKey(100L)).thenReturn(order(100L, 1L, 1));

        Integer result = orderService.mockPay(100L, 1);

        assertEquals(0, result);
        verify(orderMapper, never()).updateByExampleSelective(any(), any());
    }

    @Test
    void paySuccess_shouldDeductStockOnlyOnce() {
        OmsOrderDetail orderDetail = new OmsOrderDetail();
        orderDetail.setOrderItemList(Collections.emptyList());
        when(orderMapper.updateByExampleSelective(any(OmsOrder.class), any(OmsOrderExample.class))).thenReturn(1);
        when(portalOrderDao.getDetail(100L)).thenReturn(orderDetail);
        when(portalOrderDao.updateSkuStock(Collections.emptyList())).thenReturn(0);

        Integer result = orderService.paySuccess(100L, 1);

        assertEquals(0, result);
        verify(portalOrderDao).updateSkuStock(Collections.emptyList());
    }

    private UmsMember member(Long id, String username) {
        UmsMember member = new UmsMember();
        member.setId(id);
        member.setUsername(username);
        return member;
    }

    private OmsOrder order(Long id, Long memberId, Integer status) {
        OmsOrder order = new OmsOrder();
        order.setId(id);
        order.setMemberId(memberId);
        order.setStatus(status);
        order.setDeleteStatus(0);
        order.setPayType(1);
        return order;
    }
}
