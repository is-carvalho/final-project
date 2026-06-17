package org.raimundo.ecommerce.infrastructure.persistence.payment;

import org.raimundo.ecommerce.application.payment.PaymentRepositoryPort;
import org.raimundo.ecommerce.domain.common.Money;
import org.raimundo.ecommerce.domain.payment.PaymentResultEvent;
import org.raimundo.ecommerce.domain.payment.PaymentTransaction;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaPaymentRepositoryAdapter implements PaymentRepositoryPort {
    private final SpringDataPaymentTransactionRepository payments;
    private final SpringDataPaymentResultEventRepository events;

    public JpaPaymentRepositoryAdapter(SpringDataPaymentTransactionRepository payments, SpringDataPaymentResultEventRepository events) {
        this.payments = payments;
        this.events = events;
    }

    @Override
    public PaymentTransaction save(PaymentTransaction payment, String correlationId) {
        PaymentTransactionEntity entity = payments.findById(payment.id()).orElseGet(PaymentTransactionEntity::new);
        entity.id = payment.id();
        entity.orderId = payment.orderId();
        entity.status = payment.status();
        entity.attemptNumber = payment.attemptNumber();
        entity.idempotencyKey = payment.idempotencyKey();
        entity.providerTransactionId = payment.providerTransactionId();
        entity.amount = payment.amount().amount();
        entity.currency = payment.amount().currency();
        entity.requestedAt = payment.requestedAt();
        entity.processedAt = payment.processedAt();
        entity.failureReason = payment.failureReason();
        entity.correlationId = correlationId;
        return toDomain(payments.save(entity));
    }

    @Override
    public Optional<PaymentTransaction> findById(UUID id) {
        return payments.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<PaymentTransaction> findByIdempotencyKey(String key) {
        return payments.findByIdempotencyKey(key).map(this::toDomain);
    }

    @Override
    public boolean eventExists(String providerEventId) {
        return events.existsByProviderEventId(providerEventId);
    }

    @Override
    public void saveEvent(PaymentResultEvent event) {
        PaymentResultEventEntity entity = new PaymentResultEventEntity();
        entity.id = event.id();
        entity.paymentTransactionId = event.paymentTransactionId();
        entity.providerEventId = event.providerEventId();
        entity.eventType = event.eventType();
        entity.receivedAt = event.receivedAt();
        entity.correlationId = event.correlationId();
        entity.payloadHash = event.payloadHash();
        events.save(entity);
    }

    private PaymentTransaction toDomain(PaymentTransactionEntity entity) {
        return new PaymentTransaction(entity.id, entity.orderId, entity.status, entity.attemptNumber,
                entity.idempotencyKey, entity.providerTransactionId, new Money(entity.amount, entity.currency),
                entity.failureReason, entity.requestedAt, entity.processedAt);
    }
}
