# 校园网服务平台 Web

这个项目是桌面端 Vue 版本，功能逻辑和 Android App 保持一致，当前已经接入 Java 后端接口。

## 已接入的后端能力

- `POST /api/auth/register` 注册
- `POST /api/auth/login` 登录
- `PUT /api/auth/profile/{id}` 更新个人资料
- `GET /api/tickets/users/{userId}` 获取我的工单
- `POST /api/tickets/new-user-bind` 提交新用户绑定工单
- `POST /api/tickets/broadband-password-reset` 提交宽带密码重置工单

## 本地开发

1. 启动后端，默认地址是 `http://127.0.0.1:8080`
2. 在当前目录运行：

```powershell
npm install
npm run dev
```

开发模式下，Vite 会把 `/api` 自动代理到 `http://127.0.0.1:8080`，不需要额外配置前端地址。

## 独立部署

如果前端和后端不是同源部署，可以在当前目录创建 `.env.local`：

```env
VITE_API_BASE_URL=http://你的后端地址:8080
```

项目里已经提供示例文件：

- `.env.example`

## 后端跨域

后端已经增加 `/api/**` 的跨域配置，默认允许：

- `http://localhost:5173`
- `http://127.0.0.1:5173`

如果你后面把网页部署到别的域名，可以在后端环境变量里设置：

```env
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173,https://你的前端域名
```

## 构建

```powershell
npm run build
```
