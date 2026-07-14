-- ============================================================
-- 게시판 스키마 (이슈 #1)
-- MySQL 8.0 / 엔진 InnoDB / 문자셋 utf8mb4
--
-- 이 파일은 docker-compose의 /docker-entrypoint-initdb.d 에 마운트되어
-- DB가 "처음 생성될 때" 자동 실행된다.
-- 수동으로 다시 적용할 수도 있게, 맨 위에서 기존 테이블을 지우고 시작한다.
-- (지우는 순서는 만드는 순서의 역순 — FK로 물려 있어서)
-- ============================================================

-- 이 연결의 문자셋 선언 (없으면 클라이언트가 latin1로 오해해 한글이 깨진 채 저장될 수 있음)
SET NAMES utf8mb4;

DROP TABLE IF EXISTS attachment;
DROP TABLE IF EXISTS comment;
DROP TABLE IF EXISTS board;
DROP TABLE IF EXISTS category;

-- ------------------------------------------------------------
-- 카테고리 (코드표) : "DB에 저장되어 있음, 화면에서 관리 X"
-- 게시글이 category_id 로 이걸 가리킨다.
-- ------------------------------------------------------------
CREATE TABLE category (
    category_id  INT          NOT NULL AUTO_INCREMENT,   -- PK, 1씩 자동 증가
    name         VARCHAR(50)  NOT NULL UNIQUE,           -- 카테고리 이름 (중복 금지)
    PRIMARY KEY (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 게시글
-- ------------------------------------------------------------
CREATE TABLE board (
    board_id     INT           NOT NULL AUTO_INCREMENT,  -- PK ("몇 번째 글인지")
    category_id  INT           NOT NULL,                 -- FK → category (어떤 분류인지)
    title        VARCHAR(100)  NOT NULL,                 -- 제목 (검증: 4~100자)
    content      VARCHAR(2000) NOT NULL,                 -- 내용 (검증: 4~2000자)
    writer       VARCHAR(20)   NOT NULL,                 -- 작성자 (검증: 3~5자, 여유 있게)
    password     VARCHAR(100)  NOT NULL,                 -- 비밀번호 (지금은 평문, 실무는 해시)
    view_count   INT           NOT NULL DEFAULT 0,       -- 조회수, 처음엔 0
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 등록 시 자동 현재시각
    updated_at   DATETIME      NULL,                     -- 수정 전엔 NULL → 화면에 '-'
    PRIMARY KEY (board_id),
    CONSTRAINT fk_board_category
        FOREIGN KEY (category_id) REFERENCES category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 댓글 (게시글 1 : N 댓글)
-- ------------------------------------------------------------
CREATE TABLE comment (
    comment_id   INT           NOT NULL AUTO_INCREMENT,  -- PK
    board_id     INT           NOT NULL,                 -- FK → board (어느 글의 댓글인지)
    writer       VARCHAR(20)   NOT NULL,                 -- 댓글 작성자
    content      VARCHAR(1000) NOT NULL,                 -- 댓글 내용
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (comment_id),
    CONSTRAINT fk_comment_board
        FOREIGN KEY (board_id) REFERENCES board (board_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 첨부파일 (게시글 1 : N 첨부)
-- 파일 자체는 서버에 저장하고, 여기엔 "메타데이터"만 둔다.
--   original_name : 사용자가 올린 원래 이름 (화면 표시 / 다운로드 이름)
--   stored_name   : 서버에 실제 저장한 이름 (충돌 방지용 고유 이름)
-- ------------------------------------------------------------
CREATE TABLE attachment (
    attachment_id INT          NOT NULL AUTO_INCREMENT,  -- PK
    board_id      INT          NOT NULL,                 -- FK → board
    original_name VARCHAR(255) NOT NULL,                 -- 원본 파일명
    stored_name   VARCHAR(255) NOT NULL UNIQUE,          -- 서버 저장 파일명 (고유, 겹치면 안 됨)
    file_size     BIGINT       NOT NULL,                 -- 파일 크기(byte) — 클 수 있어 BIGINT
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (attachment_id),
    CONSTRAINT fk_attachment_board
        FOREIGN KEY (board_id) REFERENCES board (board_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 카테고리 기본값 (화면에서 관리하지 않으므로 여기서 미리 넣어둔다)
-- ------------------------------------------------------------
INSERT INTO category (name) VALUES
    ('JAVA'),
    ('JavaScript'),
    ('Database'),
    ('Spring'),
    ('etc');
