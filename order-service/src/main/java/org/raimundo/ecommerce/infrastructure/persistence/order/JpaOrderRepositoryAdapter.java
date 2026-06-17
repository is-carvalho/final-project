package org.raimundo.ecommerce.infrastructure.persistence.order;

import org.raimundo.ecommerce.application.order.OrderRepositoryPort;
import org.raimundo.ecommerce.domain.order.Order;
import org.raimundo.ecommerce.domain.order.OrderStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaOrderRepositoryAdapter implements OrderRepositoryPort {
    private static final List<OrderStatus> ACTIVE_STATUSES = List.of(OrderStatus.DRAFT, OrderStatus.CONFIRMED,
            OrderStatus.PAYMENT_PENDING, OrderStatus.PAYMENT_REJECTED);
    private final SpringDataOrderRepository repository;

    public JpaOrderRepositoryAdapter(SpringDataOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public Order save(Order order, String actor, String correlationId) {
        OrderEntity existing = repository.findById(order.id()).orElse(null);
        return OrderMapper.toDomain(repository.save(OrderMapper.toEntity(order, existing, actor, correlationId)));
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return repository.findById(id).map(OrderMapper::toDomain);
    }

    @Override
    public List<Order> findByCustomerId(String customerId) {
        return repository.findByCustomerId(customerId).stream().map(OrderMapper::toDomain).toList();
    }

    @Override
    public List<Order> findAll() {
        return repository.findAll().stream().map(OrderMapper::toDomain).toList();
    }

    @Override
    public boolean existsActiveOrderForCustomer(String customerId) {
        return repository.existsByCustomerIdAndStatusIn(customerId, ACTIVE_STATUSES);
    }
}
