package com.newbank.microservices.genai;

import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.JsonReader;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import com.newbank.microservices.genai.dto.Advisor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;

@Component
public class VectorStoreController {

    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(VectorStoreController.class);
    private final VectorStore vectorStore;
    private final WebClient webClient;

    public VectorStoreController(VectorStore vectorStore, WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    @EventListener
    public void loadAdvisorDataToVectorStoreOnStartup(ApplicationStartedEvent event) throws IOException {
        Resource resource = new ClassPathResource("vectorstore.json");

        if (resource.exists()) {
            ((SimpleVectorStore) this.vectorStore).load(resource);
            logger.info("vector store loaded from existing vectorstore.json file in the classpath");
            return;
        }

        String advisorsHost = "http://advisors-service/";
        List<Advisor> advisors = webClient
            .get()
            .uri(advisorsHost + "advisors")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<Advisor>>() {})
            .block();

        Resource advisorsAsJson = convertListToJsonResource(advisors);
        DocumentReader reader = new JsonReader(advisorsAsJson);

        List<Document> documents = reader.get();
        this.vectorStore.add(documents);

        if (vectorStore instanceof SimpleVectorStore store) {
            FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
            File file = Files.createTempFile("vectorstore", ".json", attr).toFile();
            store.save(file);
            logger.info("vector store contents written to {}", file.getAbsolutePath());
        }

        logger.info("vector store loaded with {} documents", documents.size());
    }

    public Resource convertListToJsonResource(List<Advisor> advisors) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String json = objectMapper.writeValueAsString(advisors);
            byte[] jsonBytes = json.getBytes();
            return new ByteArrayResource(jsonBytes);
        }
        catch (JacksonException e) {
            logger.error("Error processing JSON in the convertListToJsonResource function", e);
            return null;
        }
    }
}
