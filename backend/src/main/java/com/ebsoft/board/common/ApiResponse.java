package com.ebsoft.board.common;

import lombok.Getter;

/**
 * 모든 API가 공유하는 공통 응답 봉투(envelope).
 * <T> : data에 담길 타입 (목록이면 List<Board>, 상세면 Board ... API마다 다름)
 *
 *   성공  ->  { "success": true,  "data": <T>,  "message": null }
 *   실패  ->  { "success": false, "data": null, "message": "..." }
 */
@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;

    // 생성자는 private: 바깥에서는 아래 ok()/fail() 팩토리로만 만들게 강제한다.
    private ApiResponse(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    /**
     * 성공 응답 팩토리 (예시 — 이걸 참고해서 fail을 직접 만드세요).
     * 메서드 앞의 <T> : "이 정적 메서드는 T라는 타입을 받는다"는 제네릭 선언.
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /** 실패 응답 팩토리. data는 없고(null) 사유(message)만 담는다. */
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
