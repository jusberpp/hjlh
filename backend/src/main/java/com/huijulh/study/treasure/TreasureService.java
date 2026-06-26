package com.huijulh.study.treasure;

import com.huijulh.study.common.BusinessException;
import com.huijulh.study.common.ErrorCode;
import com.huijulh.study.common.GeneratedKeys;
import com.huijulh.study.security.SecurityContext;
import com.huijulh.study.storage.FileTokenService;
import com.huijulh.study.storage.LocalFileStorage;
import com.huijulh.study.storage.StoredFile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class TreasureService {
    private static final Set<String> PERSONAL_EXTENSIONS = Set.of("pdf", "doc", "docx", "xlsx");
    private static final long MAX_UNCOMPRESSED_BYTES = 1024L * 1024 * 1024;
    private static final int MAX_ENTRIES = 5000;
    private final JdbcTemplate jdbcTemplate;
    private final LocalFileStorage storage;
    private final FileTokenService tokenService;

    public TreasureService(
            JdbcTemplate jdbcTemplate,
            LocalFileStorage storage,
            FileTokenService tokenService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.storage = storage;
        this.tokenService = tokenService;
    }

    public List<Map<String, Object>> homeworkOptions(long courseId) {
        ensureCourse(courseId);
        return jdbcTemplate.query("""
                        SELECT m.material_id, m.file_name, m.open_time,
                          EXISTS(SELECT 1 FROM edu_treasure_batch tb
                            WHERE tb.homework_material_id=m.material_id AND tb.active_flag=1 AND tb.deleted=0) uploaded
                        FROM edu_material m
                        WHERE m.course_id=? AND m.org_id=? AND m.material_type='HOMEWORK'
                          AND m.deleted=0 AND m.open_time<=CURRENT_TIMESTAMP
                        ORDER BY m.open_time DESC
                        """,
                (rs, rowNum) -> {
                    boolean uploaded = rs.getBoolean("uploaded");
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("homeworkMaterialId", rs.getLong("material_id"));
                    row.put("homeworkTitle", rs.getString("file_name"));
                    row.put("displayTime", rs.getTimestamp("open_time").toLocalDateTime());
                    row.put("treasureUploaded", uploaded);
                    row.put("selectable", !uploaded);
                    row.put("disabledReason", uploaded ? "已上传提分宝" : null);
                    return row;
                }, courseId, SecurityContext.orgId());
    }

    public PageResult list(long courseId, int pageNum, int pageSize) {
        ensureCourse(courseId);
        int limit = Math.max(1, Math.min(pageSize, 100));
        Long total = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM edu_treasure_batch
                        WHERE course_id=? AND org_id=? AND deleted=0
                        """, Long.class, courseId, SecurityContext.orgId());
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                        SELECT tb.*, m.file_name homework_title
                        FROM edu_treasure_batch tb
                        JOIN edu_material m ON m.material_id=tb.homework_material_id
                        WHERE tb.course_id=? AND tb.org_id=? AND tb.deleted=0
                        ORDER BY tb.create_time DESC LIMIT ? OFFSET ?
                        """,
                (rs, rowNum) -> Map.of(
                        "batchId", rs.getLong("batch_id"),
                        "courseId", rs.getLong("course_id"),
                        "homeworkMaterialId", rs.getLong("homework_material_id"),
                        "homeworkTitle", rs.getString("homework_title"),
                        "fileName", rs.getString("source_zip_name"),
                        "parsedFileCount", rs.getInt("parsed_file_count"),
                        "parseStatus", rs.getString("parse_status"),
                        "publishStatus", rs.getString("publish_status"),
                        "createBy", rs.getString("create_by"),
                        "createTime", rs.getTimestamp("create_time").toLocalDateTime()
                ), courseId, SecurityContext.orgId(), limit, Math.max(0, pageNum - 1) * limit);
        return new PageResult(total == null ? 0 : total, rows);
    }

    public Map<String, Object> parse(long courseId, long homeworkMaterialId, MultipartFile zipFile) {
        Homework homework = ensureHomework(courseId, homeworkMaterialId, true);
        StoredFile zip = storage.storeTemp(zipFile, "treasure/zip", Set.of("zip"), 500L * 1024 * 1024);
        Map<String, StudentRef> students = courseStudents(courseId);
        List<Map<String, Object>> files = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        Set<Long> matchedStudents = new HashSet<>();
        Path extractionRoot = storage.tempPath("treasure/extract/" + UUID.randomUUID());
        long totalBytes = 0;
        int entries = 0;
        try {
            Files.createDirectories(extractionRoot);
            try (InputStream input = new BufferedInputStream(Files.newInputStream(storage.tempPath(zip.storageKey())));
                 ZipInputStream zipInput = new ZipInputStream(input)) {
                ZipEntry entry;
                while ((entry = zipInput.getNextEntry()) != null) {
                    if (entry.isDirectory() || entry.getName().startsWith("__MACOSX/")) continue;
                    entries++;
                    if (entries > MAX_ENTRIES) {
                        throw new BusinessException(42202, "ZIP 文件数量超过限制");
                    }
                    Path target = extractionRoot.resolve(entry.getName()).normalize();
                    if (!target.startsWith(extractionRoot)) {
                        throw new BusinessException(42202, "ZIP 包含非法路径");
                    }
                    String originalName = target.getFileName().toString();
                    String extension = extension(originalName);
                    if (!PERSONAL_EXTENSIONS.contains(extension)) {
                        errors.add(error(entry.getName(), null, "文件类型不允许"));
                        drain(zipInput);
                        continue;
                    }
                    Files.createDirectories(target.getParent());
                    long written = copyBounded(zipInput, target, MAX_UNCOMPRESSED_BYTES - totalBytes);
                    totalBytes += written;
                    if (written == 0) {
                        errors.add(error(entry.getName(), null, "文件为空"));
                        continue;
                    }
                    String studentNo = extractStudentNo(originalName, students);
                    StudentRef student = students.get(studentNo);
                    if (student == null) {
                        errors.add(error(entry.getName(), studentNo, "该学号不属于当前课程授权学员"));
                        continue;
                    }
                    if (!matchedStudents.add(student.studentId())) {
                        errors.add(error(entry.getName(), studentNo, "同一学生存在重复文件"));
                        continue;
                    }
                    StoredFile personal = storage.importTempPath(target, originalName, "treasure/personal");
                    files.add(Map.of(
                            "studentId", student.studentId(),
                            "studentNo", student.studentNo(),
                            "tempKey", personal.storageKey(),
                            "fileName", personal.originalName(),
                            "fileSize", personal.size(),
                            "fileHash", sha256(storage.tempPath(personal.storageKey()))
                    ));
                }
            }
        } catch (BusinessException exception) {
            storage.deleteTemp(zip.storageKey());
            throw exception;
        } catch (Exception exception) {
            storage.deleteTemp(zip.storageKey());
            throw new BusinessException(42202, "提分宝压缩包解析失败");
        } finally {
            deleteRecursively(extractionRoot);
        }
        boolean canSubmit = errors.isEmpty() && !files.isEmpty();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("courseId", courseId);
        payload.put("homeworkMaterialId", homeworkMaterialId);
        payload.put("homeworkTitle", homework.title());
        payload.put("zipTempKey", zip.storageKey());
        payload.put("zipName", zip.originalName());
        payload.put("files", files);
        payload.put("canSubmit", canSubmit);
        FileTokenService.IssuedToken issued = tokenService.issue("TREASURE_PARSE", payload);
        for (Map<String, Object> error : errors) {
            jdbcTemplate.update("""
                            INSERT INTO edu_treasure_parse_error(token_value, file_path, student_no, message)
                            VALUES (?, ?, ?, ?)
                            """, issued.token(), error.get("path"), error.get("studentNo"), error.get("message"));
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("parseToken", issued.token());
        response.put("homeworkMaterialId", homeworkMaterialId);
        response.put("homeworkTitle", homework.title());
        response.put("fileName", zip.originalName());
        response.put("parseStatus", canSubmit ? "SUCCESS" : "FAILED");
        response.put("totalCount", entries);
        response.put("successCount", files.size());
        response.put("failedCount", errors.size());
        response.put("canSubmit", canSubmit);
        response.put("message", canSubmit ? "目录结构、学生学号与文件命名校验通过" : "存在无法匹配的学号或文件错误");
        response.put("errors", errors);
        return response;
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public Map<String, Object> confirm(TreasureDtos.ConfirmRequest request) {
        ensureHomework(request.courseId(), request.homeworkMaterialId(), true);
        Map<String, Object> payload =
                tokenService.consume(request.parseToken(), "TREASURE_PARSE", ErrorCode.TREASURE_TOKEN_EXPIRED);
        if (((Number) payload.get("courseId")).longValue() != request.courseId()
                || ((Number) payload.get("homeworkMaterialId")).longValue() != request.homeworkMaterialId()
                || !Boolean.TRUE.equals(payload.get("canSubmit"))) {
            throw new BusinessException(42202, "解析令牌与课程、作业不一致或解析未通过");
        }
        List<Map<String, Object>> files = (List<Map<String, Object>>) payload.get("files");
        String zipFormalKey = storage.moveToFormal((String) payload.get("zipTempKey"), "treasure/zip");
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                                INSERT INTO edu_treasure_batch(
                                  org_id, course_id, homework_material_id, source_zip_name,
                                  source_zip_storage_key, parse_status, publish_status,
                                  parsed_file_count, published_at, active_flag, create_by
                                ) VALUES (?, ?, ?, ?, ?, 'SUCCESS', 'PUBLISHED', ?, CURRENT_TIMESTAMP, 1, ?)
                                """, Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, SecurityContext.orgId());
                statement.setLong(2, request.courseId());
                statement.setLong(3, request.homeworkMaterialId());
                statement.setString(4, (String) payload.get("zipName"));
                statement.setString(5, zipFormalKey);
                statement.setInt(6, files.size());
                statement.setString(7, SecurityContext.username());
                return statement;
            }, keyHolder);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.TREASURE_CONFLICT, "该作业已上传过提分宝");
        }
        long batchId = GeneratedKeys.require(keyHolder, "batch_id");
        for (Map<String, Object> file : files) {
            String formalKey = storage.moveToFormal((String) file.get("tempKey"), "treasure/personal");
            jdbcTemplate.update("""
                            INSERT INTO edu_treasure_file(
                              batch_id, student_id, student_no_snapshot, file_name, storage_key,
                              file_size, file_hash, publish_status
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, 'PUBLISHED')
                            """, batchId, ((Number) file.get("studentId")).longValue(), file.get("studentNo"),
                    file.get("fileName"), formalKey, ((Number) file.get("fileSize")).longValue(),
                    file.get("fileHash"));
        }
        return Map.of(
                "batchId", batchId,
                "homeworkMaterialId", request.homeworkMaterialId(),
                "homeworkTitle", payload.get("homeworkTitle"),
                "parsedFileCount", files.size()
        );
    }

    @Transactional
    public void delete(List<Long> batchIds) {
        for (Long batchId : batchIds) {
            int updated = jdbcTemplate.update("""
                            UPDATE edu_treasure_batch
                            SET deleted=1, active_flag=NULL, publish_status='REVOKED', update_time=CURRENT_TIMESTAMP
                            WHERE batch_id=? AND org_id=? AND deleted=0
                            """, batchId, SecurityContext.orgId());
            if (updated == 0) throw new BusinessException(ErrorCode.TREASURE_NOT_FOUND, "提分宝批次不存在");
            jdbcTemplate.update("""
                            UPDATE edu_treasure_file SET deleted=1, publish_status='REVOKED'
                            WHERE batch_id=?
                            """, batchId);
        }
    }

    private Homework ensureHomework(long courseId, long homeworkId, boolean requireOpen) {
        List<Homework> rows = jdbcTemplate.query("""
                        SELECT file_name, open_time FROM edu_material
                        WHERE material_id=? AND course_id=? AND org_id=?
                          AND material_type='HOMEWORK' AND deleted=0
                        """, (rs, rowNum) -> new Homework(
                        rs.getString("file_name"), rs.getTimestamp("open_time").toLocalDateTime()),
                homeworkId, courseId, SecurityContext.orgId());
        if (rows.isEmpty()) throw new BusinessException(ErrorCode.MATERIAL_NOT_FOUND, "关联作业不存在");
        Homework homework = rows.get(0);
        if (requireOpen && homework.openTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.HOMEWORK_NOT_OPEN, "作业尚未对学生显示，不能上传提分宝");
        }
        return homework;
    }

    private void ensureCourse(long courseId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM edu_course WHERE course_id=? AND org_id=? AND deleted=0
                        """, Integer.class, courseId, SecurityContext.orgId());
        if (count == null || count == 0) throw new BusinessException(ErrorCode.COURSE_NOT_FOUND, "课程不存在");
    }

    private Map<String, StudentRef> courseStudents(long courseId) {
        Map<String, StudentRef> students = new HashMap<>();
        jdbcTemplate.query("""
                        SELECT s.student_id, s.student_no FROM edu_student s
                        JOIN edu_student_course sc ON sc.student_id=s.student_id AND sc.deleted=0
                        WHERE sc.course_id=? AND s.org_id=? AND s.deleted=0 AND s.status='ENABLED'
                        """, rs -> {
                    StudentRef ref = new StudentRef(rs.getLong("student_id"), rs.getString("student_no"));
                    students.put(normalizeStudentNo(ref.studentNo()), ref);
                }, courseId, SecurityContext.orgId());
        return students;
    }

    private String extractStudentNo(String fileName, Map<String, StudentRef> students) {
        String stem = fileName.substring(0, fileName.lastIndexOf('.')).trim();
        String normalized = normalizeStudentNo(stem);
        if (students.containsKey(normalized)) return normalized;
        String first = stem.split("[_\\s]+", 2)[0];
        return normalizeStudentNo(first);
    }

    private static String normalizeStudentNo(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private static String extension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index < 0 ? "" : fileName.substring(index + 1).toLowerCase();
    }

    private static long copyBounded(InputStream input, Path target, long remaining) throws IOException {
        if (remaining <= 0) throw new BusinessException(42202, "ZIP 解压总大小超过限制");
        long total = 0;
        byte[] buffer = new byte[8192];
        try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(target))) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > remaining) throw new BusinessException(42202, "ZIP 解压总大小超过限制");
                output.write(buffer, 0, read);
            }
        }
        return total;
    }

    private static void drain(InputStream input) throws IOException {
        byte[] buffer = new byte[8192];
        while (input.read(buffer) != -1) {
            // consume current ZIP entry
        }
    }

    private static String sha256(Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot hash file", exception);
        }
    }

    private static Map<String, Object> error(String path, String studentNo, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("path", path);
        error.put("studentNo", studentNo);
        error.put("message", message);
        return error;
    }

    private static void deleteRecursively(Path path) {
        if (path == null || !Files.exists(path)) return;
        try (var stream = Files.walk(path)) {
            stream.sorted((left, right) -> right.compareTo(left)).forEach(current -> {
                try {
                    Files.deleteIfExists(current);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    public record PageResult(long total, List<Map<String, Object>> rows) {}
    private record Homework(String title, LocalDateTime openTime) {}
    private record StudentRef(long studentId, String studentNo) {}
}
