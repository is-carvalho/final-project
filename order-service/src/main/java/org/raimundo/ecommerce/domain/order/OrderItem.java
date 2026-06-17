package org.raimundo.ecommerce.domain.order;

import org.raimundo.ecommerce.domain.common.DomainException;
import org.raimundo.ecommerce.domain.common.Money;

import java.util.UUID;

public class OrderItem {
    private final UUID id;
    private final String productId;
    private int quantity;
    private Money unitPrice;

    public OrderItem(UUID id, String productId, int quantity, Money unitPrice) {
        if (productId == null || productId.isBlank()) {
            throw new DomainException("invalid_product", "Product is required");
        }
        if (quantity < 1) {
            throw new DomainException("invalid_quantity", "Quantity must be greater than zero");
        }
        this.id = id == null ? UUID.randomUUID() : id;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public UUID id() {
        return id;
    }

    public String productId() {
        return productId;
    }

    public int quantity() {
        return quantity;
    }

    public Money unitPrice() {
        return unitPrice;
    }

    public Money lineTotal() {
        return unitPrice == null ? null : unitPrice.multiply(quantity);
    }

    void increase(int amount) {
        if (amount < 1) {
            throw new DomainException("invalid_quantity", "Quantity must be greater than zero");
        }
        quantity += amount;
    }

    void reprice(Money currentPrice) {
        unitPrice = currentPrice;
    }
}
