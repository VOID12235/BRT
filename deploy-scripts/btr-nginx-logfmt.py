# -*- coding: utf-8 -*-
"""nginx log_format 加 upstream_status，前端 api.js 错误提示优化"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

# 1) 在 http 上下文加 log_format（nginx.conf 主文件）
main = run("cat /etc/nginx/nginx.conf")
if "log_format btr" not in main:
    new = main.replace("http {", "http {\n    log_format btr '$remote_addr - [$time_local] \"$request\" $status $body_bytes_sent us=$upstream_status rt=$request_time';")
    sftp = ssh.open_sftp()
    sftp.putfo(__import__("io").BytesIO(new.encode()), "/tmp/nginx.conf.new")
    sftp.close()
    run("cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.bak && cp /tmp/nginx.conf.new /etc/nginx/nginx.conf")
    print("nginx.conf 已加 log_format btr")

# 2) btr 站点 access_log 用 btr 格式
site = run("cat /etc/nginx/sites-enabled/btr")
if "access_log" not in site:
    lines = site.split("\n")
    out = []
    for ln in lines:
        out.append(ln)
        if "listen 8080;" in ln or "listen 80 default_server;" in ln or "listen 443 ssl default_server;" in ln or "listen 8081;" in ln:
            out.append("    access_log /var/log/nginx/access.log btr;")
    new = "\n".join(out)
    sftp = ssh.open_sftp()
    sftp.putfo(__import__("io").BytesIO(new.encode()), "/tmp/btr_site.new")
    sftp.close()
    run("cp /etc/nginx/sites-enabled/btr /etc/nginx/sites-enabled/btr.bak && cp /tmp/btr_site.new /etc/nginx/sites-enabled/btr")
    print("sites-enabled/btr 已加 access_log btr 格式")

print("== 校验 ==")
print(run("nginx -t 2>&1"))
print(run("nginx -s reload && echo reloaded"))
ssh.close()
print("DONE")
