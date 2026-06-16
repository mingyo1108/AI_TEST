package com.example.shop.stock.mapper;

import com.example.shop.stock.domain.HoldReason;
import com.example.shop.stock.domain.StockHold;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 판매 보류 매퍼. 보류는 사유별 건 단위로 기록하고, 해제하면 해제 일시를 채운다(활성 보류 = 해제 일시 없음).
 * SQL은 {@code resources/mapper/stock/StockHoldMapper.xml}.
 */
@Mapper
public interface StockHoldMapper {

    /** 보류를 기록한다. */
    void insertHold(@Param("lotNo") String lotNo, @Param("reason") HoldReason reason,
            @Param("comment") String comment, @Param("heldAt") LocalDateTime heldAt, @Param("heldBy") String heldBy);

    /** 로트의 활성 보류를 조회한다. */
    List<StockHold> selectActiveHoldList(@Param("lotNo") String lotNo);

    /** 모든 활성 보류를 조회한다. */
    List<StockHold> selectAllActiveHoldList();

    /** 같은 사유의 활성 보류 건수. */
    int selectActiveHoldCount(@Param("lotNo") String lotNo, @Param("reason") HoldReason reason);

    /** 한 사유의 활성 보류를 해제한다. 다른 사유의 보류는 그대로 둔다. 해제한 건수를 돌려준다. */
    int updateHoldRelease(@Param("lotNo") String lotNo, @Param("reason") HoldReason reason,
            @Param("comment") String comment, @Param("releasedAt") LocalDateTime releasedAt,
            @Param("releasedBy") String releasedBy);
}
