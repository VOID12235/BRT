# -*- coding: utf-8 -*-
import paramiko, time

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 8082 监听 ==")
print(run("ss -tlnp | grep 8082"))
print("== jar 启动时间与 PID ==")
print(run("ps aux | grep -E 'java.*btr-platform' | grep -v grep | awk '{print $2, $9}'"))
print("== 杀 135969（旧实例）==")
print(run("kill -9 135969; echo killed"))
time.sleep(3)
print("== 杀后状态 ==")
print(run("ps aux | grep -E 'java.*btr-platform' | grep -v grep | awk '{print $2, $9}'"))
print("== 健康检查 ==")
print(run('curl -s -m 5 -o /dev/null -w "HTTP %{http_code}" http://127.0.0.1:8082/api/v1/health'))
print()
print("== 8082 监听（确认 139156 持有）==")
print(run("ss -tlnp | grep 8082"))
ssh.close()
print("DONE")
