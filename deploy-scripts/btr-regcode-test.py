# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 调 send-register-code（已注册邮箱）==")
print(run("curl -s -m 10 -X POST http://127.0.0.1:8082/api/v1/auth/send-register-code -H 'Content-Type: application/json' -d '{\"email\":\"a36143614u@163.com\"}'"))
print()
print("== 日志 exists-send ==")
print(run("grep 'exists-send\\|REGISTER_CODE' /root/BRT/btr.log | tail -4"))
print("== 未注册邮箱对照 ==")
print(run("curl -s -m 10 -X POST http://127.0.0.1:8082/api/v1/auth/send-register-code -H 'Content-Type: application/json' -d '{\"email\":\"newuser-test-2026@example.com\"}'"))
ssh.close()
print("DONE")
