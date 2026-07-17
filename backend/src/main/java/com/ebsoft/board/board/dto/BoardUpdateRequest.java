package com.ebsoft.board.board.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 수정 요청 DTO. #6 등록(BoardCreateRequest)과 검증 규칙은 같지만 역할이 다르다:
 *   - 등록의 password는 새로 "저장"할 값, 수정의 password는 권한 "확인"용 값.
 *   - writer(작성자)는 수정 대상이 아니므로 여기 없다.
 *
 * 제목/내용/카테고리 규칙은 등록과 동일하게 재사용(이슈 #7 요구사항).
 */
@Getter
@Setter
public class BoardUpdateRequest {

    @NotNull
    private Integer categoryId;

    @NotBlank(message = "제목은 필수입니다")
    @Size(min = 4, max = 100, message = "제목은 4~100자여야 합니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    @Size(min = 4, max = 2000, message = "내용은 4~2000자여야 합니다")
    private String content;

    // 수정 권한 확인용 비밀번호. 저장된 "해시"와 비교만 하므로 형식 규칙(@Pattern)은 불필요.
    // "반드시 입력" 만 강제한다.
    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
}
