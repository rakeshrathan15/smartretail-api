package com.smartretail.catalog.dto.response;

import java.util.UUID;

public record ImageResponse(UUID id, String url, boolean primary) {
}
