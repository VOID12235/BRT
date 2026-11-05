# -*- coding: utf-8 -*-
"""拿验证码 key + 图片，供识别"""
import base64, json, paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

out = run("curl -s -m 8 'http://127.0.0.1:8082/api/v1/auth/captcha'")
d = json.loads(out)
data = d.get("data", {})
print("key:", data.get("key"))
img = data.get("image", "")
print("img len:", len(img))
if img.startswith("data:image/png;base64,"):
    raw = base64.b64decode(img.split(",", 1)[1])
    with open(r"D:\Users\a3614\Documents\默认项目\_probe\captcha.png", "wb") as f:
        f.write(raw)
    print("saved captcha.png", len(raw))
ssh.close()
print("DONE")
