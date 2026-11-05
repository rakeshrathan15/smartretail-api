package com.smartretail.store.mapper;

import com.smartretail.common.mapper.MapStructConfig;
import com.smartretail.store.dto.response.StoreResponse;
import com.smartretail.store.entity.Store;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface StoreMapper {

    StoreResponse toResponse(Store store, long routeCount);
}
