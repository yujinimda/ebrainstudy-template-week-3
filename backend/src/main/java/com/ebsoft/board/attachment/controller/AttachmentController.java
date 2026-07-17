package com.ebsoft.board.attachment.controller;

import com.ebsoft.board.attachment.dto.AttachmentResponse;
import com.ebsoft.board.attachment.service.AttachmentService;
import com.ebsoft.board.common.ApiResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 첨부 API. 업로드/목록은 글 하위 경로, 다운로드는 파일 단위 경로.
 * (클래스 공통 @RequestMapping 없이 메서드마다 전체 경로를 적는다 — 두 경로가 계열이 달라서)
 */
@RestController
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    /**
     * 업로드: POST /api/boards/{seq}/files  (multipart/form-data, 필드명 file)
     * JSON이 아니라 파일이라 @RequestBody가 아닌 @RequestParam MultipartFile 로 받는다.
     * (파일 + JSON 메타를 함께 받아야 하면 @RequestPart 를 쓴다 — 여기선 파일만.)
     */
    @PostMapping("/api/boards/{seq}/files")
    public ResponseEntity<ApiResponse<AttachmentResponse>> upload(
            @PathVariable Long seq,
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("업로드할 파일이 없습니다."));
        }
        AttachmentResponse res = attachmentService.upload(seq, file);
        if (res == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("게시글을 찾을 수 없습니다: " + seq));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(res));
    }

    /** 특정 글의 첨부 목록: GET /api/boards/{seq}/files */
    @GetMapping("/api/boards/{seq}/files")
    public ApiResponse<List<AttachmentResponse>> list(@PathVariable Long seq) {
        return ApiResponse.ok(attachmentService.listByBoard(seq));
    }

    /**
     * 다운로드: GET /api/files/{id}/download
     * JSON 봉투가 아니라 "바이너리"를 그대로 내려준다 → ResponseEntity<Resource>.
     * Content-Disposition: attachment; filename=... 헤더로 "원본 파일명으로 저장"되게 한다.
     * (URL 직링크가 아니라 이 API를 통해 내려받는다 — 요구사항)
     */
    @GetMapping("/api/files/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        AttachmentService.DownloadFile df = attachmentService.loadForDownload(id);
        if (df == null) {
            return ResponseEntity.notFound().build();
        }
        // 한글 파일명도 안전하게(UTF-8 인코딩) 내려주기
        ContentDisposition cd = ContentDisposition.attachment()
                .filename(df.originalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)  // "바이너리 데이터" 라는 표시
                .body(df.resource());
    }
}
