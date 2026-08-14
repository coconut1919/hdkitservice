# hdkitservice 生产集群部署指南

目标集群：`cce-hd-devkit-prod`（私有集群，从集群内网环境执行）

## 发布流程（CI/CD 自动）

生产发布由 **tag 触发**，合入 main 不再自动发版：

1. 将 `dev` 合并到 `main`（仅触发 CI 与安全扫描，不部署）
2. 确认可发布后打 tag 并推送：

   ```bash
   git checkout main && git pull
   git tag v1.2.0 && git push origin v1.2.0
   ```

3. tag `v*` 触发 `cd-production.yml`：构建镜像 `hdkitservice:v1.2.0`、推送 SWR、滚动更新 CCE

镜像 tag 与 git tag 一致（如 `v1.2.0`），可追溯、可回滚（`kubectl rollout undo`）。
如需额外人工审批，可在 GitHub 仓库的 `production` Environment 中配置 required reviewers。

## 前置条件

- 已登录 SWR（`hcloud SWR CreateSecret --cli-region=cn-south-1` 可获取临时登录凭证）
- 具备 prod 集群 kubectl 访问权限（CCE 控制台下载 kubeconfig）
- 镜像已推送：`swr.cn-south-1.myhuaweicloud.com/huaweicloud-devkit-prod/hdkitservice:20260813211510717`

## 步骤 1：更新镜像拉取凭证

SWR 临时凭证约 24h 过期，部署前务必刷新：

```bash
AUTH=$(hcloud SWR CreateSecret --cli-region=cn-south-1 \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["auths"]["swr.cn-south-1.myhuaweicloud.com"]["auth"])')
U=$(echo $AUTH | base64 -d | cut -d: -f1)
P=$(echo $AUTH | base64 -d | cut -d: -f2)
kubectl create secret docker-registry swr-secret -n backend \
  --docker-server=swr.cn-south-1.myhuaweicloud.com \
  --docker-username="$U" --docker-password="$P" \
  --dry-run=client -o yaml | kubectl apply -f -
```

## 步骤 2：创建/更新应用配置 Secret

将 `<值>` 替换为 prod 环境实际值：

```bash
kubectl create secret generic app-secrets -n backend \
  --from-literal=MYSQL_HOST=<prod MySQL 地址> \
  --from-literal=MYSQL_PORT=3306 \
  --from-literal=MYSQL_USER=<prod MySQL 用户> \
  --from-literal=MYSQL_PASSWORD=<prod MySQL 密码> \
  --from-literal=MYSQL_DATABASE=<prod 库名> \
  --from-literal=TEMPLATE_ID=<DevStation 模板 ID> \
  --from-literal=FLAVOR_ID=<DevStation 规格 ID> \
  --dry-run=client -o yaml | kubectl apply -f -
```

## 步骤 3：部署应用

```bash
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

## 步骤 4：验证

```bash
kubectl get pods -n backend -l app=hdkitservice
kubectl logs -n backend -l app=hdkitservice --tail=50
```

预期日志关键行：

```
HikariPool-1 - Start completed.          # MySQL 连接成功
Tomcat started on port 3001               # HTTP 服务就绪
Started HdkitServiceApplication           # 应用启动完成
```

接口验证（需真实 AK/SK）：

```bash
kubectl port-forward -n backend svc/hdkitservice-svc 3001:3001 &
curl -H "X-HW-AK: <AK>" -H "X-HW-SK: <SK>" \
  http://127.0.0.1:3001/rest/developer/server/hdkitservice/check-user
```

## 故障排查

| 现象 | 处理 |
|------|------|
| ErrImagePull / 401 | swr-secret 过期，重跑步骤 1 |
| Pending (Insufficient cpu) | 清理遗留 Pod 释放资源 |
| MySQL 连接失败 | 确认 MYSQL_* 值正确、RDS 安全组放行 3306 |
