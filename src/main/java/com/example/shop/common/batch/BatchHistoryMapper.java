package com.example.shop.common.batch;

import java.time.LocalDateTime;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 배치 실행 이력(batch_history) 매퍼. SQL은 {@code resources/mapper/common/BatchHistoryMapper.xml}.
 */
@Mapper
public interface BatchHistoryMapper {

    /** 실행 번호 시퀀스의 다음 값. */
    long selectNextRunSeq();

    /** 실행 시작을 기록한다. */
    void insertBatchHistory(@Param("runId") String runId, @Param("jobName") String jobName,
            @Param("serverName") String serverName, @Param("startedAt") LocalDateTime startedAt,
            @Param("status") String status);

    /** 실행 결과를 기록한다. */
    void updateBatchHistoryEnd(@Param("runId") String runId, @Param("status") String status,
            @Param("targetCount") Integer targetCount, @Param("message") String message,
            @Param("endedAt") LocalDateTime endedAt);

    /** 배치의 마지막 실행 결과 (실행 중 제외). */
    Optional<String> selectLastStatusOne(@Param("jobName") String jobName);
}
