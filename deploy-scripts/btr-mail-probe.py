# -*- coding: utf-8 -*-
"""本地直测 163 SMTP 认证（不落盘明文）"""
import smtplib

try:
    s = smtplib.SMTP_SSL("smtp.163.com", 465, timeout=15)
    s.login("a36143614u@163.com", "DMfL434EAkmarpZW")
    print("AUTH_OK")
    s.quit()
except Exception as e:
    print("AUTH_FAIL:", type(e).__name__, str(e)[:200])
