# -*- coding: utf-8 -*-
"""部署后端：DDL(contest 视频列) + mvn 构建 + restart + 验证"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

# 0) 同步本机改动到服务器源码
import os
LOCAL = r"D:\Users\a3614\Documents\默认项目\src\main\java\com\btr\platform"
REMOTE = "/root/BRT/src/main/java/com/btr/platform"
sftp = ssh.open_sftp()
files = [
    "entity/Contest.java", "dto/Requests.java", "dto/Views.java",
    "service/ContestService.java", "service/WorkService.java", "service/JudgeService.java",
]
for f in files:
    sftp.put(os.path.join(LOCAL, f), REMOTE + "/" + f)
    print("SYNC", f)
sftp.close()

def run(cmd, t=300):
    i, o, e = ssh.exec_command(cmd, timeout=t)
    out = o.read().decode("utf-8", "replace").strip()
    err = e.read().decode("utf-8", "replace").strip()
    return out, err

# 1) DDL：检查并补列
out, err = run("mysql -uroot -pQ6Cq54BgvHp8 btr -e \"SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='btr' AND table_name='contest' AND column_name='video_url';\" 2>&1 | tail -1")
print("video_url 列数:", out)
if "0" in out:
    run("mysql -uroot -pQ6Cq54BgvHp8 btr -e \"ALTER TABLE contest ADD COLUMN video_url VARCHAR(512) NULL AFTER mode, ADD COLUMN video_name VARCHAR(255) NULL AFTER video_url;\"", 60)
    print("DDL ADDED")
else:
    print("video_url 已存在")

# 2) 构建
print("building... (约1-2分钟)")
out, err = run("cd /root/BRT && mvn -q clean package -DskipTests 2>&1 | tail -20", 600)
if err and "ERROR" in err:
    print("BUILD ERR:", err[:2000])
else:
    print("BUILD stdout:", out[:500] if out else "(empty=success)")

# 3) restart
out, err = run("systemctl restart btr && sleep 6 && systemctl is-active btr", 60)
print("btr:", out)

# 4) 验证
out, err = run("curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/api/v1/contests?page=1&size=1", 30)
print("contests:", out)
out, err = run("curl -s 'http://127.0.0.1:8080/api/v1/contests/1' | head -c 400", 30)
print("detail1:", out[:300])
ssh.close()
print("DONE")
