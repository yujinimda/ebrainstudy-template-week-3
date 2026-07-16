package com.ebsoft.board.board.mapper;

import com.ebsoft.board.board.domain.Board;
import com.ebsoft.board.board.dto.BoardSearchRequest;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 게시글 매퍼. 실제 SQL은 resources/mapper/BoardMapper.xml 에 있다.
 * (인터페이스의 메서드 이름 = XML의 <select id="..."> 와 일치해야 연결됨)
 */
@Mapper
public interface BoardMapper {

    // 검색/페이징 조건에 맞는 게시글 목록 (한 페이지 분량)
    List<Board> findBoards(BoardSearchRequest search);

    // 검색 조건에 맞는 전체 개수 (페이징의 totalElements 용)
    long countBoards(BoardSearchRequest search);
}
