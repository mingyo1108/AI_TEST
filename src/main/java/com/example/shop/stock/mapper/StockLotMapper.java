package com.example.shop.stock.mapper;

import com.example.shop.stock.domain.LotStatus;
import com.example.shop.stock.domain.StockLot;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 입고 로트 매퍼. SQL은 {@code resources/mapper/stock/StockLotMapper.xml}.
 */
@Mapper
public interface StockLotMapper {

    /** 로트 번호 날짜 부분 형식. */
    DateTimeFormatter LOT_DATE = DateTimeFormatter.ofPattern("yyMMdd");

    /** 새 로트 번호를 만든다. 예: LT-261012-0001 */
    default String nextLotNo(LocalDate receivedDate) {
        return "LT-" + receivedDate.format(LOT_DATE) + "-" + String.format("%04d", selectNextLotSeq());
    }

    /** 로트 번호 시퀀스의 다음 값. */
    long selectNextLotSeq();

    /** 로트를 저장한다. */
    void insertLot(StockLot lot);

    /** 로트를 조회한다. */
    Optional<StockLot> selectLotOne(@Param("lotNo") String lotNo);

    /** 판매 가능·판매 보류 로트를 조회한다 (소진 제외). */
    List<StockLot> selectInStockList();

    /** 수량과 상태를 바꾼다. */
    void updateLotQtyAndStatus(@Param("lotNo") String lotNo, @Param("qty") int qty,
            @Param("status") LotStatus status);

    /** 상태를 바꾼다. */
    void updateLotStatus(@Param("lotNo") String lotNo, @Param("status") LotStatus status);
}
