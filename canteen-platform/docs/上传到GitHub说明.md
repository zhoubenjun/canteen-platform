# 上传到 GitHub 说明（仓库精简与忽略规则）

## 一、结论先说

本项目源码体积很小（约 **10 MB**），占空间的是"构建产物 + 本地工具链 + 数据库文件"。已在项目根目录添加 `.gitignore`，按规则纳入版本控制的文件合计约 **9.4 MB**，可以直接上传 GitHub。

## 二、本次清理（2026-11-24）

| 位置 | 清理前 | 清理后 | 释放 | 清理内容 |
|---|---|---|---|---|
| `backend/target` | 500.5 MB | 51.1 MB | ≈ 449 MB | 9 个历史 jar 备份（`canteen-platform.jar.bak*`，约 380 MB）、`tmp-jar-extract`、`stats-build`、`stage-static`、`stage-class`、`checkjar*`、`tmp-classes`、早期 `uploads` 副本 |
| `tools` | 1870.7 MB | 1457.2 MB | ≈ 414 MB | `jdk17.zip`（182 MB，已解压的 JDK 仍可用）、`mysql-download/mysql-8.0.37-winx64.zip`（231.5 MB 安装包）、调试用 `*.json / *.log / *.html / *.class / *.java` 临时文件 |
| `frontend` | — | — | — | 删除 `build-log.txt` 构建日志 |
| 合计 | 约 2594 MB | 1731.5 MB | **≈ 863 MB** | — |

**保留未动（运行必需）**：

- `backend/target/canteen-platform.jar`（正在运行的包，PID 见启动日志）
- `tools/mysql-8.0.37-winx64`（MySQL 服务端，`mysqld` 正在运行）
- `tools/mysql-data`（**数据库全部数据文件，禁止删除或搬动**）
- `backend/uploads`（菜品图片等上传文件，324 个 / 32.1 MB）
- `tools/jdk17`（JDK 17，编译与启动都要用）

## 三、代码层检查结论（为什么不需要"删代码"）

- 前端 `frontend/src/views` 共 19 个页面，`router/index.js` 中全部有对应路由，无未引用页面。
- 后端 76 个 Java 类均为 Spring 组件（`@RestController` / `@Service` / `@Configuration` 等）、实体与工具类，按注解装配，不存在"未被引用"的独立死代码；18 个 `mapper` 接口与 `resources/mapper` 下 18 个 XML 一一对应，无孤立文件。
- 因此"占用太大"的原因在构建产物与工具链，而非业务代码；本次按"只清产物、不动源码"处理。

## 四、可以忽略、不必上传的文件（`.gitignore` 已覆盖）

| 忽略项 | 原因 |
|---|---|
| `backend/target/` | Maven 构建产物，重新打包即可生成（含 42 MB jar） |
| `backend/src/main/resources/static/` | 由 `npm run build` 生成（`vue.config.js` 指定输出到此），源码在 `frontend/src` |
| `frontend/node_modules/` | 187 MB 依赖，`npm install` 可复原 |
| `frontend/dist/`、`frontend/build-log.txt` | 构建产物与日志 |
| `backend/uploads/` | 运行期上传的图片/文件，属于数据不属于代码 |
| `tools/` | JDK、MySQL 程序与数据库数据（1.46 GB），本机环境，不能入库 |
| `*.log` | 运行日志（`target/start_*.log` 等） |
| `*.zip /*.jar /*.war` | 安装包与打包产物 |
| `*.sql.bak`、`*_bak_*` | 数据库备份（本次建过 `weekly_recipe_bak_*` 备份表，导出文件不应入库） |
| `.idea/`、`*.iml`、`.vscode/` | IDE 配置 |
| `*.tmp / *.bak / *.old`、`Thumbs.db` | 临时与系统文件 |

**需要上传**：`backend/src`、`frontend/src`、`frontend/public`、`frontend/package.json`、`frontend/package-lock.json`、`frontend/vue.config.js`、`docs/`、`sql/init.sql`、`README.md`、`.gitignore`、三个 `.bat` 与 `kill-backend.ps1`。

> 注意：`package-lock.json` 一定要上传，保证他人 `npm install` 装到同一套依赖。

## 五、上传前的两个提醒

1. **数据库口令**：`backend/src/main/resources/application.yml` 里默认值为 `root / root`（`${MYSQL_USER:root}`、`${MYSQL_PASSWORD:root}`）。上传公开仓库前，建议把默认值改成占位（如 `change_me`），或确认仓库为私有。
2. **克隆者如何跑起来**：`tools/` 不入库，因此克隆后需要自备 MySQL 8 与 JDK 17；`README.md` 第 11 行"MySQL 已随附便携版于 tools/"对克隆者不再适用，建议改为"需自行安装 MySQL 8，初始化脚本见 `sql/init.sql`"。

## 六、首次上传步骤（本机当前未安装 Git）

本机 `git` 命令不可用（PATH 与常见安装路径均未找到）。任选一种：

- 安装 **Git for Windows**（https://git-scm.com/download/win）或 **GitHub Desktop**（图形化，适合不想敲命令）；
- 装好后在项目根目录执行：

```bash
git init
git add .
git status          # 确认列表里没有 tools/、node_modules/、target/、uploads/
git commit -m "食堂管理数据平台：前后端源码（不含构建产物与本机工具）"
git branch -M main
git remote add origin https://github.com/<你的账号>/<仓库名>.git
git push -u origin main
```

推送前务必先看 `git status` 的输出：若出现 `tools/`、`node_modules/`、`backend/target`、`backend/uploads` 中任何一项，说明 `.gitignore` 未生效，先排查再推送。

## 七、日常维护

- 每次改完代码重新打包后，`backend/target` 会再生成新 jar；旧的 `*.bak-*` 备份建议只留最近 1 个，其余及时删除（本次已全部清理，随时间会再次积累）。
- 数据库备份请导出为 `sql` 文件单独保存（不要放进仓库），例如 `mysqldump -uroot -proot canteen > backup_日期.sql`。
