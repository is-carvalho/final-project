package org.raimundo.ecommerce.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.raimundo.ecommerce.domain.payment.PaymentTransaction;

import java.math.BigDecimal;
import java.util.UUID;

public final class PaymentDtos {
    private PaymentDtos() {
    }

    public record InitiatePaymentRequest(@NotNull UUID orderId) {
    }

    public record PaymentCallbackRequest(@NotBlank String providerEventId, @NotBlank String outcome,
                                         String providerTransactionId, String detail) {
    }

    public record PaymentResponse(UUID id, UUID orderId, String status, int attemptNumber,
                                  String providerTransactionId, BigDecimal amount, String currency,
                                  String correlationId) {
        public static PaymentResponse from(PaymentTransaction payment) {
            return from(payment, null);
        }

        public static PaymentResponse from(PaymentTransaction payment, String correlationId) {
            return new PaymentResponse(payment.id(), payment.orderId(), payment.status().name(), payment.attemptNumber(),
                    payment.providerTransactionId(), payment.amount().amount(), payment.amount().currency(), correlationId);
        }
    }
}
