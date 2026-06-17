package org.raimundo.ecommerce.application.order;

import org.junit.jupiter.api.Test;
import org.raimundo.ecommerce.application.ports.CatalogPort;
import org.raimundo.ecommerce.domain.common.DomainException;
import org.raimundo.ecommerce.domain.common.Money;
import org.raimundo.ecommerce.domain.order.Order;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ManageOrderItemsUseCaseTest {
    private final CreateOrderUseCaseTest.InMemoryOrders orders = new CreateOrderUseCaseTest.InMemoryOrders();
    private final OrderService service = new OrderService(orders,
            (customerId, correlationId) -> new org.raimundo.ecommerce.application.ports.CustomerPort.CustomerStatus(customerId, true, true, false),
            availableCatalog(), CreateOrderUseCaseTest.noopNotifications());

    @Test
    void addsAndRemovesDraftItems() {
        Order order = service.create("customer-1", "caller", "corr");

        Order withItem = service.addItem(order.id(), "product-1", 2, "caller", "corr");
        var itemId = withItem.items().getFirst().id();

        assertThat(withItem.items().getFirst().quantity()).isEqualTo(2);
        Order withoutItem = service.removeItem(order.id(), itemId, "caller", "corr");
        assertThat(withoutItem.items()).isEmpty();
    }

    @Test
    void rejectsUnavailableProduct() {
        OrderService rejecting = new OrderService(orders,
                (customerId, correlationId) -> new org.raimundo.ecommerce.application.ports.CustomerPort.CustomerStatus(customerId, true, true, false),
                CreateOrderUseCaseTest.unavailableCatalog(), CreateOrderUseCaseTest.noopNotifications());
        Order order = rejecting.create("customer-1", "caller", "corr");

        assertThatThrownBy(() -> rejecting.addItem(order.id(), "missing", 1, "caller", "corr"))
                .isInstanceOf(DomainException.class);
    }

    public static CatalogPort availableCatalog() {
        return (productId, correlationId) -> new CatalogPort.ProductStatus(productId, true, true,
                new Money(new BigDecimal("12.34"), "BRL"));
    }
}
