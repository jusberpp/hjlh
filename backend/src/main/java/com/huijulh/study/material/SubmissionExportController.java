package com.huijulh.study.material;

import com.huijulh.study.common.BusinessException;
import com.huijulh.study.common.ErrorCode;
import com.huijulh.study.security.SecurityContext;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/course/material")
public class SubmissionExportController {
    private static final String XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private final JdbcTemplate jdbcTemplate;

    public SubmissionExportController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/{materialId}/submission/export")
    @PreAuthorize("@auth.has('course:material:export')")
    public void export(@PathVariable long materialId, HttpServletResponse response) throws IOException {
        Material material = material(materialId);
        List<Integer> questionNos = jdbcTemplate.queryForList("""
                        SELECT question_no FROM edu_homework_question
                        WHERE material_id=? AND deleted=0 ORDER BY question_no
                        """, Integer.class, materialId);
        if (!"PHYSICS".equals(material.subject()) || !"HOMEWORK".equals(material.materialType())
                || questionNos.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅已配置小题分的物理作业可以导出");
        }
        List<StudentSubmission> students = jdbcTemplate.query("""
                        SELECT s.student_id, s.school, s.grade, s.class_name, s.student_name, s.student_no,
                          hs.submission_id, hs.submitted_at
                        FROM edu_student_course sc
                        JOIN edu_student s ON s.student_id=sc.student_id AND s.deleted=0
                        LEFT JOIN edu_homework_submission hs ON hs.student_id=s.student_id
                          AND hs.material_id=? AND hs.active_flag=1
                        WHERE sc.course_id=? AND sc.deleted=0
                        ORDER BY s.school, s.class_name, s.student_no
                        """, (rs, rowNum) -> new StudentSubmission(
                        rs.getLong("student_id"), rs.getString("school"), rs.getString("grade"),
                        rs.getString("class_name"), rs.getString("student_name"), rs.getString("student_no"),
                        rs.getObject("submission_id") == null ? null : rs.getLong("submission_id"),
                        rs.getTimestamp("submitted_at") == null ? null : rs.getTimestamp("submitted_at").toLocalDateTime()
                ), materialId, material.courseId());
        byte[] bytes = workbook(students, questionNos);
        response.setContentType(XLSX);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                        .filename(material.courseName() + "-" + material.fileName() + "-小题分提交数据.xlsx",
                                StandardCharsets.UTF_8)
                        .build().toString());
        response.getOutputStream().write(bytes);
    }

    private byte[] workbook(List<StudentSubmission> students, List<Integer> questionNos) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("小题分");
            List<String> headers = new ArrayList<>(List.of(
                    "学校", "年级", "班级", "姓名", "学号", "提交状态", "提交时间"));
            questionNos.forEach(no -> headers.add("第 " + no + " 题得分"));
            headers.add("总得分");
            Row header = sheet.createRow(0);
            for (int index = 0; index < headers.size(); index++) header.createCell(index).setCellValue(headers.get(index));
            for (int rowIndex = 0; rowIndex < students.size(); rowIndex++) {
                StudentSubmission student = students.get(rowIndex);
                Map<Integer, BigDecimal> scores = scores(student.submissionId());
                Row row = sheet.createRow(rowIndex + 1);
                row.createCell(0).setCellValue(student.school());
                row.createCell(1).setCellValue(student.grade());
                row.createCell(2).setCellValue(student.className());
                row.createCell(3).setCellValue(student.studentName());
                row.createCell(4).setCellValue(student.studentNo());
                row.createCell(5).setCellValue(student.submissionId() == null ? "未提交" : "已提交");
                row.createCell(6).setCellValue(student.submittedAt() == null ? "" : student.submittedAt().toString());
                BigDecimal total = BigDecimal.ZERO;
                for (int index = 0; index < questionNos.size(); index++) {
                    BigDecimal score = scores.get(questionNos.get(index));
                    if (score != null) {
                        row.createCell(7 + index).setCellValue(score.doubleValue());
                        total = total.add(score);
                    }
                }
                row.createCell(7 + questionNos.size()).setCellValue(total.doubleValue());
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot export submission scores", exception);
        }
    }

    private Map<Integer, BigDecimal> scores(Long submissionId) {
        if (submissionId == null) return Map.of();
        Map<Integer, BigDecimal> scores = new LinkedHashMap<>();
        jdbcTemplate.query("""
                        SELECT q.question_no, ss.score
                        FROM edu_submission_score ss
                        JOIN edu_homework_question q ON q.question_id=ss.question_id
                        WHERE ss.submission_id=?
                        """, (RowCallbackHandler) rs ->
                        scores.put(rs.getInt("question_no"), rs.getBigDecimal("score")), submissionId);
        return scores;
    }

    private Material material(long materialId) {
        List<Material> rows = jdbcTemplate.query("""
                        SELECT m.course_id, m.material_type, m.file_name, c.subject, c.course_name
                        FROM edu_material m JOIN edu_course c ON c.course_id=m.course_id
                        WHERE m.material_id=? AND m.org_id=? AND m.deleted=0
                        """, (rs, rowNum) -> new Material(
                        rs.getLong("course_id"), rs.getString("material_type"),
                        rs.getString("file_name"), rs.getString("subject"), rs.getString("course_name")),
                materialId, SecurityContext.orgId());
        if (rows.isEmpty()) throw new BusinessException(ErrorCode.MATERIAL_NOT_FOUND, "资料不存在");
        return rows.get(0);
    }

    private record Material(
            long courseId, String materialType, String fileName, String subject, String courseName
    ) {}

    private record StudentSubmission(
            long studentId, String school, String grade, String className,
            String studentName, String studentNo, Long submissionId,
            java.time.LocalDateTime submittedAt
    ) {}
}
