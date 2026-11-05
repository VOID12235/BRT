# -*- coding: utf-8 -*-
import paramiko, time

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
i, o, e = ssh.exec_command("timeout 25 git ls-remote https://github.com/dong121314/btr-platform.git HEAD 2>&1; echo RC=$?", timeout=60)
out = o.read().decode("utf-8", "replace").strip()
print("OUTPUT:", out[:800])
ssh.close()
print("DONE")
