# M6 AI 助手与 RAG 模块（细化版）

> 约束：本文件仅定义 M6 执行清单；接口字段、响应结构、鉴权与分页口径统一以 `API文档与系统架构.md` 为准。

## 1. 模块目标

交付可流式输出、可引用来源、可保存会话的 AI 助手闭环，并具备后续接入真实模型和向量库的扩展能力。

## 2. 功能清单

### 2.1 对话接口与会话能力

- SSE 接口：`GET /api/ai/chat/stream`
- 事件协议：`delta`、`done`、`error`
- 会话 ID 管理：支持新建与续聊
- 消息持久化：用户消息与助手消息落库

### 2.2 检索增强能力

- 结合站内搜索结果构建回答上下文
- 返回引用来源：
  - `sourceType`
  - `sourceId`
  - `title`
  - `url`

### 2.3 安全与稳定性

- 问题参数校验（空问题拒绝）
- 异常事件明确返回，不静默失败
- 频控策略与超时控制预留
- 鉴权固定：接入 `Spring Security + JWT`，AI 全接口必须登录后访问

### 2.4 演进路线（后续增强）

- 接入 DashScope 实模型
- 向量库检索 + BM25 + RRF 融合
- 会话冷热分层（Redis + MySQL）

### 2.5 AskUserQuestion 结果（已锁定）

> 以下为本项目当前锁定的默认实现口径（不再保留来源过程描述）。

- 架构：`ChatClient + Agent Graph`（当前以 ChatClient 主流程交付，Graph 预留检查点持久化能力）
- 短期记忆：`MessageWindowChatMemory + Summary`（窗口压缩并行，防止上下文膨胀）
- 持久化：`Redis + JDBC(MySQL)`（Redis 热数据、MySQL 冷数据/审计）
- 会话隔离：`userId + sessionId` 组合键（作为 conversationId/threadId）
- 长期记忆：`RAG + 用户画像`（提取并沉淀用户偏好与稳定事实）
- 安全优先级：输入校验、限流、失败降级（fallback）
- 交付策略：平衡型（功能完整 + 稳定性）
- 认证策略：账号密码登录 + JWT（Access + Refresh），Access 默认 24h
- 权限策略：AI 与评论写操作需登录；后台管理接口需 `ROLE_ADMIN`
- 注册策略：开放注册，但必须具备反滥用（IP 频控 + 验证码 + 弱口令拒绝）

## 3. 会话与消息表结构

### 3.1 AI 会话表

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | BIGINT | 是 | 主键，自增 |
| session_id | VARCHAR(64) | 是 | 会话唯一标识（UUID） |
| title | VARCHAR(200) | 否 | 会话标题（首条消息摘要） |
| status | VARCHAR(20) | 是 | 状态：active、closed，默认 active |
| message_count | INT | 否 | 消息数量，默认 0 |
| created_at | DATETIME | 否 | 创建时间 |
| updated_at | DATETIME | 否 | 更新时间 |
| deleted_at | DATETIME | 否 | 删除时间 |

索引：session_id（唯一）、status、created_at

### 3.2 AI 消息表

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | BIGINT | 是 | 主键，自增 |
| session_id | VARCHAR(64) | 是 | 会话 ID |
| role | VARCHAR(20) | 是 | 角色：user、assistant |
| content | LONGTEXT | 是 | 消息内容 |
| sources | JSON | 否 | 引用来源（JSON 数组） |
| tokens | INT | 否 | token 消耗数量 |
| created_at | DATETIME | 否 | 创建时间 |

索引：session_id、created_at

## 4. SSE 事件协议

### 4.1 事件协议（唯一契约引用）

- SSE 事件名与语义：以 `API文档与系统架构.md` 的 `GET /api/ai/chat/stream` 为准（仅 `delta/done/error`）。
- 本文件不再定义 SSE `data:` 的 JSON 结构，避免与唯一契约产生二次定义冲突。

### 4.3 接口契约

#### 4.3.1 流式对话

- 接口路径：`GET /api/ai/chat/stream`
- 请求头：`Authorization: Bearer <accessToken>`（必填）
- 请求参数：
  - sessionId：会话 ID，空则为新建会话
  - q：用户问题内容（必填）
- 返回：SSE 流式响应

#### 4.3.2 会话历史

- 接口路径：`GET /api/ai/sessions`
- 请求头：`Authorization: Bearer <accessToken>`（必填）
- 返回：会话列表

#### 4.3.3 会话详情

- 接口路径：`GET /api/ai/sessions/{sessionId}/messages`
- 请求头：`Authorization: Bearer <accessToken>`（必填）
- 返回：消息列表

