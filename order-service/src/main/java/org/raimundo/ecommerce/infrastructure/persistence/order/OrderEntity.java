package org.raimundo.ecommerce.infrastructure.persistence.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.raimundo.ecommerce.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    public UUID id;
    public String customerId;
    @Enumerated(EnumType.STRING)
    public OrderStatus status;
    public BigDecimal totalAmount;
    public String currency;
    public int paymentRejectionCount;
    public String cancellationReason;
    public Instant createdAt;
    public Instant updatedAt;
    public String createdBy;
    public String updatedBy;
    public String correlationId;
    @Version
    public long version;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<OrderItemEntity> items = new ArrayList<>();
}
