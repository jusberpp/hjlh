package com.huijulh.study.studentauth;

import com.huijulh.study.common.ApiResponse;
import com.huijulh.study.common.BusinessException;
import com.huijulh.study.common.ErrorCode;
import com.huijulh.study.course.CourseService;
import com.huijulh.study.storage.LocalFileStorage;
import com.huijulh.study.student.PhoneProtector;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/auth")
public class StudentAuthController {
    private final JdbcTemplate jdbcTemplate;
    private final StudentTokenStore tokenStore;
    private final PhoneProtector phoneProtector;
    private final LocalFileStorage storage;
    private final Duration tokenTtl;

    public StudentAuthController(
            JdbcTemplate jdbcTemplate,
            StudentTokenStore tokenStore,
            PhoneProtector phoneProtector,
            LocalFileStorage storage,
            @Value("${study.auth.student-token-ttl}") Duration tokenTtl
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.tokenStore = tokenStore;
        this.phoneProtector = phoneProtector;
        this.storage = storage;
        this.tokenTtl = tokenTtl;
    }

    @GetMapping("/schools")
    @ResponseBody
    public ApiResponse<List<String>> schools() {
        return ApiResponse.ok(jdbcTemplate.queryForList("""
                        SELECT DISTINCT school FROM edu_student
                        WHERE deleted=0 AND status='ENABLED'
                        ORDER BY school
                        """, String.class));
    }

    @GetMapping("/grades")
    @ResponseBody
    public ApiResponse<List<String>> grades() {
        return ApiResponse.ok(List.of("高一", "高二", "高三"));
    }

    @PostMapping("/verify-student")
    @ResponseBody
    public ApiResponse<Map<String, Object>> verify(@Valid @RequestBody VerifyRequest request) {
        String grade = gradeCode(request.grade());
        List<StudentRow> students = jdbcTemplate.query("""
                        SELECT * FROM edu_student
                        WHERE school=? AND grade=? AND student_name=? AND student_no=?
                          AND deleted=0 AND status='ENABLED'
                        """,
                (rs, rowNum) -> new StudentRow(
                        rs.getLong("student_id"), rs.getLong("org_id"), rs.getString("school"),
                        rs.getString("grade"), rs.getString("class_name"), rs.getString("student_name"),
                        rs.getString("student_no"), rs.getString("learning_levels"),
                        rs.getString("authorized_phone")
                ), request.school().trim(), grade, request.name().trim(), request.studentId().trim());
        if (students.isEmpty()) throw new BusinessException(ErrorCode.STUDENT_NOT_FOUND, "未找到匹配的学生信息");
        if (students.size() > 1) throw new BusinessException(ErrorCode.BAD_REQUEST, "学生信息存在重复，请联系管理员");
        StudentRow student = students.get(0);
        CourseRow course = primaryCourse(student.studentId());
        String token = tokenStore.issue(student.studentId(), tokenTtl);
        Map<String, Object> data = studentCourseData(student, course);
        String phone = phoneProtector.decrypt(student.encryptedPhone());
        if (phone != null) data.put("phone", phoneProtector.mask(phone));
        data.put("token", token);
        return ApiResponse.ok(data);
    }

