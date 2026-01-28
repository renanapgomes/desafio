package com.coupon.demo.mapper;

import com.coupon.demo.dto.response.CouponResponseDto;
import com.coupon.demo.entity.CouponEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CouponMapper {

    /**
     * Conversão simples de entidade para DTO de resposta.
     * As regras de negócio ficam no domínio (CouponEntity) e no serviço.
     */
    CouponResponseDto toDto(CouponEntity entity);
}