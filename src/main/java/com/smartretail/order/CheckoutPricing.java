package com.smartretail.order;

import com.smartretail.catalog.Product;
import com.smartretail.common.Money;
import com.smartretail.coupon.Coupon;
import com.smartretail.order.OrderDtos.CartItem;
import com.smartretail.order.OrderDtos.CreditInfo;
import com.smartretail.order.OrderDtos.Quote;
import com.smartretail.order.OrderDtos.QuoteLine;
import com.smartretail.settings.StoreSettings;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The one place prices are calculated, for both the cart preview and placing the order.
 *
 * <ol>
 *   <li>Each line pays the bulk slab price for its case count.</li>
 *   <li>A coupon discounts the subtotal (percentage capped at its max, or a fixed amount).</li>
 *   <li>Delivery is free when subtotal minus discount reaches "free delivery above".</li>
 *   <li>The subtotal before discount must reach the minimum order value.</li>
 * </ol>
 */
final class CheckoutPricing {

    private CheckoutPricing() {
    }

    /** Adds up repeated products and keeps the first-seen order. */
    static Map<UUID, Integer> mergeItems(List<CartItem> items) {
        Map<UUID, Integer> merged = new LinkedHashMap<>();
        items.forEach(i -> merged.merge(i.productId(), i.quantity(), Integer::sum));
        return merged;
    }

    static String normalizeCoupon(String code) {
        return code == null || code.isBlank() ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * @param coupon     the coupon matching {@code couponCode}, or null if none was entered or it does not exist
     * @param credit     the shop's credit, or null when it cannot order on credit
     */
    static Quote quote(Map<UUID, Integer> items, Map<UUID, Product> products, Map<UUID, String> thumbnails,
            String couponCode, Coupon coupon, StoreSettings.Checkout settings, CreditInfo credit, Instant now) {
        List<QuoteLine> lines = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalCases = 0;

        for (Map.Entry<UUID, Integer> item : items.entrySet()) {
            Product p = products.get(item.getKey());
            int quantity = item.getValue();
            if (p == null || !isAvailable(p)) {
                String problem = "This product is no longer available";
                lines.add(new QuoteLine(item.getKey(), p == null ? null : p.getSlug(),
                        p == null ? "Unavailable product" : p.getName(), null, null, 0, quantity, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, problem));
                problems.add((p == null ? "A product" : p.getName()) + " is no longer available. Remove it to continue.");
                continue;
            }
            BigDecimal pricePerCase = p.pricePerCaseFor(quantity);
            BigDecimal lineTotal = pricePerCase.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal saving = p.getPrice().subtract(pricePerCase).multiply(BigDecimal.valueOf(quantity))
                    .setScale(2, RoundingMode.HALF_UP);
            String problem = null;
            if (p.getStockQuantity() <= 0) {
                problem = "Out of stock";
                problems.add(p.getName() + " is out of stock. Remove it to continue.");
            } else if (quantity > p.getStockQuantity()) {
                problem = "Only " + p.getStockQuantity() + " cases left";
                problems.add("Only " + p.getStockQuantity() + " cases of " + p.getName() + " are left.");
            }
            lines.add(new QuoteLine(p.getId(), p.getSlug(), p.getName(), p.getBrand(), thumbnails.get(p.getId()),
                    p.getUnitsPerCase(), quantity, p.getPrice(), pricePerCase, lineTotal, saving,
                    Math.max(p.getStockQuantity(), 0), problem));
            subtotal = subtotal.add(lineTotal);
            totalCases += quantity;
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        BigDecimal discount = BigDecimal.ZERO.setScale(2);
        String couponProblem = null;
        if (couponCode != null) {
            if (coupon == null) {
                couponProblem = "This coupon code is not valid";
            } else {
                couponProblem = coupon.problemFor(subtotal, now).orElse(null);
                if (couponProblem == null) {
                    discount = coupon.discountFor(subtotal);
                }
            }
        }

        BigDecimal afterDiscount = subtotal.subtract(discount);
        boolean freeDeliveryOffered = settings.freeDeliveryAbove().signum() > 0;
        boolean freeDelivery = freeDeliveryOffered && afterDiscount.compareTo(settings.freeDeliveryAbove()) >= 0;
        BigDecimal delivery = items.isEmpty() || freeDelivery
                ? BigDecimal.ZERO.setScale(2)
                : settings.deliveryCharge().setScale(2, RoundingMode.HALF_UP);
        BigDecimal amountForFreeDelivery = freeDeliveryOffered && !freeDelivery && delivery.signum() > 0
                ? settings.freeDeliveryAbove().subtract(afterDiscount).setScale(2, RoundingMode.HALF_UP)
                : null;
        BigDecimal total = afterDiscount.add(delivery).setScale(2, RoundingMode.HALF_UP);

        if (items.isEmpty()) {
            problems.add("Your cart is empty");
        } else if (subtotal.compareTo(settings.minOrderValue()) < 0) {
            problems.add("Minimum order is " + Money.format(settings.minOrderValue()) + ". Add "
                    + Money.format(settings.minOrderValue().subtract(subtotal)) + " more.");
        }
        if (!settings.storeOpen()) {
            problems.add(0, "The store is closed right now. You can place your order when it opens.");
        }

        return new Quote(lines, totalCases, subtotal, couponCode, discount, couponProblem, delivery,
                settings.freeDeliveryAbove(), amountForFreeDelivery, total, settings.minOrderValue(),
                settings.storeOpen(), settings.upiId(), credit, List.copyOf(problems), problems.isEmpty());
    }

    static boolean isAvailable(Product p) {
        return p.isActive() && (p.getCategory() == null || p.getCategory().isActive());
    }
}
