# -*- coding: utf-8 -*-
"""服务器 git 提交手机端满血版"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
i, o, e = ssh.exec_command(
    "cd /root/BRT && git add -A && git commit -m 'feat(m): 手机端满血版——新增忘记密码/作品集/评委工作台/管理后台页面，修复登录验证码细节与版本号统一' 2>&1 | tail -3", timeout=60)
print(o.read().decode("utf-8", "replace").strip())
ssh.close()
print("DONE")
