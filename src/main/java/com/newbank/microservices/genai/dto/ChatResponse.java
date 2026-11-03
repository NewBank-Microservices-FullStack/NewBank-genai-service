package com.newbank.microservices.genai.dto;

/** #11: structured response with the resolved conversation id and model latency. */
public record ChatResponse(String reply, String conversationId, String model, long latencyMs) {
}
