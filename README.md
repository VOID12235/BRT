# BTR 赛事比赛平台

一套可直接运行的赛事全流程平台：**赛事发布 → 在线报名 → 作品提交 → 评委分配 → 在线打分 → 榜单公示 → 通知与导出**。

页面采用浅色卡片式设计，原生 HTML / CSS / JavaScript 分离，无需前端构建工具；后端基于 Spring Boot 4（Java 21）、MyBatis-Plus、MySQL、Redis。

---

## 1. 功能概览

| 模块 | 能力 |
| --- | --- |
| 公开站 | 首页、赛事列表与筛选、赛事详情（介绍 / 规则 / 奖项 / 作品）、榜单公示、公告通知、登录注册 |
| 选手端 | 个人中心、报名赛事、取消报名、提交 / 修改 / 撤回作品、查看评分与获奖 |
| 评委端 | 任务列表、按赛事筛选、评审进度、在线打分与改分、上一件 / 下一件 |
| 管理后台 | 仪表盘、赛事 CRUD 与状态流转、报名审核、作品锁定、评委分配、奖项配置、公告管理、Excel 导出、**基础配置（邮件 SMTP / 站点信息 / 系统开关 / 通知模板 / 上传参数 / 安全策略）、账号权限管理（创建账号 / 改角色 / 重置密码 / 防锁死）、操作日志与邮件记录查询** |
| 平台能力 | JWT 登录与刷新、图形验证码、登录失败限流与临时锁定、注册开关、忘记密码（邮箱验证码）、角色权限隔离、Redis 榜单缓存、邮件通知（模板可配）、文件上传（白名单可配）、操作日志 |

### 角色

- `PLAYER` 选手：报名、提交作品、查看个人结果
- `JUDGE` 评委：仅能看到分配给自己的作品并打分
- `ADMIN` 管理员：全量管理权限

### 赛事状态机

```
DRAFT（筹备中）
  → SIGNUP（报名中）   开放报名与作品提交
  → REVIEW（评审中）   锁定全部作品，评委开始打分
  → PUBLISHED（已公示）固化获奖名单并发送获奖通知
  → FINISHED（已结束） 归档，保留榜单展示
```

---

## 2. 技术栈

- **后端**：Spring Boot 4.1.2、Spring Security、MyBatis-Plus 3.5.18、Java 21
- **数据库**：MySQL 8（生产） / H2 内存库（本地开发，MySQL 兼容模式）
- **缓存**：Redis（不可用时自动降级为进程内缓存，榜单仍可用）
- **邮件**：Spring Mail，SMTP 可在管理后台「基础配置」在线维护（密码 AES 加密存储），未配置 SMTP 时记录为 `SKIPPED`，不阻塞业务流程
- **导出**：Apache POI（报名名单 / 获奖名单 `.xlsx`）
- **前端**：原生 HTML + CSS + JS，公共头尾通过 `fetch` 注入
- **部署**：Docker Compose + Nginx

---

## 3. 目录结构

```
.
├─ src/main/java/com/btr/platform/
│  ├─ controller/      # 公开、选手、评委、管理端接口
│  ├─ service/         # 业务逻辑（赛事、报名、作品、评分、榜单、导出、邮件）
│  ├─ mapper/          # MyBatis-Plus Mapper
│  ├─ entity/ dto/     # 实体与请求/响应模型
│  ├─ security/        # JWT、过滤器、当前用户
│  ├─ config/          # 安全、Web、MyBatis 配置
│  └─ common/          # 统一响应、异常、分页、缓存
├─ src/main/resources/
│  ├─ application.yml          # 公共配置
│  ├─ application-dev.yml      # H2 开发配置
│  ├─ application-prod.yml     # MySQL / Redis / 邮件生产配置
│  ├─ schema-h2.sql            # 开发库建表
│  └─ ...
├─ frontend/
│  ├─ index.html               # 公开站
│  ├─ contest-list.html
│  ├─ contest-detail.html
│  ├─ rank.html
│  ├─ notices.html
│  ├─ login.html
│  ├─ signup.html
│  ├─ work-submit.html
│  ├─ user-center.html         # 选手端
│  ├─ judge/                   # 评委端
│  ├─ admin/                   # 管理后台
│  ├─ common/                  # 公共头部 / 页脚
│  └─ assets/                  # css / js / img
├─ deploy/
│  ├─ docker-compose.yml       # MySQL + Redis + App + Nginx
│  ├─ nginx/default.conf
│  ├─ mysql/init.sql           # 生产建表脚本
│  ├─ .env.example
│  ├─ backup.sh                # 数据库与上传文件备份
│  └─ restart.sh               # 构建并启动 / 重启
├─ scripts/dev-server.js       # 本地静态服务器 + /api 代理
├─ Dockerfile
└─ pom.xml
```

---

## 4. 本地开发

### 4.1 环境要求

- JDK 21
- Maven 3.9+（仓库内已附带 `.tools/apache-maven-3.9.9`，可直接使用）
- Node.js 18+（仅用于本地静态服务器，可选）

### 4.2 启动后端（H2 内存库）

```bash
# Windows PowerShell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21.0.12.1"
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -B -DskipTests package
java -jar target\btr-platform.jar
```

默认使用 `dev` profile，启动后监听 `http://localhost:8080`，并自动初始化演示数据。

### 4.3 启动前端

```bash
node scripts/dev-server.js
# http://localhost:5173
```

该脚本托管 `frontend/`，并把 `/api` 代理到 `http://localhost:8080`，避免跨域问题。

