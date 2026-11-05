package com.smartretail.catalog.dto.response;

import java.util.UUID;

public record CategoryResponse(UUID id, String name, String slug, String imageUrl, boolean active, int sortOrder) {
}
