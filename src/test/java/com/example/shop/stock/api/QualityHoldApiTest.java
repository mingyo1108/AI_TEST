package com.example.shop.stock.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.shop.support.ShopIntegrationTest;
import com.example.shop.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@ShopIntegrationTest
class QualityHoldApiTest {

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
    void 품질_담당은_품질_보류를_등록할_수_있다() throws Exception {
        String lotNo = data.availableLot();

        mockMvc.perform(post("/api/lots/{lotNo}/quality-holds", lotNo)
                        .header("X-User-Id", "qc01").header("X-User-Roles", "QC")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"이물 신고 접수\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("HOLD"))
                .andExpect(jsonPath("$.data.activeHoldReasons[0]").value("QUALITY"));

        assertThat(data.lotStatus(lotNo)).isEqualTo("HOLD");
    }

    @Test
    void 코멘트가_공백이면_400() throws Exception {
        String lotNo = data.availableLot();

        mockMvc.perform(post("/api/lots/{lotNo}/quality-holds", lotNo)
                        .header("X-User-Id", "qc01").header("X-User-Roles", "QC")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        assertThat(data.lotStatus(lotNo)).isEqualTo("AVAILABLE");
    }

    @Test
    void 품질_담당이_아니면_403() throws Exception {
        String lotNo = data.availableLot();

        mockMvc.perform(post("/api/lots/{lotNo}/quality-holds", lotNo)
                        .header("X-User-Id", "wh01").header("X-User-Roles", "WAREHOUSE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"이물 신고 접수\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        assertThat(data.lotStatus(lotNo)).isEqualTo("AVAILABLE");
    }

    @Test
    void 사용자_헤더가_없으면_401() throws Exception {
        String lotNo = data.availableLot();

        mockMvc.perform(post("/api/lots/{lotNo}/quality-holds", lotNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"이물 신고 접수\"}"))
                .andExpect(status().isUnauthorized());

        assertThat(data.lotStatus(lotNo)).isEqualTo("AVAILABLE");
    }

    @Test
    void 없는_로트면_404() throws Exception {
        mockMvc.perform(post("/api/lots/{lotNo}/quality-holds", "LT-NOPE")
                        .header("X-User-Id", "qc01").header("X-User-Roles", "QC")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"이물 신고 접수\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("LOT_NOT_FOUND"));
    }

    @Test
    void 이미_품질_보류_중이면_409() throws Exception {
        String lotNo = data.qualityHoldLot();

        mockMvc.perform(post("/api/lots/{lotNo}/quality-holds", lotNo)
                        .header("X-User-Id", "qc01").header("X-User-Roles", "QC")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"재신고\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("HOLD_ALREADY_EXISTS"));

        assertThat(data.activeHoldCount(lotNo, "QUALITY")).isEqualTo(1);
    }

    @Test
    void 품질_담당은_품질_보류를_해제할_수_있다() throws Exception {
        String lotNo = data.qualityHoldLot();

        mockMvc.perform(post("/api/lots/{lotNo}/quality-holds/release", lotNo)
                        .header("X-User-Id", "qc01").header("X-User-Roles", "QC")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"검사 결과 이상 없음\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.activeHoldReasons").isEmpty());

        assertThat(data.lotStatus(lotNo)).isEqualTo("AVAILABLE");
        assertThat(data.activeHoldCount(lotNo, "QUALITY")).isZero();
    }

    @Test
    void 해제할_품질_보류가_없으면_409() throws Exception {
        String lotNo = data.availableLot();

        mockMvc.perform(post("/api/lots/{lotNo}/quality-holds/release", lotNo)
                        .header("X-User-Id", "qc01").header("X-User-Roles", "QC")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"검사 결과 이상 없음\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("NO_ACTIVE_HOLD"));

        assertThat(data.lotStatus(lotNo)).isEqualTo("AVAILABLE");
    }
}
