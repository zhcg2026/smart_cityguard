# 本地调试怎么启动（按顺序做）

只做本机联调，不讲部署。

---

## 0. 你本机要先有的东西

- **MySQL**：库名 **`cityguard`**，已执行过 **`database/init.sql`**（只做一次）。
- **JDK 17**、**Maven**、**Node.js**（带 npm）。

数据库账号密码要和 **`backend/smart-cityguard-server/src/main/resources/application.yml`** 里 `spring.datasource` 一致；不对就改 yml，别猜。

---

## 1. 启动后端（必须先开）

在 PowerShell 里：

```powershell
Set-Location d:\smart_cityguard\backend
mvn spring-boot:run -pl smart-cityguard-server -DskipTests
```

看到日志里有 **Started CityguardApplication**、端口 **8080** 就 OK。

**若你改过 `smart-cityguard-geo` 模块代码**，先装一次再启动（否则片区等接口可能 404）：

```powershell
Set-Location d:\smart_cityguard\backend
mvn clean install -pl smart-cityguard-geo -am -DskipTests
mvn spring-boot:run -pl smart-cityguard-server -DskipTests
```

接口文档：http://localhost:8080/doc.html  

---

## 2. 启动管理端（需要后台管理时）

另开一个 PowerShell：

```powershell
Set-Location d:\smart_cityguard\frontend\admin-web
npm install
npm run dev
```

浏览器打开：**http://localhost:3000**

---

## 3. 启动采集端 H5（需要采集员页面时）

再开一个 PowerShell：

```powershell
Set-Location d:\smart_cityguard\frontend\collector-app
npm install
npm run dev
```

浏览器打开：**http://localhost:3003**

---

## 顺序小结

| 顺序 | 干什么 | 地址 |
|------|--------|------|
| ① | 后端 `mvn spring-boot:run ...` | http://localhost:8080 |
| ② | 管理端 `npm run dev` | http://localhost:3000 |
| ③ | 采集端 `npm run dev` | http://localhost:3003 |

前端会通过 **`/api` 代理到 8080**，所以 **① 一定要先起来**，否则前端会报代理/网络错误。

---

## 常见问题一行版

- **前端连不上接口**：先看 8080 后端有没有在跑。  
- **登录后页面乱跳转**：见 **`docs/continuation-notes.md`** 里关于路由守卫的那一节。  
- **Redis / RabbitMQ / MinIO**：本地缺了若导致启动失败，再按 `application.yml` 去装或改配置；默认里 Redis 常是关的，以你本机 yml 为准。

更细的说明需要时自己翻仓库里的 `README.md` 或问同事即可。
