package com.smartretail.catalog.dto.response;

import java.util.UUID;

public record BannerResponse(
        UUID id, String title, String subtitle, String ctaText, String link, String imageUrl,
        boolean active, int sortOrder) {
}
