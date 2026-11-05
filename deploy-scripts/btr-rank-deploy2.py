# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sftp = ssh.open_sftp()
sftp.put(r"D:\Users\a3614\Documents\默认项目\frontend\assets\js\rank.js", "/var/www/btr/assets/js/rank.js")
sftp.put(r"D:\Users\a3614\Documents\默认项目\frontend\rank.html", "/var/www/btr/rank.html")
sftp.close()

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print(run("grep -o 'rank.js?v=[a-z0-9]*' /var/www/btr/rank.html"))
print("size:", run("wc -c < /var/www/btr/assets/js/rank.js"))
ssh.close()
print("DONE")
