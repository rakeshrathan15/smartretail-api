package com.smartretail.catalog.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReorderRequest(@NotNull List<UUID> ids) {
}
