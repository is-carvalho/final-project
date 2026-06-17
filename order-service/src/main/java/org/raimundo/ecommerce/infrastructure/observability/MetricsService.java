package org.raimundo.ecommerce.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MetricsService {
    private final MeterRegistry registry;

    public MetricsService(MeterRegistry registry) {
        this.registry = registry;
    }

    public void businessError(String code) {
        Counter.builder("order_service_business_errors_total").tag("code", code).register(registry).increment();
    }

    public void paymentAttempt(String outcome) {
        Counter.builder("order_service_payment_attempts_total").tag("outcome", outcome).register(registry).increment();
    }
}
