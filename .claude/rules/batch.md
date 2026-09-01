---
paths:
  - "src/main/java/**/batch/**"
---

# 배치 규칙

기준 구현: `partner/batch/PartnerStockSyncJob.java`

- 구성: `XxxJob`(`@Scheduled`) → `BatchRunner.run(JOB_NAME, ...)` → application Service(`@Transactional`). 실행 이력은 `BatchRunner`가 `batch_history`에 남긴다.
- Job은 일정과 실행만 맡는다. 판정 로직은 Service에 두고, 판정 날짜·시각은 Job이 주입받은 `Clock`으로 구해 Service에 인자로 넘긴다.
- 정해진 시각에 도는 배치는 `@Scheduled`에 `zone = "Asia/Seoul"`을 지정한다. 서버 시간대와 상관없이 한국 시간 기준이어야 한다.
- `JOB_NAME`은 대문자 스네이크 케이스 상수로 둔다.
