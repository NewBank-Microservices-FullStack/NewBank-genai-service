package com.newbank.microservices.genai;

import java.net.URI;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import com.newbank.microservices.genai.dto.AccountDetails;
import com.newbank.microservices.genai.dto.AccountRequest;
import com.newbank.microservices.genai.dto.Advisor;
import com.newbank.microservices.genai.dto.CustomerDetails;

@Service
public class AIDataProvider {

    private final VectorStore vectorStore;
    private final RestClient restClient;
    private final DiscoveryClient discoveryClient;

    public AIDataProvider(VectorStore vectorStore, DiscoveryClient discoveryClient) {
        this.restClient = RestClient.builder().build();
        this.vectorStore = vectorStore;
        this.discoveryClient = discoveryClient;
    }

    public List<CustomerDetails> getAllCustomers() {
        return restClient
            .get()
            .uri(getCustomerServiceUri() + "/customers")
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    }

    public List<String> getAdvisors(Advisor advisorRequest) throws JacksonException {
        ObjectMapper objectMapper = new ObjectMapper();
        String advisorAsJson = objectMapper.writeValueAsString(advisorRequest);

        int topK = 20;
        if (advisorRequest == null) {
            topK = 50;
        }

        SearchRequest request = SearchRequest.builder()
            .query(advisorAsJson)
            .topK(topK)
            .build();

        List<Document> topMatches = this.vectorStore.similaritySearch(request);
        return topMatches.stream().map(Document::getFormattedContent).toList();
    }

    public AccountDetails addAccountToCustomer(int customerId, AccountRequest accountRequest) {
        return restClient
            .post()
            .uri(getCustomerServiceUri() + "/customers/" + customerId + "/accounts")
            .body(accountRequest)
            .retrieve()
            .body(AccountDetails.class);
    }

    public CustomerDetails addCustomerToBank(CustomerRequest customerRequest) {
        return restClient
            .post()
            .uri(getCustomerServiceUri() + "/customers")
            .body(customerRequest)
            .retrieve()
            .body(CustomerDetails.class);
    }

    @NotNull
    private URI getCustomerServiceUri() {
        return discoveryClient.getInstances("customers-service").get(0).getUri();
    }
}
