# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 服务器 SecurityConfig 权限规则 ==")
print(run("grep -n 'requestMatchers\\|hasRole\\|anyRequest' /root/BRT/src/main/java/com/btr/platform/config/SecurityConfig.java | head -20"))
print("== 服务器 JwtAuthFilter authority ==")
print(run("grep -n 'SimpleGrantedAuthority' /root/BRT/src/main/java/com/btr/platform/security/JwtAuthFilter.java"))
print("== 服务器 jar 构建时间 vs 源码修改时间 ==")
print(run("ls -l --time-style=+%m-%d_%H:%M:%S /root/BRT/target/btr-platform.jar /root/BRT/src/main/java/com/btr/platform/config/SecurityConfig.java"))
print("== 最近 403 日志 ==")
print(run("grep -B3 -A15 'AccessDeniedException\\|403' /root/BRT/btr.log | tail -40"))
ssh.close()
print("DONE")
