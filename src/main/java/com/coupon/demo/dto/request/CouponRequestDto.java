package com.coupon.demo.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de entrada para criação de cupom.
 * Aceita expirationDate em "yyyy-MM-dd" ou "dd-MM-yyyy" (ex.: 31-12-2026);
 * o deserializador normaliza para yyyy-MM-dd antes desta validação.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponRequestDto {

    @NotBlank(message = "O código é obrigatório")
    @Size(max = 20, message = "O código inserido é muito longo")
    private String code;

    @NotBlank(message = "A descrição é obrigatória")
    private String description;

    @NotNull(message = "O valor do desconto é obrigatório")
    @DecimalMin(value = "0.5", message = "O valor do desconto deve ser de no mínimo 0,5")
    private Double discountValue;

    @NotBlank(message = "A data de expiração é obrigatória")
    @JsonDeserialize(using = ExpirationDateDeserializer.class)
    private String expirationDate;

    private boolean published;

    /**
     * Valida se a data de expiração é futura. Usa expirationDate já normalizada (yyyy-MM-dd) pelo deserializador.
     */
    @AssertTrue(message = "A data de expiração não pode ser anterior ao momento atual")
    public boolean isDataFutura() {
        if (expirationDate == null || expirationDate.isBlank()) {
            return true;
        }
        try {
            return LocalDate.parse(expirationDate).isAfter(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }
}
