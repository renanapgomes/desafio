package com.coupon.demo.domain;


public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
