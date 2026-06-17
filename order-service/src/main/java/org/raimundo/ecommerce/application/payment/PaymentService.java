package org.raimundo.ecommerce.application.payment;

import org.raimundo.ecommerce.application.order.OrderService;
import org.raimundo.ecommerce.application.ports.NotificationPort;
import org.raimundo.ecommerce.application.ports.PaymentGatewayPort;
import org.raimundo.ecommerce.domain.common.DomainException;
import org.raimundo.ecommerce.domain.order.Order;
import org.raimundo.ecommerce.domain.payment.PaymentResultEvent;
import org.raimundo.ecommerce.domain.payment.PaymentStatus;
import org.raimundo.ecommerce.domain.payment.PaymentTransaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepositoryPort payments;
    private final OrderService orders;
    private final PaymentGatewayPort gateway;
    private final NotificationPort notifications;

    public PaymentService(PaymentRepositoryPort payments, OrderService orders, PaymentGatewayPort gateway, NotificationPort notifications) {
        this.payments = payments;
        this.orders = orders;
        this.gateway = gateway;
        this.notifications = notifications;
    }

    @Transactional
    public PaymentTransaction initiate(UUID orderId, String idempotencyKey, String correlationId) {
        return payments.findByIdempotencyKey(idempotencyKey).orElseGet(() -> {
            Order order = orders.get(orderId);
            int attempt = order.startPayment();
            orders.saveAfterPayment(order, correlationId);
            PaymentTransaction payment = payments.save(PaymentTransaction.pending(order.id(), attempt, idempotencyKey, order.total()), correlationId);
            PaymentGatewayPort.GatewayResult result = gateway.initiate(payment.id(), order.id(), order.total(), idempotencyKey, correlationId);
            if (result.approved()) {
                return process(payment.id(), "gateway-" + payment.id(), "APPROVED", result.providerTransactionId(), result.detail(), correlationId);
            }
            if (result.rejected()) {
                return process(payment.id(), "gateway-" + payment.id(), "REJECTED", result.providerTransactionId(), result.detail(), correlationId);
            }
            return payment;
        });
    }

    @Transactional(readOnly = true)
    public PaymentTransaction get(UUID id) {
        return payments.findById(id).orElseThrow(() -> new DomainException("payment_not_found", "Payment not found"));
    }

    @Transactional
    public PaymentTransaction process(UUID paymentId, String providerEventId, String outcome, String providerTransactionId,
                                      String detail, String correlationId) {
        PaymentTransaction payment = get(paymentId);
        if (payments.eventExists(providerEventId)) {
            return payment;
        }
        Order order = orders.get(payment.orderId());
        if ("APPROVED".equals(outcome)) {
            payment.approve(providerTransactionId);
            order.approvePayment();
            notifications.publish("PAYMENT_APPROVED", order.id(), order.customerId(), correlationId);
        } else if ("REJECTED".equals(outcome)) {
            payment.reject(providerTransactionId, detail);
            order.rejectPayment(detail);
            if (order.status().name().equals("CANCELLED")) {
                notifications.publish("ORDER_CANCELLED", order.id(), order.customerId(), correlationId);
            }
        } else {
            throw new DomainException("invalid_payment_outcome", "Payment outcome must be APPROVED or REJECTED");
        }
        payments.save(payment, correlationId);
        orders.saveAfterPayment(order, correlationId);
        payments.saveEvent(PaymentResultEvent.received(payment.id(), providerEventId, outcome, correlationId,
                payment.status() == PaymentStatus.APPROVED ? "approved" : "rejected"));
        return payment;
    }
}
