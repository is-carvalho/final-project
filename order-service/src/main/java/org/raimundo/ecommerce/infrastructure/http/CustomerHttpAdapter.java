package org.raimundo.ecommerce.infrastructure.http;

import org.raimundo.ecommerce.application.ports.CustomerPort;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class CustomerHttpAdapter implements CustomerPort {
    private final WebClient client;

    public CustomerHttpAdapter(WebClient.Builder builder, ExternalServiceProperties properties) {
        this.client = builder.baseUrl(properties.customerBaseUrl()).build();
    }

    @Override
    public CustomerStatus findCustomer(String customerId, String correlationId) {
        return client.get()
                .uri("/customers/{id}", customerId)
                .header("Correlation-Id", correlationId)
                .retrieve()
                .bodyToMono(CustomerStatus.class)
                .onErrorReturn(new CustomerStatus(customerId, false, false, false))
                .block();
    }
}
