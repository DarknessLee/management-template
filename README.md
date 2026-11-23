# 后台管理系统（前后端分离）

一个用于学习全栈开发的管理后台模板：前端 Vue 3 + Element Plus，后端 Spring Boot + MyBatis-Plus + MySQL + Redis，前后端通过 `/api` 真实联通，不使用 mock 数据。

## 功能特性

- **认证与安全**：图形验证码登录、JWT + Redis 白名单、退出即失效、在线用户查看与强制下线、可配置页面水印（登录人 + 日期）、操作日志与登录日志。
- **RBAC 权限**：用户、角色、权限点、菜单四级管理；按钮级 `v-permission`；角色支持数据范围（全部 / 本部门 / 本部门及以下 / 仅本人）。
- **动态菜单**：侧边栏和路由完全由数据库菜单驱动，支持「目录 + 菜单」两级结构，目录自动隐藏无权限子项。
- **组织与用户**：部门树、用户 Excel 导入导出与模板下载、状态启停、头像与个人中心。
- **内容管理**：通知公告（置顶、已读统计、阅读明细、未读提醒）、数据字典（统一维护状态等枚举，前端会话缓存）。
- **运维能力**：定时任务（Cron、手动执行、执行记录）、系统监控、日志清理、文件管理（类型筛选、图片/PDF 在线预览、批量删除）。
- **系统设置**：基础设置（品牌名称、Logo）、安全设置、通知设置；品牌配置实时同步到登录页、侧边栏和浏览器标题。
- **使用体验**：数据仪表盘（趋势图、模块分布、最近操作、最新公告）、多标签页导航（关闭/刷新/关闭其他）、面包屑与折叠侧边栏。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 前端 | Vue 3、Vite 5、Vue Router、Pinia、Element Plus、Axios |
| 后端 | Java 21、Spring Boot 3.2、MyBatis-Plus 3.5、JJWT、BCrypt |
| 存储 | MySQL 8（业务数据）、Redis 7（登录 token 白名单、验证码） |
| 环境 | Docker Compose 管理 MySQL / Redis |

## 目录结构

```
management-template/
├── frontend/                     # Vue 3 前端
│   ├── src/api/                  # 接口封装（user/role/menu/dept/dict/notice/job/file/log…）
│   ├── src/stores/               # Pinia：登录态 user、动态菜单 menu、品牌与水印 site
│   ├── src/layout/               # 布局与多标签页 TagsView
│   ├── src/router/               # 静态路由 + 根据菜单动态注册子路由
│   ├── src/directives/           # v-permission 按钮权限指令
│   ├── src/utils/                # request（token/401）、dict 字典缓存、asset 资源地址
│   └── vite.config.js            # 开发代理 /api -> localhost:8080
└── backend/                      # Spring Boot 后端（单模块分层）
    ├── db/01-schema.sql          # 建表脚本（启动时幂等执行）
    ├── docker-compose.yml        # mgmt-mysql / mgmt-redis
    └── src/main/java/com/mgmt/
        ├── common/               # Result、PageResult、BizException、UserContext
        ├── config/               # MyBatis-Plus、拦截器注册、BCrypt
        ├── security/             # JwtUtil、AuthInterceptor、@RequirePermission、@OperationLog
        ├── init/                 # DataSeeder 种子数据与历史库补丁
        └── module/               # auth/user/role/permission/menu/dept/dict/notice/
                                  # log/job/file/monitor/profile/dashboard/setting
```

## 快速开始

### 1. 启动数据库

```powershell
cd backend
docker compose up -d
```

| 容器 | 端口 | 账号 |
| --- | --- | --- |
| mgmt-mysql | 13309 | 库 `management_template`，用户 `mgmt` / `Mgmt_2026` |
| mgmt-redis | 16382 | 密码 `Mgmt_2026_redis` |

建表脚本在容器首次初始化时自动执行；后端启动时也会执行幂等 DDL，为历史库补充新增表和字段。需要完全重来时可执行：`docker compose down -v`。

### 2. 启动后端（端口 8080）

```powershell
cd backend
.\mvnw.cmd spring-boot:run
# 或打包后运行
.\mvnw.cmd -DskipTests package
java -jar target\mgmt-backend-1.0.0.jar
```

首次启动时 `DataSeeder` 会写入测试账号、角色、部门、字典、菜单和默认系统设置（全部幂等，可重复启动）。

### 3. 启动前端（端口 3000）

```powershell
cd frontend
npm install
npm run dev
```

打开 http://localhost:3000 ，使用 `admin / 123456` 或 `user / 123456` 登录（登录需要输入图形验证码）。

## 接口一览

所有接口统一返回 `{ code, message, data }`，`code=200` 表示成功；除登录前接口外均需 `Authorization: Bearer <token>`。

