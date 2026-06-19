package org.raimundo.ecommerce.interfaces.rest;

import jakarta.validation.Valid;
import org.raimundo.ecommerce.application.idempotency.IdempotencyService;
import org.raimundo.ecommerce.application.payment.PaymentService;
import org.raimundo.ecommerce.interfaces.dto.PaymentDtos;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    @PostMapping
    ResponseEntity<PaymentDtos.PaymentResponse> initiate(@Valid @RequestBody PaymentDtos.InitiatePaymentRequest request,
                                                         @RequestHeader("Idempotency-Key") String key,
                                                         @RequestHeader("Correlation-Id") String correlationId) {
        IdempotencyService.requireKey(key);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PaymentDtos.PaymentResponse.from(payments.initiate(request.orderId(), key, correlationId), correlationId));
    }

    @GetMapping("/{id}")
    PaymentDtos.PaymentResponse get(@PathVariable UUID id) {
        return PaymentDtos.PaymentResponse.from(payments.get(id));
    }

    @PostMapping("/{id}/callback")
    PaymentDtos.PaymentResponse callback(@PathVariable UUID id, @Valid @RequestBody PaymentDtos.PaymentCallbackRequest request,
                                         @RequestHeader("Idempotency-Key") String key,
                                         @RequestHeader("Correlation-Id") String correlationId) {
        IdempotencyService.requireKey(key);
        return PaymentDtos.PaymentResponse.from(payments.process(id, request.providerEventId(), request.outcome(),
                request.providerTransactionId(), request.detail(), correlationId), correlationId);
    }
}
