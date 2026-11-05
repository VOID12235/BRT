# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== AuthService 验证码相关 ==")
print(run("grep -n '验证码\\|captcha\\|sendCode\\|已注册\\|已存在\\|sendVerif\\|code' /root/BRT/src/main/java/com/btr/platform/service/AuthService.java | head -40"))
ssh.close()
print("DONE")