    @PostMapping("/bind-phone")
    @ResponseBody
    @Transactional
    public ApiResponse<Boolean> bindPhone(
            @RequestHeader("token") String token,
            @Valid @RequestBody BindPhoneRequest request,
            HttpServletRequest servletRequest
    ) {
        long tokenStudentId = requireStudent(token);
        long requestStudentId;
        try {
            requestStudentId = Long.parseLong(request.id());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "学生 ID 格式错误");
        }
        if (tokenStudentId != requestStudentId) {
            throw new BusinessException(403, "令牌与学生身份不一致");
        }
        StudentRow student = student(tokenStudentId);
        String oldPhone = phoneProtector.decrypt(student.encryptedPhone());
        String phone = phoneProtector.normalize(request.phone());
        if (phone.equals(oldPhone)) {
            tokenStore.refresh(token, tokenTtl);
            return ApiResponse.ok(true);
        }
        try {
            jdbcTemplate.update("""
                            UPDATE edu_student SET authorized_phone=?, phone_hash=?, phone_source='H5_VERIFIED',
                              phone_verified_at=CURRENT_TIMESTAMP, version=version+1, update_time=CURRENT_TIMESTAMP
                            WHERE student_id=? AND deleted=0
                            """, phoneProtector.encrypt(phone), phoneProtector.hash(phone), tokenStudentId);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.PHONE_CONFLICT, "该手机号已绑定其他学员");
        }
        jdbcTemplate.update("""
                        INSERT INTO edu_phone_bind_log(
                          student_id, old_phone_masked, new_phone_masked, source, operator, device_summary
                        ) VALUES (?, ?, ?, 'H5_VERIFIED', 'student', ?)
                        """, tokenStudentId, phoneProtector.mask(oldPhone), phoneProtector.mask(phone),
                safeDevice(servletRequest.getHeader("User-Agent")));
        CourseRow course = primaryCourse(tokenStudentId);
        jdbcTemplate.update("""
                        INSERT INTO edu_xiaoe_request_log(
                          idempotency_key, operation, student_id, course_id,
                          request_summary, response_summary, status, duration_ms
                        ) VALUES (?, 'OPEN_COURSE', ?, ?, ?, ?, 'SUCCESS', 0)
                        """,
                "bind:" + tokenStudentId + ":" + course.courseId() + ":" + phoneProtector.hash(phone),
                tokenStudentId, course.courseId(), "手机号已脱敏", "stub 模式：等待生产小鹅通凭据");
        tokenStore.refresh(token, tokenTtl);
        return ApiResponse.ok(true);
    }

    @GetMapping("/bind-result")
    @ResponseBody
    public ApiResponse<Map<String, Object>> bindResult(@RequestHeader("token") String token) {
        long studentId = requireStudent(token);
        StudentRow student = student(studentId);
        CourseRow course = primaryCourse(studentId);
        Map<String, Object> data = studentCourseData(student, course);
        String phone = phoneProtector.decrypt(student.encryptedPhone());
        data.put("phone", phoneProtector.mask(phone));
        data.put("files", openedMaterials(studentId, course.courseId()));
        tokenStore.refresh(token, tokenTtl);
        return ApiResponse.ok(data);
    }

    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<Resource> downloadMaterial(
            @PathVariable long fileId,
            @RequestHeader("token") String token
    ) {
        long studentId = requireStudent(token);
        List<FileRow> files = jdbcTemplate.query("""
                        SELECT m.file_name, m.storage_key, m.mime_type
                        FROM edu_material m
                        JOIN edu_student_course sc ON sc.course_id=m.course_id AND sc.student_id=? AND sc.deleted=0
                        WHERE m.material_id=? AND m.deleted=0 AND m.status='ENABLED'
                          AND m.open_time<=CURRENT_TIMESTAMP
                        """, (rs, rowNum) -> new FileRow(
                        rs.getString("file_name"), rs.getString("storage_key"), rs.getString("mime_type")),
                studentId, fileId);
        if (files.isEmpty()) throw new BusinessException(ErrorCode.MATERIAL_NOT_OPEN, "资料不存在、未开放或无权下载");
        tokenStore.refresh(token, tokenTtl);
        return downloadResponse(files.get(0));
    }

    @GetMapping("/treasure-files/{fileId}/download")
    public ResponseEntity<Resource> downloadTreasure(
            @PathVariable long fileId,
            @RequestHeader("token") String token
    ) {
        long studentId = requireStudent(token);
        List<FileRow> files = jdbcTemplate.query("""
                        SELECT tf.file_name, tf.storage_key, 'application/octet-stream' mime_type
                        FROM edu_treasure_file tf
                        JOIN edu_treasure_batch tb ON tb.batch_id=tf.batch_id
                        WHERE tf.treasure_file_id=? AND tf.student_id=? AND tf.deleted=0
                          AND tf.publish_status='PUBLISHED' AND tb.publish_status='PUBLISHED'
                          AND tb.deleted=0 AND (tf.expire_time IS NULL OR tf.expire_time>CURRENT_TIMESTAMP)
                        """, (rs, rowNum) -> new FileRow(
                        rs.getString("file_name"), rs.getString("storage_key"), rs.getString("mime_type")),
                fileId, studentId);
        if (files.isEmpty()) throw new BusinessException(403, "无权下载该提分宝文件");
        jdbcTemplate.update("""
                        UPDATE edu_treasure_file SET download_count=download_count+1,
                          last_download_time=CURRENT_TIMESTAMP WHERE treasure_file_id=?
                        """, fileId);
        tokenStore.refresh(token, tokenTtl);
        return downloadResponse(files.get(0));
    }

    private ResponseEntity<Resource> downloadResponse(FileRow file) {
        Resource resource = storage.load(file.storageKey());
        String disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8).build().toString();
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(file.mimeType());
        } catch (Exception exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(resource);
    }

    private Map<String, Object> studentCourseData(StudentRow student, CourseRow course) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", Long.toString(student.studentId()));
        data.put("school", student.school());
        data.put("grade", CourseService.gradeName(student.grade()));
        data.put("className", student.className());
        data.put("studentId", student.studentNo());
        data.put("studentName", student.studentName());
        data.put("learningLevels", student.learningLevels());
        data.put("goodsName", course.courseName());
        data.put("resourceId", course.externalCourseId());
        data.put("goodsImg", course.goodsImg());
        data.put("courseUrl", course.courseUrl());
        return data;
    }

    private List<Map<String, Object>> openedMaterials(long studentId, long courseId) {
        return jdbcTemplate.query("""
                        SELECT m.material_id, m.file_name, m.file_size, m.update_time
                        FROM edu_material m
                        JOIN edu_student_course sc ON sc.course_id=m.course_id
                        WHERE sc.student_id=? AND sc.course_id=? AND sc.deleted=0
                          AND m.deleted=0 AND m.status='ENABLED' AND m.open_time<=CURRENT_TIMESTAMP
                        ORDER BY m.open_time DESC
                        """, (rs, rowNum) -> Map.of(
                        "id", Long.toString(rs.getLong("material_id")),
                        "fileName", rs.getString("file_name"),
                        "fileSize", rs.getLong("file_size"),
                        "updateTime", rs.getTimestamp("update_time").toLocalDateTime()
                ), studentId, courseId);
    }

    private StudentRow student(long studentId) {
        List<StudentRow> students = jdbcTemplate.query("""
                        SELECT * FROM edu_student WHERE student_id=? AND deleted=0 AND status='ENABLED'
                        """, (rs, rowNum) -> new StudentRow(
                        rs.getLong("student_id"), rs.getLong("org_id"), rs.getString("school"),
                        rs.getString("grade"), rs.getString("class_name"), rs.getString("student_name"),
                        rs.getString("student_no"), rs.getString("learning_levels"),
                        rs.getString("authorized_phone")
                ), studentId);
        if (students.isEmpty()) throw new BusinessException(ErrorCode.STUDENT_NOT_FOUND, "学员不存在");
        return students.get(0);
    }

    private CourseRow primaryCourse(long studentId) {
        List<CourseRow> courses = jdbcTemplate.query("""
                        SELECT c.course_id, c.course_name, c.external_course_id, c.external_course_url, c.goods_img
                        FROM edu_student_course sc
                        JOIN edu_course c ON c.course_id=sc.course_id AND c.deleted=0
                        WHERE sc.student_id=? AND sc.is_primary=1 AND sc.deleted=0
                        """, (rs, rowNum) -> new CourseRow(
                        rs.getLong("course_id"), rs.getString("course_name"),
                        rs.getString("external_course_id"), rs.getString("external_course_url"),
                        rs.getString("goods_img")
                ), studentId);
        if (courses.isEmpty()) throw new BusinessException(ErrorCode.COURSE_NOT_FOUND, "学员未配置主课程");
        return courses.get(0);
    }

    private long requireStudent(String token) {
        return tokenStore.resolve(token)
                .orElseThrow(() -> new BusinessException(401, "学生认证令牌无效或已过期"));
    }

    private static String gradeCode(String grade) {
        return switch (grade.trim()) {
            case "高一", "SENIOR_ONE" -> "SENIOR_ONE";
            case "高二", "SENIOR_TWO" -> "SENIOR_TWO";
            case "高三", "SENIOR_THREE" -> "SENIOR_THREE";
            default -> grade;
        };
    }

    private static String safeDevice(String userAgent) {
        if (userAgent == null) return null;
        return userAgent.substring(0, Math.min(userAgent.length(), 250));
    }

    public record VerifyRequest(
            @NotBlank String school,
            @NotBlank String grade,
            @NotBlank String name,
            @NotBlank String studentId
    ) {}

    public record BindPhoneRequest(@NotBlank String id, @NotBlank String phone) {}

    private record StudentRow(
            long studentId,
            long orgId,
            String school,
            String grade,
            String className,
            String studentName,
            String studentNo,
            String learningLevels,
            String encryptedPhone
    ) {}

    private record CourseRow(
            long courseId,
            String courseName,
            String externalCourseId,
            String courseUrl,
            String goodsImg
    ) {}

    private record FileRow(String fileName, String storageKey, String mimeType) {}
}
