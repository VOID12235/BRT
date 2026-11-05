# -*- coding: utf-8 -*-
"""真正构建并重启后端"""
import paramiko, time

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd, timeout=60):
    i, o, e = ssh.exec_command(cmd, timeout=timeout)
    out = o.read().decode("utf-8", "replace").strip()
    err = e.read().decode("utf-8", "replace").strip()
    return out + ("\nERR: " + err if err else "")

print("== 构建 ==")
out = run("cd /root/BRT && mvn -q -DskipTests package 2>&1 | tail -8; ls -l --time-style=+%H:%M:%S target/btr-platform.jar", 480)
print(out[-1200:])

print("== 停止旧进程并重启 ==")
print(run("pkill -f 'btr-platform.jar' 2>/dev/null; sleep 3; ps aux | grep btr-platform | grep -v grep | wc -l", 30))
print(run("cd /root/BRT && nohup java -jar target/btr-platform.jar --server.port=8082 --server.address=127.0.0.1 > /root/BRT/btr.log 2>&1 & echo launched", 20))
time.sleep(14)
print("== 健康检查 ==")
print(run("curl -s -m 5 -o /dev/null -w 'health HTTP %{http_code}\\n' http://127.0.0.1:8082/api/v1/health; ps aux | grep btr-platform | grep -v grep | awk '{print $2}'", 20))
ssh.close()
print("DONE")
