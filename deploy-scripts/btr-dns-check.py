# -*- coding: utf-8 -*-
"""验证 btr.navitaire.club 解析状态与外网访问"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

cmds = [
    "nslookup btr.navitaire.club 2>&1 | tail -12",
    "getent hosts btr.navitaire.club || echo NO_RESOLVE",
    "curl -s -m 20 -o /dev/null -w 'HTTP:%{http_code} IP:%{remote_ip}\\n' http://btr.navitaire.club:20757/ 2>&1 || echo CURL_FAIL",
]
for c in cmds:
    stdin, stdout, stderr = ssh.exec_command(c, timeout=60)
    out = stdout.read().decode("utf-8", "replace").strip()
    err = stderr.read().decode("utf-8", "replace").strip()
    print("$", c[:90])
    print(out if out else ("ERR: " + err))
    print("---")

ssh.close()
print("DONE")
