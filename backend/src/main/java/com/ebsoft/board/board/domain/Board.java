package com.ebsoft.board.board.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 게시글 도메인 — DB의 board 테이블 한 행과 1:1 대응.
 * MyBatis가 SELECT 결과를 이 객체에 채워준다.
 * (application.yml의 map-underscore-to-camel-case 덕분에 board_id -> boardId 자동 매핑)
 */
@Getter
@Setter
public class Board {
    private Long boardId;           // PK (board_id)
    private Integer categoryId;     // FK -> category
    private String title;
    private String content;
    private String writer;
    private String password;        // 도메인엔 있지만, 응답 DTO에는 절대 안 내보낸다
    private int viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt; // 수정 전이면 null
}
