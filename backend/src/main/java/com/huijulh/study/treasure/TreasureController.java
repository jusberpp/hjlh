package com.huijulh.study.treasure;

import com.huijulh.study.common.ApiResponse;
import com.huijulh.study.common.TableDataInfo;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/course/treasure")
public class TreasureController {
    private final TreasureService treasureService;

    public TreasureController(TreasureService treasureService) {
        this.treasureService = treasureService;
    }

    @GetMapping("/homework-options")
    @PreAuthorize("@auth.has('course:treasure:add')")
    public ApiResponse<List<Map<String, Object>>> homeworkOptions(@RequestParam long courseId) {
        return ApiResponse.ok(treasureService.homeworkOptions(courseId));
    }

    @GetMapping("/list")
    @PreAuthorize("@auth.has('course:treasure:list')")
    public TableDataInfo<Map<String, Object>> list(
            @RequestParam long courseId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        TreasureService.PageResult result = treasureService.list(courseId, pageNum, pageSize);
        return TableDataInfo.of(result.total(), result.rows());
    }

    @PostMapping("/parse")
    @PreAuthorize("@auth.has('course:treasure:add')")
    public ApiResponse<Map<String, Object>> parse(
            @RequestParam long courseId,
            @RequestParam long homeworkMaterialId,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.ok(treasureService.parse(courseId, homeworkMaterialId, file));
    }

    @PostMapping
    @PreAuthorize("@auth.has('course:treasure:add')")
    public ApiResponse<Map<String, Object>> confirm(
            @Valid @RequestBody TreasureDtos.ConfirmRequest request
    ) {
        return ApiResponse.ok(treasureService.confirm(request));
    }

    @DeleteMapping("/{batchIds}")
    @PreAuthorize("@auth.has('course:treasure:remove')")
    public ApiResponse<Void> delete(@PathVariable String batchIds) {
        treasureService.delete(Arrays.stream(batchIds.split(",")).map(Long::parseLong).toList());
        return ApiResponse.ok();
    }
}
