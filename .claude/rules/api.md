---
paths:
  - "src/main/java/**/api/**"
  - "docs/api.md"
---

# API 규칙

기준 구현: `stock/api/QualityHoldController.java`

- Controller 메서드 첫 줄에서 `user.requireRole(Role.XXX)`로 역할을 확인한다. 요청 사용자는 `CurrentUser`로 받는다(헤더 `X-User-Id`, `X-User-Roles`).
- 요청 본문은 `XxxRequest` record에 Bean Validation을 붙이고 `@Valid`로 검증한다.
- 응답은 `ApiResponse.ok(XxxResponse.from(...))`로 한다. domain 객체를 그대로 반환하지 않는다.
- 업무·권한 오류는 `ErrorCode`에 등록한 `BusinessException`으로만 던진다. HTTP 상태 기준은 `ErrorCode` 주석을 따른다.

## `docs/api.md` 갱신

- API를 추가·변경·삭제하면 **같은 커밋에서** `docs/api.md` 표를 고친다 (메서드, 경로, 역할, 설명, 요청, 응답).
- 기존 API의 요청·응답 필드가 바뀌면 그 API 행에 반영한다. 필드 추가처럼 표에 드러나지 않는 변경은 표 아래 메모에 한 줄로 적는다.
