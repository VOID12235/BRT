# -*- coding: utf-8 -*-
"""临时赛事验证 videoUrl 序列化 + 状态机"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

BASE = "http://127.0.0.1:8082/api/v1"
script = """
BASE=%s
ATOK=$(curl -s -m 8 -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d '{"account":"admin","password":"admin123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])")
echo "ADMIN_TOKEN_LEN=${#ATOK}"
# 创建临时赛事（带视频）
CREATE=$(curl -s -m 8 -X POST "$BASE/contests" -H "Authorization: Bearer $ATOK" -H 'Content-Type: application/json' -d '{"title":"_verify_temp","mode":"OFFLINE","status":"DRAFT","isPublic":false,"videoUrl":"/files/v1/test.mp4","videoName":"测试录像.mp4","signupStart":"2026-11-01T00:00:00","signupEnd":"2026-11-10T00:00:00","reviewStart":"2026-11-11T00:00:00","reviewEnd":"2026-11-20T00:00:00"}')
echo "CREATE: $CREATE"
CID=$(echo "$CREATE" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('data',{}).get('id') or d.get('data',{}).get('contest',{}).get('id',''))")
echo "CID=$CID"
if [ -n "$CID" ]; then
  echo "== GET detail =="
  curl -s -m 8 -H "Authorization: Bearer $ATOK" "$BASE/contests/$CID" | python3 -c "
import sys,json
d=json.load(sys.stdin)
c=d.get('data',{}).get('contest',{})
print('videoUrl:', c.get('videoUrl'), '| videoName:', c.get('videoName'))
print('mode:', c.get('mode'), '| status:', c.get('status'))
"
  echo "== 状态机 REVIEW->FINISHED 可用性 =="
  curl -s -m 8 -X PUT "$BASE/contests/$CID" -H "Authorization: Bearer $ATOK" -H 'Content-Type: application/json' -d '{"status":"REVIEW","mode":"OFFLINE","isPublic":false,"videoUrl":"/files/v1/test.mp4","videoName":"测试录像.mp4","signupStart":"2026-11-01T00:00:00","signupEnd":"2026-11-10T00:00:00","reviewStart":"2026-11-11T00:00:00","reviewEnd":"2026-11-20T00:00:00"}' | head -c 150
  echo
  curl -s -m 8 -X PUT "$BASE/contests/$CID" -H "Authorization: Bearer $ATOK" -H 'Content-Type: application/json' -d '{"status":"FINISHED","mode":"OFFLINE","isPublic":false,"videoUrl":"/files/v1/test.mp4","videoName":"测试录像.mp4","signupStart":"2026-11-01T00:00:00","signupEnd":"2026-11-10T00:00:00","reviewStart":"2026-11-11T00:00:00","reviewEnd":"2026-11-20T00:00:00"}' | head -c 150
  echo
  curl -s -m 8 -X PUT "$BASE/contests/$CID" -H "Authorization: Bearer $ATOK" -H 'Content-Type: application/json' -d '{"status":"PUBLISHED","mode":"OFFLINE","isPublic":false,"videoUrl":"/files/v1/test.mp4","videoName":"测试录像.mp4","signupStart":"2026-11-01T00:00:00","signupEnd":"2026-11-10T00:00:00","reviewStart":"2026-11-11T00:00:00","reviewEnd":"2026-11-20T00:00:00"}' | head -c 150
  echo
  echo "== 清理 =="
  curl -s -m 8 -X DELETE "$BASE/contests/$CID" -H "Authorization: Bearer $ATOK" | head -c 120
  echo
else
  echo "创建失败，跳过"
fi
""" % BASE
print(run(script))
ssh.close()
print("DONE")
