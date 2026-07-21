package com.example.shop.partner.infra;

import com.example.shop.partner.domain.PartnerStockMessage;
import com.example.shop.partner.mapper.PartnerStockIfMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 제휴몰 재고 연동 송신 I/F 테이블. 연동 서버가 I/F 테이블을 읽어 제휴몰로 보낸다.
 * 형식은 docs/interface-spec.md.
 */
@Component
public class PartnerMallGateway {

    private final PartnerStockIfMapper stockIfMapper;

    public PartnerMallGateway(PartnerStockIfMapper stockIfMapper) {
        this.stockIfMapper = stockIfMapper;
    }

    /** 재고 연동 항목을 송신 I/F 테이블에 쓴다. */
    public void sendStock(List<PartnerStockMessage> messages, LocalDateTime sentAt) {
        if (messages.isEmpty()) {
            return;
        }
        stockIfMapper.insertPartnerStockIfList(messages, sentAt);
    }
}
