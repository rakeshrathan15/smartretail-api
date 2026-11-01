package com.smartretail.settings.controller;

import com.smartretail.common.response.ApiResponse;
import com.smartretail.settings.service.StoreSettings;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class SettingsController {

    private final StoreSettings settings;

    @GetMapping("/api/settings/public")
    public ApiResponse<Map<String, String>> publicSettings() {
        return ApiResponse.ok(settings.publicValues());
    }
}
