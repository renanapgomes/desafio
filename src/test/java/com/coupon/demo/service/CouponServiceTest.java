package com.coupon.demo.service;

import com.coupon.demo.dto.request.CouponRequestDto;
import com.coupon.demo.dto.response.CouponResponseDto;
import com.coupon.demo.entity.CouponEntity;
import com.coupon.demo.enums.CouponEnum;
import com.coupon.demo.exception.BusinessException;
import com.coupon.demo.exception.ResourceNotFoundException;
import com.coupon.demo.mapper.CouponMapper;
import com.coupon.demo.repository.CouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @InjectMocks
    private CouponService couponService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponMapper mapper;

    @Test
    @DisplayName("Deve criar um cupom com sucesso")
    void deveCriarCupomComSucesso() {
        CouponRequestDto request = new CouponRequestDto();
        // Após limpar caracteres especiais, resultará em "AB1234" (6 caracteres válidos)
        request.setCode("AB-12$34");
        request.setDescription("Desc");
        request.setDiscountValue(1.0);
        request.setExpirationDate(java.time.LocalDate.now().plusDays(1).toString());
        request.setPublished(true);

        CouponResponseDto expectedResponse = new CouponResponseDto();
        UUID generatedId = UUID.randomUUID();
        expectedResponse.setId(String.valueOf(generatedId));

        when(couponRepository.save(any(CouponEntity.class))).thenAnswer(invocation -> {
            CouponEntity entity = invocation.getArgument(0);
            entity.setId(generatedId);
            return entity;
        });
        when(mapper.toDto(any(CouponEntity.class))).thenReturn(expectedResponse);

        CouponResponseDto result = couponService.criarCupom(request);

        assertNotNull(result);
        assertEquals(expectedResponse.getId(), result.getId());
        verify(couponRepository, times(1)).save(any(CouponEntity.class));
    }

    @Test
    @DisplayName("Não deve criar cupom quando domínio lançar BusinessException")
    void naoDeveCriarCupomQuandoDominioFalhar() {
        CouponRequestDto request = new CouponRequestDto();
        // Código invalido (após limpar não terá 6 caracteres)
        request.setCode("ABC");
        request.setDescription("Desc");
        request.setDiscountValue(1.0);
        request.setExpirationDate(java.time.LocalDate.now().plusDays(1).toString());
        request.setPublished(true);

        assertThrows(BusinessException.class, () -> couponService.criarCupom(request));
        verify(couponRepository, never()).save(any(CouponEntity.class));
    }

    @Test
    @DisplayName("Deve buscar cupom por ID com sucesso")
    void deveBuscarCupomPorIdComSucesso() {
        UUID id = UUID.randomUUID();
        String idString = id.toString();
        CouponEntity entity = new CouponEntity();
        entity.setId(id);
        CouponResponseDto responseDto = new CouponResponseDto();
        responseDto.setId(String.valueOf(id));

        when(couponRepository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDto(entity)).thenReturn(responseDto);

        CouponResponseDto result = couponService.buscarCupomPorId(idString);

        assertNotNull(result);
        assertEquals(id.toString(), result.getId());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando ID não existir no banco")
    void deveLancarErroQuandoCupomNaoEncontradoAoBuscar() {
        UUID id = UUID.randomUUID();
        String idString = id.toString();

        when(couponRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> couponService.buscarCupomPorId(idString));

        verify(mapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando ID for nulo na busca")
    void deveLancarErroQuandoIdNuloNaBusca() {
        assertThrows(IllegalArgumentException.class, () -> couponService.buscarCupomPorId(null));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando formato do ID for inválido (não UUID)")
    void deveLancarErroQuandoIdFormatoInvalidoNaBusca() {
        assertThrows(IllegalArgumentException.class, () -> couponService.buscarCupomPorId("123-formato-errado"));
    }

    @Test
    @DisplayName("Deve realizar soft delete do cupom (mudar status) e retornar o DTO")
    void deveRealizarSoftDeleteDoCupom() {
        UUID id = UUID.randomUUID();
        String idString = id.toString();

        CouponEntity entity = new CouponEntity();
        entity.setId(id);
        entity.setStatus(CouponEnum.ACTIVE);

        CouponResponseDto responseDto = new CouponResponseDto();
        responseDto.setId(String.valueOf(id));

        when(couponRepository.findById(id)).thenReturn(Optional.of(entity));
        when(couponRepository.save(any(CouponEntity.class))).thenReturn(entity);
        when(mapper.toDto(entity)).thenReturn(responseDto);

        CouponResponseDto result = couponService.deletarCupom(idString);

        assertNotNull(result);
        assertEquals(idString, result.getId());

        ArgumentCaptor<CouponEntity> captor = ArgumentCaptor.forClass(CouponEntity.class);

        verify(couponRepository, times(1)).save(captor.capture());
        verify(couponRepository, never()).delete(any());

        CouponEntity couponSalvo = captor.getValue();

        assertEquals(CouponEnum.DELETED, couponSalvo.getStatus());
    }

    @Test
    @DisplayName("Não deve permitir deletar um cupom já deletado")
    void naoDeveDeletarCupomJaDeletado() {
        UUID id = UUID.randomUUID();
        String idString = id.toString();

        CouponEntity entity = new CouponEntity();
        entity.setId(id);
        entity.setStatus(CouponEnum.DELETED);

        when(couponRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThrows(BusinessException.class, () -> couponService.deletarCupom(idString));

        verify(couponRepository, never()).save(any(CouponEntity.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao tentar deletar cupom inexistente")
    void deveLancarErroAoDeletarCupomInexistente() {
        UUID id = UUID.randomUUID();
        String idString = id.toString();

        when(couponRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> couponService.deletarCupom(idString));

        verify(couponRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando ID for nulo ao deletar")
    void deveLancarErroQuandoIdNuloAoDeletar() {
        assertThrows(IllegalArgumentException.class, () -> couponService.deletarCupom(null));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando formato do ID for inválido ao deletar")
    void deveLancarErroQuandoIdInvalidoAoDeletar() {
        assertThrows(IllegalArgumentException.class, () -> couponService.deletarCupom("id-invalido"));
    }
}