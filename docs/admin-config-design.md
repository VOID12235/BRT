# BTR 赛事平台 · 管理后台「基础配置」模块设计文档

| 项 | 内容 |
|---|---|
| 文档版本 | v1.0 |
| 状态 | 设计稿（未实现） |
| 适用项目 | BTR 赛事比赛平台（Spring Boot 4.1.2 / Java 21 / MyBatis-Plus / MySQL+H2 / Redis） |
| 关联模块 | 认证授权、邮件、上传、操作日志、管理后台前端 |

---

## 1. 背景与目标

当前系统存在三类"写死"的配置，改动必须改代码或环境变量并重启：

1. **环境变量类**：SMTP 参数（`MAIL_HOST` 等）、验证码开关（`CAPTCHA_ENABLED`）、上传上限（yml 中 50MB）；
2. **硬编码类**：邮件 HTML 模板（`EmailService` 内）、站点名 / 页脚（`frontend/common/header.html`、`footer.html`）、上传文件类型白名单（`FileService.ALLOWED`）；
3. **缺失能力**：无忘记密码 / 邮箱验证码流程；管理员 / 评委账号只能靠 `AdminBootstrap` 启动引导或手工造数创建；`sys_log`、`sys_email_record` 两表持续记录但无任何查看入口。

**目标**：把上述配置收口到数据库 `sys_config` 单表，管理后台在线维护，改完即时生效、无需重启；未配置时回退现有默认值，**老行为完全不变**。同时补齐账号权限管理与日志/邮件查询能力。

**设计原则**

- 单表 KV 配置，按分组前缀（`mail.*`、`site.*`、`switch.*`、`template.*`、`upload.*`、`security.*`）组织；
- 所有配置带默认值，读配置时缺失即用默认，不抛错；
- 邮件未配置时维持现有 `SKIPPED` 降级行为；
- 全部配置修改与账号操作写入 `sys_log` 审计。

---

## 2. 数据库设计

### 2.1 新增表 `sys_config`

```sql
CREATE TABLE sys_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(64) NOT NULL,      -- 配置键，如 mail.host
    config_value VARCHAR(2000),           -- 配置值（密码字段加密存储）
    description VARCHAR(255),             -- 中文说明，前端表单展示用
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_config_key UNIQUE (config_key)
);
```

- 同步写入 `src/main/resources/schema-h2.sql`（开发库）与 `deploy/mysql/init.sql`（生产库）。
- 实体：`SysConfig`（@TableName("sys_config")），Mapper：`SysConfigMapper extends BaseMapper<SysConfig>`。
- 无外键、无状态位，简单 KV。

### 2.2 配置项总表（种子数据）

| 分组 | 配置键 | 默认值 | 说明 |
|---|---|---|---|
| 邮件 | `mail.host` | 空 | SMTP 服务器地址 |
| 邮件 | `mail.port` | 465 | SMTP 端口 |
| 邮件 | `mail.username` | 空 | 邮箱账号 |
| 邮件 | `mail.password` | 空 | 密码/授权码，**AES 加密入库** |
| 邮件 | `mail.from` | BTR 赛事平台 | 发件人显示名 |
| 邮件 | `mail.ssl` | true | SSL 开关 |
| 邮件 | `mail.enabled` | false | 邮件总开关 |
| 站点 | `site.name` | BTR 赛事平台 | 站点名，注入 header 标题 |
| 站点 | `site.logo_url` | 空 | 站点 Logo URL |
| 站点 | `site.footer` | © 2026 BTR Competition Platform. | 页脚信息 |
| 开关 | `switch.register_enabled` | true | 是否开放注册 |
| 开关 | `switch.captcha_enabled` | dev:false / prod:true | 图形验证码开关 |
| 模板 | `template.signup_receipt` | 见 §6.2 现有文案 | 报名成功邮件模板 |
| 模板 | `template.review_reminder` | 见 §6.2 现有文案 | 评审提醒邮件模板 |
| 模板 | `template.winner_notice` | 见 §6.2 现有文案 | 获奖通知邮件模板 |
| 模板 | `template.verify_code` | 含 `{code}` 占位符 | 验证码邮件模板 |
| 上传 | `upload.max_size_mb` | 50 | 单文件上限（MB） |
| 上传 | `upload.allowed_types` | jpg,jpeg,png,gif,webp,svg,pdf,zip,rar,7z,doc,docx,ppt,pptx,xls,xlsx,txt,md,mp4,mov | 可上传类型白名单 |
| 安全 | `security.login_fail_limit` | 5 | 登录失败锁定阈值 |
| 安全 | `security.login_lock_minutes` | 10 | 锁定时长（分钟） |

