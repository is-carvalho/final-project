package org.raimundo.ecommerce.infrastructure.persistence.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataPaymentResultEventRepository extends JpaRepository<PaymentResultEventEntity, UUID> {
    boolean existsByProviderEventId(String providerEventId);
}
