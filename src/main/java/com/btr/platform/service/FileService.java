package com.btr.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btr.platform.common.BizException;
import com.btr.platform.dto.Views.UploadResult;
import com.btr.platform.entity.UploadFile;
import com.btr.platform.mapper.UploadFileMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Set;

@Service
public class FileService {

    private static final Set<String> ALLOWED = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "svg", "pdf", "zip", "rar", "7z",
            "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "md", "mp4", "mov");

    private final UploadFileMapper uploadFileMapper;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    public FileService(UploadFileMapper uploadFileMapper) {
        this.uploadFileMapper = uploadFileMapper;
    }

    @Transactional
    public UploadResult upload(MultipartFile file, Long userId, String purpose) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = extension(original);
        if (!ALLOWED.contains(ext)) {
            throw new BizException("不支持的文件类型：" + ext);
        }
        try (InputStream in = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path tmp = Files.createTempFile(dir, "upload-", ".tmp");
            try (DigestInputStream dis = new DigestInputStream(in, digest)) {
                Files.copy(dis, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            String md5 = HexFormat.of().formatHex(digest.digest());
            UploadFile exists = uploadFileMapper.selectOne(new QueryWrapper<UploadFile>()
                    .eq("md5", md5).last("limit 1"));
            if (exists != null) {
                Files.deleteIfExists(tmp);
                return toResult(exists);
            }
            String filename = md5 + "." + ext;
            Path target = dir.resolve(filename);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);

            UploadFile record = new UploadFile();
            record.setUrl("/api/v1/files/pending");
            record.setMd5(md5);
            record.setSize(file.getSize());
            record.setOriginalName(original);
            record.setUploaderId(userId);
            record.setPurpose(purpose);
            record.setCreatedAt(LocalDateTime.now());
            uploadFileMapper.insert(record);
            record.setUrl("/api/v1/files/" + record.getId());
            uploadFileMapper.updateById(record);

            UploadResult result = toResult(record);
            result.setName(original);
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(500, "文件上传失败：" + e.getMessage());
        }
    }

    public UploadFile getOrThrow(Long id) {
        UploadFile file = uploadFileMapper.selectById(id);
        if (file == null) {
            throw new BizException(404, "文件不存在");
        }
        return file;
    }

    public Path pathOf(UploadFile file) {
        String name = file.getMd5() + "." + extension(file.getOriginalName() == null ? "" : file.getOriginalName());
        Path path = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(name).normalize();
        if (!path.startsWith(Paths.get(uploadDir).toAbsolutePath().normalize()) || !Files.exists(path)) {
            throw new BizException(404, "文件已丢失");
        }
        return path;
    }

    public String contentType(UploadFile file) {
        try {
            return Files.probeContentType(pathOf(file));
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    private UploadResult toResult(UploadFile file) {
        UploadResult result = new UploadResult();
        result.setId(file.getId());
        result.setUrl(file.getUrl());
        result.setName(file.getOriginalName());
        result.setSize(file.getSize() == null ? 0 : file.getSize());
        return result;
    }

    private String extension(String name) {
        int index = name.lastIndexOf('.');
        return index < 0 ? "" : name.substring(index + 1).toLowerCase();
    }
}
