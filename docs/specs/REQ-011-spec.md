# REQ-011 판매기한 지난 로트 자동 판매 중지

기준: REQ-011-requirements.md, REQ-011-exploration.md

## 1. 목표

로트마다 판매기한(소비기한 − 상품별 판매 마감 일수)을 정하고, 판매기한이 지난 로트를 매일 0시에 자동으로 판매 중지한다. 판매 중지는 소비기한 정정으로만 풀린다. MD는 마감 일수를 설정하고 판매 가능 재고로 반영을 확인한다.

완료 조건: 7절 수용 기준과 9절 E2E 검증을 모두 통과한다.

## 2. 범위

### 구현할 것

- 상품별 판매 마감 일수 설정 API (MD)
- 입고 때 판매기한 산출
- 매일 0시 판매 중지 배치
- 소비기한 정정 API (WAREHOUSE)와 정정 이력
- 상품별 판매 가능 재고 조회 API (MD)
- 판매 중지 로트를 제휴몰에 `STP`로 보내는 공통코드 매핑
- 기존 로트 판매기한 일괄 산출 (개발 DB는 `data.sql`, 운영은 보고)
- `docs/api.md`에 새 API 3개 추가

### 구현하지 말 것

- 판매기한 순 출고(선입선출) — REQ-019 담당
- 판매 중지 해제 API, 결재 기능 — 판매 중지는 정정으로만 풀린다
- 입고 시점의 판매 중지 처리 — 판매기한이 지난 로트도 입고는 판매 가능으로 하고, 0시 배치가 처리한다
- 주문 취소·대체 출고 — 이 시스템 범위 밖
- 판매 마감 일수 조회 전용 API
- 제휴몰 재고 연동의 실패 처리 구조 변경 (한 로트 매핑 실패 시 전체 롤백 구조 유지)
- 새 로트 상태 추가 (`LotStatus`는 그대로)
- 정정 이력 조회 API — 이력은 `expiry_correction`에 남기기만 한다
- 판매 중지 직후 제휴몰 재고 연동 즉시 실행 — 다음 정기 연동(30분 주기)에서 `STP`로 나간다

### 수정 금지

- `partner/` 아래 모든 Java 파일, `common/batch/BatchRunner.java`
- `HoldService.placeHold`·`releaseHolds`의 시그니처와 동작, `LotService.ship`, `QualityHoldController`
- `docs/interface-spec.md`
- `schema.sql`·`data.sql`의 기존 컬럼과 기존 문장 (추가만. 단, `product`에 컬럼을 붙이려고 `use_yn` 줄 끝에 쉼표를 넣는 것은 허용)
- 기존 테스트 메서드, `TestData`의 기존 메서드 (새 헬퍼 추가는 허용)

## 3. 규칙

