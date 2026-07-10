package com.macro.mall.portal.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.macro.mall.common.exception.Asserts;
import com.macro.mall.mapper.OmsOrderItemMapper;
import com.macro.mall.mapper.OmsOrderMapper;
import com.macro.mall.mapper.OmsOrderReturnApplyMapper;
import com.macro.mall.model.OmsOrder;
import com.macro.mall.model.OmsOrderItem;
import com.macro.mall.model.OmsOrderItemExample;
import com.macro.mall.model.OmsOrderReturnApply;
import com.macro.mall.model.UmsMember;
import com.macro.mall.portal.domain.OmsOrderReturnApplyParam;
import com.macro.mall.portal.service.OmsPortalOrderReturnApplyService;
import com.macro.mall.portal.service.UmsMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 订单退货管理Service实现类
 * Created by macro on 2018/10/17.
 */
@Service
public class OmsPortalOrderReturnApplyServiceImpl implements OmsPortalOrderReturnApplyService {
    @Autowired
    private OmsOrderReturnApplyMapper returnApplyMapper;
    @Autowired
    private OmsOrderMapper orderMapper;
    @Autowired
    private OmsOrderItemMapper orderItemMapper;
    @Autowired
    private UmsMemberService memberService;

    @Override
    public int create(OmsOrderReturnApplyParam returnApply) {
        UmsMember currentMember = memberService.getCurrentMember();
        OmsOrder order = orderMapper.selectByPrimaryKey(returnApply.getOrderId());
        if (order == null || !currentMember.getId().equals(order.getMemberId())) {
            Asserts.fail("无权对该订单申请退货");
        }
        if (order.getStatus() != 1 && order.getStatus() != 2 && order.getStatus() != 3) {
            Asserts.fail("当前订单状态不可退货");
        }
        OmsOrderItemExample itemExample = new OmsOrderItemExample();
        itemExample.createCriteria()
                .andOrderIdEqualTo(order.getId())
                .andProductIdEqualTo(returnApply.getProductId());
        List<OmsOrderItem> orderItems = orderItemMapper.selectByExample(itemExample);
        if (CollUtil.isEmpty(orderItems)) {
            Asserts.fail("订单中不存在该商品");
        }
        OmsOrderItem orderItem = orderItems.get(0);
        int productCount = returnApply.getProductCount() != null
                ? returnApply.getProductCount()
                : orderItem.getProductQuantity();

        OmsOrderReturnApply realApply = new OmsOrderReturnApply();
        realApply.setOrderId(order.getId());
        realApply.setOrderSn(order.getOrderSn());
        realApply.setMemberUsername(order.getMemberUsername());
        realApply.setProductId(orderItem.getProductId());
        realApply.setProductPic(orderItem.getProductPic());
        realApply.setProductName(orderItem.getProductName());
        realApply.setProductBrand(orderItem.getProductBrand());
        realApply.setProductAttr(orderItem.getProductAttr());
        realApply.setProductCount(productCount);
        realApply.setProductPrice(orderItem.getProductPrice());
        realApply.setProductRealPrice(orderItem.getRealAmount());
        realApply.setReturnAmount(orderItem.getRealAmount().multiply(new BigDecimal(productCount)));
        realApply.setReturnName(returnApply.getReturnName());
        realApply.setReturnPhone(returnApply.getReturnPhone());
        realApply.setReason(returnApply.getReason());
        realApply.setDescription(returnApply.getDescription());
        realApply.setProofPics(returnApply.getProofPics());
        realApply.setCreateTime(new Date());
        realApply.setStatus(0);
        return returnApplyMapper.insert(realApply);
    }
}
