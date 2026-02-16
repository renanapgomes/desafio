package com.coupon.demo.controller;

import com.coupon.demo.BaseIT;
import com.coupon.demo.dto.request.CouponRequestDto;
import com.coupon.demo.domain.CouponStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CouponJourneyIT extends BaseIT {

    @Test
    @DisplayName("Jornada Completa: Criar Cupom -> Validar Banco -> Buscar -> Deletar")
    void fluxoCompletoDeVidaDoCupom() throws Exception {

        CouponRequestDto request = new CouponRequestDto();
        request.setCode("JO2026");
        request.setDescription("Cupom da Jornada");
        request.setDiscountValue(15.0);
        request.setExpirationDate(LocalDate.now().plusDays(30).toString());
        request.setPublished(true);

        String responseJson = mockMvc.perform(post("/coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();

        String createdId = objectMapper.readTree(responseJson).get("id").asText();

        assertEquals(1, couponRepository.count(), "Deveria ter 1 cupom no banco");

        mockMvc.perform(get("/coupon/" + createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("JO2026"));

        mockMvc.perform(delete("/coupon/" + createdId))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.status").value("DELETED"));

        var cupomFinal = couponRepository.findById(java.util.UUID.fromString(createdId)).get();
        assertEquals(CouponStatus.DELETED, cupomFinal.getStatus());
    }

    @Test
    @DisplayName("Não deve permitir deletar o mesmo cupom duas vezes (regra de negócio)")
    void naoDeveDeletarCupomDuasVezes() throws Exception {
        CouponRequestDto request = new CouponRequestDto();
        request.setCode("DUPLO1");
        request.setDescription("Cupom para testar delete duplo");
        request.setDiscountValue(5.0);
        request.setExpirationDate(LocalDate.now().plusDays(10).toString());
        request.setPublished(false);

        String responseJson = mockMvc.perform(post("/coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String createdId = objectMapper.readTree(responseJson).get("id").asText();

        mockMvc.perform(delete("/coupon/" + createdId))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/coupon/" + createdId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Não é possível deletar um cupom que já está deletado."));
    }
}