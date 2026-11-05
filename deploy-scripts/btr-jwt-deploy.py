# -*- coding: utf-8 -*-
"""部署 prod JWT 密钥统一 + 验证重启不掉登录"""
import json, paramiko, time, urllib.request, urllib.error

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sftp = ssh.open_sftp()
sftp.put(r"D:\Users\a3614\Documents\默认项目\src\main\resources\application-prod.yml",
         "/root/BRT/src/main/resources/application-prod.yml")
sftp.close()

def run(c, timeout=120):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

print("== 构建 ==")
print(run("cd /root/BRT && mvn -q -DskipTests package 2>&1 | tail -3", 240))
print("== systemctl restart ==")
print(run("systemctl restart btr.service && echo restarted", 30))
time.sleep(25)
print("== health ==")
print(run("curl -s -m 8 http://127.0.0.1:8082/api/v1/health"))

# 登录拿 token（带 Origin）
def login(captcha_code="XXXX", key="none"):
    body = json.dumps({"account": "a36143614u@163.com", "password": "Btr163#2026",
                       "captchaKey": key, "captchaCode": captcha_code}).encode()
    h = {"Content-Type": "application/json", "Origin": "http://103.236.92.193:20757"}
    r = urllib.request.Request("http://103.236.92.193:20757/api/v1/auth/login", data=body, headers=h, method="POST")
    try:
        with urllib.request.urlopen(r, timeout=15) as resp:
            return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        return {"code": e.code}

# 拿真实验证码
d = json.loads(urllib.request.urlopen("http://103.236.92.193:20757/api/v1/auth/captcha", timeout=10).read())
key = d["data"]["key"]
import base64
open(r"D:\Users\a3614\Documents\默认项目\_probe\cap_jwt.png", "wb").write(
    base64.b64decode(d["data"]["image"].split(",", 1)[1]))
print("验证码已保存 _probe/cap_jwt.png key:", key)
open(r"D:\Users\a3614\Documents\默认项目\_probe\cap_jwt_key.txt", "w").write(key)
ssh.close()
print("DONE")
