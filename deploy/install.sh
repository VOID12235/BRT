#!/usr/bin/env bash
# ==========================================================================
# BTR 赛事平台 · Linux 一键安装脚本（满血版）
#
# 支持发行版：Ubuntu 20.04+ / Debian 11+ / Rocky Linux 9 / AlmaLinux 9 /
#             CentOS Stream 9 / Fedora 40+
#
# 用法：
#   sudo bash deploy/install.sh                          # 交互式（含邮件配置询问）
#   sudo bash deploy/install.sh --no-mail               # 跳过邮件配置
#   sudo bash deploy/install.sh \
#       --mail-host=smtp.qq.com --mail-port=465 \
#       --mail-user=xxx@qq.com --mail-pass=授权码        # 非交互预填邮件
#   sudo bash deploy/install.sh --admin-email=you@xx.com
#
# 流程：
#   1. 检测发行版与架构
#   2. 安装 Docker 引擎 + Compose 插件（官方脚本优先，apt/dnf 兜底）
#   3. 定位项目根（或提示从 GitHub 克隆）
#   4. 生成 deploy/.env（自动强随机密码 + 可选邮件/管理员邮箱）
#   5. 构建并启动 mysql/redis/app/nginx 四服务
#   6. 轮询应用健康状态
#   7. 防火墙放行对外端口；输出访问信息与运维命令
# ==========================================================================
set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info() { echo -e "${GREEN}[install]${NC} $*"; }
warn() { echo -e "${YELLOW}[install]${NC} $*"; }
die()  { echo -e "${RED}[install] ERROR${NC} $*" >&2; exit 1; }

# ---------- 参数解析 ----------
MAIL_HOST=""; MAIL_PORT=""; MAIL_USER=""; MAIL_PASS=""; ADMIN_EMAIL=""; SKIP_MAIL=0
for arg in "$@"; do
  case "$arg" in
    --mail-host=*)  MAIL_HOST="${arg#*=}" ;;
    --mail-port=*)  MAIL_PORT="${arg#*=}" ;;
    --mail-user=*)  MAIL_USER="${arg#*=}" ;;
    --mail-pass=*)  MAIL_PASS="${arg#*=}" ;;
    --admin-email=*) ADMIN_EMAIL="${arg#*=}" ;;
    --no-mail)      SKIP_MAIL=1 ;;
    *) warn "忽略未知参数: $arg" ;;
  esac
done

# sed 替换值转义（密码等可能含 & | / \ 特殊字符）
escape_sed() { printf '%s' "$1" | sed -e 's/[&|\\/]/\\&/g'; }

# ---------- 1. 前置检查 ----------
[ "$(id -u)" -eq 0 ] || die "请使用 root 运行：sudo bash deploy/install.sh"
command -v curl >/dev/null 2>&1 || die "缺少 curl，请先安装：apt/dnf install -y curl"
command -v openssl >/dev/null 2>&1 || die "缺少 openssl，请先安装。"

ARCH="$(uname -m)"
case "$ARCH" in x86_64|aarch64) ;; *) die "暂不支持架构: $ARCH" ;; esac
info "系统: $(uname -sr) / $ARCH"

# ---------- 2. 检测发行版 ----------
DISTRO="unknown"; PKG_MGR="apt"
if [ -f /etc/os-release ]; then
  . /etc/os-release
  DISTRO="${ID:-unknown}"
  case "$DISTRO" in
    ubuntu|debian) PKG_MGR="apt" ;;
    rocky|almalinux|centos|fedora|rhel) PKG_MGR="dnf" ;;
    *) warn "未识别的发行版($DISTRO)，默认按 apt 处理" ;;
  esac
fi
info "发行版: ${ID:-unknown} ${VERSION_ID:-}"

