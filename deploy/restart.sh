#!/usr/bin/env bash
# BTR 赛事平台 · 构建并启动 / 重启全部服务
set -euo pipefail

cd "$(dirname "$0")"

if [ ! -f .env ]; then
  cp .env.example .env
  echo "[deploy] 已生成 deploy/.env，请先修改其中的密码与 JWT_SECRET 后再执行本脚本。" >&2
  exit 1
fi

echo "[deploy] 校验 compose 配置 …"
docker compose config >/dev/null

echo "[deploy] 构建后端镜像 …"
docker compose build app

echo "[deploy] 启动服务 …"
docker compose up -d --remove-orphans

echo "[deploy] 等待应用健康检查 …"
for i in $(seq 1 30); do
  status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' btr-app 2>/dev/null || echo starting)"
  if [ "$status" = "healthy" ] || [ "$status" = "running" ]; then
    break
  fi
  sleep 2
done

docker compose ps
echo
echo "[deploy] 最近日志："
docker compose logs --tail=30 app
echo
echo "[deploy] 访问地址：http://localhost:${HTTP_PORT:-80}"
