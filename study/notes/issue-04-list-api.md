# Issue 04 — 목록 API GET /api/boards (검색 + 페이징)

> 관련 PR: (작성 예정) · 참고: MyBatis 동적 SQL, 페이징(LIMIT/OFFSET)

## 무엇을 만드나
검색어/카테고리/페이지를 쿼리 파라미터로 받아, 조건에 맞는 게시글 목록 + 페이지 정보를 JSON으로 응답하는 API. 첫 3층(Controller→Service→Mapper) 완주.

## 알아야 할 개념

### 1. 3층 흐름 (이번에 처음 다 거침)
```
GET /api/boards?keyword=&categoryId=&page=&size=
  → BoardController : 쿼리파라미터 받음, ApiResponse.ok(...)로 감쌈
  → BoardService    : offset/totalPages 계산, 도메인→DTO 변환, PageResponse 조립
  → BoardMapper(.xml): 실제 SQL 실행
```
- 쿼리 파라미터는 `BoardSearchRequest` 객체에 **자동 바인딩**된다(같은 이름 필드에 채워짐). `@RequestParam` 개별 선언 없이 객체로 받기.

### 2. 동적 SQL (핵심)
조건이 있을 수도/없을 수도 → MyBatis XML `<where>`/`<if>`로 조립.
```xml
<where>
  <if test="keyword != null and keyword != ''">
    AND (title LIKE CONCAT('%', #{keyword}, '%') OR content LIKE CONCAT('%', #{keyword}, '%'))
  </if>
  <if test="categoryId != null">
    AND category_id = #{categoryId}
  </if>
</where>
```
- `<where>` : 안에 조건이 하나도 없으면 WHERE를 안 붙이고, 있으면 맨 앞 `AND`를 자동으로 떼 준다.
- 문자열은 `''`(빈문자)까지 체크, **숫자(categoryId)는 null 체크만**.
- `#{...}` : 파라미터 바인딩(값을 안전하게 꽂음). 문자열 이어붙이기가 아니라서 **SQL 인젝션 방지**.

#### 검색어(LIKE) vs 카테고리(=) — 비교 방식이 다르다 (오늘 헷갈렸던 것)
- **검색어 = "포함"을 찾음** → `LIKE '%키워드%'`
  - `title LIKE CONCAT('%', #{keyword}, '%')` → keyword가 "자바"면 `title LIKE '%자바%'` → "자바 공부하기"처럼 자바가 **들어간** 글.
  - `CONCAT('%', ..., '%')` = 값 앞뒤에 `%`(아무 글자나)를 붙여 "부분 일치"를 만드는 것.
- **카테고리 = "정확히 같음"을 찾음** → `=`
  - `AND category_id = #{categoryId}` → categoryId가 3이면 `category_id = 3` → cat 3인 글만.
  - `category_id`(DB 컬럼) `=`(정확히 같은지) `#{categoryId}`(자바가 넘긴 값). "DB의 category_id가 넘어온 값과 같은 글만 조회".
- 그래서 카테고리는 LIKE/CONCAT이 필요 없다 — 부분 일치가 아니라 번호가 딱 맞는지만 보면 되니까.

#### `<if>`가 거짓이면? — else 없이 "그 줄이 통째로 빠진다" (오늘 새로 안 것)
- `<if>`는 자바 `if`와 같고 **else가 없다**. 조건이 거짓이면 다른 값이 나오는 게 아니라, **그 SQL 조각이 아예 안 들어간다.**
  - `categoryId = 3` → `... AND category_id = 3` (필터 적용)
  - `categoryId = null` → 그 줄이 사라짐 → `WHERE`에 카테고리 조건 없이 **전체 카테고리** 조회
- 즉 "조건이 있으면 SQL에 붙이고, 없으면 그냥 건너뛴다"가 동적 SQL의 핵심.

### 3. 페이징
- `LIMIT #{size} OFFSET #{offset}` 로 잘라 옴. `offset = (page-1) * size` (page 3, size 10 → 20).
- 전체 개수는 **count 쿼리 따로**. `totalPages = ceil(totalElements / size)`.
- 목록 응답 규격: `{ content, page, size, totalElements, totalPages }` → 프론트 페이지네이션에 필요한 값들.

### 4. XML 매퍼 연결 규칙
- `application.yml` 의 `mybatis.mapper-locations: classpath:mapper/*.xml` 로 XML 위치 지정.
- XML `namespace` = 매퍼 인터페이스 전체 경로, `<select id="findBoards">` = 인터페이스 메서드 이름 → 이름으로 연결.
- `<sql id="searchWhere">` + `<include refid="searchWhere"/>` 로 목록/count가 같은 WHERE 재사용 → 둘이 항상 일치.

## 2주차와 뭐가 다른가
- 2주차 검색/페이징 SQL은 재활용 가능(동적 SQL 개념 동일). 다만 결과를 화면(Thymeleaf)에 뿌리던 걸, 이제 `PageResponse`로 만들어 JSON으로 응답.

## 고민 / 함정
- 목록과 count의 WHERE가 다르면 페이지 수가 틀어짐 → `<sql>` 조각 공유로 방지.
- `resultType`을 도메인(Board)으로 두면 map-underscore-to-camel-case 덕에 컬럼 자동 매핑.

## 막히면 볼 것
- 실행 후 테스트:
  - 전체: `curl "localhost:8080/api/boards?page=1&size=3"`
  - 검색: `?keyword=REST`
  - 카테고리: `?categoryId=4`
  - 페이징: `?page=2&size=5`

## 회고
<!-- 직접 채우기 -->
- 동적 SQL `<where>/<if>` + `#{}` 바인딩을 직접 작성. 검색 조건 추가가 "if로 붙이고 뗀다"는 감으로 잡힘. 다음 상세 API(#5)에선 단건 조회 + 조회수 증가.
