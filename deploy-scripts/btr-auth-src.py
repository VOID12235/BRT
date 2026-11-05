# -*- coding: utf-8 -*-
"""查看登录请求字段与验证码校验逻辑"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

cmds = [
    "grep -rn 'captcha' /root/BRT/src/main/java/com/btr/platform/controller/AuthController.java 2>/dev/null | head -12",
    "grep -rln 'LoginReq\\|captchaKey\\|captchaCode' /root/BRT/src/main/java/com/btr/platform/dto/Requests.java /root/BRT/src/main/java/com/btr/platform/controller/AuthController.java 2>/dev/null",
    "sed -n '1,80p' /root/BRT/src/main/java/com/btr/platform/controller/AuthController.java 2>/dev/null | grep -A30 'login'",
]
for c in cmds:
    print("== " + c[:80])
    print(run(c))
    print()
ssh.close()
print("DONE")
