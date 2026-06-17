package org.raimundo.ecommerce.application.payment;

import org.raimundo.ecommerce.domain.payment.PaymentResultEvent;
import org.raimundo.ecommerce.domain.payment.PaymentTransaction;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepositoryPort {
    PaymentTransaction save(PaymentTransaction payment, String correlationId);
    Optional<PaymentTransaction> findById(UUID id);
    Optional<PaymentTransaction> findByIdempotencyKey(String key);
    boolean eventExists(String providerEventId);
    void saveEvent(PaymentResultEvent event);
}
