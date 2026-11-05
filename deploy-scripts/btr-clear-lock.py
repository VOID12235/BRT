# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 清理登录失败/锁定 ==")
print(run("redis-cli --scan --pattern 'login:fail:*' | xargs -r redis-cli del; redis-cli --scan --pattern 'login:lock:*' | xargs -r redis-cli del; echo done"))
print("== 快速自检登录（直接调后端，验证码走图形验证需前端，这里仅确认账号密码匹配链路）==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -N -e \"SELECT account, role, status FROM sys_user WHERE id=8\" 2>&1"))
ssh.close()
print("DONE")