| ID | 규칙 | 근거 |
|---|---|---|
| R1 | 판매 마감 일수는 상품별 1~365일, 기본 21일. 이 시스템에서 MD만 설정한다 (`product.sale_close_days`) | 상품마다 유통 특성이 다르다. 상품 관리 시스템에는 이 값이 없다 |
| R2 | 판매기한 = 소비기한 − 마감 일수. 오늘이 판매기한보다 뒤면 "지남" (당일까지 판매) | 고객이 받을 때 소비기한이 남아야 한다 |
| R3 | 판매기한은 입고 때 저장한다. 마감 일수를 바꿔도 기존 로트 판매기한은 그대로 | 설정 변경으로 기존 재고가 한꺼번에 막히지 않게 |
| R4 | 소비기한이 없으면 판매기한도 없고 판매 중지 대상이 아니다 | 계산할 수 없다 |
| R5 | 판매 중지 = 사유 `EXPIRED`인 판매 보류. 로트당 활성 `EXPIRED`는 하나 | 기존 사유별 보류 구조를 쓴다 |
| R6 | 입고는 판매기한과 상관없이 AVAILABLE로 만든다 | 판매기한이 지난 상품은 입고 검수에서 반려된다 |
| R7 | 판매 중지는 다른 사유의 보류와 상관없이 걸고, 다른 보류는 건드리지 않는다 | "무조건" 판매 중지 |
| R8 | 판매 중지 로트는 출고 불가, 판매 가능 재고 제외, 제휴몰에 `STP` (`QUALITY`와 함께여도 `STP`). 제휴몰에는 다음 정기 연동에서 반영한다 | 어디서도 팔리면 안 된다. 최대 30분 지연은 수용됨 |
| R9 | 판매 중지는 정정으로만 풀린다. 품질 보류 해제로는 풀리지 않는다 | 사람이 판단해 다시 파는 일을 막는다 |
| R10 | 정정하면 상품의 현재 마감 일수로 판매기한을 다시 계산한다. 안 지났으면 `EXPIRED`만 해제, 지났으면 `EXPIRED`를 건다(없을 때만, 소진 로트 제외). 다른 사유의 보류는 해제하지 않는다 | 정정 결과도 같은 규칙을 따른다 |
| R11 | 정정 사유 필수(공백 불가, 500자 이내). 정정 전·후 소비기한·판매기한, 사유, 처리자, 일시 기록 | 정정이 우회 수단이 되지 않게 추적 |
| R12 | 판매 가능 재고 = 그 상품의 `AVAILABLE` 로트 수량 합. `HOLD` 로트 수량은 `heldQty`로 따로 돌려준다 | 판매 중지 반영을 MD가 확인 |
| R13 | 0시 배치 대상: `AVAILABLE`·`HOLD`이고 `sale_deadline < 판정일`이며 활성 `EXPIRED`가 없는 로트 | 배치가 밀려도 다음 실행에서 모두 처리 |
| R14 | 시스템이 거는 판매 중지(배치, 정정 후 재판정)의 처리자는 `CurrentUser.SYSTEM_USER_ID`. 정정으로 `EXPIRED`를 해제할 때의 처리자는 정정한 사용자 | 누가 걸고 누가 풀었는지 구분 |
| R15 | 판매 중지 배치는 매일 0시(Asia/Seoul)에 돈다 | 판매기한 다음 날 0시 중지(R2)를 서버 시간대와 상관없이 지킨다 |
| R16 | 정정 이력은 보류 변화가 없어도 정정할 때마다 남긴다. 소진 로트도 정정할 수 있다 | 정정 자체를 추적한다 |
| R17 | 정정할 소비기한이 지금 소비기한과 같으면 409 `EXPIRY_DATE_UNCHANGED`로 거부한다. 소비기한·판매기한·보류·이력은 바뀌지 않는다 | 정정은 오입력을 바로잡을 때만 한다. 마감 일수를 바꾼 뒤 같은 날짜로 정정해 판매 중지를 푸는 일을 막는다 |
| R18 | 소비기한이 없는 로트에 소비기한을 넣는 것도 정정이다. 이력의 정정 전 소비기한·판매기한은 null이고, 이후 R10을 따른다 | 입고 때 소비기한을 빠뜨린 것도 오입력이다 |

## 4. 상태 전이

| 현재 | 이벤트 | 다음 | 허용 | AC |
|---|---|---|---|---|
| (없음) | 입고 | AVAILABLE | O | AC-1, AC-9, AC-10 |
| AVAILABLE | 0시 배치, 판매기한 지남 | HOLD [EXPIRED] | O | AC-6 |
| HOLD [QUALITY] | 0시 배치, 판매기한 지남 | HOLD [QUALITY, EXPIRED] | O | AC-11 |
| HOLD [EXPIRED] | 0시 배치 | 변화 없음 (중복 없음) | O | AC-7 |
| AVAILABLE | 정정, 다시 계산한 판매기한 지남 | HOLD [EXPIRED] | O | AC-17 |
| HOLD [EXPIRED] | 정정, 안 지남 | AVAILABLE | O | AC-15 |
| HOLD [EXPIRED] | 정정, 지남 | 변화 없음 | O | AC-16 |
| HOLD [QUALITY, EXPIRED] | 정정, 안 지남 | HOLD [QUALITY] | O | AC-18 |
| HOLD [QUALITY] | 정정, 안 지남 | HOLD [QUALITY] (품질 보류 유지) | O | AC-18 |
| HOLD [QUALITY, EXPIRED] | 품질 보류 해제 | HOLD [EXPIRED] | O | AC-14 |
| HOLD [EXPIRED] | 품질 보류 해제 | 409 `NO_ACTIVE_HOLD`, 변화 없음 | X | AC-14 |
| AVAILABLE (소비기한 없음) | 정정, 다시 계산한 판매기한 지남 | HOLD [EXPIRED] | O | AC-30 |
| 모든 상태 | 지금과 같은 소비기한으로 정정 | 409 `EXPIRY_DATE_UNCHANGED`, 변화 없음 | X | AC-29 |
| SOLD_OUT | 0시 배치, 정정 | 판매 중지 걸지 않음 | X | AC-27 |

