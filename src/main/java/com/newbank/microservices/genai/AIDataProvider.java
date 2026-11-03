package com.newbank.microservices.genai;

import java.util.List;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import com.newbank.microservices.genai.dto.AccountDetails;
import com.newbank.microservices.genai.dto.AccountRequest;
import com.newbank.microservices.genai.dto.Advisor;
import com.newbank.microservices.genai.dto.CustomerDetails;
import com.newbank.microservices.genai.dto.CustomerRequest;

/**
 * #7 + #8: downstream targets are configurable NewBank service URLs called via the
 * shared load-balanced RestClient (timeouts configured centrally). No
 * {@code discoveryClient.getInstances(...).get(0)} — discovery is delegated to the
 * load balancer and every call is guarded by a circuit breaker with a fallback.
 * The caller identity (JWT subject) is propagated downstream (#1).
 */
@Service
public class AIDataProvider {

    private final VectorStore vectorStore;
    private final RestClient restClient;
    private final String userServiceUrl;
    private final String accountServiceUrl;
    private final String advisorsUrl;

    public AIDataProvider(VectorStore vectorStore,
            RestClient.Builder loadBalancedRestClientBuilder,
            @Value("${newbank.genai.user-service-url:http://user-service}") String userServiceUrl,
            @Value("${newbank.genai.account-service-url:http://account-service}") String accountServiceUrl,
            @Value("${newbank.genai.advisors-url:http://advisors-service}") String advisorsUrl) {
        this.restClient = loadBalancedRestClientBuilder.build();
        this.vectorStore = vectorStore;
        this.userServiceUrl = userServiceUrl;
        this.accountServiceUrl = accountServiceUrl;
        this.advisorsUrl = advisorsUrl;
    }

    private RestClient.RequestBodySpec withAuth(RestClient.RequestBodySpec spec) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getToken().getTokenValue());
        }
        return spec;
    }

    private RestClient.RequestHeadersSpec<?> withAuthHeaders(RestClient.RequestHeadersSpec<?> spec) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getToken().getTokenValue());
        }
        return spec;
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "allCustomersFallback")
    public List<CustomerDetails> getAllCustomers() {
        return withAuthHeaders(restClient.get().uri(userServiceUrl + "/customers"))
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    }

    @SuppressWarnings("unused")
    List<CustomerDetails> allCustomersFallback(Throwable t) {
        throw new DownstreamUnavailableException("user-service unavailable", t);
    }

    /**
     * #10: a null/blank request no longer serialises to the string "null" for vector
     * search — it returns a paginated advisor list from advisors-service instead.
     */
    @CircuitBreaker(name = "advisorsService", fallbackMethod = "advisorsFallback")
    public List<String> getAdvisors(Advisor advisorRequest) throws JacksonException {
        if (advisorRequest == null) {
            return listAdvisorsPage(0, 20);
        }
        ObjectMapper objectMapper = new ObjectMapper();
        String advisorAsJson = objectMapper.writeValueAsString(advisorRequest);

        SearchRequest request = SearchRequest.builder()
            .query(advisorAsJson)
            .topK(20)
            .build();

        List<Document> topMatches = this.vectorStore.similaritySearch(request);
        return topMatches.stream().map(Document::getFormattedContent).toList();
    }

    @SuppressWarnings("unused")
    List<String> advisorsFallback(Advisor advisorRequest, Throwable t) {
        throw new DownstreamUnavailableException("advisors-service unavailable", t);
    }

    /** #10: paginated advisor listing used when no search criteria are given. */
    @CircuitBreaker(name = "advisorsService", fallbackMethod = "advisorPageFallback")
    public List<String> listAdvisorsPage(int page, int size) {
        List<Advisor> advisors = withAuthHeaders(
                restClient.get().uri(advisorsUrl + "/advisors?page={p}&size={s}", page, size))
            .retrieve()
            .body(new ParameterizedTypeReference<List<Advisor>>() {});
        if (advisors == null) {
            return List.of();
        }
        ObjectMapper objectMapper = new ObjectMapper();
        return advisors.stream().map(a -> {
            try {
                return objectMapper.writeValueAsString(a);
            }
            catch (JacksonException e) {
                return a.toString();
            }
        }).toList();
    }

    @SuppressWarnings("unused")
    List<String> advisorPageFallback(int page, int size, Throwable t) {
        throw new DownstreamUnavailableException("advisors-service unavailable", t);
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "addAccountFallback")
    public AccountDetails addAccountToCustomer(int customerId, AccountRequest accountRequest) {
        return withAuth(restClient.post()
            .uri(accountServiceUrl + "/customers/" + customerId + "/accounts")
            .body(accountRequest))
            .retrieve()
            .body(AccountDetails.class);
    }

    @SuppressWarnings("unused")
    AccountDetails addAccountFallback(int customerId, AccountRequest accountRequest, Throwable t) {
        throw new DownstreamUnavailableException("account-service unavailable", t);
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "addCustomerFallback")
    public CustomerDetails addCustomerToBank(CustomerRequest customerRequest) {
        return withAuth(restClient.post()
            .uri(userServiceUrl + "/customers")
            .body(customerRequest))
            .retrieve()
            .body(CustomerDetails.class);
    }

    @SuppressWarnings("unused")
    CustomerDetails addCustomerFallback(CustomerRequest customerRequest, Throwable t) {
        throw new DownstreamUnavailableException("user-service unavailable", t);
    }

    public static class DownstreamUnavailableException extends RuntimeException {
        public DownstreamUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
