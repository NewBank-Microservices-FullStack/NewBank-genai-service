package com.newbank.microservices.genai;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * #3: confirmation gating. The model may PROPOSE a write, but the write executes
 * only after the user re-posts with {@code confirmWrites=true} for the same
 * conversation.
 */
@Service
public class ToolConfirmationService {

    public record PendingWrite(String token, String conversationId, String summary) {}

    private final ConcurrentHashMap<String, PendingWrite> pending = new ConcurrentHashMap<>();

    public PendingWrite propose(String conversationId, String summary) {
        PendingWrite pw = new PendingWrite(UUID.randomUUID().toString(), conversationId, summary);
        pending.put(conversationId, pw);
        return pw;
    }

    public boolean confirm(String conversationId) {
        return pending.remove(conversationId) != null;
    }

    public boolean hasPending(String conversationId) {
        return pending.containsKey(conversationId);
    }
}
