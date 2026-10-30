package com.smartretail.shop;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Repayments are made against the balance, not against one order, so they clear the oldest
 * orders first. That tells the admin which credit orders are still open.
 */
final class CreditAllocation {

    private CreditAllocation() {
    }

    /** What is still due on each order, in the same order as {@code totalsOldestFirst}. */
    static List<BigDecimal> dues(List<BigDecimal> totalsOldestFirst, BigDecimal paid) {
        List<BigDecimal> dues = new ArrayList<>(totalsOldestFirst.size());
        BigDecimal left = paid.max(BigDecimal.ZERO);
        for (BigDecimal total : totalsOldestFirst) {
            BigDecimal covered = left.min(total);
            dues.add(total.subtract(covered));
            left = left.subtract(covered);
        }
        return dues;
    }
}
