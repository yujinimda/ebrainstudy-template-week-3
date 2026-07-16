package com.ebsoft.board.board.service;

import com.ebsoft.board.board.domain.Board;
import com.ebsoft.board.board.dto.BoardResponse;
import com.ebsoft.board.board.dto.BoardSearchRequest;
import com.ebsoft.board.board.mapper.BoardMapper;
import com.ebsoft.board.common.PageResponse;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
