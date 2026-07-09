package com.macro.mall.portal.service.impl;

import cn.hutool.core.util.StrUtil;
import com.macro.mall.common.exception.Asserts;
import com.macro.mall.model.UmsMember;
import com.macro.mall.portal.component.ai.AiChatClientRouter;
import com.macro.mall.portal.config.AiProperties;
import com.macro.mall.portal.domain.AiChatRequest;
import com.macro.mall.portal.domain.AiChatResponse;
import com.macro.mall.portal.domain.AiCustomerChatParam;
import com.macro.mall.portal.domain.AiKnowledgeSnippet;
import com.macro.mall.portal.domain.AiProviderStatus;
import com.macro.mall.portal.service.AiChatClient;
import com.macro.mall.portal.service.AiCustomerService;
import com.macro.mall.portal.service.AiKnowledgeService;
import com.macro.mall.portal.service.UmsMemberService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiCustomerServiceImpl implements AiCustomerService {
    private static final String SYSTEM_PROMPT = """
            你是 mall 电商系统的智能客服。只能根据提供的知识片段回答商品、订单、售后和平台规则问题。
            如果知识片段不足，明确说明无法确认，并提示用户提供商品ID、订单号或联系人工客服。
            不要编造物流单号、退款状态、库存数量或不存在的活动。
            回答要简洁，并引用命中的知识来源标题。
            """;

    private final UmsMemberService memberService;
    private final AiKnowledgeService knowledgeService;
    private final AiChatClientRouter clientRouter;
    private final AiProperties properties;

    public AiCustomerServiceImpl(UmsMemberService memberService,
                                 AiKnowledgeService knowledgeService,
                                 AiChatClientRouter clientRouter,
                                 AiProperties properties) {
        this.memberService = memberService;
        this.knowledgeService = knowledgeService;
        this.clientRouter = clientRouter;
        this.properties = properties;
    }

    @Override
    public AiChatResponse chat(AiCustomerChatParam param) {
        if (param == null || StrUtil.isBlank(param.getQuestion())) {
            Asserts.fail("问题不能为空");
        }
        UmsMember member = memberService.getCurrentMember();
        List<AiKnowledgeSnippet> snippets = knowledgeService.retrieve(param, member.getId());
        AiChatRequest request = new AiChatRequest();
        request.setQuestion(param.getQuestion());
        request.setSystemPrompt(SYSTEM_PROMPT);
        request.setContext(toContext(snippets));
        AiChatResponse response = clientRouter.chat(request);
        response.setContexts(snippets);
        if (!Boolean.TRUE.equals(response.getEnabled()) && StrUtil.isNotBlank(request.getContext())) {
            response.setAnswer(response.getAnswer() + "\n\n已召回的知识片段：\n" + request.getContext());
        }
        return response;
    }

    @Override
    public AiProviderStatus status() {
        AiChatClient client = clientRouter.selectedClient();
        AiProviderStatus status = new AiProviderStatus();
        status.setEnabled(properties.getEnabled());
        status.setProvider(client.provider());
        status.setModel(properties.getModel());
        status.setBaseUrl(properties.getBaseUrl());
        status.setApiKeyConfigured(StrUtil.isNotBlank(properties.getApiKey()) || "mock".equals(client.provider()) || "ollama".equals(client.provider()));
        status.setReady(Boolean.TRUE.equals(properties.getEnabled()) && client.configured());
        status.setMessage(status.getReady() ? "AI 客服已可用" : "AI 客服未启用或 provider 未配置");
        return status;
    }

    private String toContext(List<AiKnowledgeSnippet> snippets) {
        if (snippets == null || snippets.isEmpty()) {
            return "未召回到相关知识片段。";
        }
        return snippets.stream()
                .map(snippet -> "[" + snippet.getSourceType() + ":" + snippet.getSourceId() + "] "
                        + snippet.getTitle() + "\n" + snippet.getContent())
                .collect(Collectors.joining("\n\n"));
    }
}
