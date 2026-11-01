package com.smartretail.settings.controller;

import com.smartretail.common.response.ApiResponse;
import com.smartretail.settings.dto.request.StoreSettingsForm;
import com.smartretail.settings.service.StoreSettings;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only (enforced in SecurityConfig for /api/admin/**). */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingsController {

    private final StoreSettings settings;

    @GetMapping
    public ApiResponse<StoreSettingsForm> get() {
        return ApiResponse.ok(settings.form());
    }

    /** Replaces all settings. The storefront and checkout use the new values on their next request. */
    @PutMapping
    public ApiResponse<StoreSettingsForm> save(@Valid @RequestBody StoreSettingsForm form) {
        return ApiResponse.ok(settings.save(form));
    }
}
