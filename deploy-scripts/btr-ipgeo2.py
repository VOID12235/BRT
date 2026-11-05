# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

urls = [
    "http://ip-api.com/json/103.236.92.193?lang=zh-CN",
    "https://ipinfo.io/103.236.92.193/json",
    "https://api.ip.sb/geoip/103.236.92.193",
]
for u in urls:
    for prox in ["", "-x http://127.0.0.1:7890"]:
        cmd = "curl -s -m 15 %s '%s'; echo" % (prox, u)
        stdin, stdout, stderr = ssh.exec_command(cmd, timeout=40)
        out = stdout.read().decode("utf-8", "replace").strip()
        print(("PROXY" if prox else "DIRECT"), u, "->", out[:500])
        print("---")

ssh.close()
print("DONE")
