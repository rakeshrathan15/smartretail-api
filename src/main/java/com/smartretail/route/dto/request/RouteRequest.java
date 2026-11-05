package com.smartretail.route.dto.request;

import com.smartretail.route.dto.Zone;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record RouteRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 120) String area,
        @NotNull @Size(max = 7) List<@NotNull @Pattern(regexp = "MON|TUE|WED|THU|FRI|SAT|SUN",
                message = "must be MON, TUE, WED, THU, FRI, SAT or SUN") String> days,
        UUID storeId,
        @Pattern(regexp = "^$|^#[0-9A-Fa-f]{6}$", message = "must be a colour like #2563EB") String color,
        @Valid Zone zone,
        @Min(0) @Max(9999) Integer sortOrder,
        Boolean active) {
}
