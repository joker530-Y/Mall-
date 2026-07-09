package com.macro.mall.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode
public class SeckillOrderLogItem {
    private Long id;
    private String requestId;
    private Long memberId;
    private Long relationId;
    private Long orderId;
    private String orderSn;
    private Integer status;
    private String statusText;
    private String failReason;
    private Date createTime;
    private Date updateTime;
}
