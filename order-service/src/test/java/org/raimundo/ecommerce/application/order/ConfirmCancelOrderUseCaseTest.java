package org.raimundo.ecommerce.application.order;

import org.junit.jupiter.api.Test;
import org.raimundo.ecommerce.domain.common.DomainException;
import org.raimundo.ecommerce.domain.order.Order;
import org.raimundo.ecommerce.domain.order.OrderStatus;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfirmCancelOrderUseCaseTest {
    private final CreateOrderUseCaseTest.InMemoryOrders orders = new CreateOrderUseCaseTest.InMemoryOrders();
    private final List<String> notifications = new ArrayList<>();
    private final OrderService service = new OrderService(orders,
            (customerId, correlationId) -> new org.raimundo.ecommerce.application.ports.CustomerPort.CustomerStatus(customerId, true, true, false),
            ManageOrderItemsUseCaseTest.availableCatalog(),
            (eventType, orderId, customerId, correlationId) -> notifications.add(eventType));

    @Test
    void confirmsOrderWithCurrentPricingAndPublishesNotification() {
        Order order = service.create("customer-1", "caller", "corr");
        service.addItem(order.id(), "product-1", 1, "caller", "corr");

        Order confirmed = service.confirm(order.id(), "caller", "corr");

        assertThat(confirmed.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(confirmed.total().amount()).isEqualByComparingTo("12.34");
        assertThat(notifications).contains("ORDER_CONFIRMED");
    }

    @Test
    void cancelsBeforeApprovalAndRejectsMissingOrders() {
        Order order = service.create("customer-1", "caller", "corr");

        Order cancelled = service.cancel(order.id(), "caller", "corr");

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(notifications).contains("ORDER_CANCELLED");
        assertThatThrownBy(() -> service.get(java.util.UUID.randomUUID()))
                .isInstanceOf(DomainException.class);
    }
}
