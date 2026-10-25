package com.smartretail.settings;

import java.util.Map;
import java.util.TreeMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SettingsController {

    private final StoreSettingRepository settings;

    public SettingsController(StoreSettingRepository settings) {
        this.settings = settings;
    }

    /** Settings the storefront shows: store name, open/closed, delivery charges, contact numbers. */
    @GetMapping("/api/settings/public")
    public Map<String, String> publicSettings() {
        Map<String, String> result = new TreeMap<>();
        settings.findAllByPublicSettingTrue().forEach(s -> result.put(s.getKey(), s.getValue() == null ? "" : s.getValue()));
        return result;
    }
}
