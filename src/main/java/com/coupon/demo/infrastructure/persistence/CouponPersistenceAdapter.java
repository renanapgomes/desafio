package com.coupon.demo.infrastructure.persistence;

import com.coupon.demo.application.port.LoadCouponPort;
import com.coupon.demo.application.port.SaveCouponPort;
import com.coupon.demo.domain.Coupon;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter de persistência: implementa as portas usando JPA.
 * Converte entre domain.Coupon e CouponEntity; application não vê JPA.
 */
@Component
public class CouponPersistenceAdapter implements SaveCouponPort, LoadCouponPort {

    private final CouponRepository couponRepository;

    public CouponPersistenceAdapter(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Override
    public Coupon save(Coupon coupon) {
        CouponEntity entity = toEntity(coupon);
        CouponEntity saved = couponRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Coupon> findById(UUID id) {
        return couponRepository.findById(id).map(this::toDomain);
    }

    private CouponEntity toEntity(Coupon coupon) {
        CouponEntity entity = new CouponEntity();
        entity.setId(coupon.getId());
        entity.setCode(coupon.getCode());
        entity.setDescription(coupon.getDescription());
        entity.setDiscountValue(coupon.getDiscountValue());
        entity.setExpirationDate(coupon.getExpirationDate());
        entity.setStatus(coupon.getStatus());
        entity.setPublished(coupon.isPublished());
        return entity;
    }

    private Coupon toDomain(CouponEntity entity) {
        return Coupon.reconstitute(
                entity.getId(),
                entity.getCode(),
                entity.getDescription(),
                entity.getDiscountValue(),
                entity.getExpirationDate(),
                entity.getStatus(),
                entity.isPublished()
        );
    }
}
