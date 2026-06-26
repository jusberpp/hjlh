package com.huijulh.study.student;

import com.huijulh.study.common.BusinessException;
import com.huijulh.study.common.ErrorCode;
import com.huijulh.study.common.GeneratedKeys;
import com.huijulh.study.course.CourseService;
import com.huijulh.study.security.SecurityContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class StudentService {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbc;
    private final PhoneProtector phoneProtector;

    public StudentService(
            JdbcTemplate jdbcTemplate,
            NamedParameterJdbcTemplate namedJdbc,
            PhoneProtector phoneProtector
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbc = namedJdbc;
        this.phoneProtector = phoneProtector;
    }

    public PageResult list(
            String keyword, String grade, Long courseId, String phoneStatus, int pageNum, int pageSize
    ) {
        StringBuilder where = new StringBuilder(" WHERE s.org_id=:orgId AND s.deleted=0 ");
        MapSqlParameterSource parameters = new MapSqlParameterSource("orgId", SecurityContext.orgId());
        if (hasText(grade)) {
            where.append(" AND s.grade=:grade ");
            parameters.addValue("grade", grade);
        }
        if (courseId != null) {
            where.append("""
                     AND EXISTS(SELECT 1 FROM edu_student_course filter_sc
                       WHERE filter_sc.student_id=s.student_id AND filter_sc.course_id=:courseId
                       AND filter_sc.deleted=0)
                    """);
            parameters.addValue("courseId", courseId);
        }
        if ("BOUND".equals(phoneStatus)) where.append(" AND s.phone_hash IS NOT NULL ");
        if ("UNBOUND".equals(phoneStatus)) where.append(" AND s.phone_hash IS NULL ");
        if (hasText(keyword)) {
            String normalizedKeyword = keyword.trim();
            where.append(" AND (s.student_name LIKE :keyword OR s.student_no LIKE :keyword OR s.school LIKE :keyword ");
            parameters.addValue("keyword", "%" + normalizedKeyword + "%");
            if (normalizedKeyword.matches("^1\\d{10}$")) {
                where.append(" OR s.phone_hash=:phoneHash ");
                parameters.addValue("phoneHash", phoneProtector.hash(normalizedKeyword));
            }
            where.append(") ");
        }
        Long total = namedJdbc.queryForObject("SELECT COUNT(*) FROM edu_student s " + where, parameters, Long.class);
        int limit = Math.max(1, Math.min(pageSize, 100));
        parameters.addValue("limit", limit);
        parameters.addValue("offset", Math.max(pageNum - 1, 0) * limit);
        List<Map<String, Object>> rows = namedJdbc.query("""
                        SELECT s.* FROM edu_student s
                        """ + where + " ORDER BY s.update_time DESC, s.student_id DESC LIMIT :limit OFFSET :offset",
                parameters,
                (rs, rowNum) -> {
                    String decryptedPhone = phoneProtector.decrypt(rs.getString("authorized_phone"));
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("studentId", rs.getLong("student_id"));
                    row.put("school", rs.getString("school"));
                    row.put("grade", rs.getString("grade"));
                    row.put("gradeName", CourseService.gradeName(rs.getString("grade")));
                    row.put("className", rs.getString("class_name"));
                    row.put("studentName", rs.getString("student_name"));
                    row.put("studentNo", rs.getString("student_no"));
                    row.put("phoneMasked", phoneProtector.mask(decryptedPhone));
                    row.put("phoneStatus", decryptedPhone == null ? "UNBOUND" : "BOUND");
                    row.put("phoneSource", rs.getString("phone_source"));
                    row.put("phoneVerifiedAt", toLocalDateTime(rs.getTimestamp("phone_verified_at")));
                    row.put("updateTime", toLocalDateTime(rs.getTimestamp("update_time")));
                    return row;
                });
        rows.forEach(row -> row.put("courses", courseSummaries(((Number) row.get("studentId")).longValue())));
        return new PageResult(total == null ? 0 : total, rows);
    }

    public Map<String, Object> detail(long studentId) {
        List<Map<String, Object>> students = jdbcTemplate.query("""
                        SELECT * FROM edu_student WHERE student_id=? AND org_id=? AND deleted=0
                        """,
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("studentId", rs.getLong("student_id"));
                    row.put("school", rs.getString("school"));
                    row.put("grade", rs.getString("grade"));
                    row.put("className", rs.getString("class_name"));
                    row.put("studentName", rs.getString("student_name"));
                    row.put("studentNo", rs.getString("student_no"));
                    row.put("learningLevels", rs.getString("learning_levels"));
                    row.put("authorizedPhone", phoneProtector.decrypt(rs.getString("authorized_phone")));
                    return row;
                }, studentId, SecurityContext.orgId());
        if (students.isEmpty()) throw new BusinessException(ErrorCode.STUDENT_NOT_FOUND, "学员不存在");
        List<Map<String, Object>> courses = courseSummaries(studentId);
        students.get(0).put("courseIds", courses.stream().map(row -> row.get("courseId")).toList());
        courses.stream().filter(row -> Boolean.TRUE.equals(row.get("primary")))
                .findFirst().ifPresent(row -> students.get(0).put("primaryCourseId", row.get("courseId")));
        return students.get(0);
    }

    @Transactional
    public Map<String, Object> create(StudentDtos.SaveRequest request) {
        validateCourses(request.courseIds());
        String phone = phoneProtector.normalize(request.authorizedPhone());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                                INSERT INTO edu_student(
                                    org_id, school, grade, class_name, student_name, student_no,
                                    learning_levels, authorized_phone, phone_hash, phone_source,
                                    status, create_by, update_by
                                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ENABLED', ?, ?)
                                """, Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, SecurityContext.orgId());
                statement.setString(2, request.school().trim());
                statement.setString(3, request.grade());
                statement.setString(4, request.className().trim());
                statement.setString(5, request.studentName().trim());
                statement.setString(6, request.studentNo().trim());
                statement.setString(7, request.learningLevels());
                statement.setString(8, phoneProtector.encrypt(phone));
                statement.setString(9, phoneProtector.hash(phone));
                statement.setString(10, phone == null ? null : "ADMIN");
                statement.setString(11, SecurityContext.username());
                statement.setString(12, SecurityContext.username());
                return statement;
            }, keyHolder);
        } catch (DuplicateKeyException exception) {
            throw conflictFor(request.studentNo(), phone, null);
        }
        long studentId = GeneratedKeys.require(keyHolder, "student_id");
        replaceCourses(studentId, request.courseIds(), request.primaryCourseId(), "ADMIN", null);
        auditPhone(studentId, null, phone, phone == null ? "ADMIN_CLEAR" : "ADMIN");
        return Map.of("studentId", studentId);
    }

    @Transactional
    public void update(StudentDtos.SaveRequest request) {
        if (request.studentId() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "学员 ID 不能为空");
        Map<String, Object> old = detail(request.studentId());
        validateCourses(request.courseIds());
        String phone = phoneProtector.normalize(request.authorizedPhone());
        try {
            int updated = jdbcTemplate.update("""
                            UPDATE edu_student SET school=?, grade=?, class_name=?, student_name=?,
                              student_no=?, learning_levels=?, authorized_phone=?, phone_hash=?,
                              phone_source=?, phone_verified_at=NULL, version=version+1,
                              update_by=?, update_time=CURRENT_TIMESTAMP
                            WHERE student_id=? AND org_id=? AND deleted=0
                            """,
                    request.school().trim(), request.grade(), request.className().trim(),
                    request.studentName().trim(), request.studentNo().trim(), request.learningLevels(),
                    phoneProtector.encrypt(phone), phoneProtector.hash(phone),
                    phone == null ? null : "ADMIN", SecurityContext.username(),
                    request.studentId(), SecurityContext.orgId()
            );
            if (updated == 0) throw new BusinessException(ErrorCode.STUDENT_NOT_FOUND, "学员不存在");
        } catch (DuplicateKeyException exception) {
            throw conflictFor(request.studentNo(), phone, request.studentId());
        }
        Long oldPrimary = primaryCourse(request.studentId());
        replaceCourses(request.studentId(), request.courseIds(), request.primaryCourseId(), "ADMIN", oldPrimary);
        String oldPhone = (String) old.get("authorizedPhone");
        if (!Objects.equals(oldPhone, phone)) {
            auditPhone(request.studentId(), oldPhone, phone, phone == null ? "ADMIN_CLEAR" : "ADMIN");
        }
    }

    public List<Map<String, Object>> courseSummaries(long studentId) {
        return jdbcTemplate.query("""
                        SELECT c.course_id, c.course_name, sc.is_primary
                        FROM edu_student_course sc
                        JOIN edu_course c ON c.course_id=sc.course_id AND c.deleted=0
                        WHERE sc.student_id=? AND sc.deleted=0
                        ORDER BY sc.is_primary DESC, c.sort_order, c.course_id
                        """,
                (rs, rowNum) -> Map.of(
                        "courseId", rs.getLong("course_id"),
                        "courseName", rs.getString("course_name"),
                        "primary", rs.getBoolean("is_primary")
                ), studentId);
    }

    private void validateCourses(List<Long> courseIds) {
        List<Long> distinct = new ArrayList<>(new LinkedHashSet<>(courseIds));
        if (distinct.isEmpty()) throw new BusinessException(ErrorCode.COURSE_REQUIRED, "授权课程不能为空");
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("ids", distinct)
                .addValue("orgId", SecurityContext.orgId());
        Long count = namedJdbc.queryForObject("""
                        SELECT COUNT(*) FROM edu_course
                        WHERE course_id IN (:ids) AND org_id=:orgId AND status='ENABLED' AND deleted=0
                        """, parameters, Long.class);
        if (count == null || count != distinct.size()) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND, "授权课程不存在或已停用");
        }
    }

    private void replaceCourses(
            long studentId,
            List<Long> courseIds,
            Long requestedPrimary,
            String source,
            Long oldPrimary
    ) {
        List<Long> distinct = new ArrayList<>(new LinkedHashSet<>(courseIds));
        long primary;
        if (requestedPrimary != null && distinct.contains(requestedPrimary)) {
            primary = requestedPrimary;
        } else if (oldPrimary != null && distinct.contains(oldPrimary)) {
            primary = oldPrimary;
        } else {
            primary = distinct.get(0);
        }
        jdbcTemplate.update("DELETE FROM edu_student_course WHERE student_id=?", studentId);
        for (Long courseId : distinct) {
            jdbcTemplate.update("""
                            INSERT INTO edu_student_course(student_id, course_id, is_primary, source)
                            VALUES (?, ?, ?, ?)
                            """, studentId, courseId, courseId == primary ? 1 : 0, source);
        }
    }

    private Long primaryCourse(long studentId) {
        List<Long> ids = jdbcTemplate.queryForList("""
                        SELECT course_id FROM edu_student_course
                        WHERE student_id=? AND is_primary=1 AND deleted=0
                        """, Long.class, studentId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private BusinessException conflictFor(String studentNo, String phone, Long currentId) {
        String idClause = currentId == null ? "" : " AND student_id<>" + currentId;
        Integer studentNoCount = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM edu_student
                        WHERE org_id=? AND student_no=? AND deleted=0
                        """ + idClause, Integer.class, SecurityContext.orgId(), studentNo.trim());
        if (studentNoCount != null && studentNoCount > 0) {
            return new BusinessException(ErrorCode.STUDENT_NO_CONFLICT, "学号已存在");
        }
        if (phone != null) {
            Integer phoneCount = jdbcTemplate.queryForObject("""
                            SELECT COUNT(*) FROM edu_student
                            WHERE phone_hash=? AND deleted=0
                            """ + idClause, Integer.class, phoneProtector.hash(phone));
            if (phoneCount != null && phoneCount > 0) {
                return new BusinessException(ErrorCode.PHONE_CONFLICT, "该手机号已绑定其他学员");
            }
        }
        return new BusinessException(ErrorCode.BAD_REQUEST, "学员数据存在唯一性冲突");
    }

    private void auditPhone(long studentId, String oldPhone, String newPhone, String source) {
        jdbcTemplate.update("""
                        INSERT INTO edu_phone_bind_log(
                            student_id, old_phone_masked, new_phone_masked, source, operator
                        ) VALUES (?, ?, ?, ?, ?)
                        """, studentId, phoneProtector.mask(oldPhone), phoneProtector.mask(newPhone),
                source, SecurityContext.username());
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record PageResult(long total, List<Map<String, Object>> rows) {}
}
