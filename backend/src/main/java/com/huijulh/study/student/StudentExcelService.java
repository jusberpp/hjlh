package com.huijulh.study.student;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huijulh.study.common.BusinessException;
import com.huijulh.study.common.ErrorCode;
import com.huijulh.study.course.CourseService;
import com.huijulh.study.security.SecurityContext;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class StudentExcelService {
    private static final List<String> HEADERS =
            List.of("学校", "年级", "班级", "姓名", "学号", "授权课程", "授权手机号");
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final StudentService studentService;

    public StudentExcelService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            StudentService studentService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.studentService = studentService;
    }

    public byte[] template() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("学员信息");
            Row header = sheet.createRow(0);
            for (int index = 0; index < HEADERS.size(); index++) header.createCell(index).setCellValue(HEADERS.get(index));
            Row example = sheet.createRow(1);
            List<String> values = List.of(
                    "北京市海淀实验中学", "高一", "3班", "张晨",
                    "2026010318", "高一数学培优、高一物理", "13812346021");
            for (int index = 0; index < values.size(); index++) example.createCell(index).setCellValue(values.get(index));
            for (int index = 0; index < HEADERS.size(); index++) sheet.autoSizeColumn(index);
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot create import template", exception);
        }
    }

    public Map<String, Object> validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 10L * 1024 * 1024) {
            throw new BusinessException(42201, "Excel 文件为空或超过 10MB");
        }
        Map<String, Long> courseByName = new HashMap<>();
        jdbcTemplate.query("""
                        SELECT course_id, course_name FROM edu_course
                        WHERE org_id=? AND status='ENABLED' AND deleted=0
                        """, (RowCallbackHandler) rs ->
                        courseByName.put(rs.getString("course_name"), rs.getLong("course_id")),
                SecurityContext.orgId());
        List<Map<String, Object>> validRows = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        int emptyPhoneCount = 0;
        DataFormatter formatter = new DataFormatter();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            validateHeaders(sheet.getRow(0), formatter);
            if (sheet.getLastRowNum() > 2000) throw new BusinessException(42201, "单次导入最多 2000 条");
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRow(row, formatter)) continue;
                Map<String, String> raw = new LinkedHashMap<>();
                for (int column = 0; column < HEADERS.size(); column++) {
                    raw.put(HEADERS.get(column), value(row.getCell(column), formatter));
                }
                List<Map<String, Object>> rowErrors = validateRow(rowIndex + 1, raw, courseByName);
                errors.addAll(rowErrors);
                if (rowErrors.isEmpty()) {
                    List<Long> courseIds = Arrays.stream(raw.get("授权课程").split("[、,，]"))
                            .map(String::trim).map(courseByName::get).toList();
                    Map<String, Object> parsed = new LinkedHashMap<>();
                    parsed.put("school", raw.get("学校"));
                    parsed.put("grade", gradeCode(raw.get("年级")));
                    parsed.put("className", raw.get("班级"));
                    parsed.put("studentName", raw.get("姓名"));
                    parsed.put("studentNo", raw.get("学号"));
                    parsed.put("courseIds", courseIds);
                    parsed.put("authorizedPhone", raw.get("授权手机号").isBlank() ? null : raw.get("授权手机号"));
                    validRows.add(parsed);
                    if (raw.get("授权手机号").isBlank()) emptyPhoneCount++;
                }
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(42201, "Excel 文件无法解析");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plus(30, ChronoUnit.MINUTES);
        try {
            jdbcTemplate.update("""
                            INSERT INTO edu_import_batch(
                              token_value, org_id, file_name, payload_json, total_count,
                              valid_count, invalid_count, expires_at, create_by
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """, token, SecurityContext.orgId(), safeName(file.getOriginalFilename()),
                    objectMapper.writeValueAsString(validRows), validRows.size() + errors.size(),
                    validRows.size(), errors.size(), Timestamp.from(expiresAt), SecurityContext.username());
            Long batchId = jdbcTemplate.queryForObject(
                    "SELECT batch_id FROM edu_import_batch WHERE token_value=?", Long.class, token);
            for (Map<String, Object> error : errors) {
                jdbcTemplate.update("""
                                INSERT INTO edu_import_error(batch_id, row_num, field_name, field_value, message)
                                VALUES (?, ?, ?, ?, ?)
                                """, batchId, error.get("rowNum"), error.get("field"),
                        error.get("value"), error.get("message"));
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot persist import validation", exception);
        }
        return Map.of(
                "importToken", token,
                "fileName", safeName(file.getOriginalFilename()),
                "totalCount", validRows.size() + errors.size(),
                "validCount", validRows.size(),
                "invalidCount", errors.size(),
                "emptyPhoneCount", emptyPhoneCount,
                "canImport", errors.isEmpty() && !validRows.isEmpty(),
                "errors", errors
        );
    }

    @Transactional
    public Map<String, Object> confirm(String token, boolean updateSupport) {
        List<ImportBatch> batches = jdbcTemplate.query("""
                        SELECT batch_id, payload_json, invalid_count FROM edu_import_batch
                        WHERE token_value=? AND org_id=? AND used_at IS NULL
                          AND expires_at>CURRENT_TIMESTAMP FOR UPDATE
                        """, (rs, rowNum) -> new ImportBatch(
                        rs.getLong("batch_id"), rs.getString("payload_json"), rs.getInt("invalid_count")),
                token, SecurityContext.orgId());
        if (batches.isEmpty()) {
            throw new BusinessException(ErrorCode.IMPORT_TOKEN_EXPIRED, "导入令牌已过期或已使用");
        }
        ImportBatch batch = batches.get(0);
        if (batch.invalidCount() > 0) throw new BusinessException(42201, "导入文件仍存在错误");
        try {
            List<Map<String, Object>> rows =
                    objectMapper.readValue(batch.payloadJson(), new TypeReference<>() {});
            int created = 0;
            int updated = 0;
            for (Map<String, Object> row : rows) {
                String studentNo = (String) row.get("studentNo");
                List<Long> existing = jdbcTemplate.queryForList("""
                                SELECT student_id FROM edu_student
                                WHERE org_id=? AND student_no=? AND deleted=0
                                """, Long.class, SecurityContext.orgId(), studentNo);
                List<Long> courseIds = ((List<?>) row.get("courseIds")).stream()
                        .map(value -> ((Number) value).longValue()).toList();
                StudentDtos.SaveRequest request = new StudentDtos.SaveRequest(
                        existing.isEmpty() ? null : existing.get(0),
                        (String) row.get("school"), (String) row.get("grade"),
                        (String) row.get("className"), (String) row.get("studentName"),
                        studentNo, null, courseIds, courseIds.get(0),
                        (String) row.get("authorizedPhone")
                );
                if (existing.isEmpty()) {
                    studentService.create(request);
                    created++;
                } else if (updateSupport) {
                    studentService.update(request);
                    updated++;
                } else {
                    throw new BusinessException(ErrorCode.STUDENT_NO_CONFLICT,
                            "学号 " + studentNo + " 已存在");
                }
            }
            jdbcTemplate.update("UPDATE edu_import_batch SET used_at=CURRENT_TIMESTAMP WHERE batch_id=?",
                    batch.batchId());
            return Map.of("successCount", created + updated, "updateCount", updated, "failureCount", 0);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot confirm import", exception);
        }
    }

    public byte[] export(String keyword, String grade, Long courseId, String phoneStatus) {
        List<Map<String, Object>> rows = new ArrayList<>();
        int page = 1;
        while (true) {
            StudentService.PageResult result =
                    studentService.list(keyword, grade, courseId, phoneStatus, page, 100);
            rows.addAll(result.rows());
            if (rows.size() >= result.total()) break;
            page++;
        }
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("学员");
            List<String> headers = List.of("学校", "年级", "班级", "姓名", "学号", "授权课程", "手机号", "手机号来源");
            Row header = sheet.createRow(0);
            for (int index = 0; index < headers.size(); index++) header.createCell(index).setCellValue(headers.get(index));
            for (int index = 0; index < rows.size(); index++) {
                Map<String, Object> data = rows.get(index);
                Row row = sheet.createRow(index + 1);
                row.createCell(0).setCellValue(string(data.get("school")));
                row.createCell(1).setCellValue(string(data.get("gradeName")));
                row.createCell(2).setCellValue(string(data.get("className")));
                row.createCell(3).setCellValue(string(data.get("studentName")));
                row.createCell(4).setCellValue(string(data.get("studentNo")));
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> courses = (List<Map<String, Object>>) data.get("courses");
                row.createCell(5).setCellValue(courses.stream()
                        .map(course -> string(course.get("courseName"))).reduce((a, b) -> a + "、" + b).orElse(""));
                row.createCell(6).setCellValue(string(data.get("phoneMasked")));
                row.createCell(7).setCellValue(string(data.get("phoneSource")));
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot export students", exception);
        }
    }

    private List<Map<String, Object>> validateRow(
            int rowNum,
            Map<String, String> row,
            Map<String, Long> courseByName
    ) {
        List<Map<String, Object>> errors = new ArrayList<>();
        for (String required : List.of("学校", "年级", "班级", "姓名", "学号", "授权课程")) {
            if (row.get(required).isBlank()) errors.add(error(rowNum, required, "", "字段不能为空"));
        }
        if (!List.of("高一", "高二", "高三", "SENIOR_ONE", "SENIOR_TWO", "SENIOR_THREE")
                .contains(row.get("年级"))) {
            errors.add(error(rowNum, "年级", row.get("年级"), "年级值不合法"));
        }
        for (String courseName : row.get("授权课程").split("[、,，]")) {
            if (!courseByName.containsKey(courseName.trim())) {
                errors.add(error(rowNum, "授权课程", courseName, "课程不存在或未启用"));
            }
        }
        if (!row.get("授权手机号").isBlank() && !row.get("授权手机号").matches("^1\\d{10}$")) {
            errors.add(error(rowNum, "授权手机号", row.get("授权手机号"), "手机号格式错误"));
        }
        return errors;
    }

    private void validateHeaders(Row row, DataFormatter formatter) {
        if (row == null) throw new BusinessException(42201, "Excel 缺少表头");
        for (int index = 0; index < HEADERS.size(); index++) {
            if (!HEADERS.get(index).equals(value(row.getCell(index), formatter))) {
                throw new BusinessException(42201, "Excel 表头不正确，请使用最新模板");
            }
        }
    }

    private static boolean isBlankRow(Row row, DataFormatter formatter) {
        for (int index = 0; index < HEADERS.size(); index++) {
            if (!value(row.getCell(index), formatter).isBlank()) return false;
        }
        return true;
    }

    private static String value(Cell cell, DataFormatter formatter) {
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private static String gradeCode(String grade) {
        return switch (grade) {
            case "高一" -> "SENIOR_ONE";
            case "高二" -> "SENIOR_TWO";
            case "高三" -> "SENIOR_THREE";
            default -> grade;
        };
    }

    private static Map<String, Object> error(int row, String field, String value, String message) {
        return Map.of("rowNum", row, "field", field, "value", value, "message", message);
    }

    private static String safeName(String name) {
        return name == null ? "学员导入.xlsx" : name.replace("\r", "").replace("\n", "");
    }

    private static String string(Object value) {
        return value == null ? "" : value.toString();
    }

    private record ImportBatch(long batchId, String payloadJson, int invalidCount) {}
}
