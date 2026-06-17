package org.raimundo.ecommerce.infrastructure.persistence.order;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {
    @Id
    public UUID id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    public OrderEntity order;
    public String productId;
    public int quantity;
    public BigDecimal unitPrice;
    public BigDecimal lineTotal;
    public Instant createdAt;
    public Instant updatedAt;
}
