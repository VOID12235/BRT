# -*- coding: utf-8 -*-
"""完整打印登录响应"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

BASE = "http://127.0.0.1:8082/api/v1"
# 取新验证码
cap = run(f"curl -s -m 8 '{BASE}/auth/captcha'")
import json
data = json.loads(cap)["data"]
key = data["key"]
print("KEY:", key)
# 保存图片
import base64
with open(r"D:\Users\a3614\Documents\默认项目\captcha_tmp.png", "wb") as f:
    f.write(base64.b64decode(data["image"].split(",", 1)[1]))
print("img saved, 请读取验证码后手动继续")
ssh.close()
print("DONE")
