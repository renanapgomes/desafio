package com.coupon.demo.application.usecase;

import com.coupon.demo.application.exception.ResourceNotFoundException;
import com.coupon.demo.application.port.LoadCouponPort;
import com.coupon.demo.domain.Coupon;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Use case: buscar cupom por ID. Uma única intenção, um método público (execute).
 */
@RequiredArgsConstructor
public class GetCouponUseCase {

    private final LoadCouponPort loadCouponPort;

    public Coupon execute(UUID id) {
        return loadCouponPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cupom não encontrado"));
    }
}
