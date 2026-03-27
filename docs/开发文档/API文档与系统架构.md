# API文档与系统架构（接口契约底座）

> 本文档是“接口契约底座”。涉及 URL、方法、鉴权头、分页默认值、以及基础 DTO 字段集时，阶段文档必须引用本文并保持一致，不得给出冲突值。
>
> 阶段文档允许在“本底座未覆盖/未细化”的维度补充：端点级请求校验规则、错误场景与返回示例、与数据库表/事务边界的映射说明、以及实现所需的最小落地约束。
>
> 如果出现冲突，以阶段文档为最终落地；为消除冲突，API 文档只同步“冲突点的最小对齐信息”，并不把阶段级细节上移到本底座。

## 1. 接口约定（固定）

- 基础路径：`/api`
- 统一响应包装：`ApiResponse<T>`（固定字段，不允许阶段文档自定义变体）
  - `success: boolean`
  - `data: T | null`
  - `error: string | null`（失败时返回错误码（或 `code: message`）；错误码语义见 `错误码与错误响应.md`）
  - `timestamp: ISO8601`
  - `traceId: string`
- 追踪头：`X-Trace-Id`
  - 请求可携带；未携带时后端自动生成并回写响应头
- 认证与鉴权：`Spring Security + JWT`（固定）
  - 认证头：`Authorization: Bearer <accessToken>`
  - Access Token：默认有效期 `24h`
  - Refresh Token：用于续期 accessToken，服务端保存并支持吊销
  - 管理端权限：`ROLE_ADMIN`
  - 普通登录用户权限：`ROLE_USER`
- 鉴权范围（固定）：
  - 公开放行：内容浏览、搜索、评论读取、健康检查
  - 需要登录：`/api/ai/**`、评论写接口（创建/回复/点赞）
  - 需要管理员：`/api/admin/**`
- AI 会话隔离键：`conversationId = userId + ":" + sessionId`
- 限流：`/api/ai/**`、`/api/search`
  - 按 `userId + path` 优先限流，缺失 `userId` 时回退 `IP + path`
  - 配置项：`app.security.rate-limit-per-minute`（默认 `120`）

## 1.0 阶段细化与回填机制（允许）

- 阶段文档可以补充端点级细节（请求/响应字段约束、参数校验触发条件、典型错误场景示例、DB 映射/事务边界/索引或 outbox 事件的落地规则）。
- 阶段文档不得声明与本底座冲突的内容：基础路径 `/api`、接口方法与 URL、鉴权头要求、以及分页默认值等“确定项”保持与本文一致。
- 若阶段新增了底座未定义的“实现必要约束”，则仅在与本底座发生冲突时，才需要同步对齐到本文件对应条目或通用约定区（以维持不冲突）。

## 1.1 权限矩阵（固定）

| 能力 | 访问策略 | 说明 |
|------|----------|------|
| 文章/项目/公开内容浏览 | 匿名可访问 | 无需登录 |
| 搜索查询 | 匿名可访问 | 无需登录 |
| 评论列表查看 | 匿名可访问 | 无需登录 |
| AI 助手会话（含 SSE） | 登录用户 | 需 `ROLE_USER` 或 `ROLE_ADMIN` |
| 评论创建/回复/点赞 | 登录用户 | 不允许匿名评论 |
| 后台管理接口（`/api/admin/**`） | 管理员 | 需 `ROLE_ADMIN` 且携带 `X-Admin-Token` |

## 1.2 认证接口（固定）

1) `POST /api/auth/register`  
- 请求体：
  - `username: string`（必填，唯一）
  - `password: string`（必填，强密码策略）
  - `captchaToken?: string`（可选；默认启用最小校验闸门回退）
- 响应：`ApiResponse<{ userId: number, username: string }>`
- 反滥用要求（注册必须实现）：
  - IP 维度注册频控（滑动窗口）
  - 用户名去重 + 弱口令拒绝
  - 行为验证码（Turnstile 风格，可配置回退；默认启用）

2) `POST /api/auth/login`  
- 请求体：
  - `username: string`
  - `password: string`
