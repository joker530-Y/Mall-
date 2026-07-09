package com.macro.mall.portal.component.ai;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.macro.mall.portal.config.AiProperties;
import com.macro.mall.portal.domain.AiChatRequest;
import com.macro.mall.portal.domain.AiChatResponse;
import com.macro.mall.portal.service.AiChatClient;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OllamaChatClient implements AiChatClient {
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    public OllamaChatClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String provider() {
        return "ollama";
    }

    @Override
    public boolean configured() {
        return true;
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        AiChatResponse response = new AiChatResponse();
        response.setProvider(provider());
        response.setModel(model());
        response.setEnabled(true);
        response.setDegraded(false);
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model(),
                    "stream", false,
                    "messages", List.of(
                            Map.of("role", "system", "content", request.getSystemPrompt()),
                            Map.of("role", "user", "content", request.getContext() + "\n\n用户问题：" + request.getQuestion())
                    )
            ));
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + "/api/chat"))
                    .timeout(Duration.ofMillis(properties.getTimeoutMillis()))
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> httpResponse = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(properties.getTimeoutMillis()))
                    .build()
                    .send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                return disabledResponse("Ollama returned HTTP " + httpResponse.statusCode());
            }
            JsonNode root = objectMapper.readTree(httpResponse.body());
            response.setAnswer(StrUtil.blankToDefault(root.path("message").path("content").asText(), "Ollama 未返回有效回答。"));
            return response;
        } catch (Exception e) {
            return disabledResponse("Ollama call failed: " + e.getMessage());
        }
    }

    private AiChatResponse disabledResponse(String message) {
        AiChatResponse response = new AiChatResponse();
        response.setProvider(provider());
        response.setModel(model());
        response.setEnabled(false);
        response.setDegraded(true);
        response.setAnswer(message);
        return response;
    }

    private String baseUrl() {
        return StrUtil.removeSuffix(StrUtil.blankToDefault(properties.getBaseUrl(), "http://localhost:11434"), "/");
    }

    private String model() {
        return StrUtil.blankToDefault(properties.getModel(), "qwen2.5:7b");
    }
}
