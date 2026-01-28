package com.coupon.demo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CouponEnum {

    ACTIVE("Cupom Ativo"),
    INACTIVE("Cupom Inativo"),
    DELETED("Cupom Deletado");

    private final String description;
}
