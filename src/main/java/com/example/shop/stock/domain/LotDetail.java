package com.example.shop.stock.domain;

import java.util.List;

/**
 * 로트와 활성 보류 목록.
 *
 * @param lot 로트
 * @param activeHolds 활성 보류 (없으면 빈 목록)
 */
public record LotDetail(StockLot lot, List<StockHold> activeHolds) {

    /** 활성 보류의 사유 목록. */
    public List<HoldReason> activeReasons() {
        return activeHolds.stream().map(StockHold::reason).toList();
    }
}
