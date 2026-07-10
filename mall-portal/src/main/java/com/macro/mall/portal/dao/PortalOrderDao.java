package com.macro.mall.portal.dao;

import com.macro.mall.model.OmsOrderItem;
import com.macro.mall.portal.domain.OmsOrderDetail;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 前台订单管理自定义Dao
 * Created by macro on 2018/9/4.
 */
public interface PortalOrderDao {
    /**
     * 获取订单及下单商品详情
     */
    OmsOrderDetail getDetail(@Param("orderId") Long orderId);

    /**
     * 修改 pms_sku_stock表的锁定库存及真实库存（普通订单支付）
     */
    int updateSkuStock(@Param("itemList") List<OmsOrderItem> orderItemList);

    /**
     * 秒杀订单支付：仅扣减真实库存，不触碰 lock_stock
     */
    int decreaseSkuStockOnly(@Param("itemList") List<OmsOrderItem> orderItemList);

    /**
     * 获取超时订单
     * @param minute 超时时间（分）
     */
    List<OmsOrderDetail> getTimeOutOrders(@Param("minute") Integer minute);

    /**
     * 批量修改订单状态（仅 status=0 的待付款订单）
     */
    int updateOrderStatus(@Param("ids") List<Long> ids,@Param("status") Integer status);

    /**
     * 条件取消单个待付款订单，返回实际更新行数
     */
    int cancelPendingOrder(@Param("orderId") Long orderId);

    /**
     * 解除取消订单的库存锁定
     */
    int releaseSkuStockLock(@Param("itemList") List<OmsOrderItem> orderItemList);

    /**
     * 条件锁定库存：仅当可用库存充足时增加 lock_stock
     */
    int lockSkuStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    /**
     * 原子增减会员积分，不足时返回 0
     */
    int adjustMemberIntegration(@Param("memberId") Long memberId, @Param("delta") Integer delta);

    /**
     * 在数据库事务内占用幂等请求键，唯一约束防止重复下单
     */
    int insertOrderRequest(@Param("memberId") Long memberId,
                           @Param("requestId") String requestId,
                           @Param("createTime") Date createTime);

    /**
     * 获取幂等请求已绑定的订单ID
     */
    Long getOrderIdByRequest(@Param("memberId") Long memberId,
                             @Param("requestId") String requestId);

    /**
     * 将幂等请求与本事务创建的订单绑定
     */
    int bindOrderRequest(@Param("memberId") Long memberId,
                         @Param("requestId") String requestId,
                         @Param("orderId") Long orderId,
                         @Param("updateTime") Date updateTime);

}
