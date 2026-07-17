# Issue 06 — 게시글 등록 API POST /api/boards (검증 + 비밀번호 해시)

> 관련 PR: (작성 예정) · 참고: Bean Validation(@Valid), HTTP 상태코드(201), MyBatis useGeneratedKeys

## 무엇을 만드나
클라이언트가 보낸 JSON(제목/내용/작성자/비번/카테고리)을 받아, 검증 통과 시 DB에 새 글을 INSERT 하고 **새 글번호**를 201로 응답하는 API. 비밀번호는 평문이 아니라 SHA-256 해시로 저장.

## 알아야 할 개념

### 1. 3층 흐름 (등록 버전)
```
POST /api/boards  (JSON 바디)
  → BoardController : @Valid @RequestBody 로 받고 검증, 201 Created 로 응답
  → BoardService    : 비번 해시 → 요청 DTO를 Board 도메인으로 변환 → INSERT → 새 boardId 반환
  → BoardMapper(.xml): INSERT 실행 + 생성된 board_id 되받기
```

### 2. `@RequestBody` + `@Valid` — 역할이 둘로 나뉜다 (오늘 새로 안 것)
```java
public ResponseEntity<ApiResponse<Long>> createBoard(
        @Valid @RequestBody BoardCreateRequest request) {
//       └검증 실행    └JSON→객체 변환
```
- **`@RequestBody` = 역직렬화**: 들어온 JSON "글자"를 `BoardCreateRequest` 자바 객체로 변환.
  - 직렬화 = 객체 → 글자(내보낼 때), 역직렬화 = 글자 → 객체(받을 때).
  - 2주차 폼(`key=value&...`)은 `@ModelAttribute`로 받았지만, JSON은 `@RequestBody`.
- **`@Valid` = 검증 실행**: DTO 필드에 붙여둔 규칙(@NotBlank 등)을 **실제로 검사**하라는 신호.
  - `@Valid`가 없으면 규칙은 붙어만 있고 검사 안 함.
  - 규칙 위반 시 **컨트롤러 메서드 진입 전에** 예외 → 400. 그래서 메서드 본문은 "검증 통과한 깨끗한 데이터"만 다룸.

### 3. Bean Validation 애노테이션 (BoardCreateRequest)
| 애노테이션 | 하는 일 | 쓴 곳 |
|---|---|---|
| `@NotBlank` | null·빈문자·공백만 → 실패 (**String 전용**) | title, content, writer, password |
| `@Size(min,max)` | 문자열 길이 범위. **null은 통과** | title 4~100, content 4~2000, writer 3~5, password 4~16 |
| `@Pattern(regexp)` | 정규식 형식 검사 | password: 영문+숫자+특수문자 포함 |
| `@NotNull` | null이면 실패 (숫자/객체용) | categoryId(Integer) |

- 함정: `@Size`는 null을 통과시키므로 **"필수 + 길이"는 `@NotBlank`와 같이** 써야 둘 다 걸린다.
- categoryId는 숫자라 `@NotBlank`(String 전용)를 못 쓰고 `@NotNull`을 쓴다.
- 비번 정규식 `(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*]).+` : 앞을 내다보는(lookahead) 조건 3개로 "영문 있고 / 숫자 있고 / 특수문자 있음"을 각각 확인.

### 4. `ResponseEntity<ApiResponse<Long>>` — 3겹을 뜯어보기 (오늘 새로 안 것)
- **`ResponseEntity`** : DB 엔티티가 아니라 "**HTTP 응답 한 덩어리**"(상태코드+헤더+바디)를 담는 상자. 조회 API는 200이 자동이라 필요 없었지만, 등록은 **201을 직접 붙여야** 해서 씀.
- **`ApiResponse`** : 모든 API가 공유하는 공통 봉투 `{ success, data, message }`.
- **`<Long>`** : 그 봉투의 `data` 칸에 들어갈 타입. 등록은 "새 글번호"만 돌려주면 되니 `Long`.
```java
return ResponseEntity
    .status(HttpStatus.CREATED)      // 상태코드 = 201 (HttpStatus.CREATED = 201 상수)
    .body(ApiResponse.ok(newId));    // 바디 = { success:true, data:42, message:null }
```
- `HttpStatus.CREATED` = 201. 200 OK(조회)와 달리 "**새 자원을 만들었다**"는 의미.
- `.status(...).body(...)` : 점으로 이어가며 응답을 조립(빌더 패턴).

