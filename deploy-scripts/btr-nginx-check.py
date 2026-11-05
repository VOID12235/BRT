# -*- coding: utf-8 -*-
"""经 nginx 复现登录 403，并查 nginx 配置/日志"""
import json, paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== nginx 配置文件位置 ==")
print(run("ls /etc/nginx/conf.d/ /etc/nginx/sites-enabled/ 2>/dev/null; nginx -T 2>/dev/null | grep -E 'limit_req|limit_conn|return 403|deny' | head -10"))
print("== 经 nginx 调验证码 ==")
out = run("curl -s -m 8 'http://127.0.0.1:20757/api/v1/auth/captcha' -o /dev/null -w 'HTTP %{http_code}'")
print(out)
print("== 经 nginx 调 login（假验证码）==")
out = run("curl -s -m 10 -X POST 'http://127.0.0.1:20757/api/v1/auth/login' -H 'Content-Type: application/json' -d '{\"account\":\"a36143614u@163.com\",\"password\":\"Btr163#2026\",\"captchaKey\":\"none\",\"captchaCode\":\"XXXX\"}' -o /tmp/login_resp.txt -w 'HTTP %{http_code}'; echo; cat /tmp/login_resp.txt")
print(out)
print("== nginx error log 最近 ==")
print(run("tail -8 /var/log/nginx/error.log 2>/dev/null; tail -5 /var/log/nginx/access.log 2>/dev/null | awk '{print $1, $7, $9}'"))
ssh.close()
print("DONE")
