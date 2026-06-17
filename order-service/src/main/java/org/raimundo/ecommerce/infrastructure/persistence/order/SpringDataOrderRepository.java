package org.raimundo.ecommerce.infrastructure.persistence.order;

import org.raimundo.ecommerce.domain.order.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface SpringDataOrderRepository extends JpaRepository<OrderEntity, UUID> {
    List<OrderEntity> findByCustomerId(String customerId);
    boolean existsByCustomerIdAndStatusIn(String customerId, Collection<OrderStatus> statuses);
}