### 5. `ApiResponse.ok(newId)` — "성공 봉투를 만든다" (오늘 정리한 것)
```java
public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(true, data, null); }
```
- `ok` = "데이터만 본다"가 아니라 **success=true 켜고 data 칸에 값을 담는다**. `fail`은 반대(success=false, message에 사유).
- 같은 `ok()`인데 담는 게 다름: 상세는 `ok(board)`(글 전체), 등록은 `ok(newId)`(글번호만).
- `newId`는 확인용이 아니라 **클라이언트에 돌려주려고** 받아둔 실제 결과 번호. 프론트가 이 번호로 방금 쓴 글 상세로 이동 가능.

### 6. 비밀번호 해시 (평문 저장 금지)
```java
String hashed = sha256(request.getPassword());  // 원문 대신 해시를 저장
```
- `MessageDigest.getInstance("SHA-256")` → `digest(byte[])` → 바이트를 `%02x`로 16진수 문자열 변환(DB VARCHAR에 넣기 좋게).
- `NoSuchAlgorithmException`은 checked 예외지만 "SHA-256"은 항상 존재 → try/catch로 RuntimeException 전환.

### 7. INSERT + 생성된 키 되받기 (useGeneratedKeys)
```xml
<insert id="insertBoard"
        parameterType="com.ebsoft.board.board.domain.Board"
        useGeneratedKeys="true" keyProperty="boardId">
    INSERT INTO board (category_id, title, content, writer, password)
    VALUES (#{categoryId}, #{title}, #{content}, #{writer}, #{password})
</insert>
```
- `board_id`(AUTO_INCREMENT) / `view_count`(DEFAULT 0) / `created_at`(DEFAULT NOW)은 **DB가 채우므로 INSERT에서 뺀다.**
- `useGeneratedKeys="true" keyProperty="boardId"` : INSERT 후 MySQL이 만든 새 board_id를 **넘긴 Board 객체의 boardId에 자동 세팅**.
  → Service에서 `insertBoard(board)` 직후 `board.getBoardId()`로 새 번호를 안다.

## 2주차와 뭐가 다른가
- 2주차: `<form>` 제출 → `@ModelAttribute`로 받고 → 저장 후 **리다이렉트(화면 이동)**.
- 이번: JSON을 `@RequestBody`로 받고 → 저장 후 **새 글번호를 JSON으로 응답**(화면 이동은 프론트가 결정).
- 검증도 2주차엔 화면에서 주로 했다면, 이번엔 서버가 `@Valid`로 확실히 막고 400을 내려줌(프론트/서버 이중 방어).

## 고민 / 함정
- **왜 201인가**: 조회는 200, 생성은 201이 REST 관례. "만들어졌다"를 상태코드로 알림.
- **비번은 응답에 절대 넣지 않는다**: 그래서 응답 DTO(BoardResponse)엔 password가 없고, 등록 응답은 `Long`(번호)만.
- **검증 실패 400 메시지 다듬기**는 이번이 아니라 #11 전역 예외 처리에서 공통화 예정.
- `@Size`만 붙이고 `@NotBlank`를 빠뜨리면 빈 값이 통과된다(위 3번 함정).

## 막히면 볼 것
- 테스트: `backend/http/board-api.http` 의 POST 요청 실행(REST Client 확장).
- 성공 예: `201 Created`, 바디 `{ "success": true, "data": <새 글번호> }`
- 검증 실패 예: 짧은 제목/약한 비번 → `400`, 필드별 message.
- 실행: `JAVA_HOME` 잡고 `./gradlew bootRun` (8080), Docker DB(3310) 먼저 기동.

## 회고
<!-- 직접 채우기 -->
- 컨트롤러 3자리(반환타입 / @RequestBody 타입 / 서비스에 넘길 값)를 헷갈렸다가 "타입 자리 vs 값 자리"로 구분하니 정리됨.
- `ResponseEntity`·`<Long>`·`ok()`의 의미를 뜯어보며 "상자 안 봉투 안 데이터" 3겹 구조를 잡음.
- 다음: #11 전역 예외 처리로 검증 400 응답을 프론트 친화적으로 공통화.
