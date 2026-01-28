package com.coupon.demo.service;

import com.coupon.demo.dto.request.CouponRequestDto;
import com.coupon.demo.dto.response.CouponResponseDto;
import com.coupon.demo.entity.CouponEntity;
import com.coupon.demo.exception.BusinessException;
import com.coupon.demo.exception.ResourceNotFoundException;
import com.coupon.demo.mapper.CouponMapper;
import com.coupon.demo.repository.CouponRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponMapper mapper;

    public CouponService(CouponRepository couponRepository, CouponMapper mapper) {
        this.couponRepository = couponRepository;
        this.mapper = mapper;
    }

    public CouponResponseDto criarCupom(@Valid CouponRequestDto request) {

        CouponEntity cupomParaSalvar = CouponEntity.create(
                request.getCode(),
                request.getDescription(),
                request.getDiscountValue(),
                request.getExpirationDate(),
                request.isPublished()
        );
        CouponEntity cupomSalvo = couponRepository.save(cupomParaSalvar);

        return mapper.toDto(cupomSalvo);
    }

    public CouponResponseDto buscarCupomPorId(String id) {

        if (id == null) {
            throw new IllegalArgumentException("O ID fornecido não pode ser nulo.");
        }

        return couponRepository.findById(UUID.fromString(id))
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Cupom não encontrado"));
    }

    public CouponResponseDto deletarCupom(String id) {

        if (id == null) {
            throw new IllegalArgumentException("O ID fornecido não pode ser nulo.");
        }

        CouponEntity entity = couponRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new ResourceNotFoundException("Cupom não encontrado para deletar"));

        try {
            entity.delete();
        } catch (IllegalStateException e) {
            throw new BusinessException("Não é possível deletar um cupom que já está deletado.");
        }

        CouponEntity entitySalva = couponRepository.save(entity);

        return mapper.toDto(entitySalva);
    }

}
