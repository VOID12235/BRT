# -*- coding: utf-8 -*-
"""原始响应结构检查 v2"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace")

BASE = "http://127.0.0.1:8082/api/v1"
script = """
BASE=%s
TOK=$(curl -s -m 8 -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d '{"account":"player","password":"player123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])")
echo "TOKEN_LEN=${#TOK}"
echo "== contests/1 raw =="
curl -s -m 8 -H "Authorization: Bearer $TOK" "$BASE/contests/1" | head -c 1600
echo
echo "== contests list raw (first 400) =="
curl -s -m 8 -H "Authorization: Bearer $TOK" "$BASE/contests?all=true&page=1&size=3" | head -c 400
echo
echo "== works raw (first 300) =="
curl -s -m 8 -H "Authorization: Bearer $TOK" "$BASE/works?contestId=1&page=1&size=2" | head -c 300
""" % BASE
print(run(script))
ssh.close()
print("DONE")
