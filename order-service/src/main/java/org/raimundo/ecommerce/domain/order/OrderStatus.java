package org.raimundo.ecommerce.domain.order;

public enum OrderStatus {
    DRAFT,
    CONFIRMED,
    PAYMENT_PENDING,
    PAYMENT_APPROVED,
    PAYMENT_REJECTED,
    CANCELLED
}
