# -*- coding: utf-8 -*-
"""对比所有实体字段与数据库列（正确表名）"""
import paramiko, re

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

MAP = {"Award": "contest_award", "EmailRecord": "sys_email_record", "JudgeAssign": "contest_judge_assign",
       "Score": "contest_score", "Signup": "contest_signup", "User": "sys_user",
       "Winner": "contest_winner", "Work": "contest_work", "Contest": "contest",
       "Notice": "notice", "NoticeRead": "notice_read", "SysConfig": "sys_config",
       "SysLog": "sys_log", "UploadFile": "upload_file"}

for ent, tbl in MAP.items():
    src = run(f"cat /root/BRT/src/main/java/com/btr/platform/entity/{ent}.java")
    fields = []
    for line in src.splitlines():
        m = re.match(r"\s*private\s+[\w<>,\s\[\]]+\s+(\w+)\s*;", line)
        if m:
            fields.append(m.group(1))
    cols = run(f"mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -N -e \"SHOW COLUMNS FROM {tbl}\" 2>&1 | awk '{{print $1}}'").split()
    if not cols or "ERROR" in " ".join(cols):
        print(f"?? {tbl}: 表查询失败 {cols[:3]}")
        continue
    f_ = [re.sub(r'(?<=[a-z0-9])(?=[A-Z])', '_', f).lower() for f in fields]
    missing = [x for x in f_ if x not in cols]
    print(f"{'!!' if missing else 'OK'} {tbl} 缺列: {missing if missing else '无'}")
ssh.close()
print("DONE")
