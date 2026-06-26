package com.huijulh.study.integration.xiaoe;

public interface XiaoeCourseClient {
    CourseMetadata validate(String sourceValue);

    record CourseMetadata(String courseId, String courseUrl, String courseName, String imageUrl) {}
}
