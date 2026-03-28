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

### Docker 挂死 / 引擎 API 500（常见为 Desktop + WSL2 后端异常）

1. 在 **PowerShell 或 CMD**（建议管理员）执行：`wsl --shutdown`，然后重新启动 Docker Desktop，确认引擎就绪后再执行上面的 `docker compose`。
2. 若已恢复正常，可在项目根目录按需停止本仓库 Compose 栈（项目名与目录一致时为 `01myweb`）：
   ```bash
   docker compose -p 01myweb stop
   ```
   或直接下线容器与默认网络：
   ```bash
   docker compose -p 01myweb down
   ```

## 3. 启动后端

1. 确保根目录 `.env` 已配置必要环境变量（不要提交真实密钥）。
2. 启动应用：

```bash
cd backend
mvn spring-boot:run
```

后端仅使用 **8080** 端口，访问地址：`http://localhost:8080`。

若启动失败提示 8080 已被占用，请先释放该端口再重新运行后端：结束占用 8080 的进程后，再执行 `mvn spring-boot:run`。Windows 可先用 `netstat -ano | findstr :8080` 查看占用进程的 PID，再用 `taskkill /PID <pid> /F` 结束对应进程。

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