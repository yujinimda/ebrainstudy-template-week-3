package com.ebsoft.board.board.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 목록 조회의 검색/페이징 조건.
 * GET /api/boards?keyword=&categoryId=&page=&size= 의 쿼리 파라미터가 여기에 자동 바인딩된다.
 * (Spring이 같은 이름의 파라미터를 이 객체의 필드에 채워줌)
 */
@Getter
@Setter
public class BoardSearchRequest {
    private String keyword;       // 제목/내용 검색어 (없으면 전체)
    private Integer categoryId;   // 카테고리 필터 (없으면 전체)
    private int page = 1;         // 현재 페이지 (1부터), 기본 1
    private int size = 10;        // 페이지당 개수, 기본 10

    private int offset;           // LIMIT ... OFFSET 계산값 — Service가 채워준다
}
