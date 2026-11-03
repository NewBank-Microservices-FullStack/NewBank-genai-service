package com.newbank.microservices.genai;

import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.core.Ordered;

/**
 * #15: metadata-only advisor; logging properties managed via logback.
 */
public class MaskingAdvisor implements CallAroundAdvisor {

    @Override
    public String getName() {
        return "MaskingAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 100;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        return chain.nextAroundCall(request);
    }
}
