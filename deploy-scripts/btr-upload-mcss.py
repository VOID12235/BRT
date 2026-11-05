# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sftp = ssh.open_sftp()
sftp.put(r"D:\Users\a3614\Documents\默认项目\frontend\m\m.js", "/var/www/btr/m/m.js")
sftp.put(r"D:\Users\a3614\Documents\默认项目\frontend\m\m.css", "/var/www/btr/m/m.css")
sftp.close()

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("m.js prizeText:", run("grep -c 'prizeText' /var/www/btr/m/m.js"))
print("m.css prize-box:", run("grep -c 'prize-box' /var/www/btr/m/m.css"))
ssh.close()
print("DONE")
