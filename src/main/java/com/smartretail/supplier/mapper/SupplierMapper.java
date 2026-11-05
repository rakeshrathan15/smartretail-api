package com.smartretail.supplier.mapper;

import com.smartretail.common.mapper.MapStructConfig;
import com.smartretail.supplier.dto.response.SupplierResponse;
import com.smartretail.supplier.entity.Supplier;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface SupplierMapper {

    SupplierResponse toResponse(Supplier s);
}
