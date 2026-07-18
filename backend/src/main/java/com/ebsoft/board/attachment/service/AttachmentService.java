package com.ebsoft.board.attachment.service;

import com.ebsoft.board.attachment.domain.Attachment;
import com.ebsoft.board.attachment.dto.AttachmentResponse;
import com.ebsoft.board.attachment.mapper.AttachmentMapper;
import com.ebsoft.board.board.mapper.BoardMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * 첨부 업로드/다운로드 비즈니스 로직.
 * 파일 자체는 디스크(app.upload-dir)에 "고유 이름"으로 저장하고, DB엔 메타만 기록한다.
 */
@Service
public class AttachmentService {

    private final AttachmentMapper attachmentMapper;
    private final BoardMapper boardMapper;      // 대상 글 존재 확인용
    private final String uploadDir;

    public AttachmentService(AttachmentMapper attachmentMapper,
                             BoardMapper boardMapper,
                             @Value("${app.upload-dir}") String uploadDir) {
        this.attachmentMapper = attachmentMapper;
        this.boardMapper = boardMapper;
        this.uploadDir = uploadDir;
    }

    /**
     * 업로드: 파일을 디스크에 고유명으로 저장하고 메타를 DB에 기록한다.
     * @return 저장된 첨부 정보. 대상 글이 없으면 null(컨트롤러가 404 처리).
     */
    public AttachmentResponse upload(Long boardId, MultipartFile file) {
        // 대상 글 존재 확인 — 없는 글엔 첨부할 수 없다(FK)
        if (boardMapper.findById(boardId) == null) {
            return null;
        }

        // 1) 원본 파일명 확보 + 경로 섞임(../ 등) 방지: 파일명 부분만 남긴다
        String original = Paths.get(file.getOriginalFilename()).getFileName().toString();

        // 2) 고유 저장명 (UUID를 앞에 붙여 디스크에서 이름 충돌 방지 — stored_name은 UNIQUE)
        String storedName = UUID.randomUUID() + "-" + original;

        // 3) 업로드 폴더 준비 후 디스크에 스트리밍 저장 (파일 IO라 IOException 처리 필요)
        Path dir = Paths.get(uploadDir);
        try {
            Files.createDirectories(dir);                 // 폴더 없으면 생성
            Path target = dir.resolve(storedName);        // ./upload/UUID-원본명
            Files.copy(file.getInputStream(), target);    // 통로로 흘려담기(대용량 안전)
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }

        // 4) 메타 INSERT (파일은 디스크에, 정보는 DB에)
        Attachment a = new Attachment();
        a.setBoardId(boardId);
        a.setOriginalName(original);
        a.setStoredName(storedName);
        a.setFileSize(file.getSize());
        attachmentMapper.insertAttachment(a);   // 후 a.getAttachmentId() 채워짐

        // 5) 도메인 → 응답 DTO(storedName 제외) 변환해 반환
        return AttachmentResponse.from(a);
    }

    /**
     * 다운로드용: 메타 + 실제 파일 리소스를 함께 돌려준다.
     * 첨부가 없거나 디스크에 파일이 없으면 null.
     */
    public DownloadFile loadForDownload(Long attachmentId) {
        Attachment a = attachmentMapper.findById(attachmentId);
        if (a == null) {
            return null;
        }
        Path path = Paths.get(uploadDir).resolve(a.getStoredName());
        Resource resource = new FileSystemResource(path);
        if (!resource.exists()) {
            return null;   // DB엔 메타가 있는데 실제 파일이 사라진 경우
        }
        return new DownloadFile(a.getOriginalName(), resource);
    }

    /**
     * 삭제: DB 메타 행 + 디스크 실제 파일을 함께 지운다.
     * @return true = 삭제됨, false = 해당 첨부 없음(컨트롤러가 404 처리)
     */
    public boolean delete(Long attachmentId) {
        // 순서가 핵심: 파일 삭제는 트랜잭션 밖(롤백 불가)이라
        // "실패해도 덜 위험한" DB 삭제를 먼저, 파일 삭제를 나중에 한다.

        // 1) 조회 — 파일명(storedName) 확보가 목적. 행을 지우기 전에 해야 한다
        Attachment a = attachmentMapper.findById(attachmentId);
        if (a == null) {
            return false;   // boolean 메서드라 null 불가 — "없음" 신호는 false
        }

        // 2) DB 행부터 삭제 — 실패해도 롤백 가능한 쪽을 먼저(Q1)
        attachmentMapper.deleteById(attachmentId);

        // 3) 디스크 파일 삭제 — 실패하면 "고아 파일"이 남지만 사용자에겐 무해
        Path path = Paths.get(uploadDir).resolve(a.getStoredName());
        try {
            Files.deleteIfExists(path);   // 파일이 이미 없어도 예외 없이 지나감
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }

        // 4) 여기까지 왔으면 성공
        return true;
    }

    /** 특정 글의 첨부 목록. */
    public List<AttachmentResponse> listByBoard(Long boardId) {
        return attachmentMapper.findByBoardId(boardId).stream()
                .map(AttachmentResponse::from)
                .toList();
    }

    /** 다운로드에 필요한 두 가지(원본 파일명 + 파일 리소스)를 묶은 값. */
    public record DownloadFile(String originalName, Resource resource) {}
}
