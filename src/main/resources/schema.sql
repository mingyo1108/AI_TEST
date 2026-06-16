-- 개발 DB 스키마 (MSSQL 문법, H2 MODE=MSSQLServer). 애플리케이션이 시작할 때마다 새로 만든다 (application.yml).
-- 운영 DB에는 자동 반영되지 않는다. 바꾸면 운영 반영이 필요한 DDL을 보고에 적는다.

-- 상품 (기본정보는 상품 관리 시스템에서 받아 오고, 재고 운영에 필요한 상품별 설정은 이 시스템이 관리한다)
CREATE TABLE product (
    product_code   VARCHAR(20)  NOT NULL PRIMARY KEY,
    product_name   VARCHAR(100) NOT NULL,
    category       VARCHAR(20)  NOT NULL,   -- FRESH 신선, FROZEN 냉동, PROCESSED 가공, LIVING 생활용품
    price          INT          NOT NULL,
    use_yn         CHAR(1)      DEFAULT 'Y' NOT NULL
);

-- 입고 로트
CREATE TABLE stock_lot (
    lot_no         VARCHAR(20)  NOT NULL PRIMARY KEY,
    product_code   VARCHAR(20)  NOT NULL,
    qty            INT          NOT NULL,
    status         VARCHAR(10)  NOT NULL,   -- AVAILABLE, HOLD, SOLD_OUT
    expiry_date    DATE,                    -- 소비기한 (포장 표시, 생활용품 등은 없음)
    received_at    TIMESTAMP    NOT NULL,
    received_by    VARCHAR(20)  NOT NULL
);
-- 로트 번호 일련번호: 날짜와 상관없이 이어지고, 샘플 데이터(0001~)와 겹치지 않게 1001부터 시작한다
CREATE SEQUENCE lot_seq START WITH 1001;

-- 판매 보류 (사유별 건 단위. 해제하면 released_at을 채운다)
CREATE TABLE stock_hold (
    hold_id         BIGINT       IDENTITY PRIMARY KEY,
    lot_no          VARCHAR(20)  NOT NULL,
    reason_code     VARCHAR(20)  NOT NULL,
    hold_comment    VARCHAR(500),
    held_at         TIMESTAMP    NOT NULL,
    held_by         VARCHAR(20)  NOT NULL,
    released_at     TIMESTAMP,
    released_by     VARCHAR(20),
    release_comment VARCHAR(500)
);

-- 공통코드
CREATE TABLE common_code (
    group_code     VARCHAR(40)  NOT NULL,
    code           VARCHAR(20)  NOT NULL,
    code_name      VARCHAR(100) NOT NULL,
    attr1          VARCHAR(100),
    attr2          VARCHAR(100),
    sort_order     INT          DEFAULT 0 NOT NULL,
    use_yn         CHAR(1)      DEFAULT 'Y' NOT NULL,
    PRIMARY KEY (group_code, code)
);

-- 배치 실행 이력
CREATE TABLE batch_history (
    run_id         VARCHAR(20)  NOT NULL PRIMARY KEY,   -- B20261012-0001
    job_name       VARCHAR(40)  NOT NULL,
    server_name    VARCHAR(20)  NOT NULL,
    started_at     TIMESTAMP    NOT NULL,
    ended_at       TIMESTAMP,
    status         VARCHAR(10)  NOT NULL,                -- RUNNING, SUCCESS, FAIL
    target_count   INT,
    message        VARCHAR(500)
);
CREATE SEQUENCE batch_run_seq START WITH 1;

-- 제휴몰 재고 연동 송신 I/F (docs/interface-spec.md)
CREATE TABLE partner_stock_if (
    if_id          BIGINT       IDENTITY PRIMARY KEY,
    product_code   VARCHAR(20)  NOT NULL,
    lot_no         VARCHAR(20)  NOT NULL,
    qty            INT          NOT NULL,
    stock_status   VARCHAR(3)   NOT NULL,   -- AVL, HLD, STP
    sent_at        TIMESTAMP    NOT NULL
);
