package org.raimundo.ecommerce.domain.order;

import org.raimundo.ecommerce.domain.common.DomainException;
import org.raimundo.ecommerce.domain.common.Money;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Order {
    public static final int MAX_PAYMENT_REJECTIONS = 3;

    private final UUID id;
    private final String customerId;
    private OrderStatus status;
    private final List<OrderItem> items;
    private Money total;
    private int paymentRejectionCount;
    private String cancellationReason;
    private final Instant createdAt;
    private Instant updatedAt;

    private Order(UUID id, String customerId, OrderStatus status, List<OrderItem> items, Money total,
                  int paymentRejectionCount, String cancellationReason, Instant createdAt, Instant updatedAt) {
        if (customerId == null || customerId.isBlank()) {
            throw new DomainException("invalid_customer", "Customer is required");
        }
        this.id = id == null ? UUID.randomUUID() : id;
        this.customerId = customerId;
        this.status = status == null ? OrderStatus.DRAFT : status;
        this.items = new ArrayList<>(items == null ? List.of() : items);
        this.total = total == null ? Money.zero("BRL") : total;
        this.paymentRejectionCount = paymentRejectionCount;
        this.cancellationReason = cancellationReason;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
    }

    public static Order draft(String customerId) {
        return new Order(UUID.randomUUID(), customerId, OrderStatus.DRAFT, List.of(), Money.zero("BRL"), 0, null, Instant.now(), Instant.now());
    }

    public static Order restore(UUID id, String customerId, OrderStatus status, List<OrderItem> items, Money total,
                                int paymentRejectionCount, String cancellationReason, Instant createdAt, Instant updatedAt) {
        return new Order(id, customerId, status, items, total, paymentRejectionCount, cancellationReason, createdAt, updatedAt);
    }

    public void addItem(String productId, int quantity, Money currentPrice) {
        requireDraft();
        Optional<OrderItem> existing = items.stream().filter(item -> item.productId().equals(productId)).findFirst();
        if (existing.isPresent()) {
            existing.get().increase(quantity);
        } else {
            items.add(new OrderItem(UUID.randomUUID(), productId, quantity, currentPrice));
        }
        touch();
    }

    public void removeItem(UUID itemId) {
        requireDraft();
        if (!items.removeIf(item -> item.id().equals(itemId))) {
            throw new DomainException("item_not_found", "Order item not found");
        }
        touch();
    }

    public void confirm(List<ProductPrice> prices) {
        requireDraft();
        if (items.isEmpty()) {
            throw new DomainException("empty_order", "Cannot confirm an empty order");
        }
        for (OrderItem item : items) {
            ProductPrice price = prices.stream()
                    .filter(candidate -> candidate.productId().equals(item.productId()))
                    .findFirst()
                    .orElseThrow(() -> new DomainException("product_not_found", "Product price not found"));
            item.reprice(price.price());
        }
        total = items.stream()
                .map(OrderItem::lineTotal)
                .reduce(Money.zero(items.getFirst().unitPrice().currency()), Money::add);
        status = OrderStatus.CONFIRMED;
        touch();
    }

    public void cancel(String reason) {
        if (status == OrderStatus.PAYMENT_APPROVED || status == OrderStatus.CANCELLED) {
            throw new DomainException("invalid_transition", "Order cannot be cancelled in its current state");
        }
        status = OrderStatus.CANCELLED;
        cancellationReason = reason == null || reason.isBlank() ? "REQUESTED_BY_CUSTOMER" : reason;
        touch();
    }

    public int startPayment() {
        if (status != OrderStatus.CONFIRMED && status != OrderStatus.PAYMENT_REJECTED) {
            throw new DomainException("invalid_transition", "Payment can only start for confirmed or retryable rejected orders");
        }
        if (paymentRejectionCount >= MAX_PAYMENT_REJECTIONS) {
            throw new DomainException("payment_retry_limit", "Payment retry limit exceeded");
        }
        status = OrderStatus.PAYMENT_PENDING;
        touch();
        return paymentRejectionCount + 1;
    }

    public void approvePayment() {
        if (status != OrderStatus.PAYMENT_PENDING) {
            throw new DomainException("invalid_transition", "Only pending payments can be approved");
        }
        status = OrderStatus.PAYMENT_APPROVED;
        touch();
    }

    public void rejectPayment(String reason) {
        if (status != OrderStatus.PAYMENT_PENDING) {
            throw new DomainException("invalid_transition", "Only pending payments can be rejected");
        }
        paymentRejectionCount++;
        if (paymentRejectionCount >= MAX_PAYMENT_REJECTIONS) {
            status = OrderStatus.CANCELLED;
            cancellationReason = "PAYMENT_RETRY_LIMIT_EXCEEDED";
        } else {
            status = OrderStatus.PAYMENT_REJECTED;
        }
        touch();
    }

    public boolean active() {
        return status == OrderStatus.DRAFT || status == OrderStatus.CONFIRMED
                || status == OrderStatus.PAYMENT_PENDING || status == OrderStatus.PAYMENT_REJECTED;
    }

    private void requireDraft() {
        if (status != OrderStatus.DRAFT) {
            throw new DomainException("order_not_editable", "Only draft orders are editable");
        }
    }

    private void touch() {
        updatedAt = Instant.now();
    }

    public UUID id() { return id; }
    public String customerId() { return customerId; }
    public OrderStatus status() { return status; }
    public List<OrderItem> items() { return List.copyOf(items); }
    public Money total() { return total; }
    public int paymentRejectionCount() { return paymentRejectionCount; }
    public String cancellationReason() { return cancellationReason; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
