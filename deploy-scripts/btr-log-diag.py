# -*- coding: utf-8 -*-
"""排查登录报错：服务状态 + 后端日志"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)


def run(cmd, timeout=90):
    stdin, stdout, stderr = ssh.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode("utf-8", "replace").strip()
    err = stderr.read().decode("utf-8", "replace").strip()
    print("$", cmd[:120])
    if out.strip():
        print(out[-3500:])
    if err.strip():
        print("ERR:", err[-500:])
    print("---")


# 1. 服务状态
run("systemctl is-active btr; systemctl status btr --no-pager | head -12")

# 2. 最近日志中的错误/异常（过滤敏感行）
run("journalctl -u btr --since '30 min ago' --no-pager | grep -iE 'error|exception|fail|warn' | tail -40 || echo NO_ERROR_30MIN")

# 3. 最近 200 行日志尾部（看请求流）
run("journalctl -u btr --since '10 min ago' --no-pager | tail -60")

# 4. 接口健康
run("curl -s -o /dev/null -w 'API:%{http_code}\\n' http://127.0.0.1:8082/api/v1/contests?page=1&size=3")

ssh.close()
print("DONE")
