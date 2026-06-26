package com.huijulh.study.common;

public final class ErrorCode {
    private ErrorCode() {}

    public static final int BAD_REQUEST = 40001;
    public static final int INVALID_PHONE = 40002;
    public static final int COURSE_REQUIRED = 40003;
    public static final int STUDENT_NOT_FOUND = 40401;
    public static final int COURSE_NOT_FOUND = 40402;
    public static final int MATERIAL_NOT_FOUND = 40403;
    public static final int TREASURE_NOT_FOUND = 40404;
    public static final int COURSE_REFERENCED = 40901;
    public static final int HOMEWORK_STRUCTURE_LOCKED = 40902;
    public static final int STUDENT_NO_CONFLICT = 40903;
    public static final int COURSE_CONFLICT = 40904;
    public static final int PHONE_CONFLICT = 40906;
    public static final int TREASURE_CONFLICT = 40907;
    public static final int INVALID_XIAOE_COURSE = 42203;
    public static final int SCHEDULE_END_INVALID = 42206;
    public static final int SCHEDULE_OVERLAP = 42207;
    public static final int MATERIAL_NOT_OPEN = 42211;
    public static final int AVATAR_INVALID = 42212;
    public static final int HOMEWORK_NOT_OPEN = 42214;
    public static final int IMPORT_TOKEN_EXPIRED = 41001;
    public static final int UPLOAD_TOKEN_EXPIRED = 41002;
    public static final int TREASURE_TOKEN_EXPIRED = 41003;
}
