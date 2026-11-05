# -*- coding: utf-8 -*-
"""检查手机端 m/ 页面是否存在与可访问"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)


def run(cmd, timeout=90):
    stdin, stdout, stderr = ssh.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode("utf-8", "replace").strip()
    err = stderr.read().decode("utf-8", "replace").strip()
    print("$", cmd[:130])
    if out.strip():
        print(out[-2500:])
    if err.strip():
        print("ERR:", err[-500:])
    print("---")


# 1. m/ 目录文件列表
run("ls -la /var/www/btr/m/ 2>&1 | head -30")

# 2. 直连访问 m/index.html
run("curl -s -o /dev/null -w 'M-INDEX:%{http_code}\\n' http://127.0.0.1:8080/m/index.html")
run("curl -s -o /dev/null -w 'M-LOGIN:%{http_code}\\n' http://127.0.0.1:8080/m/login.html")

# 3. PC 首页是否引用/跳转手机端（查 UA 跳转逻辑）
run("grep -rn 'm/index\|location.*m/\|userAgent\|isMobile' /var/www/btr/index.html | head -10 || echo NO_REDIRECT_IN_INDEX")

# 4. 外网带端口访问 m/index.html
run("curl -s -m 20 -o /dev/null -w 'EXT-M-INDEX:%{http_code}\\n' http://btr.navitaire.club:20757/m/index.html")

# 5. 最新 nginx 错误日志（最近 10 分钟）
run("tail -20 /var/log/nginx/error.log 2>/dev/null || echo NO_NGINX_ERR")

ssh.close()
print("DONE")
