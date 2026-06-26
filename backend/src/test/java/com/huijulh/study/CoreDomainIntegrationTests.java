package com.huijulh.study;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huijulh.study.common.BusinessException;
import com.huijulh.study.common.ErrorCode;
import com.huijulh.study.course.CourseDtos;
import com.huijulh.study.course.CourseService;
import com.huijulh.study.security.AdminPrincipal;
import com.huijulh.study.student.StudentDtos;
import com.huijulh.study.student.StudentService;
import com.huijulh.study.treasure.TreasureService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CoreDomainIntegrationTests {
    @Autowired
    CourseService courseService;
    @Autowired
    StudentService studentService;
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    TreasureService treasureService;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void authenticateAdmin() {
        AdminPrincipal principal = new AdminPrincipal(1, "admin", "系统管理员", 1, Set.of("*"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsOverlappingSchedules() {
        CourseDtos.SaveRequest request = new CourseDtos.SaveRequest(
                null, "SENIOR_ONE", "PHYSICS", "王老师", null, "physics-a1",
                List.of(
                        new CourseDtos.ScheduleRequest(null,
                                LocalDateTime.of(2026, 7, 1, 10, 0),
                                LocalDateTime.of(2026, 7, 1, 12, 0)),
                        new CourseDtos.ScheduleRequest(null,
                                LocalDateTime.of(2026, 7, 1, 11, 0),
                                LocalDateTime.of(2026, 7, 1, 13, 0))
                ), null
        );
        assertThatThrownBy(() -> courseService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo(ErrorCode.SCHEDULE_OVERLAP);
    }

    @Test
    void keepsExactlyOnePrimaryCourseWhenStudentHasMultipleCourses() {
        long physics = createCourse("SENIOR_ONE", "PHYSICS", "physics-a1");
        long chinese = createCourse("SENIOR_ONE", "CHINESE", "chinese-a1");
        long studentId = ((Number) studentService.create(new StudentDtos.SaveRequest(
                null, "测试中学", "SENIOR_ONE", "1班", "测试学生", "NO-001",
                null, List.of(physics, chinese), physics, "13812345678"
        )).get("studentId")).longValue();

        List<Map<String, Object>> courses = studentService.courseSummaries(studentId);
        assertThat(courses).hasSize(2);
        assertThat(courses.stream().filter(row -> Boolean.TRUE.equals(row.get("primary"))).count()).isEqualTo(1);
        assertThat(courses.stream().filter(row -> Boolean.TRUE.equals(row.get("primary")))
                .findFirst().orElseThrow().get("courseId")).isEqualTo(physics);

        studentService.update(new StudentDtos.SaveRequest(
                studentId, "测试中学", "SENIOR_ONE", "1班", "测试学生", "NO-001",
                null, List.of(chinese), null, "13812345678"
        ));
        List<Map<String, Object>> updated = studentService.courseSummaries(studentId);
        assertThat(updated).singleElement().satisfies(row -> {
            assertThat(row.get("courseId")).isEqualTo(chinese);
            assertThat(row.get("primary")).isEqualTo(true);
        });
    }

    @Test
    void oldAuthContractUsesTokenHeaderAndPrimaryCourse() throws Exception {
        long physics = createCourse("SENIOR_TWO", "PHYSICS", "physics-b1");
        long studentId = ((Number) studentService.create(new StudentDtos.SaveRequest(
                null, "兼容测试中学", "SENIOR_TWO", "2班", "李同学", "S-1001",
                "A", List.of(physics), physics, null
        )).get("studentId")).longValue();

        String verifyBody = objectMapper.writeValueAsString(Map.of(
                "school", "兼容测试中学",
                "grade", "高二",
                "name", "李同学",
                "studentId", "S-1001"
        ));
        String response = mockMvc.perform(post("/auth/verify-student")
                        .contentType("application/json").content(verifyBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentId").value("S-1001"))
                .andExpect(jsonPath("$.data.id").value(Long.toString(studentId)))
                .andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        String token = root.path("data").path("token").asText();

        mockMvc.perform(post("/auth/bind-phone")
                        .header("token", token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "id", Long.toString(studentId),
                                "phone", "13912345678"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(get("/auth/bind-result").header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("139****5678"))
                .andExpect(jsonPath("$.data.resourceId").value("physics-b1"));
    }

    @Test
    void adminLoginReturnsJwt() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(post("/login")
                        .contentType("application/json")
                        .content("{\"username\":\"admin\",\"password\":\"Admin123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void databaseConstraintRejectsPhoneBoundToAnotherStudent() {
        long physics = createCourse("SENIOR_THREE", "PHYSICS", "physics-c1");
        studentService.create(new StudentDtos.SaveRequest(
                null, "唯一性中学", "SENIOR_THREE", "1班", "甲同学", "U-001",
                null, List.of(physics), physics, "13712345678"
        ));
        assertThatThrownBy(() -> studentService.create(new StudentDtos.SaveRequest(
                null, "唯一性中学", "SENIOR_THREE", "1班", "乙同学", "U-002",
                null, List.of(physics), physics, "13712345678"
        ))).isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo(ErrorCode.PHONE_CONFLICT);
    }

    @Test
    void treasureParserRejectsZipPathTraversal() throws Exception {
        long physics = createCourse("SENIOR_THREE", "PHYSICS", "physics-c2");
        jdbcTemplate.update("""
                        INSERT INTO edu_material(
                          org_id, course_id, material_type, file_name, storage_key,
                          file_size, file_extension, mime_type, open_time, status
                        ) VALUES (1, ?, 'HOMEWORK', '测试作业.pdf', 'unused.pdf',
                          1, 'pdf', 'application/pdf', CURRENT_TIMESTAMP, 'ENABLED')
                        """, physics);
        long materialId = jdbcTemplate.queryForObject(
                "SELECT MAX(material_id) FROM edu_material", Long.class);
        byte[] zip = zipWithEntry("../evil.pdf", "%PDF-test".getBytes());
        MockMultipartFile file = new MockMultipartFile(
                "file", "unsafe.zip", "application/zip", zip);

        assertThatThrownBy(() -> treasureService.parse(physics, materialId, file))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo(42202);
    }

    private long createCourse(String grade, String subject, String externalId) {
        return ((Number) courseService.create(new CourseDtos.SaveRequest(
                null, grade, subject, "测试讲师", null, externalId,
                List.of(new CourseDtos.ScheduleRequest(null,
                        LocalDateTime.of(2026, 7, 2, 10, 0),
                        LocalDateTime.of(2026, 7, 2, 12, 0))),
                null
        )).get("courseId")).longValue();
    }

    private byte[] zipWithEntry(String name, byte[] content) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(content);
            zip.closeEntry();
        }
        return output.toByteArray();
    }
}
