package com.newbank.microservices.genai;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * #2: the ChatMemory bean is shared, but every call MUST pass
 * {@code ChatMemory.CONVERSATION_ID} (see {@link NewBankChatClient}) so histories
 * are keyed per user/session instead of one global history.
 */
@Configuration
public class AIBeanConfiguration {

    @Bean
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(20).build();
    }

    /**
     * Local/dev vector store. In prod ({@code VECTOR_STORE_TYPE=pgvector}) the
     * pgvector auto-configuration provides the primary store; this remains the
     * fallback (#17).
     */
    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
