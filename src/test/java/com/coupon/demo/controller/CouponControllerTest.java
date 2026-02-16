package com.coupon.demo.controller;

import com.coupon.demo.domain.Coupon;
import com.coupon.demo.domain.CouponStatus;
import com.coupon.demo.dto.request.CouponRequestDto;
import com.coupon.demo.dto.response.CouponResponseDto;
import com.coupon.demo.infrastructure.web.CouponController;
import com.coupon.demo.infrastructure.web.CouponWebMapper;
import com.coupon.demo.application.usecase.CreateCouponUseCase;
import com.coupon.demo.application.usecase.DeleteCouponUseCase;
import com.coupon.demo.application.usecase.GetCouponUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponController.class)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateCouponUseCase createCouponUseCase;

    @MockBean
    private DeleteCouponUseCase deleteCouponUseCase;

    @MockBean
    private GetCouponUseCase getCouponUseCase;

    @MockBean
    private CouponWebMapper webMapper;

    @Test
    @DisplayName("Deve criar um cupom com sucesso e retornar status 201")
    void deveCriarCupomComSucesso() throws Exception {
        CouponRequestDto requestDto = new CouponRequestDto();
        requestDto.setCode("NATAL2");
        requestDto.setDescription("Desconto de Natal");
        requestDto.setDiscountValue(10.0);
        requestDto.setExpirationDate("2026-12-25");

        UUID id = UUID.randomUUID();
        Coupon created = Coupon.reconstitute(id, "NATAL2", "Desconto de Natal", 10.0,
                LocalDateTime.now().plusDays(10), CouponStatus.ACTIVE, true);

        CouponResponseDto responseDto = new CouponResponseDto();
        responseDto.setId(id.toString());
        responseDto.setCode("NATAL2");
        responseDto.setStatus(CouponStatus.ACTIVE);
        responseDto.setExpirationDate(LocalDate.now().plusDays(10).toString());

        when(createCouponUseCase.execute(any(), any(), any(), any(), anyBoolean())).thenReturn(created);
        when(webMapper.toResponseDto(any(Coupon.class))).thenReturn(responseDto);

        mockMvc.perform(post("/coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("NATAL2"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Não deve criar cupom com dados inválidos (Bean Validation) e deve retornar 400")
    void naoDeveCriarCupomComDadosInvalidos() throws Exception {
        CouponRequestDto requestDto = new CouponRequestDto();

        mockMvc.perform(post("/coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve buscar um cupom por ID e retornar status 200")
    void deveBuscarCupomPorId() throws Exception {
        String id = UUID.randomUUID().toString();
        Coupon coupon = Coupon.reconstitute(UUID.fromString(id), "TEST1", "Teste de busca", 1.0,
                LocalDateTime.now().plusDays(1), CouponStatus.ACTIVE, true);

        CouponResponseDto responseDto = new CouponResponseDto();
        responseDto.setId(id);
        responseDto.setCode("TEST1");
        responseDto.setDescription("Teste de busca");

        when(getCouponUseCase.execute(eq(UUID.fromString(id)))).thenReturn(coupon);
        when(webMapper.toResponseDto(any(Coupon.class))).thenReturn(responseDto);

        mockMvc.perform(get("/coupon/{id}", id).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.code").value("TEST1"));
    }

    @Test
    @DisplayName("Deve deletar cupom e retornar status 204 com corpo")
    void deveDeletarCupom() throws Exception {
        String id = UUID.randomUUID().toString();
        Coupon deleted = Coupon.reconstitute(UUID.fromString(id), "ABC123", "d", 1.0,
                LocalDateTime.now().plusDays(1), CouponStatus.DELETED, true);

        CouponResponseDto deletedDto = new CouponResponseDto();
        deletedDto.setId(id);
        deletedDto.setCode("ABC123");
        deletedDto.setStatus(CouponStatus.DELETED);

        when(deleteCouponUseCase.execute(eq(UUID.fromString(id)))).thenReturn(deleted);
        when(webMapper.toResponseDto(any(Coupon.class))).thenReturn(deletedDto);

        mockMvc.perform(delete("/coupon/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.code").value("ABC123"));
    }

    @Test
    @DisplayName("Não deve deletar cupom já deletado e deve retornar 400 com mensagem de erro")
    void naoDeveDeletarCupomJaDeletado() throws Exception {
        String id = UUID.randomUUID().toString();

        when(deleteCouponUseCase.execute(eq(UUID.fromString(id))))
                .thenThrow(new com.coupon.demo.domain.BusinessException("Não é possível deletar um cupom que já está deletado."));

        mockMvc.perform(delete("/coupon/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Não é possível deletar um cupom que já está deletado."));
    }
}
