# 01MyWeb

M0（基础框架与联调链路）当前已可运行，包含：

- 前端：Vue 3 + Vite + Router + Pinia
- 后端：Spring Boot 3.5.12 + Java 21
- 基础服务：MySQL / Redis / Elasticsearch / MinIO（Docker Compose）
- Nginx：静态托管 + `/api` 反向代理
- 健康检查：`GET /api/health`（统一响应体 + traceId）

## 1. 环境准备

- Java 21
- Node.js 20+
- Maven 3.9+
- Docker Desktop（含 Compose V2）

## 2. 启动基础服务

在项目根目录执行：

```bash
docker compose up -d mysql redis elasticsearch minio
```

可选：若已构建前端产物，也可启动 Nginx：

```bash
docker compose up -d nginx
```

## 3. 启动后端

1. 确保根目录 `.env` 已配置必要环境变量（不要提交真实密钥）。
2. 启动应用：

```bash
cd backend
mvn spring-boot:run
```

后端默认端口：`http://localhost:8080`

### 开发环境默认管理员（Flyway V7）

数据库迁移完成后会写入用户 **`myweb-admin`**，默认密码 **`MyWebAdm1n2026`**（本地/联调用；**生产务必改密或删户**）。该账号具备 `ROLE_ADMIN` / `PERM_ADMIN_PANEL`。

调用 `/api/admin/**` 时除 JWT 外还需请求头 `X-Admin-Token`，需与后端 `APP_ADMIN_TOKEN` 一致；前端开发复制 `frontend/.env.example` 为 `.env` 并设置 `VITE_ADMIN_TOKEN`。

## 4. 启动前端

1. 确保根目录 `.env` 已配置必要环境变量（前端构建时读取对应 `VITE_*` 配置）。
2. 启动开发服务器：

```bash
cd frontend
npm install
npm run dev
```

前端默认端口：`http://localhost:5173`

## 5. 验证联调

- 打开前端页面，顶栏会显示健康状态。
- 直接请求健康检查接口：
  - 本地直连：`http://localhost:8080/api/health`
  - 前端代理：`http://localhost:5173/api/health`
- 响应体字段约定：`success` / `data` / `error` / `timestamp` / `traceId`

## 6. 当前已验证

- `frontend`: `npm run build` 通过
- `backend`: `mvn -B test` 通过