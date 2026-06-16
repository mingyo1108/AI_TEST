package com.example.shop.stock.api;

import com.example.shop.common.ApiResponse;
import com.example.shop.common.auth.CurrentUser;
import com.example.shop.common.auth.Role;
import com.example.shop.stock.application.LotService;
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
 * 입고·출고 API (물류 담당).
 */
@RestController
@RequestMapping("/api/lots")
public class LotController {

    private final LotService lotService;

    public LotController(LotService lotService) {
        this.lotService = lotService;
    }

    /** 입고한다. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LotResponse> receive(@Valid @RequestBody ReceiveRequest request, CurrentUser user) {
        user.requireRole(Role.WAREHOUSE);
        LotDetail detail = lotService.receive(request.productCode(), request.qty(), request.expiryDate(),
                user.userId());
        return ApiResponse.ok(LotResponse.from(detail));
    }

    /** 출고한다. */
    @PostMapping("/{lotNo}/shipments")
    public ApiResponse<LotResponse> ship(@PathVariable String lotNo, @Valid @RequestBody ShipRequest request,
            CurrentUser user) {
        user.requireRole(Role.WAREHOUSE);
        LotDetail detail = lotService.ship(lotNo, request.qty());
        return ApiResponse.ok(LotResponse.from(detail));
    }
}
