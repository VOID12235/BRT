# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 服务器端统一替换 ==")
print(run("grep -rl 'api.js?v=20261105a2' /var/www/btr --include='*.html' | xargs -r sed -i 's#api.js?v=20261105a2#api.js?v=20261105a3#g' && echo replaced"))
print("== 残留检查 ==")
print(run("grep -r -c 'api.js?v=20261105a2' /var/www/btr --include='*.html' | grep -v ':0' || echo none-left"))
print("== 新版本分布 ==")
print(run("grep -r -h -o 'api.js?v=[0-9a-z]*' /var/www/btr --include='*.html' | sort | uniq -c"))
ssh.close()
print("DONE")
