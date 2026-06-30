package com.example.shop.stock.application;

import com.example.shop.common.BusinessException;
import com.example.shop.common.ErrorCode;
import com.example.shop.product.application.ProductService;
import com.example.shop.stock.domain.LotDetail;
import com.example.shop.stock.domain.LotStatus;
import com.example.shop.stock.domain.StockHold;
import com.example.shop.stock.domain.StockLot;
import com.example.shop.stock.mapper.StockLotMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 입고 로트 서비스: 입고, 출고, 재고 연동 대상 조회.
 */
@Service
public class LotService {

    private final StockLotMapper lotMapper;
    private final HoldService holdService;
    private final ProductService productService;
    private final Clock clock;

    public LotService(StockLotMapper lotMapper, HoldService holdService, ProductService productService,
            Clock clock) {
        this.lotMapper = lotMapper;
        this.holdService = holdService;
        this.productService = productService;
        this.clock = clock;
    }

    /** 입고한다. 로트를 새로 만들고 판매 가능 상태로 둔다. 소비기한은 없을 수 있다(생활용품 등). */
    @Transactional
    public LotDetail receive(String productCode, int qty, LocalDate expiryDate, String userId) {
        productService.getProduct(productCode);
        LocalDateTime now = LocalDateTime.now(clock);
        StockLot lot = new StockLot(lotMapper.nextLotNo(now.toLocalDate()), productCode, qty,
                LotStatus.AVAILABLE, expiryDate, now, userId);
        lotMapper.insertLot(lot);
        return new LotDetail(lot, List.of());
    }

    /** 출고한다. 판매 보류 중이거나 소진된 로트는 출고할 수 없다. 남은 수량이 0이면 소진. */
    @Transactional
    public LotDetail ship(String lotNo, int qty) {
        StockLot lot = getLot(lotNo);
        if (lot.status() != LotStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.LOT_NOT_SHIPPABLE, lotNo + ", " + lot.status());
        }
        if (qty > lot.qty()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_QTY, lotNo + ", 남은 수량 " + lot.qty());
        }
        int remaining = lot.qty() - qty;
        LotStatus status = LotStatus.resolve(remaining, false);
        lotMapper.updateLotQtyAndStatus(lotNo, remaining, status);
        return new LotDetail(lot.withQtyAndStatus(remaining, status), List.of());
    }

    /** 재고가 남은 로트(판매 가능·판매 보류)와 각 로트의 활성 보류를 조회한다. 제휴몰 재고 연동이 쓴다. */
    @Transactional(readOnly = true)
    public List<LotDetail> findInStock() {
        Map<String, List<StockHold>> holdsByLot = holdService.findAllActive().stream()
                .collect(Collectors.groupingBy(StockHold::lotNo));
        return lotMapper.selectInStockList().stream()
                .map(lot -> new LotDetail(lot, holdsByLot.getOrDefault(lot.lotNo(), List.of())))
                .toList();
    }

    private StockLot getLot(String lotNo) {
        return lotMapper.selectLotOne(lotNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOT_NOT_FOUND, lotNo));
    }
}
