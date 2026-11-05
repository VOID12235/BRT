# -*- coding: utf-8 -*-
"""prod profile 重启后端"""
import paramiko, time

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd, timeout=30):
    i, o, e = ssh.exec_command(cmd, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

print("== 杀全部旧进程 ==")
print(run("pkill -9 -f 'btr-platform.jar'; pkill -9 -f 'java -jar target'; sleep 3; ps aux | grep -E 'btr-platform|java -jar' | grep -v grep | wc -l", 30))
print("== prod 启动 ==")
print(run("bash -c 'cd /root/BRT && SPRING_PROFILES_ACTIVE=prod nohup java -jar target/btr-platform.jar --server.port=8082 --server.address=127.0.0.1 > /root/BRT/btr.log 2>&1 < /dev/null &' && echo launched", 15))
time.sleep(20)
print("== 健康 ==")
print(run("curl -s -m 5 -o /dev/null -w 'HTTP %{http_code}' http://127.0.0.1:8082/api/v1/health"))
print()
print("== 进程 profile ==")
print(run("ps aux | grep btr-platform | grep -v grep | awk '{print $2}' | head -3; grep -o 'profile is active.*' /root/BRT/btr.log | tail -1"))
ssh.close()
print("DONE")
