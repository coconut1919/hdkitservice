# hdkitservice

封装华为云 **DevStation（云开发环境/沙箱）** 开放 API 的微服务，向上层调用方提供「压缩」接口，隐藏创建、开机、取连接等多步编排细节。

## 技术栈

- JDK 21 + Spring Boot 3.3 + Maven
- Spring Web（RestClient）
- 华为云 SDK-HMAC-SHA256 签名（自实现，已用真实账号验证）

## 架构

```
调用方(AI Agent / 客户端)
      │  请求头 X-HW-AK / X-HW-SK（调用方自己的华为云 AK/SK）
      ▼
hdkitservice (Spring Boot)
      │  SandboxController ──► SandboxService（编排 + 轮询状态）
      │  DevStationClient（HTTP + SDK-HMAC-SHA256 签名）
      ▼
DevStation 开放 API ──► 沙箱（云开发环境）
```

## 对外接口

统一路径前缀 `/rest/developer/server/hdkitservice/`，仅 `GET`、`POST`。调用方通过请求头 `X-HW-AK` / `X-HW-SK` 传入华为云 AK/SK（沙箱归属调用方账号，AK/SK 不落日志、不做任何本地持久化）。

### 1. 连接沙箱（压缩）`POST /connect`

**一用户一实例**：按调用方 AK 识别，先复用已有沙箱（开机 + 注入临时 AK/SK），没有才新建，返回新 wss 连接地址。`session_id` 与 `dev_stage_id` 等价（历史兼容字段）。

```bash
curl -X POST http://<host>:<port>/rest/developer/server/hdkitservice/connect \
  -H 'Content-Type: application/json' \
  -H 'X-HW-AK: <AK>' -H 'X-HW-SK: <SK>' \
  -d '{"source":"WEB","env":{},"git":{}}'
```

响应：

```json
{
  "session_id": "9a2263d6ef534879af7b51f166ff24b7",
  "dev_stage_id": "9a2263d6ef534879af7b51f166ff24b7",
  "connection_id": "373117",
  "connection_address": "wss://...?connect_code=...&ws_type=1&source=-2074327356",
  "status": "connected"
}
```

其中 `session_id` 的值等于 `dev_stage_id`。

入参说明：`template_id`/`flavor_id`/`source`/`env`/`git` 均仅新建时生效（可选，默认取环境变量）；沙箱 `name` 由服务端自动生成。

### 2. 配置临时 AK/SK `POST /credentials`

调用 DevStation `auto-config` 为沙箱注入临时 AK/SK（需环境处于 RUNNING）。临时 AK/SK 注入沙箱内部，接口只返回过期时间。

```bash
curl -X POST http://<host>:<port>/rest/developer/server/hdkitservice/credentials \
  -H 'Content-Type: application/json' \
  -H 'X-HW-AK: <AK>' -H 'X-HW-SK: <SK>' \
  -d '{"dev_stage_id":"<dev_stage_id>","enable_sts":true}'
```

响应：

```json
{ "session_id": "9a2263d6ef534879af7b51f166ff24b7", "expires_at": "2026-08-14T04:39:54Z" }
```

`credentials` 的入参 `session_id` 是 `dev_stage_id` 的别名，二者等价。

### 错误响应

```json
{ "code": "HDKIT_NOT_AGREEMENT", "message": "用户未签署最新版协议，签署需由用户本人确认后完成", "traceId": "c1200227-..." }
```

> 错误响应固定字段为 `code` / `message` / `traceId`（注意 `traceId` 是驼峰）。

| HTTP | 业务码 | 说明 |
|------|--------|------|
| 400 | `HDKIT_INVALID_REQUEST` | 参数缺失/非法 |
| 403 | `HDKIT_NOT_REALNAME` | `check-user`：未完成实名认证，需在控制台完成 |
| 403 | `HDKIT_NOT_AGREEMENT` | `check-user` / `connect` 门禁：未签署最新版协议（`sign_status==2` 也算），签署需用户本人确认 |
| 403 | `HDKIT_NOT_REALNAME_AND_AGREEMENT` | `check-user`：实名与协议均缺失，需分别完成 |
| 404 | `HDKIT_SANDBOX_NOT_FOUND` | 环境不存在或已被删除 |
| 409 | `HDKIT_CONFLICT` | 并发冲突/已达沙箱上限 |
| 422 | `HDKIT_NOT_RUNNING` | 环境未处于 RUNNING，无法注入 AK/SK |
| 502 | `HDKIT_UPSTREAM_ERROR` / `HDKIT_CONNECT_FAILED` / `HDKIT_RELEASE_FAILED` | 上游/编排失败（`HDKIT_UPSTREAM_ERROR` 会附带真实上游原因） |
| 504 | `HDKIT_TIMEOUT` / `HDKIT_RELEASE_TIMEOUT` | 编排超时 |

