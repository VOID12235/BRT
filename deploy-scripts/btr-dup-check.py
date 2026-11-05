# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c, timeout=30):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

print("== 进程详情（含启动时间）==")
print(run("ps -eo pid,lstart,etime,args | grep -E 'btr-platform.jar' | grep -v grep"))
print("== crontab ==")
print(run("crontab -l 2>/dev/null || echo no-crontab"))
print("== systemd 相关 ==")
print(run("systemctl list-units --type=service --all 2>/dev/null | grep -i -E 'btr|java' || echo none"))
print("== /root 下脚本 ==")
print(run("ls /root/*.sh /root/BRT/*.sh 2>/dev/null | head; grep -rl 'btr-platform.jar' /root/*.sh /root/BRT/*.sh 2>/dev/null | head -3"))
print("== 端口占用 ==")
print(run("ss -ltnp 2>/dev/null | grep 8082 || echo not-listening-yet"))
ssh.close()
print("DONE")