| 模块 | 主要接口 |
| --- | --- |
| 认证 | `POST /api/auth/captcha`、`POST /api/auth/login`、`POST /api/auth/logout`、`GET /api/auth/userinfo` |
| 仪表盘 | `GET /api/dashboard`（统计、登录趋势、模块分布、最近操作、最新公告） |
| 用户 | `GET /api/users`、`GET /api/users/export`、`GET /api/users/import-template`、`POST /api/users/import`、用户增改删 |
| 角色 | `GET/POST/PUT/DELETE /api/roles`（含权限与数据范围） |
| 权限点 | `GET/POST/PUT/DELETE /api/permissions` |
| 菜单 | `GET /api/menus/current`（当前用户菜单树）、`GET/POST/PUT/DELETE /api/menus` |
| 部门 | `GET /api/depts`（树）、`GET/POST/PUT/DELETE /api/depts` |
| 字典 | `GET /api/dicts/types`、`GET /api/dicts/code/{code}/items`、字典类型/项维护 |
| 公告 | `GET/POST/PUT/DELETE /api/notices`、`PUT /api/notices/{id}/pin`、`GET /api/notices/{id}/read-stats`、`GET /api/notices/{id}/reads` |
| 日志 | `GET /api/logs`、`DELETE /api/logs/clean`、`GET /api/login-logs` |
| 定时任务 | `GET/POST/PUT/DELETE /api/jobs`、`POST /api/jobs/{id}/run`、`GET /api/jobs/{id}/logs` |
| 文件 | `GET /api/files`、`POST /api/files`、`GET /api/files/{id}/download`、`GET /api/files/{id}/preview`、`DELETE /api/files/batch` |
| 系统设置 | `GET/PUT /api/settings`、`GET /api/settings/public`（登录页品牌与水印开关，免登录） |
| 监控/在线 | `GET /api/monitor`、`GET /api/online-users`、强制下线 |
| 个人中心 | `GET/PUT /api/profile`、`PUT /api/profile/password`、`POST /api/profile/avatar` |

## 关键设计说明（学习笔记）

1. **JWT + Redis 白名单**：JWT 负责签名防篡改，Redis 负责「是否仍然有效」。退出登录只需删除 Redis key，token 立刻失效，不用等 JWT 自然过期。
2. **只用 BCrypt，不引入 Spring Security 全家桶**：`spring-security-crypto` 提供 `BCryptPasswordEncoder`，鉴权交给 `HandlerInterceptor`，一次请求如何被拦下来完全可见。
3. **路径约定**：后端 `server.servlet.context-path=/api`，前端 axios `baseURL=/api`，vite 代理原样转发 `/api`，三处一致才不会 404。
4. **逻辑删除**：`sys_user` / `sys_role` 有 `deleted` 字段，MyBatis-Plus 的 `@TableLogic` 自动把 `DELETE` 改写为 `UPDATE`。注意唯一索引仍包含已删除数据，删除后无法立刻复用同名用户名。
5. **部分更新**：MyBatis-Plus 默认忽略 null 字段，所以状态开关只传 `{ status: 0 }` 即可，不需要额外写 SQL。
6. **分页**：必须注册 `PaginationInnerInterceptor`，否则 `selectPage` 会退化成全表查询。
7. **密码不回显**：`SysUser.password` 加 `@JsonIgnore`，且列表统一返回 `UserVO`，避免哈希泄露。
8. **动态菜单与路由**：菜单存于 `sys_menu`（支持 `parent_id` 两级），`/menus/current` 按权限过滤并建树；前端只把叶子菜单注册为布局子路由，目录仅用于分组，权限不足时目录自动不显示。
9. **数据权限**：`DataScopeService` 统一计算当前用户可见的部门与用户范围，用户列表、日志查询等都复用同一套规则，避免各接口各写一遍。
10. **公告已读**：`sys_notice_read` 以 `(notice_id, user_id)` 唯一索引去重；阅读统计与阅读明细都基于该表，未读数 = 已发布公告数 - 当前用户已读数。
11. **定时任务**：任务通过 `handler_key` 映射到处理器，手动执行与 Cron 执行走同一条链路，统一写入 `sys_job_log`，记录触发方式、成败、耗时与结果。
12. **文件安全**：上传做扩展名白名单与 20MB 限制；下载使用 attachment，预览使用 inline 并返回真实 MIME，图片/PDF 可直接在弹窗中渲染。
13. **品牌配置与水印**：`GET /settings/public` 只暴露标题、Logo、水印开关等非敏感字段并加入拦截器白名单，登录页无需 token 即可读取；设置保存后前端立即刷新品牌与水印，无需刷新页面。水印由 Canvas 生成平铺底图，包含登录人、姓名和日期，`pointer-events: none` 不影响操作。
14. **字典缓存**：`useDictOptions` 在会话内缓存字典项，接口异常时使用兜底选项，避免每个页面重复请求。

## 新增模块约定

1. 在 `db/01-schema.sql` 追加 `CREATE TABLE IF NOT EXISTS ...`（启动时幂等执行）。
2. 新建 `module/xxx` 包后，把包路径加入 `MgmtApplication` 的 `@MapperScan`，否则 Mapper 不会被扫描，启动即失败。
3. 在 `DataSeeder` 中补充权限点、菜单和 admin 授权（幂等方法），老库升级时才能自动补齐。
4. 菜单的 `component` 必须匹配 `views/xxx/index.vue`，`path` 以 `/` 开头；目录类型菜单的 `component` 留空。
5. 前端新增页面后，在 `views/` 下创建对应 `index.vue`，菜单配置完成后刷新页面即可看到入口。
