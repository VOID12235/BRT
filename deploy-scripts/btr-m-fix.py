# -*- coding: utf-8 -*-
"""上传漏传的 mobile-redirect.js 与 btr-logo.svg 并验证"""
import paramiko

ROOT = r"D:\Users\a3614\Documents\默认项目\frontend"
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sftp = ssh.open_sftp()
sftp.put(ROOT + r"\assets\js\mobile-redirect.js", "/var/www/btr/assets/js/mobile-redirect.js")
sftp.put(ROOT + r"\assets\img\btr-logo.svg", "/var/www/btr/assets/img/btr-logo.svg")
print("PUT OK")
sftp.close()


def run(cmd, timeout=90):
    stdin, stdout, stderr = ssh.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode("utf-8", "replace").strip()
    err = stderr.read().decode("utf-8", "replace").strip()
    print("$", cmd[:120])
    if out.strip():
        print(out[-1200:])
    if err.strip():
        print("ERR:", err[-400:])
    print("---")


# 验证文件到位
run("ls -l /var/www/btr/assets/js/mobile-redirect.js /var/www/btr/assets/img/btr-logo.svg")

# 外网访问验证
run("curl -s -m 20 -o /dev/null -w 'JS:%{http_code}\\n' http://btr.navitaire.club:20757/assets/js/mobile-redirect.js")
run("curl -s -m 20 -o /dev/null -w 'LOGO:%{http_code}\\n' http://btr.navitaire.club:20757/assets/img/btr-logo.svg")

# 模拟手机 UA 访问首页，验证是否跳转 m/index.html
run("""curl -s -m 20 -A 'Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148' -o /dev/null -w 'MOBILE-HOME:%{http_code} REDIRECT:%{redirect_url}\\n' http://btr.navitaire.club:20757/""")

ssh.close()
print("DONE")
