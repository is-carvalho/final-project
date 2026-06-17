package org.raimundo.ecommerce.domain.order;

import org.junit.jupiter.api.Test;
import org.raimundo.ecommerce.domain.common.DomainException;
import org.raimundo.ecommerce.domain.common.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderLifecycleTest {
    @Test
    void confirmsDraftWithCurrentPrices() {
        Order order = Order.draft("customer-1");
        order.addItem("product-1", 2, new Money(BigDecimal.ONE, "BRL"));

        order.confirm(List.of(new ProductPrice("product-1", new Money(BigDecimal.TEN, "BRL"))));

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.total().amount()).isEqualByComparingTo("20.00");
    }

    @Test
    void approvedOrderCannotBeCancelled() {
        Order order = Order.draft("customer-1");
        order.addItem("product-1", 1, new Money(BigDecimal.TEN, "BRL"));
        order.confirm(List.of(new ProductPrice("product-1", new Money(BigDecimal.TEN, "BRL"))));
        order.startPayment();
        order.approvePayment();

        assertThatThrownBy(() -> order.cancel("late"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejectsEditingAndConfirmationOutsideDraft() {
        Order order = confirmedOrder();

        assertThatThrownBy(() -> order.addItem("product-2", 1, new Money(BigDecimal.ONE, "BRL")))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> order.confirm(List.of(new ProductPrice("product-1", new Money(BigDecimal.ONE, "BRL")))))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejectsEmptyConfirmationAndMissingPrice() {
        Order empty = Order.draft("customer-1");
        Order order = Order.draft("customer-1");
        order.addItem("product-1", 1, new Money(BigDecimal.TEN, "BRL"));

        assertThatThrownBy(() -> empty.confirm(List.of()))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> order.confirm(List.of(new ProductPrice("other", new Money(BigDecimal.TEN, "BRL")))))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void cancelsDraftAndMarksInactive() {
        Order order = Order.draft("customer-1");

        order.cancel("");

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.cancellationReason()).isEqualTo("REQUESTED_BY_CUSTOMER");
        assertThat(order.active()).isFalse();
        assertThatThrownBy(() -> order.cancel("again"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void paymentStartReturnsAttemptAndUpdatesTimestamp() {
        Instant old = Instant.parse("2026-01-01T00:00:00Z");
        Order order = Order.restore(UUID.randomUUID(), "customer-1", OrderStatus.CONFIRMED, List.of(),
                new Money(BigDecimal.TEN, "BRL"), 0, null, old, old);

        int attempt = order.startPayment();

        assertThat(attempt).isEqualTo(1);
        assertThat(order.status()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(order.updatedAt()).isAfter(old);
        assertThat(order.active()).isTrue();
    }

    @Test
    void rejectsInvalidPaymentStartAndApproval() {
        Order draft = Order.draft("customer-1");

        assertThatThrownBy(draft::startPayment)
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(draft::approvePayment)
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> draft.rejectPayment("bad"))
                .isInstanceOf(DomainException.class);
    }

    private Order confirmedOrder() {
        Order order = Order.draft("customer-1");
        order.addItem("product-1", 1, new Money(BigDecimal.TEN, "BRL"));
        order.confirm(List.of(new ProductPrice("product-1", new Money(BigDecimal.TEN, "BRL"))));
        return order;
    }
}
