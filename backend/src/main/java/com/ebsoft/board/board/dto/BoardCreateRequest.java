package com.ebsoft.board.board.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 요청 DTO — 클라이언트가 "글 등록" 시 보내오는 모양.
 * 응답 DTO(BoardResponse)와 필드가 다르다:
 *   - boardId/viewCount/createdAt : 서버가 정하는 값이라 요청에 없음
 *   - password : 등록 땐 받지만, 응답 땐 안 돌려줌
 * (검증 애노테이션 @NotBlank 등은 등록 API 이슈 #6에서 붙인다)
 */
@Getter
@Setter
public class BoardCreateRequest {
    private Integer categoryId;
    private String title;
    private String content;
    private String writer;
    private String password;
}
