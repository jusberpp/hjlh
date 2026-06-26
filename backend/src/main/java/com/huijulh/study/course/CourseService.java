package com.huijulh.study.course;

import com.huijulh.study.common.BusinessException;
import com.huijulh.study.common.ErrorCode;
import com.huijulh.study.common.GeneratedKeys;
import com.huijulh.study.integration.xiaoe.XiaoeCourseClient;
import com.huijulh.study.security.SecurityContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class CourseService {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbc;
    private final XiaoeCourseClient xiaoeCourseClient;

    public CourseService(
            JdbcTemplate jdbcTemplate,
            NamedParameterJdbcTemplate namedJdbc,
            XiaoeCourseClient xiaoeCourseClient
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbc = namedJdbc;
        this.xiaoeCourseClient = xiaoeCourseClient;
    }

    public Map<String, Object> options() {
        List<CourseDtos.CourseSummary> courses = jdbcTemplate.query("""
                        SELECT course_id, course_name, grade, subject
                        FROM edu_course
                        WHERE org_id = ? AND status = 'ENABLED' AND deleted = 0
                        ORDER BY sort_order, course_id
                        """,
                (rs, rowNum) -> new CourseDtos.CourseSummary(
                        rs.getLong("course_id"),
                        rs.getString("course_name"),
                        rs.getString("grade"),
                        gradeName(rs.getString("grade")),
                        rs.getString("subject"),
                        subjectName(rs.getString("subject"))
                ),
                SecurityContext.orgId()
        );
        return Map.of(
                "grades", List.of(
                        option("高一", "SENIOR_ONE"),
                        option("高二", "SENIOR_TWO"),
                        option("高三", "SENIOR_THREE")
                ),
                "subjects", List.of(
                        option("数学培优", "MATH_ELITE"),
                        option("数学跃升", "MATH_ADVANCE"),
                        option("物理", "PHYSICS"),
                        option("英语", "ENGLISH"),
                        option("语文", "CHINESE")
                ),
                "courses", courses
        );
    }

    public PageResult list(String grade, String subject, String keyword, String status, int pageNum, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE c.org_id = :orgId AND c.deleted = 0 ");
        MapSqlParameterSource parameters = new MapSqlParameterSource("orgId", SecurityContext.orgId());
        if (hasText(grade)) {
            where.append(" AND c.grade = :grade ");
            parameters.addValue("grade", grade);
        }
        if (hasText(subject)) {
            where.append(" AND c.subject = :subject ");
            parameters.addValue("subject", subject);
        }
        if (hasText(status)) {
            where.append(" AND c.status = :status ");
            parameters.addValue("status", status);
        }
        if (hasText(keyword)) {
            where.append(" AND (c.course_name LIKE :keyword OR c.external_course_id LIKE :keyword OR c.source_value LIKE :keyword) ");
            parameters.addValue("keyword", "%" + keyword.trim() + "%");
        }
        Long total = namedJdbc.queryForObject("SELECT COUNT(*) FROM edu_course c " + where, parameters, Long.class);
        parameters.addValue("limit", Math.max(1, Math.min(pageSize, 100)));
        parameters.addValue("offset", Math.max(0, pageNum - 1) * Math.max(1, Math.min(pageSize, 100)));
        List<Map<String, Object>> rows = namedJdbc.query("""
                        SELECT c.*,
                          (SELECT COUNT(*) FROM edu_student_course sc WHERE sc.course_id=c.course_id AND sc.deleted=0) student_count,
                          (SELECT COUNT(*) FROM edu_material m WHERE m.course_id=c.course_id AND m.deleted=0) material_count,
                          (SELECT COUNT(*) FROM edu_treasure_batch tb WHERE tb.course_id=c.course_id AND tb.deleted=0) treasure_count
                        FROM edu_course c
                        """ + where + " ORDER BY c.update_time DESC, c.course_id DESC LIMIT :limit OFFSET :offset",
                parameters,
                (rs, rowNum) -> mapCourse(rs.getLong("course_id"), rs.getString("course_name"),
                        rs.getString("grade"), rs.getString("subject"), rs.getString("source_value"),
                        rs.getString("external_course_id"), rs.getString("external_course_url"),
                        rs.getString("lecturer_name"), rs.getString("lecturer_avatar_key"),
                        rs.getString("status"), rs.getLong("student_count"), rs.getLong("material_count"),
                        rs.getLong("treasure_count"), rs.getTimestamp("update_time").toLocalDateTime())
        );
        rows.forEach(row -> row.put("classTimes", schedules(((Number) row.get("courseId")).longValue())));
        return new PageResult(total == null ? 0 : total, rows);
    }

    public Map<String, Object> detail(long courseId) {
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                        SELECT * FROM edu_course WHERE course_id=? AND org_id=? AND deleted=0
                        """,
                (rs, rowNum) -> mapCourse(
                        rs.getLong("course_id"), rs.getString("course_name"),
                        rs.getString("grade"), rs.getString("subject"), rs.getString("source_value"),
                        rs.getString("external_course_id"), rs.getString("external_course_url"),
                        rs.getString("lecturer_name"), rs.getString("lecturer_avatar_key"),
                        rs.getString("status"), 0, 0, 0, rs.getTimestamp("update_time").toLocalDateTime()
                ),
                courseId, SecurityContext.orgId()
        );
        if (rows.isEmpty()) throw new BusinessException(ErrorCode.COURSE_NOT_FOUND, "课程不存在");
        rows.get(0).put("classTimes", schedules(courseId));
        return rows.get(0);
    }

    @Transactional
    public Map<String, Object> create(CourseDtos.SaveRequest request) {
        validateSchedules(request.classTimes());
        XiaoeCourseClient.CourseMetadata metadata = xiaoeCourseClient.validate(request.sourceValue());
        String courseName = gradeName(request.grade()) + subjectName(request.subject());
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                var statement = connection.prepareStatement("""
                                INSERT INTO edu_course(
                                    org_id, grade, subject, course_name, source_value, external_course_id,
                                    external_course_url, goods_img, lecturer_name, lecturer_avatar_key,
                                    status, create_by, update_by
                                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ENABLED', ?, ?)
                                """, Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, SecurityContext.orgId());
                statement.setString(2, request.grade());
                statement.setString(3, request.subject());
                statement.setString(4, courseName);
                statement.setString(5, request.sourceValue().trim());
                statement.setString(6, metadata.courseId());
                statement.setString(7, metadata.courseUrl());
                statement.setString(8, metadata.imageUrl());
                statement.setString(9, request.lecturerName().trim());
                statement.setString(10, request.lecturerAvatarUrl());
                statement.setString(11, SecurityContext.username());
                statement.setString(12, SecurityContext.username());
                return statement;
            }, keyHolder);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.COURSE_CONFLICT, "同一年级和学科的课程已存在");
        }
        Long courseId = GeneratedKeys.require(keyHolder, "course_id");
        saveSchedules(Objects.requireNonNull(courseId), request.classTimes());
        return Map.of(
                "courseId", courseId,
                "externalCourseId", metadata.courseId(),
                "scheduleIds", scheduleIds(courseId)
        );
    }

    @Transactional
    public void update(CourseDtos.SaveRequest request) {
        if (request.courseId() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "课程 ID 不能为空");
        detail(request.courseId());
        validateSchedules(request.classTimes());
        XiaoeCourseClient.CourseMetadata metadata = xiaoeCourseClient.validate(request.sourceValue());
        try {
            int updated = jdbcTemplate.update("""
                            UPDATE edu_course SET grade=?, subject=?, course_name=?, source_value=?,
                              external_course_id=?, external_course_url=?, lecturer_name=?,
                              lecturer_avatar_key=?, status=COALESCE(?, status), version=version+1,
                              update_by=?, update_time=CURRENT_TIMESTAMP
                            WHERE course_id=? AND org_id=? AND deleted=0
                            """,
                    request.grade(), request.subject(), gradeName(request.grade()) + subjectName(request.subject()),
                    request.sourceValue().trim(), metadata.courseId(), metadata.courseUrl(),
                    request.lecturerName().trim(), request.lecturerAvatarUrl(), request.status(),
                    SecurityContext.username(), request.courseId(), SecurityContext.orgId()
            );
            if (updated == 0) throw new BusinessException(ErrorCode.COURSE_NOT_FOUND, "课程不存在");
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.COURSE_CONFLICT, "同一年级和学科的课程已存在");
        }
        jdbcTemplate.update("DELETE FROM edu_course_schedule WHERE course_id=?", request.courseId());
        saveSchedules(request.courseId(), request.classTimes());
    }

    public void updateStatus(CourseDtos.StatusRequest request) {
        if (!List.of("ENABLED", "DISABLED").contains(request.status())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "课程状态不合法");
        }
        int updated = jdbcTemplate.update("""
                        UPDATE edu_course SET status=?, version=version+1, update_by=?, update_time=CURRENT_TIMESTAMP
                        WHERE course_id=? AND org_id=? AND deleted=0
                        """,
                request.status(), SecurityContext.username(), request.courseId(), SecurityContext.orgId());
        if (updated == 0) throw new BusinessException(ErrorCode.COURSE_NOT_FOUND, "课程不存在");
    }

    @Transactional
    public void delete(List<Long> courseIds) {
        for (Long courseId : courseIds) {
            detail(courseId);
            Map<String, Object> counts = jdbcTemplate.queryForMap("""
                            SELECT
                              (SELECT COUNT(*) FROM edu_student_course WHERE course_id=? AND deleted=0) studentCount,
                              (SELECT COUNT(*) FROM edu_material WHERE course_id=? AND deleted=0) materialCount,
                              (SELECT COUNT(*) FROM edu_treasure_batch WHERE course_id=? AND deleted=0) treasureBatchCount
                            """, courseId, courseId, courseId);
            long references = counts.values().stream().mapToLong(value -> ((Number) value).longValue()).sum();
            if (references > 0) {
                throw new BusinessException(ErrorCode.COURSE_REFERENCED,
                        "课程存在学员、资料或提分宝关联，无法删除");
            }
            jdbcTemplate.update("UPDATE edu_course SET deleted=1, active_key=NULL, update_time=CURRENT_TIMESTAMP WHERE course_id=?",
                    courseId);
        }
    }

    private void validateSchedules(List<CourseDtos.ScheduleRequest> schedules) {
        List<CourseDtos.ScheduleRequest> sorted = new ArrayList<>(schedules);
        sorted.sort((left, right) -> left.startTime().compareTo(right.startTime()));
        for (int index = 0; index < sorted.size(); index++) {
            var schedule = sorted.get(index);
            if (!schedule.endTime().isAfter(schedule.startTime())) {
                throw new BusinessException(ErrorCode.SCHEDULE_END_INVALID, "上课结束时间必须晚于开始时间");
            }
            if (index > 0 && schedule.startTime().isBefore(sorted.get(index - 1).endTime())) {
                throw new BusinessException(ErrorCode.SCHEDULE_OVERLAP, "上课时间存在重叠");
            }
        }
    }

    private void saveSchedules(long courseId, List<CourseDtos.ScheduleRequest> schedules) {
        for (CourseDtos.ScheduleRequest schedule : schedules) {
            jdbcTemplate.update("""
                            INSERT INTO edu_course_schedule(course_id, start_time, end_time)
                            VALUES (?, ?, ?)
                            """, courseId, Timestamp.valueOf(schedule.startTime()), Timestamp.valueOf(schedule.endTime()));
        }
    }

    private List<Map<String, Object>> schedules(long courseId) {
        return jdbcTemplate.query("""
                        SELECT schedule_id, start_time, end_time FROM edu_course_schedule
                        WHERE course_id=? AND deleted=0 ORDER BY start_time
                        """,
                (rs, rowNum) -> Map.of(
                        "scheduleId", rs.getLong("schedule_id"),
                        "startTime", rs.getTimestamp("start_time").toLocalDateTime(),
                        "endTime", rs.getTimestamp("end_time").toLocalDateTime()
                ), courseId);
    }

    private List<Long> scheduleIds(long courseId) {
        return jdbcTemplate.queryForList(
                "SELECT schedule_id FROM edu_course_schedule WHERE course_id=? ORDER BY schedule_id",
                Long.class, courseId);
    }

    private Map<String, Object> mapCourse(
            long id, String name, String grade, String subject, String source,
            String externalId, String externalUrl, String lecturer, String avatar,
            String status, long studentCount, long materialCount, long treasureCount,
            LocalDateTime updateTime
    ) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("courseId", id);
        row.put("courseName", name);
        row.put("grade", grade);
        row.put("gradeName", gradeName(grade));
        row.put("subject", subject);
        row.put("subjectName", subjectName(subject));
        row.put("sourceValue", source);
        row.put("externalCourseId", externalId);
        row.put("externalCourseUrl", externalUrl);
        row.put("lecturerName", lecturer);
        row.put("lecturerAvatarUrl", avatar);
        row.put("studentCount", studentCount);
        row.put("materialCount", materialCount);
        row.put("treasureBatchCount", treasureCount);
        row.put("status", status);
        row.put("updateTime", updateTime);
        return row;
    }

    private static Map<String, String> option(String label, String value) {
        return Map.of("label", label, "value", value);
    }

    public static String gradeName(String grade) {
        return switch (grade == null ? "" : grade.toUpperCase(Locale.ROOT)) {
            case "SENIOR_ONE" -> "高一";
            case "SENIOR_TWO" -> "高二";
            case "SENIOR_THREE" -> "高三";
            default -> grade;
        };
    }

    public static String subjectName(String subject) {
        return switch (subject == null ? "" : subject.toUpperCase(Locale.ROOT)) {
            case "MATH_ELITE" -> "数学培优";
            case "MATH_ADVANCE" -> "数学跃升";
            case "PHYSICS" -> "物理";
            case "ENGLISH" -> "英语";
            case "CHINESE" -> "语文";
            default -> subject;
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record PageResult(long total, List<Map<String, Object>> rows) {}
}
