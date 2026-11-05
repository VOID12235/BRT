package com.btr.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btr.platform.common.BizException;
import com.btr.platform.dto.Views.UploadResult;
import com.btr.platform.entity.UploadFile;
import com.btr.platform.mapper.UploadFileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
public class FileService {

    private static final Set<String> DEFAULT_ALLOWED = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "svg", "pdf", "zip", "rar", "7z",
            "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "md", "mp4", "mov");

    /** 可安全内联预览的类型，其余一律以附件形式下载，避免存储型 XSS。 */
    private static final Set<String> INLINE_TYPES = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "pdf", "mp4", "mov", "txt", "md");

    private final UploadFileMapper uploadFileMapper;
    private final ConfigService configService;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    public FileService(UploadFileMapper uploadFileMapper, ConfigService configService) {
        this.uploadFileMapper = uploadFileMapper;
        this.configService = configService;
    }

    /** 可上传类型白名单：管理后台配置优先，未配置回退默认 */
    private Set<String> allowedTypes() {
        String types = configService.getUpload().getAllowedTypes();
        if (types == null || types.isBlank()) {
            return DEFAULT_ALLOWED;
        }
        Set<String> parsed = java.util.Arrays.stream(types.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toSet());
        return parsed.isEmpty() ? DEFAULT_ALLOWED : parsed;
    }

    @Transactional
    public UploadResult upload(MultipartFile file, Long userId, String purpose) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        int maxSizeMb = Math.max(1, configService.getUpload().getMaxSizeMb());
        long maxBytes = maxSizeMb * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BizException("文件大小不能超过 " + maxSizeMb + "MB");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = extension(original);
        if (!allowedTypes().contains(ext)) {
            throw new BizException("不支持的文件类型：" + ext);
        }
        if (!magicMatches(ext, readHeader(file))) {
            throw new BizException("文件内容与扩展名不符，已拒绝上传");
        }

        Path tmp = null;
        try (InputStream in = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            Path dir = uploadDir();
            Files.createDirectories(dir);
            tmp = Files.createTempFile(dir, "upload-", ".tmp");
            try (DigestInputStream dis = new DigestInputStream(in, digest)) {
                Files.copy(dis, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            String md5 = HexFormat.of().formatHex(digest.digest());
            String filename = md5 + "." + ext;
            Path target = dir.resolve(filename);

            // 相同内容只保留一份物理文件与一条记录（md5 唯一），重复上传直接复用，避免浪费磁盘
            UploadFile existing = uploadFileMapper.selectOne(new QueryWrapper<UploadFile>()
                    .eq("md5", md5).last("limit 1"));
            if (existing != null) {
                if (!Files.exists(target)) {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                }
                UploadResult result = toResult(existing);
                result.setName(original);
                return result;
            }

            if (!Files.exists(target)) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }

            UploadFile record = new UploadFile();
            record.setUrl("/api/v1/files/pending");
            record.setMd5(md5);
            record.setSize(file.getSize());
            record.setOriginalName(original);
            record.setUploaderId(userId);
            record.setPurpose(purpose);
            record.setCreatedAt(LocalDateTime.now());
            try {
                uploadFileMapper.insert(record);
            } catch (org.springframework.dao.DataIntegrityViolationException race) {
                // 并发上传同一文件：另一请求已先写入，直接复用其记录
                UploadFile concurrent = uploadFileMapper.selectOne(new QueryWrapper<UploadFile>()
                        .eq("md5", md5).last("limit 1"));
                if (concurrent == null) {
                    throw race;
                }
                UploadResult result = toResult(concurrent);
                result.setName(original);
                return result;
            }
            record.setUrl("/api/v1/files/" + record.getId() + "/" + md5);
            uploadFileMapper.updateById(record);

            UploadResult result = toResult(record);
            result.setName(original);
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("文件上传失败：{}", e.getMessage());
            throw new BizException(500, "文件上传失败，请稍后重试");
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                    // 清理临时文件失败不影响主流程
                }
            }
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
        Path dir = uploadDir();
        Path path = dir.resolve(name).normalize();
        if (!path.startsWith(dir) || !Files.exists(path)) {
            throw new BizException(404, "文件已丢失");
        }
        return path;
    }

    public String contentType(UploadFile file) {
        String ext = extension(file.getOriginalName() == null ? "" : file.getOriginalName());
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "pdf" -> "application/pdf";
            case "mp4" -> "video/mp4";
            case "mov" -> "video/quicktime";
            case "txt", "md" -> "text/plain; charset=UTF-8";
            default -> "application/octet-stream";
        };
    }

    public boolean inlineSafe(UploadFile file) {
        String ext = extension(file.getOriginalName() == null ? "" : file.getOriginalName());
        return INLINE_TYPES.contains(ext);
    }

    private Path uploadDir() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
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
        return index < 0 ? "" : name.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private byte[] readHeader(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(16);
        } catch (IOException e) {
            throw new BizException(400, "无法读取上传文件");
        }
    }

    /** 按扩展名校验文件头（魔数），阻止把可执行内容伪装成图片。 */
    private boolean magicMatches(String ext, byte[] h) {
        return switch (ext) {
            case "jpg", "jpeg" -> startsWith(h, 0xFF, 0xD8, 0xFF);
            case "png" -> startsWith(h, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "gif" -> ascii(h, 0, "GIF87a") || ascii(h, 0, "GIF89a");
            case "webp" -> h.length >= 12 && ascii(h, 0, "RIFF") && ascii(h, 8, "WEBP");
            case "pdf" -> ascii(h, 0, "%PDF-");
            case "zip", "docx", "pptx", "xlsx" ->
                    startsWith(h, 0x50, 0x4B, 0x03, 0x04) || startsWith(h, 0x50, 0x4B, 0x05, 0x06);
            case "rar" -> startsWith(h, 0x52, 0x61, 0x72, 0x21, 0x1A, 0x07);
            case "7z" -> startsWith(h, 0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C);
            case "doc", "ppt", "xls" -> startsWith(h, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
            case "mp4", "mov" -> h.length >= 8 && ascii(h, 4, "ftyp");
            case "svg" -> looksLikeSvg(h);
            // txt / md 等纯文本没有魔数，交由扩展名白名单约束
            default -> true;
        };
    }

    private boolean startsWith(byte[] data, int... prefix) {
        if (data == null || data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((data[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean ascii(byte[] data, int offset, String text) {
        if (data == null || data.length < offset + text.length()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (data[offset + i] != (byte) text.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private boolean looksLikeSvg(byte[] header) {
        String text = new String(header, StandardCharsets.UTF_8).trim();
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1).trim();
        }
        return text.startsWith("<svg") || text.startsWith("<?xml") || text.startsWith("<!--");
    }
}
