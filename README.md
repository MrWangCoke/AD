# AD Project

一个围绕校园网问题处理搭建的完整项目，包含 3 个部分：

- Android 客户端：用于用户登录、绑定信息、查看问题类型、提交工单。
- Java 后端：提供用户、绑定、工单相关接口，并持久化到 PostgreSQL。
- Python 自动化：登录学校远程系统，进入“认证计费管理系统 -> 业务管理 -> 修改资料”，按工单顺序自动处理宽带信息。

这个仓库的核心目标不是单独做一个 App 或单独做一个脚本，而是把“前台提交问题 -> 后端入库 -> 自动化去远程系统处理”串成一整条闭环。

## 目录结构

```text
AD-project/
├─ android-app/      Android Jetpack Compose 客户端
├─ backend/          Spring Boot 后端
├─ AD-Automation/    Python 自动化程序
├─ README.md
└─ 启动.txt
```

## 项目流程

整体流程可以理解成下面这条链路：

1. 用户在 Android App 中登录或提交绑定/工单。
2. 后端把用户数据和工单数据写入 PostgreSQL。
3. 自动化程序读取数据库中的待处理工单。
4. 自动化程序登录学校远程系统。
5. 自动化程序进入“修改资料”页面，按顺序填写学号、宽带账号、新密码并提交。
6. 处理成功后，把工单 `status` 更新为 `3`。

当前自动化处理重点依赖这些字段：

- `student_id`：学号
- `broadband_account`：宽带账号
- `new_password`：宽带密码
- `status`：工单状态
- `created_at`：工单创建时间，用于本地游标顺序消费

## 1. Android 客户端

路径：

`E:\MrWang\Desktop\AD-project\android-app`

### 技术栈

- Kotlin
- Jetpack Compose
- Navigation Compose
- Retrofit
- Room
- Material 3

### 当前功能概览

- 主页支持新用户绑定入口。
- 主页展示校园网连接步骤。
- 主页展示问题类型弹窗和处理说明。
- 主页售后卡片支持复制 QQ 群号和微信联系方式。
- 使用浮动玻璃质感底部导航。
- 支持登录、个人资料、消息等页面骨架。

### 关键代码位置

- 应用根布局：
  `E:\MrWang\Desktop\AD-project\android-app\app\src\main\java\com\mrwang\ad\app\AppRoot.kt`
- 主页：
  `E:\MrWang\Desktop\AD-project\android-app\app\src\main\java\com\mrwang\ad\feature\home\HomeScreen.kt`
- 底部导航：
  `E:\MrWang\Desktop\AD-project\android-app\app\src\main\java\com\mrwang\ad\navigation\FloatingBottomBar.kt`
- 玻璃组件：
  `E:\MrWang\Desktop\AD-project\android-app\app\src\main\java\com\mrwang\ad\core\ui\components\GlassComponents.kt`

### 接口地址配置

Android 端的后端地址配置在：

`E:\MrWang\Desktop\AD-project\android-app\app\build.gradle.kts`

当前默认值：

```kotlin
buildConfigField("String", "AUTH_BASE_URL", "\"http://10.0.2.2:8080/\"")
```

说明：

- `10.0.2.2` 适用于 Android 模拟器访问当前电脑上的本地后端。
- 如果改成真机联调，需要换成电脑局域网 IP 或已部署域名。
- 如果改成线上环境，可以填类似：
  `https://your-domain/`

### 运行要求

- Android Studio
- Android SDK
- JDK 17 更稳妥

说明：

- 模块里 Kotlin/JVM 目标是 `11`
- 但当前 Android 构建环境通常建议直接使用 JDK 17

### 运行方式

1. 用 Android Studio 打开 `E:\MrWang\Desktop\AD-project\android-app`
2. 确认 `AUTH_BASE_URL` 指向正确的后端地址
3. 启动模拟器或连接真机
4. 运行 `app` 模块

## 2. Java 后端

路径：

