package com.smartretail.settings;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Typed access to the store settings that checkout depends on. Read fresh on every call. */
@Service
@Transactional(readOnly = true)
public class StoreSettings {

    private final StoreSettingRepository settings;

    public StoreSettings(StoreSettingRepository settings) {
        this.settings = settings;
    }

    public record Checkout(boolean storeOpen, BigDecimal deliveryCharge, BigDecimal freeDeliveryAbove,
            BigDecimal minOrderValue, String upiId) {
    }

    public Checkout checkout() {
        Map<String, String> values = new HashMap<>();
        settings.findAll().forEach(s -> values.put(s.getKey(), s.getValue()));
        return new Checkout(
                !"false".equalsIgnoreCase(values.getOrDefault("store_open", "true")),
                money(values.get("delivery_charge")),
                money(values.get("free_delivery_above")),
                money(values.get("min_order_value")),
                values.getOrDefault("upi_id", ""));
    }

    private static BigDecimal money(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