#### 4.3.4 删除会话

- 接口路径：`DELETE /api/ai/sessions/{sessionId}`
- 请求头：`Authorization: Bearer <accessToken>`（必填）
- 操作：软删除会话及关联消息

### 4.4 错误响应

- 错误返回结构与 HTTP 状态码口径：统一以 `API文档与系统架构.md` 为准，本文件不再维护“错误码表/状态码表”以避免冲突。

## 5. 引用来源格式

```json
[
  {
    "sourceType": "blog",
    "sourceId": 1,
    "title": "博客标题",
    "url": "/blog/1"
  },
  {
    "sourceType": "project",
    "sourceId": 2,
    "title": "项目标题",
    "url": "/projects/2"
  }
]
```

## 6. 测试计划

- SSE 流式事件顺序正确（delta -> done / error）。
- 引用字段结构完整并可跳转。
- 空问题/异常场景返回规范错误。

## 7. 验收标准（逐项打勾）

### 7.1 数据库层
- [ ] AI 会话表结构正确
- [ ] AI 消息表结构正确
- [ ] 软删除逻辑生效

### 7.2 对话接口
- [ ] `GET /api/ai/chat/stream` 流式响应正常
- [ ] delta 事件正确发送内容片段
- [ ] done 事件包含完整内容和引用来源
- [ ] error 事件正确返回错误信息
- [ ] 新建会话返回新 sessionId
- [ ] 续聊可传入已有 sessionId

### 7.3 会话管理
- [ ] `GET /api/ai/sessions` 返回会话列表
- [ ] `GET /api/ai/sessions/{sessionId}/messages` 返回消息历史
- [ ] `DELETE /api/ai/sessions/{sessionId}` 删除成功

### 7.4 引用功能
- [ ] 引用来源包含 sourceType、sourceId、title、url
- [ ] 引用 URL 可跳转

### 7.5 安全与稳定性
- [ ] 空消息返回错误
- [ ] 异常情况返回 error 事件
- [ ] 超时控制生效
- [ ] conversationId 隔离生效（不同用户/会话不串台）
- [ ] 限流策略生效（用户维度/会话维度）
- [ ] 调用失败降级链路生效（重试 + 可解释错误）

### 7.6 构建与测试
- [ ] `mvn test` 全量通过
- [ ] `npm run build` 构建成功

## 8. DoD（完成定义）

- AI 对话可用，引用链路可追溯。
- 会话数据可审计，可用于后续分析。
- 回归不影响 M1~M5。

## 8A. 固定技术实现方案（禁止自由发挥）

### 8A.1 固定实现顺序

1. 建立会话表与消息表（含索引与软删字段）。
2. 实现 SSE 对话接口，事件固定为 `delta/done/error`。
3. 实现会话管理接口（列表/消息详情/删除）。
4. 实现站内检索增强与引用返回。
5. 完成异常链路、超时控制、测试回归。

### 8A.2 固定技术方案

- 会话协议：`sessionId` 为空则创建新会话，非空则续聊。
- SSE 协议：只允许 3 种事件，禁止新增自定义事件名破坏前端契约。
- 回答策略：优先知识库；知识不足时可补充通用知识，并必须脚注 `（来源：通用公开信息）`。
- 引用最小要求：每条引用至少包含 `title` 与 `url`，结构仍保留 `sourceType/sourceId`。
- 错误处理：异常必须发送 `error` 事件，不允许静默断流。

### 8A.3 通用约束引用（固定）

- 本阶段“验证方式 / 禁止项 / 提交要求”统一执行 `00C-最终压缩执行版（AI只读此文档先行）.md`，不在本文件重复定义。

---

## 9. 阶段执行卡（预填）

- 阶段：`M6`
- 负责人：``
- 状态：`未开始`
- 开始时间：``
- 更新时间：`2026-03-24`

### 9.1 本阶段目标

- 完成 AI SSE 对话、会话管理、来源返回三链路闭环。

### 9.2 本阶段任务

- [ ] 完成 `GET /api/ai/chat/stream`（delta/done/error）
- [ ] 完成会话列表/消息详情/删除接口
- [ ] 完成消息持久化与会话续聊
- [ ] 完成来源返回结构（title/url 最小必填）
- [ ] 完成异常链路与超时验证

### 9.3 测试证据

- 后端：`mvn test` -> ``
- 前端：`npm run build` -> ``
- 联调说明：``

### 9.4 DoD 结论

- 全局闸门填写位置：`99-阶段验收清单.md` 的 `M6` 结论区。
- 本文件仅记录 M6 差异项与证据。
