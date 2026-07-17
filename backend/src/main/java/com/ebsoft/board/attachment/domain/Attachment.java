package com.ebsoft.board.attachment.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 첨부파일 도메인 — DB의 attachment 테이블 한 행과 1:1.
 * 파일 자체는 디스크에 저장하고, 여기엔 "메타데이터"만 둔다.
 *   originalName : 사용자가 올린 원래 이름(화면 표시 / 다운로드 파일명)
 *   storedName   : 서버에 실제 저장한 고유 이름(충돌 방지). 응답에는 내보내지 않는다.
 */
@Getter
@Setter
public class Attachment {
    private Long attachmentId;   // PK
    private Long boardId;        // FK → board
    private String originalName; // 원본 파일명
    private String storedName;   // 서버 저장 파일명 (UNIQUE)
    private long fileSize;       // 파일 크기(byte)
    private LocalDateTime createdAt;
}
