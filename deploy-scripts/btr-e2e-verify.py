# -*- coding: utf-8 -*-
"""绔埌绔獙璇侊細鐧诲綍->videoUrl 搴忓垪鍖?>鐘舵€佹満->璇佷功"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=45)
    return o.read().decode("utf-8", "replace").strip()

BASE = "http://127.0.0.1:8082/api/v1"
KEY = "5d7b60a0-f01e-4dab-8dbe-042930f4e04c"
CODE = "2EHK"

script = """
BASE=%s
KEY=%s
CODE=%s
ATOK=$(curl -s -m 8 -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d "{\\"account\\":\\"admin\\",\\"password\\":\\"admin123\\",\\"captchaKey\\":\\"$KEY\\",\\"captchaCode\\":\\"$CODE\\"}" | python3 -c "import sys,json;print(json.load(sys.stdin).get('data',{}).get('accessToken',''))")
echo "ADMIN_TOKEN_LEN=${#ATOK}"
if [ -z "$ATOK" ]; then echo "鐧诲綍澶辫触锛岀粓姝?; exit 0; fi
CREATE=$(curl -s -m 8 -X POST "$BASE/contests" -H "Authorization: Bearer $ATOK" -H 'Content-Type: application/json' -d '{"title":"_verify_temp","mode":"OFFLINE","status":"DRAFT","isPublic":false,"videoUrl":"/files/v1/test.mp4","videoName":"娴嬭瘯褰曞儚.mp4","signupStart":"2026-11-01T00:00:00","signupEnd":"2026-11-10T00:00:00","reviewStart":"2026-11-11T00:00:00","reviewEnd":"2026-11-20T00:00:00"}')
echo "CREATE: $CREATE"
CID=$(echo "$CREATE" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('data',{}).get('id',''))")
echo "CID=$CID"
if [ -z "$CID" ]; then echo "鍒涘缓澶辫触锛岀粓姝?; exit 0; fi
echo "== GET detail锛坴ideoUrl 搴斿彲瑙侊級=="
curl -s -m 8 -H "Authorization: Bearer $ATOK" "$BASE/contests/$CID" | python3 -c "
import sys,json
c=json.load(sys.stdin).get('data',{}).get('contest',{})
print('videoUrl:', c.get('videoUrl'), '| videoName:', c.get('videoName'))
"
echo "== 鐘舵€佹満 DRAFT->REVIEW->FINISHED->PUBLISHED =="
for ST in REVIEW FINISHED PUBLISHED; do
  R=$(curl -s -m 8 -X PUT "$BASE/contests/$CID" -H "Authorization: Bearer $ATOK" -H 'Content-Type: application/json' -d "{\\"status\\":\\"$ST\\",\\"mode\\":\\"OFFLINE\\",\\"isPublic\\":false,\\"videoUrl\\":\\"/files/v1/test.mp4\\",\\"videoName\\":\\"娴嬭瘯褰曞儚.mp4\\",\\"signupStart\\":\\"2026-11-01T00:00:00\\",\\"signupEnd\\":\\"2026-11-10T00:00:00\\",\\"reviewStart\\":\\"2026-11-11T00:00:00\\",\\"reviewEnd\\":\\"2026-11-20T00:00:00\\"}")
  echo "  -> $ST: $R" | head -c 200
  echo
done
echo "== 娓呯悊涓存椂璧涗簨 =="
curl -s -m 8 -X DELETE "$BASE/contests/$CID" -H "Authorization: Bearer $ATOK" | head -c 120
echo
""" % (BASE, KEY, CODE)
print(run(script))
ssh.close()
print("DONE")

