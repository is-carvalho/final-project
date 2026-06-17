package org.raimundo.ecommerce.infrastructure.persistence.order;

import org.raimundo.ecommerce.domain.common.Money;
import org.raimundo.ecommerce.domain.order.Order;
import org.raimundo.ecommerce.domain.order.OrderItem;

import java.math.BigDecimal;
import java.time.Instant;

class OrderMapper {
    private OrderMapper() {
    }

    static Order toDomain(OrderEntity entity) {
        return Order.restore(entity.id, entity.customerId, entity.status,
                entity.items.stream()
                        .map(item -> new OrderItem(item.id, item.productId, item.quantity,
                                item.unitPrice == null ? null : new Money(item.unitPrice, entity.currency)))
                        .toList(),
                new Money(entity.totalAmount, entity.currency), entity.paymentRejectionCount,
                entity.cancellationReason, entity.createdAt, entity.updatedAt);
    }

    static OrderEntity toEntity(Order order, OrderEntity entity, String actor, String correlationId) {
        OrderEntity target = entity == null ? new OrderEntity() : entity;
        target.id = order.id();
        target.customerId = order.customerId();
        target.status = order.status();
        target.totalAmount = order.total().amount();
        target.currency = order.total().currency();
        target.paymentRejectionCount = order.paymentRejectionCount();
        target.cancellationReason = order.cancellationReason();
        target.createdAt = order.createdAt();
        target.updatedAt = order.updatedAt();
        target.createdBy = target.createdBy == null ? actor : target.createdBy;
        target.updatedBy = actor;
        target.correlationId = correlationId;
        target.items.clear();
        for (OrderItem item : order.items()) {
            OrderItemEntity child = new OrderItemEntity();
            child.id = item.id();
            child.order = target;
            child.productId = item.productId();
            child.quantity = item.quantity();
            child.unitPrice = item.unitPrice() == null ? null : item.unitPrice().amount();
            child.lineTotal = item.lineTotal() == null ? BigDecimal.ZERO : item.lineTotal().amount();
            child.createdAt = Instant.now();
            child.updatedAt = Instant.now();
            target.items.add(child);
        }
        return target;
    }
}
