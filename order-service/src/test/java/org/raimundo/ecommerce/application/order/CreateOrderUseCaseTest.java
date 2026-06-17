package org.raimundo.ecommerce.application.order;

import org.junit.jupiter.api.Test;
import org.raimundo.ecommerce.application.ports.CatalogPort;
import org.raimundo.ecommerce.application.ports.CustomerPort;
import org.raimundo.ecommerce.application.ports.NotificationPort;
import org.raimundo.ecommerce.domain.common.DomainException;
import org.raimundo.ecommerce.domain.order.Order;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CreateOrderUseCaseTest {
    private final InMemoryOrders orders = new InMemoryOrders();
    private final OrderService service = new OrderService(orders,
            (customerId, correlationId) -> new CustomerPort.CustomerStatus(customerId, true, true, false),
            unavailableCatalog(), noopNotifications());

    @Test
    void createsDraftOrderForEligibleCustomer() {
        Order created = service.create("customer-1", "caller", "corr");

        assertThat(created.customerId()).isEqualTo("customer-1");
        assertThat(created.active()).isTrue();
    }

    @Test
    void preventsDuplicateActiveOrderForCustomer() {
        service.create("customer-1", "caller", "corr");

        assertThatThrownBy(() -> service.create("customer-1", "caller", "corr"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejectsIneligibleCustomer() {
        OrderService blocked = new OrderService(orders,
                (customerId, correlationId) -> new CustomerPort.CustomerStatus(customerId, true, true, true),
                unavailableCatalog(), noopNotifications());

        assertThatThrownBy(() -> blocked.create("customer-2", "caller", "corr"))
                .isInstanceOf(DomainException.class);
    }

    public static CatalogPort unavailableCatalog() {
        return (productId, correlationId) -> new CatalogPort.ProductStatus(productId, false, false, null);
    }

    public static NotificationPort noopNotifications() {
        return (eventType, orderId, customerId, correlationId) -> { };
    }

    public static class InMemoryOrders implements OrderRepositoryPort {
        final Map<UUID, Order> store = new LinkedHashMap<>();

        @Override
        public Order save(Order order, String actor, String correlationId) {
            store.put(order.id(), order);
            return order;
        }

        @Override
        public Optional<Order> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Order> findByCustomerId(String customerId) {
            return store.values().stream().filter(order -> order.customerId().equals(customerId)).toList();
        }

        @Override
        public List<Order> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public boolean existsActiveOrderForCustomer(String customerId) {
            return store.values().stream().anyMatch(order -> order.customerId().equals(customerId) && order.active());
        }
    }
}
