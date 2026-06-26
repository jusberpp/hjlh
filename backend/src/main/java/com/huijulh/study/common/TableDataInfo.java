package com.huijulh.study.common;

import java.util.List;

public record TableDataInfo<T>(int code, String msg, long total, List<T> rows) {
    public static <T> TableDataInfo<T> of(long total, List<T> rows) {
        return new TableDataInfo<>(200, "查询成功", total, rows);
    }
}
