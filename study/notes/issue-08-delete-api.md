# Issue 08 — 삭제 API DELETE /api/boards/{seq} (비밀번호 확인 + 연관 삭제)

> 관련 PR: (작성 예정) · 참고: `@DeleteMapping`, FK 삭제 순서, `@Transactional`(원자성)

## 무엇을 만드나
비밀번호 확인 후 게시글을 삭제하는 API. FK로 물린 자식(댓글/첨부)까지 함께, **한 트랜잭션**으로 지운다. 비번이 틀리면 아무것도 지워지지 않는다.

## 알아야 할 개념

### 1. 흐름 (수정과 거의 동일 + 삭제 순서)
```
DELETE /api/boards/{seq} (바디: {"password":"..."})
  → BoardController : 비번을 바디로 받고, 결과를 상태코드로 번역
  → BoardService    : (1) 조회 (2) 비번 확인 (3) 자식→부모 순서로 삭제  → DeleteResult 반환
  → BoardMapper(.xml): DELETE 3방(comment, attachment, board)
```
- 비번 확인 로직은 #7과 완전히 동일하게 재사용(`sha256(입력).equals(저장 해시)`).

### 2. FK 자식부터 삭제 (오늘의 핵심)
`comment`/`attachment` 테이블이 `board_id`로 `board`를 **FK 참조**하는데 `ON DELETE CASCADE`가 없다.
- 부모(board)를 먼저 지우려 하면 → "자식이 아직 참조 중"이라 DB가 **거부**(FK 제약 위반).
- 그래서 **자식(댓글·첨부) 먼저 → board 마지막** 순서로 지운다.
```java
boardMapper.deleteCommentsByBoardId(boardId);
boardMapper.deleteAttachmentsByBoardId(boardId);
boardMapper.deleteBoard(boardId);
```
- (대안: 스키마에 `ON DELETE CASCADE`를 걸면 board만 지워도 자식이 자동 삭제. 여기선 앱에서 순서를 직접 제어하는 방식을 택함.)

### 3. `@Transactional` — 전부 성공 or 전부 취소 (원자성)
```java
@Transactional
public DeleteResult deleteBoard(...) { ... 세 번의 DELETE ... }
```
- 세 DELETE를 **한 묶음**으로 실행. 중간에 하나라도 실패하면 앞서 지운 것도 **롤백**된다.
- 없으면? 예: 댓글·첨부는 지워졌는데 board 삭제에서 오류 → **자식만 사라지고 글은 남는 반쪽 상태**가 될 수 있다. 트랜잭션이 이걸 막는다.
- 비번 불일치는 삭제를 **시작하기도 전에** return → 아무것도 안 지워짐(완료 조건의 "원자성").

### 4. `@DeleteMapping` + 200 vs 204
- `@DeleteMapping("/{seq}")` : DELETE 요청을 이 메서드로.
- 비번을 **바디(@RequestBody)**로 받는다: URL 쿼리(`?password=`)로 넣으면 로그·브라우저 히스토리에 비번이 남는다. (DELETE에 바디를 싣는 건 흔치 않지만 이 이유로 택함)
- **200 vs 204**: 삭제 성공에 204 No Content(바디 없음)도 흔하다. 여기선 다른 API와 같은 공통 봉투(ApiResponse)를 내려주려고 **200 + { success:true }** 를 씀.

### 5. 상태코드 번역 (컨트롤러) — #7과 동일 패턴
- SUCCESS → 200 / NOT_FOUND → 404 / WRONG_PASSWORD → 403.
- 검증 실패(비번 누락 등)는 `@Valid`가 서비스 진입 전에 400.

## 2주차와 뭐가 다른가
- 2주차 삭제: 폼/링크로 요청 후 목록으로 리다이렉트. 이번: 결과를 상태코드+JSON으로 응답.
- "자식부터 삭제 + 트랜잭션"이라는 데이터 정합성 관점이 서버 계층에서 명시적으로 드러남.

## 고민 / 함정
- **삭제 순서**: 부모 먼저 지우면 FK 위반. 항상 자식 → 부모.
- **원자성**: 여러 테이블을 건드리는 삭제는 `@Transactional`로 묶어야 반쪽 삭제를 막는다.
- 시드 데이터(1~24)는 비번이 **평문**이라, 앱의 해시 비교와 어긋나 삭제가 403이 난다. → 성공 테스트는 "POST로 새 글 생성(해시 저장) → 그 글 삭제".

## 오늘 실제로 잡은 버그 (중요)
**증상:** 올바른 비번인데도 수정/삭제가 계속 403(비번 불일치). DB의 저장 해시와 `sha256(입력)`이 분명히 같은데도 실패.
**원인:** 비번 확인에 재사용한 `findById`(원래 #5 상세조회용)의 SELECT에 **`password` 컬럼이 빠져 있었다.** 상세 응답엔 비번을 안 내보내려 일부러 뺐던 것인데, 그 탓에 `board.getPassword()`가 항상 `null` → `sha256(입력).equals(null)` = false → 403.
**교훈:**
- 조회 메서드를 "다른 목적"으로 재사용할 때, 그 SELECT가 **필요한 컬럼을 다 가져오는지** 확인해야 한다.
- 이 버그는 #7 수정 API에도 잠복해 있었다(같은 findById 사용). 이번 findById 수정으로 #7·#8 둘 다 해결됨.
**해결:** findById SELECT에 `password` 추가. 상세 API는 `BoardResponse.from()`이 password를 안 담으므로 노출 위험 없음(도메인 객체 안에만 실림).

## 막히면 볼 것
- 테스트: `backend/http/board-api.http` 의 DELETE(생성→삭제 200 / 비번틀림 403 / 없는글 404).
- 자식까지 함께 지워지는 경로는 댓글/첨부 등록 API(#9·#10)가 생긴 뒤 "새 글+자식 생성 → 삭제"로 검증 가능.

## 회고
- 실행 테스트로 200/201/400/403/404 전 경로 확인. 특히 "올바른 비번인데 403" → findById가 password를 안 읽어오던 잠복 버그를 로그(SELECT 컬럼 목록)로 추적해 잡음.
- 검증(@Valid)이 서비스보다 먼저 도는 것도 체감(짧은 제목이면 403/404 이전에 400).
