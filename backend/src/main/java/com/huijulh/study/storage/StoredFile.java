package com.huijulh.study.storage;

public record StoredFile(
        String storageKey,
        String originalName,
        long size,
        String extension,
        String mimeType
) {}
