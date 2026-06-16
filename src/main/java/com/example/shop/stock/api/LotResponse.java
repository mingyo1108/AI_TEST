package com.example.shop.stock.api;

import com.example.shop.stock.domain.LotDetail;
import com.example.shop.stock.domain.StockLot;
import java.time.LocalDate;
import java.util.List;

/**
 * 로트 응답.
 *
 * @param lotNo 로트 번호
 * @param productCode 상품 코드
 * @param qty 남은 수량
 * @param status 상태 (AVAILABLE, HOLD, SOLD_OUT)
 * @param expiryDate 소비기한 (없으면 null)
 * @param activeHoldReasons 활성 보류 사유
 */
public record LotResponse(String lotNo, String productCode, int qty, String status, LocalDate expiryDate,
        List<String> activeHoldReasons) {

    /** 도메인 객체로 만든다. */
    public static LotResponse from(LotDetail detail) {
        StockLot lot = detail.lot();
        return new LotResponse(lot.lotNo(), lot.productCode(), lot.qty(), lot.status().name(), lot.expiryDate(),
                detail.activeReasons().stream().map(Enum::name).toList());
    }
}
