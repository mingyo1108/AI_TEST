package com.example.shop.partner.domain;

/**
 * 제휴몰 재고 연동 항목 (로트 한 건). 형식은 docs/interface-spec.md.
 *
 * @param productCode 상품 코드
 * @param lotNo 로트 번호
 * @param qty 수량
 * @param stockStatus 제휴몰 재고 상태 코드 (AVL, HLD, STP)
 */
public record PartnerStockMessage(String productCode, String lotNo, int qty, String stockStatus) {
}
