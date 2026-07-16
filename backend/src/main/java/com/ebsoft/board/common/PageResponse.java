package com.ebsoft.board.common;

import lombok.Getter;

import java.util.List;

/**
 * 목록 응답 규격 — 데이터 배열(content) + 페이지 정보.
 * ApiResponse의 data 자리에 이게 들어간다: ApiResponse<PageResponse<BoardResponse>>
 */
@Getter
public class PageResponse<T> {
    private final List<T> content;     // 이번 페이지 데이터
    private final int page;            // 현재 페이지 (1부터)
    private final int size;            // 페이지당 개수
    private final long totalElements;  // 조건에 맞는 전체 개수 (count 쿼리 결과)
    private final int totalPages;      // 전체 페이지 수

    public PageResponse(List<T> content, int page, int size, long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }
}
