package com.example.shop.stock.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LotStatusTest {

    @Test
    void 활성_보류가_없으면_판매_가능() {
        assertThat(LotStatus.resolve(10, false)).isEqualTo(LotStatus.AVAILABLE);
    }

    @Test
    void 활성_보류가_하나라도_있으면_판매_보류() {
        assertThat(LotStatus.resolve(10, true)).isEqualTo(LotStatus.HOLD);
    }

    @Test
    void 수량이_0이면_보류와_상관없이_소진() {
        assertThat(LotStatus.resolve(0, true)).isEqualTo(LotStatus.SOLD_OUT);
        assertThat(LotStatus.resolve(0, false)).isEqualTo(LotStatus.SOLD_OUT);
    }
}
