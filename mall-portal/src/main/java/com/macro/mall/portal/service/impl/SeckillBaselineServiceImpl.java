package com.macro.mall.portal.service.impl;

import com.macro.mall.common.exception.Asserts;
import com.macro.mall.mapper.*;
import com.macro.mall.model.*;
import com.macro.mall.portal.dao.SeckillBaselineDao;
import com.macro.mall.portal.domain.BaselineSeckillOrderParam;
import com.macro.mall.portal.domain.BaselineSeckillOrderResult;
import com.macro.mall.portal.service.SeckillBaselineService;
import com.macro.mall.portal.service.UmsMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SeckillBaselineServiceImpl implements SeckillBaselineService {
    private static final BigDecimal ZERO = new BigDecimal("0");
    private static final AtomicLong ORDER_SN_SEQUENCE = new AtomicLong(0);

    @Autowired
    private UmsMemberService memberService;
    @Autowired
    private SeckillBaselineDao seckillBaselineDao;
    @Autowired
    private SmsFlashPromotionProductRelationMapper relationMapper;
    @Autowired
    private PmsProductMapper productMapper;
    @Autowired
    private PmsSkuStockMapper skuStockMapper;
    @Autowired
    private UmsMemberReceiveAddressMapper receiveAddressMapper;
    @Autowired
    private OmsOrderMapper orderMapper;
    @Autowired
    private OmsOrderItemMapper orderItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaselineSeckillOrderResult generateOrder(BaselineSeckillOrderParam orderParam) {
        if (orderParam == null || orderParam.getRelationId() == null) {
            Asserts.fail("relationId is required");
        }
        Integer quantity = normalizeQuantity(orderParam.getQuantity());
        UmsMember currentMember = memberService.getCurrentMember();
        SmsFlashPromotionProductRelation relation = relationMapper.selectByPrimaryKey(orderParam.getRelationId());
        if (relation == null) {
            Asserts.fail("seckill relation does not exist");
        }
        if (relation.getFlashPromotionLimit() != null && quantity > relation.getFlashPromotionLimit()) {
            Asserts.fail("quantity exceeds seckill purchase limit");
        }
        PmsProduct product = productMapper.selectByPrimaryKey(relation.getProductId());
        if (product == null || Integer.valueOf(1).equals(product.getDeleteStatus())) {
            Asserts.fail("seckill product does not exist");
        }
        PmsSkuStock skuStock = getSkuStock(orderParam.getProductSkuId(), relation.getProductId());

        Date now = new Date();
        insertOrderLog(currentMember.getId(), relation.getId(), now);
        int decreased = seckillBaselineDao.decreaseFlashPromotionStock(relation.getId(), quantity);
        if (decreased != 1) {
            Asserts.fail("seckill stock is not enough");
        }

        OmsOrder order = buildOrder(orderParam, currentMember, product, relation, quantity, now);
        orderMapper.insertSelective(order);
        OmsOrderItem orderItem = buildOrderItem(order, product, skuStock, relation, quantity);
        orderItemMapper.insertSelective(orderItem);
        seckillBaselineDao.updateOrderLogSuccess(currentMember.getId(), relation.getId(), order.getId(), now);

        BaselineSeckillOrderResult result = new BaselineSeckillOrderResult();
        result.setOrderId(order.getId());
        result.setOrderSn(order.getOrderSn());
        result.setRelationId(relation.getId());
        result.setProductId(relation.getProductId());
        result.setProductSkuId(skuStock.getId());
        result.setQuantity(quantity);
        result.setPayAmount(order.getPayAmount());
        result.setRemainingStock(seckillBaselineDao.getRemainingStock(relation.getId()));
        return result;
    }

    private Integer normalizeQuantity(Integer quantity) {
        if (quantity == null) {
            return 1;
        }
        if (quantity <= 0) {
            Asserts.fail("quantity must be greater than 0");
        }
        return quantity;
    }

    private PmsSkuStock getSkuStock(Long skuId, Long productId) {
        PmsSkuStock skuStock = null;
        if (skuId != null) {
            skuStock = skuStockMapper.selectByPrimaryKey(skuId);
            if (skuStock == null || !productId.equals(skuStock.getProductId())) {
                Asserts.fail("productSkuId does not belong to seckill product");
            }
            return skuStock;
        }
        PmsSkuStockExample example = new PmsSkuStockExample();
        example.createCriteria().andProductIdEqualTo(productId);
        List<PmsSkuStock> skuStockList = skuStockMapper.selectByExample(example);
        if (skuStockList == null || skuStockList.isEmpty()) {
            Asserts.fail("seckill product has no SKU");
        }
        return skuStockList.get(0);
    }

    private void insertOrderLog(Long memberId, Long relationId, Date now) {
        try {
            seckillBaselineDao.insertOrderLog(memberId, relationId, UUID.randomUUID().toString(), now);
        } catch (DuplicateKeyException e) {
            Asserts.fail("each member can only place one order for this seckill relation");
        }
    }

    private OmsOrder buildOrder(BaselineSeckillOrderParam orderParam,
                                UmsMember member,
                                PmsProduct product,
                                SmsFlashPromotionProductRelation relation,
                                Integer quantity,
                                Date now) {
        BigDecimal payAmount = relation.getFlashPromotionPrice().multiply(new BigDecimal(quantity));
        OmsOrder order = new OmsOrder();
        order.setOrderSn(generateOrderSn(orderParam.getPayType()));
        order.setMemberId(member.getId());
        order.setMemberUsername(member.getUsername());
        order.setCreateTime(now);
        order.setTotalAmount(payAmount);
        order.setPayAmount(payAmount);
        order.setFreightAmount(ZERO);
        order.setPromotionAmount(ZERO);
        order.setIntegrationAmount(ZERO);
        order.setCouponAmount(ZERO);
        order.setDiscountAmount(ZERO);
        order.setPayType(orderParam.getPayType() == null ? 0 : orderParam.getPayType());
        order.setSourceType(1);
        order.setStatus(0);
        order.setOrderType(1);
        order.setAutoConfirmDay(15);
        order.setIntegration(product.getGiftPoint() == null ? 0 : product.getGiftPoint() * quantity);
        order.setGrowth(product.getGiftGrowth() == null ? 0 : product.getGiftGrowth() * quantity);
        order.setPromotionInfo("baseline-seckill relationId=" + relation.getId());
        order.setConfirmStatus(0);
        order.setDeleteStatus(0);
        order.setUseIntegration(0);
        applyReceiver(order, orderParam.getMemberReceiveAddressId(), member);
        return order;
    }

    private void applyReceiver(OmsOrder order, Long addressId, UmsMember member) {
        UmsMemberReceiveAddress address = null;
        if (addressId != null) {
            address = receiveAddressMapper.selectByPrimaryKey(addressId);
            if (address == null || !member.getId().equals(address.getMemberId())) {
                Asserts.fail("member receive address does not exist");
            }
        } else {
            UmsMemberReceiveAddressExample example = new UmsMemberReceiveAddressExample();
            example.createCriteria().andMemberIdEqualTo(member.getId());
            example.setOrderByClause("default_status desc, id asc");
            List<UmsMemberReceiveAddress> addressList = receiveAddressMapper.selectByExample(example);
            if (addressList != null && !addressList.isEmpty()) {
                address = addressList.get(0);
            }
        }
        if (address != null) {
            order.setReceiverName(address.getName());
            order.setReceiverPhone(address.getPhoneNumber());
            order.setReceiverPostCode(address.getPostCode());
            order.setReceiverProvince(address.getProvince());
            order.setReceiverCity(address.getCity());
            order.setReceiverRegion(address.getRegion());
            order.setReceiverDetailAddress(address.getDetailAddress());
            return;
        }
        order.setReceiverName(member.getUsername());
        order.setReceiverPhone(member.getPhone() == null ? "00000000000" : member.getPhone());
        order.setReceiverPostCode("");
        order.setReceiverProvince("");
        order.setReceiverCity("");
        order.setReceiverRegion("");
        order.setReceiverDetailAddress("");
    }

    private OmsOrderItem buildOrderItem(OmsOrder order,
                                        PmsProduct product,
                                        PmsSkuStock skuStock,
                                        SmsFlashPromotionProductRelation relation,
                                        Integer quantity) {
        OmsOrderItem orderItem = new OmsOrderItem();
        orderItem.setOrderId(order.getId());
        orderItem.setOrderSn(order.getOrderSn());
        orderItem.setProductId(product.getId());
        orderItem.setProductPic(product.getPic());
        orderItem.setProductName(product.getName());
        orderItem.setProductBrand(product.getBrandName());
        orderItem.setProductSn(product.getProductSn());
        orderItem.setProductPrice(relation.getFlashPromotionPrice());
        orderItem.setProductQuantity(quantity);
        orderItem.setProductSkuId(skuStock.getId());
        orderItem.setProductSkuCode(skuStock.getSkuCode());
        orderItem.setProductCategoryId(product.getProductCategoryId());
        orderItem.setPromotionName("baseline-seckill relationId=" + relation.getId());
        orderItem.setPromotionAmount(ZERO);
        orderItem.setCouponAmount(ZERO);
        orderItem.setIntegrationAmount(ZERO);
        orderItem.setRealAmount(relation.getFlashPromotionPrice());
        orderItem.setGiftIntegration(product.getGiftPoint() == null ? 0 : product.getGiftPoint());
        orderItem.setGiftGrowth(product.getGiftGrowth() == null ? 0 : product.getGiftGrowth());
        orderItem.setProductAttr(skuStock.getSpData());
        return orderItem;
    }

    private String generateOrderSn(Integer payType) {
        String date = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        long sequence = ORDER_SN_SEQUENCE.updateAndGet(value -> value >= 9999 ? 1 : value + 1);
        int payCode = payType == null ? 0 : payType;
        return "SK" + date + String.format("%02d%04d%04d", payCode, sequence, ThreadLocalRandom.current().nextInt(10000));
    }
}
