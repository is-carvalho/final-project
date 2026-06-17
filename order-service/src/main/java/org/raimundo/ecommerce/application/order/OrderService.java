package org.raimundo.ecommerce.application.order;

import org.raimundo.ecommerce.application.ports.CatalogPort;
import org.raimundo.ecommerce.application.ports.CustomerPort;
import org.raimundo.ecommerce.application.ports.NotificationPort;
import org.raimundo.ecommerce.domain.common.DomainException;
import org.raimundo.ecommerce.domain.order.Order;
import org.raimundo.ecommerce.domain.order.ProductPrice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepositoryPort orders;
    private final CustomerPort customers;
    private final CatalogPort catalog;
    private final NotificationPort notifications;

    public OrderService(OrderRepositoryPort orders, CustomerPort customers, CatalogPort catalog, NotificationPort notifications) {
        this.orders = orders;
        this.customers = customers;
        this.catalog = catalog;
        this.notifications = notifications;
    }

    @Transactional
    public Order create(String customerId, String actor, String correlationId) {
        CustomerPort.CustomerStatus customer = customers.findCustomer(customerId, correlationId);
        if (!customer.eligible()) {
            throw new DomainException("customer_not_eligible", "Customer cannot create orders");
        }
        if (orders.existsActiveOrderForCustomer(customerId)) {
            throw new DomainException("active_order_exists", "Customer already has an active order");
        }
        return orders.save(Order.draft(customerId), actor, correlationId);
    }

    @Transactional(readOnly = true)
    public Order get(UUID id) {
        return orders.findById(id).orElseThrow(() -> new DomainException("order_not_found", "Order not found"));
    }

    @Transactional(readOnly = true)
    public List<Order> find(String customerId) {
        return customerId == null || customerId.isBlank() ? orders.findAll() : orders.findByCustomerId(customerId);
    }

    @Transactional
    public Order addItem(UUID id, String productId, int quantity, String actor, String correlationId) {
        Order order = get(id);
        CatalogPort.ProductStatus product = catalog.findProduct(productId, correlationId);
        if (!product.sellable()) {
            throw new DomainException("product_not_available", "Product is not available");
        }
        order.addItem(productId, quantity, product.currentPrice());
        return orders.save(order, actor, correlationId);
    }

    @Transactional
    public Order removeItem(UUID id, UUID itemId, String actor, String correlationId) {
        Order order = get(id);
        order.removeItem(itemId);
        return orders.save(order, actor, correlationId);
    }

    @Transactional
    public Order confirm(UUID id, String actor, String correlationId) {
        Order order = get(id);
        List<ProductPrice> prices = order.items().stream()
                .map(item -> {
                    CatalogPort.ProductStatus product = catalog.findProduct(item.productId(), correlationId);
                    if (!product.sellable()) {
                        throw new DomainException("product_not_available", "Product is not available");
                    }
                    return new ProductPrice(item.productId(), product.currentPrice());
                })
                .toList();
        order.confirm(prices);
        Order saved = orders.save(order, actor, correlationId);
        notifications.publish("ORDER_CONFIRMED", saved.id(), saved.customerId(), correlationId);
        return saved;
    }

    @Transactional
    public Order cancel(UUID id, String actor, String correlationId) {
        Order order = get(id);
        order.cancel("REQUESTED_BY_CUSTOMER");
        Order saved = orders.save(order, actor, correlationId);
        notifications.publish("ORDER_CANCELLED", saved.id(), saved.customerId(), correlationId);
        return saved;
    }

    @Transactional
    public Order saveAfterPayment(Order order, String correlationId) {
        return orders.save(order, "payment-system", correlationId);
    }
}
