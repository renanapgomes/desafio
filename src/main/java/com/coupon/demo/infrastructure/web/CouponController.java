package com.coupon.demo.infrastructure.web;

import com.coupon.demo.application.usecase.CreateCouponUseCase;
import com.coupon.demo.application.usecase.DeleteCouponUseCase;
import com.coupon.demo.application.usecase.GetCouponUseCase;
import com.coupon.demo.dto.request.CouponRequestDto;
import com.coupon.demo.dto.response.CouponResponseDto;
import com.coupon.demo.domain.Coupon;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller HTTP: traduz DTO ↔ comando e delega para os use cases.
 * Não contém regras de negócio; apenas adapta Web para Application.
 */
@RestController
@RequestMapping("/coupon")
public class CouponController {

    private final CreateCouponUseCase createCouponUseCase;
    private final DeleteCouponUseCase deleteCouponUseCase;
    private final GetCouponUseCase getCouponUseCase;
    private final CouponWebMapper webMapper;

    public CouponController(CreateCouponUseCase createCouponUseCase,
                            DeleteCouponUseCase deleteCouponUseCase,
                            GetCouponUseCase getCouponUseCase,
                            CouponWebMapper webMapper) {
        this.createCouponUseCase = createCouponUseCase;
        this.deleteCouponUseCase = deleteCouponUseCase;
        this.getCouponUseCase = getCouponUseCase;
        this.webMapper = webMapper;
    }

    @PostMapping
    public ResponseEntity<CouponResponseDto> criarCupom(@Valid @RequestBody CouponRequestDto request) {
        Coupon created = createCouponUseCase.execute(
                request.getCode(),
                request.getDescription(),
                request.getDiscountValue(),
                request.getExpirationDate(),
                request.isPublished()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toResponseDto(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CouponResponseDto> buscarCoupon(@PathVariable String id) {
        UUID uuid = parseId(id);
        Coupon coupon = getCouponUseCase.execute(uuid);
        return ResponseEntity.ok(webMapper.toResponseDto(coupon));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CouponResponseDto> deletarCoupon(@PathVariable String id) {
        UUID uuid = parseId(id);
        Coupon deleted = deleteCouponUseCase.execute(uuid);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(webMapper.toResponseDto(deleted));
    }

    private static UUID parseId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID fornecido não pode ser nulo.");
        }
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("ID inválido: " + id);
        }
    }
}
