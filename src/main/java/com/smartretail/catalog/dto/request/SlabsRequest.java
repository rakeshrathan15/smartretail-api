package com.smartretail.catalog.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SlabsRequest(@NotNull @Valid List<SlabRequest> slabs) {
}
