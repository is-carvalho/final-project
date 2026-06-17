package org.raimundo.ecommerce.interfaces.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.raimundo.ecommerce.application.idempotency.IdempotencyService;
import org.raimundo.ecommerce.application.order.OrderService;
import org.raimundo.ecommerce.interfaces.dto.OrderDtos;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orders;
    private final IdempotencyService idempotency;
    private final ObjectMapper mapper;

    public OrderController(OrderService orders, IdempotencyService idempotency, ObjectMapper mapper) {
        this.orders = orders;
        this.idempotency = idempotency;
        this.mapper = mapper;
    }

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody OrderDtos.CreateOrderRequest request,
                             @RequestHeader("Idempotency-Key") String key,
                             @RequestHeader("Correlation-Id") String correlationId,
                             Authentication authentication) throws JsonProcessingException {
        String body = mapper.writeValueAsString(request);
        var replay = idempotency.replay("create-order", "-", caller(authentication), key, body);
        if (replay.isPresent()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(mapper.readTree(replay.get()));
        }
        var response = OrderDtos.OrderResponse.from(orders.create(request.customerId(), caller(authentication), correlationId));
        idempotency.remember("create-order", "-", caller(authentication), key, body, 201, mapper.writeValueAsString(response));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    List<OrderDtos.OrderResponse> find(@RequestParam(required = false) String customerId) {
        return orders.find(customerId).stream().map(OrderDtos.OrderResponse::from).toList();
    }

    @GetMapping("/{id}")
    OrderDtos.OrderResponse get(@PathVariable UUID id) {
        return OrderDtos.OrderResponse.from(orders.get(id));
    }

    @PostMapping("/{id}/items")
    OrderDtos.OrderResponse addItem(@PathVariable UUID id, @Valid @RequestBody OrderDtos.AddOrderItemRequest request,
                                    @RequestHeader("Idempotency-Key") String key,
                                    @RequestHeader("Correlation-Id") String correlationId,
                                    Authentication authentication) {
        IdempotencyService.requireKey(key);
        return OrderDtos.OrderResponse.from(orders.addItem(id, request.productId(), request.quantity(), caller(authentication), correlationId));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    OrderDtos.OrderResponse removeItem(@PathVariable UUID id, @PathVariable UUID itemId,
                                       @RequestHeader("Idempotency-Key") String key,
                                       @RequestHeader("Correlation-Id") String correlationId,
                                       Authentication authentication) {
        IdempotencyService.requireKey(key);
        return OrderDtos.OrderResponse.from(orders.removeItem(id, itemId, caller(authentication), correlationId));
    }

    @PostMapping("/{id}/confirm")
    OrderDtos.OrderResponse confirm(@PathVariable UUID id, @RequestHeader("Idempotency-Key") String key,
                                    @RequestHeader("Correlation-Id") String correlationId,
                                    Authentication authentication) {
        IdempotencyService.requireKey(key);
        return OrderDtos.OrderResponse.from(orders.confirm(id, caller(authentication), correlationId));
    }

    @DeleteMapping("/{id}")
    OrderDtos.OrderResponse cancel(@PathVariable UUID id, @RequestHeader("Idempotency-Key") String key,
                                   @RequestHeader("Correlation-Id") String correlationId,
                                   Authentication authentication) {
        IdempotencyService.requireKey(key);
        return OrderDtos.OrderResponse.from(orders.cancel(id, caller(authentication), correlationId));
    }

    private String caller(Authentication authentication) {
        return authentication == null ? "anonymous" : authentication.getName();
    }
}
