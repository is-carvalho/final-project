package org.raimundo.ecommerce.application.ports;

import org.raimundo.ecommerce.domain.common.Money;

import java.util.UUID;

public interface PaymentGatewayPort {
    GatewayResult initiate(UUID paymentId, UUID orderId, Money amount, String idempotencyKey, String correlationId);

    record GatewayResult(String providerTransactionId, String outcome, String detail) {
        public boolean approved() {
            return "APPROVED".equals(outcome);
        }

        public boolean rejected() {
            return "REJECTED".equals(outcome);
        }
    }
}
