package org.raimundo.ecommerce.domain.payment;

import org.junit.jupiter.api.Test;
import org.raimundo.ecommerce.domain.common.Money;
import org.raimundo.ecommerce.domain.order.Order;
import org.raimundo.ecommerce.domain.order.OrderStatus;
import org.raimundo.ecommerce.domain.order.ProductPrice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentLifecycleTest {
    @Test
    void thirdRejectionCancelsOrder() {
        Order order = confirmedOrder();

        order.startPayment();
        order.rejectPayment("nope");
        order.startPayment();
        order.rejectPayment("nope");
        order.startPayment();
        order.rejectPayment("nope");

        assertThat(order.paymentRejectionCount()).isEqualTo(3);
        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void transactionCanBeApproved() {
        PaymentTransaction payment = PaymentTransaction.pending(UUID.randomUUID(), 1, "key", new Money(BigDecimal.TEN, "BRL"));

        payment.approve("provider-1");

        assertThat(payment.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.providerTransactionId()).isEqualTo("provider-1");
        assertThat(payment.processedAt()).isNotNull();
    }

    @Test
    void transactionCanBeRejectedAndExposeAllFields() {
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Instant requestedAt = Instant.parse("2026-01-01T00:00:00Z");
        PaymentTransaction payment = new PaymentTransaction(id, orderId, null, 2, "key",
                null, new Money(BigDecimal.TEN, "BRL"), null, requestedAt, null);

        payment.reject("provider-2", "insufficient funds");

        assertThat(payment.id()).isEqualTo(id);
        assertThat(payment.orderId()).isEqualTo(orderId);
        assertThat(payment.status()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(payment.attemptNumber()).isEqualTo(2);
        assertThat(payment.idempotencyKey()).isEqualTo("key");
        assertThat(payment.providerTransactionId()).isEqualTo("provider-2");
        assertThat(payment.amount().amount()).isEqualByComparingTo("10.00");
        assertThat(payment.failureReason()).isEqualTo("insufficient funds");
        assertThat(payment.requestedAt()).isEqualTo(requestedAt);
        assertThat(payment.processedAt()).isNotNull();
    }

    @Test
    void resultEventCapturesProviderIdentity() {
        UUID paymentId = UUID.randomUUID();

        PaymentResultEvent event = PaymentResultEvent.received(paymentId, "event-1", "APPROVED", "corr-1", "hash");

        assertThat(event.id()).isNotNull();
        assertThat(event.paymentTransactionId()).isEqualTo(paymentId);
        assertThat(event.providerEventId()).isEqualTo("event-1");
        assertThat(event.eventType()).isEqualTo("APPROVED");
        assertThat(event.correlationId()).isEqualTo("corr-1");
        assertThat(event.payloadHash()).isEqualTo("hash");
        assertThat(event.receivedAt()).isNotNull();
    }

    private Order confirmedOrder() {
        Order order = Order.draft("customer-1");
        order.addItem("product-1", 1, new Money(BigDecimal.TEN, "BRL"));
        order.confirm(List.of(new ProductPrice("product-1", new Money(BigDecimal.TEN, "BRL"))));
        return order;
    }
}
