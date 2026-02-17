package com.coupon.demo.application.port;

import com.coupon.demo.domain.Coupon;

/**
 * Porta para persistir cupom. Implementação (adapter) fica na infra;
 * application não importa JPA nem Spring Data.
 */
public interface SaveCouponPort {

    Coupon save(Coupon coupon);
}
