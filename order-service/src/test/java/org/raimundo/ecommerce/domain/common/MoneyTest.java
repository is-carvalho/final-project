package org.raimundo.ecommerce.domain.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {
    @Test
    void roundsAddsAndMultipliesMoney() {
        Money ten = new Money(new BigDecimal("10.005"), "BRL");
        Money five = new Money(new BigDecimal("5.004"), "BRL");

        assertThat(ten.amount()).isEqualByComparingTo("10.01");
        assertThat(ten.add(five).amount()).isEqualByComparingTo("15.01");
        assertThat(five.multiply(3).amount()).isEqualByComparingTo("15.00");
        assertThat(Money.zero("BRL").currency()).isEqualTo("BRL");
    }

    @Test
    void rejectsCurrencyMismatchAndNulls() {
        Money brl = new Money(BigDecimal.TEN, "BRL");

        assertThatThrownBy(() -> brl.add(new Money(BigDecimal.ONE, "USD")))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new Money(null, "BRL"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, null))
                .isInstanceOf(NullPointerException.class);
    }
}
