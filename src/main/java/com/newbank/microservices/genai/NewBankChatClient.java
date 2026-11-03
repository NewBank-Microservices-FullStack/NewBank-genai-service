package com.newbank.microservices.genai;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.newbank.microservices.genai.dto.ChatRequest;
import com.newbank.microservices.genai.dto.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * #1: every call requires a JWT (see {@link SecurityConfig}). #2: chat memory is
 * keyed by {@code CONVERSATION_ID} derived from conversation/session/user id — no
 * shared global history. #11: JSON DTO in/out plus SSE streaming, Bean Validation,
 * and RFC 7807 errors ({@link GlobalExceptionHandler}) instead of 200-with-error-string.
 */
@RestController
@RequestMapping("/")
@SecurityRequirement(name = "bearer-jwt")
public class NewBankChatClient {

    private static final Logger LOG = LoggerFactory.getLogger(NewBankChatClient.class);

    private final ChatClient chatClient;
    private final Guardrails guardrails;
    private final ModelMetrics metrics;
    private final RateLimiterRegistry limiterRegistry;
    private final ConcurrentHashMap<String, RateLimiter> perUserLimiters;

    public NewBankChatClient(ChatClient.Builder builder, ChatMemory chatMemory, BankTools bankTools,
            Guardrails guardrails, ModelMetrics metrics,
            RateLimiterRegistry limiterRegistry,
            ConcurrentHashMap<String, RateLimiter> perUserLimiters) {
        this.guardrails = guardrails;
        this.metrics = metrics;
        this.limiterRegistry = limiterRegistry;
        this.perUserLimiters = perUserLimiters;
        this.chatClient = builder
            .defaultSystem(Guardrails.SYSTEM_PROMPT)
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory)
                    .order(10)
                    .build(),
                // #15: metadata-only advisor; SimpleLoggerAdvisor (full PII) is gone.
                new MaskingAdvisor())
            .defaultTools(bankTools)
            .build();
    }

    /** #2: resolve the memory key — explicit id wins, never a shared default. */
    static String conversationId(ChatRequest req, String userId) {
        if (req.conversationId() != null && !req.conversationId().isBlank()) {
            return req.conversationId();
        }
        if (req.sessionId() != null && !req.sessionId().isBlank()) {
            return "sess-" + req.sessionId();
        }
        if (userId != null) {
            return "user-" + userId;
        }
        return "anon-" + UUID.randomUUID();
    }

    private String currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : String.valueOf(auth.getName());
    }

    private void checkRateLimit(String userId) {
        RateLimiter limiter = perUserLimiters.computeIfAbsent(
            userId == null ? "anonymous" : userId,
            k -> limiterRegistry.rateLimiter("chatPerUser-" + k));
        if (!limiter.acquirePermission()) {
            throw new RateLimitedException("Too many requests. Try again later.");
        }
    }

    @PostMapping("/chatclient")
    @Operation(summary = "Chat with the NewBank assistant")
    public ChatResponse exchange(@Valid @RequestBody ChatRequest request) {
        String userId = currentUserId();
        guardrails.moderate(request.message());
        checkRateLimit(userId);
        String capped = guardrails.enforceLength(request.message());
        String convId = conversationId(request, userId);
        long start = System.currentTimeMillis();
        Timer.Sample sample = metrics.start();
        try {
            String reply = this.chatClient.prompt()
                .user(capped)
                .advisors(a -> a.param(CONVERSATION_ID, convId))
                .toolContext(Map.of("conversationId", convId, "confirmed", request.confirmWrites()))
                .call()
                .content();
            metrics.addTokens((capped == null ? 0 : capped.length() / 4) + (reply == null ? 0 : reply.length() / 4));
            return new ChatResponse(reply, convId, null, System.currentTimeMillis() - start);
        }
        finally {
            metrics.stopLatency(sample);
        }
    }

    @PostMapping(value = "/chatclient/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Streaming chat (SSE)")
    public Flux<String> stream(@Valid @RequestBody ChatRequest request) {
        String userId = currentUserId();
        guardrails.moderate(request.message());
        checkRateLimit(userId);
        String capped = guardrails.enforceLength(request.message());
        String convId = conversationId(request, userId);
        LOG.info("stream chat conversation={}", convId);
        return this.chatClient.prompt()
            .user(capped)
            .advisors(a -> a.param(CONVERSATION_ID, convId))
            .toolContext(Map.of("conversationId", convId, "confirmed", request.confirmWrites()))
            .stream()
            .content();
    }

    public static class RateLimitedException extends RuntimeException {
        public RateLimitedException(String message) {
            super(message);
        }
    }
}
