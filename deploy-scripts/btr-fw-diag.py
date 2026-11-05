# -*- coding: utf-8 -*-
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
cmds = [
    ("iptables INPUT", "iptables -L INPUT -n --line-numbers 2>/dev/null | head -20"),
    ("ufw", "ufw status 2>/dev/null | head -5"),
    ("nft", "nft list ruleset 2>/dev/null | head -20"),
    ("firewalld", "firewall-cmd --state 2>/dev/null"),
    ("cloud init?", "ls /etc/iptables* 2>/dev/null; ls /etc/sysconfig/iptables 2>/dev/null"),
]
for name, cmd in cmds:
    i, o, e = cli.exec_command(cmd, timeout=30)
    out = o.read().decode("utf-8", "replace").strip()
    print("===", name, "===")
    print(out[:1500])
cli.close()
