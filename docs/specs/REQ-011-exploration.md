# REQ-011 코드베이스 탐색과 기술 결정

<!-- 1~4절: 2단계(탐색)에서 Claude가 쓴다. 코드는 수정하지 않는다.
     5절: 3단계(인터뷰)의 기술 질문에 개발자가 답한 내용. 4단계에서 채운다.
     이 문서와 REQ-011-requirements.md가 SPEC의 입력이다. -->

## 1. 관련 코드

| 파일 | 역할 | REQ-011과의 관계 |
|---|---|---|
| `src/main/resources/schema.sql` | 개발 DB 스키마 | `product`에 마감 일수, `stock_lot`에 판매기한이 없다. 정정 이력 테이블도 없다 |
| `src/main/resources/data.sql` | 공통코드·샘플 데이터 | `PARTNER_STOCK_STATUS_MAP`에 `QUALITY → HLD` 한 줄뿐 (8~11행) |
| `stock/domain/HoldReason.java` | 보류 사유 enum | `QUALITY` 하나뿐 |
| `stock/domain/StockLot.java` | 로트 record | 판매기한 필드 없음 |
| `stock/domain/LotStatus.java` | 상태 결정 `resolve(qty, hasActiveHold)` | 수정 불필요. 사유와 상관없이 활성 보류가 있으면 HOLD |
| `stock/application/LotService.java` | 입고·출고·연동 대상 조회 | `receive`(41행)에 판매기한 산출 추가 필요. `ship`(52행)은 상태만 봐서 수정 불필요 |
| `stock/application/HoldService.java` | 보류 등록·해제 | `placeHold`(38행) 재사용 가능. `releaseHolds`(49행)는 풀 보류가 없으면 `NO_ACTIVE_HOLD` 예외(53행) |
| `stock/mapper/StockLotMapper.java`·`.xml`, `StockHoldMapper.java`·`.xml` | 로트·보류 매퍼 (SQL은 `resources/mapper/stock/`) | 판매기한 컬럼 반영(`stockLotMap`의 `<constructor>` 순서 포함), 배치 대상 조회 추가 필요 |
| `stock/api/LotController.java`, `LotResponse.java` | 입고·출고 API | 응답에 판매기한 추가 필요 |
| `stock/api/QualityHoldController.java` | 품질 보류 API | `QUALITY`만 다룸. 수정 불필요 |
| `product/domain/Product.java`, `product/mapper/ProductMapper.java`·`.xml`, `product/application/ProductService.java` | 상품 조회 | 마감 일수 필드·설정 기능 없음. `product/api` 패키지 없음 |
| `partner/application/PartnerStockSyncService.java` | 제휴몰 재고 연동 | 보류 사유를 공통코드로 상태 코드로 바꿈. 매핑이 없으면 예외(66행)로 실행 전체 롤백 |
| `common/batch/BatchRunner.java`, `partner/batch/PartnerStockSyncJob.java` | 배치 실행 패턴 | 새 0시 배치가 따를 패턴 |
| `common/auth/CurrentUser.java` | 사용자, `SYSTEM_USER_ID`(16행) | 배치 처리자 ID |
| `docs/interface-spec.md` | 제휴몰 연동 규격 | `STP`(판매불가, "다시 팔 수 없는 재고") 코드가 이미 정의돼 있음 |
| `docs/api.md` | API 목록 | 새 API 추가 필요 |
| `src/test/.../support/TestData.java`, `TestClock.java` | 테스트 지원 | 판매기한 있는 로트를 만드는 헬퍼 없음. 기본 시각 2026-10-12 10:00 |

## 2. 따를 기존 패턴

- API: `QualityHoldController` — 첫 줄 `user.requireRole(...)`, `@Valid` Request record, `ApiResponse.ok(XxxResponse.from(...))`
- 보류: 사유별 건 단위. 걸 때 `HoldService.placeHold`, 풀 때 `releaseHolds`, 상태는 `refreshStatus`가 다시 계산 (보류는 `HoldService`로만 다룬다, `.claude/rules/application.md`)
- 배치: `XxxJob`(@Scheduled) → `BatchRunner.run(JOB_NAME, service::method)` → Service `@Transactional`
- 기능 간 호출: stock은 상품 정보를 `ProductService`로만 가져온다 (`LotService.receive`가 예)
- 테스트: `@ShopIntegrationTest` + MockMvc, 날짜는 `TestClock.set(...)`, 데이터는 `TestData`

