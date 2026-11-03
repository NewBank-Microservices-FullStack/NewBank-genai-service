package com.newbank.microservices.genai;

import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.api.ChatClientResponse;
import org.springframework.core.Ordered;

/**
 * #15: metadata-only advisor; logging properties managed via logback.
 */
public class MaskingAdvisor implements CallAdvisor {

    @Override
    public String getName() {
        return "MaskingAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 100;
    }

    @Override
    public ChatClientResponse aroundCall(ChatClientRequest request, CallAdvisorChain chain) {
        return chain.nextAroundCall(request);
    }
}
