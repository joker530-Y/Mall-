package com.macro.mall.portal.component.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.macro.mall.portal.config.AiProperties;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekChatClient extends AbstractOpenAiCompatibleChatClient {
    public DeepSeekChatClient(AiProperties properties, ObjectMapper objectMapper) {
        super(properties, objectMapper);
    }

    @Override
    public String provider() {
        return "deepseek";
    }

    @Override
    protected String defaultBaseUrl() {
        return "https://api.deepseek.com/v1";
    }

    @Override
    protected String defaultModel() {
        return "deepseek-chat";
    }
}
