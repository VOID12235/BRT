# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== nginx 配置 ==")
print(run("grep -rn 'uploads\\|root\\|proxy_pass' /etc/nginx/conf.d/ /etc/nginx/sites-enabled/ 2>/dev/null | head -20"))
print("== upload_file 表结构 ==")
print(run('mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -N -e "SHOW COLUMNS FROM upload_file" | awk \'{print $1, $2}\''))
print("== 服务器 uploads 目录 ==")
print(run("ls /root/BRT/uploads/ 2>/dev/null | head; ls /var/www/btr/uploads 2>/dev/null | head"))
ssh.close()
print("DONE")
