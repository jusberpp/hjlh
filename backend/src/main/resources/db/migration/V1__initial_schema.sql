CREATE TABLE sys_user (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    org_id BIGINT NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    permissions VARCHAR(2000) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_user_username (username)
);

CREATE TABLE edu_course (
    course_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_id BIGINT NOT NULL,
    grade VARCHAR(32) NOT NULL,
    subject VARCHAR(32) NOT NULL,
    course_name VARCHAR(100) NOT NULL,
    source_value VARCHAR(500) NOT NULL,
    external_course_id VARCHAR(128) NOT NULL,
    external_course_url VARCHAR(500),
    goods_img VARCHAR(500),
    lecturer_name VARCHAR(30) NOT NULL,
    lecturer_avatar_key VARCHAR(500),
    learning_levels VARCHAR(64),
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    sort_order INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    active_key TINYINT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_by VARCHAR(64),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500),
    UNIQUE KEY uk_course_org_grade_subject_active (org_id, grade, subject, active_key),
    KEY idx_course_org_status (org_id, status, deleted)
);

CREATE TABLE edu_course_schedule (
    schedule_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_schedule_course_time (course_id, start_time, deleted),
    CONSTRAINT fk_schedule_course FOREIGN KEY (course_id) REFERENCES edu_course(course_id)
);

CREATE TABLE edu_student (
    student_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_id BIGINT NOT NULL,
    school VARCHAR(100) NOT NULL,
    grade VARCHAR(32) NOT NULL,
    class_name VARCHAR(50) NOT NULL,
    student_name VARCHAR(50) NOT NULL,
    student_no VARCHAR(64) NOT NULL,
    learning_levels VARCHAR(64),
    authorized_phone VARCHAR(255),
    phone_hash CHAR(64),
    phone_source VARCHAR(32),
    phone_verified_at TIMESTAMP NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    version INT NOT NULL DEFAULT 0,
    active_key TINYINT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_by VARCHAR(64),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500),
    UNIQUE KEY uk_student_org_no_active (org_id, student_no, active_key),
    UNIQUE KEY uk_student_phone_hash_active (phone_hash, active_key),
    KEY idx_student_search (org_id, grade, school, deleted)
);

CREATE TABLE edu_student_course (
    relation_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    is_primary TINYINT NOT NULL DEFAULT 0,
    authorized_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source VARCHAR(32) NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_student_course_deleted (student_id, course_id, deleted),
    KEY idx_student_primary (student_id, is_primary, deleted),
    KEY idx_course_students (course_id, deleted),
    CONSTRAINT fk_student_course_student FOREIGN KEY (student_id) REFERENCES edu_student(student_id),
    CONSTRAINT fk_student_course_course FOREIGN KEY (course_id) REFERENCES edu_course(course_id)
);

CREATE TABLE edu_material (
    material_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    material_type VARCHAR(16) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_extension VARCHAR(20) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    open_time TIMESTAMP NOT NULL,
    submit_deadline TIMESTAMP NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_by VARCHAR(64),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_material_course_open (course_id, open_time, deleted),
    CONSTRAINT fk_material_course FOREIGN KEY (course_id) REFERENCES edu_course(course_id)
);

CREATE TABLE edu_homework_question (
    question_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    question_no INT NOT NULL,
    full_score DECIMAL(6,2) NOT NULL,
    required_flag TINYINT NOT NULL DEFAULT 1,
    allow_decimal TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_question_material_no_deleted (material_id, question_no, deleted),
    CONSTRAINT fk_question_material FOREIGN KEY (material_id) REFERENCES edu_material(material_id)
);

CREATE TABLE edu_homework_submission (
    submission_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    submitted_at TIMESTAMP NULL,
    idempotency_key VARCHAR(128),
    active_flag TINYINT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_submission_active (student_id, material_id, active_flag),
    CONSTRAINT fk_submission_student FOREIGN KEY (student_id) REFERENCES edu_student(student_id),
    CONSTRAINT fk_submission_material FOREIGN KEY (material_id) REFERENCES edu_material(material_id)
);

