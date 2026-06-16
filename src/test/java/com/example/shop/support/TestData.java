package com.example.shop.support;

import com.example.shop.stock.domain.LotStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 테스트 데이터 만들기. 번호가 겹치지 않도록 테스트 전용 접두어(T)를 쓴다.
 */
public class TestData {

    /** data.sql에 있는 상품: 국산콩 두부 (신선). */
    public static final String TOFU = "P1001";
    /** data.sql에 있는 상품: 왕교자 만두 (냉동). */
    public static final String DUMPLING = "P2001";

    private static final AtomicInteger SEQ = new AtomicInteger(1);

    private final JdbcTemplate jdbc;

    public TestData(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 로트를 만든다 (소비기한 없음). */
    public String lot(String productCode, int qty, LotStatus status) {
        return lot(productCode, qty, status, null);
    }

    /** 소비기한이 있는 로트를 만든다. */
    public String lot(String productCode, int qty, LotStatus status, LocalDate expiryDate) {
        String lotNo = "TL-" + SEQ.getAndIncrement();
        jdbc.update("""
                INSERT INTO stock_lot (lot_no, product_code, qty, status, expiry_date, received_at, received_by)
                VALUES (?, ?, ?, ?, ?, ?, 'test')
                """, lotNo, productCode, qty, status.name(), expiryDate, LocalDateTime.of(2026, 10, 1, 9, 0));
        return lotNo;
    }

    /** 판매 가능 로트를 만든다. */
    public String availableLot() {
        return lot(TOFU, 100, LotStatus.AVAILABLE);
    }

    /** 품질 보류 중인 로트를 만든다. */
    public String qualityHoldLot() {
        String lotNo = lot(TOFU, 100, LotStatus.HOLD);
        activeHold(lotNo, "QUALITY");
        return lotNo;
    }

    /** 활성 보류 기록을 직접 넣는다 (로트 상태는 바꾸지 않는다). */
    public void activeHold(String lotNo, String reasonCode) {
        jdbc.update("""
                INSERT INTO stock_hold (lot_no, reason_code, hold_comment, held_at, held_by)
                VALUES (?, ?, 'test hold', ?, 'test')
                """, lotNo, reasonCode, LocalDateTime.of(2026, 10, 1, 10, 0));
    }

    /** 로트 상태. */
    public String lotStatus(String lotNo) {
        return jdbc.queryForObject("SELECT status FROM stock_lot WHERE lot_no = ?", String.class, lotNo);
    }

    /** 로트 수량. */
    public int lotQty(String lotNo) {
        Integer qty = jdbc.queryForObject("SELECT qty FROM stock_lot WHERE lot_no = ?", Integer.class, lotNo);
        return qty == null ? 0 : qty;
    }

    /** 한 사유의 활성 보류 개수. */
    public int activeHoldCount(String lotNo, String reasonCode) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock_hold WHERE lot_no = ? AND reason_code = ? AND released_at IS NULL",
                Integer.class, lotNo, reasonCode);
        return count == null ? 0 : count;
    }

    /** 제휴몰 재고 연동 I/F에 쓰인 로트의 상태 코드 (마지막 송신분). */
    public String partnerStockStatus(String lotNo) {
        return jdbc.queryForObject("""
                SELECT TOP 1 stock_status FROM partner_stock_if WHERE lot_no = ? ORDER BY if_id DESC
                """, String.class, lotNo);
    }
}
