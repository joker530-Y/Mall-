package com.macro.mall.portal.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiChatResponse {
    private String answer;
    private String provider;
    private String model;
    private Boolean enabled;
    private Boolean degraded;
    private List<AiKnowledgeSnippet> contexts = new ArrayList<>();
}
