# -*- coding: utf-8 -*-
"""重启后端 -> 用之前存的 token 调管理接口"""
import json, paramiko, time, urllib.request, urllib.error

BASE = "http://103.236.92.193:20757/api/v1"

def call(path, token):
    h = {"Content-Type": "application/json", "Origin": "http://103.236.92.193:20757",
         "Authorization": "Bearer " + token}
    r = urllib.request.Request(BASE + path, headers=h)
    try:
        with urllib.request.urlopen(r, timeout=15) as resp:
            return resp.status, resp.read().decode("utf-8", "replace")[:150]
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")[:150]

token = open(r"D:\Users\a3614\Documents\默认项目\_probe\token.txt").read().strip()
print("token 前30:", token[:30])

# 重启前先调一次（确认 token 有效）
st, txt = call("/admin/dashboard", token)
print("重启前 dashboard:", st, txt[:80])

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

st2, txt2 = call("/admin/dashboard", token)
print("== 重启后旧 token dashboard ==")
print("HTTP", st2, txt2[:120])
ssh.close()
print("DONE")
