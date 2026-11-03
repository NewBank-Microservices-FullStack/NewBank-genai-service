package com.newbank.microservices.genai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** #11: JSON contract — replaces raw String in/out on POST /chatclient. */
public record ChatRequest(
        @NotBlank @Size(max = 4000) String message,
        String conversationId,
        String sessionId,
        /** #3: write tools only run when the user explicitly confirms. */
        boolean confirmWrites) {
}
