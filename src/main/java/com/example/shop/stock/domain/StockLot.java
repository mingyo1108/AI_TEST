package com.example.shop.stock.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 입고 로트. 같은 상품이라도 입고할 때마다 로트를 새로 만든다.
 *
 * @param lotNo 로트 번호 (LT-yyMMdd-0001)
 * @param productCode 상품 코드
 * @param qty 남은 수량
 * @param status 상태
 * @param expiryDate 소비기한 (포장 표시. 생활용품 등은 없음)
 * @param receivedAt 입고 일시
 * @param receivedBy 입고 처리자
 */
public record StockLot(String lotNo, String productCode, int qty, LotStatus status, LocalDate expiryDate,
        LocalDateTime receivedAt, String receivedBy) {

    /** 상태만 바꾼 로트를 만든다. */
    public StockLot withStatus(LotStatus newStatus) {
        return withQtyAndStatus(qty, newStatus);
    }

    /** 수량과 상태를 바꾼 로트를 만든다. */
    public StockLot withQtyAndStatus(int newQty, LotStatus newStatus) {
        return new StockLot(lotNo, productCode, newQty, newStatus, expiryDate, receivedAt, receivedBy);
    }
}