> **协议门禁（connect 前置校验）**：上游 DevStation 对协议非最新版的账号会拒绝**所有**请求（`HD.83700031`）。因此 `connect` 在编排前同步校验协议状态，非最新版直接返回 `403 HDKIT_NOT_AGREEMENT`，避免被打成 500 内部错误。
>
> **check-user 判定**：实名与协议**并行查询**，按缺失情况返回对应 403（`HDKIT_NOT_REALNAME` / `HDKIT_NOT_AGREEMENT` / `HDKIT_NOT_REALNAME_AND_AGREEMENT`），全部通过才返回 200 `{"realnameVerified":true,"agreementSigned":true}`。

## 快速开始

### 构建与测试

```bash
# 需 JDK 21 + Maven
mvn test       # 50 个单元测试
mvn package    # 打包 target/hdkitservice-0.0.1.jar
```

### 运行

```bash
export TEMPLATE_ID=9891956fbad845f497e3c71c84910f5e   # 模板 uuid（GET /open-api-public/v1/templates）
export FLAVOR_ID=23ebdfe3b2d34cc4b34a1aaf03d27c52    # 规格 flavor_id（GET /open-api-public/v1/specs）
export PORT=3002

java -jar target/hdkitservice-0.0.1.jar
```

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DEVSTATION_ENDPOINT` | `https://devstation.myhuaweicloud.com` | 上游基址 |
| `DEVSTATION_SOURCE` | `CLI` | 操作 source 标识 |
| `TEMPLATE_ID` | 空 | 沙箱模板 ID |
| `FLAVOR_ID` | 空 | 沙箱规格 ID |
| `PORT` | `3001` | 服务端口 |
| `POLL_INTERVAL_MS` | `5000` | 状态轮询间隔 |
| `CONNECT_TIMEOUT` | `300000` | 连接编排超时 |
| `RELEASE_TIMEOUT` | `180000` | 释放编排超时 |
| `MAX_CONCURRENT` | `5` | 并发沙箱上限（按账号维度实时统计，含手动创建的环境） |

## 上游 DevStation API 映射

| hdkitservice 接口 | 内部下游调用 |
|-------------------|--------------|
| `/connect` | ⓪ `GET /open-api-public/v1/agreements`（协议门禁）① `GET /open-api-public/v2/devenvs`（找已有/并发计数）② `POST /open-api-public/v2/devenvs`（新建）③ 轮询至 `cde.0004` ④ `POST /open-api-public/v1/devenvs/{id}/start` ⑤ 轮询至 `cde.0002` ⑥ `POST /open-api-public/v1/auto-config`（注入临时 AK/SK）⑦ `POST /open-api-public/v1/devenvs/{id}/connections` ⑧ `GET /open-api-public/v1/devenvs/{id}/connections/{connId}` |
| `/credentials` | ① 轮询确认 `cde.0002` ② `POST /open-api-public/v1/auto-config` |

关键字段（已实测）：创建返回 `result.dev_stage_instance_id`；连接地址 `result.connection_info.url`；临时凭证过期 `result.sts_expires_at`。状态码 `cde.0002`=RUNNING、`cde.0004`=已就绪/关机。**删除前必须先关机完成**，否则报 `HD.98320063`。

## 项目结构

```
src/main/java/com/huaweicloud/hdkitservice/
├── controller/        # REST 接口 + 全局异常处理
├── service/           # 编排（SandboxService）、上游客户端（DevStationClient）
├── sign/              # SDK-HMAC-SHA256 签名
├── config/            # 环境变量配置
└── model/             # DTO
```

## License

[Apache License 2.0](LICENSE)
