package com.smartretail.settings;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Typed access to the store settings. Read fresh on every call, so admin changes apply at once. */
@Service
@Transactional(readOnly = true)
public class StoreSettings {

    private final StoreSettingRepository settings;

    public StoreSettings(StoreSettingRepository settings) {
        this.settings = settings;
    }

    /** Settings the storefront may show: store name, open/closed, delivery charges, contact numbers. */
    public Map<String, String> publicValues() {
        Map<String, String> result = new TreeMap<>();
        settings.findAllByPublicSettingTrue().forEach(s -> result.put(s.getKey(), s.getValue() == null ? "" : s.getValue()));
        return result;
    }

    public record Checkout(boolean storeOpen, BigDecimal deliveryCharge, BigDecimal freeDeliveryAbove,
            BigDecimal minOrderValue, String upiId) {
    }

    public Checkout checkout() {
        Map<String, String> values = values();
        return new Checkout(
                !"false".equalsIgnoreCase(values.getOrDefault("store_open", "true")),
                money(values.get("delivery_charge")),
                money(values.get("free_delivery_above")),
                money(values.get("min_order_value")),
                values.getOrDefault("upi_id", ""));
    }

    /** All admin-editable settings, typed. Missing rows read as empty, zero or their default. */
    public StoreSettingsForm form() {
        Map<String, String> v = values();
        return new StoreSettingsForm(
                v.getOrDefault("store_name", ""),
                v.getOrDefault("store_phone", ""),
                v.getOrDefault("store_email", ""),
                v.getOrDefault("store_address", ""),
                v.getOrDefault("whatsapp_number", ""),
                !"false".equalsIgnoreCase(v.getOrDefault("store_open", "true")),
                money(v.get("delivery_charge")),
                money(v.get("free_delivery_above")),
                money(v.get("min_order_value")),
                v.getOrDefault("upi_id", ""),
                v.getOrDefault("gst_number", ""),
                v.getOrDefault("fssai_number", ""),
                !"false".equalsIgnoreCase(v.getOrDefault("show_certifications", "true")));
    }

    /** Saves every setting at once. Text is trimmed; codes are upper-cased. Takes effect on the next request. */
    @Transactional
    public StoreSettingsForm save(StoreSettingsForm f) {
        Map<String, String> next = new LinkedHashMap<>();
        next.put("store_name", f.storeName().trim());
        next.put("store_phone", trim(f.storePhone()));
        next.put("store_email", trim(f.storeEmail()).toLowerCase(Locale.ROOT));
        next.put("store_address", trim(f.storeAddress()));
        next.put("whatsapp_number", trim(f.whatsappNumber()));
        next.put("store_open", String.valueOf(f.storeOpen()));
        next.put("delivery_charge", plain(f.deliveryCharge()));
        next.put("free_delivery_above", plain(f.freeDeliveryAbove()));
        next.put("min_order_value", plain(f.minOrderValue()));
        next.put("upi_id", trim(f.upiId()));
        next.put("gst_number", trim(f.gstNumber()).toUpperCase(Locale.ROOT));
        next.put("fssai_number", trim(f.fssaiNumber()));
        next.put("show_certifications", String.valueOf(f.showCertifications()));

        Map<String, StoreSetting> existing = new HashMap<>();
        settings.findAll().forEach(s -> existing.put(s.getKey(), s));
        next.forEach((key, value) -> {
            StoreSetting setting = existing.get(key);
            if (setting == null) {
                settings.save(new StoreSetting(key, value, true));
            } else {
                setting.setValue(value);
            }
        });
        settings.flush();
        return form();
    }

    private Map<String, String> values() {
        Map<String, String> values = new HashMap<>();
        settings.findAll().forEach(s -> values.put(s.getKey(), s.getValue() == null ? "" : s.getValue()));
        return values;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    /** "500" rather than "500.00" or "5E+2", matching how the seed data stores amounts. */
    private static String plain(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
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
