package com.example.shop.stock.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 입고 요청.
 *
 * @param productCode 상품 코드
 * @param qty 입고 수량 (1~10000)
 * @param expiryDate 소비기한 (포장 표시, 선택. 생활용품 등은 없음)
 */
public record ReceiveRequest(
        @NotBlank String productCode,
        @NotNull @Min(1) @Max(10000) Integer qty,
        LocalDate expiryDate) {
}
