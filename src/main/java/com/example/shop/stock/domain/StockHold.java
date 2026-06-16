package com.example.shop.stock.domain;

import java.time.LocalDateTime;

/**
 * 판매 보류 한 건 (활성 보류).
 *
 * @param holdId 보류 ID
 * @param lotNo 로트 번호
 * @param reason 사유
 * @param comment 보류 코멘트
 * @param heldAt 보류 일시
 * @param heldBy 보류 처리자
 */
public record StockHold(long holdId, String lotNo, HoldReason reason, String comment,
        LocalDateTime heldAt, String heldBy) {
}
