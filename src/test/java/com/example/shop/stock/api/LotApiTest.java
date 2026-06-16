package com.example.shop.stock.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.shop.stock.domain.LotStatus;
import com.example.shop.support.ShopIntegrationTest;
import com.example.shop.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@ShopIntegrationTest
class LotApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    TestData data;

    @BeforeEach
    void setUp() {
        data = new TestData(jdbcTemplate);
    }

    @Test
    void 물류_담당은_입고할_수_있다() throws Exception {
        mockMvc.perform(post("/api/lots")
                        .header("X-User-Id", "wh01").header("X-User-Roles", "WAREHOUSE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"P1001\",\"qty\":40}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.lotNo").value(org.hamcrest.Matchers.startsWith("LT-261012-")))
                .andExpect(jsonPath("$.data.qty").value(40))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"));
    }

    @Test
    void 소비기한을_함께_입고할_수_있다() throws Exception {
        mockMvc.perform(post("/api/lots")
                        .header("X-User-Id", "wh01").header("X-User-Roles", "WAREHOUSE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"P1001\",\"qty\":40,\"expiryDate\":\"2026-11-21\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.expiryDate").value("2026-11-21"));
    }

    @Test
    void 없는_상품이면_404() throws Exception {
        mockMvc.perform(post("/api/lots")
                        .header("X-User-Id", "wh01").header("X-User-Roles", "WAREHOUSE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"P0000\",\"qty\":40}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void 입고_수량이_0이면_400() throws Exception {
        mockMvc.perform(post("/api/lots")
                        .header("X-User-Id", "wh01").header("X-User-Roles", "WAREHOUSE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"P1001\",\"qty\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void 물류_담당이_아니면_입고할_수_없다() throws Exception {
        mockMvc.perform(post("/api/lots")
                        .header("X-User-Id", "md01").header("X-User-Roles", "MD")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"P1001\",\"qty\":40}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 출고하면_수량이_줄고_다_나가면_소진() throws Exception {
        String lotNo = data.lot(TestData.TOFU, 10, LotStatus.AVAILABLE);

        mockMvc.perform(post("/api/lots/{lotNo}/shipments", lotNo)
                        .header("X-User-Id", "wh01").header("X-User-Roles", "WAREHOUSE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qty\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.qty").value(0))
                .andExpect(jsonPath("$.data.status").value("SOLD_OUT"));

        assertThat(data.lotStatus(lotNo)).isEqualTo("SOLD_OUT");
    }

    @Test
    void 판매_보류_로트는_출고할_수_없다() throws Exception {
        String lotNo = data.qualityHoldLot();

        mockMvc.perform(post("/api/lots/{lotNo}/shipments", lotNo)
                        .header("X-User-Id", "wh01").header("X-User-Roles", "WAREHOUSE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qty\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("LOT_NOT_SHIPPABLE"));

        assertThat(data.lotQty(lotNo)).isEqualTo(100);
    }

    @Test
    void 남은_수량보다_많이_출고할_수_없다() throws Exception {
        String lotNo = data.lot(TestData.TOFU, 5, LotStatus.AVAILABLE);

        mockMvc.perform(post("/api/lots/{lotNo}/shipments", lotNo)
                        .header("X-User-Id", "wh01").header("X-User-Roles", "WAREHOUSE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qty\":6}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_QTY"));
    }
}
