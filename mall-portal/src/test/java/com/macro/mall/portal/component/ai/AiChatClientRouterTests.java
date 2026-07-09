package com.macro.mall.portal.component.ai;

import com.macro.mall.portal.config.AiProperties;
import com.macro.mall.portal.domain.AiChatRequest;
import com.macro.mall.portal.domain.AiChatResponse;
import com.macro.mall.portal.service.AiChatClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiChatClientRouterTests {
    @Test
    void shouldUseMockProviderByDefault() {
        AiProperties properties = new AiProperties();
        AiChatClientRouter router = new AiChatClientRouter(properties, List.of(new StubClient("mock", true)));

        AiChatResponse response = router.chat(request());

        assertTrue(response.getEnabled());
        assertFalse(response.getDegraded());
        assertEquals("mock", response.getProvider());
        assertEquals("ok", response.getAnswer());
    }

    @Test
    void shouldReturnDegradedResponseWhenCloudProviderHasNoKey() {
        AiProperties properties = new AiProperties();
        properties.setProvider("openai");
        AiChatClientRouter router = new AiChatClientRouter(properties, List.of(
                new StubClient("mock", true),
                new StubClient("openai", false)
        ));

        AiChatResponse response = router.chat(request());

        assertFalse(response.getEnabled());
        assertTrue(response.getDegraded());
        assertEquals("openai", response.getProvider());
        assertTrue(response.getAnswer().contains("not configured"));
    }

    @Test
    void shouldReturnDegradedResponseWhenAiDisabled() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(false);
        AiChatClientRouter router = new AiChatClientRouter(properties, List.of(new StubClient("mock", true)));

        AiChatResponse response = router.chat(request());

        assertFalse(response.getEnabled());
        assertTrue(response.getDegraded());
        assertEquals("mock", response.getProvider());
    }

    private AiChatRequest request() {
        AiChatRequest request = new AiChatRequest();
        request.setQuestion("退款多久到账");
        request.setSystemPrompt("system");
        request.setContext("context");
        return request;
    }

    private static class StubClient implements AiChatClient {
        private final String provider;
        private final boolean configured;

        private StubClient(String provider, boolean configured) {
            this.provider = provider;
            this.configured = configured;
        }

        @Override
        public String provider() {
            return provider;
        }

        @Override
        public boolean configured() {
            return configured;
        }

        @Override
        public AiChatResponse chat(AiChatRequest request) {
            AiChatResponse response = new AiChatResponse();
            response.setProvider(provider);
            response.setEnabled(true);
            response.setDegraded(false);
            response.setAnswer("ok");
            return response;
        }
    }
}
