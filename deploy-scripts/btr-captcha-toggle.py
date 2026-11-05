# -*- coding: utf-8 -*-
"""临时开关登录验证码（key: switch.captcha_enabled）"""
import paramiko, sys

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
val = sys.argv[1] if len(sys.argv) > 1 else "false"
sql = "UPDATE sys_config SET config_value='" + val + "' WHERE config_key='switch.captcha_enabled';"
sql += "SELECT config_key, config_value FROM sys_config WHERE config_key='switch.captcha_enabled';"
cmd = "mysql -ubtr -pbtr_pass btr -t -e \"" + sql + "\" 2>/dev/null"
stdin, stdout, stderr = cli.exec_command(cmd, timeout=60)
print(stdout.read().decode("utf-8", "replace").strip())
cli.close()
