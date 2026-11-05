# -*- coding: utf-8 -*-
import paramiko, time

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c, timeout=30):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

print("== 杀所有实例 ==")
print(run("ps -eo pid,args | grep -E 'btr-platform.jar' | grep -v grep | awk '{print $1}' | xargs -r kill -9; sleep 3; ps -eo pid,args | grep -E 'btr-platform.jar' | grep -v grep || echo all-stopped"))
print("== 重启 ==")
run("SPRING_PROFILES_ACTIVE=prod DB_HOST=127.0.0.1 REDIS_HOST=127.0.0.1 nohup java -jar /root/BRT/target/btr-platform.jar --server.port=8082 --server.address=127.0.0.1 > /root/BRT/btr.log 2>&1 < /dev/null &", 15)
time.sleep(22)
print("== 进程 ==")
print(run("ps -eo pid,args | grep -E 'btr-platform.jar' | grep -v grep | wc -l"))
print("== health ==")
print(run("curl -s -m 8 http://127.0.0.1:8082/api/v1/health"))
ssh.close()
print("DONE")
