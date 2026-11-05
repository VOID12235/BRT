# -*- coding: utf-8 -*-
"""部署 CORS 修复：上传源码->构建->清缓存->重启->health"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sftp = ssh.open_sftp()
sftp.put(r"D:\Users\a3614\Documents\默认项目\src\main\resources\application-prod.yml",
         "/root/BRT/src/main/resources/application-prod.yml")
sftp.close()

def run(c, timeout=120):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

print("== 构建 ==")
print(run("cd /root/BRT && mvn -q -DskipTests package 2>&1 | tail -3", 240))
print("== 清用户鉴权缓存 ==")
print(run("redis-cli --scan --pattern 'user:auth:*' | xargs -r redis-cli del; echo cleared"))
print("== 停旧进程 ==")
print(run("ps -eo pid,args | grep -E 'java.*btr-platform.jar' | grep -v grep | awk '{print $1}' | xargs -r kill -9; sleep 2; echo stopped"))
print("== 启动 ==")
run("SPRING_PROFILES_ACTIVE=prod DB_HOST=127.0.0.1 REDIS_HOST=127.0.0.1 nohup java -jar /root/BRT/target/btr-platform.jar --server.port=8082 --server.address=127.0.0.1 > /root/BRT/btr.log 2>&1 < /dev/null &", 15)
import time
time.sleep(18)
print("== health ==")
print(run("curl -s -m 8 http://127.0.0.1:8082/api/v1/health"))
print("== CORS 配置生效确认 ==")
print(run("grep -n 'allowed-origins' /root/BRT/src/main/resources/application-prod.yml"))
ssh.close()
print("DONE")
