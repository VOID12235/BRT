# -*- coding: utf-8 -*-
"""查看验证码校验逻辑"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

cmds = [
    "grep -rn 'captcha' /root/BRT/src/main/java/com/btr/platform/service/AuthService.java | head -15",
    "sed -n '1,60p' /root/BRT/src/main/java/com/btr/platform/service/CaptchaService.java",
]
for c in cmds:
    print("== " + c[:80])
    print(run(c))
    print()
ssh.close()
print("DONE")
