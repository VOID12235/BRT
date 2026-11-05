# -*- coding: utf-8 -*-
"""精确清场：列 pid → 杀 → 确认 0 → 启动 → 确认单实例"""
import paramiko, time

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c, timeout=30):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

print("== 全部相关进程 ==")
print(run("ps -eo pid,etime,args | grep -E 'java|btr' | grep -v grep | head -15"))
print("== 杀 ==")
pids = run("ps -eo pid,args | grep -E 'java.*btr-platform.jar' | grep -v grep | awk '{print $1}'")
print("pids:", pids)
for p in pids.split():
    run(f"kill -9 {p} 2>/dev/null")
time.sleep(5)
print("== 确认 0 ==")
print(run("ps -eo pid,args | grep -E 'java.*btr-platform.jar' | grep -v grep | wc -l"))
print("== 启动 ==")
ssh.exec_command("bash -c 'cd /root/BRT && SPRING_PROFILES_ACTIVE=prod DB_HOST=127.0.0.1 REDIS_HOST=127.0.0.1 nohup java -jar target/btr-platform.jar --server.port=8082 --server.address=127.0.0.1 > /root/BRT/btr.log 2>&1 < /dev/null &'", timeout=15)
time.sleep(55)
print("== 最终 ==")
print(run("ps -eo pid,etime,args | grep -E 'java.*btr-platform.jar' | grep -v grep | awk '{print $1, $2}'"))
print(run("curl -s -m 5 -o /dev/null -w 'health HTTP %{http_code}\\n' http://127.0.0.1:8082/api/v1/health", 15))
print(run("grep -E 'Started BtrApplication|APPLICATION FAILED' /root/BRT/btr.log | tail -3", 15))
ssh.close()
print("DONE")
