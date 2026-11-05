# -*- coding: utf-8 -*-
"""干净重启：单进程 + 长等待验证"""
import paramiko, time

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd, timeout=30):
    i, o, e = ssh.exec_command(cmd, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

print("== 清场 ==")
print(run("pkill -9 -f 'btr-platform.jar'; sleep 4; ps aux | grep btr-platform | grep -v grep | wc -l", 30))
print("== 启动 ==")
ssh.exec_command("bash -c 'cd /root/BRT && SPRING_PROFILES_ACTIVE=prod DB_HOST=127.0.0.1 REDIS_HOST=127.0.0.1 nohup java -jar target/btr-platform.jar --server.port=8082 --server.address=127.0.0.1 > /root/BRT/btr.log 2>&1 < /dev/null &'", timeout=15)
time.sleep(35)
print("== 验证 ==")
print(run("curl -s -m 5 -o /dev/null -w 'health HTTP %{http_code}\\n' http://127.0.0.1:8082/api/v1/health", 15))
print(run("ps aux | grep btr-platform | grep -v grep | awk '{print $2, $9}'", 15))
ssh.close()
print("DONE")
