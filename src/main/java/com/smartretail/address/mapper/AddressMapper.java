package com.smartretail.address.mapper;

import com.smartretail.address.dto.response.AddressResponse;
import com.smartretail.address.entity.Address;
import com.smartretail.common.mapper.MapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface AddressMapper {

    @Mapping(target = "isDefault", source = "defaultAddress")
    AddressResponse toResponse(Address a);
}
