# -*- coding: utf-8 -*-
"""部署：RankService + 恢复 winners + 前端上传 + 清缓存"""
import paramiko, os, time

BASE = r"D:\Users\a3614\Documents\默认项目"
FE = os.path.join(BASE, "frontend")

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c, timeout=600):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

sftp = ssh.open_sftp()
# 后端源码
sftp.put(os.path.join(BASE, "src", "main", "java", "com", "btr", "platform", "service", "RankService.java"),
         "/root/BRT/src/main/java/com/btr/platform/service/RankService.java")
print("== RankService 已上传 ==")
# 前端
for local, remote in [
    (os.path.join(FE, "m", "m.js"), "/var/www/btr/m/m.js"),
    (os.path.join(FE, "m", "m.css"), "/var/www/btr/m/m.css"),
    (os.path.join(FE, "assets", "js", "rank.js"), "/var/www/btr/assets/js/rank.js"),
    (os.path.join(FE, "rank.html"), "/var/www/btr/rank.html"),
]:
    sftp.put(local, remote)
    print("前端已传:", remote)
for name in os.listdir(os.path.join(FE, "m")):
    if name.endswith(".html"):
        sftp.put(os.path.join(FE, "m", name), "/var/www/btr/m/" + name)
print("== 15 个 m/*.html 已传 ==")
sftp.close()

# 恢复 id3 获奖记录（SQL 无 $，直接执行安全）
print("== 恢复 contest_winner ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"INSERT INTO contest_winner (contest_id, award_id, work_id, signup_id, rank_no, published, created_at) VALUES (3,7,7,9,1,1,NOW()),(3,8,6,8,2,1,NOW()),(3,8,8,10,3,1,NOW()) ON DUPLICATE KEY UPDATE award_id=VALUES(award_id), published=1\" 2>&1"))
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"SELECT contest_id, award_id, work_id, signup_id, rank_no FROM contest_winner\" 2>&1"))

# 构建
print("== 构建 ==")
print(run("cd /root/BRT && /usr/bin/mvn -q -DskipTests package 2>&1 | tail -5", timeout=600))
print(run("ls -l --time-style=+%H:%M:%S /root/BRT/target/btr-platform.jar"))

# 清榜单缓存
print("== 清 rank 缓存 ==")
print(run("redis-cli --scan --pattern 'rank:contest:*' | xargs -r redis-cli del; echo cleared"))

# 重启
print("== 清场重启 ==")
print(run("for pid in $(ps -eo pid,args | grep -E 'java.*btr-platform.jar' | grep -v grep | awk '{print $1}'); do kill -9 $pid 2>/dev/null && echo killed $pid; done; sleep 2; ps -eo pid,args | grep -E 'java.*btr-platform.jar' | grep -v grep | wc -l"))
print(run("cd /root/BRT && SPRING_PROFILES_ACTIVE=prod DB_HOST=127.0.0.1 REDIS_HOST=127.0.0.1 nohup java -jar target/btr-platform.jar --server.port=8082 --server.address=127.0.0.1 > /root/BRT/btr.log 2>&1 < /dev/null & sleep 1; echo started", timeout=30))
time.sleep(25)
print("== 启动 ==")
print(run("grep -E 'Tomcat started|Port 8082 was already' /root/BRT/btr.log | tail -2"))
print(run("curl -s -m 8 http://127.0.0.1:8082/api/v1/health"))
ssh.close()
print()
print("DONE")
