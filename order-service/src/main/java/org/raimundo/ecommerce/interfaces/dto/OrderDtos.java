package org.raimundo.ecommerce.interfaces.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.raimundo.ecommerce.domain.order.Order;
import org.raimundo.ecommerce.domain.order.OrderItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class OrderDtos {
    private OrderDtos() {
    }

    public record CreateOrderRequest(@NotBlank String customerId) {
    }

    public record AddOrderItemRequest(@NotBlank String productId, @Min(1) int quantity) {
    }

    public record OrderResponse(UUID id, String customerId, String status, List<OrderItemResponse> items,
                                BigDecimal totalAmount, String currency, int paymentRejectionCount) {
        public static OrderResponse from(Order order) {
            return new OrderResponse(order.id(), order.customerId(), order.status().name(),
                    order.items().stream().map(OrderItemResponse::from).toList(),
                    order.total().amount(), order.total().currency(), order.paymentRejectionCount());
        }
    }

    public record OrderItemResponse(UUID id, String productId, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(item.id(), item.productId(), item.quantity(),
                    item.unitPrice() == null ? null : item.unitPrice().amount(),
                    item.lineTotal() == null ? null : item.lineTotal().amount());
        }
    }
}