`E:\MrWang\Desktop\AD-project\backend`

### 技术栈

- Java 17
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- PostgreSQL
- Lombok

### 当前接口职责

后端目前主要包含两类能力：

- 用户相关接口
- 工单相关接口

### 主要接口

认证与用户：

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/bind`
- `PUT /api/auth/profile/{id}`

工单：

- `POST /api/tickets/new-user-bind`
- `GET /api/tickets/pending`
- `GET /api/tickets/users/{userId}`

关键控制器位置：

- `E:\MrWang\Desktop\AD-project\backend\src\main\java\dx\ahut\adbackend\auth\AuthController.java`
- `E:\MrWang\Desktop\AD-project\backend\src\main\java\dx\ahut\adbackend\ticket\TicketController.java`

### 数据库

默认数据库：

`campus_network_db`

`application.properties` 位置：

`E:\MrWang\Desktop\AD-project\backend\src\main\resources\application.properties`

当前默认配置：

```properties
server.port=8080
server.address=0.0.0.0

spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/campus_network_db}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:postgres}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
```

### 工单表说明

自动化当前使用的是 `tickets` 表。

项目里定义的典型状态值：

- `0`：待处理
- `1`：已入队，也视为自动化待处理
- `2`：处理中
- `3`：已完成

项目里定义的典型工单类型：

- `1`：新用户绑定
- `2`：账户不存在
- `3`：宽带密码相关

对应实体：

`E:\MrWang\Desktop\AD-project\backend\src\main\java\dx\ahut\adbackend\ticket\Ticket.java`

### 本地启动方式

#### 方式 1：直接 Maven 启动

在 `E:\MrWang\Desktop\AD-project\backend` 下运行：

```powershell
.\mvnw.cmd spring-boot:run
```

启动前需要准备数据库环境变量：

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/campus_network_db"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="your_password"
```

#### 方式 2：使用脚本启动

先复制：

`E:\MrWang\Desktop\AD-project\backend\.env.local.example`

为：

`E:\MrWang\Desktop\AD-project\backend\.env.local`

然后填入数据库密码，再运行：

```powershell
.\start-backend.ps1
```

脚本特点：

- 自动读取 `.env.local`
- 自动切到 `backend` 目录
- 自动设置 `JAVA_HOME`
- 自动检查 `8080` 端口是否已被占用

脚本位置：

`E:\MrWang\Desktop\AD-project\backend\start-backend.ps1`

### 验证后端是否启动成功

本地启动成功后，可以直接访问：

