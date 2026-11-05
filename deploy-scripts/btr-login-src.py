# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== login 方法 ==")
print(run("sed -n '155,200p' /root/BRT/src/main/java/com/btr/platform/service/AuthService.java"))
print("== LoginReq 字段 ==")
print(run("cat /root/BRT/src/main/java/com/btr/platform/dto/LoginReq.java 2>/dev/null || find /root/BRT/src -name 'LoginReq.java' -exec cat {} \\;"))
ssh.close()
print("DONE")
