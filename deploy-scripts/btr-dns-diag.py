# -*- coding: utf-8 -*-
"""诊断 btr.navitaire.club 域名访问"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

cmds = [
    # 1. 解析状态
    "nslookup btr.navitaire.club 2>&1 | tail -8",
    # 2. 带端口（正确地址）
    "curl -s -m 20 -o /dev/null -w 'WITH-PORT:%{http_code}\\n' http://btr.navitaire.club:20757/",
    # 3. 不带端口（80，预期被拦）
    "curl -s -m 20 -o /dev/null -w 'NO-PORT:%{http_code}\\n' http://btr.navitaire.club/ || echo NO_PORT_FAIL",
    # 4. 本地服务健康
    "systemctl is-active btr",
    "curl -s -o /dev/null -w 'LOCAL-8080:%{http_code}\\n' http://127.0.0.1:8080/",
]
for c in cmds:
    stdin, stdout, stderr = ssh.exec_command(c, timeout=60)
    out = stdout.read().decode("utf-8", "replace").strip()
    err = stderr.read().decode("utf-8", "replace").strip()
    print("$", c[:100])
    print(out if out else ("ERR: " + err))
    print("---")

ssh.close()
print("DONE")
