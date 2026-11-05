# -*- coding: utf-8 -*-
"""获取验证码图片保存到本地"""
import paramiko, base64, json

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

out = run("curl -s -m 8 'http://127.0.0.1:8082/api/v1/auth/captcha'")
data = json.loads(out)["data"]
key = data["key"]
img = data["image"].split(",", 1)[1]
with open(r"D:\Users\a3614\Documents\默认项目\captcha_tmp.png", "wb") as f:
    f.write(base64.b64decode(img))
with open(r"D:\Users\a3614\Documents\默认项目\captcha_key.txt", "w") as f:
    f.write(key)
print("saved captcha", key, "img_bytes:", len(img))
ssh.close()
