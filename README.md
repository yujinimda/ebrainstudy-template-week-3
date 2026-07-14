# eBrainSoft 스터디 3주차 — Spring Boot API + Next.js 게시판

2주차([Spring Boot 게시판](https://github.com/yujinimda/ebrainstudy-template-week-2))와 **같은 게시판**을,
이번엔 **REST API 서버 + Next.js 프론트로 분리**해서 다시 만든다.
"화면까지 서버가 그려주던 걸(Thymeleaf) 떼어내면 무엇이 어떻게 달라지는가"를 몸으로 이해하는 게 목표.

## 기술 스택

| 구분 | 사용 기술 |
|---|---|
| 백엔드 (`backend/`) | Spring Boot 3.x, MyBatis, Lombok |
| 프론트 (`frontend/`) | Next.js (App Router), axios, React Query, TypeScript |
| DB | MySQL 8.0 (docker, 포트 **3310**) |
| 통합 | nginx (리버스 프록시) |
| 빌드 | 백엔드 Gradle · 프론트 npm |

## 저장소 구조 (모노레포)

```
ebrainstudy-template-week-3/
├── backend/     # Spring Boot REST API (이슈 #2에서 직접 생성)
├── frontend/    # Next.js 앱          (이슈 #12에서 직접 생성)
├── docker/      # MySQL (미리 제공)
├── nginx/       # 리버스 프록시 설정  (이슈 #16에서 직접 생성)
└── study/       # 학습 노트
```

> `backend/`, `frontend/`, `nginx/`는 **비어 있다.** 셋업 자체가 학습 대상이라 이슈 #2·#12·#16에서 직접 만든다.
> **API를 먼저 전부 완성(#2~#11)한 뒤 프론트(#12~#15)를 만들고**, 마지막에 nginx로 통합(#16)한다.

## 학습 루틴

이슈 하나당: **개념 설명 → 퀴즈 → 핵심 로직은 내가 직접(TODO(human)) → 리뷰 → 노트 → PR**
자세한 규칙은 [study/00-learning-workflow.md](study/00-learning-workflow.md).

## 진행 현황

**0단계 — 개념 & 셋업**
- [ ] #1 3주차 오리엔테이션: REST API란 + 서버/프론트 분리 아키텍처
- [ ] #2 백엔드 셋업: Spring Boot API 프로젝트 + MySQL + MyBatis

**1단계 — 백엔드 REST API (여기까지 API 완성이 목표)**
- [ ] #3 공통 응답 규격(ApiResponse) + 도메인/DTO
- [ ] #4 목록 API `GET /api/boards` (검색 + 페이징)
- [ ] #5 상세 API `GET /api/boards/{seq}` + 조회수 증가
- [ ] #6 등록 API `POST /api/boards` + 서버 검증
- [ ] #7 수정 API `PUT /api/boards/{seq}` (비번 확인)
- [ ] #8 삭제 API `DELETE /api/boards/{seq}` (비번 확인)
- [ ] #9 댓글 API (목록 + 등록)
- [ ] #10 파일 업로드/다운로드 API (multipart + binary)
- [ ] #11 전역 예외 처리 → HTTP 상태코드 매핑 + CORS

**2단계 — 프론트엔드 (Next) — API 완성 후 착수**
- [ ] #12 프론트 셋업: Next.js(App Router) + axios + React Query
- [ ] #13 목록 페이지 (검색/페이징 + React Query 연동)
- [ ] #14 보기 페이지 (상세 + 댓글 + 파일 다운로드)
- [ ] #15 등록/수정 폼 (프론트 검증 + 서버 에러 표시 + 비번 확인 + 삭제)

**3단계 — 통합**
- [ ] #16 nginx 리버스 프록시로 한 도메인 통합

## DB 띄우기

```bash
cd docker
docker compose up -d
# 접속: localhost:3310 / ebsoft / ebsoft / ebrainsoft_study
```

최초 기동 시 `docker/initdb/`의 스키마·시드가 자동 적용된다. (2주차와 같은 스키마, 페이징 테스트용으로 시드 데이터를 늘려 둠)

## 폰에서 복습

- 이슈 본문과 `study/notes/`가 전부 이 리포에 있어서 폰 GitHub 앱으로 바로 읽을 수 있다.
- 폰 Claude 앱에서 GitHub 연동으로 이 리포를 연결하면 노트 기반으로 질문/복습을 이어갈 수 있다.
