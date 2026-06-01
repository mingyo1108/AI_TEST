package com.example.shop.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

/**
 * 매퍼 테스트 공통 설정 (MyBatis 테스트 슬라이스).
 *
 * <ul>
 *   <li>매퍼와 데이터소스만 띄운다 (Controller·Service 없음). 통합 테스트보다 빠르다</li>
 *   <li>DB는 application.yml 의 인메모리 H2 (MSSQL 모드) + schema.sql, data.sql. 내장 DB로 바꾸지 않는다</li>
 *   <li>테스트마다 트랜잭션 롤백</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public @interface ShopMapperTest {
}
