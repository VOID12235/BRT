# -*- coding: utf-8 -*-
"""验证后端部署：health + 新接口存在性"""
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

cmds = [
    "curl -s http://127.0.0.1:8082/api/v1/health",
    "ls -la /root/BRT/target/btr-platform.jar 2>/dev/null | head -1",
    "curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8082/api/v1/admin/judge-assign/scores?contestId=3",
    "systemctl status btr.service | grep -E 'Active|Main PID' | head -2",
]
for cmd in cmds:
    stdin, stdout, stderr = cli.exec_command(cmd, timeout=60)
    out = stdout.read().decode("utf-8", "replace").strip()
    print(">>", cmd[:60])
    print("  ", out[:300])
cli.close()
