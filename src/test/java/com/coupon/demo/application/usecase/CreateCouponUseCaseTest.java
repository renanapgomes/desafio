package com.coupon.demo.application.usecase;

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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateCouponUseCaseTest {

    @InjectMocks
    private CreateCouponUseCase createCouponUseCase;

    @Mock
    private SaveCouponPort saveCouponPort;

    @Test
    @DisplayName("Deve criar cupom com sucesso e persistir")
    void deveCriarCupomComSucesso() {
        String code = "AB1234";
        String description = "Desc";
        Double discountValue = 1.0;
        String expirationDate = java.time.LocalDate.now().plusDays(1).toString();
        boolean published = true;

        UUID generatedId = UUID.randomUUID();
        when(saveCouponPort.save(any(Coupon.class))).thenAnswer(invocation -> {
            Coupon c = invocation.getArgument(0);
            c.setId(generatedId);
            return c;
        });

        Coupon result = createCouponUseCase.execute(code, description, discountValue, expirationDate, published);

        assertNotNull(result);
        assertEquals(generatedId, result.getId());
        assertEquals("AB1234", result.getCode());
        assertEquals(CouponStatus.ACTIVE, result.getStatus());

        ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);
        verify(saveCouponPort, times(1)).save(captor.capture());
        assertEquals("AB1234", captor.getValue().getCode());
    }

    @Test
    @DisplayName("Não deve persistir quando domínio lançar BusinessException")
    void naoDevePersistirQuandoDominioFalhar() {
        String code = "ABC";
        String description = "Desc";
        Double discountValue = 1.0;
        String expirationDate = java.time.LocalDate.now().plusDays(1).toString();
        boolean published = true;

        assertThrows(BusinessException.class, () ->
                createCouponUseCase.execute(code, description, discountValue, expirationDate, published));

        verify(saveCouponPort, never()).save(any(Coupon.class));
    }
}
