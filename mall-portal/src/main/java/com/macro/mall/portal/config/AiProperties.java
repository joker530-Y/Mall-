package com.macro.mall.portal.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mall.ai")
public class AiProperties {
    private Boolean enabled = true;
    private String provider = "mock";
    private String apiKey;
    private String baseUrl;
    private String model;
    private Integer timeoutMillis = 5000;
    private Integer maxContextCount = 6;
    private Integer maxTokens = 800;
}
