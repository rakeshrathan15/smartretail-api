package com.smartretail.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BannerRequest(
        @NotBlank @Size(max = 120) String title,
        @Size(max = 200) String subtitle,
        @Size(max = 40) String ctaText,
        @Size(max = 300) String link,
        @Size(max = 500) String imageUrl,
        Boolean active) {
}
