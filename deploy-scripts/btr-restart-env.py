# -*- coding: utf-8 -*-
"""查 redis/mysql 端口映射并带 env 重启"""
import paramiko, time

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd, timeout=30):
    i, o, e = ssh.exec_command(cmd, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

print("== redis/mysql 映射 ==")
print(run("docker ps --format '{{.Names}} {{.Ports}}' 2>/dev/null | head -5") or "(无 docker 或未运行)")
print("== 本机端口 ==")
print(run("ss -ltn 2>/dev/null | grep -E '6379|3306' | head -5") or "(无)")
print("== 杀旧进程 ==")
print(run("pkill -9 -f 'btr-platform.jar' 2>/dev/null; sleep 2; ps aux | grep btr-platform | grep -v grep | wc -l", 20))
print("== 带 env 启动 ==")
cmd = "bash -c 'cd /root/BRT && SPRING_PROFILES_ACTIVE=prod DB_HOST=127.0.0.1 REDIS_HOST=127.0.0.1 nohup java -jar target/btr-platform.jar --server.port=8082 --server.address=127.0.0.1 > /root/BRT/btr.log 2>&1 < /dev/null &'"
i, o, e = ssh.exec_command(cmd, timeout=15)
time.sleep(2)
ssh.close()
print("launched")
print("DONE")
