package com.macro.mall.portal.component.ai;

import cn.hutool.core.util.StrUtil;
import com.macro.mall.portal.config.AiProperties;
import com.macro.mall.portal.domain.AiChatRequest;
import com.macro.mall.portal.domain.AiChatResponse;
import com.macro.mall.portal.service.AiChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AiChatClientRouter {
    private final AiProperties properties;
    private final Map<String, AiChatClient> clientMap;

    public AiChatClientRouter(AiProperties properties, List<AiChatClient> clients) {
        this.properties = properties;
        this.clientMap = clients.stream().collect(Collectors.toMap(AiChatClient::provider, Function.identity()));
    }

    public AiChatResponse chat(AiChatRequest request) {
        AiChatClient client = selectedClient();
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            return disabledResponse(client, "AI is disabled. Set mall.ai.enabled=true to enable customer service chat.");
        }
        if (!client.configured()) {
            return disabledResponse(client, "AI provider is not configured. Use provider=mock for local demo, set API key for cloud provider, or run Ollama locally.");
        }
        return client.chat(request);
    }

    public AiChatClient selectedClient() {
        String provider = StrUtil.blankToDefault(properties.getProvider(), "mock").toLowerCase();
        return clientMap.getOrDefault(provider, clientMap.get("mock"));
    }

    private AiChatResponse disabledResponse(AiChatClient client, String message) {
        AiChatResponse response = new AiChatResponse();
        response.setProvider(client.provider());
        response.setModel(properties.getModel());
        response.setEnabled(false);
        response.setDegraded(true);
        response.setAnswer(message);
        return response;
    }
}
