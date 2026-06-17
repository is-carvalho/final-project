package org.raimundo.ecommerce.application.ports;

import org.raimundo.ecommerce.domain.common.Money;

public interface CatalogPort {
    ProductStatus findProduct(String productId, String correlationId);

    record ProductStatus(String productId, boolean exists, boolean available, Money currentPrice) {
        public boolean sellable() {
            return exists && available;
        }
    }
}
