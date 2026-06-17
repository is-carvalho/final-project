package org.raimundo.ecommerce.infrastructure.http;

import org.raimundo.ecommerce.application.ports.CatalogPort;
import org.raimundo.ecommerce.domain.common.Money;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

@Component
public class CatalogHttpAdapter implements CatalogPort {
    private final WebClient client;

    public CatalogHttpAdapter(WebClient.Builder builder, ExternalServiceProperties properties) {
        this.client = builder.baseUrl(properties.catalogBaseUrl()).build();
    }

    @Override
    public ProductStatus findProduct(String productId, String correlationId) {
        ProductResponse response = client.get()
                .uri("/products/{id}", productId)
                .header("Correlation-Id", correlationId)
                .retrieve()
                .bodyToMono(ProductResponse.class)
                .onErrorReturn(new ProductResponse(productId, false, false, BigDecimal.ZERO, "BRL"))
                .block();
        return new ProductStatus(response.productId(), response.exists(), response.available(),
                new Money(response.currentPrice(), response.currency()));
    }

    record ProductResponse(String productId, boolean exists, boolean available, BigDecimal currentPrice, String currency) {
    }
}
