# -*- coding: utf-8 -*-
"""检查 8082 监听与 uploads 目录暴露风险"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

cmds = [
    ("8082监听", "ss -ltnp | grep 8082 || echo NO_8082"),
    ("8080监听", "ss -ltnp | grep -E ':8080|:8081|:20757' || echo NONE"),
    ("BRT uploads", "ls -la /root/BRT/uploads 2>/dev/null | head -20 || echo NO_DIR"),
    ("www uploads", "ls -la /var/www/btr/uploads 2>/dev/null | head -20 || echo NO_DIR"),
    ("uploads配置", "grep -rn 'upload.dir' /root/BRT/src/main/resources/ 2>/dev/null || echo NO_CFG"),
    ("jar内配置", "unzip -p /root/BRT/target/btr-platform.jar BOOT-INF/classes/application-prod.yml 2>/dev/null | grep -A2 upload"),
]
for name, cmd in cmds:
    i, o, e = ssh.exec_command(cmd, timeout=60)
    out = o.read().decode("utf-8", "replace").strip()
    print(f"=== {name} ===\n{out}\n")
ssh.close()
print("DONE")
