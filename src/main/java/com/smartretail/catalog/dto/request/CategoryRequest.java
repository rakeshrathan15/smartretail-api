package com.smartretail.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 500) String imageUrl,
        Boolean active,
        Integer sortOrder) {
}
