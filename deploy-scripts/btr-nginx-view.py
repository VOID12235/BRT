# -*- coding: utf-8 -*-
"""查看 nginx 代理头配置"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
i, o, e = ssh.exec_command("cat /etc/nginx/sites-enabled/btr", timeout=60)
print(o.read().decode("utf-8", "replace"))
ssh.close()
print("DONE")
