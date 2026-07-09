package com.macro.mall.portal.component.ai;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.macro.mall.portal.config.AiProperties;
import com.macro.mall.portal.domain.AiChatRequest;
import com.macro.mall.portal.domain.AiChatResponse;
import com.macro.mall.portal.service.AiChatClient;
import org.springframework.http.HttpHeaders;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public abstract class AbstractOpenAiCompatibleChatClient implements AiChatClient {
    protected final AiProperties properties;
    protected final ObjectMapper objectMapper;

    protected AbstractOpenAiCompatibleChatClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean configured() {
        return StrUtil.isNotBlank(properties.getApiKey());
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        if (!configured()) {
            return disabledResponse("AI provider is not configured. Set mall.ai.api-key or AI_API_KEY.");
        }
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model(),
                    "messages", List.of(
                            Map.of("role", "system", "content", request.getSystemPrompt()),
                            Map.of("role", "user", "content", request.getContext() + "\n\n用户问题：" + request.getQuestion())
                    ),
                    "temperature", 0.2,
                    "max_tokens", properties.getMaxTokens()
            ));
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + "/chat/completions"))
                    .timeout(Duration.ofMillis(properties.getTimeoutMillis()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> httpResponse = httpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                return disabledResponse("AI provider returned HTTP " + httpResponse.statusCode());
            }
            JsonNode root = objectMapper.readTree(httpResponse.body());
            String content = root.path("choices").path(0).path("message").path("content").asText();
            AiChatResponse response = baseResponse(false);
            response.setAnswer(StrUtil.blankToDefault(content, "模型未返回有效回答。"));
            return response;
        } catch (Exception e) {
            return disabledResponse("AI provider call failed: " + e.getMessage());
        }
    }

    protected AiChatResponse baseResponse(boolean degraded) {
        AiChatResponse response = new AiChatResponse();
        response.setProvider(provider());
        response.setModel(model());
        response.setEnabled(true);
        response.setDegraded(degraded);
        return response;
    }

    protected AiChatResponse disabledResponse(String message) {
        AiChatResponse response = baseResponse(true);
        response.setEnabled(false);
        response.setAnswer(message);
        return response;
    }

    protected HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getTimeoutMillis()))
                .build();
    }

    protected String baseUrl() {
        return StrUtil.removeSuffix(StrUtil.blankToDefault(properties.getBaseUrl(), defaultBaseUrl()), "/");
    }

    protected String model() {
        return StrUtil.blankToDefault(properties.getModel(), defaultModel());
    }

    protected abstract String defaultBaseUrl();

    protected abstract String defaultModel();
}
