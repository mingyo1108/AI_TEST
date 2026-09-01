---
paths:
  - "src/main/java/**/application/**"
---

# 서비스 규칙

기준 구현: `stock/application/LotService.java`(진입) → `stock/application/HoldService.java`(공용)

## 같은 기능 안 서비스끼리의 호출

| 종류 | 하는 일 | 예 |
|---|---|---|
| 진입 서비스 | Controller·Job이 부르는 유스케이스. 공용 서비스를 조합한다 | `LotService` (판매 중지·소비기한 정정 같은 새 유스케이스도 진입 서비스로 만든다) |
| 공용 서비스 | 한 가지 상태를 맡아 그 상태를 바꾸는 규칙을 모은다. 유스케이스 하나뿐이면 Controller가 바로 불러도 된다 | `HoldService`(보류와 그에 따른 로트 상태) |

- 진입 서비스는 같은 기능의 공용 서비스를 부를 수 있다. 진입 서비스끼리는 부르지 않는다.
- 공용 서비스는 다른 서비스를 부르지 않는다. 호출은 한 단계까지만 이어지고 순환이 생기지 않는다.
- 두 진입 서비스가 같은 처리를 필요로 하면 서로 부르지 말고, 그 처리를 공용 서비스나 domain으로 옮긴다.
- 보류(`stock_hold`)는 `HoldService`만 다룬다. 다른 서비스는 `StockHoldMapper`를 주입받지 않고, 조회도 `HoldService`의 조회 메서드로 한다.
- 새 공용 서비스가 필요해 보이면 만들기 전에 보고한다.

## 트랜잭션

- 트랜잭션은 진입 서비스의 `@Transactional` 메서드에서 시작한다. 한 API 호출이나 한 번의 배치 실행이 바꾸는 것은 그 메서드 하나에서 끝낸다.
- 공용 서비스 메서드는 기본 전파(`REQUIRED`)로 바깥 트랜잭션에 참여한다. 공용 서비스 안에서 `REQUIRES_NEW`를 쓰거나 예외를 잡아 삼키지 않는다. 공용 서비스가 던진 예외는 전체를 롤백시킨다.

## 다른 기능 호출

- 다른 기능은 그 기능의 application Service로만 쓴다. 다른 기능의 Mapper·infra를 직접 쓰지 않는다. 그 Service가 주고받는 domain 타입(record·enum)은 써도 된다.
- 기능 사이 의존은 `partner` → `stock` → `product` 한 방향이다. 거꾸로 불러야 할 것 같으면 멈추고 보고한다.
