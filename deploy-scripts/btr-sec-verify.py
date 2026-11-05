# -*- coding: utf-8 -*-
"""重启 btr 服务并验证加固效果"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd, t=60):
    i, o, e = ssh.exec_command(cmd, timeout=t)
    return o.read().decode("utf-8", "replace").strip(), e.read().decode("utf-8", "replace").strip()

out, _ = run("systemctl daemon-reload && systemctl restart btr && sleep 10 && systemctl is-active btr")
print("=== 服务状态 ===\n" + out)

out, _ = run("ss -ltnp | grep 8082")
print("=== 8082 监听（应仅 127.0.0.1）===\n" + (out or "NO LISTEN"))

out, _ = run("curl -s -o /dev/null -w 'local8082:%{http_code}' -m 5 http://127.0.0.1:8082/api/v1/health; echo; "
             "curl -s -o /dev/null -w 'local8080:%{http_code}' -m 5 http://127.0.0.1:8080/api/v1/health")
print("=== 本机健康 ===\n" + out)

out, _ = run("journalctl -u btr --since '2 minutes ago' --no-pager | grep -iE 'error|exception|started' | tail -5")
print("=== 启动日志 ===\n" + (out or "(无异常)"))

out, _ = run("curl -s -o /dev/null -w '%{http_code}' -m 5 http://127.0.0.1:8082/uploads/ 2>/dev/null; echo ' <- /uploads/ 直读(应404)'; "
             "curl -s -o /dev/null -w '%{http_code}' -m 5 'http://127.0.0.1:8082/api/v1/files/3c0f11ee42e26135f526210c0b1f281a.png' 2>/dev/null; echo ' <- files 无指纹(应404)'")
print("=== 遗留通道验证 ===\n" + out)
ssh.close()
print("DONE")
