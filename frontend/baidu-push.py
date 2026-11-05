#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
BTR 赛事平台 - 百度收录主动推送脚本
=====================================
用法：
  1. 登录百度搜索资源平台 https://ziyuan.baidu.com ，添加站点（http://103.236.92.193:20757）
     并完成验证（推荐 CNAME 或文件验证，文件验证可把验证 html 放到 /var/www/btr/ 下）。
  2. 在"普通收录 -> 主动推送"中获取推送 token，填入下方 TOKEN。
  3. 服务器执行：python3 /root/btr-seo/baidu-push.py
     （可加入 crontab：0 6 * * * python3 /root/btr-seo/baidu-push.py >> /root/btr-seo/push.log 2>&1）

说明：百度是否收录由百度算法决定，本脚本只负责把 URL 提交给百度，加速收录。
"""
import json
import sys
import urllib.request

# ===== 请填写：百度站长平台主动推送 token =====
TOKEN = "在此填写百度站长平台的推送token"

API = "http://data.zz.baidu.com/urls?site=http://103.236.92.193:20757&token=" + TOKEN

URLS = [
    "http://103.236.92.193:20757/",
    "http://103.236.92.193:20757/contest-list.html",
    "http://103.236.92.193:20757/contest-detail.html?id=1",
    "http://103.236.92.193:20757/contest-detail.html?id=2",
    "http://103.236.92.193:20757/contest-detail.html?id=3",
    "http://103.236.92.193:20757/contest-detail.html?id=4",
    "http://103.236.92.193:20757/contest-detail.html?id=6",
    "http://103.236.92.193:20757/rank.html",
    "http://103.236.92.193:20757/notices.html",
    "http://103.236.92.193:20757/portfolio.html",
    "http://103.236.92.193:20757/signup.html",
    "http://103.236.92.193:20757/work-submit.html",
    "http://103.236.92.193:20757/about.html",
]


def main():
    if "在此填写" in TOKEN:
        print("[-] 请先在 ziyuan.baidu.com 获取推送 token 并填入脚本 TOKEN 字段。")
        sys.exit(1)
    data = "\n".join(URLS).encode("utf-8")
    req = urllib.request.Request(API, data=data, method="POST")
    req.add_header("Content-Type", "text/plain")
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            body = resp.read().decode("utf-8")
            print("[+] 百度推送响应：", body)
            result = json.loads(body)
            if result.get("success"):
                print("[+] 推送成功 {} 条，今日剩余配额 {}".format(
                    result.get("success"), result.get("remain")))
            else:
                print("[-] 推送失败：", result)
    except Exception as e:
        print("[-] 推送异常：", e)
        sys.exit(1)


if __name__ == "__main__":
    main()
