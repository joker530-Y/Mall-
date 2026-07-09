package com.macro.mall.portal.domain;

import lombok.Data;

@Data
public class AiChatRequest {
    private String question;
    private String systemPrompt;
    private String context;
}
