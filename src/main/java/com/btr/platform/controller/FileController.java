package com.btr.platform.controller;

import com.btr.platform.common.ApiResponse;
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

    @GetMapping("/{id}")
    public ResponseEntity<Resource> preview(@PathVariable Long id) {
        UploadFile file = fileService.getOrThrow(id);
        Resource resource = new FileSystemResource(fileService.pathOf(file));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.parseMediaType(fileService.contentType(file)))
                .body(resource);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        UploadFile file = fileService.getOrThrow(id);
        Resource resource = new FileSystemResource(fileService.pathOf(file));
        String name = URLEncoder.encode(file.getOriginalName() == null ? "download" : file.getOriginalName(),
                StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + name)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
