# -*- coding: utf-8 -*-
"""全量同步本机 Java 源码到服务器并重新构建"""
import os
import paramiko

HOST = "103.236.92.193"
PORT = 45185
USER = "root"
PWD = "Q6Cq54BgvHp8"
ROOT = r"D:\Users\a3614\Documents\默认项目"

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(HOST, port=PORT, username=USER, password=PWD, timeout=30)
sftp = ssh.open_sftp()

# 同步 src/main/java 全部 .java 与 src/main/resources 全部文件
count = 0
for base_rel in ["src/main/java", "src/main/resources"]:
    local_base = os.path.join(ROOT, base_rel)
    remote_base = "/root/BRT/" + base_rel
    for dirpath, dirnames, filenames in os.walk(local_base):
        for fn in filenames:
            if not (fn.endswith(".java") or fn.endswith(".yml") or fn.endswith(".yaml")
                    or fn.endswith(".properties") or fn.endswith(".xml")):
                continue
            local = os.path.join(dirpath, fn)
            rel = os.path.relpath(local, local_base).replace("\\", "/")
            remote = remote_base + "/" + rel
            try:
                sftp.stat(os.path.dirname(remote))
            except IOError:
                parts = rel.split("/")[:-1]
                cur = remote_base
                for p in parts:
                    cur += "/" + p
                    try:
                        sftp.stat(cur)
                    except IOError:
                        sftp.mkdir(cur)
            sftp.put(local, remote)
            count += 1
print("SYNCED", count, "files")

sftp.close()

def run(cmd, timeout=900):
    stdin, stdout, stderr = ssh.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode("utf-8", "replace")
    err = stderr.read().decode("utf-8", "replace")
    code = stdout.channel.recv_exit_status()
    print("$", cmd[:150])
    if out.strip():
        print(out[-4000:])
    if err.strip():
        print("ERR:", err[-2000:])
    print("[exit=%s]" % code)
    return code

run("cd /root/BRT && mvn -q clean package -DskipTests 2>&1 | tail -30", timeout=900)
run("ls -la /root/BRT/target/*.jar")
run("grep -n 'proxy_pass' /etc/nginx/sites-enabled/btr | head -10")
run("systemctl restart btr && sleep 10 && systemctl is-active btr && systemctl status btr --no-pager | head -14")
run("curl -s -o /dev/null -w 'API8082:%{http_code}\n' http://127.0.0.1:8082/api/v1/auth/captcha; curl -s -o /dev/null -w 'WEB20757:%{http_code}\n' http://127.0.0.1:20757/", timeout=60)
run("ss -tlnp | grep -E '8080|8082|20757' | head -10")
ssh.close()
print("ALL_DONE")
