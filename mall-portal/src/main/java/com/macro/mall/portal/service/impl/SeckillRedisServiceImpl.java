package com.macro.mall.portal.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.macro.mall.common.exception.Asserts;
import com.macro.mall.mapper.*;
import com.macro.mall.model.*;
import com.macro.mall.portal.component.SeckillOrderSender;
import com.macro.mall.portal.dao.SeckillBaselineDao;
import com.macro.mall.portal.domain.*;
import com.macro.mall.portal.service.SeckillRedisService;
import com.macro.mall.portal.service.UmsMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SeckillRedisServiceImpl implements SeckillRedisService {
    private static final BigDecimal ZERO = new BigDecimal("0");
    private static final long RESULT_TTL_SECONDS = 1800L;
    private static final AtomicLong ORDER_SN_SEQUENCE = new AtomicLong(0);

    private static final String SECKILL_SCRIPT = """
            local stock = tonumber(redis.call('GET', KEYS[1]) or '-1')
            if stock < 0 then
                return -4
            end
            local quantity = tonumber(ARGV[1])
            local limit = tonumber(ARGV[2])
            local bought = tonumber(redis.call('GET', KEYS[2]) or '0')
            if bought + quantity > limit then
                return -2
            end
            if stock < quantity then
                return -1
            end
            if redis.call('EXISTS', KEYS[3]) == 1 then
                return -3
            end
            redis.call('DECRBY', KEYS[1], quantity)
            redis.call('INCRBY', KEYS[2], quantity)
            redis.call('EXPIRE', KEYS[2], ARGV[4])
            redis.call('SET', KEYS[3], ARGV[3], 'EX', ARGV[4])
            return tonumber(redis.call('GET', KEYS[1]))
            """;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UmsMemberService memberService;
    @Autowired
    private SeckillOrderSender seckillOrderSender;
    @Autowired
    private SeckillBaselineDao seckillDao;
    @Autowired
    private SmsFlashPromotionProductRelationMapper relationMapper;
    @Autowired
    private PmsProductMapper productMapper;
    @Autowired
    private PmsSkuStockMapper skuStockMapper;
    @Autowired
    private UmsMemberMapper memberMapper;
    @Autowired
    private UmsMemberReceiveAddressMapper receiveAddressMapper;
    @Autowired
    private OmsOrderMapper orderMapper;
    @Autowired
    private OmsOrderItemMapper orderItemMapper;

    @Override
    public SeckillWarmupResult warmup(Long relationId) {
        SmsFlashPromotionProductRelation relation = getRelation(relationId);
        Integer stock = relation.getFlashPromotionCount() == null ? 0 : relation.getFlashPromotionCount();
        Integer limit = getLimit(relation);
        String stockKey = stockKey(relationId);
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
        stringRedisTemplate.opsForValue().set(limitConfigKey(relationId), String.valueOf(limit));

        SeckillWarmupResult result = new SeckillWarmupResult();
        result.setRelationId(relationId);
        result.setStock(stock);
        result.setLimit(limit);
        result.setStockKey(stockKey);
        result.setWarmedAt(LocalDateTime.now().toString());
        return result;
    }

    @Override
    public SeckillOrderSubmitResult submitOrder(SeckillOrderParam orderParam) {
        if (orderParam == null || orderParam.getRelationId() == null) {
            Asserts.fail("relationId is required");
        }
        Integer quantity = normalizeQuantity(orderParam.getQuantity());
        UmsMember currentMember = memberService.getCurrentMember();
        SmsFlashPromotionProductRelation relation = getRelation(orderParam.getRelationId());
        PmsSkuStock skuStock = getSkuStock(orderParam.getProductSkuId(), relation.getProductId());
        Integer limit = getLimit(relation);
        if (quantity > limit) {
            Asserts.fail("quantity exceeds seckill purchase limit");
        }

        String requestId = UUID.randomUUID().toString();
        String resultKey = resultKey(relation.getId(), currentMember.getId());
        SeckillOrderResult processing = buildResult(requestId, relation.getId(), "PROCESSING", null);
        Long luaResult = executeSeckillScript(relation.getId(), currentMember.getId(), quantity, limit, resultKey, processing);

        if (luaResult != null && luaResult >= 0) {
            SeckillOrderMessage message = new SeckillOrderMessage();
            message.setRequestId(requestId);
            message.setRelationId(relation.getId());
            message.setMemberId(currentMember.getId());
            message.setProductSkuId(skuStock.getId());
            message.setMemberReceiveAddressId(orderParam.getMemberReceiveAddressId());
            message.setQuantity(quantity);
            message.setPayType(orderParam.getPayType());
            try {
                seckillOrderSender.send(message);
            } catch (Exception e) {
                compensateRedis(message, "MQ_SEND_FAILED");
                Asserts.fail("send seckill order message failed");
            }
            return submitResult(requestId, relation.getId(), "PROCESSING", luaResult.intValue());
        }

        String status = switch (luaResult == null ? -99 : luaResult.intValue()) {
            case -1 -> "SOLD_OUT";
            case -2, -3 -> "REPEAT";
            case -4 -> "NOT_WARMED";
            default -> "FAILED";
        };
        if (!"REPEAT".equals(status) || !Boolean.TRUE.equals(stringRedisTemplate.hasKey(resultKey))) {
            writeResult(buildResult(requestId, relation.getId(), status, status), currentMember.getId());
        }
        return submitResult(requestId, relation.getId(), status, getRedisStock(relation.getId()));
    }

    @Override
    public SeckillOrderResult getResult(Long relationId) {
        UmsMember currentMember = memberService.getCurrentMember();
        String json = stringRedisTemplate.opsForValue().get(resultKey(relationId, currentMember.getId()));
        if (json == null) {
            SeckillOrderResult result = new SeckillOrderResult();
            result.setRelationId(relationId);
            result.setStatus("NOT_FOUND");
            return result;
        }
        try {
            return objectMapper.readValue(json, SeckillOrderResult.class);
        } catch (JsonProcessingException e) {
            Asserts.fail("invalid seckill result cache");
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void consumeOrder(SeckillOrderMessage message) {
        Long existingOrderId = seckillDao.getSuccessOrderId(message.getMemberId(), message.getRelationId());
        if (existingOrderId != null) {
            OmsOrder existingOrder = orderMapper.selectByPrimaryKey(existingOrderId);
            writeSuccessResult(message, existingOrder);
            return;
        }

        Date now = new Date();
        try {
            seckillDao.insertOrderLog(message.getMemberId(), message.getRelationId(), message.getRequestId(), now);
        } catch (DuplicateKeyException e) {
            Long orderId = seckillDao.getSuccessOrderId(message.getMemberId(), message.getRelationId());
            if (orderId != null) {
                writeSuccessResult(message, orderMapper.selectByPrimaryKey(orderId));
            }
            return;
        }

        SmsFlashPromotionProductRelation relation = getRelation(message.getRelationId());
        UmsMember member = memberMapper.selectByPrimaryKey(message.getMemberId());
        if (member == null) {
            throw new IllegalStateException("member does not exist");
        }
        PmsProduct product = productMapper.selectByPrimaryKey(relation.getProductId());
        if (product == null || Integer.valueOf(1).equals(product.getDeleteStatus())) {
            throw new IllegalStateException("seckill product does not exist");
        }
        PmsSkuStock skuStock = getSkuStock(message.getProductSkuId(), relation.getProductId());

        int decreased = seckillDao.decreaseFlashPromotionStock(relation.getId(), message.getQuantity());
        if (decreased != 1) {
            throw new IllegalStateException("db seckill stock is not enough");
        }

        OmsOrder order = buildOrder(message, member, product, relation, now);
        orderMapper.insertSelective(order);
        OmsOrderItem orderItem = buildOrderItem(order, product, skuStock, relation, message.getQuantity());
        orderItemMapper.insertSelective(orderItem);
        seckillDao.updateOrderLogSuccess(member.getId(), relation.getId(), order.getId(), now);
        writeSuccessResult(message, order);
    }

    @Override
    public void markConsumeFailed(SeckillOrderMessage message, String reason) {
        compensateRedis(message, reason);
        seckillDao.updateOrderLogFailed(message.getMemberId(), message.getRelationId(), reason, new Date());
    }

    private Long executeSeckillScript(Long relationId,
                                      Long memberId,
                                      Integer quantity,
                                      Integer limit,
                                      String resultKey,
                                      SeckillOrderResult processing) {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(SECKILL_SCRIPT);
        redisScript.setResultType(Long.class);
        return stringRedisTemplate.execute(
                redisScript,
                Arrays.asList(stockKey(relationId), limitKey(relationId, memberId), resultKey),
                String.valueOf(quantity),
                String.valueOf(limit),
                toJson(processing),
                String.valueOf(RESULT_TTL_SECONDS)
        );
    }

    private void compensateRedis(SeckillOrderMessage message, String reason) {
        stringRedisTemplate.opsForValue().increment(stockKey(message.getRelationId()), message.getQuantity());
        Long bought = stringRedisTemplate.opsForValue().increment(limitKey(message.getRelationId(), message.getMemberId()), -message.getQuantity());
        if (bought != null && bought <= 0) {
            stringRedisTemplate.delete(limitKey(message.getRelationId(), message.getMemberId()));
        }
        writeResult(buildResult(message.getRequestId(), message.getRelationId(), "FAILED", reason), message.getMemberId());
    }

    private SmsFlashPromotionProductRelation getRelation(Long relationId) {
        SmsFlashPromotionProductRelation relation = relationMapper.selectByPrimaryKey(relationId);
        if (relation == null) {
            Asserts.fail("seckill relation does not exist");
        }
        return relation;
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

    private Integer getLimit(SmsFlashPromotionProductRelation relation) {
        return relation.getFlashPromotionLimit() == null || relation.getFlashPromotionLimit() <= 0
                ? 1
                : relation.getFlashPromotionLimit();
    }

    private PmsSkuStock getSkuStock(Long skuId, Long productId) {
        if (skuId != null) {
            PmsSkuStock skuStock = skuStockMapper.selectByPrimaryKey(skuId);
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

    private OmsOrder buildOrder(SeckillOrderMessage message,
                                UmsMember member,
                                PmsProduct product,
                                SmsFlashPromotionProductRelation relation,
                                Date now) {
        BigDecimal payAmount = relation.getFlashPromotionPrice().multiply(new BigDecimal(message.getQuantity()));
        OmsOrder order = new OmsOrder();
        order.setOrderSn(generateOrderSn(message.getPayType()));
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
        order.setPayType(message.getPayType() == null ? 0 : message.getPayType());
        order.setSourceType(1);
        order.setStatus(0);
        order.setOrderType(1);
        order.setAutoConfirmDay(15);
        order.setIntegration(product.getGiftPoint() == null ? 0 : product.getGiftPoint() * message.getQuantity());
        order.setGrowth(product.getGiftGrowth() == null ? 0 : product.getGiftGrowth() * message.getQuantity());
        order.setPromotionInfo("redis-seckill relationId=" + relation.getId());
        order.setConfirmStatus(0);
        order.setDeleteStatus(0);
        order.setUseIntegration(0);
        applyReceiver(order, message.getMemberReceiveAddressId(), member);
        return order;
    }

    private void applyReceiver(OmsOrder order, Long addressId, UmsMember member) {
        UmsMemberReceiveAddress address = null;
        if (addressId != null) {
            address = receiveAddressMapper.selectByPrimaryKey(addressId);
            if (address == null || !member.getId().equals(address.getMemberId())) {
                throw new IllegalStateException("member receive address does not exist");
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
        orderItem.setPromotionName("redis-seckill relationId=" + relation.getId());
        orderItem.setPromotionAmount(ZERO);
        orderItem.setCouponAmount(ZERO);
        orderItem.setIntegrationAmount(ZERO);
        orderItem.setRealAmount(relation.getFlashPromotionPrice());
        orderItem.setGiftIntegration(product.getGiftPoint() == null ? 0 : product.getGiftPoint());
        orderItem.setGiftGrowth(product.getGiftGrowth() == null ? 0 : product.getGiftGrowth());
        orderItem.setProductAttr(skuStock.getSpData());
        return orderItem;
    }

    private void writeSuccessResult(SeckillOrderMessage message, OmsOrder order) {
        SeckillOrderResult result = buildResult(message.getRequestId(), message.getRelationId(), "SUCCESS", null);
        if (order != null) {
            result.setOrderId(order.getId());
            result.setOrderSn(order.getOrderSn());
        }
        writeResult(result, message.getMemberId());
    }

    private void writeResult(SeckillOrderResult result, Long memberId) {
        stringRedisTemplate.opsForValue().set(
                resultKey(result.getRelationId(), memberId),
                toJson(result),
                RESULT_TTL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    private SeckillOrderResult buildResult(String requestId, Long relationId, String status, String reason) {
        SeckillOrderResult result = new SeckillOrderResult();
        result.setRequestId(requestId);
        result.setRelationId(relationId);
        result.setStatus(status);
        result.setReason(reason);
        return result;
    }

    private SeckillOrderSubmitResult submitResult(String requestId, Long relationId, String status, Integer remainingStock) {
        SeckillOrderSubmitResult result = new SeckillOrderSubmitResult();
        result.setRequestId(requestId);
        result.setRelationId(relationId);
        result.setStatus(status);
        result.setRemainingStock(remainingStock);
        return result;
    }

    private Integer getRedisStock(Long relationId) {
        String value = stringRedisTemplate.opsForValue().get(stockKey(relationId));
        return value == null ? null : Integer.valueOf(value);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize seckill result failed", e);
        }
    }

    private String stockKey(Long relationId) {
        return "mall:seckill:stock:" + relationId;
    }

    private String limitKey(Long relationId, Long memberId) {
        return "mall:seckill:limit:" + relationId + ":" + memberId;
    }

    private String limitConfigKey(Long relationId) {
        return "mall:seckill:limit-config:" + relationId;
    }

    private String resultKey(Long relationId, Long memberId) {
        return "mall:seckill:result:" + relationId + ":" + memberId;
    }

    private String generateOrderSn(Integer payType) {
        String date = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        long sequence = ORDER_SN_SEQUENCE.updateAndGet(value -> value >= 9999 ? 1 : value + 1);
        int payCode = payType == null ? 0 : payType;
        return "RSK" + date + String.format("%02d%04d%04d", payCode, sequence, ThreadLocalRandom.current().nextInt(10000));
    }
}
