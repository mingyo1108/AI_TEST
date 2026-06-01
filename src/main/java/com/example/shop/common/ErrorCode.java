package com.example.shop.common;

import org.springframework.http.HttpStatus;

/**
 * 오류 코드. 업무 오류는 여기에 등록하고 {@link BusinessException}으로 던진다.
 *
 * <p>상태 코드 기준: 대상 없음 404, 상태 충돌 409, 입력 오류 400, 인증 없음 401, 권한 없음 403.
 * 배치에서만 나는 오류는 500으로 둔다.
 */
public enum ErrorCode {

    // 공통
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "사용자 정보가 없습니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "처리 중 오류가 발생했습니다"),

    // 상품
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품이 없습니다"),

    // 재고
    LOT_NOT_FOUND(HttpStatus.NOT_FOUND, "로트가 없습니다"),
    LOT_NOT_SHIPPABLE(HttpStatus.CONFLICT, "판매 보류 중이거나 소진된 로트는 출고할 수 없습니다"),
    INSUFFICIENT_QTY(HttpStatus.CONFLICT, "로트 수량이 부족합니다"),
    HOLD_ALREADY_EXISTS(HttpStatus.CONFLICT, "같은 사유의 판매 보류가 이미 있습니다"),
    NO_ACTIVE_HOLD(HttpStatus.CONFLICT, "해제할 판매 보류가 없습니다"),

    // 배치
    PARTNER_STOCK_MAPPING_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "제휴몰 재고 상태 매핑이 없습니다");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    /** HTTP 상태. */
    public HttpStatus getStatus() {
        return status;
    }

    /** 기본 메시지. */
    public String getMessage() {
        return message;
    }
}