## 5. 관련 코드

경로는 `src/main/java/com/example/shop/` 기준.

- 수정
  - `src/main/resources/schema.sql` — `product.sale_close_days`, `stock_lot.sale_deadline` 컬럼 추가, `expiry_correction` 테이블 추가
  - `src/main/resources/data.sql` — `EXPIRED` 매핑 추가, 끝에 기존 로트 판매기한 산출 `UPDATE` 추가
  - `stock/domain/HoldReason.java` — `EXPIRED` 추가
  - `stock/domain/StockLot.java` — `saleDeadline`(`LocalDate`, nullable)을 `expiryDate` 바로 뒤에 추가
  - `stock/mapper/StockLotMapper.java`, `resources/mapper/stock/StockLotMapper.xml` — `sale_deadline` 매핑(`stockLotMap` `<constructor>`)·저장, `selectSaleDeadlinePassedList(LocalDate today)`, `updateLotExpiry(lotNo, expiryDate, saleDeadline)`, 상품별 재고 로트 조회 `selectInStockListByProduct(productCode)`
  - `stock/application/LotService.java` — `receive`에서 판매기한 산출, 상품별 판매 가능 재고 조회 추가
  - `stock/api/LotController.java` — `POST /{lotNo}/expiry-corrections` 추가
  - `stock/api/LotResponse.java` — `saleDeadline` 추가
  - `product/domain/Product.java`, `product/mapper/ProductMapper.java`·`.xml`, `product/application/ProductService.java` — `saleCloseDays` 조회·수정
  - `common/ErrorCode.java` — 재고 구역에 `EXPIRY_DATE_UNCHANGED` 추가
  - `docs/api.md` — 새 API 3개
- 신규
  - `stock/domain/SaleDeadline.java` — `calculate(expiryDate, saleCloseDays)`(소비기한 null이면 null), `isPassed(saleDeadline, today)`(null이면 false, `today.isAfter(saleDeadline)`)
  - `stock/domain/AvailableStock.java` — `productCode, productName, availableQty, heldQty`
  - `stock/application/SaleStopService.java` — `stopExpired(LocalDate today)`
  - `stock/application/ExpiryCorrectionService.java` — `LotDetail correct(lotNo, expiryDate, reason, userId)`
  - `stock/batch/SaleStopJob.java` — 0시 배치
  - `stock/mapper/ExpiryCorrectionMapper.java`, `resources/mapper/stock/ExpiryCorrectionMapper.xml` — 정정 이력 저장 `insertExpiryCorrection`
  - `stock/api/ExpiryCorrectionRequest.java`
  - `stock/api/AvailableStockController.java`, `AvailableStockResponse.java`
  - `product/api/ProductController.java`, `SaleCloseDaysRequest.java`, `ProductResponse.java`
- 테스트 지원
  - `src/test/.../support/TestData.java` — 판매기한이 있는 로트 만들기, 판매 마감 일수 설정·조회, 로트의 판매기한 조회, 활성 보류의 처리자·코멘트 조회, 정정 이력 조회
- 참고 패턴
  - `stock/api/QualityHoldController.java` — 컨트롤러 구성, 역할 확인, 응답
  - `partner/batch/PartnerStockSyncJob.java` + `common/batch/BatchRunner.java` — 배치 구성
  - `stock/mapper/StockHoldMapper.java`·`.xml` — 매퍼 구성 (`.claude/rules/mybatis.md`)
  - `src/test/.../stock/api/QualityHoldApiTest.java` — API 테스트 구성

