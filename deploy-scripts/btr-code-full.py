# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 注册验证码发送完整实现 ==")
print(run("sed -n '105,165p' /root/BRT/src/main/java/com/btr/platform/service/AuthService.java"))
print("== 重置验证码发送 ==")
print(run("sed -n '378,410p' /root/BRT/src/main/java/com/btr/platform/service/AuthService.java"))
ssh.close()
print("DONE")
