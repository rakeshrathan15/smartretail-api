package com.smartretail.settings;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only (enforced in SecurityConfig for /api/admin/**). */
@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingsController {

    private final StoreSettings settings;

    public AdminSettingsController(StoreSettings settings) {
        this.settings = settings;
    }

    @GetMapping
    public StoreSettingsForm get() {
        return settings.form();
    }

    /** Replaces all settings. The storefront and checkout use the new values on their next request. */
    @PutMapping
    public StoreSettingsForm save(@Valid @RequestBody StoreSettingsForm form) {
        return settings.save(form);
    }
}
