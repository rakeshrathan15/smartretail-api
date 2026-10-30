package com.smartretail.shop;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreditAllocationTest {

    private static final List<BigDecimal> ORDERS = List.of(new BigDecimal("1000"), new BigDecimal("500"), new BigDecimal("200"));

    private static List<BigDecimal> dues(String paid) {
        return CreditAllocation.dues(ORDERS, new BigDecimal(paid));
    }

    @Test
    void paymentsClearTheOldestOrdersFirst() {
        assertThat(dues("0")).containsExactly(new BigDecimal("1000"), new BigDecimal("500"), new BigDecimal("200"));
        assertThat(dues("1200")).containsExactly(new BigDecimal("0"), new BigDecimal("300"), new BigDecimal("200"));
        assertThat(dues("1700")).containsExactly(new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"));
    }

    @Test
    void anAdvanceLeavesNothingDue() {
        assertThat(dues("5000")).allMatch(due -> due.signum() == 0);
        assertThat(CreditAllocation.dues(List.of(), new BigDecimal("100"))).isEmpty();
    }
}
