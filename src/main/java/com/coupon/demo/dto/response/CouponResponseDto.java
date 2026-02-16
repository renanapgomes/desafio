package com.coupon.demo.dto.response;

import com.coupon.demo.domain.CouponStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponseDto {

    @NotBlank(message = "O id é obrigatório")
    private String id;

    @NotBlank(message = "O código é obrigatório")
    private String code;

    @NotBlank(message = "A descrição é obrigatória ")
    private String description;

    @NotNull(message = "O valor do desconto é obrigatório")
    private Double discountValue;

    @NotBlank(message = "A data de expiração é obrigatório")
    private String expirationDate;

    @NotNull(message = "O status é obrigatório")
    private CouponStatus status;

    private boolean published;
}

