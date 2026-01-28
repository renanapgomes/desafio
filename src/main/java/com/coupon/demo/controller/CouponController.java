package com.coupon.demo.controller;

import com.coupon.demo.dto.request.CouponRequestDto;
import com.coupon.demo.dto.response.CouponResponseDto;
import com.coupon.demo.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/coupon")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping
    public ResponseEntity<CouponResponseDto> criarCupom(@Valid @RequestBody CouponRequestDto request){

        CouponResponseDto response = couponService.criarCupom(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CouponResponseDto> buscarCoupon(@PathVariable String id){

        CouponResponseDto cupomEncontrado = couponService.buscarCupomPorId(id);

        return ResponseEntity.ok(cupomEncontrado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CouponResponseDto> deletarCoupon(@PathVariable String id){

        CouponResponseDto cupomDeletado = couponService.deletarCupom(id);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(cupomDeletado);
    }
}
