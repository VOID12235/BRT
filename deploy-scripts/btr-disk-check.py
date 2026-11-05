# -*- coding: utf-8 -*-
import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
def run(c, timeout=60):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()
print("== 磁盘大头 ==")
print(run("du -sh /root/BRT/target /root/BRT/*.log /var/log/nginx /var/lib/nginx /root/.m2 /tmp 2>/dev/null"))
print("== nginx 日志大小 ==")
print(run("ls -lh /var/log/nginx/ 2>/dev/null"))
print("== target 下 jar ==")
print(run("ls -lh /root/BRT/target/*.jar 2>/dev/null"))
ssh.close()
print("DONE")
