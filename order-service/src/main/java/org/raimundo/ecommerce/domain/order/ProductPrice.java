package org.raimundo.ecommerce.domain.order;

import org.raimundo.ecommerce.domain.common.Money;

public record ProductPrice(String productId, Money price) {
}
