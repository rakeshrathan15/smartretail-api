package com.smartretail.shop.service;

import com.smartretail.common.exception.ApiException;
import com.smartretail.common.exception.ErrorCode;
import com.smartretail.shop.dto.request.PaymentRequest;
import com.smartretail.shop.dto.response.Ledger;
import com.smartretail.shop.dto.response.OpenOrder;
import com.smartretail.shop.entity.ShopPayment;
import com.smartretail.shop.repository.ShopDirectory;
import com.smartretail.shop.repository.ShopLedgerDirectory;
import com.smartretail.shop.repository.ShopLedgerDirectory.CreditOrder;
import com.smartretail.shop.repository.ShopPaymentRepository;
import com.smartretail.shop.repository.ShopRepository;
import com.smartretail.shop.support.CreditAllocation;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Money shops pay back: recording and voiding repayments, and each shop's statement. */
@RequiredArgsConstructor
@Slf4j
@Service
@Transactional
public class ShopLedgerService {

    private final ShopRepository shops;
    private final ShopPaymentRepository payments;
    private final ShopDirectory directory;
    private final ShopLedgerDirectory ledgerDirectory;
    private final Clock clock = Clock.systemUTC();

    /** A shop that was turned off can still pay what it owes. More than it owes counts as an advance. */
    public Ledger recordPayment(UUID shopId, PaymentRequest request, UUID adminId) {
        lockShop(shopId);
        payments.saveAndFlush(new ShopPayment(shopId, request.amount(), request.method(),
                request.reference() == null || request.reference().isBlank() ? null : request.reference().trim(),
                adminId));
        log.info("Shop repayment recorded: shopId={} amount={} method={} adminId={}", shopId, request.amount(),
                request.method(), adminId);
        return ledger(shopId);
    }

    /** Takes a wrong entry or bounced cheque out of the balance; it stays in the history. */
    public Ledger voidPayment(UUID shopId, UUID paymentId, UUID adminId) {
        lockShop(shopId);
        ShopPayment payment = payments.findByIdAndShopId(paymentId, shopId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND, "Payment not found"));
        if (payment.isVoided()) {
            throw new ApiException(ErrorCode.PAYMENT_ALREADY_VOIDED, "This payment has already been voided");
        }
        payment.voidBy(adminId, clock.instant());
        payments.flush();
        log.info("Shop repayment voided: shopId={} paymentId={} adminId={}", shopId, paymentId, adminId);
        return ledger(shopId);
    }

    @Transactional(readOnly = true)
    public Ledger ledger(UUID shopId) {
        var shop = directory.get(shopId);
        BigDecimal paid = ledgerDirectory.paidTotal(shopId);
        List<CreditOrder> orders = ledgerDirectory.creditOrders(shopId);
        List<BigDecimal> dues = CreditAllocation.dues(orders.stream().map(CreditOrder::total).toList(), paid);
        List<OpenOrder> open = new ArrayList<>();
        BigDecimal ordersTotal = BigDecimal.ZERO;
        for (int i = 0; i < orders.size(); i++) {
            CreditOrder order = orders.get(i);
            ordersTotal = ordersTotal.add(order.total());
            if (dues.get(i).signum() > 0) {
                open.add(new OpenOrder(order.id(), order.orderNumber(), order.createdAt().toInstant(), order.status(),
                        order.total(), dues.get(i)));
            }
        }
        return new Ledger(shop, ordersTotal, paid, open, ledgerDirectory.recentPayments(shopId));
    }

    /** One change at a time per shop, and the 404 for a shop that does not exist. */
    private void lockShop(UUID shopId) {
        shops.lockById(shopId).orElseThrow(() -> new ApiException(ErrorCode.SHOP_NOT_FOUND, "Shop not found"));
    }
}