## 3. 영향 받는 곳

| 영역 | 내용 | 확인/추정 |
|---|---|---|
| 숨은 영향 | 새 보류 사유를 만들고 `PARTNER_STOCK_STATUS_MAP`에 매핑을 넣지 않으면, 그 사유가 걸린 첫 연동부터 제휴몰 재고 연동이 매번 전체 실패한다 | 확인 (`PartnerStockSyncService` 66행, `data.sql` 11행) |
| 계약 | 제휴몰 연동 규격에 없는 코드를 보내면 제휴몰이 묶음 전체를 거부한다. 규격에 있는 코드만 써야 한다 | 확인 (`interface-spec.md` 2장) |
| 계약 | `POST /api/lots` 응답에 필드가 추가된다 (기존 필드는 그대로) | 확인 |
| 데이터 | 샘플 로트 중 `LT-261002-0006`(소비기한 10-30, 품질 보류 중)은 판매기한 산출 시 10-09가 되어 TestClock 기준(10-12) 이미 지났다. `LT-261001-0002`(11-03 → 10-13)도 경계 근처 | 확인 |
| 시각 | `@Scheduled` cron은 zone을 지정하지 않으면 서버 시간대로 돈다. 기존 30분 배치는 상관없지만 0시 배치는 시간대가 중요하다 | 확인 (`PartnerStockSyncJob` 26행) |
| 예외 | 정정 때 무조건 `releaseHolds`를 부르면, 판매 중지가 없는 로트에서 `NO_ACTIVE_HOLD`(409)가 난다 | 확인 (`HoldService` 53행) |
| 운영 | `schema.sql`·`data.sql`은 운영 DB에 자동 반영되지 않는다 (DDL, 기존 로트 판매기한 계산, 공통코드) | 확인 (`schema.sql`·`data.sql` 머리 주석) |
| 운영 | 기존 코드는 새 판매기한 컬럼을 채우지 않는다. 기존 로트 판매기한을 배포 전에 채우면, 그 뒤 배포 전까지 입고된 로트는 판매기한이 비어 남는다 | 확인 (`StockLotMapper.xml` 27행 `insertLot`이 컬럼을 나열) |

## 4. 코드나 원문이 이미 답한 것 (인터뷰에서 묻지 않음)

- 판매기한 당일 판매 여부 → FN3 "판매기한 당일까지 판매"
- 판매 중지 로트 출고 차단 방법 → `LotService.ship`이 상태(AVAILABLE)만 본다
- 품질 보류와 겹칠 때 표현 → 보류가 이미 사유별 건 단위
- 품질 보류 해제가 판매 중지를 푸는지 → `QualityHoldController`는 `QUALITY`만 해제
- 판매 가능 재고 정의 → `CLAUDE.md` 도메인 메모 "AVAILABLE 로트의 수량 합"

## 5. 기술 결정 (인터뷰에서 개발자가 답함)

