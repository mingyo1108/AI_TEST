package com.example.shop.stock.domain;

/**
 * 입고 로트 상태.
 */
public enum LotStatus {
    /** 판매 가능. */
    AVAILABLE,
    /** 판매 보류: 활성 보류가 하나라도 있음. 판매 가능 수량에서 빠지고 출고할 수 없다. */
    HOLD,
    /** 소진: 수량 0. */
    SOLD_OUT;

    /** 수량과 활성 보류 여부로 상태를 정한다. 소진이 보류보다 우선한다. */
    public static LotStatus resolve(int qty, boolean hasActiveHold) {
        if (qty == 0) {
            return SOLD_OUT;
        }
        return hasActiveHold ? HOLD : AVAILABLE;
    }
}