- 响应：`ApiResponse<{ accessToken: string, refreshToken: string, expiresIn: number }>`

3) `POST /api/auth/refresh`  
- 请求体：
  - `refreshToken: string`
- 响应：`ApiResponse<{ accessToken: string, refreshToken: string, expiresIn: number }>`

4) `POST /api/auth/logout`  
- 请求头：`Authorization: Bearer <accessToken>`
- 请求体：
  - `refreshToken: string`
- 响应：`ApiResponse<null>`

5) `GET /api/auth/me`  
- 请求头：`Authorization: Bearer <accessToken>`
- 响应：`ApiResponse<{ userId: number, username: string, roles: string[], permissions: string[] }>`

### 1.1 鉴权模型（RBAC，数据库驱动）

- **基本约定**
  - `roles`：以 `ROLE_` 前缀表示角色（如 `ROLE_USER`、`ROLE_ADMIN`）
  - `permissions`：以 `PERM_` 前缀表示权限（如 `PERM_AI_ACCESS`）
  - Spring Security 侧统一用 `authority` 字符串做鉴权（`hasAuthority(...)`）

- **数据模型（MySQL / Flyway）**
  - `user_accounts`：仅存账号基础信息（不再存储 `roles` 字段）
  - `rbac_roles(authority)`：角色表（唯一）
  - `rbac_permissions(authority)`：权限表（唯一）
  - `rbac_user_roles(user_id, role_id)`：用户-角色关联（多对多）
  - `rbac_role_permissions(role_id, permission_id)`：角色-权限关联（多对多）

- **默认权限集（种子数据）**
  - `ROLE_USER` -> `PERM_AI_ACCESS`、`PERM_COMMENT_WRITE`
  - `ROLE_ADMIN` -> `PERM_ADMIN_PANEL`、`PERM_AI_ACCESS`、`PERM_COMMENT_WRITE`

## 2. API清单（URL + 参数）

### 2.1 健康检查

1) `GET /api/health`  
- 查询参数：无  
- 请求体：无  
- 响应：`ApiResponse<HealthVO>`
  - `data.mysql: boolean`
  - `data.redis: boolean`
  - `data.elasticsearch: boolean`
  - `data.elasticsearchStatus: string`
  - `data.minio: boolean`

### 2.2 项目（公开，分页默认：`page=0`，`limit=10`）

1) `GET /api/projects`  
- 查询参数：
  - `category?: string`
  - `page?: number`（默认 `0`）
  - `limit?: number`（默认 `10`）  
- 响应：`ApiResponse<PagedResult<ProjectResponseDTO>>`

2) `GET /api/projects/{id}`  
- 路径参数：
  - `id: number`  
- 响应：`ApiResponse<ProjectResponseDTO>`

### 2.3 项目（管理，分页默认：`page=0`，`limit=10`）

公共要求：请求头需携带 `Authorization: Bearer <accessToken>` 且具备 `ROLE_ADMIN`，并携带 `X-Admin-Token`

1) `GET /api/admin/projects`  
- 查询参数：
  - `category?: string`
  - `visible?: boolean`（可选，用于筛选可见状态）
  - `page?: number`（默认 `0`）
  - `limit?: number`（默认 `10`）  
- 响应：`ApiResponse<PagedResult<ProjectResponseDTO>>`

2) `GET /api/admin/projects/{id}`  
- 路径参数：`id: number`  
- 响应：`ApiResponse<ProjectResponseDTO>`

3) `POST /api/admin/projects`  
- 请求体：`ProjectCreateRequest`
  - `title: string`（必填，<=200）
  - `summary: string`（必填，<=500）
  - `description?: string`
  - `category?: string`（<=80）
  - `tags: string[]`（必填，元素必填且<=60，最多 10 个）
  - `coverUrl?: string`（<=1000）
  - `projectUrl?: string`（<=1000）
  - `sourceUrl?: string`（<=1000）
  - `sortOrder?: number`
  - `visible: boolean`（必填）  
- 响应：`ApiResponse<ProjectResponseDTO>`

