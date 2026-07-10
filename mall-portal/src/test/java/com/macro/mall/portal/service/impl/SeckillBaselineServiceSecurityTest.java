package com.macro.mall.portal.service.impl;

import com.macro.mall.common.exception.ApiException;
import com.macro.mall.mapper.OmsOrderItemMapper;
import com.macro.mall.mapper.OmsOrderMapper;
import com.macro.mall.mapper.PmsProductMapper;
import com.macro.mall.mapper.PmsSkuStockMapper;
import com.macro.mall.mapper.SmsFlashPromotionProductRelationMapper;
import com.macro.mall.mapper.UmsMemberReceiveAddressMapper;
import com.macro.mall.model.PmsProduct;
import com.macro.mall.model.PmsSkuStock;
import com.macro.mall.model.SmsFlashPromotionProductRelation;
import com.macro.mall.model.UmsMember;
import com.macro.mall.portal.dao.SeckillBaselineDao;
import com.macro.mall.portal.domain.BaselineSeckillOrderParam;
import com.macro.mall.portal.service.UmsMemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeckillBaselineServiceSecurityTest {

    @InjectMocks
    private SeckillBaselineServiceImpl seckillBaselineService;

    @Mock
    private UmsMemberService memberService;
    @Mock
    private SeckillBaselineDao seckillBaselineDao;
    @Mock
    private SmsFlashPromotionProductRelationMapper relationMapper;
    @Mock
    private PmsProductMapper productMapper;
    @Mock
    private PmsSkuStockMapper skuStockMapper;
    @Mock
    private UmsMemberReceiveAddressMapper receiveAddressMapper;
    @Mock
    private OmsOrderMapper orderMapper;
    @Mock
    private OmsOrderItemMapper orderItemMapper;

    @Test
    void generateOrder_shouldRejectDuplicateMemberOrder() {
        BaselineSeckillOrderParam orderParam = new BaselineSeckillOrderParam();
        orderParam.setRelationId(1L);

        UmsMember member = new UmsMember();
        member.setId(1L);
        member.setUsername("memberA");

        SmsFlashPromotionProductRelation relation = new SmsFlashPromotionProductRelation();
        relation.setId(1L);
        relation.setProductId(10L);
        relation.setFlashPromotionPrice(new BigDecimal("9.90"));
        relation.setFlashPromotionLimit(1);

        PmsProduct product = new PmsProduct();
        product.setId(10L);
        product.setDeleteStatus(0);

        PmsSkuStock skuStock = new PmsSkuStock();
        skuStock.setId(100L);
        skuStock.setProductId(10L);

        when(memberService.getCurrentMember()).thenReturn(member);
        when(relationMapper.selectByPrimaryKey(1L)).thenReturn(relation);
        when(productMapper.selectByPrimaryKey(10L)).thenReturn(product);
        when(skuStockMapper.selectByExample(any())).thenReturn(Collections.singletonList(skuStock));
        when(seckillBaselineDao.insertOrderLog(anyLong(), anyLong(), any(), any()))
                .thenThrow(new DuplicateKeyException("duplicate member relation"));

        ApiException exception = assertThrows(ApiException.class, () -> seckillBaselineService.generateOrder(orderParam));

        assertTrue(exception.getMessage().contains("each member can only place one order"));
    }
}