> 注意：`upload.max_size_mb` 需要与 `spring.servlet.multipart.max-file-size` 联动。Spring 允许在运行时通过 `MultipartProperties` 动态调整，实现时须处理该联动，否则仅展示不生效（降级方案：文档标注该参数仅对后续请求生效）。

---

## 3. 后端接口设计

统一前缀 `/api/v1`，统一响应 `{ code: 0, message: "ok", data: {} }`。

### 3.1 配置管理（均要求 ADMIN）

**3.1.1 邮件配置**

- `GET /api/v1/admin/config/mail`
  - 响应 data：`{ host, port, username, password: "******", from, ssl, enabled }`
  - password 一律脱敏返回 `******`。

- `PUT /api/v1/admin/config/mail`
  - 请求 body：`{ host, port, username, password?, from, ssl, enabled }`
  - 规则：`password` 为空或 `******` 时保留原值；非空则 AES 加密存储。
  - 保存后不重启，`EmailService` 下次发送时读取最新配置动态构建 `JavaMailSenderImpl`。

- `POST /api/v1/admin/config/mail/test`
  - 请求 body：`{ to }`
  - 行为：用当前配置构建 sender 向 `to` 发送测试邮件，返回 `{ success, message }`；失败返回具体原因（认证失败/连接超时等）。
  - 该测试邮件也写入 `sys_email_record`（template=`MAIL_TEST`）。

**3.1.2 站点信息**

- `GET /api/v1/admin/config/site` → `{ name, logoUrl, footer }`
- `PUT /api/v1/admin/config/site` → body 同上三项。
- 生效方式：前端 `header.html` / `footer.html` 改为启动时 `fetch /api/v1/config/public-site`（公开只读接口，无需登录）动态注入；未配置回退现有静态内容。

**3.1.3 系统开关**

- `GET /api/v1/admin/config/switches` → `{ registerEnabled, captchaEnabled }`
- `PUT /api/v1/admin/config/switches`
- 联动：`AuthService.register` 读 `switch.register_enabled`，关闭时返回「当前暂未开放注册」；`captchaEnabled` 读取位置不变（`AuthService.isCaptchaEnabled()` 改为读配置表，默认值跟随环境）。

**3.1.4 通知模板**

- `GET /api/v1/admin/config/templates` → 全部模板 key + 内容
- `PUT /api/v1/admin/config/templates/{key}` → body `{ content }`
- 联动：`EmailService` 的 `signupReceipt / reviewReminder / winnerNotice / sendVerifyCode` 从配置表读取模板，未配置时使用代码内默认文案（§6.2）。模板占位符：`{nickname}`、`{contestTitle}`、`{awardName}`、`{code}`。

**3.1.5 上传参数**

- `GET /api/v1/admin/config/uploads` → `{ maxSizeMb, allowedTypes }`
- `PUT /api/v1/admin/config/uploads`
- 联动：`FileService` 白名单改为读配置；大小限制联动 `MultipartProperties`（见 §2.2 注意）。

**3.1.6 安全策略**

- `GET /api/v1/admin/config/security` → `{ loginFailLimit, loginLockMinutes }`
- `PUT /api/v1/admin/config/security`
- 联动：`AuthService.login` 增加失败计数（Redis key `login:fail:{account}`，达阈值锁定 `login:lock:{account}`，TTL=lockMinutes）。

### 3.2 账号权限管理（均要求 ADMIN）

| 接口 | 说明 |
|---|---|
| `POST /api/v1/admin/users` | body `{ account, password, nickname, email, role }`；role 白名单 `ADMIN/JUDGE/PLAYER`；账号/邮箱唯一校验（复用注册逻辑） |
| `PUT /api/v1/admin/users/{id}/password` | body `{ newPassword }`；允许重置任意账号（含自己，等价于改自己密码）；重置后旧登录态仍有效，下一次登录使用新密码 |
| `PUT /api/v1/admin/users/{id}/role` | body `{ role }`；**最后一个启用 ADMIN 不可降级** |
| `PUT /api/v1/admin/users/{id}/status` | 复用现有接口；**最后一个启用 ADMIN 不可禁用** |
| `GET /api/v1/admin/users` | 复用现有接口，按 role 筛选 |

防锁死校验统一在 `AuthService` 新增方法：`assertNotLastAdmin(targetUserId)`——统计 `role=ADMIN AND status=ACTIVE` 数量，若为 1 且目标为该账号则拒绝。

所有操作后 `logService.record(currentUserId, "ADMIN_USER_XXX", account, ip)`。

### 3.3 忘记密码 / 邮箱验证码（公开接口）

**3.3.1 发送验证码**

