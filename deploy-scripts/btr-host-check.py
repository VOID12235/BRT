# -*- coding: utf-8 -*-
"""验证 nginx 对域名 Host 的响应（方案②免备案域名+端口访问）"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

cmds = [
    "curl -s -o /dev/null -w 'HOST-BTR:%{http_code}\\n' -H 'Host: btr.navitaire.club' http://127.0.0.1:8080/",
    "curl -s -o /dev/null -w 'HOST-IP:%{http_code}\\n' -H 'Host: 103.236.92.193' http://127.0.0.1:8080/",
    "curl -s -o /dev/null -w 'HOST-DEFAULT:%{http_code}\\n' http://127.0.0.1:8080/",
    "curl -s -m 15 -o /dev/null -w 'EXT-20757:%{http_code}\\n' http://103.236.92.193:20757/",
    "curl -s -m 15 -o /dev/null -w 'EXT-20757-HOST:%{http_code}\\n' -H 'Host: btr.navitaire.club' http://103.236.92.193:20757/",
    "grep -rn 'server_name' /etc/nginx/sites-enabled/btr | head -8",
]
for c in cmds:
    stdin, stdout, stderr = ssh.exec_command(c, timeout=60)
    out = stdout.read().decode("utf-8", "replace").strip()
    err = stderr.read().decode("utf-8", "replace").strip()
    print("$", c[:110])
    print(out if out else ("ERR: " + err))
    print("---")

ssh.close()
print("DONE")
