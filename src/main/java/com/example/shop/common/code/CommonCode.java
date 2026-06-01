package com.example.shop.common.code;

/**
 * 공통코드 한 건. 그룹마다 attr1·attr2의 뜻이 다르다 (data.sql 주석 참고).
 *
 * @param groupCode 그룹 코드
 * @param code 코드
 * @param codeName 코드명
 * @param attr1 속성 1
 * @param attr2 속성 2
 */
public record CommonCode(String groupCode, String code, String codeName, String attr1, String attr2) {
}
