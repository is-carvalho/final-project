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

class OrderDraftTest {
    @Test
    void addsSameProductByIncreasingQuantity() {
        Order order = Order.draft("customer-1");

        order.addItem("product-1", 1, new Money(BigDecimal.TEN, "BRL"));
        order.addItem("product-1", 2, new Money(BigDecimal.TEN, "BRL"));

        assertThat(order.items()).hasSize(1);
        assertThat(order.items().getFirst().quantity()).isEqualTo(3);
    }

    @Test
    void rejectsInvalidQuantity() {
        Order order = Order.draft("customer-1");

        assertThatThrownBy(() -> order.addItem("product-1", 0, new Money(BigDecimal.TEN, "BRL")))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejectsInvalidCustomerAndProduct() {
        assertThatThrownBy(() -> Order.draft(""))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new OrderItem(UUID.randomUUID(), "", 1, new Money(BigDecimal.TEN, "BRL")))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void removesItemsAndReportsMissingItems() {
        Order order = Order.draft("customer-1");
        order.addItem("product-1", 1, new Money(BigDecimal.TEN, "BRL"));
        UUID itemId = order.items().getFirst().id();

        order.removeItem(itemId);

        assertThat(order.items()).isEmpty();
        assertThatThrownBy(() -> order.removeItem(itemId))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void restoredOrderExposesStateAndCanUseDefaults() {
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        Instant updated = Instant.parse("2026-01-02T00:00:00Z");
        UUID id = UUID.randomUUID();
        OrderItem item = new OrderItem(UUID.randomUUID(), "product-1", 2, new Money(BigDecimal.TEN, "BRL"));

        Order restored = Order.restore(id, "customer-1", OrderStatus.PAYMENT_REJECTED, List.of(item),
                new Money(new BigDecimal("20.00"), "BRL"), 1, "reason", created, updated);
        Order defaulted = Order.restore(null, "customer-2", null, null, null, 0, null, null, null);

        assertThat(restored.id()).isEqualTo(id);
        assertThat(restored.customerId()).isEqualTo("customer-1");
        assertThat(restored.cancellationReason()).isEqualTo("reason");
        assertThat(restored.createdAt()).isEqualTo(created);
        assertThat(restored.updatedAt()).isEqualTo(updated);
        assertThat(restored.active()).isTrue();
        assertThat(defaulted.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(defaulted.items()).isEmpty();
        assertThat(defaulted.total().amount()).isEqualByComparingTo("0.00");
    }
}
