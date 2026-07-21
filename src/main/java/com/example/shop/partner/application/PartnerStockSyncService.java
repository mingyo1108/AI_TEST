package com.example.shop.partner.application;

import com.example.shop.common.BusinessException;
import com.example.shop.common.ErrorCode;
import com.example.shop.common.code.CommonCode;
import com.example.shop.common.code.CommonCodeMapper;
import com.example.shop.partner.domain.PartnerStockMessage;
import com.example.shop.partner.infra.PartnerMallGateway;
import com.example.shop.stock.application.LotService;
import com.example.shop.stock.domain.HoldReason;
import com.example.shop.stock.domain.LotDetail;
import com.example.shop.stock.domain.StockLot;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 제휴몰 재고 연동. 재고가 남은 로트를 제휴몰 재고 상태 코드와 함께 보낸다.
 *
 * <p>판매 보류 로트는 보류 사유를 공통코드로 제휴몰 상태 코드로 바꾼다. 사유가 여러 개면 우선순위가 높은
 * (숫자가 작은) 상태를 보낸다. 바꿀 수 없는 로트가 하나라도 있으면 이번 실행 전체를 보내지 않는다(전체 롤백).
 */
@Service
public class PartnerStockSyncService {

    /** 판매 가능 상태 코드. */
    static final String STATUS_AVAILABLE = "AVL";
    /** 보류 사유 → 제휴몰 상태 코드 매핑 그룹. */
    static final String STATUS_MAP_GROUP = "PARTNER_STOCK_STATUS_MAP";

    private final LotService lotService;
    private final CommonCodeMapper commonCodeMapper;
    private final PartnerMallGateway gateway;
    private final Clock clock;

    public PartnerStockSyncService(LotService lotService, CommonCodeMapper commonCodeMapper,
            PartnerMallGateway gateway, Clock clock) {
        this.lotService = lotService;
        this.commonCodeMapper = commonCodeMapper;
        this.gateway = gateway;
        this.clock = clock;
    }

    /** 재고를 연동한다. */
    @Transactional
    public int sync() {
        List<PartnerStockMessage> messages = lotService.findInStock().stream().map(this::toMessage).toList();
        gateway.sendStock(messages, LocalDateTime.now(clock));
        return messages.size();
    }

    private PartnerStockMessage toMessage(LotDetail detail) {
        StockLot lot = detail.lot();
        return new PartnerStockMessage(lot.productCode(), lot.lotNo(), lot.qty(), toStockStatus(detail));
    }

    private String toStockStatus(LotDetail detail) {
        if (detail.activeHolds().isEmpty()) {
            return STATUS_AVAILABLE;
        }
        CommonCode selected = null;
        for (HoldReason reason : detail.activeReasons()) {
            CommonCode mapping = commonCodeMapper.selectCodeOne(STATUS_MAP_GROUP, reason.name())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_STOCK_MAPPING_MISSING,
                            "lot=" + detail.lot().lotNo() + ", reason=" + reason));
            if (selected == null || priority(mapping) < priority(selected)) {
                selected = mapping;
            }
        }
        return selected.attr1();
    }

    private int priority(CommonCode mapping) {
        return mapping.attr2() == null ? Integer.MAX_VALUE : Integer.parseInt(mapping.attr2().trim());
    }
}
