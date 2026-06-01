package com.example.shop.support;

import com.example.shop.config.ClockConfig;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * 테스트용 시계. 테스트에서 현재 시각을 정할 수 있다.
 *
 * <pre>
 * {@literal @}Autowired TestClock clock;
 * clock.set(LocalDateTime.of(2026, 11, 1, 0, 0));
 * </pre>
 */
public class TestClock extends Clock {

    /** 기본 현재 시각. */
    public static final LocalDateTime DEFAULT_NOW = LocalDateTime.of(2026, 10, 12, 10, 0);

    private final ZoneId zone = ClockConfig.SHOP_ZONE;
    private Instant instant = toInstant(DEFAULT_NOW);

    /** 현재 시각을 바꾼다. */
    public void set(LocalDateTime now) {
        this.instant = toInstant(now);
    }

    /** 기본 현재 시각으로 돌린다. */
    public void reset() {
        set(DEFAULT_NOW);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zoneId) {
        return Clock.fixed(instant, zoneId);
    }

    @Override
    public Instant instant() {
        return instant;
    }

    private Instant toInstant(LocalDateTime dateTime) {
        return dateTime.atZone(zone).toInstant();
    }

    /** 운영 시계 대신 테스트 시계를 쓰게 한다. */
    @TestConfiguration
    public static class Config {

        @Bean
        @Primary
        TestClock testClock() {
            return new TestClock();
        }
    }

    /** 테스트가 끝날 때마다 시계를 기본값으로 돌린다. */
    public static class ResetExtension implements AfterEachCallback {

        @Override
        public void afterEach(ExtensionContext context) {
            SpringExtension.getApplicationContext(context).getBean(TestClock.class).reset();
        }
    }
}
