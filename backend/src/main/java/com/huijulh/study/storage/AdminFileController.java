package com.huijulh.study.storage;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/common")
public class AdminFileController {
    private final LocalFileStorage storage;

    public AdminFileController(LocalFileStorage storage) {
        this.storage = storage;
    }

    @GetMapping("/files")
    public ResponseEntity<Resource> file(@RequestParam String key) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .body(storage.load(key));
    }
}
