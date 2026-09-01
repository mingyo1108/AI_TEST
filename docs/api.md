# API 목록

모든 API는 `ApiResponse` 형식으로 응답한다 (성공 `{"success": true, "data": …}`, 실패 `{"success": false, "error": {"code", "message"}}`).
요청 사용자는 헤더 `X-User-Id`, `X-User-Roles`로 보낸다.

| 메서드 | 경로 | 역할 | 설명 | 요청 | 응답 |
|---|---|---|---|---|---|
| POST | `/api/lots` | WAREHOUSE | 입고 (로트 생성, 소비기한 선택) | `ReceiveRequest` | 201 `LotResponse` |
| POST | `/api/lots/{lotNo}/shipments` | WAREHOUSE | 출고 | `ShipRequest` | `LotResponse` |
| POST | `/api/lots/{lotNo}/quality-holds` | QC | 품질 보류 등록 | `HoldRequest` | 201 `LotResponse` |
| POST | `/api/lots/{lotNo}/quality-holds/release` | QC | 품질 보류 해제 | `HoldRequest` | `LotResponse` |
