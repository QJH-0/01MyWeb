# 00D-M6 AI 助手与 RAG：类设计蓝图（按需读取）

> 何时读取：仅当在 M6 阶段需要锁定类名/职责边界时读取。  
> 接口契约：SSE 事件/接口 URL/鉴权/返回字段以 `API文档与系统架构.md` 为准。

---

## 1. 实现方案（M6）

1. 对话入口为 SSE：`delta/done/error`。
2. 召回固定为混合检索：BM25 + 向量，融合策略 RRF。
3. 会话分层：Redis 短期上下文 + MySQL 长期消息。
4. 回答必须返回可追溯来源（`sourceType/sourceId/title/url`）。
5. 模型超时降级到“仅检索回答”。

---

## 2. 类设计（必须存在）

- `AiController`
- `AiAssistantService`
- `AiOrchestrator`
- `RetrievalService`
- `CitationBuilder`
- `ChatSessionCache`（Redis）
- `ChatSessionRepository`（MySQL）
- `ChatMessageRepository`

---

## 3. 方法签名与参数校验

方法签名、入参/出参结构与参数校验细节必须在对应阶段文档（如 `06-M6`）落地定义；本文件仅给出“类职责与边界”，避免过早设计导致上下文膨胀。

