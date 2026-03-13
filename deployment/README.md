# Modelica IDE Online - Nginx 部署包

## 📦 部署包内容

```
deployment/
├── nginx/                      # 前端静态文件
│   ├── index.html
│   └── assets/
├── modelica-ide-backend.jar    # 后端可执行 JAR (19MB)
├── nginx.conf                  # Nginx 配置文件
├── docker-compose.yml          # Docker Compose 配置
└── README.md                   # 本文件
```

## 🚀 快速部署

### 方式 1: Docker Compose (推荐)

1. **启动服务**
   ```bash
   cd deployment
   docker-compose up -d
   ```

2. **访问应用**
   ```
   http://localhost
   ```

3. **停止服务**
   ```bash
   docker-compose down
   ```

### 方式 2: 手动部署

#### 后端部署

1. **启动后端服务**
   ```bash
   java -jar modelica-ide-backend.jar
   ```
   后端将在 `http://localhost:8080` 运行

#### Nginx 部署

1. **复制前端文件到 Nginx**
   ```bash
   cp -r nginx/* /usr/share/nginx/html/
   ```

2. **配置 Nginx**
   ```bash
   cp nginx.conf /etc/nginx/conf.d/modelica-ide.conf
   ```

3. **重启 Nginx**
   ```bash
   nginx -s reload
   ```

## ⚙️ 配置说明

### 后端配置

后端默认配置：
- 端口: `8080`
- API 路径: `/api/*`
- WebSocket 路径: `/ws/*`

可通过环境变量修改：
```bash
java -Dserver.port=8080 -jar modelica-ide-backend.jar
```

### Nginx 配置

`nginx.conf` 已配置：
- 静态文件服务
- API 反向代理 (`/api/` → `http://backend:8080/api/`)
- WebSocket 代理 (`/ws/` → `http://backend:8080/ws/`)

### 环境变量

前端可通过环境变量配置后端地址：
- `VITE_API_URL`: API 地址
- `VITE_WS_URL`: WebSocket 地址

## 📊 系统要求

- **Java**: JDK 17+
- **Nginx**: 1.18+ (如使用 Docker 则不需要)
- **Docker**: 20.10+ (如使用 Docker Compose)
- **内存**: 最少 512MB
- **磁盘**: 最少 100MB

## 🔧 故障排查

### 后端无法启动
```bash
# 检查 Java 版本
java -version

# 检查端口占用
netstat -ano | findstr :8080
```

### 前端无法访问后端
```bash
# 检查 Nginx 配置
nginx -t

# 检查后端是否运行
curl http://localhost:8080/api/health
```

### WebSocket 连接失败
- 检查 Nginx WebSocket 代理配置
- 确认防火墙允许 WebSocket 连接
- 查看浏览器控制台错误信息

## 📝 版本信息

- **前端版本**: 1.0.0
- **后端版本**: 1.0.0-SNAPSHOT
- **构建时间**: 2026-03-04
- **构建工具**: Vite 5.4.21 + Gradle 8.5

## 📄 许可证

MIT License