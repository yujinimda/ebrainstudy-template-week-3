package com.ebsoft.board.board.service;

import com.ebsoft.board.board.domain.Board;
import com.ebsoft.board.board.dto.BoardCreateRequest;
import com.ebsoft.board.board.dto.BoardResponse;
import com.ebsoft.board.board.dto.BoardSearchRequest;
import com.ebsoft.board.board.dto.BoardUpdateRequest;
import com.ebsoft.board.board.mapper.BoardMapper;
import com.ebsoft.board.common.PageResponse;
import org.springframework.stereotype.Service;

import java.util.List;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

/**
 * 게시글 비즈니스 로직. 컨트롤러와 매퍼 사이에서
 * 페이지 계산(offset/totalPages) + 도메인 -> 응답 DTO 변환을 담당한다.
 */
@Service
public class BoardService {

    private final BoardMapper boardMapper;

    public BoardService(BoardMapper boardMapper) {
        this.boardMapper = boardMapper;
    }

    public PageResponse<BoardResponse> getBoards(BoardSearchRequest search) {
        // 1) offset 계산 후 조건에 세팅 (page 3, size 10 -> offset 20)
        search.setOffset((search.getPage() - 1) * search.getSize());

        // 2) 전체 개수 + 이번 페이지 목록 조회
        long totalElements = boardMapper.countBoards(search);
        List<Board> boards = boardMapper.findBoards(search);

        // 3) 도메인 -> 응답 DTO 변환 (password 제거된 모양으로)
        List<BoardResponse> content = boards.stream()
                .map(BoardResponse::from)
                .toList();

        // 4) 전체 페이지 수 = ceil(전체개수 / 페이지크기)
        int totalPages = (int) Math.ceil((double) totalElements / search.getSize());

        return new PageResponse<>(content, search.getPage(), search.getSize(), totalElements, totalPages);
    }

    /**
     * 상세 조회 + 조회수 증가.
     * 먼저 +1 하고 조회하므로, 응답에는 증가된 조회수가 담긴다.
     * 없는 글이면 null 반환(컨트롤러가 실패 응답으로 처리; 제대로 된 404는 #11).
     */
    public BoardResponse getBoard(Long boardId) {
        boardMapper.increaseViewCount(boardId);          // 조회수 +1 (없는 id면 0행, 무해)
        Board board = boardMapper.findById(boardId);     // 증가된 상태로 1건 조회
        if (board == null) {
            return null;
        }
        return BoardResponse.from(board);
    }

    /**
     * 게시글 등록. 검증은 컨트롤러(@Valid)에서 이미 끝난 상태로 들어온다.
     * 여기서는 (1) 비번 해시 (2) 요청→도메인 변환 (3) INSERT (4) 새 글번호 반환 을 한다.
     *
     * @return 새로 생성된 게시글의 boardId
     */
    public Long createBoard(BoardCreateRequest request) {
        // TODO(human): 아래 4단계를 직접 구현하세요.
        //
        //  1) 비밀번호 해시 (평문 저장 금지!)
        //     - request.getPassword() 를 SHA-256 으로 해시한 문자열을 만든다.
        //     - 자바 표준: java.security.MessageDigest.getInstance("SHA-256")
        //         byte[] digest = md.digest(원문.getBytes(StandardCharsets.UTF_8));
        //       digest(byte[]) 를 16진수 문자열로 바꿔야 DB(VARCHAR)에 넣기 좋다.
        //       (hex 변환은 별도 private 헬퍼 메서드로 빼면 깔끔 — 예: String sha256(String raw))
        //     - MessageDigest.getInstance 는 checked 예외(NoSuchAlgorithmException)를 던진다.
        //       "SHA-256"은 항상 존재하므로 try/catch로 감싸 RuntimeException으로 바꿔도 된다.
        //
        //  2) 요청 DTO → Board 도메인 변환
        //     - new Board() 후 categoryId/title/content/writer 를 request 값으로 세팅
        //     - password 에는 (1)에서 만든 "해시값"을 세팅 (원문 아님!)
        //     - board_id/view_count/created_at 은 세팅하지 않는다 (DB가 채움)
        //
        //  3) INSERT 실행
        //     - boardMapper.insertBoard(board) 호출
        //     - useGeneratedKeys 덕분에 이 호출 후 board.getBoardId() 에 새 번호가 들어있다.
        //
        //  4) 새 글번호 반환
        //     - return board.getBoardId();

        String hashed = sha256(request.getPassword());

        Board board = new Board();
        board.setCategoryId(request.getCategoryId());
        board.setTitle(request.getTitle());
        board.setContent(request.getContent());
        board.setWriter(request.getWriter());
        board.setPassword(hashed); 

        boardMapper.insertBoard(board);

        return board.getBoardId();
    }

    /**
     * 수정 결과 신호. 컨트롤러가 이 값을 보고 HTTP 상태코드를 고른다.
     * (지금은 이 방식으로 실패를 구분하고, 제대로 된 예외→상태코드 매핑은 #11에서 공통화)
     */
    public enum UpdateResult { SUCCESS, NOT_FOUND, WRONG_PASSWORD }

    /**
     * 게시글 수정. 검증(@Valid)은 컨트롤러에서 이미 끝났다.
     * 흐름: (1) 대상 글 조회 (2) 비밀번호 확인 (3) 값 갱신.
     *
     * @return SUCCESS / NOT_FOUND(글 없음) / WRONG_PASSWORD(비번 불일치)
     */
    public UpdateResult updateBoard(Long boardId, BoardUpdateRequest request) {
        // 1) 대상 글 조회 — 없으면 수정할 게 없다
        Board board = boardMapper.findById(boardId);
        if (board == null) {
            return UpdateResult.NOT_FOUND;
        }

        // 2) 비밀번호 확인 — 입력 비번을 같은 방식(SHA-256)으로 해시해 저장된 해시와 비교
        if (!sha256(request.getPassword()).equals(board.getPassword())) {
            return UpdateResult.WRONG_PASSWORD;
        }

        // 3) 값 갱신 — password/writer는 그대로 두고 나머지만 교체 (updated_at은 XML의 NOW()가 채움)
        board.setCategoryId(request.getCategoryId());
        board.setTitle(request.getTitle());
        board.setContent(request.getContent());
        boardMapper.updateBoard(board);
        return UpdateResult.SUCCESS;

    }

    private String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));  // 바이트 → 16진수 2자리
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