- `POST /api/v1/auth/forgot-password`
  - body：`{ email }`
  - 流程：
    1. 校验邮箱存在且账号 `ACTIVE`；不存在也返回成功（防枚举），但记录日志；
    2. 检查 Redis `pwd:code:{email}` 是否存在（60 秒防重发），存在则返回「发送过于频繁」；
    3. 生成 6 位数字验证码，存 Redis `pwd:code:{email}`，TTL 10 分钟；错误计数 `pwd:fail:{email}` 清零；
    4. 读 `template.verify_code` 渲染（占位符 `{code}`），调 `EmailService.sendVerifyCode`；
    5. 邮件未配置（SKIPPED）时返回明确提示：「管理员暂未配置邮件服务，请联系管理员重置」。
- `POST /api/v1/auth/reset-password`
  - body：`{ email, code, newPassword }`
  - 流程：
    1. 校验验证码（Redis），错误计数 +1，**连续错 5 次作废验证码**；
    2. 校验新密码强度（与注册同规则）；
    3. 重置密码（BCrypt），删除验证码 key，写入 `sys_log`。

### 3.4 日志与邮件记录查询（均要求 ADMIN）

| 接口 | 说明 |
|---|---|
| `GET /api/v1/admin/logs` | 分页；参数 `userId / action / ip / start / end / page / size`；数据源 `sys_log`（排序 created_at desc） |
| `GET /api/v1/admin/mails` | 分页；参数 `status / recipient / page / size`；数据源 `sys_email_record`；返回含 error 字段（失败原因） |

---

## 4. 前端页面设计

### 4.1 `admin/settings.html`（基础配置）

标签页布局（Tab 切换，单页 6 个表单区块）：

| Tab | 表单字段 | 交互 |
|---|---|---|
| 邮件配置 | host / port / username / password（placeholder `******`）/ from / ssl 开关 / enabled 开关 | 「保存」+「发送测试邮件」（弹窗输入收件箱） |
| 站点信息 | name / logoUrl / footer | 「保存」，保存后提示刷新前台生效 |
| 系统开关 | registerEnabled / captchaEnabled | 「保存」 |
| 通知模板 | 4 个模板各一个 textarea + 占位符说明 | 「保存」按 key 逐个提交 |
| 上传参数 | maxSizeMb / allowedTypes（逗号分隔） | 「保存」 |
| 安全策略 | loginFailLimit / loginLockMinutes | 「保存」 |

### 4.2 `admin/user-manage.html`（账号管理）

- 列表：账号 / 昵称 / 邮箱 / 角色徽标 / 状态 / 创建时间；
- 筛选：角色下拉 + 关键词；
- 「创建账号」弹窗：account / password / nickname / email / role 下拉；
- 行内操作：重置密码（弹窗输入新密码）、改角色（下拉）、启用/禁用（开关，禁用态操作时提示防锁死错误）。

### 4.3 `admin/logs.html`（操作日志）

表格：时间 / 操作人 / 动作 / 目标 / IP；筛选：动作、关键词、时间范围。

### 4.4 `admin/mails.html`（邮件记录）

表格：时间 / 收件人 / 模板 / 主题 / 状态徽标（SENT/FAILED/SKIPPED）/ 失败原因；筛选：状态。

### 4.5 导航入口（`admin-index.html`）

侧边栏新增：**基础配置、账号管理、系统日志、邮件记录**。

---

## 5. 安全设计

| 项 | 方案 |
|---|---|
| SMTP 密码 | AES 加密存库（密钥取自 `JWT_SECRET` 派生或独立 `CONFIG_SECRET`）；接口回显 `******`；前端不传则保留原值 |
| 验证码 | Redis 存储、TTL 10 分钟、60 秒防重发、连续错 5 次作废 |
| 防锁死 | 系统最后一个启用中的 ADMIN 禁止禁用 / 降级 / 重置自身密码 |
| 审计 | 配置修改、账号创建/改密/改角色/启禁用均写 `sys_log` |
| 降级 | 配置缺失回退默认；邮件未配置保持 `SKIPPED`；公开接口（forgot-password）对不存在邮箱返回相同响应防枚举 |
| 权限 | 全部配置/账号/日志接口 `hasRole('ADMIN')`；公开只读站点配置接口仅暴露 name/logo/footer |

---

## 6. 与现有代码的衔接点

### 6.1 邮件动态化

现有 `EmailService` 通过 `ObjectProvider<JavaMailSender>` 注入，未配置时 `SKIPPED`。改造为：

```
send() 时：读取 sys_config(mail.*) → enabled 且 host 非空？
  ├─ 是：new JavaMailSenderImpl()（host/port/username/password/SSL 按配置设置）→ 发送
  └─ 否：维持 SKIPPED 逻辑
```

