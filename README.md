# 校园跑腿任务发布与接单系统

一个完整的校园跑腿任务平台，覆盖 **任务发布 → 接单 → 配送 → 结算 → 评价** 全流程。
学生发布跑腿任务并支付报酬，跑腿员接单完成配送，任务完成后自动结算，双方可互相评价。

## 一、技术栈

| 端 | 技术 |
| --- | --- |
| 后端 | Java 8 · Spring Boot 2.7.18 · MyBatis-Plus 3.5.7 · MySQL 8.0 · JWT · BCrypt · springdoc 接口文档 |
| 前端 | Vue 3 · Vite 5 · Element Plus · Pinia · Vue Router · Axios |
| 工具 | Maven 3.9 · IntelliJ IDEA 2026.1.3 |

## 二、目录结构

```
E:\IdeaProjects\campus-errand\
├── backend\                      # 后端工程（IDEA 打开此目录）
│   ├── pom.xml
│   ├── sql\campus_errand.sql     # 建库建表脚本
│   └── src\main\
│       ├── java\com\campus\errand\
│       │   ├── common\           # 统一返回、分页、全局异常
│       │   ├── config\           # MyBatis-Plus、CORS、Jackson、接口文档、数据初始化
│       │   ├── controller\       # auth/task/order/settlement/evaluation/admin 接口
│       │   ├── dto\              # 入参对象
│       │   ├── entity\           # 实体（user/task/errand_order/settlement/withdrawal/evaluation）
│       │   ├── mapper\           # MyBatis-Plus Mapper
│       │   ├── security\         # JWT 工具、登录/管理员拦截器
│       │   ├── service\          # 业务接口 + 实现
│       │   └── vo\               # 出参对象
│       └── resources\application.yml
└── frontend\                     # 前端工程（Vue3 + Vite）
    ├── package.json
    ├── vite.config.js            # 开发代理 /api → localhost:8080
    └── src\
        ├── api\                  # 接口封装
        ├── router\               # 路由与登录守卫
        ├── store\                # Pinia 用户状态
        ├── utils\status.js       # 状态/分类字典、进度节点、倒计时与加急判断
        └── views\                # 页面（登录/工作台/任务广场/发布/详情/我的发布/我的接单/钱包/管理后台）
```

## 二·一、跑腿员工作台（本次新增）

接单之后「活在哪、到哪一步了」是这个平台最关键的信息，因此新增了以跑腿员视角组织的一整套界面：

- **工作台首页**（`/workbench`）：登录后的默认落地页，一屏给出今日接单数、手上待处理单量、累计收入、信用评分，以及「我进行中的接单」进度列表和广场推荐任务；
- **接单后直达**：任务广场/任务详情接单成功后，会直接提示「查看我的接单」，一键跳到手上的单，不用再自己找入口；
- **菜单角标**：顶部「我的接单」实时显示进行中数量，每次切换页面自动刷新；
- **我的接单页**（`/my/accepted`）：按 进行中 / 已完成 / 已取消 / 全部 分组，每单带状态标签、进度条、截止倒计时、发布者与接单时间，可直接开始配送、标记送达、取消接单；
- **任务广场**（`/tasks`）：支持按最新发布 / 报酬最高 / 即将截止排序，24 小时内截止的任务打「加急」标，分类带图标，并提供「只看待接单」等快捷筛选；
- **任务详情**（`/task/{id}`）：顶部五段式状态时间线（发布→接单→配送→送达→完成），展示接单时间与双方评分。


## 三、数据库设计

6 张表：`user`（用户/钱包）、`task`（任务）、`errand_order`（接单记录）、`settlement`（结算记录）、`withdrawal`（提现申请）、`evaluation`（评价）。

### 任务状态机

```
PENDING(待接单) ─接单→ ACCEPTED(已接单) ─开始配送→ IN_PROGRESS(配送中)
    │                                    ─标记送达→ FINISHED(待确认) ─发布者确认→ COMPLETED(已完成)
    └─────────── 取消 ────────────────→ CANCELLED(已取消)
```

- 可取消方：发布者（PENDING/ACCEPTED/IN_PROGRESS）、跑腿员（ACCEPTED/IN_PROGRESS）；
- 取消即触发退款：发布时冻结的报酬金额原路退回发布者余额；
- 发布者确认完成：冻结金额转入跑腿员余额，生成结算记录，双方进入互评阶段。

