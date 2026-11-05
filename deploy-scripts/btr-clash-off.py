# -*- coding: utf-8 -*-
"""关闭服务器 clash 代理 + 清理 git 代理配置"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)


def run(cmd, timeout=90):
    stdin, stdout, stderr = ssh.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode("utf-8", "replace").strip()
    err = stderr.read().decode("utf-8", "replace").strip()
    code = stdout.channel.recv_exit_status()
    print("$", cmd[:150])
    if out.strip():
        print(out[-2500:])
    if err.strip():
        print("ERR:", err[-600:])
    print("[exit=%s]" % code)
    return code


# 1. 找到 clash 相关服务
run("systemctl list-units --all --no-pager | grep -iE 'clash|mihomo' || echo NO_SVC")

# 2. 停止并禁用
run("systemctl stop clash 2>&1; systemctl disable clash 2>&1; echo STOPPED")

# 3. 确认停止 & 端口释放
run("systemctl is-active clash 2>&1 || true")
run("ss -ltnp | grep 7890 || echo PORT_7890_FREE")

# 4. 清理 git 全局代理
run("git config --global --get-regexp 'http\\..*proxy|https\\..*proxy' || echo NO_GIT_PROXY")
run("git config --global --unset-all http.proxy; git config --global --unset-all https.proxy; echo GIT_PROXY_CLEARED")

# 5. 测试 git 直连 GitHub（短超时）
run("timeout 20 git ls-remote https://github.com/dong121314/btr-platform.git HEAD 2>&1 | tail -3 || echo GIT_DIRECT_FAIL")

ssh.close()
print("DONE")
