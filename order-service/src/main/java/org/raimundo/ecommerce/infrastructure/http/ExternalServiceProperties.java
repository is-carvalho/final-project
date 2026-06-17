package org.raimundo.ecommerce.infrastructure.http;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external")
public record ExternalServiceProperties(String customerBaseUrl, String catalogBaseUrl, String paymentBaseUrl,
                                        String notificationBaseUrl) {
}
