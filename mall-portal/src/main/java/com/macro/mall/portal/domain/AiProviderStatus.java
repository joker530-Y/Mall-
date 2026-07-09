package com.macro.mall.portal.domain;

import lombok.Data;

@Data
public class AiProviderStatus {
    private Boolean enabled;
    private String provider;
    private String model;
    private String baseUrl;
    private Boolean apiKeyConfigured;
    private Boolean ready;
    private String message;
}
