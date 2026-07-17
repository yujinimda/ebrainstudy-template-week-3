package com.ebsoft.board.board.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 삭제 요청 DTO — 비밀번호 하나만 받는다(권한 확인용).
 * DELETE에 바디를 싣는 건 흔치 않지만, 비번을 URL 쿼리(?password=)로 노출하지 않으려고
 * 바디(@RequestBody)로 받는다. (로그/브라우저 히스토리에 비번이 남지 않게)
 */
@Getter
@Setter
public class BoardDeleteRequest {

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
}
