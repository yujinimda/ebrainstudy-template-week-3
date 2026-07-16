package com.ebsoft.board.board.dto;

import com.ebsoft.board.board.domain.Board;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 응답 DTO — 클라이언트에게 "내보내는" 게시글 모양.
 * 도메인(Board)과 거의 같지만 password는 뺀다(민감정보는 절대 응답에 넣지 않는다).
 * LocalDateTime은 Jackson이 기본으로 ISO-8601 문자열("2026-07-01T10:00:00")로 직렬화한다.
 */
@Getter
public class BoardResponse {
    private final Long boardId;
    private final Integer categoryId;
    private final String title;
    private final String content;
    private final String writer;
    private final int viewCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private BoardResponse(Board b) {
        this.boardId = b.getBoardId();
        this.categoryId = b.getCategoryId();
        this.title = b.getTitle();
        this.content = b.getContent();
        this.writer = b.getWriter();
        this.viewCount = b.getViewCount();
        this.createdAt = b.getCreatedAt();
        this.updatedAt = b.getUpdatedAt();
    }

    // 도메인 -> 응답 DTO 변환 (컨트롤러에서 BoardResponse.from(board) 로 사용)
    public static BoardResponse from(Board board) {
        return new BoardResponse(board);
    }
}
