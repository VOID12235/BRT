#!/usr/bin/env bash
# BTR 赛事平台 · 数据库与上传文件备份
# 用法：./backup.sh [保留天数，默认 14]
set -euo pipefail

cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "[backup] 缺少 deploy/.env，请先复制 .env.example 并填写。" >&2
  exit 1
fi

# shellcheck disable=SC1091
set -a
. ./.env
set +a

KEEP_DAYS="${1:-14}"
STAMP="$(date +%Y%m%d-%H%M%S)"
DIR="backups"
mkdir -p "$DIR"

echo "[backup] 导出 MySQL 数据库 ${DB_NAME:-btr} …"
docker compose exec -T \
  -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" \
  mysql mysqldump \
  -u root \
  --single-transaction --quick --routines --triggers --default-character-set=utf8mb4 \
  "${DB_NAME:-btr}" | gzip -9 > "${DIR}/mysql-${STAMP}.sql.gz"

echo "[backup] 打包上传文件 …"
docker compose exec -T app tar czf - -C /app/uploads . > "${DIR}/uploads-${STAMP}.tar.gz"

echo "[backup] 清理 ${KEEP_DAYS} 天前的备份 …"
find "$DIR" -type f -name 'mysql-*.sql.gz' -mtime "+${KEEP_DAYS}" -delete
find "$DIR" -type f -name 'uploads-*.tar.gz' -mtime "+${KEEP_DAYS}" -delete

echo "[backup] 完成："
ls -lh "${DIR}" | tail -n +2
