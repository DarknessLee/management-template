# 后台管理系统（前后端分离）

一个用于学习全栈开发的管理后台模板：前端 Vue 3 + Element Plus，后端 Spring Boot + MyBatis-Plus + MySQL + Redis，前后端通过 `/api` 真实联通，不再使用 mock 数据。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 前端 | Vue 3、Vite 5、Vue Router、Pinia、Element Plus、Axios |
| 后端 | Java 21、Spring Boot 3.2、MyBatis-Plus 3.5、JJWT、BCrypt |
| 存储 | MySQL 8（业务数据）、Redis 7（登录 token 白名单） |
| 环境 | Docker Compose 管理 MySQL / Redis |

## 目录结构

```
management-template/
├── frontend/                 # Vue 3 前端
│   ├── src/api/              # 接口封装（auth/user/role/dashboard/settings）
│   ├── src/stores/           # Pinia 状态（登录态）
│   ├── src/utils/request.js  # axios 实例 + token 注入 + 401 处理
│   ├── src/views/            # 登录、仪表盘、用户、角色、设置
│   └── vite.config.js        # 开发代理 /api -> localhost:8080
└── backend/                  # Spring Boot 后端（单模块分层）
    ├── db/01-schema.sql      # 建表脚本（容器首次启动自动执行）
    ├── docker-compose.yml    # mgmt-mysql / mgmt-redis
    └── src/main/java/com/mgmt/
        ├── common/           # Result、PageResult、异常、UserContext
        ├── config/           # MyBatis-Plus、拦截器注册、BCrypt
        ├── security/         # JwtUtil、AuthInterceptor
        ├── init/             # DataSeeder 种子数据
        └── module/           # auth / user / role / dashboard / setting
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

建表脚本在容器首次初始化时自动执行；如果数据库卷已存在，删卷重来：`docker compose down -v`。

### 2. 启动后端（端口 8080）

```powershell
cd backend
.\mvnw.cmd spring-boot:run
# 或打包后运行
.\mvnw.cmd -DskipTests package
java -jar target\mgmt-backend-1.0.0.jar
```

首次启动时 `DataSeeder` 会写入测试账号、3 个角色和默认系统设置（表为空才写入，可重复启动）。

### 3. 启动前端（端口 3000）

```powershell
cd frontend
npm install
npm run dev
```

打开 http://localhost:3000 ，使用 `admin / 123456` 或 `user / 123456` 登录。

## 接口一览

所有接口统一返回 `{ code, message, data }`，`code=200` 表示成功。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /api/auth/login | 登录，返回 JWT |
| GET | /api/auth/userinfo | 当前登录用户信息 |
| POST | /api/auth/logout | 退出登录 |
| GET | /api/users?page&size&keyword | 用户分页 + 关键字搜索 |
| POST/PUT/DELETE | /api/users[/{id}] | 用户增改删（PUT 支持只传 status） |
| GET | /api/roles | 角色列表（含权限） |
| POST/PUT/DELETE | /api/roles[/{id}] | 角色增改删 |
| GET | /api/dashboard | 仪表盘统计 |
| GET/PUT | /api/settings | 系统设置读取 / 保存 |

## 关键设计说明（学习笔记）

1. **JWT + Redis 白名单**：JWT 负责签名防篡改，Redis 负责“是否仍然有效”。退出登录只需删除 Redis key，token 立刻失效，不用等 JWT 自然过期。
2. **只用 BCrypt，不引入 Spring Security 全家桶**：`spring-security-crypto` 提供 `BCryptPasswordEncoder`，鉴权交给 `HandlerInterceptor`，一次请求如何被拦下来完全可见。
3. **路径约定**：后端 `server.servlet.context-path=/api`，前端 axios `baseURL=/api`，vite 代理原样转发 `/api`，三处一致才不会 404。
4. **逻辑删除**：`sys_user` / `sys_role` 有 `deleted` 字段，MyBatis-Plus 的 `@TableLogic` 自动把 `DELETE` 改写为 `UPDATE`。注意唯一索引仍包含已删除数据，删除后无法立刻复用同名用户名。
5. **部分更新**：MyBatis-Plus 默认忽略 null 字段，所以状态开关只传 `{ status: 0 }` 即可，不需要额外写 SQL。
6. **分页**：必须注册 `PaginationInnerInterceptor`，否则 `selectPage` 会退化成全表查询。
7. **密码不回显**：`SysUser.password` 加 `@JsonIgnore`，且列表统一返回 `UserVO`，避免哈希泄露。