4) `PUT /api/admin/projects/{id}`  
- 路径参数：`id: number`  
- 请求体：`ProjectUpdateRequest`（字段与创建一致）  
- 响应：`ApiResponse<ProjectResponseDTO>`

5) `DELETE /api/admin/projects/{id}`  
- 路径参数：`id: number`  
- 响应：`ApiResponse<null>`

### 2.4 博客（公开，分页默认：`page=0`，`limit=10`）

1) `GET /api/blogs`  
- 查询参数：
  - `category?: string`
  - `tag?: string`
  - `page?: number`（默认 `0`）
  - `limit?: number`（默认 `10`）  
- 响应：`ApiResponse<PagedResult<BlogResponseDTO>>`

2) `GET /api/blogs/{id}`  
- 路径参数：`id: number`  
- 响应：`ApiResponse<BlogResponseDTO>`

3) `GET /api/blogs/slug/{slug}`  
- 路径参数：
  - `slug: string`
- 响应：`ApiResponse<BlogResponseDTO>`

### 2.5 博客（管理，分页默认：`page=0`，`limit=10`）

公共要求：请求头需携带 `Authorization: Bearer <accessToken>` 且具备 `ROLE_ADMIN`，并携带 `X-Admin-Token`

1) `GET /api/admin/blogs`  
- 查询参数：
  - `status?: string`（如 `DRAFT` / `PUBLISHED`）
  - `category?: string`
  - `page?: number`（默认 `0`）
  - `limit?: number`（默认 `10`）  
- 响应：`ApiResponse<PagedResult<BlogResponseDTO>>`

2) `GET /api/admin/blogs/{id}`  
- 路径参数：`id: number`  
- 响应：`ApiResponse<BlogResponseDTO>`

3) `POST /api/admin/blogs`  
- 请求体：`BlogCreateRequest`
  - `title: string`（必填，<=160）
  - `slug: string`（必填，<=200）
  - `summary: string`（必填，<=500）
  - `category?: string`（<=80）
  - `tags: string[]`（必填，元素必填且<=60）
  - `content: string`（必填）
  - `coverUrl?: string`（<=1000）
- 响应：`ApiResponse<BlogResponseDTO>`

4) `PUT /api/admin/blogs/{id}`  
- 路径参数：`id: number`  
- 请求体：`BlogUpdateRequest`（字段与创建一致）  
- 响应：`ApiResponse<BlogResponseDTO>`

5) `DELETE /api/admin/blogs/{id}`  
- 路径参数：`id: number`  
- 响应：`ApiResponse<null>`

6) `POST /api/admin/blogs/{id}/publish`  
- 路径参数：`id: number`  
- 响应：`ApiResponse<BlogResponseDTO>`

7) `POST /api/admin/blogs/{id}/unpublish`  
- 路径参数：`id: number`  
- 响应：`ApiResponse<BlogResponseDTO>`

### 2.6 搜索（分页默认：`page=0`，`limit=10`）

1) `GET /api/search`  
- 查询参数：
  - `q: string`（必填，关键词）
  - `type?: string`（可选：`blog` / `project`）
  - `page?: number`（默认 `0`）
  - `limit?: number`（默认 `10`）  
- 响应：`ApiResponse<PagedResult<SearchItemDTO>>`

### 2.7 搜索索引管理

公共要求：请求头需携带 `Authorization: Bearer <accessToken>` 且具备 `ROLE_ADMIN`，并携带 `X-Admin-Token`

1) `POST /api/admin/search/consume`  
- 参数：无  
- 响应：`ApiResponse<{ processed: number }>`

2) `POST /api/admin/search/rebuild`  
- 参数：无  
- 响应：`ApiResponse<null>`

### 2.8 文件管理（分页默认：`page=0`，`limit=10`）

1) `POST /api/admin/files/upload`  
- 请求头：`Content-Type: multipart/form-data`  
- 表单参数：
  - `file: MultipartFile`（必填）  
  - `folder?: string`（可选；用于生成 storage_key 的目录前缀）
- 响应：`ApiResponse<FileItemDTO>`

2) `GET /api/admin/files`  
- 查询参数：
  - `page?: number`（默认 `0`）
  - `limit?: number`（默认 `10`）  
