package com.coupon.demo.application.usecase;

import com.coupon.demo.application.exception.ResourceNotFoundException;
import com.coupon.demo.application.port.LoadCouponPort;
import com.coupon.demo.application.port.SaveCouponPort;
import com.coupon.demo.domain.BusinessException;
import com.coupon.demo.domain.Coupon;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Use case: deletar cupom (soft delete). Uma única intenção, um método público (execute).
 * Orquestra: carregar → aplicar regra de domínio (não deletar duas vezes) → salvar.
 */
@RequiredArgsConstructor
public class DeleteCouponUseCase {

    private final LoadCouponPort loadCouponPort;
    private final SaveCouponPort saveCouponPort;

    /**
     * Marca o cupom como deletado. Regra "não deletar duas vezes" está no domínio (Coupon.delete).
     */
    public Coupon execute(UUID id) {
        Coupon coupon = loadCouponPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cupom não encontrado para deletar"));

        try {
            coupon.delete();
        } catch (IllegalStateException e) {
            throw new BusinessException("Não é possível deletar um cupom que já está deletado.");
        }

        return saveCouponPort.save(coupon);
    }
}
