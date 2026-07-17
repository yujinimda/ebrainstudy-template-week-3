# Issue 07 — 수정 API PUT /api/boards/{seq} (비밀번호 확인)

> 관련 PR: (작성 예정) · 참고: `@PutMapping`/멱등성, 비밀번호 해시 비교, HTTP 상태코드(200/403/404)

## 무엇을 만드나
게시글 수정 API. 수정 전 **비밀번호를 확인**하고, 결과에 따라 상태코드를 다르게 응답한다. 값 변경은 UPDATE로, 수정 시각(`updated_at`)도 이때 채운다.

## 알아야 할 개념

### 1. 3층 흐름 (수정 버전) — 서비스는 "판단", 컨트롤러는 "응답"
```
PUT /api/boards/{seq} (JSON 바디)
  → BoardController : @Valid @RequestBody 로 받고, 서비스 결과를 HTTP 상태코드로 번역
  → BoardService    : (1) 글 조회 (2) 비번 확인 (3) 값 갱신 → UpdateResult 반환
  → BoardMapper(.xml): UPDATE 실행
```
- **서비스는 HTTP를 모른다.** `SUCCESS/NOT_FOUND/WRONG_PASSWORD`(내부 신호)만 돌려줌.
- **컨트롤러가 그걸 상태코드로 번역**해서 프론트로 내보냄. (재료=서비스, 포장·배송=컨트롤러)

### 2. `enum UpdateResult` — 내부 신호지 프론트로 나가는 값이 아니다 (오늘 헷갈렸던 것)
```java
public enum UpdateResult { SUCCESS, NOT_FOUND, WRONG_PASSWORD }
```
- 이 세 단어는 **어디서 가져온 게 아니라 여기서 내가 지어낸 이름.** (`HttpStatus.NOT_FOUND`(스프링 제공, =404)와 이름만 같고 완전히 다른 것)
- enum = "가능한 값이 딱 정해진 목록" 타입. 정해진 셋 외엔 못 써서 오타/잘못된 값이 원천 차단됨(문자열 `"not_found"`보다 안전). 신호등 빨강/노랑/초록 같은 것.
- **프론트는 이 단어를 절대 못 본다.** 서비스→컨트롤러 사이의 내부 귓속말일 뿐.
```
[Service]  return UpdateResult.NOT_FOUND     (내부 신호, 프론트 못 봄)
[Controller] → 404 + { success:false, message:"게시글을 찾을 수 없습니다" }  ← 이게 프론트로 감
[프론트]   받는 건 상태코드(404) + JSON 메시지. "NOT_FOUND" 단어 자체는 안 나감.
```

### 3. 비밀번호 확인 — 해시끼리 비교 (이번 핵심)
```java
if (!sha256(request.getPassword()).equals(board.getPassword())) {
    return UpdateResult.WRONG_PASSWORD;
}
```
- DB엔 **해시**가 저장돼 있다(등록 때 sha256). 원문 비교가 아니라, **입력 비번을 같은 방식으로 해시해서** 저장된 해시와 비교.
- 순서(괄호): `sha256(입력)` 을 **먼저** 완전히 닫고 → 그 뒤에 `.equals(board.getPassword())`. (`sha256(x.equals(y))`처럼 안쪽에 넣으면 꼬임)
- 비교 상대: 왼쪽 `sha256(request.getPassword())`(사용자 입력 해시) vs 오른쪽 `board.getPassword()`(DB 저장 해시).
- `!` = "아니다". "안 맞으면 WRONG_PASSWORD"라서 `.equals(...)` 앞에 `!`.

### 4. `=` vs `==`, `set` vs `get` (오늘 실수했던 것)
- **`=` (하나)** = 대입(넣기), **`==` (둘)** = 비교(같은지). 자바는 `if(board = null)`처럼 대입을 조건에 쓰면 **컴파일 에러로 막아줌**(참/거짓이 아니라서). → `if (board == null)`.
- **문자열은 `==` 말고 `.equals`** 로 비교. (`==`는 "같은 객체인지"를 봐서 값이 같아도 false일 수 있음)
- **`getPassword()`** = 값 꺼내기(읽기), **`setPassword(값)`** = 값 넣기(쓰기, 인자 필요). 저장된 비번을 "읽어서" 비교하는 거라 `board.getPassword()`.