# ---------- 3. 安装 Docker（若未安装） ----------
install_docker() {
  if command -v docker >/dev/null 2>&1; then
    docker version --format '{{.Server.Version}}' >/dev/null 2>&1 || \
      die "docker 命令存在但守护进程不可用，请先启动/重启 docker 服务"
    info "Docker 已安装: $(docker version --format '{{.Server.Version}}')"
  else
    info "安装 Docker 引擎（官方脚本）..."
    curl -fsSL https://get.docker.com | sh || { warn "官方脚本失败，尝试发行版包管理器安装"; install_docker_pkg; }
  fi

  if docker compose version >/dev/null 2>&1; then
    info "Compose 插件已安装: $(docker compose version --short)"
  else
    info "安装 Docker Compose 插件..."
    case "$PKG_MGR" in
      apt) apt-get update -y && apt-get install -y docker-compose-plugin ;;
      dnf) dnf install -y docker-compose-plugin ;;
    esac || die "Compose 插件安装失败，请手动安装后重试"
    docker compose version >/dev/null 2>&1 || die "Compose 插件仍不可用"
  fi

  systemctl enable --now docker >/dev/null 2>&1 || systemctl start docker >/dev/null 2>&1 || \
    warn "未能自动启动 docker 服务，请手动执行：systemctl start docker"
  docker info >/dev/null 2>&1 || die "Docker 守护进程不可用"
}
install_docker_pkg() {
  case "$PKG_MGR" in
    apt)
      apt-get update -y
      apt-get install -y docker.io docker-compose-v2 || die "apt 安装 docker 失败"
      ;;
    dnf)
      dnf install -y dnf-plugins-core
      dnf config-manager --add-repo https://download.docker.com/linux/"$DISTRO"/docker-ce.repo 2>/dev/null || true
      dnf install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin \
        || dnf install -y docker docker-compose-plugin || die "dnf 安装 docker 失败"
      ;;
  esac
}
install_docker

# ---------- 4. 定位项目根 ----------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/../Dockerfile" ] && [ -f "$SCRIPT_DIR/docker-compose.yml" ]; then
  PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
  info "检测到项目根目录: $PROJECT_ROOT"
else
  warn "未检测到项目文件，正在从 GitHub 克隆..."
  PROJECT_ROOT="$(pwd)/BRT"
  git clone --depth 1 https://github.com/VOID12235/BRT.git "$PROJECT_ROOT" || die "克隆失败，请手动克隆后重试"
fi
cd "$PROJECT_ROOT/deploy"

# ---------- 5. 生成 .env（自动强随机密码 + 邮件配置） ----------
if [ -f .env ]; then
  info "已存在 deploy/.env，沿用现有配置（如需重置请删除该文件后重跑）"
else
  cp .env.example .env
  MYSQL_ROOT_PASSWORD="$(openssl rand -hex 12)"
  DB_PASSWORD="$(openssl rand -hex 12)"
  ADMIN_PASSWORD="$(openssl rand -hex 8)"
  JWT_SECRET="$(openssl rand -hex 32)"

  sed -i "s/^MYSQL_ROOT_PASSWORD=.*/MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}/" .env
  sed -i "s/^DB_PASSWORD=.*/DB_PASSWORD=${DB_PASSWORD}/" .env
  sed -i "s/^ADMIN_PASSWORD=.*/ADMIN_PASSWORD=${ADMIN_PASSWORD}/" .env
  sed -i "s/^JWT_SECRET=.*/JWT_SECRET=${JWT_SECRET}/" .env
  info "已生成 deploy/.env（数据库与管理员密码均为随机值，见下方输出）"
fi

# 管理员邮箱（可选）
if [ -n "$ADMIN_EMAIL" ]; then
  sed -i "s|^ADMIN_EMAIL=.*|ADMIN_EMAIL=$(escape_sed "$ADMIN_EMAIL")|" .env
  info "管理员邮箱已设置: $ADMIN_EMAIL"
fi

# 邮件服务配置（注册验证码/通知邮件）——参数优先，其次交互询问，--no-mail 跳过
configure_mail() {
  if [ "$SKIP_MAIL" = "1" ]; then
    warn "已跳过邮件配置（可在部署后编辑 deploy/.env 的 MAIL_*）"
    return
  fi
  if [ -z "$MAIL_HOST" ]; then
    echo ""
    read -r -p "  是否现在配置邮件服务（注册验证码/通知邮件需要）？[y/N]: " _ans
    case "$_ans" in
      y|Y|yes|YES)
        read -r -p "  SMTP 服务器（QQ 邮箱填 smtp.qq.com）: " MAIL_HOST
        read -r -p "  SMTP 端口（QQ 邮箱 465/SSL 或 587，回车默认 465）: " MAIL_PORT
        read -r -p "  邮箱账号（如 123@qq.com）: " MAIL_USER
        read -r -s -p "  密码/授权码（QQ 邮箱用 SMTP 授权码）: " MAIL_PASS; echo ""
        ;;
      *) return ;;
    esac
  fi
  [ -z "$MAIL_PORT" ] && MAIL_PORT=465
  if [ -n "$MAIL_HOST" ] && [ -n "$MAIL_USER" ] && [ -n "$MAIL_PASS" ]; then
    sed -i "s|^MAIL_HOST=.*|MAIL_HOST=$(escape_sed "$MAIL_HOST")|" .env
    sed -i "s|^MAIL_PORT=.*|MAIL_PORT=${MAIL_PORT}|" .env
    sed -i "s|^MAIL_USER=.*|MAIL_USER=$(escape_sed "$MAIL_USER")|" .env
    sed -i "s|^MAIL_PASSWORD=.*|MAIL_PASSWORD=$(escape_sed "$MAIL_PASS")|" .env
    info "邮件配置已写入 deploy/.env（重启后生效）"
  else
    warn "邮件配置不完整，已跳过（部署后可编辑 deploy/.env 的 MAIL_* 再执行 bash restart.sh）"
  fi
}
configure_mail

