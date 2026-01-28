package com.coupon.demo.entity;

import com.coupon.demo.enums.CouponEnum;
import com.coupon.demo.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CouponEntityTest {

    @Test
    @DisplayName("Deve criar cupom válido com código normalizado, desconto mínimo e data futura")
    void deveCriarCupomValido() {
        String rawCode = "ab-12$34"; // vira AB1234 (6 caracteres)
        String description = "Teste";
        Double discount = 1.0;
        String expiration = LocalDate.now().plusDays(1).toString();
        boolean published = true;

        CouponEntity entity = CouponEntity.create(rawCode, description, discount, expiration, published);

        assertNotNull(entity);
        assertEquals("AB1234", entity.getCode());
        assertEquals(discount, entity.getDiscountValue());
        assertEquals(CouponEnum.ACTIVE, entity.getStatus());
        assertTrue(entity.isPublished());

        LocalDateTime expectedExpiration = LocalDate.parse(expiration).atStartOfDay();
        assertEquals(expectedExpiration, entity.getExpirationDate());
    }

    @Test
    @DisplayName("Não deve criar cupom com código nulo ou vazio")
    void naoDeveCriarCupomComCodigoInvalido() {
        String expiration = LocalDate.now().plusDays(1).toString();

        assertThrows(BusinessException.class,
                () -> CouponEntity.create(null, "desc", 1.0, expiration, true));

        assertThrows(BusinessException.class,
                () -> CouponEntity.create("   ", "desc", 1.0, expiration, true));
    }

    @Test
    @DisplayName("Não deve criar cupom quando código limpo não tiver exatamente 6 caracteres")
    void naoDeveCriarCupomComCodigoComTamanhoDiferenteDeSeis() {
        String expiration = LocalDate.now().plusDays(1).toString();

        // Após limpar fica "ABC12" (5 chars)
        assertThrows(BusinessException.class,
                () -> CouponEntity.create("AB-C12", "desc", 1.0, expiration, true));

        // Após limpar fica "ABCDEFG" (7 chars)
        assertThrows(BusinessException.class,
                () -> CouponEntity.create("ABCDEF7", "desc", 1.0, expiration, true));
    }

    @Test
    @DisplayName("Não deve criar cupom com desconto nulo ou menor que 0.5")
    void naoDeveCriarCupomComDescontoInvalido() {
        String expiration = LocalDate.now().plusDays(1).toString();

        assertThrows(BusinessException.class,
                () -> CouponEntity.create("ABC123", "desc", null, expiration, true));

        assertThrows(BusinessException.class,
                () -> CouponEntity.create("ABC123", "desc", 0.4, expiration, true));
    }

    @Test
    @DisplayName("Não deve criar cupom com data de expiração vazia, formato inválido ou não futura")
    void naoDeveCriarCupomComDataInvalida() {
        String today = LocalDate.now().toString();
        String past = LocalDate.now().minusDays(1).toString();

        // Nula ou vazia
        assertThrows(BusinessException.class,
                () -> CouponEntity.create("ABC123", "desc", 1.0, null, true));
        assertThrows(BusinessException.class,
                () -> CouponEntity.create("ABC123", "desc", 1.0, "   ", true));

        // Formato inválido
        assertThrows(BusinessException.class,
                () -> CouponEntity.create("ABC123", "desc", 1.0, "2026/12/30", true));

        // Hoje ou passado
        assertThrows(BusinessException.class,
                () -> CouponEntity.create("ABC123", "desc", 1.0, today, true));
        assertThrows(BusinessException.class,
                () -> CouponEntity.create("ABC123", "desc", 1.0, past, true));
    }

    @Test
    @DisplayName("Deve realizar delete mudando status para DELETED")
    void deveDeletarCupomAtivo() {
        CouponEntity entity = new CouponEntity();
        entity.setStatus(CouponEnum.ACTIVE);

        entity.delete();

        assertEquals(CouponEnum.DELETED, entity.getStatus());
    }

    @Test
    @DisplayName("Não deve deletar cupom já deletado (regra de domínio)")
    void naoDeveDeletarCupomJaDeletadoNoDominio() {
        CouponEntity entity = new CouponEntity();
        entity.setStatus(CouponEnum.DELETED);

        assertThrows(IllegalStateException.class, entity::delete);
    }
}

