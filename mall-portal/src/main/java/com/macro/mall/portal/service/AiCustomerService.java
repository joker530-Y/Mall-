package com.macro.mall.portal.service;

import com.macro.mall.portal.domain.AiChatResponse;
import com.macro.mall.portal.domain.AiCustomerChatParam;
import com.macro.mall.portal.domain.AiProviderStatus;

public interface AiCustomerService {
    AiChatResponse chat(AiCustomerChatParam param);

    AiProviderStatus status();
}