주의
- 정정에서 보류는 `EXPIRED`만 다룬다. `releaseHolds(lotNo, EXPIRED, ...)`는 활성 `EXPIRED`가 있을 때만 부른다. 없으면 `NO_ACTIVE_HOLD`(409)가 난다. 모든 보류를 푸는 메서드를 만들지 않는다.
- `EXPIRED` 매핑 없이 `EXPIRED` 보류가 생기면 제휴몰 재고 연동이 매번 전체 실패한다. 매핑은 `HoldReason.EXPIRED`를 추가하는 Task 3에서 함께 넣는다.
- 배치 대상 조회 SQL(매퍼 XML)의 `sale_deadline < #{today}`(XML에서는 `&lt;`)는 `SaleDeadline.isPassed`와 같은 규칙이다. 한쪽을 바꾸면 다른 쪽도 바꾼다.

## 6. 인터페이스 변경

### DB

개발 DB(`schema.sql`, 시작할 때마다 다시 만듦)

```sql
-- product: 기존 컬럼 뒤에 추가
sale_close_days INT DEFAULT 21 NOT NULL,   -- 판매 마감 일수
-- stock_lot: expiry_date 뒤에 추가
sale_deadline  DATE,                      -- 판매기한 = 소비기한 - 판매 마감 일수

CREATE TABLE expiry_correction (
    correction_id         BIGINT       IDENTITY PRIMARY KEY,
    lot_no                VARCHAR(20)  NOT NULL,
    before_expiry_date    DATE,
    after_expiry_date     DATE         NOT NULL,
    before_sale_deadline  DATE,
    after_sale_deadline   DATE         NOT NULL,
    reason                VARCHAR(500) NOT NULL,
    corrected_by          VARCHAR(20)  NOT NULL,
    corrected_at          TIMESTAMP    NOT NULL
);
```

운영 반영 DDL (보고만)

```sql
ALTER TABLE product ADD COLUMN sale_close_days INT DEFAULT 21 NOT NULL;
ALTER TABLE stock_lot ADD COLUMN sale_deadline DATE;
-- expiry_correction CREATE TABLE은 위와 같음
```

### 기준 데이터 (`data.sql`, 운영 반영 대상)

```sql
-- PARTNER_STOCK_STATUS_MAP: 기존 INSERT 문은 그대로 두고 별도 INSERT 문으로 추가
INSERT INTO common_code (group_code, code, code_name, attr1, attr2, sort_order) VALUES
('PARTNER_STOCK_STATUS_MAP', 'EXPIRED', '판매기한 경과 → 판매불가', 'STP', '1', 2);

-- 파일 끝: 기존 로트 판매기한 산출
UPDATE stock_lot l
   SET sale_deadline = DATEADD('DAY',
         -(SELECT p.sale_close_days FROM product p WHERE p.product_code = l.product_code), l.expiry_date)
 WHERE l.expiry_date IS NOT NULL
   AND l.sale_deadline IS NULL;
```

### 운영 반영 순서

1. 배포 전: 운영 DDL(컬럼 2개, 테이블 1개)과 `EXPIRED` 공통코드 INSERT. 기존 코드는 INSERT에 컬럼을 나열하므로 컬럼이 먼저 생겨도 동작한다
2. 애플리케이션 배포
3. 배포 직후, 첫 0시 배치 전: 기존 로트 판매기한 산출 UPDATE (위 `data.sql`과 같은 SQL). 배포 전에 실행하면 실행 뒤 배포 전까지 기존 코드로 입고된 로트의 판매기한이 비어 판매 중지 대상에서 빠진다. `sale_deadline IS NULL` 조건으로 새 코드가 이미 계산한 값은 덮어쓰지 않는다

### API

