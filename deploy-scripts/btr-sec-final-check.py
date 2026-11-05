# -*- coding: utf-8 -*-
"""纯查询：确认 8082 收口 127.0.0.1 且服务健康（不重启）"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd, t=60):
    i, o, e = ssh.exec_command(cmd, timeout=t)
    return o.read().decode("utf-8", "replace").strip()

print("=== 服务状态 ===")
print(run("systemctl is-active btr"))

print("=== 8082 监听（预期 127.0.0.1:8082）===")
print(run("ss -ltnp | grep 8082") or "NO LISTEN")

print("=== 本机健康 ===")
print(run("curl -s -o /dev/null -w 'local8082:%{http_code}' -m 5 http://127.0.0.1:8082/api/v1/health"))
print(run("curl -s -o /dev/null -w 'nginx8080:%{http_code}' -m 5 http://127.0.0.1:8080/api/v1/health"))

print("=== 遗留通道（预期 404/403）===")
print(run("curl -s -o /dev/null -w 'uploads直读:%{http_code}' -m 5 http://127.0.0.1:8082/uploads/"))
print(run("curl -s -o /dev/null -w 'files无指纹:%{http_code}' -m 5 http://127.0.0.1:8082/api/v1/files/1/x"))

print("=== 前端接口经 nginx 正常性 ===")
print(run("curl -s -o /dev/null -w 'public-contests:%{http_code}' -m 5 'http://127.0.0.1:8080/api/v1/contests?page=1&size=1'"))
print(run("curl -s -o /dev/null -w 'captcha:%{http_code}' -m 5 http://127.0.0.1:8080/api/v1/auth/captcha"))
ssh.close()
print("DONE")
