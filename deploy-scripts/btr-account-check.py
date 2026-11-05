# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== account 值 ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"SELECT id, account, email FROM sys_user\" 2>&1"))
print("== 登录接口是否用 email？查注册/登录控制器 ==")
print(run("grep -rn 'findByAccount\\|findByEmail\\|login(' /root/BRT/src/main/java --include='*.java' | head -10"))
ssh.close()
print("DONE")
