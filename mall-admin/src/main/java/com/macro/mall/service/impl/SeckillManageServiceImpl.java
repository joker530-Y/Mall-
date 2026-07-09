package com.macro.mall.service.impl;

import com.macro.mall.common.exception.Asserts;
import com.macro.mall.dto.SeckillManageSummary;
import com.macro.mall.dto.SeckillManageWarmupResult;
import com.macro.mall.dto.SeckillOrderLogItem;
import com.macro.mall.mapper.SmsFlashPromotionProductRelationMapper;
import com.macro.mall.model.SmsFlashPromotionProductRelation;
import com.macro.mall.service.SeckillManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class SeckillManageServiceImpl implements SeckillManageService {
    @Autowired
    private SmsFlashPromotionProductRelationMapper relationMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public SeckillManageWarmupResult warmup(Long relationId) {
        SmsFlashPromotionProductRelation relation = getRelation(relationId);
        Integer stock = relation.getFlashPromotionCount() == null ? 0 : relation.getFlashPromotionCount();
        Integer limit = getLimit(relation);
        String stockKey = stockKey(relationId);
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
        stringRedisTemplate.opsForValue().set(limitConfigKey(relationId), String.valueOf(limit));

        SeckillManageWarmupResult result = new SeckillManageWarmupResult();
        result.setRelationId(relationId);
        result.setStock(stock);
        result.setLimit(limit);
        result.setStockKey(stockKey);
        result.setWarmedAt(LocalDateTime.now().toString());
        return result;
    }

    @Override
    public SeckillManageSummary summary(Long relationId) {
        SmsFlashPromotionProductRelation relation = getRelation(relationId);
        int successCount = countByStatus(relationId, 1);
        int processingCount = countByStatus(relationId, 0);
        int failedCount = countByStatus(relationId, 2);
        int dbRemainingStock = relation.getFlashPromotionCount() == null ? 0 : relation.getFlashPromotionCount();
        int currentCapacity = dbRemainingStock + successCount;

        SeckillManageSummary summary = new SeckillManageSummary();
        summary.setRelationId(relationId);
        summary.setProductId(relation.getProductId());
        summary.setFlashPromotionId(relation.getFlashPromotionId());
        summary.setFlashPromotionSessionId(relation.getFlashPromotionSessionId());
        summary.setDbRemainingStock(dbRemainingStock);
        summary.setRedisStock(parseInteger(stringRedisTemplate.opsForValue().get(stockKey(relationId))));
        summary.setLimit(parseInteger(stringRedisTemplate.opsForValue().get(limitConfigKey(relationId))));
        summary.setProcessingCount(processingCount);
        summary.setSuccessCount(successCount);
        summary.setFailedCount(failedCount);
        summary.setTotalRequestCount(processingCount + successCount + failedCount);
        summary.setDuplicateMemberCount(countDuplicateMembers(relationId));
        summary.setOversoldCount(Math.max(successCount - currentCapacity, 0));
        summary.setStockKey(stockKey(relationId));
        summary.setRefreshedAt(LocalDateTime.now().toString());
        return summary;
    }

    @Override
    public List<SeckillOrderLogItem> listOrderLogs(Long relationId, Integer pageSize, Integer pageNum) {
        int size = normalizePageSize(pageSize);
        int offset = (normalizePageNum(pageNum) - 1) * size;
        try {
            return jdbcTemplate.query("""
                    SELECT l.id, l.request_id, l.member_id, l.relation_id, l.order_id, o.order_sn,
                           l.status, l.fail_reason, l.create_time, l.update_time
                    FROM sms_seckill_order_log l
                    LEFT JOIN oms_order o ON o.id = l.order_id
                    WHERE l.relation_id = ?
                    ORDER BY l.create_time DESC, l.id DESC
                    LIMIT ? OFFSET ?
                    """, (rs, rowNum) -> mapOrderLog(rs), relationId, size, offset);
        } catch (DataAccessException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public long countOrderLogs(Long relationId) {
        try {
            Long value = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sms_seckill_order_log WHERE relation_id = ?",
                    Long.class,
                    relationId
            );
            return value == null ? 0L : value;
        } catch (DataAccessException e) {
            return 0L;
        }
    }

    private SmsFlashPromotionProductRelation getRelation(Long relationId) {
        if (relationId == null) {
            Asserts.fail("relationId is required");
        }
        SmsFlashPromotionProductRelation relation = relationMapper.selectByPrimaryKey(relationId);
        if (relation == null) {
            Asserts.fail("seckill relation does not exist");
        }
        return relation;
    }

    private Integer getLimit(SmsFlashPromotionProductRelation relation) {
        return relation.getFlashPromotionLimit() == null || relation.getFlashPromotionLimit() <= 0
                ? 1
                : relation.getFlashPromotionLimit();
    }

    private int countByStatus(Long relationId, int status) {
        try {
            Integer value = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sms_seckill_order_log WHERE relation_id = ? AND status = ?",
                    Integer.class,
                    relationId,
                    status
            );
            return value == null ? 0 : value;
        } catch (DataAccessException e) {
            return 0;
        }
    }

    private int countDuplicateMembers(Long relationId) {
        try {
            Integer value = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM (
                        SELECT member_id
                        FROM sms_seckill_order_log
                        WHERE relation_id = ? AND status = 1
                        GROUP BY member_id
                        HAVING COUNT(*) > 1
                    ) duplicate_members
                    """, Integer.class, relationId);
            return value == null ? 0 : value;
        } catch (DataAccessException e) {
            return 0;
        }
    }

    private SeckillOrderLogItem mapOrderLog(ResultSet rs) throws SQLException {
        SeckillOrderLogItem item = new SeckillOrderLogItem();
        item.setId(rs.getLong("id"));
        item.setRequestId(rs.getString("request_id"));
        item.setMemberId(rs.getLong("member_id"));
        item.setRelationId(rs.getLong("relation_id"));
        item.setOrderId(rs.getObject("order_id") == null ? null : rs.getLong("order_id"));
        item.setOrderSn(rs.getString("order_sn"));
        item.setStatus(rs.getInt("status"));
        item.setStatusText(statusText(item.getStatus()));
        item.setFailReason(rs.getString("fail_reason"));
        item.setCreateTime(rs.getTimestamp("create_time"));
        item.setUpdateTime(rs.getTimestamp("update_time"));
        return item;
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status) {
            case 0 -> "PROCESSING";
            case 1 -> "SUCCESS";
            case 2 -> "FAILED";
            default -> "UNKNOWN";
        };
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    private String stockKey(Long relationId) {
        return "mall:seckill:stock:" + relationId;
    }

    private String limitConfigKey(Long relationId) {
        return "mall:seckill:limit-config:" + relationId;
    }
}
