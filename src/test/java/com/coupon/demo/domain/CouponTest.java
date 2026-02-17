package com.coupon.demo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CouponTest {

    @Test
    @DisplayName("Deve criar cupom válido com código normalizado, desconto mínimo e data futura")
    void deveCriarCupomValido() {
        String rawCode = "ab-12$34";
        String description = "Teste";
        Double discount = 1.0;
        String expiration = LocalDate.now().plusDays(1).toString();
        boolean published = true;

        Coupon coupon = Coupon.create(rawCode, description, discount, expiration, published);

        assertNotNull(coupon);
        assertEquals("AB1234", coupon.getCode());
        assertEquals(discount, coupon.getDiscountValue());
        assertEquals(CouponStatus.ACTIVE, coupon.getStatus());
        assertTrue(coupon.isPublished());

        LocalDateTime expectedExpiration = LocalDate.parse(expiration).atStartOfDay();
        assertEquals(expectedExpiration, coupon.getExpirationDate());
    }

    @Test
    @DisplayName("Não deve criar cupom com código nulo ou vazio")
    void naoDeveCriarCupomComCodigoInvalido() {
        String expiration = LocalDate.now().plusDays(1).toString();

        assertThrows(BusinessException.class,
                () -> Coupon.create(null, "desc", 1.0, expiration, true));

        assertThrows(BusinessException.class,
                () -> Coupon.create("   ", "desc", 1.0, expiration, true));
    }

    @Test
    @DisplayName("Não deve criar cupom quando código limpo não tiver exatamente 6 caracteres")
    void naoDeveCriarCupomComCodigoComTamanhoDiferenteDeSeis() {
        String expiration = LocalDate.now().plusDays(1).toString();

        assertThrows(BusinessException.class,
                () -> Coupon.create("AB-C12", "desc", 1.0, expiration, true));

        assertThrows(BusinessException.class,
                () -> Coupon.create("ABCDEF7", "desc", 1.0, expiration, true));
    }

    @Test
    @DisplayName("Não deve criar cupom com desconto nulo ou menor que 0.5")
    void naoDeveCriarCupomComDescontoInvalido() {
        String expiration = LocalDate.now().plusDays(1).toString();

        assertThrows(BusinessException.class,
                () -> Coupon.create("ABC123", "desc", null, expiration, true));

        assertThrows(BusinessException.class,
                () -> Coupon.create("ABC123", "desc", 0.4, expiration, true));
    }

    @Test
    @DisplayName("Não deve criar cupom com data de expiração vazia, formato inválido ou não futura")
    void naoDeveCriarCupomComDataInvalida() {
        String today = LocalDate.now().toString();
        String past = LocalDate.now().minusDays(1).toString();

        assertThrows(BusinessException.class,
                () -> Coupon.create("ABC123", "desc", 1.0, null, true));
        assertThrows(BusinessException.class,
                () -> Coupon.create("ABC123", "desc", 1.0, "   ", true));

        assertThrows(BusinessException.class,
                () -> Coupon.create("ABC123", "desc", 1.0, "2026/12/30", true));

        assertThrows(BusinessException.class,
                () -> Coupon.create("ABC123", "desc", 1.0, today, true));
        assertThrows(BusinessException.class,
                () -> Coupon.create("ABC123", "desc", 1.0, past, true));
    }

    @Test
    @DisplayName("Deve realizar delete mudando status para DELETED")
    void deveDeletarCupomAtivo() {
        Coupon coupon = Coupon.reconstitute(
                java.util.UUID.randomUUID(), "ABC123", "d", 1.0,
                LocalDateTime.now().plusDays(1), CouponStatus.ACTIVE, true);

        coupon.delete();

        assertEquals(CouponStatus.DELETED, coupon.getStatus());
    }

    @Test
    @DisplayName("Não deve deletar cupom já deletado (regra de domínio)")
    void naoDeveDeletarCupomJaDeletadoNoDominio() {
        Coupon coupon = Coupon.reconstitute(
                java.util.UUID.randomUUID(), "ABC123", "d", 1.0,
                LocalDateTime.now().plusDays(1), CouponStatus.DELETED, true);

        assertThrows(IllegalStateException.class, coupon::delete);
    }
}
