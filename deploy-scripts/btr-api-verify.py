# -*- coding: utf-8 -*-
"""鐧诲綍鍚庨獙璇佹柊鎺ュ彛琛屼负锛堝彧璇伙級"""
import paramiko, json

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

BASE = "http://127.0.0.1:8082/api/v1"
script = r"""
BASE={base}
TOK=$(curl -s -m 8 -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d '{"account":"player","password":"player123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])")
echo "== player token: ${TOK:0:12}..."
echo "== contests/1 瀛楁妫€鏌ワ紙videoUrl/videoName 鏄惁瀛樺湪锛?"
curl -s -m 8 -H "Authorization: Bearer $TOK" "$BASE/contests/1" | python3 -c "
import sys,json
d=json.load(sys.stdin)
c=d.get('contest',{})
print('status:', c.get('status'), '| mode:', c.get('mode'))
print('has videoUrl field:', 'videoUrl' in c, '| has videoName field:', 'videoName' in c)
print('myWork keys:', list(d.get('myWork',{}).keys())[:6] if d.get('myWork') else None)
print('mySignup:', d.get('mySignup'))
"
echo "== works 鍒楄〃:"
curl -s -m 8 -H "Authorization: Bearer $TOK" "$BASE/works?contestId=1&page=1&size=2" | python3 -c "
import sys,json
d=json.load(sys.stdin)
print('code:', d.get('code'), '| records:', len(d.get('records',[])))
if d.get('records'):
    w=d['records'][0]
    print('work keys sample:', [k for k in w if 'video' in k or 'file' in k or 'cover' in k][:6])
"
echo "== 鑾峰璇佷功 PDF锛堣嫢璇ヨ处鍙锋湁鑾峰璁板綍锛?"
curl -s -m 15 -o /tmp/cert.pdf -w "HTTP %{http_code} size %{size_download} type %{content_type}" -H "Authorization: Bearer $TOK" "$BASE/awards/mine/pdf?contestId=1"
echo
""".replace("{base}", BASE)
print(run(script))
ssh.close()
print("DONE")

