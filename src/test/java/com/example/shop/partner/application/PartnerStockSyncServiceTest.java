package com.example.shop.partner.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.shop.stock.domain.LotStatus;
import com.example.shop.support.ShopIntegrationTest;
import com.example.shop.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@ShopIntegrationTest
class PartnerStockSyncServiceTest {

    @Autowired
    PartnerStockSyncService syncService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    TestData data;

    @BeforeEach
    void setUp() {
        data = new TestData(jdbcTemplate);
    }

    @Test
    void 판매_가능_로트는_AVL로_보낸다() {
        String lotNo = data.availableLot();

        syncService.sync();

        assertThat(data.partnerStockStatus(lotNo)).isEqualTo("AVL");
    }

    @Test
    void 품질_보류_로트는_HLD로_보낸다() {
        String lotNo = data.qualityHoldLot();

        syncService.sync();

        assertThat(data.partnerStockStatus(lotNo)).isEqualTo("HLD");
    }

    @Test
    void 소진_로트는_보내지_않는다() {
        String lotNo = data.lot(TestData.TOFU, 0, LotStatus.SOLD_OUT);

        int sent = syncService.sync();

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM partner_stock_if WHERE lot_no = ?",
                Integer.class, lotNo);
        assertThat(count).isZero();
        assertThat(sent).isPositive();
    }
}
