# -*- coding: utf-8 -*-
"""部署安全加固：同步 2 个修复文件 + 8082 收口 127.0.0.1 + 构建重启 + 验证"""
import paramiko
import time

HOST, PORT, USER, PWD = "103.236.92.193", 45185, "root", "Q6Cq54BgvHp8"
LOCAL = r"D:\Users\a3614\Documents\默认项目\src\main\java\com\btr\platform"
REMOTE = "/root/BRT/src/main/java/com/btr/platform"

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(HOST, port=PORT, username=USER, password=PWD, timeout=30)

def run(cmd, t=60):
    i, o, e = ssh.exec_command(cmd, timeout=t)
    out = o.read().decode("utf-8", "replace")
    err = e.read().decode("utf-8", "replace")
    return out.strip(), err.strip()

# 1. 同步修复文件
sftp = ssh.open_sftp()
for rel in ["common/ClientIpResolver.java", "config/WebConfig.java"]:
    sftp.put(LOCAL + "\\" + rel.replace("/", "\\"), REMOTE + "/" + rel)
    print(f"SFTP OK: {rel}")
sftp.close()

# 2. systemd ExecStart 增加 --server.address=127.0.0.1
out, err = run("grep 'server.port=8082' /etc/systemd/system/btr.service && "
               "sed -i 's|--server.port=8082|--server.port=8082 --server.address=127.0.0.1|' /etc/systemd/system/btr.service && "
               "grep ExecStart /etc/systemd/system/btr.service")
print("=== ExecStart 更新 ===\n" + (out or err))

# 3. 后台构建
run("cd /root/BRT && nohup mvn -q clean package -DskipTests > /tmp/btr-build.log 2>&1 & echo BUILD_STARTED")
print("构建已启动，等待完成...")

# 4. 轮询 jar 时间戳（最多 240s）
import datetime
def jar_mtime():
    out, _ = run("stat -c %Y /root/BRT/target/btr-platform.jar 2>/dev/null")
    return out
old = jar_mtime()
deadline = time.time() + 240
ok = False
while time.time() < deadline:
    time.sleep(10)
    cur = jar_mtime()
    if cur and cur != old:
        ok = True
        print(f"新 jar 已生成 (mtime={cur})")
        break
    # 构建失败快速退出
    out, _ = run("grep -c 'BUILD FAILURE' /tmp/btr-build.log 2>/dev/null")
    if out and out != "0":
        fail, _ = run("tail -30 /tmp/btr-build.log")
        print("!!! BUILD FAILURE\n" + fail)
        ssh.close()
        raise SystemExit(1)
if not ok:
    tail, _ = run("tail -40 /tmp/btr-build.log")
    print("!!! 构建超时\n" + tail)
    ssh.close()
    raise SystemExit(1)

# 5. 重启服务
out, err = run("systemctl daemon-reload && systemctl restart btr && sleep 8 && systemctl is-active btr")
print("=== 服务状态 ===\n" + (out or err))

# 6. 验证：监听地址 + 本机健康 + 外网入口
out, _ = run("ss -ltnp | grep 8082")
print("=== 8082 监听 ===\n" + (out or "NO LISTEN"))
out, _ = run("curl -s -o /dev/null -w 'local8082:%{http_code}' -m 5 http://127.0.0.1:8082/api/v1/health")
print("=== 本机健康 ===\n" + (out or "FAIL"))
out, _ = run("curl -s -o /dev/null -w 'local8080:%{http_code}' -m 5 http://127.0.0.1:8080/api/v1/health")
print("=== nginx 健康 ===\n" + (out or "FAIL"))
ssh.close()
print("DONE")