### 5. SQL `UPDATE` — SET의 `=`(대입) vs WHERE의 `=`(비교) (오늘 새로 안 것)
```sql
UPDATE board
SET   category_id = #{categoryId},   -- SET의 = : 대입("이 칸에 새 값 넣어라")
      title       = #{title},
      updated_at  = NOW()            -- 수정 시각을 이때 채움(등록 땐 NULL이던 컬럼)
WHERE board_id = #{boardId}          -- WHERE의 = : 비교("이 값과 같은 줄을 찾아라")
```
- **`WHERE`** = "어느 줄(행)에 적용할지" 고르는 필터. **WHERE 없으면 전체 줄이 다 바뀜**(위험). board_id로 그 한 줄만 지정.
- 같은 `=`인데 위치로 뜻이 갈림: `SET`에선 대입(자바 `x=5`), `WHERE`에선 비교(자바 `x==5`).
- password/writer/view_count/created_at은 SET에 없음 = 수정 대상 아님(그대로 둠).

### 6. INSERT vs UPDATE — 왜 문법이 다른가 (오늘 새로 안 것)
```sql
INSERT INTO board (category_id, title, ...) VALUES (#{categoryId}, #{title}, ...)  -- 새 줄 추가
UPDATE board SET title = #{title}, ... WHERE board_id = #{boardId}                 -- 있는 줄 수정
```
| | INSERT | UPDATE |
|---|---|---|
| 하는 일 | 새 줄 **추가** | 있는 줄 **수정** |
| 짝 맞추기 | (칸들) / (값들) 을 **순서(위치)**로 | `칸 = 값` 개별 지정 |
| `=` | 안 씀(순서로 짝지음) | SET=대입, WHERE=비교 |
| `WHERE` | 없음(새로 만드니 대상 줄이 없음) | 있음(어느 줄 고칠지 골라야) |
- INSERT는 순서로 짝지어서 `=` 불필요, UPDATE는 칸마다 값을 대입해야 해서 `=` 필요.

### 7. 매퍼 인터페이스는 "선언"만, 진짜 SQL은 XML (오늘 다시 정리)
```java
Board findById(@Param("boardId") Long boardId);   // 몸통 {} 없이 ; 로 끝 = 선언(이름표)만
```
- 인터페이스엔 메서드 **이름/입출력 선언만** 있고 SQL이 없다(한 줄인 게 정상).
- 실제 SQL은 `BoardMapper.xml`의 `<select id="findById">`에 있고, **같은 이름(id)으로 연결**됨. → 구현 클래스를 안 만들어도 되는 이유 = **XML이 곧 구현**. (#4 노트의 namespace/id 연결과 같은 얘기)

### 8. 대문자 타입 vs 소문자 변수 (오늘 새로 안 것)
```java
private final BoardMapper boardMapper;   // BoardMapper=타입(설계도), boardMapper=변수(실제 물건)
```
- 자바 관례: **타입(클래스/인터페이스)은 대문자 시작**(`BoardMapper`), **변수/메서드는 소문자 시작**(`boardMapper`, `findById`).
- `boardMapper.findById(...)` = "boardMapper 변수에 담긴 객체에게 findById를 시켜라". `BoardMapper`라는 메서드는 없다(파일명=public 타입명 규칙이라 파일명과 인터페이스명이 같을 뿐).

## 2주차와 뭐가 다른가
- 2주차 수정: 폼 제출 → 저장 후 리다이렉트. 이번: JSON을 받고 결과를 상태코드+JSON으로 응답(화면 이동은 프론트가 결정).
- 비번 확인이라는 "권한 판단"이 서비스 계층으로 명확히 분리됨.

## 고민 / 함정
- **비번 확인을 SQL WHERE에 넣지 않는 이유**: `WHERE ... AND password=...` 로 하면 실패 시 "바뀐 행 0"만 남아 **글 없음(404)인지 비번 틀림(403)인지 구분 불가**. 그래서 서비스에서 findById로 존재를 먼저 보고, 비번을 따로 비교해 둘을 갈라 응답.
- 상태코드 선택: 성공 200 / 글 없음 404 / 비번 불일치 403(글은 있으나 권한 없음).
- 검증 실패(짧은 제목 등)는 `@Valid`가 서비스 진입 전에 400으로 막음.

## 막히면 볼 것
- 테스트: `backend/http/board-api.http` 의 PUT 요청들(성공/비번틀림/없는글/검증실패).
- 기대: 성공 200 · 비번틀림 403 · 없는글 404 · 검증실패 400.
- 실행: `JAVA_HOME` 잡고 `./gradlew bootRun`(8080) + Docker DB(3310).

## 회고
<!-- PR 끝나고 채우기 -->
- 서비스(판단)와 컨트롤러(응답)의 역할 분리를 enum 반환으로 체감. `=`/`==`, `set`/`get`, 괄호 순서 같은 기본 실수를 직접 겪고 고침.
