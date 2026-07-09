package com.macro.mall.portal.service;

import com.macro.mall.portal.domain.AiChatRequest;
import com.macro.mall.portal.domain.AiChatResponse;

public interface AiChatClient {
    String provider();

    boolean configured();

    AiChatResponse chat(AiChatRequest request);
}
