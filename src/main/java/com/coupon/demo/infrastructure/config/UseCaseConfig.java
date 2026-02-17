package com.coupon.demo.infrastructure.config;

import com.coupon.demo.application.port.LoadCouponPort;
import com.coupon.demo.application.port.SaveCouponPort;
import com.coupon.demo.application.usecase.CreateCouponUseCase;
import com.coupon.demo.application.usecase.DeleteCouponUseCase;
import com.coupon.demo.application.usecase.GetCouponUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra os use cases como beans. Application não usa Spring;
 * a composição fica na infra.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public CreateCouponUseCase createCouponUseCase(SaveCouponPort saveCouponPort) {
        return new CreateCouponUseCase(saveCouponPort);
    }

    @Bean
    public DeleteCouponUseCase deleteCouponUseCase(LoadCouponPort loadCouponPort, SaveCouponPort saveCouponPort) {
        return new DeleteCouponUseCase(loadCouponPort, saveCouponPort);
    }

    @Bean
    public GetCouponUseCase getCouponUseCase(LoadCouponPort loadCouponPort) {
        return new GetCouponUseCase(loadCouponPort);
    }
}
