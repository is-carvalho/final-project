package org.raimundo.ecommerce.infrastructure.http;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(ExternalServiceProperties.class)
public class HttpClientConfig {
    @Bean
    WebClient.Builder webClientBuilder() {
        return WebClient.builder().defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    }
}