`JavaMailSenderImpl` 的创建成本低，可每次发送构建（简单可靠）；性能敏感时可改为缓存 + 配置变更失效。

### 6.2 现有邮件文案（作为模板默认值）

- 报名成功：`<p>Hi {nickname}，</p><p>你已成功报名 <b>{contestTitle}</b>，请留意作品提交截止时间。</p>`
- 评审提醒：`<p>Hi {nickname}，</p><p><b>{contestTitle}</b> 的评审已开始，请尽快完成分配作品的打分。</p>`
- 获奖通知：`<p>Hi {nickname}，</p><p>恭喜你在 <b>{contestTitle}</b> 中获得 <b>{awardName}</b>！</p>`

### 6.3 站点信息动态化

`frontend/common/header.html` / `footer.html` 内写死内容替换为：

```html
<!-- 占位节点 -->
<span id="site-name">BTR 赛事平台</span>
<span id="site-footer">© 2026 BTR Competition Platform.</span>
<script>fetch('/api/v1/config/public-site').then(r=>r.json()).then(d=>{/* 注入 */});</script>
```

---

## 7. 改动文件清单

**后端（新增 4 + 修改 8）**

| 动作 | 文件 |
|---|---|
| 新增 | `entity/SysConfig.java` |
| 新增 | `mapper/SysConfigMapper.java` |
| 新增 | `service/ConfigService.java`（读/写/脱敏/默认值） |
| 新增 | `dto/Views.java` 增加 MailConfig / SiteConfig / SwitchConfig / TemplateConfig / UploadConfig / SecurityConfig 视图（或独立 DTO 文件） |
| 修改 | `service/EmailService.java`（动态 sender + 模板读取 + sendVerifyCode） |
| 修改 | `service/AuthService.java`（登录限流、注册开关、忘记/重置密码、防锁死校验） |
| 修改 | `controller/AuthController.java`（forgot-password / reset-password 2 接口） |
| 修改 | `controller/AdminController.java`（配置 8 接口 + 账号 3 接口 + 日志/邮件查询 2 接口） |
| 修改 | `controller/FileController.java` 或 `FileService`（上传参数读配置） |
| 修改 | `resources/schema-h2.sql`、`deploy/mysql/init.sql`（加 sys_config 表 + 种子数据） |
| 修改 | `resources/application.yml`（配置默认值注释，保持兼容） |
| 修改 | `common/GlobalExceptionHandler.java`（如需新增错误码文案） |

**前端（新增 4 + 修改 2）**

| 动作 | 文件 |
|---|---|
| 新增 | `admin/settings.html` |
| 新增 | `admin/user-manage.html` |
| 新增 | `admin/logs.html` |
| 新增 | `admin/mails.html` |
| 修改 | `admin/admin-index.html`（侧边栏入口） |
| 修改 | `assets/js/admin.js`（页面逻辑 + API 封装） |
| 修改 | `common/header.html` / `common/footer.html`（站点信息动态注入） |

---

## 8. 实施顺序与验收标准

| 阶段 | 内容 | 验收标准 |
|---|---|---|
| 1 | sys_config 表 + ConfigService + 邮件配置接口 + 测试发送 | 后台保存 SMTP 后可收到测试邮件；未配置时业务邮件仍 SKIPPED |
| 2 | 账号管理 3 接口 + user-manage.html | 可创建 ADMIN/JUDGE/PLAYER；最后一个 ADMIN 无法禁用/降级 |
| 3 | 站点信息/开关 + header/footer 动态化 | 修改站点名刷新前台即生效；注册开关关闭后注册接口拒绝 |
| 4 | 忘记密码 + 验证码模板 | 真实邮箱可收到验证码并重置密码；错 5 次作废；60 秒防重发 |
| 5 | 日志/邮件记录查询页 | 两表数据可按条件分页检索 |
| 6 | 上传参数/安全策略 + 登录限流 | 改白名单即时生效；连续输错 5 次锁 10 分钟 |

每阶段完成后执行：`mvn -B -DskipTests package` 构建通过 + 手工接口冒烟 + 关键页面浏览器验证。

---

## 9. 风险与说明

1. `upload.max_size_mb` 运行时联动 Multipart 上限存在框架限制，若无法动态生效则降级为「重启后生效」，文档标注。
2. `mail.password` 的 AES 密钥若遗失，已存密码无法解密，需重新配置。
3. 模板修改只影响新发送的邮件，历史记录不受影响。
4. 忘记密码接口对不存在邮箱返回相同响应，防止账号枚举；但日志中保留排查信息（仅管理员可见）。