- 响应：`ApiResponse<PagedResult<FileItemDTO>>`

3) `DELETE /api/admin/files/{id}`  
- 路径参数：`id: number`  
- 响应：`ApiResponse<null>`

4) `GET /api/files/{id}/download`  
- 路径参数：`id: number`  
- 响应：文件流（非 `ApiResponse`）

### 2.9 评论模块

1) `GET /api/comments`  
- 查询参数：
  - `targetType: string`（如 `blog` / `project`）
  - `targetId: number`
  - `page?: number`（默认 `0`）
  - `limit?: number`（默认 `10`）
- 响应：`ApiResponse<PagedResult<CommentItemDTO>>`
- 权限：匿名可访问

2) `POST /api/comments`  
- 请求头：`Authorization: Bearer <accessToken>`
- 请求体：
  - `targetType: string`
  - `targetId: number`
  - `content: string`
- 响应：`ApiResponse<CommentItemDTO>`
- 权限：登录必需

3) `POST /api/comments/{id}/reply`  
- 请求头：`Authorization: Bearer <accessToken>`
- 路径参数：`id: number`
- 请求体：
  - `content: string`
- 响应：`ApiResponse<CommentItemDTO>`
- 权限：登录必需

4) `POST /api/comments/{id}/like`  
- 请求头：`Authorization: Bearer <accessToken>`
- 路径参数：`id: number`
- 响应：`ApiResponse<{ liked: boolean, likeCount: number }>`
- 权限：登录必需

### 2.10 AI助手（固定为 GET + SSE）

1) `GET /api/ai/chat/stream`  
- 请求头：
  - `Authorization: Bearer <accessToken>`（必填）
- 查询参数：
  - `q: string`（必填，问题）
  - `sessionId?: string`（可选，会话续聊）  
- 响应：`text/event-stream`（SSE）
  - `event: delta` -> 增量文本
  - `event: done` -> `{ sessionId, sources[] }`
  - `event: error` -> 错误信息
- 固定规则：
  - 对话隔离键：`conversationId = 当前登录userId + ":" + sessionId`
  - 若 `sessionId` 为空，服务端先创建 `sessionId` 再开始流式响应
  - 模型调用失败需走降级链路（重试后返回可解释错误，不静默断流）

2) `GET /api/ai/sessions`  
- 请求头：
  - `Authorization: Bearer <accessToken>`（必填）  
- 响应：`ApiResponse<SessionSummary[]>`

3) `GET /api/ai/sessions/{sessionId}/messages`  
- 路径参数：`sessionId: string`  
- 请求头：
  - `Authorization: Bearer <accessToken>`（必填）  
- 响应：`ApiResponse<ChatMessageItem[]>`

4) `DELETE /api/ai/sessions/{sessionId}`  
- 路径参数：`sessionId: string`  
- 请求头：
  - `Authorization: Bearer <accessToken>`（必填）  
- 响应：`ApiResponse<null>`

### 2.11 内容聚合（M1 页面内容）

1) `GET /api/content/home`  
- 查询参数：无
- 响应：`ApiResponse<{ title: string, summary: string, sections: any[], updatedAt: string }>`

2) `GET /api/content/about`  
- 查询参数：无
- 响应：`ApiResponse<{ title: string, summary: string, sections: any[], updatedAt: string }>`

3) `GET /api/content/experience`  
- 查询参数：无
- 响应：`ApiResponse<{ title: string, summary: string, sections: any[], updatedAt: string }>`

### 2.12 通用 DTO 字段约定（最小字段集）

- `PagedResult<T>`
  - `list: T[]`
  - `total: number`
  - `page: number`
  - `limit: number`

- `ProjectResponseDTO`
  - `id: number`
  - `title: string`
  - `summary: string`
  - `description?: string`
  - `category?: string`
  - `tags: string[]`
  - `coverUrl?: string`
  - `projectUrl?: string`
  - `sourceUrl?: string`
  - `visible: boolean`
  - `sortOrder?: number`
  - `createdAt: string`
  - `updatedAt: string`
  - `deletedAt?: string | null`

