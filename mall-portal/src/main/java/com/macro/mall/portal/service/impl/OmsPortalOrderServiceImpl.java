package com.macro.mall.portal.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.github.pagehelper.PageHelper;
import com.macro.mall.common.api.CommonPage;
import com.macro.mall.common.exception.Asserts;
import com.macro.mall.common.service.RedisService;
import com.macro.mall.mapper.*;
import com.macro.mall.model.*;
import com.macro.mall.portal.component.CancelOrderSender;
import com.macro.mall.portal.dao.PortalOrderDao;
import com.macro.mall.portal.dao.PortalOrderItemDao;
import com.macro.mall.portal.dao.SmsCouponHistoryDao;
import com.macro.mall.portal.domain.*;
import com.macro.mall.portal.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 前台订单管理Service
 * Created by macro on 2018/8/30.
 */
@Service
public class OmsPortalOrderServiceImpl implements OmsPortalOrderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OmsPortalOrderServiceImpl.class);
    private static final String ORDER_IDEMPOTENT_KEY_PREFIX = "mall:portal:order:idempotent:";
    private static final String ORDER_PROCESSING_KEY_PREFIX = "mall:portal:order:processing:";
    private static final long ORDER_IDEMPOTENT_TTL_SECONDS = 86400L;
    private static final long ORDER_PROCESSING_TTL_SECONDS = 300L;
    private static final int MAX_REQUEST_ID_LENGTH = 128;

    @Autowired
    private UmsMemberService memberService;
    @Autowired
    private OmsCartItemService cartItemService;
    @Autowired
    private UmsMemberReceiveAddressService memberReceiveAddressService;
    @Autowired
    private UmsMemberCouponService memberCouponService;
    @Autowired
    private UmsIntegrationConsumeSettingMapper integrationConsumeSettingMapper;
    @Autowired
    private PmsSkuStockMapper skuStockMapper;
    @Autowired
    private SmsCouponHistoryDao couponHistoryDao;
    @Autowired
    private OmsOrderMapper orderMapper;
    @Autowired
    private PortalOrderItemDao orderItemDao;
    @Autowired
    private SmsCouponHistoryMapper couponHistoryMapper;
    @Autowired
    private RedisService redisService;
    @Value("${redis.key.orderId}")
    private String REDIS_KEY_ORDER_ID;
    @Value("${redis.database}")
    private String REDIS_DATABASE;
    @Autowired
    private PortalOrderDao portalOrderDao;
    @Autowired
    private OmsOrderSettingMapper orderSettingMapper;
    @Autowired
    private OmsOrderItemMapper orderItemMapper;
    @Autowired
    private CancelOrderSender cancelOrderSender;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public ConfirmOrderResult generateConfirmOrder(ConfirmOrderParam param) {
        if (param == null || CollectionUtils.isEmpty(param.getCartIds())) {
            Asserts.fail("请选择购物车商品");
        }
        ConfirmOrderResult result = new ConfirmOrderResult();
        UmsMember currentMember = memberService.getCurrentMember();
        List<CartPromotionItem> cartPromotionItemList = cartItemService.listPromotion(currentMember.getId(), param.getCartIds());
        result.setCartPromotionItemList(cartPromotionItemList);
        result.setMemberReceiveAddressList(memberReceiveAddressService.list());
        result.setCouponHistoryDetailList(memberCouponService.listCart(cartPromotionItemList, 1));
        result.setMemberIntegration(currentMember.getIntegration());
        result.setIntegrationConsumeSetting(integrationConsumeSettingMapper.selectByPrimaryKey(1L));
        result.setCalcAmount(calcConfirmAmount(cartPromotionItemList, param.getCouponId(), param.getUseIntegration(), currentMember));
        return result;
    }

    @Override
    public Map<String, Object> generateOrder(OrderParam orderParam, String requestId) {
        UmsMember currentMember = memberService.getCurrentMember();
        if (StrUtil.isBlank(requestId)) {
            return doGenerateOrder(orderParam, currentMember);
        }
        requestId = requestId.trim();
        if (requestId.length() > MAX_REQUEST_ID_LENGTH) {
            Asserts.fail("X-Request-Id 长度不能超过128个字符");
        }
        Map<String, Object> cached = getIdempotentResult(currentMember.getId(), requestId);
        if (cached != null) {
            return cached;
        }
        Map<String, Object> durableResult = getDurableIdempotentResult(currentMember.getId(), requestId);
        if (durableResult != null) {
            return durableResult;
        }
        String processingKey = buildProcessingKey(currentMember.getId(), requestId);
        Boolean acquired = tryAcquireProcessingKey(processingKey);
        if (Boolean.FALSE.equals(acquired)) {
            cached = getIdempotentResult(currentMember.getId(), requestId);
            if (cached != null) {
                return cached;
            }
            durableResult = getDurableIdempotentResult(currentMember.getId(), requestId);
            if (durableResult != null) {
                return durableResult;
            }
            Asserts.fail("订单处理中，请勿重复提交");
        }
        try {
            cached = getIdempotentResult(currentMember.getId(), requestId);
            if (cached != null) {
                scheduleIdempotentResult(currentMember.getId(), requestId, cached, processingKey);
                return cached;
            }
            if (!reserveOrderRequest(currentMember.getId(), requestId)) {
                durableResult = getDurableIdempotentResult(currentMember.getId(), requestId);
                if (durableResult == null) {
                    Asserts.fail("订单处理中，请勿重复提交");
                }
                scheduleIdempotentResult(currentMember.getId(), requestId, durableResult, processingKey);
                return durableResult;
            }
            Map<String, Object> result = doGenerateOrder(orderParam, currentMember);
            OmsOrder createdOrder = (OmsOrder) result.get("order");
            int bound = portalOrderDao.bindOrderRequest(
                    currentMember.getId(), requestId, createdOrder.getId(), new Date());
            if (bound != 1) {
                Asserts.fail("保存订单幂等记录失败");
            }
            scheduleIdempotentResult(currentMember.getId(), requestId, result, processingKey);
            return result;
        } catch (RuntimeException ex) {
            safeDeleteProcessingKey(processingKey);
            throw ex;
        }
    }

    private Map<String, Object> doGenerateOrder(OrderParam orderParam, UmsMember currentMember) {
        //校验收货地址
        if(orderParam.getMemberReceiveAddressId()==null){
            Asserts.fail("请选择收货地址！");
        }
        if (CollectionUtils.isEmpty(orderParam.getCartIds())) {
            Asserts.fail("请选择购物车商品");
        }
        //获取购物车及优惠信息
        List<CartPromotionItem> cartPromotionItemList = cartItemService.listPromotion(currentMember.getId(), orderParam.getCartIds());
        if (CollectionUtils.isEmpty(cartPromotionItemList)) {
            Asserts.fail("购物车商品不存在或已失效");
        }
        List<OmsOrderItem> orderItemList = buildOrderItemsFromCart(cartPromotionItemList);
        //判断购物车中商品是否都有库存
        if (!hasStock(cartPromotionItemList)) {
            Asserts.fail("库存不足，无法下单");
        }
        //判断使用使用了优惠券
        if (orderParam.getCouponId() == null) {
            //不用优惠券
            for (OmsOrderItem orderItem : orderItemList) {
                orderItem.setCouponAmount(new BigDecimal(0));
            }
        } else {
            //使用优惠券
            SmsCouponHistoryDetail couponHistoryDetail = getUseCoupon(cartPromotionItemList, orderParam.getCouponId());
            if (couponHistoryDetail == null) {
                Asserts.fail("该优惠券不可用");
            }
            //对下单商品的优惠券进行处理
            handleCouponAmount(orderItemList, couponHistoryDetail);
        }
        //判断是否使用积分
        if (orderParam.getUseIntegration() == null||orderParam.getUseIntegration().equals(0)) {
            //不使用积分
            for (OmsOrderItem orderItem : orderItemList) {
                orderItem.setIntegrationAmount(new BigDecimal(0));
            }
        } else {
            //使用积分
            BigDecimal totalAmount = calcTotalAmount(orderItemList);
            BigDecimal integrationAmount = getUseIntegrationAmount(orderParam.getUseIntegration(), totalAmount, currentMember, orderParam.getCouponId() != null);
            if (integrationAmount.compareTo(new BigDecimal(0)) == 0) {
                Asserts.fail("积分不可用");
            } else {
                //可用情况下分摊到可用商品中
                for (OmsOrderItem orderItem : orderItemList) {
                    BigDecimal perAmount = orderItem.getProductPrice().divide(totalAmount, 3, RoundingMode.HALF_EVEN).multiply(integrationAmount);
                    orderItem.setIntegrationAmount(perAmount);
                }
            }
        }
        //计算order_item的实付金额
        handleRealAmount(orderItemList);
        //进行库存锁定
        lockStock(cartPromotionItemList);
        //根据商品合计、运费、活动优惠、优惠券、积分计算应付金额
        OmsOrder order = new OmsOrder();
        order.setDiscountAmount(new BigDecimal(0));
        order.setTotalAmount(calcTotalAmount(orderItemList));
        order.setFreightAmount(new BigDecimal(0));
        order.setPromotionAmount(calcPromotionAmount(orderItemList));
        order.setPromotionInfo(getOrderPromotionInfo(orderItemList));
        if (orderParam.getCouponId() == null) {
            order.setCouponAmount(new BigDecimal(0));
        } else {
            order.setCouponId(orderParam.getCouponId());
            order.setCouponAmount(calcCouponAmount(orderItemList));
        }
        if (orderParam.getUseIntegration() == null || orderParam.getUseIntegration().equals(0)) {
            order.setUseIntegration(0);
            order.setIntegrationAmount(new BigDecimal(0));
        } else {
            order.setUseIntegration(orderParam.getUseIntegration());
            order.setIntegrationAmount(calcIntegrationAmount(orderItemList));
        }
        order.setPayAmount(calcPayAmount(order));
        //转化为订单信息并插入数据库
        order.setMemberId(currentMember.getId());
        order.setCreateTime(new Date());
        order.setMemberUsername(currentMember.getUsername());
        //支付方式：0->未支付；1->支付宝；2->微信
        order.setPayType(orderParam.getPayType());
        //订单来源：0->PC订单；1->app订单
        order.setSourceType(1);
        //订单状态：0->待付款；1->待发货；2->已发货；3->已完成；4->已关闭；5->无效订单
        order.setStatus(0);
        //订单类型：0->正常订单；1->秒杀订单
        order.setOrderType(0);
        //收货人信息：姓名、电话、邮编、地址
        UmsMemberReceiveAddress address = memberReceiveAddressService.getItem(orderParam.getMemberReceiveAddressId());
        if (address == null) {
            Asserts.fail("收货地址不存在");
        }
        order.setReceiverName(address.getName());
        order.setReceiverPhone(address.getPhoneNumber());
        order.setReceiverPostCode(address.getPostCode());
        order.setReceiverProvince(address.getProvince());
        order.setReceiverCity(address.getCity());
        order.setReceiverRegion(address.getRegion());
        order.setReceiverDetailAddress(address.getDetailAddress());
        //0->未确认；1->已确认
        order.setConfirmStatus(0);
        order.setDeleteStatus(0);
        //计算赠送积分
        order.setIntegration(calcGifIntegration(orderItemList));
        //计算赠送成长值
        order.setGrowth(calcGiftGrowth(orderItemList));
        //生成订单号
        order.setOrderSn(generateOrderSn(order));
        //设置自动收货天数
        List<OmsOrderSetting> orderSettings = orderSettingMapper.selectByExample(new OmsOrderSettingExample());
        if(CollUtil.isNotEmpty(orderSettings)){
            order.setAutoConfirmDay(orderSettings.get(0).getConfirmOvertime());
        }
        // TODO: 2018/9/3 bill_*,delivery_*
        //插入order表和order_item表
        orderMapper.insert(order);
        for (OmsOrderItem orderItem : orderItemList) {
            orderItem.setOrderId(order.getId());
            orderItem.setOrderSn(order.getOrderSn());
        }
        orderItemDao.insertList(orderItemList);
        //如使用优惠券更新优惠券使用状态
        if (orderParam.getCouponId() != null) {
            updateCouponStatus(orderParam.getCouponId(), currentMember.getId(), 1);
        }
        //如使用积分需要扣除积分
        if (orderParam.getUseIntegration() != null && orderParam.getUseIntegration() > 0) {
            int deducted = portalOrderDao.adjustMemberIntegration(currentMember.getId(), -orderParam.getUseIntegration());
            if (deducted == 0) {
                Asserts.fail("积分不足，无法下单");
            }
            int currentIntegration = currentMember.getIntegration() == null ? 0 : currentMember.getIntegration();
            currentMember.setIntegration(currentIntegration - orderParam.getUseIntegration());
        }
        //删除购物车中的下单商品
        deleteCartItemList(cartPromotionItemList, currentMember);
        //发送延迟消息取消订单
        sendDelayMessageCancelOrder(order.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("orderItemList", orderItemList);
        return result;
    }

    @Override
    public Integer paySuccess(Long orderId, Integer payType) {
        OmsOrderExample example = new OmsOrderExample();
        example.createCriteria()
                .andIdEqualTo(orderId)
                .andStatusEqualTo(0)
                .andDeleteStatusEqualTo(0);
        OmsOrder updateOrder = new OmsOrder();
        updateOrder.setStatus(1);
        updateOrder.setPaymentTime(new Date());
        updateOrder.setPayType(payType);
        int updated = orderMapper.updateByExampleSelective(updateOrder, example);
        if (updated == 0) {
            OmsOrder existing = orderMapper.selectByPrimaryKey(orderId);
            if (existing != null && existing.getStatus() == 1) {
                return 0;
            }
            Asserts.fail("订单状态不可支付");
        }
        OmsOrderDetail orderDetail = portalOrderDao.getDetail(orderId);
        if (orderDetail == null || CollectionUtils.isEmpty(orderDetail.getOrderItemList())) {
            Asserts.fail("订单商品不存在，支付失败");
        }
        List<OmsOrderItem> stockItems = aggregateStockItems(orderDetail.getOrderItemList());
        int stockUpdated;
        // 秒杀订单未锁定 lock_stock，支付时只扣真实库存
        if (Integer.valueOf(1).equals(orderDetail.getOrderType())) {
            stockUpdated = portalOrderDao.decreaseSkuStockOnly(stockItems);
        } else {
            stockUpdated = portalOrderDao.updateSkuStock(stockItems);
        }
        if (stockUpdated != stockItems.size()) {
            Asserts.fail("库存扣减失败，支付已回滚");
        }
        return stockUpdated;
    }

    private List<OmsOrderItem> aggregateStockItems(List<OmsOrderItem> orderItems) {
        Map<Long, Integer> quantityBySku = new LinkedHashMap<>();
        for (OmsOrderItem item : orderItems) {
            if (item.getProductSkuId() == null || item.getProductQuantity() == null || item.getProductQuantity() <= 0) {
                Asserts.fail("订单商品库存信息无效");
            }
            quantityBySku.merge(item.getProductSkuId(), item.getProductQuantity(), Integer::sum);
        }
        List<OmsOrderItem> stockItems = new ArrayList<>();
        quantityBySku.forEach((skuId, quantity) -> {
            OmsOrderItem item = new OmsOrderItem();
            item.setProductSkuId(skuId);
            item.setProductQuantity(quantity);
            stockItems.add(item);
        });
        return stockItems;
    }

    @Override
    public Integer mockPay(Long orderId, Integer payType) {
        UmsMember currentMember = memberService.getCurrentMember();
        OmsOrder order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null || order.getDeleteStatus() == 1) {
            Asserts.fail("订单不存在");
        }
        if (!currentMember.getId().equals(order.getMemberId())) {
            Asserts.fail("无权支付该订单");
        }
        if (order.getStatus() == 1) {
            return 0;
        }
        if (order.getStatus() != 0) {
            Asserts.fail("订单状态不可支付");
        }
        Integer resolvedPayType = payType != null ? payType : order.getPayType();
        return paySuccess(orderId, resolvedPayType);
    }

    @Override
    public Integer cancelTimeOutOrder() {
        Integer count=0;
        OmsOrderSetting orderSetting = orderSettingMapper.selectByPrimaryKey(1L);
        //查询超时、未支付的订单及订单详情
        List<OmsOrderDetail> timeOutOrders = portalOrderDao.getTimeOutOrders(orderSetting.getNormalOrderOvertime());
        if (CollectionUtils.isEmpty(timeOutOrders)) {
            return count;
        }
        for (OmsOrderDetail timeOutOrder : timeOutOrders) {
            // 仅成功从待付款迁移到关闭的调用方才释放资源，避免与延迟取消/用户取消竞态
            int updated = portalOrderDao.cancelPendingOrder(timeOutOrder.getId());
            if (updated == 0) {
                continue;
            }
            count++;
            if (!CollectionUtils.isEmpty(timeOutOrder.getOrderItemList())
                    && !Integer.valueOf(1).equals(timeOutOrder.getOrderType())) {
                portalOrderDao.releaseSkuStockLock(timeOutOrder.getOrderItemList());
            }
            updateCouponStatus(timeOutOrder.getCouponId(), timeOutOrder.getMemberId(), 0);
            if (timeOutOrder.getUseIntegration() != null && timeOutOrder.getUseIntegration() > 0) {
                portalOrderDao.adjustMemberIntegration(timeOutOrder.getMemberId(), timeOutOrder.getUseIntegration());
            }
        }
        return count;
    }

    @Override
    public void cancelOrder(Long orderId) {
        OmsOrder cancelOrder = orderMapper.selectByPrimaryKey(orderId);
        if (cancelOrder == null || cancelOrder.getDeleteStatus() == 1 || cancelOrder.getStatus() != 0) {
            return;
        }
        int updated = portalOrderDao.cancelPendingOrder(orderId);
        if (updated == 0) {
            return;
        }
        OmsOrderItemExample orderItemExample = new OmsOrderItemExample();
        orderItemExample.createCriteria().andOrderIdEqualTo(orderId);
        List<OmsOrderItem> orderItemList = orderItemMapper.selectByExample(orderItemExample);
        // 秒杀订单未锁定 lock_stock，取消时无需释放
        if (!CollectionUtils.isEmpty(orderItemList) && !Integer.valueOf(1).equals(cancelOrder.getOrderType())) {
            portalOrderDao.releaseSkuStockLock(orderItemList);
        }
        updateCouponStatus(cancelOrder.getCouponId(), cancelOrder.getMemberId(), 0);
        if (cancelOrder.getUseIntegration() != null && cancelOrder.getUseIntegration() > 0) {
            portalOrderDao.adjustMemberIntegration(cancelOrder.getMemberId(), cancelOrder.getUseIntegration());
        }
    }

    @Override
    public void cancelUserOrder(Long orderId) {
        UmsMember member = memberService.getCurrentMember();
        OmsOrder order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null || !member.getId().equals(order.getMemberId())) {
            Asserts.fail("无权取消该订单");
        }
        cancelOrder(orderId);
    }

    @Override
    public void sendDelayMessageCancelOrder(Long orderId) {
        //获取订单超时时间
        OmsOrderSetting orderSetting = orderSettingMapper.selectByPrimaryKey(1L);
        long delayTimes = orderSetting.getNormalOrderOvertime() * 60 * 1000;
        //发送延迟消息
        cancelOrderSender.sendMessage(orderId, delayTimes);
    }

    @Override
    public void confirmReceiveOrder(Long orderId) {
        UmsMember member = memberService.getCurrentMember();
        OmsOrder order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null || order.getDeleteStatus() == 1) {
            Asserts.fail("订单不存在");
        }
        if(!member.getId().equals(order.getMemberId())){
            Asserts.fail("不能确认他人订单！");
        }
        if(order.getStatus()!=2){
            Asserts.fail("该订单还未发货！");
        }
        order.setStatus(3);
        order.setConfirmStatus(1);
        order.setReceiveTime(new Date());
        orderMapper.updateByPrimaryKey(order);
    }

    @Override
    public CommonPage<OmsOrderDetail> list(Integer status, Integer pageNum, Integer pageSize) {
        if(status==-1){
            status = null;
        }
        UmsMember member = memberService.getCurrentMember();
        PageHelper.startPage(pageNum,pageSize);
        OmsOrderExample orderExample = new OmsOrderExample();
        OmsOrderExample.Criteria criteria = orderExample.createCriteria();
        criteria.andDeleteStatusEqualTo(0)
                .andMemberIdEqualTo(member.getId());
        if(status!=null){
            criteria.andStatusEqualTo(status);
        }
        orderExample.setOrderByClause("create_time desc");
        List<OmsOrder> orderList = orderMapper.selectByExample(orderExample);
        CommonPage<OmsOrder> orderPage = CommonPage.restPage(orderList);
        //设置分页信息
        CommonPage<OmsOrderDetail> resultPage = new CommonPage<>();
        resultPage.setPageNum(orderPage.getPageNum());
        resultPage.setPageSize(orderPage.getPageSize());
        resultPage.setTotal(orderPage.getTotal());
        resultPage.setTotalPage(orderPage.getTotalPage());
        if(CollUtil.isEmpty(orderList)){
            return resultPage;
        }
        //设置数据信息
        List<Long> orderIds = orderList.stream().map(OmsOrder::getId).collect(Collectors.toList());
        OmsOrderItemExample orderItemExample = new OmsOrderItemExample();
        orderItemExample.createCriteria().andOrderIdIn(orderIds);
        orderItemExample.setOrderByClause("order_id asc, id asc");
        List<OmsOrderItem> orderItemList = orderItemMapper.selectByExample(orderItemExample);
        Map<Long, List<OmsOrderItem>> orderItemMap = orderItemList.stream()
                .collect(Collectors.groupingBy(OmsOrderItem::getOrderId));
        List<OmsOrderDetail> orderDetailList = new ArrayList<>();
        for (OmsOrder omsOrder : orderList) {
            OmsOrderDetail orderDetail = new OmsOrderDetail();
            BeanUtil.copyProperties(omsOrder,orderDetail);
            List<OmsOrderItem> relatedItemList = orderItemMap.getOrDefault(orderDetail.getId(), Collections.emptyList());
            orderDetail.setOrderItemList(relatedItemList);
            orderDetailList.add(orderDetail);
        }
        resultPage.setList(orderDetailList);
        return resultPage;
    }

    @Override
    public OmsOrderDetail detail(Long orderId) {
        UmsMember member = memberService.getCurrentMember();
        OmsOrder omsOrder = orderMapper.selectByPrimaryKey(orderId);
        if (omsOrder == null || omsOrder.getDeleteStatus() == 1) {
            Asserts.fail("订单不存在");
        }
        if (!member.getId().equals(omsOrder.getMemberId())) {
            Asserts.fail("无权查看该订单");
        }
        OmsOrderItemExample example = new OmsOrderItemExample();
        example.createCriteria().andOrderIdEqualTo(orderId);
        List<OmsOrderItem> orderItemList = orderItemMapper.selectByExample(example);
        OmsOrderDetail orderDetail = new OmsOrderDetail();
        BeanUtil.copyProperties(omsOrder,orderDetail);
        orderDetail.setOrderItemList(orderItemList);
        return orderDetail;
    }

    @Override
    public void deleteOrder(Long orderId) {
        UmsMember member = memberService.getCurrentMember();
        OmsOrder order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null || order.getDeleteStatus() == 1) {
            Asserts.fail("订单不存在");
        }
        if(!member.getId().equals(order.getMemberId())){
            Asserts.fail("不能删除他人订单！");
        }
        if(order.getStatus()==3||order.getStatus()==4){
            order.setDeleteStatus(1);
            orderMapper.updateByPrimaryKey(order);
        }else{
            Asserts.fail("只能删除已完成或已关闭的订单！");
        }
    }

    @Override
    public void paySuccessByOrderSn(String orderSn, Integer payType) {
        OmsOrderExample example =  new OmsOrderExample();
        example.createCriteria()
                .andOrderSnEqualTo(orderSn)
                .andStatusEqualTo(0)
                .andDeleteStatusEqualTo(0);
        List<OmsOrder> orderList = orderMapper.selectByExample(example);
        if(CollUtil.isNotEmpty(orderList)){
            OmsOrder order = orderList.get(0);
            paySuccess(order.getId(),payType);
        }
    }

    /**
     * 生成18位订单编号:8位日期+2位平台号码+2位支付方式+6位以上自增id
     */
    private String generateOrderSn(OmsOrder order) {
        StringBuilder sb = new StringBuilder();
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String key = REDIS_DATABASE+":"+ REDIS_KEY_ORDER_ID + date;
        Long increment = redisService.incr(key, 1);
        sb.append(date);
        sb.append(String.format("%02d", order.getSourceType()));
        sb.append(String.format("%02d", order.getPayType()));
        String incrementStr = increment.toString();
        if (incrementStr.length() <= 6) {
            sb.append(String.format("%06d", increment));
        } else {
            sb.append(incrementStr);
        }
        return sb.toString();
    }

    /**
     * 删除下单商品的购物车信息
     */
    private void deleteCartItemList(List<CartPromotionItem> cartPromotionItemList, UmsMember currentMember) {
        List<Long> ids = new ArrayList<>();
        for (CartPromotionItem cartPromotionItem : cartPromotionItemList) {
            ids.add(cartPromotionItem.getId());
        }
        cartItemService.delete(currentMember.getId(), ids);
    }

    /**
     * 计算该订单赠送的成长值
     */
    private Integer calcGiftGrowth(List<OmsOrderItem> orderItemList) {
        Integer sum = 0;
        for (OmsOrderItem orderItem : orderItemList) {
            sum = sum + orderItem.getGiftGrowth() * orderItem.getProductQuantity();
        }
        return sum;
    }

    /**
     * 计算该订单赠送的积分
     */
    private Integer calcGifIntegration(List<OmsOrderItem> orderItemList) {
        int sum = 0;
        for (OmsOrderItem orderItem : orderItemList) {
            sum += orderItem.getGiftIntegration() * orderItem.getProductQuantity();
        }
        return sum;
    }

    /**
     * 将优惠券信息更改为指定状态
     *
     * @param couponId  优惠券id
     * @param memberId  会员id
     * @param useStatus 0->未使用；1->已使用
     */
    private void updateCouponStatus(Long couponId, Long memberId, Integer useStatus) {
        if (couponId == null) return;
        //查询第一张优惠券
        SmsCouponHistoryExample example = new SmsCouponHistoryExample();
        example.createCriteria().andMemberIdEqualTo(memberId)
                .andCouponIdEqualTo(couponId).andUseStatusEqualTo(useStatus == 0 ? 1 : 0);
        List<SmsCouponHistory> couponHistoryList = couponHistoryMapper.selectByExample(example);
        if (!CollectionUtils.isEmpty(couponHistoryList)) {
            SmsCouponHistory couponHistory = couponHistoryList.get(0);
            couponHistory.setUseTime(new Date());
            couponHistory.setUseStatus(useStatus);
            couponHistoryMapper.updateByPrimaryKeySelective(couponHistory);
        }
    }

    private void handleRealAmount(List<OmsOrderItem> orderItemList) {
        for (OmsOrderItem orderItem : orderItemList) {
            //原价-促销优惠-优惠券抵扣-积分抵扣
            BigDecimal realAmount = orderItem.getProductPrice()
                    .subtract(orderItem.getPromotionAmount())
                    .subtract(orderItem.getCouponAmount())
                    .subtract(orderItem.getIntegrationAmount());
            orderItem.setRealAmount(realAmount);
        }
    }

    /**
     * 获取订单促销信息
     */
    private String getOrderPromotionInfo(List<OmsOrderItem> orderItemList) {
        StringBuilder sb = new StringBuilder();
        for (OmsOrderItem orderItem : orderItemList) {
            sb.append(orderItem.getPromotionName());
            sb.append(";");
        }
        String result = sb.toString();
        if (result.endsWith(";")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * 计算订单应付金额
     */
    private BigDecimal calcPayAmount(OmsOrder order) {
        //总金额+运费-促销优惠-优惠券优惠-积分抵扣
        BigDecimal payAmount = order.getTotalAmount()
                .add(order.getFreightAmount())
                .subtract(order.getPromotionAmount())
                .subtract(order.getCouponAmount())
                .subtract(order.getIntegrationAmount());
        return payAmount;
    }

    /**
     * 计算订单优惠券金额
     */
    private BigDecimal calcIntegrationAmount(List<OmsOrderItem> orderItemList) {
        BigDecimal integrationAmount = new BigDecimal(0);
        for (OmsOrderItem orderItem : orderItemList) {
            if (orderItem.getIntegrationAmount() != null) {
                integrationAmount = integrationAmount.add(orderItem.getIntegrationAmount().multiply(new BigDecimal(orderItem.getProductQuantity())));
            }
        }
        return integrationAmount;
    }

    /**
     * 计算订单优惠券金额
     */
    private BigDecimal calcCouponAmount(List<OmsOrderItem> orderItemList) {
        BigDecimal couponAmount = new BigDecimal(0);
        for (OmsOrderItem orderItem : orderItemList) {
            if (orderItem.getCouponAmount() != null) {
                couponAmount = couponAmount.add(orderItem.getCouponAmount().multiply(new BigDecimal(orderItem.getProductQuantity())));
            }
        }
        return couponAmount;
    }

    /**
     * 计算订单活动优惠
     */
    private BigDecimal calcPromotionAmount(List<OmsOrderItem> orderItemList) {
        BigDecimal promotionAmount = new BigDecimal(0);
        for (OmsOrderItem orderItem : orderItemList) {
            if (orderItem.getPromotionAmount() != null) {
                promotionAmount = promotionAmount.add(orderItem.getPromotionAmount().multiply(new BigDecimal(orderItem.getProductQuantity())));
            }
        }
        return promotionAmount;
    }

    /**
     * 获取可用积分抵扣金额
     *
     * @param useIntegration 使用的积分数量
     * @param totalAmount    订单总金额
     * @param currentMember  使用的用户
     * @param hasCoupon      是否已经使用优惠券
     */
    private BigDecimal getUseIntegrationAmount(Integer useIntegration, BigDecimal totalAmount, UmsMember currentMember, boolean hasCoupon) {
        BigDecimal zeroAmount = new BigDecimal(0);
        //判断用户是否有这么多积分
        if (useIntegration.compareTo(currentMember.getIntegration()) > 0) {
            return zeroAmount;
        }
        //根据积分使用规则判断是否可用
        //是否可与优惠券共用
        UmsIntegrationConsumeSetting integrationConsumeSetting = integrationConsumeSettingMapper.selectByPrimaryKey(1L);
        if (hasCoupon && integrationConsumeSetting.getCouponStatus().equals(0)) {
            //不可与优惠券共用
            return zeroAmount;
        }
        //是否达到最低使用积分门槛
        if (useIntegration.compareTo(integrationConsumeSetting.getUseUnit()) < 0) {
            return zeroAmount;
        }
        //是否超过订单抵用最高百分比
        BigDecimal integrationAmount = new BigDecimal(useIntegration).divide(new BigDecimal(integrationConsumeSetting.getUseUnit()), 2, RoundingMode.HALF_EVEN);
        BigDecimal maxPercent = new BigDecimal(integrationConsumeSetting.getMaxPercentPerOrder()).divide(new BigDecimal(100), 2, RoundingMode.HALF_EVEN);
        if (integrationAmount.compareTo(totalAmount.multiply(maxPercent)) > 0) {
            return zeroAmount;
        }
        return integrationAmount;
    }

    /**
     * 对优惠券优惠进行处理
     *
     * @param orderItemList       order_item列表
     * @param couponHistoryDetail 可用优惠券详情
     */
    private void handleCouponAmount(List<OmsOrderItem> orderItemList, SmsCouponHistoryDetail couponHistoryDetail) {
        SmsCoupon coupon = couponHistoryDetail.getCoupon();
        if (coupon.getUseType().equals(0)) {
            //全场通用
            calcPerCouponAmount(orderItemList, coupon);
        } else if (coupon.getUseType().equals(1)) {
            //指定分类
            List<OmsOrderItem> couponOrderItemList = getCouponOrderItemByRelation(couponHistoryDetail, orderItemList, 0);
            calcPerCouponAmount(couponOrderItemList, coupon);
        } else if (coupon.getUseType().equals(2)) {
            //指定商品
            List<OmsOrderItem> couponOrderItemList = getCouponOrderItemByRelation(couponHistoryDetail, orderItemList, 1);
            calcPerCouponAmount(couponOrderItemList, coupon);
        }
    }

    /**
     * 对每个下单商品进行优惠券金额分摊的计算
     *
     * @param orderItemList 可用优惠券的下单商品商品
     */
    private void calcPerCouponAmount(List<OmsOrderItem> orderItemList, SmsCoupon coupon) {
        BigDecimal totalAmount = calcTotalAmount(orderItemList);
        for (OmsOrderItem orderItem : orderItemList) {
            //(商品价格/可用商品总价)*优惠券面额
            BigDecimal couponAmount = orderItem.getProductPrice().divide(totalAmount, 3, RoundingMode.HALF_EVEN).multiply(coupon.getAmount());
            orderItem.setCouponAmount(couponAmount);
        }
    }

    /**
     * 获取与优惠券有关系的下单商品
     *
     * @param couponHistoryDetail 优惠券详情
     * @param orderItemList       下单商品
     * @param type                使用关系类型：0->相关分类；1->指定商品
     */
    private List<OmsOrderItem> getCouponOrderItemByRelation(SmsCouponHistoryDetail couponHistoryDetail, List<OmsOrderItem> orderItemList, int type) {
        List<OmsOrderItem> result = new ArrayList<>();
        if (type == 0) {
            List<Long> categoryIdList = new ArrayList<>();
            for (SmsCouponProductCategoryRelation productCategoryRelation : couponHistoryDetail.getCategoryRelationList()) {
                categoryIdList.add(productCategoryRelation.getProductCategoryId());
            }
            for (OmsOrderItem orderItem : orderItemList) {
                if (categoryIdList.contains(orderItem.getProductCategoryId())) {
                    result.add(orderItem);
                } else {
                    orderItem.setCouponAmount(new BigDecimal(0));
                }
            }
        } else if (type == 1) {
            List<Long> productIdList = new ArrayList<>();
            for (SmsCouponProductRelation productRelation : couponHistoryDetail.getProductRelationList()) {
                productIdList.add(productRelation.getProductId());
            }
            for (OmsOrderItem orderItem : orderItemList) {
                if (productIdList.contains(orderItem.getProductId())) {
                    result.add(orderItem);
                } else {
                    orderItem.setCouponAmount(new BigDecimal(0));
                }
            }
        }
        return result;
    }

    /**
     * 获取该用户可以使用的优惠券
     *
     * @param cartPromotionItemList 购物车优惠列表
     * @param couponId              使用优惠券id
     */
    private SmsCouponHistoryDetail getUseCoupon(List<CartPromotionItem> cartPromotionItemList, Long couponId) {
        List<SmsCouponHistoryDetail> couponHistoryDetailList = memberCouponService.listCart(cartPromotionItemList, 1);
        for (SmsCouponHistoryDetail couponHistoryDetail : couponHistoryDetailList) {
            if (couponHistoryDetail.getCoupon().getId().equals(couponId)) {
                return couponHistoryDetail;
            }
        }
        return null;
    }

    /**
     * 计算总金额
     */
    private BigDecimal calcTotalAmount(List<OmsOrderItem> orderItemList) {
        BigDecimal totalAmount = new BigDecimal("0");
        for (OmsOrderItem item : orderItemList) {
            totalAmount = totalAmount.add(item.getProductPrice().multiply(new BigDecimal(item.getProductQuantity())));
        }
        return totalAmount;
    }

    /**
     * 锁定下单商品的所有库存
     */
    private void lockStock(List<CartPromotionItem> cartPromotionItemList) {
        for (CartPromotionItem cartPromotionItem : cartPromotionItemList) {
            int updated = portalOrderDao.lockSkuStock(cartPromotionItem.getProductSkuId(), cartPromotionItem.getQuantity());
            if (updated == 0) {
                Asserts.fail("库存不足，无法下单");
            }
        }
    }

    /**
     * 判断下单商品是否都有库存
     */
    private boolean hasStock(List<CartPromotionItem> cartPromotionItemList) {
        if (CollectionUtils.isEmpty(cartPromotionItemList)) {
            return false;
        }
        for (CartPromotionItem cartPromotionItem : cartPromotionItemList) {
            if (cartPromotionItem.getRealStock()==null //判断真实库存是否为空
                    ||cartPromotionItem.getRealStock() <= 0 //判断真实库存是否小于0
                    || cartPromotionItem.getRealStock() < cartPromotionItem.getQuantity()) //判断真实库存是否小于下单的数量
            {
                return false;
            }
        }
        return true;
    }

    /**
     * 计算购物车中商品的价格
     */
    private List<OmsOrderItem> buildOrderItemsFromCart(List<CartPromotionItem> cartPromotionItemList) {
        List<OmsOrderItem> orderItemList = new ArrayList<>();
        for (CartPromotionItem cartPromotionItem : cartPromotionItemList) {
            OmsOrderItem orderItem = new OmsOrderItem();
            orderItem.setProductId(cartPromotionItem.getProductId());
            orderItem.setProductName(cartPromotionItem.getProductName());
            orderItem.setProductPic(cartPromotionItem.getProductPic());
            orderItem.setProductAttr(cartPromotionItem.getProductAttr());
            orderItem.setProductBrand(cartPromotionItem.getProductBrand());
            orderItem.setProductSn(cartPromotionItem.getProductSn());
            orderItem.setProductPrice(cartPromotionItem.getPrice());
            orderItem.setProductQuantity(cartPromotionItem.getQuantity());
            orderItem.setProductSkuId(cartPromotionItem.getProductSkuId());
            orderItem.setProductSkuCode(cartPromotionItem.getProductSkuCode());
            orderItem.setProductCategoryId(cartPromotionItem.getProductCategoryId());
            orderItem.setPromotionAmount(cartPromotionItem.getReduceAmount());
            orderItem.setPromotionName(cartPromotionItem.getPromotionMessage());
            orderItem.setGiftIntegration(cartPromotionItem.getIntegration());
            orderItem.setGiftGrowth(cartPromotionItem.getGrowth());
            orderItemList.add(orderItem);
        }
        return orderItemList;
    }

    private ConfirmOrderResult.CalcAmount calcConfirmAmount(List<CartPromotionItem> cartPromotionItemList,
                                                            Long couponId,
                                                            Integer useIntegration,
                                                            UmsMember currentMember) {
        List<OmsOrderItem> orderItemList = buildOrderItemsFromCart(cartPromotionItemList);
        applyCouponPreview(orderItemList, cartPromotionItemList, couponId);
        applyIntegrationPreview(orderItemList, useIntegration, currentMember, couponId != null);
        OmsOrder order = new OmsOrder();
        order.setTotalAmount(calcTotalAmount(orderItemList));
        order.setFreightAmount(new BigDecimal(0));
        order.setPromotionAmount(calcPromotionAmount(orderItemList));
        order.setCouponAmount(couponId == null ? new BigDecimal(0) : calcCouponAmount(orderItemList));
        order.setIntegrationAmount(useIntegration == null || useIntegration.equals(0)
                ? new BigDecimal(0) : calcIntegrationAmount(orderItemList));
        ConfirmOrderResult.CalcAmount calcAmount = new ConfirmOrderResult.CalcAmount();
        calcAmount.setTotalAmount(order.getTotalAmount());
        calcAmount.setFreightAmount(order.getFreightAmount());
        calcAmount.setPromotionAmount(order.getPromotionAmount());
        calcAmount.setCouponAmount(order.getCouponAmount());
        calcAmount.setIntegrationAmount(order.getIntegrationAmount());
        calcAmount.setPayAmount(calcPayAmount(order));
        return calcAmount;
    }

    private void applyCouponPreview(List<OmsOrderItem> orderItemList,
                                    List<CartPromotionItem> cartPromotionItemList,
                                    Long couponId) {
        if (couponId == null) {
            for (OmsOrderItem orderItem : orderItemList) {
                orderItem.setCouponAmount(new BigDecimal(0));
            }
            return;
        }
        SmsCouponHistoryDetail couponHistoryDetail = getUseCoupon(cartPromotionItemList, couponId);
        if (couponHistoryDetail == null) {
            for (OmsOrderItem orderItem : orderItemList) {
                orderItem.setCouponAmount(new BigDecimal(0));
            }
            return;
        }
        handleCouponAmount(orderItemList, couponHistoryDetail);
    }

    private void applyIntegrationPreview(List<OmsOrderItem> orderItemList,
                                         Integer useIntegration,
                                         UmsMember currentMember,
                                         boolean hasCoupon) {
        if (useIntegration == null || useIntegration.equals(0)) {
            for (OmsOrderItem orderItem : orderItemList) {
                orderItem.setIntegrationAmount(new BigDecimal(0));
            }
            return;
        }
        BigDecimal totalAmount = calcTotalAmount(orderItemList);
        BigDecimal integrationAmount = getUseIntegrationAmount(useIntegration, totalAmount, currentMember, hasCoupon);
        if (integrationAmount.compareTo(new BigDecimal(0)) == 0) {
            for (OmsOrderItem orderItem : orderItemList) {
                orderItem.setIntegrationAmount(new BigDecimal(0));
            }
            return;
        }
        for (OmsOrderItem orderItem : orderItemList) {
            BigDecimal perAmount = orderItem.getProductPrice()
                    .divide(totalAmount, 3, RoundingMode.HALF_EVEN)
                    .multiply(integrationAmount);
            orderItem.setIntegrationAmount(perAmount);
        }
    }

    private String buildIdempotentKey(Long memberId, String requestId) {
        return ORDER_IDEMPOTENT_KEY_PREFIX + memberId + ":" + requestId;
    }

    private String buildProcessingKey(Long memberId, String requestId) {
        return ORDER_PROCESSING_KEY_PREFIX + memberId + ":" + requestId;
    }

    private Map<String, Object> getIdempotentResult(Long memberId, String requestId) {
        Object cached;
        try {
            cached = redisService.get(buildIdempotentKey(memberId, requestId));
        } catch (RuntimeException ex) {
            // Redis 仅作为加速层，数据库唯一键仍可保证最终幂等
            LOGGER.warn("read order idempotent cache failed, memberId={}, requestId={}", memberId, requestId, ex);
            return null;
        }
        if (cached == null) {
            return null;
        }
        OrderIdempotentSnapshot snapshot = JSONUtil.toBean(cached.toString(), OrderIdempotentSnapshot.class);
        if (snapshot == null || snapshot.getOrder() == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("order", snapshot.getOrder());
        result.put("orderItemList", snapshot.getOrderItemList());
        return result;
    }

    private Boolean tryAcquireProcessingKey(String processingKey) {
        try {
            return stringRedisTemplate.opsForValue()
                    .setIfAbsent(processingKey, "1", ORDER_PROCESSING_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (RuntimeException ex) {
            LOGGER.warn("acquire order processing key failed, fallback to database idempotency, key={}",
                    processingKey, ex);
            return null;
        }
    }

    private boolean reserveOrderRequest(Long memberId, String requestId) {
        try {
            return portalOrderDao.insertOrderRequest(memberId, requestId, new Date()) == 1;
        } catch (DuplicateKeyException ignored) {
            return false;
        }
    }

    private Map<String, Object> getDurableIdempotentResult(Long memberId, String requestId) {
        Long orderId = portalOrderDao.getOrderIdByRequest(memberId, requestId);
        if (orderId == null) {
            return null;
        }
        OmsOrder order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null || !memberId.equals(order.getMemberId())) {
            return null;
        }
        OmsOrderItemExample example = new OmsOrderItemExample();
        example.createCriteria().andOrderIdEqualTo(orderId);
        List<OmsOrderItem> orderItems = orderItemMapper.selectByExample(example);
        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("orderItemList", orderItems);
        return result;
    }

    private void scheduleIdempotentResult(Long memberId, String requestId, Map<String, Object> result, String processingKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            saveIdempotentResult(memberId, requestId, result);
            safeDeleteProcessingKey(processingKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    saveIdempotentResult(memberId, requestId, result);
                } catch (RuntimeException ex) {
                    // 数据库唯一键已提供最终幂等保障，缓存失败不能把已提交订单伪装成失败
                    LOGGER.warn("cache committed order idempotent result failed, memberId={}, requestId={}",
                            memberId, requestId, ex);
                }
            }

            @Override
            public void afterCompletion(int status) {
                safeDeleteProcessingKey(processingKey);
            }
        });
    }

    private void safeDeleteProcessingKey(String processingKey) {
        try {
            stringRedisTemplate.delete(processingKey);
        } catch (RuntimeException ex) {
            LOGGER.warn("delete order processing key failed, key={}", processingKey, ex);
        }
    }

    private void saveIdempotentResult(Long memberId, String requestId, Map<String, Object> result) {
        OrderIdempotentSnapshot snapshot = new OrderIdempotentSnapshot();
        snapshot.setOrder((OmsOrder) result.get("order"));
        snapshot.setOrderItemList((List<OmsOrderItem>) result.get("orderItemList"));
        redisService.set(buildIdempotentKey(memberId, requestId), JSONUtil.toJsonStr(snapshot), ORDER_IDEMPOTENT_TTL_SECONDS);
    }

}
