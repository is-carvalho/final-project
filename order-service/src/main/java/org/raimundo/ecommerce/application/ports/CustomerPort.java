package org.raimundo.ecommerce.application.ports;

public interface CustomerPort {
    CustomerStatus findCustomer(String customerId, String correlationId);

    record CustomerStatus(String customerId, boolean exists, boolean active, boolean blocked) {
        public boolean eligible() {
            return exists && active && !blocked;
        }
    }
}
