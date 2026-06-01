package com.example.shop.common;

import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * API 오류 응답을 한곳에서 만든다. 모든 오류 응답은 {@link ApiResponse#fail(String, String)} 형식이다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 업무 오류. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.fail(code.name(), e.getMessage()));
    }

    /** 요청 본문 검증 실패 (Bean Validation). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.INVALID_REQUEST.name(), detail));
    }

    /** 읽을 수 없는 요청 본문 (JSON 형식 오류, 날짜 형식 오류 등). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.INVALID_REQUEST.name(), "요청 본문을 읽을 수 없습니다"));
    }

    /**
     * 그 밖의 예외. 스프링이 상태를 정한 예외(없는 경로 404, 지원하지 않는 메서드 405 등)는 그 상태로,
     * 나머지는 500으로 응답하고 내부 메시지는 노출하지 않는다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        if (e instanceof ErrorResponse springError) {
            return ResponseEntity.status(springError.getStatusCode())
                    .body(ApiResponse.fail(ErrorCode.INVALID_REQUEST.name(), "요청을 처리할 수 없습니다"));
        }
        log.error("처리하지 못한 예외", e);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR.name(), ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
