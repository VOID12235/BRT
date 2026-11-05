# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== FileController 路由 ==")
print(run("grep -n 'Mapping\\|download\\|Path\\|md5\\|uploads' /root/BRT/src/main/java/com/btr/platform/controller/FileController.java | head -25"))
print("== FileService 读取逻辑 ==")
print(run("grep -n 'uploads\\|Paths\\|resolve\\|download\\|Content-Disposition' /root/BRT/src/main/java/com/btr/platform/service/FileService.java 2>/dev/null | head -15"))
print("== 上传目录配置 ==")
print(run("grep -rn 'uploads\\|upload' /root/BRT/src/main/resources/application.yml /root/BRT/src/main/resources/application-prod.yml 2>/dev/null | head -8"))
ssh.close()
print("DONE")
