package com.coupon.demo.entity;

import com.coupon.demo.enums.CouponEnum;
import com.coupon.demo.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "coupons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 6)
    private String code;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private Double discountValue;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponEnum status = CouponEnum.ACTIVE;

    private boolean published;

    /**
     * Regra de domínio para criação de um cupom.
     * Centraliza:
     * - normalização do código (6 caracteres alfanuméricos, upper-case)
     * - validação de desconto mínimo
     * - validação de data de expiração futura (yyyy-MM-dd)
     */
    public static CouponEntity create(String code,
                                      String description,
                                      Double discountValue,
                                      String expirationDate,
                                      boolean published) {

        String normalizedCode = normalizeCode(code);
        validateDiscount(discountValue);
        LocalDateTime expiration = toFutureExpiration(expirationDate);

        CouponEntity entity = new CouponEntity();
        entity.setCode(normalizedCode);
        entity.setDescription(description);
        entity.setDiscountValue(discountValue);
        entity.setExpirationDate(expiration);
        entity.setStatus(CouponEnum.ACTIVE);
        entity.setPublished(published);
        return entity;
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
            date = LocalDate.parse(expirationDate); // formato yyyy-MM-dd
        } catch (Exception e) {
            throw new BusinessException("A data de expiração deve estar no formato yyyy-MM-dd");
        }

        if (!date.isAfter(LocalDate.now())) {
            throw new BusinessException("A data de expiração não pode ser anterior ou igual ao momento atual");
        }

        return date.atStartOfDay();
    }

    public void delete() {
        if (this.status == CouponEnum.DELETED) {
            throw new IllegalStateException("Cupom já está deletado");
        }
        this.status = CouponEnum.DELETED;
    }
}
