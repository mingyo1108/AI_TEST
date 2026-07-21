package com.example.shop.partner.mapper;

import com.example.shop.partner.domain.PartnerStockMessage;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 제휴몰 재고 연동 송신 I/F 테이블(partner_stock_if) 매퍼. SQL은 {@code resources/mapper/partner/PartnerStockIfMapper.xml}.
 */
@Mapper
public interface PartnerStockIfMapper {

    /** 재고 연동 항목을 한 번에 쓴다. 빈 목록이면 부르지 않는다. */
    void insertPartnerStockIfList(@Param("messages") List<PartnerStockMessage> messages,
            @Param("sentAt") LocalDateTime sentAt);
}
