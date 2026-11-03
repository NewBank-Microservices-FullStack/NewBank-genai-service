package com.newbank.microservices.genai;

import java.time.Duration;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * #8: the shared load-balanced RestClient with connect/read timeouts —
 * replaces the bare {@code RestClient.builder().build()} with no timeouts or
 * retries that AIDataProvider used to construct per instance.
 */
@Configuration
public class ResilientClientConfig {

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
            .withConnectTimeout(Duration.ofSeconds(2))
            .withReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder()
            .requestFactory(ClientHttpRequestFactories.get(settings));
    }
}
