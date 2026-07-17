package com.ebsoft.board.attachment.mapper;

import com.ebsoft.board.attachment.domain.Attachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 첨부 매퍼. 실제 SQL은 resources/mapper/AttachmentMapper.xml 에 있다.
 */
@Mapper
public interface AttachmentMapper {

    // 첨부 메타 INSERT. useGeneratedKeys 로 새 attachment_id 를 객체에 채워준다.
    int insertAttachment(Attachment attachment);

    // 첨부 1건 조회 (다운로드 시 storedName/originalName 확보용). 없으면 null.
    Attachment findById(@Param("attachmentId") Long attachmentId);

    // 특정 글의 첨부 목록.
    List<Attachment> findByBoardId(@Param("boardId") Long boardId);
}
