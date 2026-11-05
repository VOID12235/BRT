# -*- coding: utf-8 -*-
"""核心验证：登录拿 token -> systemctl restart -> 旧 token 仍有效"""
import json, paramiko, time, urllib.request, urllib.error

BASE = "http://103.236.92.193:20757/api/v1"

def call(path, token=None, method="GET", body=None):
    h = {"Content-Type": "application/json", "Origin": "http://103.236.92.193:20757"}
    if token:
        h["Authorization"] = "Bearer " + token
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request(BASE + path, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(r, timeout=15) as resp:
            return resp.status, resp.read().decode("utf-8", "replace")[:160]
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")[:160]

# 1) 登录
key = open(r"D:\Users\a3614\Documents\默认项目\_probe\cap_jwt_key.txt").read().strip()
st, txt = call("/auth/login", method="POST",
               body={"account": "a36143614u@163.com", "password": "Btr163#2026", "captchaKey": key, "captchaCode": "5UTX"})
print("登录: HTTP", st)
d = json.loads(txt) if st == 200 else {}
token = d.get("data", {}).get("accessToken", "")
print("token 获取:", "OK" if token else "FAIL")
print("token 前30:", token[:30])

# 2) 重启后端
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
def run(c, timeout=30):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()
print("== 重启 ==")
print(run("systemctl restart btr.service && echo restarted", 30))
time.sleep(25)
print("== health ==")
print(run("curl -s -m 8 http://127.0.0.1:8082/api/v1/health"))
ssh.close()

# 3) 旧 token 调管理接口
st2, txt2 = call("/admin/dashboard", token=token)
print("== 重启后旧 token 调 dashboard ==")
print("HTTP", st2, txt2[:120])
ssh.close()
print("DONE")
