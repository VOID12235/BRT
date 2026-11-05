# -*- coding: utf-8 -*-
"""服务器重新构建并重启后端（含榜单修复）"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd, timeout=60):
    i, o, e = ssh.exec_command(cmd, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip() + ("\nERR: " + e.read().decode("utf-8", "replace").strip() if e.read() else "")

# 找到旧进程并停止（用 pkill 优雅停止 java -jar btr）
print("== 停止旧服务 ==")
print(run("pkill -f 'btr-platform.jar' ; sleep 2 ; ps aux | grep btr-platform | grep -v grep | wc -l", 20))
print("== 重新打包（跳过测试）==")
out = run("cd /root/BRT && /root/BRT/mvnw -q -DskipTests clean package -f pom.xml 2>&1 | tail -5; ls -l --time-style=+%H:%M:%S target/btr-platform.jar", 480)
print(out[-1500:])
print("== 重启服务 ==")
print(run("cd /root/BRT && nohup java -jar target/btr-platform.jar --server.port=8082 --server.address=127.0.0.1 > /root/BRT/btr.log 2>&1 & echo started; sleep 12; curl -s -m 5 -o /dev/null -w 'health HTTP %{http_code}' http://127.0.0.1:8082/api/v1/health; echo", 40))
ssh.close()
print("DONE")
