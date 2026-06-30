package com.example.shop.stock.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 판매 보류 등록·해제 요청.
 *
 * @param comment 코멘트 (필수, 500자 이내)
 */
public record HoldRequest(@NotBlank @Size(max = 500) String comment) {
}
