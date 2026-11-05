# -*- coding: utf-8 -*-
"""部署后冒烟验证"""
import json
import paramiko

HOST = "103.236.92.193"
PORT = 45185
USER = "root"
PWD = "Q6Cq54BgvHp8"

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(HOST, port=PORT, username=USER, password=PWD, timeout=30)


def run(cmd, timeout=120):
    stdin, stdout, stderr = ssh.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode("utf-8", "replace")
    err = stderr.read().decode("utf-8", "replace")
    code = stdout.channel.recv_exit_status()
    print("$", cmd[:160])
    if out.strip():
        print(out[-2500:])
    if err.strip():
        print("ERR:", err[-800:])
    print("[exit=%s]" % code)
    return out

# 等服务完全起来
run("sleep 15; curl -s -o /dev/null -w 'API:%{http_code}\\n' http://127.0.0.1:8082/api/v1/auth/captcha")
run("curl -s -o /dev/null -w 'NGINX:%{http_code}\\n' http://127.0.0.1:8080/")
run("curl -s -o /dev/null -w 'ROBOTS:%{http_code}\\n' http://127.0.0.1:8080/robots.txt; curl -s -o /dev/null -w 'SITEMAP:%{http_code}\\n' http://127.0.0.1:8080/sitemap.xml")

# 外部全链路（本机连服务器公网 NAT 20757）
run("curl -s -o /dev/null -w 'NAT-HOME:%{http_code}\\n' http://103.236.92.193:20757/ -m 20")
run("curl -s -m 20 http://103.236.92.193:20757/ | grep -o '<title>[^<]*</title>' | head -1")
run("curl -s -m 20 http://103.236.92.193:20757/ | grep -o '<meta name=\"description\"[^>]*>' | head -1")

# 版本参数已生效
run("curl -s -m 20 http://103.236.92.193:20757/m/index.html | grep -o 'm/m.js?v=[^\\\"]*' | head -1")

# 新密码登录 admin
run("curl -s -m 20 -X POST http://127.0.0.1:8082/api/v1/auth/login -H 'Content-Type: application/json' -d '{\"account\":\"admin\",\"password\":\"S7r!rANA*C*^jQ\"}' | head -c 300; echo")

# 旧密码应失败
run("curl -s -m 20 -X POST http://127.0.0.1:8082/api/v1/auth/login -H 'Content-Type: application/json' -d '{\"account\":\"admin\",\"password\":\"admin123\"}' | head -c 200; echo")

# 未公示赛事 winners 接口（无 token 应 403）
run("curl -s -m 20 -o /dev/null -w 'WINNERS-REVIEW:%{http_code}\\n' 'http://127.0.0.1:8082/api/v1/ranks/winners?contestId=2'")
# 已公示赛事 winners（应 200）
run("curl -s -m 20 -o /dev/null -w 'WINNERS-PUB:%{http_code}\\n' 'http://127.0.0.1:8082/api/v1/ranks/winners?contestId=3'")

# 赛事列表
run("curl -s -m 20 http://127.0.0.1:8082/api/v1/contests | head -c 300; echo")

# 榜单（赛事3 已公示 -> winner 表）
run("curl -s -m 20 'http://127.0.0.1:8082/api/v1/ranks/result?contestId=3' | head -c 400; echo")

ssh.close()
print("DONE")
