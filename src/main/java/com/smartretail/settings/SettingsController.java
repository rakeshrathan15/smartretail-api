package com.smartretail.settings;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SettingsController {

    private final StoreSettings settings;

    public SettingsController(StoreSettings settings) {
        this.settings = settings;
    }

    @GetMapping("/api/settings/public")
    public Map<String, String> publicSettings() {
        return settings.publicValues();
    }
}
