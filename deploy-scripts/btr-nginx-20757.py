# -*- coding: utf-8 -*-
import paramiko
import time

CONF = """server {
    listen 20757;
    access_log /var/log/nginx/access.log btr;
    listen [::]:20757;
    root /var/www/btr;
    index index.html;
    server_name _;
    client_max_body_size 60m;
    location /api/ {
        proxy_pass http://127.0.0.1:8082;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    location ~* \\.html$ {
        add_header Cache-Control "no-cache";
        try_files $uri $uri/ =404;
    }
    location ~* \\.(css|js)$ {
        expires 1d;
        add_header Cache-Control "public, max-age=86400";
    }
    location ~* \\.(svg|png|jpg|jpeg|gif|ico|webp|woff2?|ttf|eot|mp4)$ {
        expires 7d;
        add_header Cache-Control "public, max-age=604800";
    }
    location / { try_files $uri $uri/ =404; }
}
"""

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sftp = cli.open_sftp()
with sftp.open("/etc/nginx/sites-available/btr-20757", "w") as f:
    f.write(CONF)
sftp.close()
i, o, e = cli.exec_command("ln -sf /etc/nginx/sites-available/btr-20757 /etc/nginx/sites-enabled/btr-20757 && nginx -t 2>&1 | tail -2", timeout=30)
print("TEST:", o.read().decode("utf-8", "replace").strip())
i, o, e = cli.exec_command("systemctl reload nginx && echo RELOADED", timeout=30)
print("RELOAD:", o.read().decode("utf-8", "replace").strip())
time.sleep(2)
for name, cmd in [
    ("20757 static", "curl -s -o /dev/null -w '%{http_code} %{time_total}s' http://127.0.0.1:20757/m/index.html"),
    ("20757 health", "curl -s -o /dev/null -w '%{http_code} %{time_total}s' http://127.0.0.1:20757/api/v1/health"),
    ("20757 captcha", "curl -s -o /dev/null -w '%{http_code} %{time_total}s' http://127.0.0.1:20757/api/v1/auth/captcha"),
    ("listen", "ss -tlnp | grep 20757"),
]:
    i, o, e = cli.exec_command(cmd, timeout=30)
    print(name, "=>", o.read().decode("utf-8", "replace").strip())
cli.close()
