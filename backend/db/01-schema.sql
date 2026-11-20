-- 后台管理系统建表脚本
-- 该目录挂载到 MySQL 容器的 /docker-entrypoint-initdb.d，容器首次初始化时自动执行。
-- 数据初始化由后端 DataSeeder 负责（密码需要 BCrypt 加密，放在 Java 里更直观）。

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '登录名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
    real_name   VARCHAR(50)  NOT NULL COMMENT '姓名',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像相对路径',
    role_id     BIGINT       NOT NULL COMMENT '角色ID',
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
    title       VARCHAR(50)  NOT NULL COMMENT '菜单名称',
    path        VARCHAR(100) NOT NULL COMMENT '前端路由路径',
    component   VARCHAR(100) NOT NULL COMMENT '前端视图路径，如 views/users/index.vue',
    icon        VARCHAR(50)  DEFAULT NULL COMMENT 'Element Plus 图标组件名',
    permission  VARCHAR(50)  DEFAULT NULL COMMENT '所需权限编码，为空表示登录可见',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    visible     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否显示：1显示 0隐藏',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_menu_path (path)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='菜单表';
