package com.example.shop.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 테스트 공통 설정.
 *
 * <ul>
 *   <li>스프링 컨텍스트 + 내장 H2 (schema.sql, data.sql 적용)</li>
 *   <li>배치 스케줄 끔 (shop.scheduling.enabled=false)</li>
 *   <li>MockMvc 사용 가능</li>
 *   <li>테스트마다 트랜잭션 롤백</li>
 *   <li>시계는 {@link TestClock} (기본 2026-10-12 10:00, 테스트가 끝나면 기본값으로 돌아감)</li>
 * </ul>
 *
 * 모든 통합 테스트가 같은 설정을 쓰면 스프링 컨텍스트를 한 번만 띄운다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = "shop.scheduling.enabled=false")
@AutoConfigureMockMvc
@Import(TestClock.Config.class)
@ExtendWith(TestClock.ResetExtension.class)
@Transactional
public @interface ShopIntegrationTest {
}
