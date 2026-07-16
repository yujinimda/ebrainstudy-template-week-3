# Issue 03 — 공통 응답 규격(ApiResponse) + 도메인/DTO

> 관련 PR: (작성 예정) · 참고: 자바 제네릭, 정적 팩토리 메서드, Lombok

## 무엇을 만드나
모든 API가 공유할 **공통 응답 봉투** `ApiResponse<T>` 와, 게시글 **도메인/DTO**를 만든다.
"서버가 어떤 모양의 JSON을 주고받을지"를 확정하는 규격 합의 단계.

## 알아야 할 개념

### 1. 응답 봉투(envelope)
모든 응답을 같은 모양으로 감싼다 → 프론트가 항상 `success`만 보고 분기하면 됨.
```json
성공: { "success": true,  "data": {...}, "message": null }
실패: { "success": false, "data": null,  "message": "글을 찾을 수 없습니다" }
```

### 2. 제네릭 <T> — 선언 vs 사용 (오늘 헷갈렸던 것)
`data`에 담기는 타입이 API마다 다르다(Board / List<Board> ...) → 타입을 갈아끼우려고 제네릭 사용. TS 제네릭과 개념 동일.
```java
public static <T> ApiResponse<T> ok(T data) { ... }
//            ↑① 선언        ↑② 사용    ↑ 사용
```
- **① 앞의 `<T>` = 선언**: "지금부터 T라는 타입 변수를 쓰겠다" 도입. 이게 없으면 뒤의 T가 뭔지 모름.
- **② `ApiResponse<T>` / `(T data)` = 사용**: 선언한 T를 실제로 갖다 씀.
- TS: `function ok<T>(data: T): ApiResponse<T>` 와 같음 (자바는 `<T>`를 반환타입 앞에 둘 뿐).
- **static 메서드가 자기 `<T>`를 또 선언하는 이유**: 클래스의 `<T>`는 인스턴스(객체)에 딸린 것. static은 객체 없이 호출되므로 클래스 T를 못 봐서, 자기 전용 T를 새로 선언해야 함.

### 3. 정적 팩토리 메서드 (ok / fail)
`new ApiResponse<>(...)`를 매번 쓰는 대신 간편 생성자 제공. 생성자는 `private`으로 막아서 **팩토리로만** 만들게 강제.
```java
public static <T> ApiResponse<T> ok(T data)      { return new ApiResponse<>(true,  data, null); }
public static <T> ApiResponse<T> fail(String message) { return new ApiResponse<>(false, null, message); }
```
- `ok`는 성공 데이터(`T data`)를, `fail`은 실패 사유(`String message`)를 받는다 → 받는 게 다르니 파라미터도 다름.
- `fail`도 반환타입은 `ApiResponse<T>` → 호출부에서 성공과 **같은 타입**으로 받게 하려고 (`ApiResponse<Board> r = ApiResponse.fail("...")`).

### 4. 생성자 / this / new 실행 순서 (오늘 새로 안 것)
```java
public class ApiResponse<T> {      // 클래스 선언
    private ApiResponse(boolean success, T data, String message) {  // 생성자 (클래스와 같은 이름, 반환타입 없음)
        this.success = success;    // this.success = 새로 만들어지는 객체의 필드 / success = 밖에서 받은 값
        this.data = data;
        this.message = message;
    }
}
```
- `ApiResponse` 안의 `ApiResponse(...)`는 **클래스 안 클래스가 아니라 생성자**. (클래스와 이름 같고 반환타입 없음)
- `new ApiResponse<>(true, user, "성공")` 실행 순서:
  1. 객체 담을 메모리 공간 확보 (`new`가 시작)
  2. 생성자 호출 → 전달값을 필드에 저장 (`this.x = x`)
  3. 완성된 객체 반환
- "안쪽 생성자가 먼저" 실행되는 게 아니라, `new`가 객체 생성을 시작하고 그 과정에서 생성자를 호출하는 것.

### 5. Lombok @Getter (오늘 새로 안 것)
- `@Getter`는 **값을 꺼내는 뜻이 아니라**, 각 필드의 getter 메서드(`isSuccess()`, `getData()`, `getMessage()`)를 **자동 생성**해주는 것.
- 없으면 그 getter들을 손으로 다 써야 함. **필드/생성자는 자동 생성 안 됨** — 직접 작성.

### 6. DTO 분리 — 요청 vs 응답
"받는 모양"과 "주는 모양"이 다르다.
- **BoardCreateRequest**(요청): `categoryId, title, content, writer, password` — 등록 시 클라이언트가 보냄.
- **BoardResponse**(응답): password **제외**(민감정보), 대신 `boardId, viewCount, createdAt` 포함(서버가 정하는 값).
- **Board**(도메인): DB board 테이블 한 행과 1:1. MyBatis가 SELECT 결과를 채워줌.
- `LocalDateTime`은 Jackson이 기본으로 ISO-8601 문자열(`2026-07-01T10:00:00`)로 직렬화.

## 2주차와 뭐가 다른가
- 2주차는 화면에 바로 뿌려서 "응답 규격"이란 개념이 약했다. API로 분리하니 **JSON 계약**을 미리 정하는 게 중요해짐.

## 고민 / 함정
- 도메인(Board)을 그대로 응답에 쓰면 password가 노출됨 → 반드시 응답 DTO로 걸러 내보낸다.
- 파일 위치: `common/ApiResponse.java`, `board/domain/Board.java`, `board/dto/BoardResponse.java`, `board/dto/BoardCreateRequest.java`

## 막히면 볼 것
- 컴파일: `cd backend && ./gradlew compileJava`
- 제네릭: `<T>`는 선언(앞) → 사용(뒤). static 메서드는 자기 `<T>` 필요.

## 회고
<!-- 직접 채우기 -->
- 제네릭 선언/사용 구분, 생성자와 `new` 실행 순서, `@Getter`가 하는 일을 이번에 확실히 잡음. 이 `ApiResponse.ok/fail` 봉투가 #4부터 모든 컨트롤러 응답에 쓰인다.
