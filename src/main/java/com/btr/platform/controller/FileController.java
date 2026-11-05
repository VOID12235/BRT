package com.btr.platform.controller;

import com.btr.platform.common.ApiResponse;
import com.btr.platform.common.BizException;
import com.btr.platform.dto.Views.UploadResult;
import com.btr.platform.entity.UploadFile;
import com.btr.platform.security.SecurityUtils;
import com.btr.platform.service.FileService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping
    public ApiResponse<UploadResult> upload(@RequestPart("file") MultipartFile file,
                                            @RequestParam(defaultValue = "work") String purpose) {
        return ApiResponse.ok(fileService.upload(file, SecurityUtils.current().id(), purpose));
    }

    /**
     * 带内容指纹的访问地址：id 连续可枚举，因此必须校验 URL 中的指纹（md5），
     * 相当于一个不可猜的能力令牌，避免未公示作品文件被遍历下载。
     */
    @GetMapping("/{id}/{token}")
    public ResponseEntity<Resource> preview(@PathVariable Long id, @PathVariable String token) {
        return serve(verify(id, token), false);
    }

    @GetMapping("/{id}/{token}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, @PathVariable String token) {
        return serve(verify(id, token), true);
    }

    /** 兼容旧数据：仅当记录本身仍使用旧的 /files/{id} 地址时放行。 */
    @GetMapping("/{id}")
    public ResponseEntity<Resource> legacyPreview(@PathVariable Long id) {
        UploadFile file = fileService.getOrThrow(id);
        if (!("/api/v1/files/" + id).equals(file.getUrl())) {
            throw new BizException(404, "文件不存在");
        }
        return serve(file, false);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> legacyDownload(@PathVariable Long id) {
        UploadFile file = fileService.getOrThrow(id);
        if (!("/api/v1/files/" + id).equals(file.getUrl())) {
            throw new BizException(404, "文件不存在");
        }
        return serve(file, true);
    }

    private UploadFile verify(Long id, String token) {
        UploadFile file = fileService.getOrThrow(id);
        if (file.getMd5() == null || !file.getMd5().equalsIgnoreCase(token)) {
            throw new BizException(404, "文件不存在");
        }
        return file;
    }

    private ResponseEntity<Resource> serve(UploadFile file, boolean forceDownload) {
        Resource resource = new FileSystemResource(fileService.pathOf(file));
        boolean inline = !forceDownload && fileService.inlineSafe(file);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, inline ? "inline" : attachment(file))
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .contentType(forceDownload
                        ? MediaType.APPLICATION_OCTET_STREAM
                        : MediaType.parseMediaType(fileService.contentType(file)));
        if (!inline) {
            // 非预览类型强制下载，并加上沙箱 CSP，彻底阻断 SVG/HTML 存储型 XSS
            builder.header("Content-Security-Policy", "default-src 'none'; sandbox");
        }
        return builder.body(resource);
    }

    private String attachment(UploadFile file) {
        String name = URLEncoder.encode(file.getOriginalName() == null ? "download" : file.getOriginalName(),
                StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename*=UTF-8''" + name;
    }
}
