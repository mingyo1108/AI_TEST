package com.example.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 식품 온라인몰 재고 백오피스 (실습용 가상 시스템).
 */
@SpringBootApplication
public class ShopApplication {

    /** 애플리케이션을 시작한다. */
    public static void main(String[] args) {
        SpringApplication.run(ShopApplication.class, args);
    }
}
