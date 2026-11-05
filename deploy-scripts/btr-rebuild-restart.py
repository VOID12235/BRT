# -*- coding: utf-8 -*-
"""构建 + 干净重启"""
import paramiko, time

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c, timeout=60):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

print("== 构建 ==")
out = run("cd /root/BRT && mvn -q -DskipTests package 2>&1 | tail -5; ls -l --time-style=+%H:%M:%S target/btr-platform.jar", 240)
print(out)
print("== 杀旧进程 ==")
print(run("for p in $(ps aux | grep -E 'java.*btr-platform' | grep -v grep | awk '{print $2}'); do kill -9 $p 2>/dev/null; done; sleep 4; ps aux | grep java | grep -v grep | wc -l", 30))
print("== 启动 ==")
ssh.exec_command("bash -c 'cd /root/BRT && SPRING_PROFILES_ACTIVE=prod DB_HOST=127.0.0.1 REDIS_HOST=127.0.0.1 nohup java -jar target/btr-platform.jar --server.port=8082 --server.address=127.0.0.1 > /root/BRT/btr.log 2>&1 < /dev/null &'", timeout=15)
time.sleep(50)
print("== 验证 ==")
print(run("curl -s -m 5 -o /dev/null -w 'health HTTP %{http_code}\\n' http://127.0.0.1:8082/api/v1/health", 15))
print(run("ss -ltn | grep 8082 || echo no-listen", 15))
print(run("grep -E 'Started|ERROR' /root/BRT/btr.log | tail -4", 15))
ssh.close()
print("DONE")
