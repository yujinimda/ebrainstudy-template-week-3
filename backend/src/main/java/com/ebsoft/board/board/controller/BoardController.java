package com.ebsoft.board.board.controller;

import com.ebsoft.board.board.dto.BoardCreateRequest;
import com.ebsoft.board.board.dto.BoardResponse;
import com.ebsoft.board.board.dto.BoardSearchRequest;
import com.ebsoft.board.board.service.BoardService;
import com.ebsoft.board.common.ApiResponse;
import com.ebsoft.board.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게시글 API. @RequestMapping("/api/boards")로 이 컨트롤러의 공통 URL을 잡아둔다.
 */
@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    /**
     * 목록 조회: GET /api/boards?keyword=&categoryId=&page=&size=
     * 쿼리 파라미터는 BoardSearchRequest에 자동 바인딩된다(@RequestParam 없이 객체로 받기).
     * 응답은 공통 봉투로 감싼다: { success, data: { content, page, ... }, message }
     */
    @GetMapping
    public ApiResponse<PageResponse<BoardResponse>> getBoards(BoardSearchRequest search) {
        return ApiResponse.ok(boardService.getBoards(search));
    }

    /**
     * 상세 조회: GET /api/boards/{seq}  (예: /api/boards/24)
     * URL 경로의 값을 @PathVariable로 받는다. 조회 시 조회수 +1.
     */
    @GetMapping("/{seq}")
    public ApiResponse<BoardResponse> getBoard(@PathVariable Long seq) {
        BoardResponse board = boardService.getBoard(seq);
        if (board == null) {
            return ApiResponse.fail("게시글을 찾을 수 없습니다: " + seq);
        }
        return ApiResponse.ok(board);
    }

    /**
     * 게시글 등록: POST /api/boards
     * JSON 요청 바디를 BoardCreateRequest로 받고(@RequestBody), @Valid로 검증한다.
     * 새 자원을 만들었으므로 200 OK가 아니라 201 Created로 응답한다.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createBoard(@Valid @RequestBody BoardCreateRequest request) {
        Long newId = boardService.createBoard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(newId));
    }
}
