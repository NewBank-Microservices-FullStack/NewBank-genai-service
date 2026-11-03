package com.newbank.microservices.genai;

import java.util.List;
import java.util.Set;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.client.RestClient;

import com.newbank.microservices.genai.dto.Advisor;
import com.newbank.microservices.genai.dto.Expertise;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * #10 + #13: a null advisor request returns the paginated advisor list instead of
 * searching the vector store for the string "null". Downstream HTTP is stubbed
 * with WireMock; the vector store is a Mockito mock.
 */
class AdvisorNullWireMockTest {

    private WireMockServer wiremock;
    private VectorStore vectorStore;
    private AIDataProvider provider;

    @BeforeEach
    void setUp() {
        wiremock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wiremock.start();
        wiremock.stubFor(get(urlPathEqualTo("/advisors")).willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("""
                [{"id":1,"firstName":"Helen","lastName":"Leary",
                  "specialties":[{"id":1,"name":"radiology"}]}]""")));
        vectorStore = mock(VectorStore.class);
        provider = new AIDataProvider(vectorStore, RestClient.builder(),
            "http://user-service",
            "http://account-service",
            wiremock.baseUrl());
    }

    @AfterEach
    void tearDown() {
        wiremock.stop();
    }

    @Test
    void nullRequest_returnsPaginatedListWithoutVectorSearch() throws Exception {
        List<String> result = provider.getAdvisors(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).contains("Helen");
        verifyNoInteractions(vectorStore);
    }

    @Test
    void realRequest_doesNotHitAdvisorsHttpEndpoint() {
        var request = new Advisor(2, "James", "Carter", Set.of(new Expertise(2, "surgery")));
        org.mockito.Mockito.when(vectorStore.similaritySearch(org.mockito.ArgumentMatchers.any()))
            .thenReturn(List.of());

        List<String> result = sneaky(() -> provider.getAdvisors(request));

        assertThat(result).isEmpty();
        assertThat(wiremock.getAllServeEvents()).isEmpty();
    }

    private static List<String> sneaky(ThrowingCall call) {
        try {
            return call.call();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private interface ThrowingCall {
        List<String> call() throws Exception;
    }
}
