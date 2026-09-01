---
paths:
  - "src/main/resources/mapper/**"
  - "src/main/java/**/mapper/**"
---

# 매퍼 규칙

기준 구현: `stock/mapper/StockLotMapper.java` ↔ `resources/mapper/stock/StockLotMapper.xml`

## 사내 MyBatis 컨벤션

| ID | 규칙 |
|---|---|
| C1 | 조회하는 모든 테이블에 `WITH (NOLOCK)` (조인·서브쿼리 테이블 포함, INSERT·UPDATE·DELETE 제외) |
| C2 | 값은 `#{}`로만 바인딩한다. `${}` 금지. 정렬 컬럼·방향은 `<choose>`로 허용한 값만 |
| C3 | `SELECT *` 금지. 컬럼을 나열하고 결과는 `resultMap`으로 컬럼→필드를 명시한다 (record는 `<constructor>`, 순서 = record 생성자 순서) |
| C4 | `namespace` = 매퍼 인터페이스 전체 경로, 쿼리 `id` = 메서드 이름, `@Param` 이름 = `#{}` 이름 |
| C5 | 쿼리 id: `select{대상}{One\|List\|Count}`, `insert{대상}`, `update{대상}…`, `delete{대상}` |
| C6 | 여러 쿼리가 같이 쓰는 컬럼 목록은 `<sql id>` + `<include>` |
| C7 | 동적 조건은 `<where>`·`<if>`. 문자열 조건은 `null`과 빈 문자열을 모두 검사하고, `<foreach>`는 빈 목록일 때를 처리한다 |
| C8 | 운영 DB는 조회만 한다. 데이터 변경은 사람이 승인한 스크립트로 |

## 작성 방법

- 매퍼 인터페이스는 `<기능>/mapper/`에 `@Mapper`로 두고, SQL은 XML에만 쓴다 (애너테이션 SQL 금지).
- 한 건 조회는 `Optional<T>`를 돌려준다. 건수 확인은 `select…Count` + 서비스에서 `> 0`.
- SQL은 MSSQL 문법(H2 `MODE=MSSQLServer`): `TOP n`, `ISNULL`, `GETDATE()`. `LIMIT`·`AUTO_INCREMENT`는 쓰지 않는다.
- XML 안의 `<`·`>`는 `&lt;`·`&gt;`로 쓴다.
- 매퍼가 만드는 실제 SQL은 `node scripts/render-sql.mjs XxxMapper.queryId params.json`으로 확인하고, 개발 DB에서 `node scripts/db.mjs --env dev`로 실행해 본다.
