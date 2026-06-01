package com.example.shop.common;

/**
 * 모든 API의 응답 형식. 성공이면 {@code data}, 실패면 {@code error}가 채워진다.
 *
 * <pre>
 * 성공: {"success": true,  "data": {...}, "error": null}
 * 실패: {"success": false, "data": null,  "error": {"code": "LOT_NOT_FOUND", "message": "로트가 없습니다 (LT-...)"}}
 * </pre>
 *
 * @param success 성공 여부
 * @param data 응답 데이터 (실패면 null)
 * @param error 오류 정보 (성공이면 null)
 * @param <T> 응답 데이터 타입
 */
public record ApiResponse<T>(boolean success, T data, ErrorBody error) {

    /** 성공 응답을 만든다. */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /** 실패 응답을 만든다. 오류 응답은 {@link GlobalExceptionHandler}만 만든다. */
    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(code, message));
    }

    /**
     * 오류 정보.
     *
     * @param code 오류 코드
     * @param message 메시지
     */
    public record ErrorBody(String code, String message) {
    }
}