### 4.4 演示账号

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 选手 | `player` | `player123` |
| 选手 | `luna` / `kai` / `mia` | `player123` |
| 评委 | `judge` / `judge2` | `judge123` |
| 管理员 | `admin` | `admin123` |

> 开发环境默认关闭图形验证码（`CAPTCHA_ENABLED=false`），生产环境默认开启。

### 4.5 常用地址

- 公开站：`http://localhost:5173/index.html`
- 选手端：`http://localhost:5173/user-center.html`
- 评委端：`http://localhost:5173/judge/judge-index.html`
- 管理后台：`http://localhost:5173/admin/admin-index.html`

---

## 5. 生产部署（Docker Compose）

### 5.1 准备环境变量

```bash
cd deploy
cp .env.example .env
# 修改数据库密码、JWT_SECRET、管理员密码、CORS_ORIGINS
```

关键变量：

| 变量 | 说明 |
| --- | --- |
| `HTTP_PORT` | Nginx 对外端口，默认 80 |
| `DB_PASSWORD` / `MYSQL_ROOT_PASSWORD` | MySQL 业务账号 / root 密码 |
| `JWT_SECRET` | JWT 签名密钥，至少 32 位随机字符串 |
| `ADMIN_PASSWORD` | 首次启动自动创建的管理员密码；留空则不创建 |
| `SEED_DEMO` | 是否写入演示数据，生产务必 `false` |
| `CAPTCHA_ENABLED` | 是否启用图形验证码，生产建议 `true` |
| `CORS_ORIGINS` | 允许跨域的来源，同域部署可填站点域名 |
| `MAIL_*` | SMTP 配置，留空则邮件记录为 `SKIPPED` |

### 5.2 启动

```bash
cd deploy
chmod +x restart.sh backup.sh
./restart.sh
```

或手动执行：

```bash
docker compose build app
docker compose up -d
docker compose ps
```

启动顺序：MySQL / Redis 健康检查通过 → 后端 → Nginx。首次启动会执行 `deploy/mysql/init.sql` 建表，并按 `ADMIN_PASSWORD` 创建管理员。

访问 `http://服务器IP`（或 `HTTP_PORT` 指定端口）。

### 5.3 停止与查看日志

```bash
cd deploy
docker compose logs -f app
docker compose down          # 停止并移除容器（保留数据卷）
docker compose down -v       # 同时删除数据卷，慎用
```

---

## 6. 数据备份与恢复

### 备份

```bash
cd deploy
./backup.sh            # 默认保留 14 天
./backup.sh 30         # 保留 30 天
```

产物位于 `deploy/backups/`：

- `mysql-YYYYmmdd-HHMMSS.sql.gz`
- `uploads-YYYYmmdd-HHMMSS.tar.gz`

### 恢复

```bash
cd deploy
gunzip -c backups/mysql-20260101-120000.sql.gz | \
  docker compose exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql \
  mysql -u root btr

docker compose exec -T app tar xzf - -C /app/uploads < backups/uploads-20260101-120000.tar.gz
```

建议将 `deploy/backups/` 定时同步到对象存储或异地服务器。

---

## 7. 接口约定

- 统一前缀：`/api/v1`
- 响应结构：

```json
{ "code": 0, "message": "ok", "data": {} }
```

- 鉴权：请求头 `Authorization: Bearer <accessToken>`
- 登录返回 `accessToken`（2 小时）与 `refreshToken`（7 天），前端自动刷新
- 分页参数：`page`、`size`，返回 `{ records, total, page, size }`

主要接口分组：

| 分组 | 示例 |
| --- | --- |
| 认证 | `POST /auth/login`、`POST /auth/register`、`GET /auth/me`、`GET /auth/captcha` |
| 公开 | `GET /contests`、`GET /contests/{id}`、`GET /ranks`、`GET /notices`、`GET /stats` |
| 选手 | `GET /signups/mine`、`POST /signups`、`DELETE /signups/{id}`、`GET /works/mine`、`POST /works` |
| 评委 | `GET /judge/tasks`、`GET /judge/progress`、`POST /judge/scores` |
| 管理 | `GET /admin/dashboard`、`POST /contests`、`POST /contests/{id}/status`、`GET /admin/signups`、`POST /admin/judge-assign`、`GET /admin/export/signups` |

---

## 8. 设计说明

- **榜单**：按评委平均分降序聚合，Redis ZSet 缓存；评分或作品变更时失效重算。赛事公示时按奖项名额与最低分固化获奖名单。
- **权限**：评委只能读取分配给自己的作品；管理接口统一要求 `ADMIN`；选手只能操作自己的报名与作品。
- **缓存降级**：Redis 连接失败时自动使用进程内缓存，单机部署仍可正常展示榜单。
- **邮件**：未配置 SMTP 时不抛错，仅记录日志并在邮件记录中标记 `SKIPPED`。
- **上传**：单文件 50MB，支持 PDF / ZIP / 图片 / 音视频；生产环境挂载独立数据卷持久化。
- **验证码**：开发环境关闭便于调试，生产环境默认开启。

---

## 9. 验收清单

- [x] 公开站首页、赛事列表 / 详情、榜单、公告、登录注册
- [x] 选手报名 → 提交作品 → 个人中心查看状态
- [x] 管理员创建赛事、状态流转、审核报名、锁定作品
- [x] 评委分配、任务列表、在线打分 / 改分、进度统计
- [x] 榜单聚合、公示固化、获奖通知
- [x] 报名名单 / 获奖名单 Excel 导出
- [x] Docker Compose + Nginx + MySQL + Redis 部署文件
- [x] 数据库与上传文件备份脚本

---

© 2026 BTR Competition Platform.
