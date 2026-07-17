package com.ebsoft.board.board.controller;

import com.ebsoft.board.board.dto.BoardCreateRequest;
import com.ebsoft.board.board.dto.BoardDeleteRequest;
import com.ebsoft.board.board.dto.BoardResponse;
import com.ebsoft.board.board.dto.BoardSearchRequest;
import com.ebsoft.board.board.dto.BoardUpdateRequest;
import com.ebsoft.board.board.service.BoardService;
import com.ebsoft.board.common.ApiResponse;
import com.ebsoft.board.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    /**
     * 게시글 수정: PUT /api/boards/{seq}
     * PUT = "그 자원을 이 내용으로 바꿔라"(멱등: 같은 요청 여러 번 보내도 결과 동일).
     * 서비스가 돌려준 결과(UpdateResult)에 따라 상태코드를 다르게 내려준다:
     *   - SUCCESS        → 200 OK        (수정됨)
     *   - NOT_FOUND      → 404 Not Found (그런 글 없음)
     *   - WRONG_PASSWORD → 403 Forbidden (글은 있으나 비번 불일치 = 권한 없음)
     * (검증 실패는 @Valid가 진입 전에 400으로 막는다. 이 응답들 다듬기는 #11에서 공통화)
     */
    @PutMapping("/{seq}")
    public ResponseEntity<ApiResponse<Void>> updateBoard(
            @PathVariable Long seq,
            @Valid @RequestBody BoardUpdateRequest request) {
        BoardService.UpdateResult result = boardService.updateBoard(seq, request);
        if (result == BoardService.UpdateResult.NOT_FOUND) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("게시글을 찾을 수 없습니다: " + seq));
        }
        if (result == BoardService.UpdateResult.WRONG_PASSWORD) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail("비밀번호가 일치하지 않습니다."));
        }
        return ResponseEntity.ok(ApiResponse.ok(null));  // 200 OK
    }

    /**
     * 게시글 삭제: DELETE /api/boards/{seq}
     * 비밀번호를 바디로 받아 확인한 뒤, 글과 연관 데이터를 함께 삭제한다.
     * 결과 → 상태코드: SUCCESS 200 / NOT_FOUND 404 / WRONG_PASSWORD 403.
     *
     * (200 vs 204: 204 No Content면 바디가 없다. 여기선 다른 API와 같은 공통 봉투를
     *  내려주려고 200 + ApiResponse 를 쓴다.)
     */
    @DeleteMapping("/{seq}")
    public ResponseEntity<ApiResponse<Void>> deleteBoard(
            @PathVariable Long seq,
            @Valid @RequestBody BoardDeleteRequest request) {
        BoardService.DeleteResult result = boardService.deleteBoard(seq, request);
        if (result == BoardService.DeleteResult.NOT_FOUND) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("게시글을 찾을 수 없습니다: " + seq));
        }
        if (result == BoardService.DeleteResult.WRONG_PASSWORD) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail("비밀번호가 일치하지 않습니다."));
        }
        return ResponseEntity.ok(ApiResponse.ok(null));  // 200 OK
    }
}