# ---------- 6. 构建并启动 ----------
set -a; . ./.env; set +a
[ -n "${HTTP_PORT:-}" ] || HTTP_PORT=80

info "校验 compose 配置..."
docker compose config >/dev/null || die "compose 配置校验失败"

info "构建后端镜像（首次约 5-10 分钟，请耐心等待）..."
docker compose build app || die "镜像构建失败"

info "启动全部服务..."
docker compose up -d --remove-orphans || die "服务启动失败"

# ---------- 7. 等待健康 ----------
info "等待应用健康检查（最长 120 秒）..."
for i in $(seq 1 60); do
  STATUS="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' btr-app 2>/dev/null || echo starting)"
  if [ "$STATUS" = "healthy" ]; then
    info "应用已就绪（约 $((i*2)) 秒）"
    break
  fi
  if [ "$STATUS" != "starting" ] && [ "$i" -gt 10 ]; then
    warn "btr-app 状态异常($STATUS)，请查看日志：docker compose logs -f app"
  fi
  sleep 2
done

docker compose ps

# ---------- 8. 防火墙放行对外端口 ----------
if command -v ufw >/dev/null 2>&1; then
  ufw allow "$HTTP_PORT"/tcp >/dev/null 2>&1 && info "ufw 已放行端口 $HTTP_PORT" || warn "ufw 放行失败，请手动执行：ufw allow $HTTP_PORT/tcp"
elif command -v firewall-cmd >/dev/null 2>&1; then
  firewall-cmd --permanent --add-port="$HTTP_PORT"/tcp >/dev/null 2>&1 \
    && firewall-cmd --reload >/dev/null 2>&1 \
    && info "firewalld 已放行端口 $HTTP_PORT" || warn "firewalld 放行失败，请手动放行 $HTTP_PORT"
fi

# ---------- 9. 输出结果 ----------
IP="$(hostname -I 2>/dev/null | awk '{print $1}')"
[ -n "$IP" ] || IP="服务器IP"
MAIL_STATE="未配置（注册验证码暂不可用）"
[ -n "${MAIL_HOST:-}" ] && MAIL_STATE="已配置（${MAIL_HOST}:${MAIL_PORT:-465}）"

echo ""
echo "=============================================================="
echo "  BTR 赛事平台部署完成"
echo "=============================================================="
echo ""
echo "  访问地址:   http://${IP}:${HTTP_PORT}/"
echo "  管理员账号: ${ADMIN_ACCOUNT:-admin}"
echo "  管理员密码: ${ADMIN_PASSWORD:-（未设置，请在 .env 配置后重启）}"
echo "  管理员邮箱: ${ADMIN_EMAIL:-（未设置）}"
echo "  邮件服务:   ${MAIL_STATE}"
echo ""
echo "  数据库:    btr（账号 ${DB_USER:-btr}，容器内网访问）"
echo "  Redis:     容器内网访问，持久化已开启"
echo "  开机自启:   docker 服务已启用，容器 restart=unless-stopped 随开机自动恢复"
echo ""
echo "  常用命令（在 deploy/ 目录下）:"
echo "    查看日志   docker compose logs -f app"
echo "    重启服务   bash restart.sh"
echo "    数据备份   bash backup.sh [保留天数]"
echo "    停止服务   docker compose down"
echo ""
echo "  上线前请检查 deploy/.env：ADMIN_EMAIL / MAIL_* / CORS_ORIGINS"
echo "=============================================================="
