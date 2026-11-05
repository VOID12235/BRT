# -*- coding: utf-8 -*-
"""构建 + 重启 + 验证各类型 inline 响应头"""
import paramiko, time

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sftp = ssh.open_sftp()
sftp.put(r"D:\Users\a3614\Documents\默认项目\src\main\java\com\btr\platform\service\FileService.java",
         "/root/BRT/src/main/java/com/btr/platform/service/FileService.java")
sftp.close()

def run(c, timeout=120):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

print("== 构建 ==")
print(run("cd /root/BRT && mvn -q -DskipTests package 2>&1 | tail -3", 240))
print("== 重启 ==")
print(run("systemctl restart btr.service && echo restarted", 30))
time.sleep(25)
print("== health ==")
print(run("curl -s -m 8 http://127.0.0.1:8082/api/v1/health"))
ssh.close()
print("DONE")
