CREATE TABLE sys_dict_type (
    dict_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dict_name VARCHAR(100) NOT NULL,
    dict_type VARCHAR(100) NOT NULL,
    status CHAR(1) NOT NULL DEFAULT '0',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dict_type (dict_type)
);

CREATE TABLE sys_dict_data (
    dict_code BIGINT PRIMARY KEY AUTO_INCREMENT,
    dict_sort INT NOT NULL DEFAULT 0,
    dict_label VARCHAR(100) NOT NULL,
    dict_value VARCHAR(100) NOT NULL,
    dict_type VARCHAR(100) NOT NULL,
    status CHAR(1) NOT NULL DEFAULT '0',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dict_value (dict_type, dict_value)
);

CREATE TABLE sys_menu (
    menu_id BIGINT PRIMARY KEY,
    menu_name VARCHAR(100) NOT NULL,
    parent_id BIGINT NOT NULL DEFAULT 0,
    order_num INT NOT NULL DEFAULT 0,
    path VARCHAR(200),
    component VARCHAR(255),
    menu_type CHAR(1) NOT NULL,
    perms VARCHAR(100),
    visible CHAR(1) NOT NULL DEFAULT '0',
    status CHAR(1) NOT NULL DEFAULT '0'
);

INSERT INTO sys_dict_type(dict_name, dict_type) VALUES
('年级', 'edu_grade'),
('学科', 'edu_subject'),
('资料类型', 'edu_material_type'),
('课程状态', 'edu_course_status');

INSERT INTO sys_dict_data(dict_sort, dict_label, dict_value, dict_type) VALUES
(1, '高一', 'SENIOR_ONE', 'edu_grade'),
(2, '高二', 'SENIOR_TWO', 'edu_grade'),
(3, '高三', 'SENIOR_THREE', 'edu_grade'),
(1, '数学培优', 'MATH_ELITE', 'edu_subject'),
(2, '数学跃升', 'MATH_ADVANCE', 'edu_subject'),
(3, '物理', 'PHYSICS', 'edu_subject'),
(4, '英语', 'ENGLISH', 'edu_subject'),
(5, '语文', 'CHINESE', 'edu_subject'),
(1, '讲义', 'HANDOUT', 'edu_material_type'),
(2, '作业', 'HOMEWORK', 'edu_material_type'),
(1, '已启用', 'ENABLED', 'edu_course_status'),
(2, '已停用', 'DISABLED', 'edu_course_status');

INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, menu_type, perms) VALUES
(2000, '学习信息管理', 0, 10, 'study', 'Layout', 'M', NULL),
(2001, '学员信息管理', 2000, 1, 'students', 'study/student/index', 'C', 'study:student:list'),
(2010, '学科课程管理', 0, 11, 'course', 'Layout', 'M', NULL),
(2011, '课程管理', 2010, 1, 'list', 'course/course/index', 'C', 'course:course:list'),
(2012, '课程资料管理', 2010, 2, 'materials', 'course/material/index', 'C', 'course:material:list'),
(2101, '学员查询', 2001, 1, '#', '', 'F', 'study:student:query'),
(2102, '学员新增', 2001, 2, '#', '', 'F', 'study:student:add'),
(2103, '学员修改', 2001, 3, '#', '', 'F', 'study:student:edit'),
(2104, '学员导入', 2001, 4, '#', '', 'F', 'study:student:import'),
(2105, '学员导出', 2001, 5, '#', '', 'F', 'study:student:export'),
(2111, '课程查询', 2011, 1, '#', '', 'F', 'course:course:query'),
(2112, '课程新增', 2011, 2, '#', '', 'F', 'course:course:add'),
(2113, '课程修改', 2011, 3, '#', '', 'F', 'course:course:edit'),
(2114, '课程删除', 2011, 4, '#', '', 'F', 'course:course:remove'),
(2121, '资料查询', 2012, 1, '#', '', 'F', 'course:material:query'),
(2122, '资料新增', 2012, 2, '#', '', 'F', 'course:material:add'),
(2123, '资料修改', 2012, 3, '#', '', 'F', 'course:material:edit'),
(2124, '资料删除', 2012, 4, '#', '', 'F', 'course:material:remove'),
(2125, '小题分导出', 2012, 5, '#', '', 'F', 'course:material:export'),
(2131, '提分宝列表', 2012, 6, '#', '', 'F', 'course:treasure:list'),
(2132, '提分宝上传', 2012, 7, '#', '', 'F', 'course:treasure:add'),
(2133, '提分宝删除', 2012, 8, '#', '', 'F', 'course:treasure:remove');
