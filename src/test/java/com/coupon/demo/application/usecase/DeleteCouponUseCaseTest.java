package com.coupon.demo.application.usecase;

import com.coupon.demo.application.exception.ResourceNotFoundException;
import com.coupon.demo.application.port.LoadCouponPort;
import com.coupon.demo.application.port.SaveCouponPort;
import com.coupon.demo.domain.BusinessException;
import com.coupon.demo.domain.Coupon;
import com.coupon.demo.domain.CouponStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteCouponUseCaseTest {

    @InjectMocks
    private DeleteCouponUseCase deleteCouponUseCase;

    @Mock
    private LoadCouponPort loadCouponPort;

    @Mock
    private SaveCouponPort saveCouponPort;

    @Test
    @DisplayName("Deve realizar soft delete e persistir")
    void deveRealizarSoftDeleteDoCupom() {
        UUID id = UUID.randomUUID();
        Coupon coupon = Coupon.reconstitute(id, "ABC123", "d", 1.0,
                LocalDateTime.now().plusDays(1), CouponStatus.ACTIVE, true);

        when(loadCouponPort.findById(id)).thenReturn(Optional.of(coupon));
        when(saveCouponPort.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Coupon result = deleteCouponUseCase.execute(id);

        assertNotNull(result);
        assertEquals(CouponStatus.DELETED, result.getStatus());

        ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);
        verify(saveCouponPort, times(1)).save(captor.capture());
        assertEquals(CouponStatus.DELETED, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("Não deve permitir deletar um cupom já deletado")
    void naoDeveDeletarCupomJaDeletado() {
        UUID id = UUID.randomUUID();
        Coupon coupon = Coupon.reconstitute(id, "ABC123", "d", 1.0,
                LocalDateTime.now().plusDays(1), CouponStatus.DELETED, true);

        when(loadCouponPort.findById(id)).thenReturn(Optional.of(coupon));

        assertThrows(BusinessException.class, () -> deleteCouponUseCase.execute(id));

        verify(saveCouponPort, never()).save(any(Coupon.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao tentar deletar cupom inexistente")
    void deveLancarErroAoDeletarCupomInexistente() {
        UUID id = UUID.randomUUID();
        when(loadCouponPort.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> deleteCouponUseCase.execute(id));

        verify(saveCouponPort, never()).save(any(Coupon.class));
    }
}
