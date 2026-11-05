# -*- coding: utf-8 -*-
"""查服务器邮件配置 + 模拟找回密码接口"""
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
        print(out[-2500:])
    if err.strip():
        print("ERR:", err[-500:])
    print("---")


# 1. 邮件配置（脱敏：只显示是否有值）
run("grep -iE 'mail|smtp' /root/BRT/src/main/resources/application-prod.yml | sed 's/password:.*/password: ***/' | head -20")
run("grep -iE 'mail|smtp' /root/BRT/src/main/resources/application.yml | sed 's/password:.*/password: ***/' | head -20")

# 2. 模拟找回密码请求（观察返回与日志）
run("curl -s -m 20 -X POST http://127.0.0.1:8082/api/v1/auth/forgot-password -H 'Content-Type: application/json' -d '{\"email\":\"2692772782@qq.com\"}'; echo")
run("journalctl -u btr --since '1 min ago' --no-pager | grep -iE 'mail|smtp|forgot|reset|code' | tail -8 || echo NO_MAIL_LOG")

# 3. 邮件发送记录表
run("mysql -ubtr -pbtr_pass btr -e \"SELECT * FROM sys_email_record ORDER BY id DESC LIMIT 5;\" 2>&1 | tail -10")

ssh.close()
print("DONE")