- [http://localhost:8080/api/tickets/pending](http://localhost:8080/api/tickets/pending)

如果数据库中没有数据，通常会返回空数组 `[]`。

## 3. Python 自动化

路径：

`E:\MrWang\Desktop\AD-project\AD-Automation`

### 技术栈

- Python
- Playwright
- PyAutoGUI
- OpenCV
- psycopg
- requests

### 自动化职责

这部分不是普通接口脚本，而是完整的“浏览器 + 图像定位 + 远程系统操作”自动化。

它会负责：

- 打开学校运维安全网关页面
- 登录校园网远程系统
- 选择保存的 `dx` 账号
- 处理图形验证码
- 检测是否真正进入远程页面
- 打开：
  - 认证计费管理系统
  - 业务管理
  - 修改资料
- 从数据库顺序读取待处理工单
- 自动填写并提交工单内容
- 成功后把工单状态改为 `3`

### 关键脚本

- 主入口：
  `E:\MrWang\Desktop\AD-project\AD-Automation\main.py`
- 远程系统主流程：
  `E:\MrWang\Desktop\AD-project\AD-Automation\tasks\remote_drcom_page.py`
- 校园网入口流程：
  `E:\MrWang\Desktop\AD-project\AD-Automation\tasks\campus_portal_task.py`
- 登录逻辑：
  `E:\MrWang\Desktop\AD-project\AD-Automation\tasks\portal_login.py`
- 图片定位：
  `E:\MrWang\Desktop\AD-project\AD-Automation\tasks\image_automation.py`
- 工单读取与状态更新：
  `E:\MrWang\Desktop\AD-project\AD-Automation\tasks\ticket_queue.py`
- 全局配置：
  `E:\MrWang\Desktop\AD-project\AD-Automation\config.py`

### 环境变量

示例文件：

`E:\MrWang\Desktop\AD-project\AD-Automation\.env.example`

当前支持的关键配置包括：

```env
BACKEND_URL=http://localhost:8080
RUN_MODE=tickets
TICKET_SOURCE=api
DATABASE_URL=postgresql://localhost:5432/campus_network_db
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=change_me
TARGET_URL=https://blj.ahut.edu.cn/bhost/
HEADLESS=false
BROWSER_CHANNEL=chrome
CHROME_CDP_URL=
USE_AUTH_STATE=false
PORTAL_USERNAME=your_username
PORTAL_PASSWORD=your_password
```

说明：

- `RUN_MODE=portal`：真正执行浏览器自动化流程
- `RUN_MODE=tickets`：只读取并打印待处理工单摘要
- `TICKET_SOURCE=api`：通过后端接口读取工单
- `TICKET_SOURCE=db`：直接连 PostgreSQL 读取工单

当前代码也支持下面这组数据库配置：

```env
DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_NAME=campus_network_db
DATABASE_USER=postgres
DATABASE_PASSWORD=your_password
```

也就是说，自动化读取数据库时有两种方式：

1. 配置 `DATABASE_URL`
2. 不配 `DATABASE_URL`，改用 `DATABASE_HOST / DATABASE_PORT / DATABASE_NAME / DATABASE_USER / DATABASE_PASSWORD`

### 图片模板

自动化会用到 `assets` 目录中的模板图。

常见模板包括：

- 用户名输入框
- `dx` 候选账号
- 验证码输入框
- 小人头像
- 认证计费管理系统
- 业务管理
- 修改资料
- 学号输入框
- 查询按钮
- 宽带账号输入框
- 宽带密码输入框
- 确认按钮

路径：

`E:\MrWang\Desktop\AD-project\AD-Automation\assets`

### 自动化运行前准备

1. 安装 Python
2. 创建并激活虚拟环境
3. 安装依赖
4. 安装 Playwright 浏览器
5. 配好 `.env`
6. 确认模板图齐全

示例命令：

```powershell
cd E:\MrWang\Desktop\AD-project\AD-Automation
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
playwright install
```

### 自动化运行方式

#### 方式 1：只看待处理工单

`.env` 中配置：

```env
RUN_MODE=tickets
```

然后运行：

```powershell
.\.venv\Scripts\python.exe .\main.py
```

#### 方式 2：执行完整远程处理流程

`.env` 中配置：

```env
RUN_MODE=portal
TICKET_SOURCE=db
```

然后运行：

```powershell
.\.venv\Scripts\python.exe .\main.py
```

### 当前自动化工单处理逻辑

进入“修改资料”页面后，自动化会：

1. 进入工单处理循环
2. 从数据库读取 `status in (0, 1)` 的下一条工单
3. 读取顺序按 `created_at ASC, id ASC`
4. 通过本地游标记录上次处理位置
5. 没有新工单时，每秒刷新一次“修改资料”并打印空闲时长
6. 有工单时，自动填写：
   - 学号
   - 查询
   - 宽带账号
   - 宽带密码
   - 回车提交
   - 点击确认
7. 成功后把工单状态更新为 `3`

本地游标文件路径：

`E:\MrWang\Desktop\AD-project\AD-Automation\output\ticket_cursor.json`

### 验证码处理说明

当前验证码流程是“自动化辅助 + 人工兜底”的混合模式。

大致行为：

- 自动截取验证码图
- 尝试识别或辅助定位输入框
- 需要时让人工查看截图并输入验证码
- 提交后检测是否仍停留在验证码页
- 如果没真正进入下一页，会重新走用户名和 `dx` 账号选择，再重新输入验证码

### 输出目录

输出目录：

`E:\MrWang\Desktop\AD-project\AD-Automation\output`

主要用于：

- 验证码截图
- 验证码组合图
- 本地工单游标
- 调试中间文件

当前逻辑会在合适时机自动清理旧验证码截图，避免目录里堆太多历史文件。

## 推荐启动顺序

建议按下面顺序本地联调：

1. 启动 PostgreSQL
2. 启动后端 `backend`
3. 确认后端接口可访问
4. 启动 Android App 验证前台提交是否能入库
5. 启动自动化 `AD-Automation`
6. 观察自动化是否能顺序消费工单并更新状态

## 本地联调建议

### Android + Backend

如果 Android 模拟器访问本机后端：

- Android 端使用 `http://10.0.2.2:8080/`
- 后端监听 `0.0.0.0:8080`

### Automation + Database

如果自动化直连数据库：

- 推荐 `TICKET_SOURCE=db`
- 推荐在 `AD-Automation/.env` 中明确填写数据库连接信息

### Automation + Backend API

如果自动化先走后端接口读取工单：

- 使用 `TICKET_SOURCE=api`
- 确认 `BACKEND_URL` 正确

## 第一次搭环境

这一节按“第一次接手项目”的顺序来写，尽量做到不看代码也能先跑起来。

### 第 1 步：准备基础环境

建议先安装这些：

- Git
- JDK 17
- Android Studio
- Python 3.11 或更新版本
- PostgreSQL 15 或更新版本
- Chrome 浏览器

建议检查版本：

```powershell
java -version
python --version
git --version
```

如果是第一次在 Windows 上跑 PowerShell 脚本，可能还需要：

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

### 第 2 步：拉代码并确认目录

```powershell
cd E:\
git clone <your-repo-url> AD-project
cd E:\MrWang\Desktop\AD-project
```

如果代码已经在本地，只需要确认这 3 个目录都在：

- `E:\MrWang\Desktop\AD-project\android-app`
- `E:\MrWang\Desktop\AD-project\backend`
- `E:\MrWang\Desktop\AD-project\AD-Automation`

### 第 3 步：创建 PostgreSQL 数据库

先在 PostgreSQL 中创建数据库：

```sql
CREATE DATABASE campus_network_db;
```

建议同时准备一个专用账号，而不是一直直接使用默认超级用户。

示例：

```sql
CREATE USER ad_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE campus_network_db TO ad_user;
```

说明：

- 当前后端配置里开启了 `spring.jpa.hibernate.ddl-auto=update`
- 也就是说后端第一次启动时会自动尝试建表和更新表结构

### 第 4 步：启动后端

先准备：

`E:\MrWang\Desktop\AD-project\backend\.env.local`

内容可以参考：

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/campus_network_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_password
```

然后启动：

```powershell
cd E:\MrWang\Desktop\AD-project\backend
.\start-backend.ps1
```

如果不用脚本，也可以直接：

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/campus_network_db"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="your_password"
.\mvnw.cmd spring-boot:run
```

启动后先测一下接口：

- [http://localhost:8080/api/tickets/pending](http://localhost:8080/api/tickets/pending)

如果能返回 `[]` 或 JSON 数组，说明后端和数据库至少已经通了。

### 第 5 步：启动 Android 客户端

打开：

`E:\MrWang\Desktop\AD-project\android-app`

确认：

`E:\MrWang\Desktop\AD-project\android-app\app\build.gradle.kts`

里的 `AUTH_BASE_URL` 是你当前联调用的地址。

常见情况：

- Android 模拟器访问本机后端：
  `http://10.0.2.2:8080/`
- 真机访问同一局域网电脑：
  `http://你的电脑局域网IP:8080/`
- 访问线上后端：
  `https://你的域名/`

然后在 Android Studio 中直接运行 `app`。

建议先验证 2 件事：

1. App 能正常打开主页
2. 新用户绑定提交后，数据库 `tickets` 表中能看到记录

### 第 6 步：准备自动化环境

进入：

```powershell
cd E:\MrWang\Desktop\AD-project\AD-Automation
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
playwright install
```

然后准备：

`E:\MrWang\Desktop\AD-project\AD-Automation\.env`

如果你当前是让自动化直接读数据库，推荐这样配：

```env
RUN_MODE=portal
TICKET_SOURCE=db
DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_NAME=campus_network_db
DATABASE_USER=postgres
DATABASE_PASSWORD=your_password
PORTAL_USERNAME=your_username
PORTAL_PASSWORD=your_password
HEADLESS=false
BROWSER_CHANNEL=chrome
```

如果你想先只测试工单读取：

```env
RUN_MODE=tickets
TICKET_SOURCE=db
```

然后运行：

```powershell
.\.venv\Scripts\python.exe .\main.py
```

### 第 7 步：准备自动化模板图

自动化成功率很依赖 `assets` 目录中的模板图。

重点检查这些文件是否存在、是否与当前真实页面样式一致：

- `account_input.png`
- `dx_candidate.png`
- `captcha_input.png`
- `remote_user_avatar.png`
- `billing_system_entry.png`
- `business_management_menu.png`
- `edit_profile_menu.png`
- `student_id_input.png`
- `query_button.png`
- `broadband_account_input.png`
- `broadband_password_input.png`
- `confirm_button.png`

如果页面样式、缩放、主题变化了，优先更新这里的截图模板。

### 第 8 步：跑一条完整链路

推荐这样验收：

1. 后端启动
2. Android 提交一条工单
3. 去数据库确认该工单 `status` 为 `0` 或 `1`
4. 启动自动化
5. 完成验证码流程
6. 确认自动化进入“修改资料”页面
7. 确认自动化处理该工单
8. 回数据库确认该工单 `status` 已变成 `3`

如果这条链能走通，说明整个项目已经具备基本可用性。

## 部署建议

这一节偏“长期运行”，适合准备把项目放到一台固定机器上持续使用。

### 方案 1：最简单的本地常驻方案

适合单人维护或校园内固定电脑长期运行：

- PostgreSQL：装在本机
- 后端：本机启动
- 自动化：本机启动
- Android：连接本机后端或域名

优点：

- 搭建简单
- 排查问题直接
- 自动化最容易控制浏览器和屏幕环境

缺点：

- 依赖这台电脑一直开机
- 自动化受桌面环境影响比较大

### 方案 2：后端上云，自动化留本地

这是更推荐的结构：

- PostgreSQL：本地或云数据库
- Spring Boot 后端：部署到云服务器
- Android：直接请求线上域名
- 自动化：仍然跑在本地 Windows 电脑

优点：

- App 不依赖你本地电脑 IP
- 接口更稳定
- 自动化仍然保留本地图像识别能力

缺点：

- 需要维护线上环境变量和域名
- 自动化和数据库/后端的连通性要额外确认

### 方案 3：自动化专机

如果后续工单量变大，建议给自动化单独准备一台 Windows 电脑：

- 专门用于运行 Chrome、Playwright、PyAutoGUI
- 固定分辨率
- 固定缩放比例
- 固定浏览器版本
- 固定模板图

这样能显著减少“图片匹配突然失效”的概率。

## 后端部署参考

如果你准备把后端部署到服务器，建议至少做到下面这些：

### 1. 准备环境

- Linux 或 Windows Server
- JDK 17
- PostgreSQL
- 反向代理，常见是 Nginx

### 2. 配置环境变量

至少准备：

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/campus_network_db
SPRING_DATASOURCE_USERNAME=<db_user>
SPRING_DATASOURCE_PASSWORD=<db_password>
```

### 3. 打包

```powershell
cd E:\MrWang\Desktop\AD-project\backend
.\mvnw.cmd clean package
```

产物一般会在：

`E:\MrWang\Desktop\AD-project\backend\target`

### 4. 启动 Jar

示例：

```powershell
java -jar .\target\ad-backend-0.0.1-SNAPSHOT.jar
```

如果是在 Linux 上，通常会用：

```bash
nohup java -jar ad-backend-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

### 5. 建议补的生产项

- HTTPS
- 域名
- 日志轮转
- 数据库定时备份
- 监控和告警
- 防火墙白名单

## 自动化部署参考

自动化和普通后端不一样，它非常依赖桌面环境，所以部署思路也不同。

### 推荐环境

- Windows
- 固定分辨率
- 固定显示缩放
- 固定 Chrome 版本
- 固定账号环境

### 推荐做法

1. 使用单独 Windows 用户跑自动化
2. 不在运行时频繁改系统缩放
3. 保持 Chrome 页面样式稳定
4. 每次页面 UI 有变化时，及时更新 `assets` 模板图
5. 定期清理 `output` 和无效游标

### 自动化常驻运行建议

如果自动化要长期跑循环：

- 电脑不要休眠
- 浏览器窗口不要被其他程序遮挡太多
- 不要临时切换 DPI 或多显示器布局
- 最好关闭无关弹窗和系统通知

### 自动化故障恢复建议

建议准备一套最基本的恢复手册：

1. 自动化退出后重新激活虚拟环境
2. 检查数据库连接
3. 检查远程系统页面是否改版
4. 检查模板图是否过期
5. 必要时删除：
   - `output\ticket_cursor.json`
   - 旧验证码截图

## 新人接手建议

如果后面要把项目交给别人，建议让接手人先完成下面这几个动作：

1. 本地启动后端
2. 跑通 Android 模拟器
3. 在数据库里手动插入一条测试工单
4. 跑通自动化从读取工单到更新状态的整条流程
5. 学会更新 `assets` 模板图

这样接手人就不只是“能看代码”，而是真的能维护整套系统。

## 常见问题

### 1. Android 无法请求本地后端

检查：

- `AUTH_BASE_URL` 是否是 `10.0.2.2:8080`
- 后端是否已启动
- Windows 防火墙是否放行

### 2. 后端启动失败

检查：

- 是否使用 JDK 17
- PostgreSQL 是否已启动
- 数据库用户名密码是否正确
- `SPRING_DATASOURCE_*` 环境变量是否配置正确

### 3. 自动化提示数据库配置错误

检查：

- 是否配置了 `DATABASE_URL`
- 如果没配 `DATABASE_URL`，是否配置了：
  - `DATABASE_HOST`
  - `DATABASE_PORT`
  - `DATABASE_NAME`
  - `DATABASE_USER`
  - `DATABASE_PASSWORD`

### 4. 自动化图片匹配不到

检查：

- `assets` 模板图是否和当前页面样式一致
- 页面缩放比例是否变化
- Chrome 窗口尺寸是否变化太大
- 是否真的停留在正确页面

### 5. 自动化一直没有工单

检查：

- 数据库中是否存在 `status in (0, 1)` 的记录
- 这些工单的 `created_at` 是否晚于本地游标
- `output/ticket_cursor.json` 是否需要清理后重新消费

## 后续可继续补强的方向

- 给后端补正式的工单状态流转接口，而不是只靠自动化直改数据库
- 给自动化补更稳定的模板管理和页面状态机
- 给 Android 端补工单详情、工单进度和登录态管理
- 给整个项目补统一的部署文档

## 提交规范提醒

不要提交这些本地文件或生成物：

- `build/`
- `target/`
- `.gradle/`
- `.idea/`
- `local.properties`
- `.venv/`
- 浏览器本地 profile
- 真实账号密码
- 真实数据库密码

如果这个项目后面要交给别人接手，建议优先同步：

- 数据库表结构
- `.env` 模板
- 自动化模板图使用规范
- 工单状态定义
