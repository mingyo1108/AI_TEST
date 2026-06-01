package com.example.shop.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 배치 스케줄 활성화. {@code shop.scheduling.enabled=false}이면 배치가 돌지 않는다(테스트용).
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "shop.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
