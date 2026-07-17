package com.ebsoft.board.attachment.dto;

import com.ebsoft.board.attachment.domain.Attachment;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 첨부 응답 DTO — 클라이언트에게 내보내는 모양.
 * storedName(서버 내부 저장명)은 노출하지 않는다. 다운로드는 attachmentId로만 한다.
 */
@Getter
public class AttachmentResponse {
    private final Long attachmentId;
    private final Long boardId;
    private final String originalName;
    private final long fileSize;
    private final LocalDateTime createdAt;

    private AttachmentResponse(Attachment a) {
        this.attachmentId = a.getAttachmentId();
        this.boardId = a.getBoardId();
        this.originalName = a.getOriginalName();
        this.fileSize = a.getFileSize();
        this.createdAt = a.getCreatedAt();
    }

    public static AttachmentResponse from(Attachment a) {
        return new AttachmentResponse(a);
    }
}
