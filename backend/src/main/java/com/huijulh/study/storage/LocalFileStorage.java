package com.huijulh.study.storage;

import com.huijulh.study.common.BusinessException;
import com.huijulh.study.common.ErrorCode;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class LocalFileStorage {
    private static final Set<String> SAFE_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "jpg", "jpeg", "png", "webp", "zip"
    );
    private final Path root;
    private final Path tempRoot;

    public LocalFileStorage(
            @Value("${study.storage.root}") String root,
            @Value("${study.storage.temp-root}") String tempRoot
    ) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.tempRoot = Path.of(tempRoot).toAbsolutePath().normalize();
    }

    @PostConstruct
    void initialize() throws IOException {
        Files.createDirectories(root);
        Files.createDirectories(tempRoot);
    }

    public StoredFile storeTemp(MultipartFile file, String category, Set<String> allowedExtensions, long maxBytes) {
        validateUpload(file, allowedExtensions, maxBytes);
        String extension = extension(file.getOriginalFilename());
        String key = safeCategory(category) + "/" + UUID.randomUUID() + "." + extension;
        Path destination = resolve(tempRoot, key);
        try {
            Files.createDirectories(destination.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            String mime = detectMime(destination, extension);
            validateMagic(destination, extension);
            return new StoredFile(key, safeOriginalName(file.getOriginalFilename()), file.getSize(), extension, mime);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件保存失败");
        }
    }

    public StoredFile storeFormal(MultipartFile file, String category, Set<String> allowedExtensions, long maxBytes) {
        StoredFile temporary = storeTemp(file, category, allowedExtensions, maxBytes);
        String formalKey = moveToFormal(temporary.storageKey(), category);
        return new StoredFile(formalKey, temporary.originalName(), temporary.size(),
                temporary.extension(), temporary.mimeType());
    }

    public String moveToFormal(String tempKey, String category) {
        Path source = resolve(tempRoot, tempKey);
        String destinationKey = safeCategory(category) + "/" + LocalDate.now() + "/" +
                UUID.randomUUID() + "." + extension(tempKey);
        Path destination = resolve(root, destinationKey);
        try {
            Files.createDirectories(destination.getParent());
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            return destinationKey;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "临时文件转存失败");
        }
    }

    public StoredFile importTempPath(Path path, String originalName, String category) {
        String extension = extension(originalName);
        if (!SAFE_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件类型不允许");
        }
        String key = safeCategory(category) + "/" + UUID.randomUUID() + "." + extension;
        Path destination = resolve(tempRoot, key);
        try {
            Files.createDirectories(destination.getParent());
            Files.move(path, destination, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(key, safeOriginalName(originalName), Files.size(destination),
                    extension, detectMime(destination, extension));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "解析文件保存失败");
        }
    }

    public Resource load(String storageKey) {
        try {
            Path file = resolve(root, storageKey);
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException(ErrorCode.MATERIAL_NOT_FOUND, "文件不存在");
            }
            return resource;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.MATERIAL_NOT_FOUND, "文件不存在");
        }
    }

    public Path tempPath(String key) {
        return resolve(tempRoot, key);
    }

    public void deleteTemp(String key) {
        try {
            Files.deleteIfExists(resolve(tempRoot, key));
        } catch (IOException ignored) {
        }
    }

    private void validateUpload(
            MultipartFile file,
            Set<String> allowedExtensions,
            long maxBytes
    ) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件不能为空");
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件超过大小限制");
        }
        String extension = extension(file.getOriginalFilename());
        if (!allowedExtensions.contains(extension)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件类型不允许");
        }
    }

    private void validateMagic(Path path, String extension) throws IOException {
        byte[] header = new byte[8];
        int read;
        try (InputStream input = Files.newInputStream(path)) {
            read = input.read(header);
        }
        if (read < 4) throw new BusinessException(ErrorCode.BAD_REQUEST, "文件内容为空或损坏");
        boolean valid = switch (extension) {
            case "pdf" -> header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F';
            case "zip", "docx", "xlsx", "pptx" -> header[0] == 'P' && header[1] == 'K';
            case "png" -> (header[0] & 0xff) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G';
            case "jpg", "jpeg" -> (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8;
            default -> true;
        };
        if (!valid) throw new BusinessException(ErrorCode.BAD_REQUEST, "文件内容与扩展名不一致");
    }

    private String detectMime(Path path, String extension) throws IOException {
        String detected = Files.probeContentType(path);
        if (detected != null) return detected;
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "zip" -> "application/zip";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    private Path resolve(Path base, String key) {
        Path resolved = base.resolve(key).normalize();
        if (!resolved.startsWith(base)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "非法文件路径");
        }
        return resolved;
    }

    private String extension(String fileName) {
        String safe = safeOriginalName(fileName);
        int index = safe.lastIndexOf('.');
        if (index < 0 || index == safe.length() - 1) return "";
        return safe.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String safeOriginalName(String fileName) {
        if (fileName == null) return "unnamed";
        return Path.of(fileName).getFileName().toString()
                .replace("\r", "").replace("\n", "").replace("\"", "");
    }

    private String safeCategory(String category) {
        if (category == null || !category.matches("[a-z0-9/_-]+")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "非法存储分类");
        }
        return category;
    }
}
