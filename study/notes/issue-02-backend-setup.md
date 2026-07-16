# Issue 02 — 백엔드 셋업: Spring Boot API + MySQL + MyBatis

> 관련 PR: #18 · 참고: [MyBatis Spring Boot Quick Start](https://github.com/mybatis/spring-boot-starter/wiki/Quick-Start)

## 무엇을 만드나
`backend/`에 Spring Boot API 프로젝트를 세우고, MyBatis로 MySQL(3310)에 붙어 **쿼리 한 방이 도는 것**까지 확인. 헬스체크 엔드포인트 `GET /api/health` 가 board 개수를 세어 돌려준다.

## 알아야 할 개념
- **Spring Boot 3층 구조** (프론트 감각 대응):

  | 프론트 | Spring Boot | 역할 |
  |---|---|---|
  | `app/api/.../route.ts` | **Controller** (`@RestController`) | HTTP 요청 받고 JSON 응답 |
  | 핸들러 안 로직 함수 | **Service** | 비즈니스 로직 |
  | Prisma/Drizzle | **Mapper** (MyBatis) | 실제 SQL 실행 |

- **MyBatis는 ORM이 아니다.** Prisma는 SQL을 대신 생성하지만, MyBatis는 **내가 직접 쓴 SQL을 그대로 실행**하고 결과를 Java 객체에 매핑한다. (2주차 SQL 재활용 가능한 이유)
- **`@Mapper` 인터페이스**: 인터페이스만 만들면 MyBatis가 런타임에 구현체를 자동 생성. 구현 클래스 안 짬.
- **`@Select("SQL")` 애노테이션**: "이 메서드를 호출하면 이 SQL을 실행해줘"라고 다는 꼬리표.
  ```java
  @Mapper
  public interface HealthMapper {
      @Select("SELECT COUNT(*) FROM board")
      long countBoards();
  }
  ```
- **Controller가 Map/객체를 반환하면 Spring이 JSON으로 직렬화**해준다 (화면을 "그리는" 게 아님).
- **메서드 = 그 객체가 가지고 있는 기능(행동)** (나중에 다시 확인한 것):
  ```
  객체.메서드(값)   →   누가.무엇을한다(재료)
  boardService.getBoard(3L)  →  "boardService야, 3번 글 조회 기능을 실행해줘"
  ```
  - 프론트도 똑같다: `console.log(...)`(console 객체의 log 메서드), `array.push(...)`(배열 객체의 push 메서드).
  - 매퍼 인터페이스에 선언한 것들(`findById`, `increaseViewCount`...)도 전부 "boardMapper 객체가 가진 기능"이다.

## 2주차와 뭐가 다른가
- 2주차: `@Controller` + Thymeleaf → 뷰(HTML) 반환.
- 3주차: `@RestController` → 객체 반환 → Spring이 JSON으로 변환.
- 화면 렌더링이 서버에서 완전히 빠짐.

## 고민해봐야 할 것 / 함정
- **버전 호환성**: 최신 Spring Initializr는 Boot 4.1을 주지만 `mybatis-spring-boot-starter`가 아직 Boot 4 미호환.
  → 안정적인 **Boot 3.4.1 + mybatis 3.0.4** 로 맞췄다. (프론트에서 peer dependency 안 맞아 버전 내리는 것과 동일)
- **내가 막혔던 지점 (중요):**
  1. `@Select` 애노테이션 없이 SQL을 맨몸으로 적음 → 자바는 SQL을 못 읽는다. SQL은 반드시 `@Select("...")` **문자열 안**에.
  2. `@SELECT` 로 적음 → 애노테이션 이름은 **대소문자 구분**. import한 `Select`와 정확히 일치해야 함(`@Select`).
  3. 헷갈린 핵심: `@Select`(자바한테 하는 말) vs 따옴표 안 `SELECT`(DB한테 하는 말)는 **다른 두 세계**.

## 막히면 볼 것
- 실행: `cd backend && ./gradlew bootRun`
- 확인: `curl http://localhost:8080/api/health` → `{"status":"UP","boardCount":24}`
- DB 설정: `backend/src/main/resources/application.yml` (url `jdbc:mysql://localhost:3310/ebrainsoft_study`)
- `map-underscore-to-camel-case: true` → DB `created_at` ↔ 자바 `createdAt` 자동 매핑

## 회고
<!-- 직접 채우기 -->
- `@Select` 애노테이션과 SQL 문자열의 경계를 몸으로 익힘. 이 `@Mapper` + `@Select` 패턴이 앞으로 모든 API(#4~#10)의 데이터 접근 뼈대가 된다.
