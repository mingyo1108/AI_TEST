package com.example.shop.stock.domain;

/**
 * 판매 보류 사유. 보류는 사유별 건 단위로 기록하고, 해제도 사유별로 한다.
 */
public enum HoldReason {
    /** 품질 이슈 (이물 신고, 고객 클레임 등). 품질 담당이 등록·해제한다. */
    QUALITY
}
