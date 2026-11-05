# -*- coding: utf-8 -*-
import paramiko, time

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c, timeout=30):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

print("== btr.service 配置 ==")
print(run("cat /etc/systemd/system/btr.service 2>/dev/null || systemctl cat btr.service 2>/dev/null | head -30"))
print("== 杀残留实例 ==")
print(run("ps -eo pid,args | grep -E 'btr-platform.jar' | grep -v grep | awk '{print $1}' | xargs -r kill -9; sleep 2; echo killed"))
print("== systemctl restart ==")
print(run("systemctl restart btr.service && echo restarted"))
time.sleep(25)
print("== 进程数 ==")
print(run("ps -eo pid,args | grep -E 'btr-platform.jar' | grep -v grep | wc -l"))
print("== health ==")
print(run("curl -s -m 8 http://127.0.0.1:8082/api/v1/health || echo health-fail"))
ssh.close()
print("DONE")
