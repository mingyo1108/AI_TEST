# shop-advanced

식품 온라인몰의 재고 백오피스 (실습용 가상 시스템). 상품, 입고 로트 재고, 판매 보류(품질 보류 등), 출고,
제휴몰 재고 연동을 담당한다.

## 기술 스택과 명령

- Java 25, Spring Boot 4.1 (Spring MVC, MyBatis), Gradle 9 (wrapper 포함), H2(MSSQL 호환 모드)
- DB 접근은 MyBatis: 매퍼 인터페이스(`<기능>/mapper/XxxMapper.java`) + 매퍼 XML(`src/main/resources/mapper/<기능>/XxxMapper.xml`). JPA 없음. 배치는 Spring `@Scheduled`
- SQL은 MSSQL 문법(H2 `MODE=MSSQLServer`)으로 쓴다. 사내 MyBatis 컨벤션은 `.claude/rules/mybatis.md`

| 할 일 | 명령 |
|---|---|
| 전체 테스트 | `./gradlew test` |
| 테스트 하나 | `./gradlew test --tests "com.example.shop.stock.api.QualityHoldApiTest"` |
| DB 초기화 (개발·운영 흉내) | `./gradlew dbReset` |
| SQL 실행 (개발 DB) | `node scripts/db.mjs --env dev "SELECT ..."` |
| 매퍼 SQL 렌더링 | `node scripts/render-sql.mjs XxxMapper.queryId params.json` |
| 린트 | `./gradlew checkstyleMain` |
| 실행 | `./gradlew bootRun` (http://localhost:8080) |

## 코드 구조 (`src/main/java/com/example/shop`)

최상위 패키지는 **기능**(`product`, `stock`, `partner`), 기능 안은 **계층**으로 나눈다. `common`·`config`는 공통이다.

| 계층 | 두는 것 |
|---|---|
| `api` | Controller, `XxxRequest`, `XxxResponse` |
| `application` | Service (트랜잭션 경계) |
| `domain` | record, enum, 업무 규칙 (순수 Java) |
| `mapper` | 매퍼 인터페이스 (SQL은 `resources/mapper/<기능>/`의 XML) |
| `infra` | 외부 연동 (I/F 테이블 송신) |
| `batch` | `XxxJob` (배치가 있는 기능만) |

## 핵심 규칙

- **의존 방향**: 계층은 api → application → domain·mapper 방향으로만 의존하고, 다른 기능은 그 기능의 application Service로만 사용한다 (그 Service가 주고받는 domain 타입은 import해도 되고, 다른 기능의 mapper·api·batch·infra는 import하지 않는다).
- **비즈니스 로직**: 업무 규칙은 domain에 두고, Controller는 입력 검증·역할 확인·DTO 변환만 한다.
- **예외 처리**: 업무 오류와 권한 오류는 `ErrorCode`에 등록된 `BusinessException`으로만 던진다.
- **날짜 처리**: 현재 날짜와 시각은 주입받은 `Clock`으로만 구한다.
- **완료 조건**: `./gradlew test`와 `./gradlew checkstyleMain`을 통과시킨 뒤에만 완료로 보고한다.
- **범위**: 요청 범위 밖의 리팩터링은 하지 않고, 기존 코드를 바꾸기 전에 영향 범위를 먼저 확인한다.
- **운영 DB**: 운영 DB(`--env prod`)는 조회만 한다. 데이터 변경은 사람이 승인한 스크립트로만 한다.
- **언어**: 답변과 문서는 한국어로 쓴다.

## 세부 규칙과 문서

`.claude/rules/`의 규칙은 해당 파일을 다룰 때 자동으로 적용된다.

| 주제 | 위치 |
|---|---|
| API 작성, `docs/api.md` 갱신 | `.claude/rules/api.md` |
| 매퍼 Java·XML 작성 (사내 MyBatis 컨벤션) | `.claude/rules/mybatis.md` |
| 스키마·기준 데이터 변경, 운영 반영 보고, DB 도구 | `.claude/rules/database.md` |
| 서비스 간 호출, 트랜잭션 | `.claude/rules/application.md` |
| 배치 | `.claude/rules/batch.md` |
| 테스트 (날짜, 배치, 테스트 데이터) | `.claude/rules/testing.md` |
| 커밋 메시지 | `.claude/rules/git.md` |
| 업무 용어·흐름 | `docs/domain-guide.md` |
| API 목록 / 외부 연동 규격 | `docs/api.md` / `docs/interface-spec.md` |

## 도메인 메모

- 로트 상태: `AVAILABLE`(판매 가능) / `HOLD`(판매 보류) / `SOLD_OUT`(소진). 판매 가능 재고는 `AVAILABLE` 로트의 수량 합이다.
- 판매 보류는 사유별 건 단위로 `stock_hold`에 기록하고 사유별로 해제한다. 활성 보류가 하나라도 남아 있으면 로트는 `HOLD`다.
- 제휴몰 재고 연동은 I/F 테이블을 거친다. 형식은 `docs/interface-spec.md`.
