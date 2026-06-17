package org.raimundo.ecommerce.application.ports;

import java.util.UUID;

public interface NotificationPort {
    void publish(String eventType, UUID orderId, String customerId, String correlationId);
}
