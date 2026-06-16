package com.example.shop.stock.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 출고 요청.
 *
 * @param qty 출고 수량 (1 이상)
 */
public record ShipRequest(@NotNull @Min(1) Integer qty) {
}
