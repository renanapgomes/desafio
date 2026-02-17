package com.coupon.demo.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CouponStatus {

    ACTIVE("Cupom Ativo"),
    INACTIVE("Cupom Inativo"),
    DELETED("Cupom Deletado");

    private final String description;
}
