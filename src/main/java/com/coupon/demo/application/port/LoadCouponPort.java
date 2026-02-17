package com.coupon.demo.application.port;

import com.coupon.demo.domain.Coupon;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta para carregar cupom. Implementação (adapter) fica na infra;
 * application não importa JPA nem Spring Data.
 */
public interface LoadCouponPort {

    Optional<Coupon> findById(UUID id);
}
