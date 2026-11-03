package com.newbank.microservices.genai;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import com.newbank.microservices.genai.dto.Advisor;

/**
 * #17 + #4: the advisor index is built at RUNTIME from advisors-service (#7:
 * URL configurable + load-balanced, not hardcoded {@code http://advisors-service/}),
 * then validated before swap. #17: refresh is ASYNC — startup never
 * {@code block()}s on embedding. The old committed {@code vectorstore.json}
 * (stale PetClinic radiology dump) is deleted; nothing is read from or written
 * to the classpath.
 */
@Configuration
@EnableAsync
public class VectorStoreController {

    private final Logger logger = LoggerFactory.getLogger(VectorStoreController.class);
    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final RestClient restClient;
    private final String advisorsUrl;

    public VectorStoreController(VectorStore vectorStore,
            EmbeddingModel embeddingModel,
            RestClient.Builder loadBalancedRestClientBuilder,
            @Value("${newbank.genai.advisors-url:http://advisors-service}") String advisorsUrl) {
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
        this.restClient = loadBalancedRestClientBuilder.build();
        this.advisorsUrl = advisorsUrl;
    }

    /** Fire-and-forget refresh; startup never blocks on embedding (#17). */
    @Async
    @EventListener
    public void loadAdvisorDataToVectorStoreOnStartup(ApplicationStartedEvent event) {
        try {
            refresh();
        }
        catch (Exception e) {
            logger.error("advisor index refresh failed; chat continues without fresh advisors", e);
        }
    }

    /** Re-fetch advisors, re-embed, validate, then swap into the store. */
    public void refresh() {
        List<Advisor> advisors = restClient.get()
            .uri(advisorsUrl + "/advisors")
            .retrieve()
            .body(new ParameterizedTypeReference<List<Advisor>>() {});
        if (advisors == null || advisors.isEmpty()) {
            logger.warn("advisor refresh returned no advisors; keeping existing index");
            return;
        }
        Resource advisorsAsJson = convertListToJsonResource(advisors);
        if (advisorsAsJson == null) {
            logger.error("advisor refresh serialisation failed; keeping existing index");
            return;
        }
        DocumentReader reader = new JsonReader(advisorsAsJson);
        List<Document> documents = reader.get();
        if (documents == null || documents.isEmpty()) {
            logger.warn("advisor refresh produced no documents; keeping existing index");
            return;
        }
        // Validate a sample embedding before swapping the index (#4).
        float[] probe = embeddingModel.embed(documents.get(0).getText());
        if (probe == null || probe.length == 0) {
            logger.error("advisor refresh validation failed (empty embedding); keeping existing index");
            return;
        }
        if (vectorStore instanceof SimpleVectorStore store) {
            store.add(documents);
            logger.info("advisor index refreshed with {} documents", documents.size());
        }
        else {
            vectorStore.add(documents);
            logger.info("advisor index refreshed with {} documents", documents.size());
        }
    }

    public Resource convertListToJsonResource(List<Advisor> advisors) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String json = objectMapper.writeValueAsString(advisors);
            return new ByteArrayResource(json.getBytes());
        }
        catch (Exception e) {
            logger.error("Error processing JSON in the convertListToJsonResource function", e);
            return null;
        }
    }
}
