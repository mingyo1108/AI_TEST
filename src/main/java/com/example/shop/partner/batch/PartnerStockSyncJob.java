package com.example.shop.partner.batch;

import com.example.shop.common.batch.BatchRunner;
import com.example.shop.partner.application.PartnerStockSyncService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 제휴몰 재고 연동 배치. 30분마다 실행한다.
 */
@Component
public class PartnerStockSyncJob {

    /** 배치 이름. */
    public static final String JOB_NAME = "PARTNER_STOCK_SYNC";

    private final PartnerStockSyncService syncService;
    private final BatchRunner batchRunner;

    public PartnerStockSyncJob(PartnerStockSyncService syncService, BatchRunner batchRunner) {
        this.syncService = syncService;
        this.batchRunner = batchRunner;
    }

    /** 매시 0분·30분에 실행한다. */
    @Scheduled(cron = "0 0/30 * * * *", zone = "Asia/Seoul")
    public void run() {
        batchRunner.run(JOB_NAME, syncService::sync);
    }
}
