package com.huijulh.study.common;

public record ApiResponse<T>(int code, String msg, T data) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(200, "操作成功", null);
    }
}
