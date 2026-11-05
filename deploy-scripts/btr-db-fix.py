# -*- coding: utf-8 -*-
"""修复生产库 sys_user 缺 token_version 列"""
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
        print(out[-1500:])
    if err.strip():
        print("ERR:", err[-600:])
    print("---")


# 1. 检查当前表结构是否有该列
run("mysql -ubtr -pbtr_pass btr -e \"SHOW COLUMNS FROM sys_user LIKE 'token_version';\" 2>&1")

# 2. 加列（幂等：已存在则跳过）
run("""mysql -ubtr -pbtr_pass btr -e "ALTER TABLE sys_user ADD COLUMN token_version INT NOT NULL DEFAULT 0;" 2>&1 || echo ALTER_FAIL""")

# 3. 确认加列成功
run("mysql -ubtr -pbtr_pass btr -e \"SHOW COLUMNS FROM sys_user LIKE 'token_version';\" 2>&1")

# 4. 验证登录 SQL 通路（不带验证码会返回验证码错误，但 SQL 错应消失）
run("curl -s -m 20 -X POST http://127.0.0.1:8082/api/v1/auth/login -H 'Content-Type: application/json' -d '{\"account\":\"admin\",\"password\":\"S7r!rANA*C*^jQ\"}' | head -c 300; echo")

# 5. 看最新日志是否有新 SQL 错
run("journalctl -u btr --since '1 min ago' --no-pager | grep -iE 'token_version|BadSqlGrammar|SQLSyntaxError' | tail -5 || echo NO_SQL_ERROR")

ssh.close()
print("DONE")
