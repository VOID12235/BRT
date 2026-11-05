# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 锁定键 ==")
print(run("redis-cli --keyspace-notification '' ; redis-cli --scan --pattern 'login:lock:*' ; redis-cli --scan --pattern 'login:fail:*'"))
print("== 清理 ==")
print(run("redis-cli --scan --pattern 'login:lock:*' | xargs -r redis-cli del; redis-cli --scan --pattern 'login:fail:*' | xargs -r redis-cli del; echo cleared"))
print("== 锁定配置 ==")
print(run("grep -n 'login.lock\\|lock-minutes\\|max-fail\\|lockMinutes\\|maxFail' /root/BRT/src/main/resources/application-prod.yml /root/BRT/src/main/resources/application.yml 2>/dev/null"))
ssh.close()
print("DONE")
