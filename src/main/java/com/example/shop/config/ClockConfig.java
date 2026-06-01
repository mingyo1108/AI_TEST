package com.example.shop.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 시각 기준. 한국 시간(Asia/Seoul)을 쓴다. 현재 시각·날짜는 항상 이 Clock으로 구한다.
 */
@Configuration
public class ClockConfig {

    /** 서비스 시간대. */
    public static final ZoneId SHOP_ZONE = ZoneId.of("Asia/Seoul");

    /** 서비스 시간대의 시스템 시계. */
    @Bean
    public Clock clock() {
        return Clock.system(SHOP_ZONE);
    }
}
