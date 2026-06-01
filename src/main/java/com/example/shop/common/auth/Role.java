package com.example.shop.common.auth;

/**
 * 사용자 역할.
 */
public enum Role {
    /** 상품 담당(MD): 상품별 판매 정책. */
    MD,
    /** 물류 담당: 입고, 출고. */
    WAREHOUSE,
    /** 품질 담당: 품질 보류 등록·해제. */
    QC
}
