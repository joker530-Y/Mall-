package com.macro.mall.portal.component.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.macro.mall.portal.config.AiProperties;
import org.springframework.stereotype.Component;

@Component
public class OpenAiChatClient extends AbstractOpenAiCompatibleChatClient {
    public OpenAiChatClient(AiProperties properties, ObjectMapper objectMapper) {
        super(properties, objectMapper);
    }

    @Override
    public String provider() {
        return "openai";
    }

    @Override
    protected String defaultBaseUrl() {
        return "https://api.openai.com/v1";
    }

    @Override
    protected String defaultModel() {
        return "gpt-4.1-mini";
    }
}
