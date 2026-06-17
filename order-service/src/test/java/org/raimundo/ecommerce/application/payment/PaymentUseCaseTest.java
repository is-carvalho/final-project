package org.raimundo.ecommerce.application.payment;

import org.junit.jupiter.api.Test;
import org.raimundo.ecommerce.application.order.CreateOrderUseCaseTest;
import org.raimundo.ecommerce.application.order.ManageOrderItemsUseCaseTest;
import org.raimundo.ecommerce.application.order.OrderService;
import org.raimundo.ecommerce.application.ports.PaymentGatewayPort;
import org.raimundo.ecommerce.domain.payment.PaymentStatus;
import org.raimundo.ecommerce.domain.payment.PaymentTransaction;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentUseCaseTest {
    private final CreateOrderUseCaseTest.InMemoryOrders orders = new CreateOrderUseCaseTest.InMemoryOrders();
    private final InMemoryPayments payments = new InMemoryPayments();
    private final List<String> notifications = new ArrayList<>();
    private final OrderService orderService = new OrderService(orders,
            (customerId, correlationId) -> new org.raimundo.ecommerce.application.ports.CustomerPort.CustomerStatus(customerId, true, true, false),
            ManageOrderItemsUseCaseTest.availableCatalog(),
            (eventType, orderId, customerId, correlationId) -> notifications.add(eventType));

    @Test
    void initiationIsIdempotentByKey() {
        var order = orderService.create("customer-1", "caller", "corr");
        orderService.addItem(order.id(), "product-1", 1, "caller", "corr");
        orderService.confirm(order.id(), "caller", "corr");
        PaymentService service = new PaymentService(payments, orderService, pendingGateway(), CreateOrderUseCaseTest.noopNotifications());

        PaymentTransaction first = service.initiate(order.id(), "pay-key", "corr");
        PaymentTransaction replay = service.initiate(order.id(), "pay-key", "corr");

        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(payments.store).hasSize(1);
    }

    @Test
    void duplicateCallbackIsIgnored() {
        var order = orderService.create("customer-1", "caller", "corr");
        orderService.addItem(order.id(), "product-1", 1, "caller", "corr");
        orderService.confirm(order.id(), "caller", "corr");
        notifications.clear();
        PaymentService service = new PaymentService(payments, orderService, pendingGateway(),
                (eventType, orderId, customerId, correlationId) -> notifications.add(eventType));
        PaymentTransaction payment = service.initiate(order.id(), "pay-key", "corr");

        service.process(payment.id(), "event-1", "APPROVED", "provider-1", "ok", "corr");
        PaymentTransaction replay = service.process(payment.id(), "event-1", "APPROVED", "provider-1", "ok", "corr");

        assertThat(replay.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payments.events).containsExactly("event-1");
        assertThat(notifications).containsExactly("PAYMENT_APPROVED");
    }

    static PaymentGatewayPort pendingGateway() {
        return (paymentId, orderId, amount, idempotencyKey, correlationId) ->
                new PaymentGatewayPort.GatewayResult(null, "PENDING", "accepted");
    }

    static class InMemoryPayments implements PaymentRepositoryPort {
        final Map<UUID, PaymentTransaction> store = new LinkedHashMap<>();
        final Set<String> events = new LinkedHashSet<>();

        @Override
        public PaymentTransaction save(PaymentTransaction payment, String correlationId) {
            store.put(payment.id(), payment);
            return payment;
        }

        @Override
        public Optional<PaymentTransaction> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<PaymentTransaction> findByIdempotencyKey(String key) {
            return store.values().stream().filter(payment -> key.equals(payment.idempotencyKey())).findFirst();
        }

        @Override
        public boolean eventExists(String providerEventId) {
            return events.contains(providerEventId);
        }

        @Override
        public void saveEvent(org.raimundo.ecommerce.domain.payment.PaymentResultEvent event) {
            events.add(event.providerEventId());
        }
    }
}
