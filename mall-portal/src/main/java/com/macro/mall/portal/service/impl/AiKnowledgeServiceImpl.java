package com.macro.mall.portal.service.impl;

import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.macro.mall.mapper.CmsHelpMapper;
import com.macro.mall.mapper.OmsOrderItemMapper;
import com.macro.mall.mapper.OmsOrderMapper;
import com.macro.mall.mapper.PmsProductMapper;
import com.macro.mall.model.CmsHelp;
import com.macro.mall.model.CmsHelpExample;
import com.macro.mall.model.OmsOrder;
import com.macro.mall.model.OmsOrderExample;
import com.macro.mall.model.OmsOrderItem;
import com.macro.mall.model.OmsOrderItemExample;
import com.macro.mall.model.PmsProduct;
import com.macro.mall.model.PmsProductExample;
import com.macro.mall.portal.config.AiProperties;
import com.macro.mall.portal.domain.AiCustomerChatParam;
import com.macro.mall.portal.domain.AiKnowledgeSnippet;
import com.macro.mall.portal.service.AiKnowledgeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AiKnowledgeServiceImpl implements AiKnowledgeService {
    private static final Pattern ORDER_SN_PATTERN = Pattern.compile("[A-Za-z0-9]{12,32}");

    private final PmsProductMapper productMapper;
    private final OmsOrderMapper orderMapper;
    private final OmsOrderItemMapper orderItemMapper;
    private final CmsHelpMapper helpMapper;
    private final AiProperties properties;
    private final Resource afterSalesRules;

    public AiKnowledgeServiceImpl(PmsProductMapper productMapper,
                                  OmsOrderMapper orderMapper,
                                  OmsOrderItemMapper orderItemMapper,
                                  CmsHelpMapper helpMapper,
                                  AiProperties properties,
                                  @Value("classpath:ai/after-sales-rules.md") Resource afterSalesRules) {
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.helpMapper = helpMapper;
        this.properties = properties;
        this.afterSalesRules = afterSalesRules;
    }

    @Override
    public List<AiKnowledgeSnippet> retrieve(AiCustomerChatParam param, Long memberId) {
        int max = param.getMaxContextCount() == null ? properties.getMaxContextCount() : param.getMaxContextCount();
        List<AiKnowledgeSnippet> snippets = new ArrayList<>();
        snippets.addAll(retrieveProduct(param));
        snippets.addAll(retrieveOrder(param, memberId));
        snippets.addAll(retrieveHelp(param.getQuestion()));
        snippets.add(afterSalesSnippet(param.getQuestion()));
        return snippets.stream()
                .filter(snippet -> StrUtil.isNotBlank(snippet.getContent()))
                .sorted(Comparator.comparing(AiKnowledgeSnippet::getScore).reversed())
                .limit(Math.max(1, max))
                .collect(Collectors.toList());
    }

    private List<AiKnowledgeSnippet> retrieveProduct(AiCustomerChatParam param) {
        List<PmsProduct> products = new ArrayList<>();
        if (param.getProductId() != null) {
            PmsProduct product = productMapper.selectByPrimaryKey(param.getProductId());
            if (product != null) {
                products.add(product);
            }
        }
        String keyword = keyword(param.getQuestion());
        if (StrUtil.isNotBlank(keyword)) {
            PmsProductExample example = new PmsProductExample();
            example.createCriteria()
                    .andDeleteStatusEqualTo(0)
                    .andPublishStatusEqualTo(1)
                    .andNameLike("%" + keyword + "%");
            PmsProductExample.Criteria keywordCriteria = example.createCriteria()
                    .andDeleteStatusEqualTo(0)
                    .andPublishStatusEqualTo(1)
                    .andKeywordsLike("%" + keyword + "%");
            example.or(keywordCriteria);
            PageHelper.startPage(1, 3);
            products.addAll(productMapper.selectByExample(example));
        }
        Map<Long, PmsProduct> productMap = products.stream()
                .filter(product -> product.getId() != null)
                .collect(Collectors.toMap(PmsProduct::getId, product -> product, (left, right) -> left, LinkedHashMap::new));
        return productMap.values().stream()
                .map(product -> new AiKnowledgeSnippet(
                        "PRODUCT",
                        String.valueOf(product.getId()),
                        "商品：" + product.getName(),
                        productContent(product),
                        param.getProductId() != null && param.getProductId().equals(product.getId()) ? 1.0 : 0.75
                ))
                .collect(Collectors.toList());
    }

    private List<AiKnowledgeSnippet> retrieveOrder(AiCustomerChatParam param, Long memberId) {
        String orderSn = StrUtil.blankToDefault(param.getOrderSn(), findOrderSn(param.getQuestion()));
        OmsOrderExample example = new OmsOrderExample();
        OmsOrderExample.Criteria criteria = example.createCriteria()
                .andMemberIdEqualTo(memberId)
                .andDeleteStatusEqualTo(0);
        if (StrUtil.isNotBlank(orderSn)) {
            criteria.andOrderSnEqualTo(orderSn);
        }
        example.setOrderByClause("create_time desc");
        PageHelper.startPage(1, StrUtil.isNotBlank(orderSn) ? 1 : 2);
        List<OmsOrder> orders = orderMapper.selectByExample(example);
        return orders.stream()
                .map(order -> new AiKnowledgeSnippet(
                        "ORDER",
                        order.getOrderSn(),
                        "订单：" + order.getOrderSn(),
                        orderContent(order),
                        StrUtil.isNotBlank(orderSn) ? 0.95 : 0.55
                ))
                .collect(Collectors.toList());
    }

    private List<AiKnowledgeSnippet> retrieveHelp(String question) {
        CmsHelpExample example = new CmsHelpExample();
        example.createCriteria().andShowStatusEqualTo(1);
        example.setOrderByClause("read_count desc");
        PageHelper.startPage(1, 5);
        List<CmsHelp> helps = helpMapper.selectByExampleWithBLOBs(example);
        String lowerQuestion = StrUtil.blankToDefault(question, "").toLowerCase(Locale.ROOT);
        return helps.stream()
                .map(help -> {
                    String text = StrUtil.blankToDefault(help.getTitle(), "") + " " + StrUtil.blankToDefault(help.getContent(), "");
                    double score = containsAny(lowerQuestion, text) ? 0.8 : 0.35;
                    return new AiKnowledgeSnippet("FAQ", String.valueOf(help.getId()), "FAQ：" + help.getTitle(), clean(text), score);
                })
                .collect(Collectors.toList());
    }

    private AiKnowledgeSnippet afterSalesSnippet(String question) {
        String content;
        try {
            content = StreamUtils.copyToString(afterSalesRules.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            content = "售后规则暂未加载，请联系人工客服确认。";
        }
        double score = containsAny(StrUtil.blankToDefault(question, ""), "退款 退货 售后 发票 物流 运费") ? 0.85 : 0.4;
        return new AiKnowledgeSnippet("POLICY", "after-sales", "售后与平台规则", content, score);
    }

    private String productContent(PmsProduct product) {
        return "商品ID：" + product.getId()
                + "\n名称：" + product.getName()
                + "\n品牌：" + StrUtil.blankToDefault(product.getBrandName(), "未配置")
                + "\n分类：" + StrUtil.blankToDefault(product.getProductCategoryName(), "未配置")
                + "\n价格：" + product.getPrice()
                + "\n促销价：" + product.getPromotionPrice()
                + "\n库存：" + product.getStock()
                + "\n副标题：" + StrUtil.blankToDefault(product.getSubTitle(), "")
                + "\n描述：" + clean(StrUtil.blankToDefault(product.getDescription(), product.getDetailDesc()));
    }

    private String orderContent(OmsOrder order) {
        List<OmsOrderItem> items = orderItems(order.getId());
        String itemText = items.stream()
                .map(item -> item.getProductName() + " x " + item.getProductQuantity() + "，SKU：" + item.getProductSkuCode())
                .collect(Collectors.joining("；"));
        return "订单号：" + order.getOrderSn()
                + "\n订单状态：" + statusName(order.getStatus())
                + "\n订单类型：" + (Integer.valueOf(1).equals(order.getOrderType()) ? "秒杀订单" : "普通订单")
                + "\n下单时间：" + order.getCreateTime()
                + "\n应付金额：" + order.getPayAmount()
                + "\n配送公司：" + StrUtil.blankToDefault(order.getDeliveryCompany(), "暂未发货")
                + "\n物流单号：" + StrUtil.blankToDefault(order.getDeliverySn(), "暂未生成")
                + "\n商品：" + itemText;
    }

    private List<OmsOrderItem> orderItems(Long orderId) {
        OmsOrderItemExample example = new OmsOrderItemExample();
        example.createCriteria().andOrderIdEqualTo(orderId);
        return orderItemMapper.selectByExample(example);
    }

    private String keyword(String question) {
        String clean = StrUtil.blankToDefault(question, "")
                .replaceAll("[^0-9A-Za-z\\u4e00-\\u9fa5]", " ")
                .trim();
        if (StrUtil.isBlank(clean)) {
            return "";
        }
        String[] words = clean.split("\\s+");
        String best = "";
        for (String word : words) {
            if (word.length() > best.length() && word.length() <= 20 && !word.matches("\\d+")) {
                best = word;
            }
        }
        return best;
    }

    private String findOrderSn(String question) {
        Matcher matcher = ORDER_SN_PATTERN.matcher(StrUtil.blankToDefault(question, ""));
        return matcher.find() ? matcher.group() : "";
    }

    private boolean containsAny(String question, String text) {
        String lowerQuestion = StrUtil.blankToDefault(question, "").toLowerCase(Locale.ROOT);
        String lowerText = StrUtil.blankToDefault(text, "").toLowerCase(Locale.ROOT);
        for (String word : lowerQuestion.split("\\s+")) {
            if (StrUtil.isNotBlank(word) && lowerText.contains(word)) {
                return true;
            }
        }
        for (String word : lowerText.split("\\s+")) {
            if (StrUtil.isNotBlank(word) && lowerQuestion.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private String clean(String value) {
        return StrUtil.blankToDefault(value, "")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String statusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待付款";
            case 1 -> "待发货";
            case 2 -> "已发货";
            case 3 -> "已完成";
            case 4 -> "已关闭";
            case 5 -> "无效订单";
            default -> "未知";
        };
    }
}
