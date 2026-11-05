# -*- coding: utf-8 -*-
"""补 contest 表 video 列并重启后端"""
import paramiko, time

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd, timeout=30):
    i, o, e = ssh.exec_command(cmd, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

print("== 加列 ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"ALTER TABLE contest ADD COLUMN video_url VARCHAR(512) NULL COMMENT '线下赛事录像视频URL' AFTER is_public, ADD COLUMN video_name VARCHAR(255) NULL COMMENT '视频名称' AFTER video_url;\" && echo ALTER-OK"))
print("== 重启 ==")
print(run("pkill -f 'btr-platform.jar' 2>/dev/null; sleep 3; ps aux | grep btr-platform | grep -v grep | wc -l", 30))
# 用 setsid 彻底脱离会话避免 stdout 阻塞
print(run("cd /root/BRT && setsid nohup java -jar target/btr-platform.jar --server.port=8082 --server.address=127.0.0.1 > /root/BRT/btr.log 2>&1 < /dev/null & disown; echo launched", 15))
time.sleep(15)
print("== 健康 ==")
print(run("curl -s -m 5 -o /dev/null -w 'HTTP %{http_code}' http://127.0.0.1:8082/api/v1/health"))
print()
ssh.close()
print("DONE")
