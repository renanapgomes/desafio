package com.coupon.demo.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modelo de domínio do cupom. Contém todas as regras de negócio;
 * sem dependência de JPA, Spring ou qualquer tecnologia.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Coupon {

    private UUID id;
    private final String code;
    private final String description;
    private final Double discountValue;
    private final LocalDateTime expirationDate;
    private CouponStatus status;
    private final boolean published;

    /**
     * Regra de domínio para criação de um cupom.
     * Centraliza: normalização do código, validação de desconto e data futura.
     */
    public static Coupon create(String code, String description, Double discountValue,
                                String expirationDate, boolean published) {
        String normalizedCode = normalizeCode(code);
        validateDiscount(discountValue);
        LocalDateTime expiration = toFutureExpiration(expirationDate);

        return new Coupon(null, normalizedCode, description, discountValue,
                expiration, CouponStatus.ACTIVE, published);
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("O código é obrigatório");
        }
        String limpo = code.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (limpo.length() != 6) {
            throw new BusinessException("O código do cupom deve ter exatamente 6 caracteres alfanuméricos");
        }
        return limpo;
    }

    private static void validateDiscount(Double discountValue) {
        if (discountValue == null) {
            throw new BusinessException("O valor do desconto é obrigatório");
        }
        if (discountValue < 0.5) {
            throw new BusinessException("O valor do desconto deve ser de no mínimo 0,5");
        }
    }

    private static LocalDateTime toFutureExpiration(String expirationDate) {
        if (expirationDate == null || expirationDate.isBlank()) {
            throw new BusinessException("A data de expiração é obrigatória");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(expirationDate);
        } catch (Exception e) {
            throw new BusinessException("A data de expiração deve estar no formato yyyy-MM-dd");
        }
        if (!date.isAfter(LocalDate.now())) {
            throw new BusinessException("A data de expiração não pode ser anterior ou igual ao momento atual");
        }
        return date.atStartOfDay();
    }

    /**
     * Regra de domínio: não deletar duas vezes.
     */
    /**
     * Reconstitui um cupom a partir do banco (sem validações de criação).
     */
    public static Coupon reconstitute(UUID id, String code, String description, Double discountValue,
                                      LocalDateTime expirationDate, CouponStatus status, boolean published) {
        Coupon c = new Coupon(id, code, description, discountValue, expirationDate, status, published);
        return c;
    }

    public void delete() {
        if (this.status == CouponStatus.DELETED) {
            throw new IllegalStateException("Cupom já está deletado");
        }
        this.status = CouponStatus.DELETED;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
