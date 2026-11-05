# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== nginx 主配置 ==")
print(run("cat /etc/nginx/nginx.conf 2>/dev/null | grep -n 'include\\|server_name\\|listen' | head -10"))
print(run("ls /etc/nginx/conf.d/ 2>/dev/null; ls /etc/nginx/sites-enabled/ 2>/dev/null"))
print(run("for f in /etc/nginx/conf.d/*.conf /etc/nginx/sites-enabled/*; do [ -f \"$f\" ] && echo \"--- $f\" && cat \"$f\" | grep -vE '^\\s*#' | head -40; done 2>/dev/null"))
ssh.close()
print("DONE")
