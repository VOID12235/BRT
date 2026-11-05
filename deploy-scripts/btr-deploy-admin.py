# -*- coding: utf-8 -*-
"""部署管理后台：admin-index.html + 全部 admin/*.html + admin.js + echarts.min.js"""
import paramiko, os, stat

HOST, PORT, USER, PW = "103.236.92.193", 45185, "root", "Q6Cq54BgvHp8"
fe = r"D:\Users\a3614\Documents\默认项目\frontend"

files = []
for name in os.listdir(os.path.join(fe, "admin")):
    if name.endswith(".html"):
        files.append((os.path.join(fe, "admin", name), "/var/www/btr/admin/" + name))
files += [
    (os.path.join(fe, "assets", "js", "admin.js"), "/var/www/btr/assets/js/admin.js"),
    (os.path.join(fe, "assets", "js", "echarts.min.js"), "/var/www/btr/assets/js/echarts.min.js"),
    (os.path.join(fe, "assets", "js", "wangeditor.min.js"), "/var/www/btr/assets/js/wangeditor.min.js"),
]

t = paramiko.Transport((HOST, PORT))
t.connect(username=USER, password=PW)
sftp = paramiko.SFTPClient.from_transport(t)
ok = 0
for local, remote in files:
    sftp.put(local, remote)
    ok += 1
    print("UP", os.path.basename(local), "->", remote)
# 校验
print("== server check ==")
for local, remote in files:
    try:
        size = sftp.stat(remote).st_size
        local_size = os.path.getsize(local)
        print("OK" if size == local_size else "MISMATCH", remote, size)
    except Exception as e:
        print("ERR", remote, e)
sftp.close()
t.close()
print("uploaded:", ok)
