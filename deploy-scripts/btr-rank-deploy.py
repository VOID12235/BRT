# -*- coding: utf-8 -*-
"""同步前端+后端改动到服务器并清缓存"""
import os, paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sftp = ssh.open_sftp()

LOCAL = r"D:\Users\a3614\Documents\默认项目"
FILES = [
    (LOCAL + r"\frontend\assets\js\rank.js", "/var/www/btr/assets/js/rank.js"),
    (LOCAL + r"\frontend\assets\js\contest-detail.js", "/var/www/btr/assets/js/contest-detail.js"),
    (LOCAL + r"\frontend\m\m.js", "/var/www/btr/m/m.js"),
    (LOCAL + r"\src\main\java\com\btr\platform\service\RankService.java", "/root/BRT/src/main/java/com/btr/platform/service/RankService.java"),
    (LOCAL + r"\src\main\java\com\btr\platform\dto\Views.java", "/root/BRT/src/main/java/com/btr/platform/dto/Views.java"),
]
for lp, rp in FILES:
    sftp.put(lp, rp)
    print("UP", rp, os.path.getsize(lp))

# 清 Redis 榜单缓存
i, o, e = ssh.exec_command("redis-cli --scan --pattern 'rank:contest:*' | xargs -r redis-cli del; echo cache-cleared", timeout=20)
print(o.read().decode("utf-8", "replace").strip())

sftp.close()
ssh.close()
print("DONE")
