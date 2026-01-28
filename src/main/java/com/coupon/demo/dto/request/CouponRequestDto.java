package com.coupon.demo.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @NotBlank(message = "A data de expiração é obrigatório")
    private String expirationDate;

    private boolean published;

}
