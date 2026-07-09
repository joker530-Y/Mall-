package com.macro.mall.portal.service;

import com.macro.mall.portal.domain.AiCustomerChatParam;
import com.macro.mall.portal.domain.AiKnowledgeSnippet;

import java.util.List;

public interface AiKnowledgeService {
    List<AiKnowledgeSnippet> retrieve(AiCustomerChatParam param, Long memberId);
}
