# Issue 10 — 파일 업로드/다운로드 API (multipart + 바이너리)

> 관련 PR: (작성 예정) · 참고: MultipartFile, java.nio.file.Files, ResponseEntity<Resource>

## 무엇을 만드나
게시글에 파일을 첨부(업로드)하고 내려받는(다운로드) API. 파일 자체는 디스크(`upload/`)에 고유명으로 저장하고, DB(attachment)엔 메타데이터만 기록한다. 다운로드는 원본 파일명으로 바이너리를 내려준다.

## 알아야 할 개념

### 1. multipart vs 청크(chunk) 업로드 (오늘 정리)
- **multipart/form-data**: 파일을 HTTP 요청으로 보내는 기본 형식. `@RequestParam MultipartFile`로 받음(JSON 아니라 `@RequestBody` 아님).
- **청크 업로드**: 그 요청을 **여러 번** 보내도록 직접 설계한 고급 방식. **프론트가 파일을 잘라(`Blob.slice`) 여러 번 전송 → 서버가 합침**. 중간에 끊겨도 받은 조각부터 이어서 재전송 가능(대용량/영상). → 자세한 건 [[issue-28-large-file-upload]].
- 우리 게시판(10MB)은 기본 multipart로 충분.

### 2. 저장 전략: 원본명 vs 저장명
| | 값 예시 | 용도 |
|---|---|---|
| originalName | `report.pdf` | 사용자가 올린 원래 이름, **다운로드 시 보여줄 이름** (DB) |
| storedName | `a72c...-report.pdf` | 서버 디스크에 실제 저장한 **고유 이름** (사용자에 노출 X) |
- 왜 나누나: A와 B가 둘 다 `report.pdf`를 올리면 원래 이름 그대로 저장 시 **덮어써짐**. 앞에 `UUID`를 붙여 충돌 방지.
- `stored_name`은 DB에서도 UNIQUE — 만일의 중복도 DB가 막아줌.

### 3. 업로드 구현 5단계 (Service.upload)
```java
// 1) 원본 파일명 + 경로 섞임 방지
String original = Paths.get(file.getOriginalFilename()).getFileName().toString();
// 2) 고유 저장명
String storedName = UUID.randomUUID() + "-" + original;
// 3) 폴더 준비 + 디스크에 스트리밍 저장
Path dir = Paths.get(uploadDir);
try {
    Files.createDirectories(dir);              // 폴더 없으면 생성
    Path target = dir.resolve(storedName);     // ./upload/UUID-원본명
    Files.copy(file.getInputStream(), target); // 통로로 흘려담기
} catch (java.io.IOException e) { throw new RuntimeException(e); }
// 4) 메타 INSERT (파일은 디스크, 정보는 DB)
Attachment a = new Attachment();
a.setBoardId(boardId); a.setOriginalName(original);
a.setStoredName(storedName); a.setFileSize(file.getSize());
attachmentMapper.insertAttachment(a);          // 후 a.getAttachmentId() 채워짐
// 5) 도메인 → 응답 DTO 변환(storedName 제외)
return AttachmentResponse.from(a);
```

#### 왜 `getOriginalFilename()`만 쓰면 안 되나 (오늘 새로 안 것)
- 업로드 파일명은 **사용자가 준 값**이라 믿으면 안 됨. 브라우저/악의적 요청이 경로를 섞어 보낼 수 있음: `C:\...\report.pdf`, `../../중요파일.txt`.
- `Paths.get(original).getFileName().toString()` = 경로를 버리고 **마지막 파일명 부분만** 뽑음 → 항상 `report.pdf` 모양만 남김(안전).

#### `Files.createDirectories` / `resolve` / `Files.copy` (오늘 새로 안 것)
- `Paths.get(uploadDir)` : `"./upload"` 문자열을 Java가 다룰 수 있는 `Path`로.
- `Files.createDirectories(dir)` : 폴더 없으면 생성, 있으면 통과(첫 업로드에도 에러 X).
- `dir.resolve(storedName)` : 폴더 + 파일명을 합쳐 최종 경로(`./upload/UUID-원본명`).
- `Files.copy(file.getInputStream(), target)` : 업로드 내용을 **읽는 통로(InputStream)**에서 흘려 target에 새 파일로 저장. `getInputStream`은 파일을 통째로 메모리에 안 올려 대용량 안전. → [[issue-28-large-file-upload]]

### 4. checked 예외(IOException)는 왜 try/catch? (오늘 새로 안 것)
- 파일 저장은 **디스크 부족/권한 없음/경로 문제** 등으로 실패할 수 있음 = `IOException`.
- Java 예외 두 종류:
  - **RuntimeException**(unchecked): 처리 강제 안 함.
  - **checked exception**(IOException 등): "일어날 수 있으니 **반드시 처리**하라"고 컴파일러가 강제 → try/catch 또는 `throws`.
- `Files.copy`/`createDirectories`의 메서드 선언에 `throws IOException`이 붙어 있어서, 이걸 쓰면 컴파일러가 처리를 요구함. (Java가 "파일 저장"이라는 뜻을 이해하는 게 아니라, 메서드 시그니처의 `throws`를 보고 아는 것.)
- 여기선 `catch (IOException e) { throw new RuntimeException(e); }` — 서비스 안에서 런타임 예외로 바꿔, 컨트롤러까지 IOException을 전파하지 않게 함.

### 5. 다운로드 = 바이너리 응답 (JSON 봉투 아님)
```java
ContentDisposition cd = ContentDisposition.attachment()
        .filename(originalName, StandardCharsets.UTF_8).build();  // 한글명 안전
return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
        .contentType(MediaType.APPLICATION_OCTET_STREAM)          // "바이너리"
        .body(resource);   // FileSystemResource — 디스크 파일
```
- 조회/등록과 달리 `ApiResponse`(JSON 봉투)로 안 감쌈. 파일 그 자체를 내려주므로 `ResponseEntity<Resource>`.
- `Content-Disposition: attachment; filename=...` → 브라우저가 **원본 이름으로 저장**하게 함. URL 직링크가 아니라 이 API를 통해 받음(요구사항).

### 6. multipart 설정 (application.yml)
```yaml
spring.servlet.multipart:
  max-file-size: 10MB      # 파일 하나 최대
  max-request-size: 20MB   # 요청 전체 최대
app.upload-dir: ./upload   # 저장 폴더(@Value로 주입, gitignore됨)
```

## 실행 테스트로 확인한 것
- 업로드 201(디스크에 `UUID-원본명` 저장) / 목록 조회 / 다운로드 200(원본명 헤더 + 내용 일치) / 없는 파일·글 404.

## 고민 / 함정
- **파일 저장 성공 + DB INSERT 실패 시**: 디스크에 파일만 남는 고아 파일 발생 가능. 지금은 흐름 이해에 집중, 실무는 `@Transactional` + 실패 시 파일 삭제로 보완.
- **삭제(#8)와의 연계**: board 삭제 시 attachment **메타(DB)**만 지우고 **디스크 파일**은 안 지움 → 개선 포인트(고아 파일 청소).
- 파일명은 사용자 입력 → 경로 정리 필수(2번).

## 회고
- 업로드/다운로드를 직접 구현하며 "파일은 디스크, 메타는 DB" 분리와 스트리밍 저장을 체감.
- checked 예외를 왜 여기서 try/catch 하는지(메서드 `throws IOException`), 경로 정리가 왜 필요한지 이해.
- 대용량(청크/S3)은 별도로 정리 → [[issue-28-large-file-upload]].