| 메서드 | 경로 | 역할 | 요청 | 응답 | 오류 |
|---|---|---|---|---|---|
| PUT | `/api/products/{productCode}/sale-close-days` | MD | `{"saleCloseDays": 7}` 필수, 1~365 | 200 `{"productCode", "productName", "category", "saleCloseDays"}` | 400 `INVALID_REQUEST`, 403 `FORBIDDEN`, 404 `PRODUCT_NOT_FOUND` |
| GET | `/api/products/{productCode}/available-stock` | MD | - | 200 `{"productCode", "productName", "availableQty", "heldQty"}` | 403 `FORBIDDEN`, 404 `PRODUCT_NOT_FOUND` |
| POST | `/api/lots/{lotNo}/expiry-corrections` | WAREHOUSE | `{"expiryDate": "2026-12-31", "reason": "입고 때 소비기한 오입력"}` 둘 다 필수, 사유 공백 불가·500자 이내 | 200 `LotResponse` | 400 `INVALID_REQUEST`, 403 `FORBIDDEN`, 404 `LOT_NOT_FOUND`, 404 `PRODUCT_NOT_FOUND`(로트의 상품이 사용 중지된 경우), 409 `EXPIRY_DATE_UNCHANGED`(지금 소비기한과 같음) |
| POST | `/api/lots` (기존) | WAREHOUSE | 변경 없음 | `LotResponse`에 `saleDeadline` 추가 | 변경 없음 |

`LotResponse` 변경

```java
// 전
record LotResponse(String lotNo, String productCode, int qty, String status, LocalDate expiryDate,
        List<String> activeHoldReasons)
// 후
record LotResponse(String lotNo, String productCode, int qty, String status, LocalDate expiryDate,
        LocalDate saleDeadline, List<String> activeHoldReasons)
```

새 `ErrorCode`는 `EXPIRY_DATE_UNCHANGED(HttpStatus.CONFLICT, "소비기한이 바뀌지 않는 정정은 할 수 없습니다")` 하나만 만든다. 그 밖에 기존 코드로 안 되는 경우가 생기면 멈추고 보고한다.

### 배치

```java
// stock/batch/SaleStopJob.java
public static final String JOB_NAME = "SALE_STOP";

@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")   // 매일 0시(한국). 다른 시각 금지
public void run() {
    batchRunner.run(JOB_NAME, () -> saleStopService.stopExpired(LocalDate.now(clock)));
}
```

`stopExpired`는 한 트랜잭션에서 R13 대상에 `HoldService.placeHold(lotNo, EXPIRED, ..., SYSTEM)`를 걸고, 건 로트 수를 돌려준다.

보류 코멘트
- 판매 중지(배치, 정정 후 재판정): `판매기한 경과 (판매기한 yyyy-MM-dd)`
- 정정으로 해제: `소비기한 정정 (판매기한 yyyy-MM-dd)`

## 7. 수용 기준

테스트 파일은 `src/test/java/com/example/shop/` 기준. 날짜·배치 테스트 방법은 `.claude/rules/testing.md`를 따른다. 판매기한 계산·판정은 `stock/domain/SaleDeadlineTest`로 단위 검증한다.

