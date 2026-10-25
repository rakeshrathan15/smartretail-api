package com.smartretail.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartretail.catalog.Product;
import com.smartretail.coupon.Coupon;
import com.smartretail.coupon.Coupon.DiscountType;
import com.smartretail.order.OrderDtos.CartItem;
import com.smartretail.order.OrderDtos.CreditInfo;
import com.smartretail.order.OrderDtos.Quote;
import com.smartretail.settings.StoreSettings;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CheckoutPricingTest {

    private static final Instant NOW = Instant.parse("2026-10-25T10:00:00Z");
    private static final StoreSettings.Checkout SETTINGS = new StoreSettings.Checkout(
            true, new BigDecimal("500"), new BigDecimal("5000"), new BigDecimal("1000"), "shop@upi");

    private final UUID colaId = UUID.randomUUID();
    private final UUID waterId = UUID.randomUUID();
    private final Product cola = product(colaId, "Cola", "420", 80);
    private final Product water = product(waterId, "Water", "240", 150);

    private static Product product(UUID id, String name, String price, int stock) {
        Product p = new Product(name, name.toLowerCase(), new BigDecimal(price));
        ReflectionTestUtils.setField(p, "id", id);
        p.setStockQuantity(stock);
        return p;
    }

    private static Coupon coupon(DiscountType type, String value, String minOrder, String maxDiscount) {
        Coupon c = new Coupon("TEST");
        c.setDiscountType(type);
        c.setDiscountValue(new BigDecimal(value));
        c.setMinOrderValue(new BigDecimal(minOrder));
        c.setMaxDiscount(maxDiscount == null ? null : new BigDecimal(maxDiscount));
        return c;
    }

    private Quote quote(Map<UUID, Integer> items, Coupon coupon, StoreSettings.Checkout settings) {
        return CheckoutPricing.quote(items, Map.of(colaId, cola, waterId, water), Map.of(),
                coupon == null ? null : "TEST", coupon, settings, null, NOW);
    }

    @Test
    void appliesSlabsPerLineAndChargesDeliveryBelowTheFreeThreshold() throws Exception {
        addSlab(cola, 10, 24, "378");
        Quote q = quote(Map.of(colaId, 10, waterId, 2), null, SETTINGS);

        assertThat(q.subtotal()).isEqualByComparingTo("4260"); // 10 × 378 + 2 × 240
        assertThat(q.lines()).filteredOn(l -> l.productId().equals(colaId)).singleElement()
                .satisfies(l -> {
                    assertThat(l.pricePerCase()).isEqualByComparingTo("378");
                    assertThat(l.saving()).isEqualByComparingTo("420");
                });
        assertThat(q.deliveryCharge()).isEqualByComparingTo("500");
        assertThat(q.amountForFreeDelivery()).isEqualByComparingTo("740");
        assertThat(q.total()).isEqualByComparingTo("4760");
        assertThat(q.totalCases()).isEqualTo(12);
        assertThat(q.canPlaceOrder()).isTrue();
    }

    @Test
    void deliveryIsFreeOnceTheOrderReachesTheThreshold() {
        Quote q = quote(Map.of(colaId, 12), null, SETTINGS); // 12 × 420 = 5040
        assertThat(q.deliveryCharge()).isEqualByComparingTo("0");
        assertThat(q.amountForFreeDelivery()).isNull();
        assertThat(q.total()).isEqualByComparingTo("5040");
    }

    @Test
    void percentageCouponIsCappedAndCountsTowardsFreeDelivery() {
        // 12 × 420 = 5040; 5% = 252, capped at 200 → 4840, now below free delivery.
        Quote q = quote(Map.of(colaId, 12), coupon(DiscountType.PERCENTAGE, "5", "2000", "200"), SETTINGS);
        assertThat(q.couponDiscount()).isEqualByComparingTo("200");
        assertThat(q.couponProblem()).isNull();
        assertThat(q.deliveryCharge()).isEqualByComparingTo("500");
        assertThat(q.total()).isEqualByComparingTo("5340");
    }

    @Test
    void couponProblemsAreReportedWithoutBlockingTheCart() {
        Quote belowMinimum = quote(Map.of(waterId, 5), coupon(DiscountType.FIXED, "100", "1500", null), SETTINGS);
        assertThat(belowMinimum.couponDiscount()).isEqualByComparingTo("0");
        assertThat(belowMinimum.couponProblem()).isEqualTo("Add items worth ₹1,500 or more to use this coupon");
        assertThat(belowMinimum.canPlaceOrder()).isTrue();

        Coupon expired = coupon(DiscountType.FIXED, "100", "0", null);
        expired.setValidUntil(NOW.minusSeconds(60));
        assertThat(quote(Map.of(waterId, 5), expired, SETTINGS).couponProblem()).isEqualTo("This coupon has expired");

        Quote unknown = CheckoutPricing.quote(Map.of(waterId, 5), Map.of(waterId, water), Map.of(), "NOPE", null,
                SETTINGS, null, NOW);
        assertThat(unknown.couponProblem()).isEqualTo("This coupon code is not valid");
    }

    @Test
    void fixedCouponNeverExceedsTheSubtotal() {
        Coupon big = coupon(DiscountType.FIXED, "10000", "0", null);
        assertThat(big.discountFor(new BigDecimal("480"))).isEqualByComparingTo("480");
    }

    @Test
    void blocksOrdersBelowMinimumOrOverStockOrWhenClosed() {
        Quote small = quote(Map.of(waterId, 2), null, SETTINGS);
        assertThat(small.canPlaceOrder()).isFalse();
        assertThat(small.problems()).containsExactly("Minimum order is ₹1,000. Add ₹520 more.");

        Quote tooMany = quote(Map.of(colaId, 81), null, SETTINGS);
        assertThat(tooMany.problems()).containsExactly("Only 80 cases of Cola are left.");
        assertThat(tooMany.lines().getFirst().problem()).isEqualTo("Only 80 cases left");

        StoreSettings.Checkout closed = new StoreSettings.Checkout(false, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, "");
        assertThat(quote(Map.of(colaId, 1), null, closed).problems().getFirst()).startsWith("The store is closed");
    }

    @Test
    void unavailableProductsAreExcludedAndBlockTheOrder() {
        cola.setActive(false);
        Quote q = quote(Map.of(colaId, 3, waterId, 5), null, SETTINGS);
        assertThat(q.subtotal()).isEqualByComparingTo("1200");
        assertThat(q.canPlaceOrder()).isFalse();
        assertThat(q.problems()).anyMatch(p -> p.contains("Cola is no longer available"));
    }

    @Test
    void mergesRepeatedProductsBeforePricing() {
        Map<UUID, Integer> merged = CheckoutPricing.mergeItems(List.of(
                new CartItem(colaId, 4), new CartItem(waterId, 1), new CartItem(colaId, 6)));
        assertThat(merged).containsExactly(Map.entry(colaId, 10), Map.entry(waterId, 1));
    }

    @Test
    void passesCreditThrough() {
        CreditInfo credit = new CreditInfo(new BigDecimal("5000"), new BigDecimal("1200"), new BigDecimal("3800"));
        Quote q = CheckoutPricing.quote(Map.of(colaId, 3), Map.of(colaId, cola), Map.of(), null, null, SETTINGS,
                credit, NOW);
        assertThat(q.credit()).isEqualTo(credit);
    }

    private static void addSlab(Product product, int min, Integer max, String price) throws Exception {
        // Product.addSlab is package-private to the catalog; tests outside it go through the method by reflection.
        var method = Product.class.getDeclaredMethod("addSlab", int.class, Integer.class, BigDecimal.class);
        method.setAccessible(true);
        method.invoke(product, min, max, new BigDecimal(price));
    }
}
