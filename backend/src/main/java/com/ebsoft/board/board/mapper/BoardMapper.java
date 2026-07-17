package com.ebsoft.board.board.mapper;

import com.ebsoft.board.board.domain.Board;
import com.ebsoft.board.board.dto.BoardSearchRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    // 게시글 1건 조회 (없으면 null). @Param 으로 XML에서 #{boardId}로 참조.
    Board findById(@Param("boardId") Long boardId);

    // 조회수 +1 (반환값 = 실제 바뀐 행 수)
    int increaseViewCount(@Param("boardId") Long boardId);

    // 게시글 INSERT. Board 객체를 통째로 넘긴다(XML에서 #{title}, #{writer}...로 참조).
    // XML에서 useGeneratedKeys 로 새로 생긴 board_id를 board.boardId 에 다시 채워준다.
    // 반환값 = 삽입된 행 수(보통 1).  SQL 본문은 BoardMapper.xml 의 TODO(human).
    int insertBoard(Board board);
}
