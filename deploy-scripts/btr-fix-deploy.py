# -*- coding: utf-8 -*-
"""BTR 生产加固 + 构建部署脚本（服务器端执行）"""
import os
import paramiko

HOST = "103.236.92.193"
PORT = 45185
USER = "root"
PWD = "Q6Cq54BgvHp8"

ADMIN_HASH = "$2b$10$jTaFO3fOwl3y1uRjGUmVN.mGbHSufpxmvU1F1SxAWU/8bV9e03qEm"
PLAYER_HASH = "$2b$10$/O.xXAY9GsUu43Gz021nlecyH9E6uPR6QddfZIVfccxSPnBjKkeDa"
JUDGE_HASH = "$2b$10$R2bcOhbOUjT4kQBQ02RJMu8ewvSAjWOzzLSL5SkGeD7bR0LFRXp2e"
JWT_SECRET = "bcmYipJB-8GB5UIw7FIUVuFlDNqJNs2dm3p8Xqn65CLzz9ycPtb8RwvUL4p03JSd"
ADMIN_PWD = "S7r!rANA*C*^jQ"
PLAYER_PWD = "Y*KLzmGqViZz#K"
JUDGE_PWD = "&3^iuswx1WLllj"

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(HOST, port=PORT, username=USER, password=PWD, timeout=30)


def run(cmd, timeout=600):
    stdin, stdout, stderr = ssh.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode("utf-8", "replace")
    err = stderr.read().decode("utf-8", "replace")
    code = stdout.channel.recv_exit_status()
    print("$", cmd[:180])
    if out.strip():
        print(out[-3000:])
    if err.strip():
        print("ERR:", err[-1500:])
    print("[exit=%s]" % code)
    return code, out, err


# 1) 更新数据库账号密码（存在才更新）
sql = (
    "UPDATE sys_user SET password='%s' WHERE account='admin' AND status='ACTIVE';"
    "UPDATE sys_user SET password='%s' WHERE account='player' AND status='ACTIVE';"
    "UPDATE sys_user SET password='%s' WHERE account='judge' AND status='ACTIVE';"
) % (ADMIN_HASH, PLAYER_HASH, JUDGE_HASH)
# 密码里含 $ 与特殊字符，改用参数化方式：通过 mysql -e 传文件
sql_file = os.path.join(os.environ.get("TEMP", "."), "btr_pwd.sql")
with open(sql_file, "w") as f:
    f.write(sql)
sftp = ssh.open_sftp()
sftp.put(sql_file, "/tmp/btr_pwd.sql")
sftp.close()
run("mysql -ubtr -pbtr_pass btr < /tmp/btr_pwd.sql && echo SQL_OK")

# 2) 校验 sys_user 现有账号
run("mysql -ubtr -pbtr_pass btr -e \"SELECT id,account,role,substr(password,1,7) pw FROM sys_user WHERE account IN ('admin','player','judge');\" 2>/dev/null")

# 3) 更新 btr.service 环境变量：换 ADMIN_PASSWORD、加 JWT_SECRET、TRUST_PROXY=true
sed_cmds = [
    "sed -i 's|Environment=\"ADMIN_PASSWORD=admin123\"|Environment=\"ADMIN_PASSWORD=S7r!rANA*C*^jQ\"|' /etc/systemd/system/btr.service",
    "sed -i '/Environment=\"ADMIN_EMAIL=2692772782@qq.com\"/a Environment=\"JWT_SECRET=bcmYipJB-8GB5UIw7FIUVuFlDNqJNs2dm3p8Xqn65CLzz9ycPtb8RwvUL4p03JSd\"' /etc/systemd/system/btr.service",
    "sed -i '/Environment=\"JWT_SECRET=bcmYipJB-8GB5UIw7FIUVuFlDNqJNs2dm3p8Xqn65CLzz9ycPtb8RwvUL4p03JSd\"/a Environment=\"TRUST_PROXY=true\"' /etc/systemd/system/btr.service",
]
for c in sed_cmds:
    run(c)
run("grep -n Environment /etc/systemd/system/btr.service")
run("systemctl daemon-reload && echo RELOAD_OK")

# 4) 构建
run("cd /root/BRT && mvn -q clean package -DskipTests 2>&1 | tail -30", timeout=900)
run("ls -la /root/BRT/target/*.jar")

# 5) 重启服务
run("systemctl restart btr && sleep 8 && systemctl is-active btr && systemctl status btr --no-pager | head -12")

# 6) 冒烟：本机 curl（后端 8082 + 前端 20757 经 NAT）
run("curl -s -o /dev/null -w 'API:%{http_code}\\n' http://127.0.0.1:8082/api/v1/auth/captcha", timeout=60)
run("curl -s http://127.0.0.1:8082/api/v1/auth/captcha | head -c 200; echo", timeout=60)
run("curl -s -o /dev/null -w 'WEB:%{http_code}\\n' http://127.0.0.1:8080/ 2>/dev/null; curl -s -o /dev/null -w 'WEB20757:%{http_code}\\n' http://127.0.0.1:20757/ 2>/dev/null", timeout=60)

ssh.close()
print("ALL_DONE")
