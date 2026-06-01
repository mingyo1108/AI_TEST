package com.example.shop.common.batch;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.IntSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 배치 본문을 실행하고 실행 이력(batch_history)을 남긴다. 모든 {@code XxxJob}이 이것으로 본문을 실행한다.
 *
 * <p>이력은 본문 트랜잭션과 따로 기록한다(이 클래스는 트랜잭션을 걸지 않음). 그래야 본문이 롤백돼도
 * 실패한 실행이 이력에 남는다.
 */
@Component
public class BatchRunner {

    /** 실행 중. */
    public static final String RUNNING = "RUNNING";
    /** 성공. */
    public static final String SUCCESS = "SUCCESS";
    /** 실패. */
    public static final String FAIL = "FAIL";

    private static final Logger log = LoggerFactory.getLogger(BatchRunner.class);
    private static final DateTimeFormatter RUN_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final BatchHistoryMapper historyMapper;
    private final Clock clock;
    private final String serverName;

    public BatchRunner(BatchHistoryMapper historyMapper, Clock clock,
            @Value("${shop.server-name:local}") String serverName) {
        this.historyMapper = historyMapper;
        this.clock = clock;
        this.serverName = serverName;
    }

    /** 배치 본문을 한 번 실행한다. 본문이 실패해도 예외를 밖으로 던지지 않고 이력에 남긴다. */
    public String run(String jobName, IntSupplier body) {
        String runId = start(jobName);
        log.info("[{}] 시작 run={}", jobName, runId);
        try {
            int count = body.getAsInt();
            finish(runId, SUCCESS, count, null);
            log.info("[{}] 완료 run={} count={}", jobName, runId, count);
            return SUCCESS;
        } catch (RuntimeException e) {
            log.error("[{}] 실패 run={}: {}", jobName, runId, e.getMessage());
            finish(runId, FAIL, null, e.getMessage());
            return FAIL;
        }
    }

    /** 배치의 마지막 실행 결과. */
    public Optional<String> findLastStatus(String jobName) {
        return historyMapper.selectLastStatusOne(jobName);
    }

    private String start(String jobName) {
        LocalDateTime now = LocalDateTime.now(clock);
        long seq = historyMapper.selectNextRunSeq();
        String runId = "B" + now.format(RUN_DATE) + "-" + String.format("%04d", seq);
        historyMapper.insertBatchHistory(runId, jobName, serverName, now, RUNNING);
        return runId;
    }

    private void finish(String runId, String status, Integer targetCount, String message) {
        historyMapper.updateBatchHistoryEnd(runId, status, targetCount, truncate(message), LocalDateTime.now(clock));
    }

    private String truncate(String message) {
        if (message == null || message.length() <= 500) {
            return message;
        }
        return message.substring(0, 500);
    }
}
