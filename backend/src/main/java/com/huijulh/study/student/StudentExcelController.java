package com.huijulh.study.student;

import com.huijulh.study.common.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/study/student")
@Validated
public class StudentExcelController {
    private static final String XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private final StudentExcelService excelService;

    public StudentExcelController(StudentExcelService excelService) {
        this.excelService = excelService;
    }

    @PostMapping("/import-template")
    @PreAuthorize("@auth.has('study:student:import')")
    public void template(HttpServletResponse response) throws IOException {
        writeExcel(response, "学员信息导入模板.xlsx", excelService.template());
    }

    @PostMapping("/import/validate")
    @PreAuthorize("@auth.has('study:student:import')")
    public ApiResponse<Map<String, Object>> validate(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(excelService.validate(file));
    }

    @PostMapping("/import/confirm")
    @PreAuthorize("@auth.has('study:student:import')")
    public ApiResponse<Map<String, Object>> confirm(@Valid @RequestBody ConfirmRequest request) {
        return ApiResponse.ok(excelService.confirm(
                request.importToken(), Boolean.TRUE.equals(request.updateSupport())));
    }

    @PostMapping("/export")
    @PreAuthorize("@auth.has('study:student:export')")
    public void export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String phoneStatus,
            HttpServletResponse response
    ) throws IOException {
        writeExcel(response, "学员信息.xlsx", excelService.export(keyword, grade, courseId, phoneStatus));
    }

    private void writeExcel(HttpServletResponse response, String filename, byte[] content) throws IOException {
        response.setContentType(XLSX);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString());
        response.getOutputStream().write(content);
    }

    public record ConfirmRequest(@NotBlank String importToken, Boolean updateSupport) {}
}
