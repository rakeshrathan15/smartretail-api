package com.smartretail.common.mapper;

import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

/** Shared by all mappers: Spring beans, and a response field nothing fills in fails the build. */
@MapperConfig(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MapStructConfig {
}