- `BlogResponseDTO`
  - `id: number`
  - `title: string`
  - `slug: string`
  - `summary: string`
  - `content?: string | null`
  - `category?: string`
  - `tags: string[]`
  - `status: string`（如 `DRAFT` / `PUBLISHED`）
  - `coverUrl?: string`
  - `viewCount?: number`
  - `publishedAt?: string | null`
  - `createdAt: string`
  - `updatedAt: string`
  - `deletedAt?: string | null`

- `FileItemDTO`
  - `id: number`
  - `fileName: string`
  - `fileType: string`
  - `fileSize: number`
  - `storageKey: string`
  - `accessUrl: string`
  - `uploadedBy?: string | null`
  - `createdAt: string`
  - `deletedAt?: string | null`

- `SearchItemDTO`（搜索列表最小字段集）
  - `sourceType: string`（如 `blog` / `project`）
  - `sourceId: number`
  - `title: string`
  - `url: string`
  - `summary: string`
  - `highlights?: string[]`（按后端高亮片段生成顺序）

- `CommentItemDTO`（评论列表最小字段集）
  - `id: number`
  - `targetType: string`（如 `blog` / `project`）
  - `targetId: number`
  - `content: string`
  - `parentId?: number | null`
  - `likeCount: number`
  - `createdAt: string`

- `CitationItem`（AI 回答 sources[] 最小字段集）
  - `sourceType: string`
  - `sourceId: number`
  - `title: string`
  - `url: string`

- `SessionSummary`
  - `sessionId: string`
  - `title?: string | null`
  - `status: string`（如 `active` / `closed`）
  - `messageCount?: number`
  - `createdAt: string`
  - `updatedAt: string`

- `ChatMessageItem`
  - `role: string`（如 `user` / `assistant`）
  - `content: string`
  - `sources?: CitationItem[]`
  - `tokens?: number | null`
  - `createdAt: string`

## 3. 系统架构

### 3.1 总体分层（树形 + 注释）

```text
01MyWeb
├─ Frontend (Vue3)                         # 页面展示层，处理用户交互与路由切换
│  ├─ Views                                # 页面级组件（首页、项目、博客、AI、搜索等）
│  ├─ API Clients                          # axios 封装，统一调用后端 /api 接口
│  └─ Router                               # URL 到页面组件的映射关系
├─ Backend (Spring Boot)                   # 业务服务层，对外提供 REST/SSE 接口
│  ├─ Controller                           # 接口入口：参数接收、响应包装、鉴权边界
│  ├─ Service                              # 业务规则：项目/博客/搜索/文件/评论/AI 逻辑
│  ├─ Repository                           # 数据访问：JPA 仓储
│  ├─ Entity / DTO                         # 持久化模型与接口传输模型
│  └─ Config / Common / Security           # CORS、过滤器链、异常处理、traceId、JWT 鉴权
└─ Infra (MySQL / Redis / ES / MinIO)      # 基础设施：存储、缓存、搜索、文件能力
```

### 3.2 后端包结构（树形到类 + 注释）

