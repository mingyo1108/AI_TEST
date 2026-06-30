package com.example.shop.stock.api;

import com.example.shop.common.ApiResponse;
import com.example.shop.common.auth.CurrentUser;
import com.example.shop.common.auth.Role;
import com.example.shop.stock.application.HoldService;
import com.example.shop.stock.domain.HoldReason;
import com.example.shop.stock.domain.LotDetail;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 품질 보류 등록·해제 API (품질 담당). 사유가 QUALITY인 판매 보류만 다룬다.
 */
@RestController
@RequestMapping("/api/lots/{lotNo}/quality-holds")
public class QualityHoldController {

    private final HoldService holdService;

    public QualityHoldController(HoldService holdService) {
        this.holdService = holdService;
    }

    /** 품질 보류를 등록한다. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LotResponse> hold(@PathVariable String lotNo, @Valid @RequestBody HoldRequest request,
            CurrentUser user) {
        user.requireRole(Role.QC);
        LotDetail detail = holdService.placeHold(lotNo, HoldReason.QUALITY, request.comment(), user.userId());
        return ApiResponse.ok(LotResponse.from(detail));
    }

    /** 품질 보류를 해제한다. 다른 사유의 보류가 남아 있으면 판매 보류 상태가 유지된다. */
    @PostMapping("/release")
    public ApiResponse<LotResponse> release(@PathVariable String lotNo, @Valid @RequestBody HoldRequest request,
            CurrentUser user) {
        user.requireRole(Role.QC);
        LotDetail detail = holdService.releaseHolds(lotNo, HoldReason.QUALITY, request.comment(), user.userId());
        return ApiResponse.ok(LotResponse.from(detail));
    }
}
