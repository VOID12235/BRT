# -*- coding: utf-8 -*-
import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
def run(c, timeout=30):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()
print(run("mysql -ubtr -pbtr_pass btr -e \"DELETE FROM upload_file WHERE id IN (8,9);\" 2>&1"))
print(run("rm -f /root/BRT/uploads/2cd8bde463f5d82aae0f0cec061d6b8f.png /root/BRT/uploads/a57204d29953e172d6b681aee2da017d.txt; echo cleaned"))
ssh.close()
print("DONE")
