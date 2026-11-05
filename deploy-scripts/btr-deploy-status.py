# -*- coding: utf-8 -*-
"""部署 api.js + 公共 css（JUDGING 状态徽标）"""
import paramiko, os

FE = r"D:\Users\a3614\Documents\默认项目\frontend"
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sftp = ssh.open_sftp()
files = [
    (os.path.join(FE, "assets", "js", "api.js"), "/var/www/btr/assets/js/api.js"),
    (os.path.join(FE, "assets", "css", "base.css"), "/var/www/btr/assets/css/base.css"),
    (os.path.join(FE, "assets", "css", "components.css"), "/var/www/btr/assets/css/components.css"),
    (os.path.join(FE, "m", "m.css"), "/var/www/btr/m/m.css"),
]
for lp, rp in files:
    sftp.put(lp, rp)
    print("OK", os.path.basename(lp))
sftp.close()
ssh.close()