CREATE TABLE edu_submission_score (
    score_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    submission_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    score DECIMAL(6,2) NOT NULL,
    UNIQUE KEY uk_submission_question (submission_id, question_id),
    CONSTRAINT fk_score_submission FOREIGN KEY (submission_id) REFERENCES edu_homework_submission(submission_id),
    CONSTRAINT fk_score_question FOREIGN KEY (question_id) REFERENCES edu_homework_question(question_id)
);

CREATE TABLE edu_treasure_batch (
    batch_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    homework_material_id BIGINT NOT NULL,
    source_zip_name VARCHAR(255) NOT NULL,
    source_zip_storage_key VARCHAR(500),
    parse_status VARCHAR(20) NOT NULL,
    publish_status VARCHAR(20) NOT NULL,
    parsed_file_count INT NOT NULL DEFAULT 0,
    published_at TIMESTAMP NULL,
    active_flag TINYINT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_by VARCHAR(64),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_treasure_homework_active (homework_material_id, active_flag),
    KEY idx_treasure_course (course_id, deleted),
    CONSTRAINT fk_treasure_course FOREIGN KEY (course_id) REFERENCES edu_course(course_id),
    CONSTRAINT fk_treasure_homework FOREIGN KEY (homework_material_id) REFERENCES edu_material(material_id)
);

CREATE TABLE edu_treasure_file (
    treasure_file_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    student_no_snapshot VARCHAR(64) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_hash CHAR(64) NOT NULL,
    file_version INT NOT NULL DEFAULT 1,
    publish_status VARCHAR(20) NOT NULL,
    expire_time TIMESTAMP NULL,
    download_count INT NOT NULL DEFAULT 0,
    last_download_time TIMESTAMP NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_treasure_batch_student_deleted (batch_id, student_id, deleted),
    CONSTRAINT fk_treasure_file_batch FOREIGN KEY (batch_id) REFERENCES edu_treasure_batch(batch_id),
    CONSTRAINT fk_treasure_file_student FOREIGN KEY (student_id) REFERENCES edu_student(student_id)
);

CREATE TABLE edu_file_token (
    token_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token_value CHAR(64) NOT NULL,
    business_type VARCHAR(32) NOT NULL,
    operator_id BIGINT NOT NULL,
    org_id BIGINT NOT NULL,
    payload_json TEXT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_file_token_value (token_value)
);

CREATE TABLE edu_phone_bind_log (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    old_phone_masked VARCHAR(32),
    new_phone_masked VARCHAR(32),
    source VARCHAR(32) NOT NULL,
    operator VARCHAR(64),
    device_summary VARCHAR(255),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_phone_log_student (student_id, create_time)
);

CREATE TABLE edu_import_batch (
    batch_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token_value CHAR(64) NOT NULL,
    org_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    payload_json TEXT NOT NULL,
    total_count INT NOT NULL,
    valid_count INT NOT NULL,
    invalid_count INT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    create_by VARCHAR(64),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_import_token (token_value)
);

CREATE TABLE edu_import_error (
    error_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    row_num INT NOT NULL,
    field_name VARCHAR(64),
    field_value VARCHAR(500),
    message VARCHAR(500) NOT NULL,
    CONSTRAINT fk_import_error_batch FOREIGN KEY (batch_id) REFERENCES edu_import_batch(batch_id)
);

CREATE TABLE edu_treasure_parse_error (
    error_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token_value CHAR(64) NOT NULL,
    file_path VARCHAR(500),
    student_no VARCHAR(64),
    message VARCHAR(500) NOT NULL
);

CREATE TABLE edu_file_cleanup_task (
    task_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    storage_key VARCHAR(500) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_time TIMESTAMP NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE edu_xiaoe_request_log (
    request_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    idempotency_key VARCHAR(128) NOT NULL,
    operation VARCHAR(32) NOT NULL,
    student_id BIGINT,
    course_id BIGINT,
    request_summary VARCHAR(1000),
    response_summary VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    duration_ms BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_xiaoe_idempotency (idempotency_key)
);

INSERT INTO sys_user(username, password_hash, display_name, org_id, enabled, permissions)
VALUES (
    'admin',
    '{bcrypt}$2a$10$X/MknTo5BvzYgUO.nVNnAuUXZPOx58VhHwA2sRk8hIRS4rLVCZ1ii',
    '系统管理员',
    1,
    1,
    '*'
);