### 资金规则（结算模块）

| 环节 | 资金操作 |
| --- | --- |
| 发布任务 | 余额扣减 `reward`，转入冻结余额 |
| 任务取消 | 冻结余额退回余额 |
| 确认完成 | 冻结余额清零，跑腿员余额增加 `reward`，生成 `settlement`(TASK) |
| 提现申请 | 扣减余额，生成 `withdrawal`(PENDING)，管理员审批通过或驳回（驳回退回） |

## 四、接口清单（统一前缀 /api，除登录注册外均需 Token）

| 模块 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 认证 | POST | /auth/register | 注册（STUDENT/RUNNER） |
| | POST | /auth/login | 登录，返回 JWT |
| | GET | /auth/info | 当前用户 |
| | POST | /auth/recharge | 模拟充值 |
| 任务 | POST | /tasks | 发布任务 |
| | GET | /tasks | 任务广场分页（keyword/category/status/sort，sort 支持 latest·reward·deadline） |
| | GET | /tasks/overview | 工作台统计（进行中/已完成/收入/评分/广场概览） |
| | GET | /tasks/{id} | 任务详情（含接单时间、双方评分） |
| | POST | /tasks/{id}/accept | 接单 |
| | POST | /tasks/{id}/start | 开始配送 |
| | POST | /tasks/{id}/finish | 标记送达 |
| | POST | /tasks/{id}/confirm | 发布者确认完成（结算） |
| | POST | /tasks/{id}/cancel | 取消任务 |
| | GET | /tasks/mine/published | 我发布的 |
| | GET | /tasks/mine/accepted | 我接的（支持 status=ACTIVE/COMPLETED/CANCELLED） |
| 接单 | GET | /orders | 我的接单记录（同样支持 status 筛选） |
| 结算 | GET | /settlements | 我的结算记录 |
| | GET | /settlements/wallet | 钱包信息（余额/收支统计） |
| | POST | /settlements/withdrawals | 申请提现 |
| | GET | /settlements/withdrawals | 我的提现记录 |
| 评价 | POST | /evaluations | 提交评价 |
| | GET | /evaluations/task/{taskId} | 任务评价列表 |
| 管理 | GET | /admin/users · /admin/tasks · /admin/settlements · /admin/withdrawals | 后台列表 |
| | POST | /admin/withdrawals/{id}/approve · /reject | 提现审批 |

接口文档（Swagger）：启动后端后访问 `http://localhost:8080/api/swagger-ui.html`

## 五、快速启动

### 1. 数据库

```sql
-- 已内置脚本，或手动执行：
mysql -uroot -p715620504 < E:\IdeaProjects\campus-errand\backend\sql\campus_errand.sql
```

数据库连接配置在 `backend/src/main/resources/application.yml`（用户名 root，密码默认 `715620504`，
可通过环境变量 `MYSQL_PASSWORD` 覆盖）。

### 2. 后端（IDEA）

1. 用 IntelliJ IDEA 打开 `E:\IdeaProjects\campus-errand\backend`；
2. 等待 Maven 依赖导入完成；
3. 运行启动类 `com.campus.errand.ErrandApplication`，端口 8080。

> 首次启动会自动写入测试账号与示例任务（见 DataInitializer）。

### 3. 前端

```bash
cd E:\IdeaProjects\campus-errand\frontend
npm install
npm run dev
```

浏览器访问 `http://localhost:5173`，开发代理已配置 `/api → http://localhost:8080`。

## 六、测试账号

| 账号 | 密码 | 角色 | 用途 |
| --- | --- | --- | --- |
| admin | 123456 | 管理员 | 管理后台、提现审批 |
| stu001 | 123456 | 学生 | 发布任务 |
| runner001 | 123456 | 跑腿员 | 接单赚钱 |

演示链路：`runner001 登录 → 工作台看到手上的单 → 任务广场接单（角标 +1）→ 开始配送 → 标记送达 → stu001 确认完成（结算到账）→ 双方评价`。

## 七、主要依赖版本

Spring Boot 2.7.18 · MyBatis-Plus 3.5.7 · mysql-connector-java 8.0.33 · jjwt 0.9.1 · springdoc-openapi 1.7.0 · Vue 3.4 · Element Plus 2.7 · Vite 5.3
