package com.macro.mall.portal.component.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.macro.mall.portal.config.AiProperties;
import org.springframework.stereotype.Component;

@Component
public class QwenChatClient extends AbstractOpenAiCompatibleChatClient {
    public QwenChatClient(AiProperties properties, ObjectMapper objectMapper) {
        super(properties, objectMapper);
    }

    @Override
    public String provider() {
        return "qwen";
    }

    @Override
    protected String defaultBaseUrl() {
        return "https://dashscope.aliyuncs.com/compatible-mode/v1";
    }

    @Override
    protected String defaultModel() {
        return "qwen-turbo";
    }
}