```text
backend/src/main/java/com/myweb
├─ BackendApplication.java                                  # Spring Boot 启动入口（启用定时任务）
├─ common                                                   # 通用能力（统一响应、异常、过滤器）
│  ├─ ApiResponse.java                                      # 统一响应模型
│  ├─ GlobalExceptionHandler.java                           # 全局异常转标准错误响应
│  ├─ NotFoundException.java                                # 404 业务异常
│  ├─ PagedResult.java                                      # 分页返回模型
│  ├─ RateLimitFilter.java                                  # /api/ai 与 /api/search 限流（userId 优先）+ 安全响应头
│  └─ TraceIdFilter.java                                    # 透传/生成 X-Trace-Id
├─ config                                                   # Web 与基础配置
│  ├─ CorsConfig.java                                       # 跨域配置
│  └─ RestTemplateConfig.java                               # RestTemplate Bean 配置
├─ security                                                 # 安全与认证
│  ├─ SecurityConfig.java                                   # Spring Security 路由鉴权与放行策略
│  ├─ JwtAuthenticationFilter.java                          # JWT 解析与用户上下文注入
│  ├─ JwtTokenProvider.java                                 # access/refresh 生成与校验
│  ├─ CustomUserDetailsService.java                         # 用户加载与权限映射
│  └─ PasswordConfig.java                                   # 密码加密器（BCrypt）
├─ controller                                               # 接口控制器
│  ├─ AuthController.java                                   # 注册/登录/刷新/登出/当前用户
│  ├─ AdminBlogController.java                              # 后台博客 CRUD + 发布/下线
│  ├─ AdminFileController.java                              # 后台文件上传/列表/删除
│  ├─ AdminProjectController.java                           # 后台项目 CRUD
│  ├─ AdminSearchController.java                            # 搜索索引消费与重建
│  ├─ AiController.java                                     # AI SSE 对话 + 会话管理（JWT 用户隔离）
│  ├─ BlogPublicController.java                             # 前台博客列表/详情
│  ├─ CommentController.java                                # 评论列表/创建/回复/点赞
│  ├─ ContentPublicController.java                          # M1 首页/关于/经历聚合内容读取
│  ├─ FilePublicController.java                             # 文件下载
│  ├─ HealthController.java                                 # 健康检查（MySQL/Redis/ES/MinIO）
│  ├─ HealthVO.java                                         # 健康检查返回对象
│  ├─ ProjectPublicController.java                          # 前台项目列表/详情
│  └─ SearchController.java                                 # 公开搜索
├─ dto                                                      # 请求/响应 DTO
│  ├─ BlogCreateRequest.java                                # 博客创建参数
│  ├─ BlogResponseDTO.java                                  # 博客响应模型
│  ├─ BlogUpdateRequest.java                                # 博客更新参数
│  ├─ CommentCreateRequest.java                             # 评论创建参数
│  ├─ CommentReplyRequest.java                              # 评论回复参数
│  ├─ CommentItemDTO.java                                   # 评论响应模型
│  ├─ ContentPageDTO.java                                   # 内容聚合页面模型
│  ├─ FileItemDTO.java                                      # 文件响应模型
│  ├─ ProjectCreateRequest.java                             # 项目创建参数
│  ├─ ProjectResponseDTO.java                               # 项目响应模型
│  ├─ ProjectUpdateRequest.java                             # 项目更新参数
│  └─ SearchItemDTO.java                                    # 搜索结果项
├─ entity                                                   # JPA 实体与枚举
│  ├─ User.java                                             # 用户实体（账号状态/角色）
│  ├─ RefreshToken.java                                     # 刷新令牌实体（可吊销）
│  ├─ Blog.java                                             # 博客实体
│  ├─ BlogStatus.java                                       # 博客状态枚举
│  ├─ ChatMessage.java                                      # AI 会话消息实体
│  ├─ Comment.java                                          # 评论实体（含回复关系/点赞计数）
│  ├─ ManagedFile.java                                      # 文件实体
│  ├─ Project.java                                          # 项目实体
│  ├─ SearchDocument.java                                   # 搜索索引文档实体
│  ├─ SearchOutboxEvent.java                                # 搜索 outbox 事件实体
│  └─ SearchSyncEventType.java                              # 搜索同步事件类型枚举
├─ repository                                               # 数据访问层
│  ├─ UserRepository.java
│  ├─ RefreshTokenRepository.java
│  ├─ BlogRepository.java
│  ├─ ChatMessageRepository.java
│  ├─ CommentRepository.java
│  ├─ ManagedFileRepository.java
│  ├─ ProjectRepository.java
│  ├─ SearchDocumentRepository.java
│  └─ SearchOutboxEventRepository.java
└─ service                                                  # 业务逻辑层
   ├─ AuthService.java                                      # 认证编排（注册/登录/刷新/登出）
   ├─ AiAssistantService.java                               # AI 问答编排与会话管理（conversationId/threadId 口径）
   ├─ BlogSearchSyncPublisher.java                          # 博客搜索同步发布接口
   ├─ BlogService.java                                      # 博客核心业务
   ├─ CommentService.java                                   # 评论业务（创建/回复/点赞/分页）
   ├─ ContentQueryService.java                              # M1 页面内容聚合查询
   ├─ FileStorageService.java                               # 文件存储业务
   ├─ NoopBlogSearchSyncPublisher.java                      # 搜索同步空实现
   ├─ ProjectSearchSyncPublisher.java                       # 项目搜索同步发布
   ├─ ProjectService.java                                   # 项目核心业务
   ├─ SearchIndexService.java                               # 搜索索引消费/重建
   ├─ SearchOutboxPublisher.java                            # outbox 事件发布
   └─ SearchService.java                                    # 搜索查询服务
```

