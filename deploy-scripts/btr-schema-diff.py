# -*- coding: utf-8 -*-
"""对比所有实体字段与数据库列"""
import paramiko, re

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

# 实体文件清单
entities = run("ls /root/BRT/src/main/java/com/btr/platform/entity/").split()
print("实体:", entities)
for ent in entities:
    name = ent.replace(".java", "")
    # 提取字段名（private 类型 字段;）
    src = run(f"cat /root/BRT/src/main/java/com/btr/platform/entity/{ent}")
    fields = []
    for line in src.splitlines():
        m = re.match(r"\s*private\s+[\w<>,\s\[\]]+\s+(\w+)\s*;", line)
        if m:
            fields.append(m.group(1))
    # 表列
    tbl = re.sub(r'(?<=[a-z0-9])(?=[A-Z])', '_', name).lower()
    cols = run(f"mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -N -e \"SHOW COLUMNS FROM {tbl}\" 2>/dev/null | awk '{{print $1}}'").split()
    # 实体字段转下划线
    f_ = [re.sub(r'(?<=[a-z0-9])(?=[A-Z])', '_', f).lower() for f in fields]
    missing = [x for x in f_ if x not in cols]
    if missing:
        print(f"!! {tbl} 缺列: {missing}")
    else:
        print(f"OK {tbl} ({len(cols)}列)")
ssh.close()
print("DONE")
