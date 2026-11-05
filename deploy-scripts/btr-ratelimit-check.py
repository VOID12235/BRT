# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== Redis 限流键 ==")
print(run("redis-cli --scan --pattern 'login-ip*' | head; redis-cli --scan --pattern 'login:*' | head"))
print("== RateLimitService check 实现 ==")
print(run("grep -n -A12 'public.*check' /root/BRT/src/main/java/com/btr/platform/service/RateLimitService.java 2>/dev/null | head -20"))
print("== 清理限流 ==")
print(run("redis-cli --scan --pattern 'login-ip*' | xargs -r redis-cli del; redis-cli --scan --pattern 'login:*' | xargs -r redis-cli del; echo cleared"))
ssh.close()
print("DONE")