| # | 질문 | 결정 | 이유 |
|---|---|---|---|
| T1 | 판매 중지를 새 로트 상태로 둘까, 보류 사유로 둘까? | 보류 사유 `EXPIRED` 추가. 로트 상태는 그대로 | 막는 범위(출고 불가, 판매 가능 재고 제외)가 품질 보류와 같고 이유·해제 조건만 다르다 |
| T2 | 판매기한을 저장할까, 매번 계산할까? | `stock_lot.sale_deadline`에 저장 | 마감 일수를 바꿔도 기존 로트 판매기한이 바뀌지 않아야 한다(R3) |
| T3 | 제휴몰에 보낼 상태 코드는? | `EXPIRED → STP`, 우선순위 1 (품질 `HLD` 2보다 우선) | 규격의 STP 정의와 맞고, 정정으로 풀리면 AVL로 다시 보내면 제휴몰이 판매를 재개한다 |
| T4 | 0시 배치의 시각 기준은? | `@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")`, 판정일은 `LocalDate.now(clock)` | 서버 시간대와 상관없이 한국 0시 |
| T5 | 배치가 실패하거나 하루 건너뛰면? | 대상 조건을 `판매기한 < 판정일`로 한다. 다음 실행에서 밀린 로트도 모두 판매 중지 | "무조건" 판매 중지 |
| T6 | 배치·정정 즉시 판매 중지의 처리자는? 정정으로 해제할 때는? | 거는 쪽은 `CurrentUser.SYSTEM_USER_ID`, 정정으로 해제하는 쪽은 정정한 사용자 | 누가 걸고 누가 풀었는지 구분한다 |
| T7 | 한 번의 배치에서 일부 로트가 실패하면? | 기존 패턴대로 한 실행 = 한 트랜잭션. 실패하면 전체 롤백, 다음 실행에서 다시 처리 | T5로 다음 실행이 밀린 로트를 처리한다 |
| T8 | 정정 이력은 어디에? | 새 테이블 `expiry_correction` | 보류 이력과 성격이 다르다 |
| T9 | 새 API 경로와 위치는? | 마감 일수 `PUT /api/products/{productCode}/sale-close-days`(새 `ProductController`), 판매 가능 재고 `GET /api/products/{productCode}/available-stock`(새 `AvailableStockController`), 정정 `POST /api/lots/{lotNo}/expiry-corrections`(기존 `LotController`에 추가) | 기존 경로 체계를 따른다. 정정은 로트에 대한 물류 작업이라 입고·출고와 같은 컨트롤러 |
| T10 | 판매 가능 재고 조회는 어느 기능에 두나? | stock 기능(`stock/api`). 상품명은 `ProductService`로 가져온다 | 재고 계산은 stock 데이터다 |
| T11 | 스키마 변경 방식은? | 개발 DB는 기존 `CREATE TABLE`에 컬럼을 추가하고 새 테이블을 덧붙인다(시작할 때마다 다시 만듦). 운영 반영용 `ALTER TABLE` 문은 따로 보고 | 기존 컬럼은 바꾸지 않고 추가만 한다 |
| T12 | 배포 단위와 운영 반영 순서는? | 판매 중지 배치와 정정을 함께 배포. 운영 반영은 ① DDL·공통코드 매핑(배포 전) → ② 배포 → ③ 기존 로트 판매기한 UPDATE(배포 직후, 첫 0시 전, `sale_deadline IS NULL`인 행만) | 잘못 막힌 로트를 바로 풀 수 있어야 하고, 매핑이 없으면 제휴몰 연동이 깨진다. 기존 코드는 INSERT에 컬럼을 나열하므로 컬럼이 먼저 생겨도 동작한다. UPDATE를 배포 전에 돌리면 그 뒤 배포 전까지 기존 코드로 입고된 로트의 판매기한이 비어 판매 중지되지 않는다 |
| T13 | 보류 변화가 없는 정정도 이력을 남기나? | 예. 정정할 때마다 남긴다 (소진 로트 정정 포함, R15) | 정정 행위 자체를 추적한다 |
| T14 | 정정할 때 로트의 상품이 사용 중지(use_yn='N')면? | 기존 `ProductService.getProduct`대로 404 `PRODUCT_NOT_FOUND` | 기존 동작을 그대로 쓴다 |
| T15 | 같은 날짜로 하는 정정(R14)은 어떻게 거부하나? | 409 `EXPIRY_DATE_UNCHANGED`를 새로 만든다. 거부할 때는 소비기한·판매기한·보류·이력을 바꾸지 않는다 | 요청 형식은 맞고 현재 상태와 같아서 거부하는 것이라 400보다 409(상태 충돌)가 맞다. `INVALID_REQUEST`로는 이유를 구분할 수 없다 |
| T16 | 0시 판매 중지와 30분 재고 연동이 겹치면? | 별도 처리 없음. 다음 정기 연동에서 `STP`로 나간다 | 최대 30분 지연을 고객이 수용했다(R8). 판매 중지 직후 연동을 바로 돌리려면 stock이 partner를 불러야 해서 기능 사이 의존 방향(partner → stock)을 거스른다 |
| T17 | 판매 마감 일수는 어디에 저장하나? | `product.sale_close_days` (기본 21) | 이 시스템이 관리하는 상품별 설정이다(R1). 상품 관리 시스템에서 다시 받아 올 때는 기본정보 컬럼만 갱신되므로 덮어써지지 않는다(`docs/domain-guide.md`) |
