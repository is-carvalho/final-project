package org.raimundo.ecommerce.application.order;

import org.raimundo.ecommerce.domain.order.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {
    Order save(Order order, String actor, String correlationId);
    Optional<Order> findById(UUID id);
    List<Order> findByCustomerId(String customerId);
    List<Order> findAll();
    boolean existsActiveOrderForCustomer(String customerId);
}
