# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 注册验证码接口路径 ==")
print(run("grep -n 'register-code\\|sendRegisterCode\\|PostMapping' /root/BRT/src/main/java/com/btr/platform/controller/AuthController.java | head -12"))
print("== 邮件配置状态 ==")
print(run("grep -n 'mail' /root/BRT/src/main/resources/application-prod.yml | head -8"))
ssh.close()
print("DONE")