| AC | Given | When | Then | 유형 | 규칙 | 검증 |
|---|---|---|---|---|---|---|
| AC-1 | 마감 일수 미설정 상품 `P1001` | 소비기한 2026-11-21로 입고 | `saleDeadline` 2026-10-31, AVAILABLE | 정상 | R1, R2 | `stock/api/LotApiTest` |
| AC-2 | `P1001` 마감 일수 7일로 설정 | 소비기한 2026-11-21로 입고 | `saleDeadline` 2026-11-14 | 정상 | R1 | `product/api/ProductApiTest` |
| AC-3 | 마감 일수 21일 | 0, 366, null로 설정 | 400 `INVALID_REQUEST`, 21일 유지 | 예외 | R1 | `ProductApiTest` |
| AC-4 | WAREHOUSE 사용자 / 없는 상품 | 마감 일수 설정 | 403 `FORBIDDEN` / 404 `PRODUCT_NOT_FOUND` | 예외 | R1 | `ProductApiTest` |
| AC-5 | 판매기한 2026-10-31 AVAILABLE 로트 | 시계 2026-10-31 00:00, 배치 | AVAILABLE 유지 | 정상 | R2, R13 | `stock/application/SaleStopServiceTest` |
| AC-6 | 같은 로트 | 시계 2026-11-01 00:00, 배치 | HOLD, 활성 `EXPIRED` 1건, 처리자 `SYSTEM` | 정상 | R2, R13, R14 | `SaleStopServiceTest` |
| AC-7 | 활성 `EXPIRED`가 있는 로트 | 다음 날 `SaleStopJob.run()` | 활성 `EXPIRED` 1건 그대로, 배치 SUCCESS | 예외 | R5, R13 | `stock/batch/SaleStopJobTest` |
| AC-8 | 마감 일수 21일일 때 입고한 로트(판매기한 2026-10-31) | 마감 일수를 7일로 변경 | 로트 `sale_deadline` 2026-10-31 그대로 | 정상 | R3 | `ProductApiTest` |
| AC-9 | 소비기한 없이 입고한 로트 | 배치 | AVAILABLE, `saleDeadline` null | 예외 | R4 | `LotApiTest`, `SaleStopServiceTest` |
| AC-10 | 시계 2026-11-01 10:00 | 소비기한 2026-11-21로 입고, 시계 2026-11-02 00:00 배치 | 입고 응답 AVAILABLE·`saleDeadline` 2026-10-31, 배치 후 HOLD [EXPIRED] | 예외 | R6, R13 | `SaleStopServiceTest` |
| AC-11 | 품질 보류 중, 판매기한 지난 로트 | 배치 | 활성 `QUALITY`·`EXPIRED` 각 1건, HOLD | 정상 | R7 | `SaleStopServiceTest` |
| AC-12 | 판매 중지 로트 | 출고 | 409 `LOT_NOT_SHIPPABLE`, 수량 그대로 | 예외 | R8 | `LotApiTest` |
| AC-13 | 판매 중지 로트, 판매 중지 + 품질 보류 로트 | 제휴몰 재고 연동 | 둘 다 `STP` | 정상 | R8 | `partner/application/PartnerStockSyncServiceTest` |
| AC-14 | 판매 중지 + 품질 보류 로트 / 판매 중지만 걸린 로트 | 품질 보류 해제 | 앞: 200, HOLD [EXPIRED] / 뒤: 409 `NO_ACTIVE_HOLD`, 그대로 | 예외 | R9 | `stock/api/QualityHoldApiTest` (메서드 추가) |
| AC-15 | 2026-11-01 판매 중지 로트(마감 21일) | 2026-11-02 소비기한 2026-12-31로 정정 | `saleDeadline` 2026-12-10, AVAILABLE. 2026-11-03 배치 후에도 AVAILABLE | 정상 | R10 | `stock/api/ExpiryCorrectionApiTest` |
| AC-16 | 같은 로트 | 2026-11-02 소비기한 2026-11-15로 정정 | `saleDeadline` 2026-10-25, HOLD [EXPIRED] 유지 | 예외 | R10 | `ExpiryCorrectionApiTest` |
| AC-17 | AVAILABLE 로트(판매기한 2026-11-10) | 2026-11-02 소비기한 2026-11-20으로 정정 | `saleDeadline` 2026-10-30, HOLD [EXPIRED], 처리자 `SYSTEM` | 예외 | R10, R14 | `ExpiryCorrectionApiTest` |
| AC-18 | 판매 중지 + 품질 보류 로트 / 품질 보류만 걸린 로트 | 판매기한이 안 지나게 정정 | 둘 다 HOLD [QUALITY], 활성 `QUALITY` 1건 그대로 | 정상 | R10 | `ExpiryCorrectionApiTest` |
| AC-19 | 입고 후 마감 일수가 7일로 바뀐 상품의 판매 중지 로트 | 2026-11-02 소비기한 2026-11-10으로 정정 | `saleDeadline` 2026-11-03, AVAILABLE | 정상 | R10 | `ExpiryCorrectionApiTest` |
| AC-20 | 판매 중지 로트 | 사유 `""`, `"   "`, 누락, 501자로 정정 | 400 `INVALID_REQUEST`, 소비기한·판매기한·보류 그대로, 이력 0건 | 예외 | R11 | `ExpiryCorrectionApiTest` |
| AC-21 | 정정한 로트 (보류 변화 없는 정정 포함) | `expiry_correction` 조회 | 정정마다 전·후 소비기한·판매기한, 사유, 처리자, 일시 1건 | 정상 | R11, R16 | `ExpiryCorrectionApiTest` |
| AC-22 | MD 사용자 / 없는 로트 | 정정 | 403 `FORBIDDEN` / 404 `LOT_NOT_FOUND` | 예외 | R10 | `ExpiryCorrectionApiTest` |
| AC-23 | 한 상품에 AVAILABLE 40, `EXPIRED` HOLD 30, `QUALITY` HOLD 20, SOLD_OUT(수량 0) 로트 | MD 조회 | 데이터를 넣기 전 조회값 대비 `availableQty` +40, `heldQty` +50 | 정상 | R12 | `stock/api/AvailableStockApiTest` |
| AC-24 | WAREHOUSE 사용자 / 없는 상품 | 조회 | 403 `FORBIDDEN` / 404 `PRODUCT_NOT_FOUND` | 예외 | R12 | `AvailableStockApiTest` |
| AC-25 | `data.sql`의 `LT-261001-0002`(소비기한 2026-11-03) | 시계 2026-10-14 00:00, 배치 | `sale_deadline` 2026-10-13, HOLD [EXPIRED] | 정상 | R13 | `SaleStopServiceTest` |
| AC-26 | 판매기한 2026-10-31 로트, 10-31·11-01 배치 미실행 | 시계 2026-11-02 00:00, 배치 | HOLD [EXPIRED] | 예외 | R13 | `SaleStopServiceTest` |
| AC-27 | 판매기한 지난 SOLD_OUT 로트 | 배치 / 판매기한이 지나게 정정 | 활성 `EXPIRED` 없음, SOLD_OUT 유지 | 예외 | R10, R13 | `SaleStopServiceTest`, `ExpiryCorrectionApiTest` |
| AC-28 | `SaleStopJob.run` | 스케줄 선언 확인 | `cron = "0 0 0 * * *"`, `zone = "Asia/Seoul"` | 정상 | R15 | `stock/batch/SaleStopJobTest` (리플렉션으로 `@Scheduled` 값 확인) |
| AC-29 | 판매 중지 로트(소비기한 2026-11-21), 입고 후 마감 일수가 7일로 바뀐 상품 | 2026-11-02 소비기한 2026-11-21(같은 날짜)로 정정 | 409 `EXPIRY_DATE_UNCHANGED`, `sale_deadline`·HOLD [EXPIRED] 그대로, 이력 0건 | 예외 | R17 | `ExpiryCorrectionApiTest` |
| AC-30 | 소비기한 없이 입고한 AVAILABLE 로트(마감 21일) | 2026-11-02 소비기한 2026-11-20으로 정정 | `saleDeadline` 2026-10-30, HOLD [EXPIRED], 이력의 정정 전 소비기한·판매기한 null | 예외 | R18, R10 | `ExpiryCorrectionApiTest` |

