package com.macro.mall.portal.component.ai;

import com.macro.mall.portal.domain.AiChatRequest;
import com.macro.mall.portal.domain.AiChatResponse;
import com.macro.mall.portal.service.AiChatClient;
import org.springframework.stereotype.Component;

@Component
public class MockAiChatClient implements AiChatClient {
    @Override
    public String provider() {
        return "mock";
    }

    @Override
    public boolean configured() {
        return true;
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        AiChatResponse response = new AiChatResponse();
        response.setProvider(provider());
        response.setModel("mock-rag");
        response.setEnabled(true);
        response.setDegraded(false);
        response.setAnswer("【Mock客服】我已根据召回的商品、订单或售后知识整理回答。"
                + "请以命中的知识片段为准；切换 mall.ai.provider 为 openai/qwen/deepseek/ollama 后可调用真实模型。\n\n"
                + "问题：" + request.getQuestion() + "\n\n"
                + "参考上下文：\n" + request.getContext());
        return response;
    }
}