### 3.3 前端结构（树形到页面/模块 + 注释）

```text
frontend/src
├─ App.vue                                                   # 根组件
├─ main.ts                                                   # 应用入口（挂载 Pinia/Router/ElementPlus）
├─ style.css                                                 # 全局基础样式
├─ api                                                       # 接口调用封装层
│  ├─ auth.ts                                                # 注册/登录/refresh/me
│  ├─ adminBlogs.ts                                          # 后台博客 API
│  ├─ adminFiles.ts                                          # 后台文件 API
│  ├─ adminProjects.ts                                       # 后台项目 API
│  ├─ ai.ts                                                  # AI 助手 API（会话/流式）
│  ├─ blogs.ts                                               # 前台博客 API
│  ├─ comments.ts                                            # 评论 API
│  ├─ content.ts                                             # M1 内容聚合 API
│  ├─ files.ts                                               # 前台文件下载 API
│  ├─ health.ts                                              # 健康检查 API
│  ├─ projects.ts                                            # 前台项目 API
│  └─ search.ts                                              # 搜索 API
├─ router
│  └─ index.ts                                               # 路由表定义
├─ views                                                     # 页面级视图
│  ├─ LoginView.vue                                          # /login
│  ├─ HomeView.vue                                           # /
│  ├─ AboutView.vue                                          # /about
│  ├─ ExperienceView.vue                                     # /experience
│  ├─ ContactView.vue                                        # /contact
│  ├─ ProjectsView.vue                                       # /projects
│  ├─ ProjectDetailView.vue                                  # /projects/:id
│  ├─ AdminProjectsView.vue                                  # /admin/projects
│  ├─ BlogView.vue                                           # /blog
│  ├─ BlogDetailView.vue                                     # /blog/:id
│  ├─ AdminBlogsView.vue                                     # /admin/blogs
│  ├─ AdminFilesView.vue                                     # /admin/files
│  ├─ SearchView.vue                                         # /search
│  ├─ AiAssistantView.vue                                    # /ai
│  └─ NotFoundView.vue                                       # /:pathMatch(.*)*
├─ components                                                # 可复用组件
│  ├─ HelloWorld.vue                                         # 示例组件
│  ├─ common/Card.vue                                        # 通用卡片
│  ├─ common/Section.vue                                     # 通用区块容器
│  ├─ common/TimeLine.vue                                    # 时间线组件
│  └─ contact/ContactForm.vue                                # 联系表单
├─ styles
│  └─ global.scss                                            # 全局 SCSS 变量与基础风格
└─ assets
   ├─ hero.png                                               # 首页资源图
   ├─ vite.svg
   └─ vue.svg
```

## 4. 主要调用链（简图）

1) 页面触发请求  
`views/*` -> `api/*.ts` -> `Controller`

2) 后端处理流程  
`JwtAuthenticationFilter` -> `Controller` -> `Service` -> `Repository` -> `MySQL`  
`Service`（搜索相关）-> `SearchOutboxEvent` -> `SearchIndexService` -> `Elasticsearch`

2.1) 认证流程  
`AuthController` -> `AuthService` -> `UserRepository/RefreshTokenRepository` -> `JwtTokenProvider` -> 返回 access/refresh

3) AI 流式会话  
`AiAssistantView.vue (EventSource)` -> `AiController#chat/stream` -> `AiAssistantService`（按 `userId + sessionId` 隔离）-> SSE `delta/done/error`