## 8. 구현 순서

Task 하나가 끝날 때마다 `./gradlew test`가 통과해야 한다.

| Task | 내용 | AC |
|---|---|---|
| 1 | 판매 마감 일수: `product` 컬럼, `Product`·Mapper(Java·XML)·Service, `product/api` | AC-3, AC-4 |
| 2 | 판매 가능 재고 조회 API | AC-23, AC-24 |
| 3 | 판매기한 산출 + `EXPIRED` + 매핑 + 0시 배치 + 기존 로트 산출 | AC-1, AC-2, AC-5~14, AC-25~28 |
| 4 | 소비기한 정정 API + 정정 이력 | AC-15~22, AC-27(정정), AC-29, AC-30 |

- AC-2·AC-8은 판매기한이 생기는 Task 3에서 검증한다.
- AC-23의 `EXPIRED` 보류 데이터는 Task 2 시점에 enum이 없으므로 `TestData.activeHold(lotNo, "EXPIRED")`로 넣는다. 수량 합은 사유를 읽지 않는다.
- Task 3에는 새 API가 없다. `docs/api.md`에 `LotResponse.saleDeadline`만 반영한다.
- 9절 시나리오 테스트는 Task 4에서 만든다.
- `EXPIRED` enum과 `data.sql` 매핑은 Task 3에서 함께 추가한다.
- Task 3과 4는 함께 배포한다. 같은 브랜치에서 연달아 진행한다.
- 각 Task에서 `docs/api.md`를 함께 고친다.

