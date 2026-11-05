# -*- coding: utf-8 -*-
"""上传 JwtAuthFilter 修复 → 构建 → 清用户缓存 → 重启 → 验证"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c, timeout=600):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

# 1. 上传修复后的源码
sftp = ssh.open_sftp()
sftp.put(r"D:\Users\a3614\Documents\默认项目\src\main\java\com\btr\platform\security\JwtAuthFilter.java",
         "/root/BRT/src/main/java/com/btr/platform/security/JwtAuthFilter.java")
sftp.close()
print("== 源码已上传 ==")
print(run("grep -n 'setRole\\|parts.length >= 3' /root/BRT/src/main/java/com/btr/platform/security/JwtAuthFilter.java | head -5"))

# 2. 构建
print("== 构建 ==")
print(run("cd /root/BRT && /usr/bin/mvn -q -DskipTests package 2>&1 | tail -5", timeout=600))
print(run("ls -l --time-style=+%H:%M:%S /root/BRT/target/btr-platform.jar"))

# 3. 清用户认证缓存（旧格式缺 role）
print("== 清 user:auth 缓存 ==")
print(run("redis-cli --scan --pattern 'user:auth:*' | xargs -r redis-cli del; echo cleared"))

# 4. 精确重启
print("== 清场重启 ==")
print(run("for pid in $(ps -eo pid,args | grep -E 'java.*btr-platform.jar' | grep -v grep | awk '{print $1}'); do kill -9 $pid 2>/dev/null && echo killed $pid; done; sleep 2; ps -eo pid,args | grep -E 'java.*btr-platform.jar' | grep -v grep | wc -l"))
print(run("cd /root/BRT && SPRING_PROFILES_ACTIVE=prod DB_HOST=127.0.0.1 REDIS_HOST=127.0.0.1 nohup java -jar target/btr-platform.jar --server.port=8082 --server.address=127.0.0.1 > /root/BRT/btr.log 2>&1 < /dev/null & sleep 1; echo started", timeout=30))
import time
time.sleep(25)
print("== 启动日志 ==")
print(run("grep -E 'Tomcat started|Started BtrPlatformApplication|Port 8082 was already' /root/BRT/btr.log | tail -3"))
print("== health ==")
print(run("curl -s -m 8 http://127.0.0.1:8082/api/v1/health"))
ssh.close()
print()
print("DONE")
