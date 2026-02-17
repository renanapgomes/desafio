package com.coupon.demo.infrastructure.web;

import com.coupon.demo.domain.Coupon;
import com.coupon.demo.dto.response.CouponResponseDto;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Mapeia modelo de domínio para DTO de resposta (camada web).
 */
@Component
public class CouponWebMapper {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public CouponResponseDto toResponseDto(Coupon coupon) {
        CouponResponseDto dto = new CouponResponseDto();
        dto.setId(coupon.getId() != null ? coupon.getId().toString() : null);
        dto.setCode(coupon.getCode());
        dto.setDescription(coupon.getDescription());
        dto.setDiscountValue(coupon.getDiscountValue());
        dto.setExpirationDate(coupon.getExpirationDate() != null
                ? coupon.getExpirationDate().toLocalDate().format(DATE_FORMAT)
                : null);
        dto.setStatus(coupon.getStatus());
        dto.setPublished(coupon.isPublished());
        return dto;
    }
}
