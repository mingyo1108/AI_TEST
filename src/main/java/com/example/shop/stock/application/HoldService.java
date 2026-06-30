package com.example.shop.stock.application;

import com.example.shop.common.BusinessException;
import com.example.shop.common.ErrorCode;
import com.example.shop.stock.domain.HoldReason;
import com.example.shop.stock.domain.LotDetail;
import com.example.shop.stock.domain.LotStatus;
import com.example.shop.stock.domain.StockHold;
import com.example.shop.stock.domain.StockLot;
import com.example.shop.stock.mapper.StockHoldMapper;
import com.example.shop.stock.mapper.StockLotMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 판매 보류 서비스. 보류는 사유별 건 단위이고, 활성 보류가 하나라도 남아 있으면 로트는 판매 보류(HOLD)다.
 *
 * <p>공용 서비스: 보류와 그에 따른 로트 상태는 이 서비스로만 바꾸고 조회한다. 다른 서비스를 부르지 않는다.
 */
@Service
public class HoldService {

    private final StockLotMapper lotMapper;
    private final StockHoldMapper holdMapper;
    private final Clock clock;

    public HoldService(StockLotMapper lotMapper, StockHoldMapper holdMapper, Clock clock) {
        this.lotMapper = lotMapper;
        this.holdMapper = holdMapper;
        this.clock = clock;
    }

    /** 판매 보류를 건다. 같은 사유의 활성 보류가 이미 있으면 409. */
    @Transactional
    public LotDetail placeHold(String lotNo, HoldReason reason, String comment, String userId) {
        StockLot lot = getLot(lotNo);
        if (holdMapper.selectActiveHoldCount(lotNo, reason) > 0) {
            throw new BusinessException(ErrorCode.HOLD_ALREADY_EXISTS, lotNo + ", " + reason);
        }
        holdMapper.insertHold(lotNo, reason, comment, LocalDateTime.now(clock), userId);
        return refreshStatus(lot);
    }

    /** 한 사유의 판매 보류를 해제한다. 다른 사유의 보류가 남아 있으면 판매 보류 상태를 유지한다. */
    @Transactional
    public LotDetail releaseHolds(String lotNo, HoldReason reason, String comment, String userId) {
        StockLot lot = getLot(lotNo);
        int released = holdMapper.updateHoldRelease(lotNo, reason, comment, LocalDateTime.now(clock), userId);
        if (released == 0) {
            throw new BusinessException(ErrorCode.NO_ACTIVE_HOLD, lotNo + ", " + reason);
        }
        return refreshStatus(lot);
    }

    /** 모든 로트의 활성 보류를 조회한다. 재고 연동 대상 조회가 쓴다. */
    @Transactional(readOnly = true)
    public List<StockHold> findAllActive() {
        return holdMapper.selectAllActiveHoldList();
    }

    private LotDetail refreshStatus(StockLot lot) {
        List<StockHold> activeHolds = holdMapper.selectActiveHoldList(lot.lotNo());
        LotStatus status = LotStatus.resolve(lot.qty(), !activeHolds.isEmpty());
        lotMapper.updateLotStatus(lot.lotNo(), status);
        return new LotDetail(lot.withStatus(status), activeHolds);
    }

    private StockLot getLot(String lotNo) {
        return lotMapper.selectLotOne(lotNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOT_NOT_FOUND, lotNo));
    }
}
