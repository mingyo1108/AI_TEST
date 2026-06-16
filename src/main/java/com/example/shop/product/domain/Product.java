package com.example.shop.product.domain;

/**
 * 상품. 기본정보(상품명·분류·판매가)는 상품 관리 시스템에서 받아 오고, 재고 운영에 필요한 상품별 설정은 이 시스템이 관리한다.
 *
 * @param productCode 상품 코드
 * @param productName 상품명
 * @param category 분류 (FRESH 신선, FROZEN 냉동, PROCESSED 가공, LIVING 생활용품)
 * @param price 판매가
 */
public record Product(String productCode, String productName, String category, int price) {
}
