package com.coupon.demo.application.usecase;

import com.coupon.demo.application.exception.ResourceNotFoundException;
import com.coupon.demo.application.port.LoadCouponPort;
import com.coupon.demo.domain.Coupon;
import com.coupon.demo.domain.CouponStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetCouponUseCaseTest {

    @InjectMocks
    private GetCouponUseCase getCouponUseCase;

    @Mock
    private LoadCouponPort loadCouponPort;

    @Test
    @DisplayName("Deve retornar cupom quando existir")
    void deveRetornarCupomQuandoExistir() {
        UUID id = UUID.randomUUID();
        Coupon coupon = Coupon.reconstitute(id, "ABC123", "Desc", 1.0,
                LocalDateTime.now().plusDays(1), CouponStatus.ACTIVE, true);

        when(loadCouponPort.findById(id)).thenReturn(Optional.of(coupon));

        Coupon result = getCouponUseCase.execute(id);

        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals("ABC123", result.getCode());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando cupom não existir")
    void deveLancarErroQuandoCupomNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(loadCouponPort.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> getCouponUseCase.execute(id));
    }
}
