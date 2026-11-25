-- 后台管理系统建表脚本
-- 该目录挂载到 MySQL 容器的 /docker-entrypoint-initdb.d，容器首次初始化时自动执行。
-- 数据初始化由后端 DataSeeder 负责（密码需要 BCrypt 加密，放在 Java 里更直观）。

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '登录名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
    password_update_time DATETIME DEFAULT NULL COMMENT '密码最近修改时间',
    real_name   VARCHAR(50)  NOT NULL COMMENT '姓名',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像相对路径',
    role_id     BIGINT       NOT NULL COMMENT '角色ID',
    dept_id     BIGINT       NOT NULL DEFAULT 0 COMMENT '所属部门ID，0表示未分配',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='系统用户表';

CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(50)  NOT NULL COMMENT '角色名称',
    code        VARCHAR(50)  NOT NULL COMMENT '角色标识（英文）',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    data_scope  TINYINT      NOT NULL DEFAULT 1 COMMENT '数据范围：1全部 2本部门 3本部门及以下 4仅本人',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='角色表';

CREATE TABLE IF NOT EXISTS sys_permission (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    code        VARCHAR(50) NOT NULL COMMENT '权限编码，如 users:view',
    name        VARCHAR(50) NOT NULL COMMENT '权限名称，如 查看用户',
    module      VARCHAR(50) NOT NULL COMMENT '归属模块，用于授权时分组',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='权限点定义表';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id    BIGINT      NOT NULL COMMENT '角色ID',
    permission VARCHAR(50) NOT NULL COMMENT '权限标识，如 users:view',
    PRIMARY KEY (role_id, permission)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='角色权限表';

CREATE TABLE IF NOT EXISTS sys_dept (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '上级部门ID，0为顶级',
    name        VARCHAR(50)  NOT NULL COMMENT '部门名称',
    leader      VARCHAR(50)  DEFAULT NULL COMMENT '负责人',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='部门表';

CREATE TABLE IF NOT EXISTS sys_job (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name            VARCHAR(50)  NOT NULL COMMENT '任务名称',
    handler_key     VARCHAR(50)  NOT NULL COMMENT '处理器标识',
    cron_expression VARCHAR(50)  NOT NULL COMMENT 'Cron 表达式',
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：1启用 0停用',
    remark          VARCHAR(255) DEFAULT NULL COMMENT '备注',
    last_run_time   DATETIME     DEFAULT NULL COMMENT '最近执行时间',
    last_result     VARCHAR(500) DEFAULT NULL COMMENT '最近执行结果',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_handler_key (handler_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='定时任务表';

CREATE TABLE IF NOT EXISTS sys_job_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    job_id       BIGINT       NOT NULL COMMENT '任务ID',
    job_name     VARCHAR(50)  NOT NULL COMMENT '任务名称（冗余，任务删除后仍可追溯）',
    handler_key  VARCHAR(50)  NOT NULL COMMENT '处理器标识',
    trigger_type TINYINT      NOT NULL DEFAULT 1 COMMENT '触发方式：1定时 2手动',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '结果：1成功 0失败',
    duration_ms  BIGINT       NOT NULL DEFAULT 0 COMMENT '耗时（毫秒）',
    result       VARCHAR(500) DEFAULT NULL COMMENT '执行结果或失败原因',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
    PRIMARY KEY (id),
    KEY idx_job_id (job_id),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='定时任务执行记录表';

CREATE TABLE IF NOT EXISTS sys_file (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_name   VARCHAR(255) NOT NULL COMMENT '存储相对路径，如 202611/uuid.pdf',
    url           VARCHAR(255) NOT NULL COMMENT '访问路径',
    size          BIGINT       NOT NULL COMMENT '文件大小（字节）',
    content_type  VARCHAR(100) DEFAULT NULL COMMENT 'MIME 类型',
    uploader_id   BIGINT       DEFAULT NULL COMMENT '上传人ID',
    uploader_name VARCHAR(50)  DEFAULT NULL COMMENT '上传人姓名',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (id),
    KEY idx_original_name (original_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='文件表';

CREATE TABLE IF NOT EXISTS sys_setting (
    setting_key  VARCHAR(50) NOT NULL COMMENT '配置分组：security/basic/notification',
    setting_json JSON        NOT NULL COMMENT '配置内容（JSON）',
    update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (setting_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='系统配置表';

CREATE TABLE IF NOT EXISTS login_log (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT      NOT NULL COMMENT '用户ID',
    username    VARCHAR(50) NOT NULL COMMENT '登录名（冗余，便于审计）',
    login_time  DATETIME    NOT NULL COMMENT '登录时间',
    logout_time DATETIME    DEFAULT NULL COMMENT '退出时间',
    PRIMARY KEY (id),
    KEY idx_login_time (login_time),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='登录日志表';

CREATE TABLE IF NOT EXISTS sys_operation_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT       DEFAULT NULL COMMENT '操作人ID',
    username    VARCHAR(50)  DEFAULT NULL COMMENT '操作人登录名（冗余，用户改名后仍可追溯）',
    module      VARCHAR(50)  NOT NULL COMMENT '业务模块，如 用户管理',
    action      VARCHAR(50)  NOT NULL COMMENT '操作动作，如 删除用户',
    method      VARCHAR(10)  DEFAULT NULL COMMENT 'HTTP 方法',
    uri         VARCHAR(200) DEFAULT NULL COMMENT '请求路径',
    params      TEXT         DEFAULT NULL COMMENT '请求参数 JSON（已脱敏、已截断）',
    ip          VARCHAR(50)  DEFAULT NULL COMMENT '客户端 IP',
    duration_ms BIGINT       NOT NULL DEFAULT 0 COMMENT '耗时（毫秒）',
    success     TINYINT      NOT NULL DEFAULT 1 COMMENT '结果：1成功 0失败',
    error_msg   VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_create_time (create_time),
    KEY idx_module (module),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='操作日志表';

CREATE TABLE IF NOT EXISTS sys_notice (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    title        VARCHAR(100) NOT NULL COMMENT '公告标题',
    content      TEXT         NOT NULL COMMENT '公告内容',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：1已发布 0已下线',
    pinned       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否置顶：1是 0否',
    publish_time DATETIME     DEFAULT NULL COMMENT '最近一次发布时间',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_status_publish (status, publish_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='通知公告表';

CREATE TABLE IF NOT EXISTS sys_notice_read (
    id        BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    notice_id BIGINT   NOT NULL COMMENT '公告ID',
    user_id   BIGINT   NOT NULL COMMENT '用户ID',
    read_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_notice_user (notice_id, user_id),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='公告已读记录表';

CREATE TABLE IF NOT EXISTS sys_message (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT       NOT NULL COMMENT '接收人ID',
    type        VARCHAR(30)  NOT NULL COMMENT '消息类型：notice/job/kick/system',
    title       VARCHAR(100) NOT NULL COMMENT '消息标题',
    content     VARCHAR(500) DEFAULT NULL COMMENT '消息内容',
    link        VARCHAR(200) DEFAULT NULL COMMENT '点击跳转的前端路由',
    read_flag   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：1已读 0未读',
    read_time   DATETIME     DEFAULT NULL COMMENT '已读时间',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_read (user_id, read_flag),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='站内消息表';

CREATE TABLE IF NOT EXISTS sys_dict_type (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(50)  NOT NULL COMMENT '字典名称',
    code        VARCHAR(50)  NOT NULL COMMENT '字典编码，业务代码引用',
    remark      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='字典类型表';

CREATE TABLE IF NOT EXISTS sys_dict_item (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    type_id     BIGINT      NOT NULL COMMENT '字典类型ID',
    label       VARCHAR(50) NOT NULL COMMENT '展示标签',
    value       VARCHAR(50) NOT NULL COMMENT '存储值',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_type_value (type_id, value),
    KEY idx_type_id (type_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='字典项表';

CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '上级菜单ID，0为顶级',
    title       VARCHAR(50)  NOT NULL COMMENT '菜单名称',
    title_en    VARCHAR(100) DEFAULT NULL COMMENT '英文菜单名称，为空时回退中文',
    path        VARCHAR(100) NOT NULL COMMENT '前端路由路径，目录用于菜单分组标识',
    component   VARCHAR(100) DEFAULT NULL COMMENT '前端视图路径，如 views/users/index.vue；目录为空',
    icon        VARCHAR(50)  DEFAULT NULL COMMENT 'Element Plus 图标组件名',
    permission  VARCHAR(50)  DEFAULT NULL COMMENT '所需权限编码，为空表示登录可见',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    visible     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否显示：1显示 0隐藏',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_menu_path (path),
    KEY idx_parent_id (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='菜单表';
