package com.macro.mall.portal.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiKnowledgeSnippet {
    private String sourceType;
    private String sourceId;
    private String title;
    private String content;
    private Double score;
}
