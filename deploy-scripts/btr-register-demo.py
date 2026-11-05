# -*- coding: utf-8 -*-
"""上传 demo PDF 到服务器并注册/关联"""
import os, paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sftp = ssh.open_sftp()

OUT = r"D:\Users\a3614\Documents\默认项目\demo_works"
# (work_id, 文件名md5, original_name, uploader_id)
WORKS = [
    ("6", "4ccc27b9cad9719a62dda2a2fb3c0aaa", "把夏天留在这里-作品文件.pdf", "4"),
    ("7", "a0b6b54611c8114c08e9a0da1957334b", "潮汐来信-作品文件.pdf", "5"),
    ("8", "097108a5ef6b08a481d861f37f0a6dfd", "北纬三十一度-作品文件.pdf", "6"),
    ("9", "e394045029089b91959cccaef79744f3", "慢下来-城市公共座椅计划-作品文件.pdf", "7"),
    ("10", "735e1b42ab0699f40f3caaed22bea448", "纸上的风-作品文件.pdf", "5"),
]

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

# 1. 上传物理文件 uploads/{md5}.pdf
for wid, md5, oname, uid in WORKS:
    lp = os.path.join(OUT, f"work{wid}.pdf")
    rp = f"/root/BRT/uploads/{md5}.pdf"
    sftp.put(lp, rp)
    sz = os.path.getsize(lp)
    # 2. 插 upload_file 记录
    esc_oname = oname.replace("'", "''")
    run(f'mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e "INSERT INTO upload_file (url, md5, size, original_name, uploader_id, purpose, created_at) VALUES (\'/api/v1/files/pending\', \'{md5}\', {sz}, \'{esc_oname}\', {uid}, \'WORK\', NOW())"')
    fid = run(f'mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -N -e "SELECT id FROM upload_file WHERE md5=\'{md5}\' ORDER BY id DESC LIMIT 1"')
    # 3. 更新 url 与 work.file_url
    url = f"/api/v1/files/{fid}/{md5}"
    run(f'mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e "UPDATE upload_file SET url=\'{url}\' WHERE id={fid}; UPDATE contest_work SET file_url=\'{url}\' WHERE id={wid}"')
    print(f"work{wid}: upload_file#{fid} url={url}")

sftp.close()
ssh.close()
print("DONE")
