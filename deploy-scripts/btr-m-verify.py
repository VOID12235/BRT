# -*- coding: utf-8 -*-
"""验证手机端满血版部署：页面 200 + 版本号 + 公开接口"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

cmds = [
    "for p in forgot-password portfolio judge admin index login center contests contest rank notices signup work about download; do code=$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/m/$p.html); echo \"$p.html -> $code\"; done",
    "curl -s http://127.0.0.1:8080/m/judge.html | grep -o 'm.js?v=[0-9a-z]*' | head -1",
    "curl -s http://127.0.0.1:8080/m/forgot-password.html | grep -o 'm.css?v=[0-9a-z]*' | head -1",
    "curl -s -o /dev/null -w 'auth/captcha -> %{http_code}\\n' http://127.0.0.1:8080/api/v1/auth/captcha",
    "curl -s -o /dev/null -w 'contests -> %{http_code}\\n' 'http://127.0.0.1:8080/api/v1/contests?page=1&size=3'",
    "curl -s -o /dev/null -w 'portfolio-api(未登录) -> %{http_code}\\n' http://127.0.0.1:8080/api/v1/users/1/portfolio",
]
for c in cmds:
    i, o, e = ssh.exec_command(c, timeout=60)
    print(o.read().decode("utf-8", "replace").strip())
    err = e.read().decode("utf-8", "replace").strip()
    if err: print("ERR:", err)
ssh.close()
print("DONE")
