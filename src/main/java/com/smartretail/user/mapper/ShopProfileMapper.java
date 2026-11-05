package com.smartretail.user.mapper;

import com.smartretail.common.mapper.MapStructConfig;
import com.smartretail.user.dto.response.ShopProfileResponse;
import com.smartretail.user.entity.Profile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface ShopProfileMapper {

    @Mapping(target = "ownerName", expression = "java(p.getOwnerName() != null ? p.getOwnerName() : p.getFullName())")
    @Mapping(target = "address", source = "shopAddress")
    @Mapping(target = "city", source = "shopCity")
    @Mapping(target = "state", source = "shopState")
    @Mapping(target = "pincode", source = "shopPincode")
    @Mapping(target = "lat", source = "shopLat")
    @Mapping(target = "lng", source = "shopLng")
    @Mapping(target = "complete", source = "shopProfileComplete")
    ShopProfileResponse toResponse(Profile p);
}
