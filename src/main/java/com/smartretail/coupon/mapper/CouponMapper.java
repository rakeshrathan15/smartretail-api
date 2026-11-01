package com.smartretail.coupon.mapper;

import com.smartretail.common.mapper.MapStructConfig;
import com.smartretail.coupon.dto.response.CouponResponse;
import com.smartretail.coupon.entity.Coupon;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface CouponMapper {

    CouponResponse toResponse(Coupon c);
}
