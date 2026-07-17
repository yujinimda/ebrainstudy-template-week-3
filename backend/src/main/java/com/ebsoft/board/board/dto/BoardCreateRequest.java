package com.ebsoft.board.board.dto;

// 검증 애노테이션 모음 (jakarta.validation.constraints). *로 한 번에 열어둔다.
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 요청 DTO — 클라이언트가 "글 등록" 시 보내오는 모양.
 * 응답 DTO(BoardResponse)와 필드가 다르다:
 *   - boardId/viewCount/createdAt : 서버가 정하는 값이라 요청에 없음
 *   - password : 등록 땐 받지만, 응답 땐 안 돌려줌
 *
 * 컨트롤러에서 @Valid 로 이 DTO를 받으면, 아래 필드에 붙인 애노테이션 규칙을
 * 스프링이 자동으로 검사한다. 하나라도 어기면 컨트롤러 진입 전에 예외 발생.
 */
@Getter
@Setter
public class BoardCreateRequest {

    // ── TODO(human): 각 필드에 검증 애노테이션을 붙이세요. ──────────────────────
    //
    //  규칙(이슈 #6):
    //    writer   작성자 : 3~5자,   비어있으면 안 됨
    //    password 비번   : 4~16자,  영문/숫자/특수문자 포함 (형식은 @Pattern)
    //    title    제목   : 4~100자, 비어있으면 안 됨
    //    content  내용   : 4~2000자, 비어있으면 안 됨
    //    categoryId      : 필수(null 금지)
    //
    //  쓸 애노테이션(필드 바로 윗줄에):
    //    @NotBlank(message = "...")            문자열이 null/빈문자/공백만 이면 실패
    //    @Size(min = , max = , message = "...") 문자열 길이 범위
    //    @Pattern(regexp = "...", message="")  정규식 형식 (비번 구성규칙에)
    //    @NotNull(message = "...")             숫자/객체가 null 이면 실패 (categoryId 용)
    //
    //  예시(이 형태로 각 필드 위에 붙이면 됨):
    //      @NotBlank(message = "작성자는 필수입니다")
    //      @Size(min = 3, max = 5, message = "작성자는 3~5자여야 합니다")
    //      private String writer;
    //
    //  힌트: @NotBlank 는 String 전용(공백 검사 O). categoryId(Integer)는 @NotNull 사용.
    //        @Size 는 null 은 통과시키므로 @NotBlank 와 함께 써야 "필수 + 길이" 둘 다 됨.
    // ────────────────────────────────────────────────────────────────────────

    @NotNull
    private Integer categoryId;

    @NotBlank(message="제목은 필수입니다")
    @Size(min = 4, max = 100, message="제목은 4~100자여야 합니다")
    private String title;

    @NotBlank(message="글은 필수입니다")
    @Size(min = 4, max = 2000, message="글은 4~2000자여야 합니다")
    private String content;

    @NotBlank(message="작성자는 필수입니다.")
    @Size(min = 3, max = 5, message="작성자는 3~5자여야 합니다")
    private String writer;

    @NotBlank
    @Size(min = 4, max = 16, message="비밀번호는 4~16자여야 합니다")
    @Pattern(
      regexp = "(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*]).+",
      message = "비밀번호는 영문/숫자/특수문자를 모두 포함해야 합니다"
    )
    private String password;
}
