package com.example.shop.common;

/**
 * 업무 오류. 업무 규칙 위반은 이 예외 하나로 던진다.
 *
 * <p>메시지는 "기본 메시지 (상세)" 형식이다. 예: {@code 로트가 없습니다 (LT-261012-0001)}
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + " (" + detail + ")");
        this.errorCode = errorCode;
    }

    /** 오류 코드. */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
