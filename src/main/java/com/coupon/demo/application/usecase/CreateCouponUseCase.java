package com.coupon.demo.application.usecase;

import com.coupon.demo.application.port.SaveCouponPort;
import com.coupon.demo.domain.Coupon;
import lombok.RequiredArgsConstructor;

/**
 * Use case: criar cupom. Uma única intenção, um método público (execute).
 * Orquestra o fluxo; regras de negócio ficam no domínio (Coupon.create).
 */
@RequiredArgsConstructor
public class CreateCouponUseCase {

    private final SaveCouponPort saveCouponPort;

    /**
     * Cria um cupom válido e persiste. Validações são feitas no domínio.
     */
    public Coupon execute(String code, String description, Double discountValue,
                          String expirationDate, boolean published) {
        Coupon coupon = Coupon.create(code, description, discountValue, expirationDate, published);
        return saveCouponPort.save(coupon);
    }
}
