# -*- coding: utf-8 -*-
"""验证修复后：进程/health/登录/dashboard"""
import json, paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c, timeout=30):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

print("== 进程 ==")
print(run("ps -eo pid,etime,args | grep -E 'java.*btr-platform.jar' | grep -v grep"))
print("== 启动日志 ==")
print(run("grep -E 'Tomcat started|Started BtrPlatformApplication|Port 8082 was already|ERROR' /root/BRT/btr.log | tail -5"))
print("== health ==")
print(run("curl -s -m 8 http://127.0.0.1:8082/api/v1/health"))

# 取验证码并识别
out = run("curl -s -m 8 'http://127.0.0.1:8082/api/v1/auth/captcha'")
d = json.loads(out)
data = d.get("data", {})
key = data.get("key")
import base64
img = data.get("image", "")
raw = base64.b64decode(img.split(",", 1)[1])
open(r"D:\Users\a3614\Documents\默认项目\_probe\captcha2.png", "wb").write(raw)
print("== 验证码 key ==", key, "len", len(raw))
ssh.close()
print("DONE")