## 9. E2E 검증

### 자동 검증

1. `./gradlew test` — 전체 통과
2. `./gradlew checkstyleMain` — 위반 0
3. 시나리오 테스트 `stock/SaleDeadlineScenarioTest` (신규, `@ShopIntegrationTest`)를 한 메서드에서 순서대로 실행한다.

| 순서 | 시계 | 동작 | 기대 결과 |
|---|---|---|---|
| 1 | 2026-10-20 10:00 | WAREHOUSE가 `P1004` 40개, 소비기한 2026-11-21 입고 | 201, `saleDeadline` 2026-10-31, AVAILABLE |
| 2 | 2026-10-31 00:00 | `SaleStopJob.run()` | 1의 로트 AVAILABLE |
| 3 | 2026-11-01 00:00 | `SaleStopJob.run()` | 1의 로트 HOLD [EXPIRED] |
| 4 | 2026-11-01 10:00 | 1의 로트 10개 출고 | 409 `LOT_NOT_SHIPPABLE` |
| 5 | 2026-11-01 10:00 | MD 판매 가능 재고 조회 (`P1004`) | `availableQty` 0, `heldQty` 85 (1의 로트 40 + 판매기한 10-20이 지난 샘플 `LT-261002-0007` 45) |
| 6 | 2026-11-01 10:00 | `PartnerStockSyncService.sync()` | 1의 로트 `STP` |
| 7 | 2026-11-02 10:00 | WAREHOUSE가 1의 로트 소비기한 2026-12-31로 정정 | 200, `saleDeadline` 2026-12-10, AVAILABLE |
| 8 | 2026-11-02 10:00 | `PartnerStockSyncService.sync()` | 1의 로트 `AVL` |
| 9 | 2026-11-03 00:00 | `SaleStopJob.run()` | 1의 로트 AVAILABLE, `expiry_correction` 1건 |

### 실행 확인 (선택)

`./gradlew bootRun` 후 실행한다. 실제 날짜와 상관없이 결과가 같도록 소비기한을 먼 미래로 쓴다.

```bash
# 1. 입고 → AVAILABLE, saleDeadline = 소비기한 - 21일
curl -s -X POST localhost:8080/api/lots -H 'Content-Type: application/json' \
  -H 'X-User-Id: wh01' -H 'X-User-Roles: WAREHOUSE' \
  -d '{"productCode":"P3002","qty":10,"expiryDate":"2099-12-31"}'
# 2. 응답의 lotNo로 정정 → saleDeadline 2099-11-29
curl -s -X POST localhost:8080/api/lots/<lotNo>/expiry-corrections -H 'Content-Type: application/json' \
  -H 'X-User-Id: wh01' -H 'X-User-Roles: WAREHOUSE' \
  -d '{"expiryDate":"2099-12-20","reason":"입고 때 소비기한 오입력"}'
# 3. 판매 가능 재고 → 2의 로트 10개 포함
curl -s localhost:8080/api/products/P3002/available-stock -H 'X-User-Id: md01' -H 'X-User-Roles: MD'
# 4. 마감 일수 범위 밖 → 400
curl -s -X PUT localhost:8080/api/products/P3002/sale-close-days -H 'Content-Type: application/json' \
  -H 'X-User-Id: md01' -H 'X-User-Roles: MD' -d '{"saleCloseDays":366}'
```

## 10. 완료 보고

- AC별 통과 여부와 검증한 테스트 메서드, 실행한 명령과 결과를 표로 보고한다.
- 운영 반영 사항을 6절 운영 반영 순서대로 보고한다: ① 운영 DDL(컬럼 2개, 테이블 1개)·`EXPIRED` 공통코드(배포 전) → ② 배포 → ③ 기존 로트 판매기한 산출 SQL(배포 직후, 첫 0시 배치 전).
- 이 스펙과 기존 코드·CLAUDE.md·`.claude/rules/`가 충돌하면 임의로 해결하지 말고 구현을 멈추고 보고한다.
