package com.huijulh.study.material;

import com.huijulh.study.common.BusinessException;
import com.huijulh.study.common.ErrorCode;
import com.huijulh.study.common.GeneratedKeys;
import com.huijulh.study.course.CourseService;
import com.huijulh.study.security.SecurityContext;
import com.huijulh.study.storage.FileTokenService;
import com.huijulh.study.storage.LocalFileStorage;
import com.huijulh.study.storage.StoredFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class MaterialService {
    private static final Set<String> MATERIAL_EXTENSIONS =
            Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx");
    private final JdbcTemplate jdbcTemplate;
    private final LocalFileStorage storage;
    private final FileTokenService tokenService;

    public MaterialService(
            JdbcTemplate jdbcTemplate,
            LocalFileStorage storage,
            FileTokenService tokenService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.storage = storage;
        this.tokenService = tokenService;
    }

    public List<Map<String, Object>> courseList(String keyword) {
        String like = keyword == null || keyword.isBlank() ? "%" : "%" + keyword.trim() + "%";
        return jdbcTemplate.query("""
                        SELECT c.course_id, c.course_name, c.grade, c.subject,
                          (SELECT COUNT(*) FROM edu_material m WHERE m.course_id=c.course_id AND m.deleted=0) material_count,
                          (SELECT COUNT(*) FROM edu_treasure_batch tb WHERE tb.course_id=c.course_id AND tb.deleted=0) treasure_count
                        FROM edu_course c
                        WHERE c.org_id=? AND c.deleted=0
                          AND (c.course_name LIKE ? OR c.grade LIKE ? OR c.subject LIKE ?)
                        ORDER BY c.sort_order, c.course_id
                        """,
                (rs, rowNum) -> Map.of(
                        "courseId", rs.getLong("course_id"),
                        "courseName", rs.getString("course_name"),
                        "grade", rs.getString("grade"),
                        "gradeName", CourseService.gradeName(rs.getString("grade")),
                        "subject", rs.getString("subject"),
                        "subjectName", CourseService.subjectName(rs.getString("subject")),
                        "materialCount", rs.getLong("material_count"),
                        "treasureBatchCount", rs.getLong("treasure_count"),
                        "supportsQuestionScore", "PHYSICS".equals(rs.getString("subject"))
                ), SecurityContext.orgId(), like, like, like);
    }

    public List<Map<String, Object>> list(long courseId, String materialType) {
        ensureCourse(courseId);
        String typeFilter = materialType == null || materialType.isBlank() ? "" : " AND m.material_type=?";
        Object[] args = typeFilter.isEmpty()
                ? new Object[]{courseId, SecurityContext.orgId()}
                : new Object[]{courseId, SecurityContext.orgId(), materialType};
        return jdbcTemplate.query("""
                        SELECT m.*,
                          (SELECT COUNT(*) FROM edu_homework_submission hs WHERE hs.material_id=m.material_id AND hs.active_flag=1) submitted_count,
                          (SELECT COUNT(*) FROM edu_student_course sc WHERE sc.course_id=m.course_id AND sc.deleted=0) authorized_count
                        FROM edu_material m
                        WHERE m.course_id=? AND m.org_id=? AND m.deleted=0
                        """ + typeFilter + " ORDER BY m.open_time DESC, m.material_id DESC",
                (rs, rowNum) -> materialMap(
                        rs.getLong("material_id"), rs.getLong("course_id"), rs.getString("file_name"),
                        rs.getLong("file_size"), rs.getString("file_extension"), rs.getString("material_type"),
                        rs.getTimestamp("open_time").toLocalDateTime(),
                        toLocalDateTime(rs.getTimestamp("submit_deadline")),
                        rs.getLong("submitted_count"), rs.getLong("authorized_count")
                ), args);
    }

    public List<Map<String, Object>> upload(List<MultipartFile> files) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            StoredFile stored = storage.storeTemp(file, "material", MATERIAL_EXTENSIONS, 100L * 1024 * 1024);
            FileTokenService.IssuedToken issued = tokenService.issue("MATERIAL_UPLOAD", Map.of(
                    "tempKey", stored.storageKey(),
                    "fileName", stored.originalName(),
                    "fileSize", stored.size(),
                    "extension", stored.extension(),
                    "mimeType", stored.mimeType()
            ));
            results.add(Map.of(
                    "uploadToken", issued.token(),
                    "fileName", stored.originalName(),
                    "fileSize", stored.size(),
                    "fileSizeText", formatSize(stored.size()),
                    "fileExtension", stored.extension(),
                    "expiresAt", issued.expiresAt()
            ));
        }
        return results;
    }

    @Transactional
    public Map<String, Object> createBatch(MaterialDtos.BatchRequest request) {
        CourseInfo course = ensureCourse(request.courseId());
        List<Long> materialIds = new ArrayList<>();
        for (MaterialDtos.BatchFile file : request.files()) {
            validateHomework(course.subject(), request.materialType(), file.openTime(),
                    file.submitDeadline(), file.questionCount(), file.questionScores());
            Map<String, Object> payload =
                    tokenService.consume(file.uploadToken(), "MATERIAL_UPLOAD", ErrorCode.UPLOAD_TOKEN_EXPIRED);
            String formalKey = storage.moveToFormal((String) payload.get("tempKey"), "material");
            Long materialId = insertMaterial(request.courseId(), request.materialType(), file,
                    payload, formalKey);
            if (isPhysicsHomework(course.subject(), request.materialType())) {
                saveQuestions(materialId, Objects.requireNonNull(file.questionScores()));
            }
            materialIds.add(materialId);
        }
        return Map.of("successCount", materialIds.size(), "materialIds", materialIds);
    }

    public Map<String, Object> detail(long materialId) {
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                        SELECT m.*,
                          (SELECT COUNT(*) FROM edu_homework_submission hs WHERE hs.material_id=m.material_id AND hs.active_flag=1) submitted_count,
                          (SELECT COUNT(*) FROM edu_student_course sc WHERE sc.course_id=m.course_id AND sc.deleted=0) authorized_count
                        FROM edu_material m
                        WHERE m.material_id=? AND m.org_id=? AND m.deleted=0
                        """,
                (rs, rowNum) -> materialMap(
                        rs.getLong("material_id"), rs.getLong("course_id"), rs.getString("file_name"),
                        rs.getLong("file_size"), rs.getString("file_extension"), rs.getString("material_type"),
                        rs.getTimestamp("open_time").toLocalDateTime(),
                        toLocalDateTime(rs.getTimestamp("submit_deadline")),
                        rs.getLong("submitted_count"), rs.getLong("authorized_count")
                ), materialId, SecurityContext.orgId());
        if (rows.isEmpty()) throw new BusinessException(ErrorCode.MATERIAL_NOT_FOUND, "资料不存在");
        return rows.get(0);
    }

    @Transactional
    public void update(MaterialDtos.UpdateRequest request) {
        Map<String, Object> current = detail(request.materialId());
        CourseInfo course = ensureCourse(((Number) current.get("courseId")).longValue());
        validateHomework(course.subject(), request.materialType(), request.openTime(),
                request.submitDeadline(), request.questionCount(), request.questionScores());
        long submittedCount = ((Number) current.get("submittedCount")).longValue();
        if (submittedCount > 0 && questionsChanged(request.materialId(), request.questionScores())) {
            throw new BusinessException(ErrorCode.HOMEWORK_STRUCTURE_LOCKED,
                    "已有学生提交作业，不能修改题目数量或小题分");
        }
        jdbcTemplate.update("""
                        UPDATE edu_material SET material_type=?, open_time=?, submit_deadline=?,
                          version=version+1, update_by=?, update_time=CURRENT_TIMESTAMP
                        WHERE material_id=? AND org_id=? AND deleted=0
                        """, request.materialType(), Timestamp.valueOf(request.openTime()),
                request.submitDeadline() == null ? null : Timestamp.valueOf(request.submitDeadline()),
                SecurityContext.username(), request.materialId(), SecurityContext.orgId());
        if (submittedCount == 0) {
            jdbcTemplate.update("DELETE FROM edu_homework_question WHERE material_id=?", request.materialId());
            if (isPhysicsHomework(course.subject(), request.materialType())) {
                saveQuestions(request.materialId(), Objects.requireNonNull(request.questionScores()));
            }
        }
    }

    @Transactional
    public void delete(List<Long> materialIds) {
        for (Long materialId : materialIds) {
            Map<String, Object> current = detail(materialId);
            if ("HOMEWORK".equals(current.get("materialType"))
                    && ((Number) current.get("submittedCount")).longValue() > 0) {
                throw new BusinessException(ErrorCode.HOMEWORK_STRUCTURE_LOCKED,
                        "作业已有提交记录，不能删除");
            }
            jdbcTemplate.update("""
                            UPDATE edu_material SET deleted=1, update_by=?, update_time=CURRENT_TIMESTAMP
                            WHERE material_id=? AND org_id=?
                            """, SecurityContext.username(), materialId, SecurityContext.orgId());
        }
    }

    private Long insertMaterial(
            long courseId,
            String materialType,
            MaterialDtos.BatchFile file,
            Map<String, Object> payload,
            String formalKey
    ) {
        var keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                            INSERT INTO edu_material(
                              org_id, course_id, material_type, file_name, storage_key,
                              file_size, file_extension, mime_type, open_time, submit_deadline,
                              status, create_by, update_by
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ENABLED', ?, ?)
                            """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, SecurityContext.orgId());
            statement.setLong(2, courseId);
            statement.setString(3, materialType);
            statement.setString(4, (String) payload.get("fileName"));
            statement.setString(5, formalKey);
            statement.setLong(6, ((Number) payload.get("fileSize")).longValue());
            statement.setString(7, (String) payload.get("extension"));
            statement.setString(8, (String) payload.get("mimeType"));
            statement.setTimestamp(9, Timestamp.valueOf(file.openTime()));
            statement.setTimestamp(10, file.submitDeadline() == null ? null : Timestamp.valueOf(file.submitDeadline()));
            statement.setString(11, SecurityContext.username());
            statement.setString(12, SecurityContext.username());
            return statement;
        }, keyHolder);
        return GeneratedKeys.require(keyHolder, "material_id");
    }

    private void validateHomework(
            String subject,
            String materialType,
            LocalDateTime openTime,
            LocalDateTime deadline,
            Integer questionCount,
            List<MaterialDtos.QuestionScore> scores
    ) {
        if (!isPhysicsHomework(subject, materialType)) return;
        if (deadline == null) throw new BusinessException(42208, "物理作业提交截止时间不能为空");
        if (!deadline.isAfter(openTime)) throw new BusinessException(42209, "提交截止时间必须晚于开放时间");
        if (questionCount == null || questionCount < 1 || questionCount > 30) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "题目数量范围为 1-30");
        }
        if (scores == null || scores.size() != questionCount) {
            throw new BusinessException(42204, "小题分数量与题目数量不一致");
        }
        for (int index = 0; index < scores.size(); index++) {
            MaterialDtos.QuestionScore score = scores.get(index);
            if (score.questionNo() != index + 1
                    || score.score().compareTo(BigDecimal.ONE) < 0
                    || score.score().compareTo(BigDecimal.valueOf(20)) > 0) {
                throw new BusinessException(42204, "题号必须连续且每题满分范围为 1-20");
            }
        }
    }

    private void saveQuestions(long materialId, List<MaterialDtos.QuestionScore> scores) {
        for (MaterialDtos.QuestionScore score : scores) {
            jdbcTemplate.update("""
                            INSERT INTO edu_homework_question(
                              material_id, question_no, full_score, required_flag, allow_decimal
                            ) VALUES (?, ?, ?, 1, 1)
                            """, materialId, score.questionNo(), score.score());
        }
    }

    private boolean questionsChanged(long materialId, List<MaterialDtos.QuestionScore> requested) {
        List<MaterialDtos.QuestionScore> existing = jdbcTemplate.query("""
                        SELECT question_no, full_score FROM edu_homework_question
                        WHERE material_id=? AND deleted=0 ORDER BY question_no
                        """, (rs, rowNum) -> new MaterialDtos.QuestionScore(
                        rs.getInt("question_no"), rs.getBigDecimal("full_score")), materialId);
        return !Objects.equals(existing, requested == null ? List.of() : requested);
    }

    private CourseInfo ensureCourse(long courseId) {
        List<CourseInfo> rows = jdbcTemplate.query("""
                        SELECT subject FROM edu_course WHERE course_id=? AND org_id=? AND deleted=0
                        """, (rs, rowNum) -> new CourseInfo(rs.getString("subject")),
                courseId, SecurityContext.orgId());
        if (rows.isEmpty()) throw new BusinessException(ErrorCode.COURSE_NOT_FOUND, "课程不存在");
        return rows.get(0);
    }

    private Map<String, Object> materialMap(
            long materialId, long courseId, String fileName, long fileSize,
            String extension, String materialType, LocalDateTime openTime,
            LocalDateTime submitDeadline, long submittedCount, long authorizedCount
    ) {
        List<MaterialDtos.QuestionScore> scores = jdbcTemplate.query("""
                        SELECT question_no, full_score FROM edu_homework_question
                        WHERE material_id=? AND deleted=0 ORDER BY question_no
                        """, (rs, rowNum) -> new MaterialDtos.QuestionScore(
                        rs.getInt("question_no"), rs.getBigDecimal("full_score")), materialId);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("materialId", materialId);
        row.put("courseId", courseId);
        row.put("fileName", fileName);
        row.put("fileSize", fileSize);
        row.put("fileSizeText", formatSize(fileSize));
        row.put("fileExtension", extension);
        row.put("materialType", materialType);
        row.put("openTime", openTime);
        row.put("openStatus", LocalDateTime.now().isBefore(openTime) ? "SCHEDULED" : "OPEN");
        row.put("submitDeadline", submitDeadline);
        row.put("questionCount", scores.size());
        row.put("totalScore", scores.stream().map(MaterialDtos.QuestionScore::score)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        row.put("questionScores", scores);
        row.put("submittedCount", submittedCount);
        row.put("authorizedStudentCount", authorizedCount);
        return row;
    }

    private static boolean isPhysicsHomework(String subject, String materialType) {
        return "PHYSICS".equals(subject) && "HOMEWORK".equals(materialType);
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
    }

    private record CourseInfo(String subject) {}
}
