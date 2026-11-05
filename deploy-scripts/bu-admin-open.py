# -*- coding: utf-8 -*-
import seed_browser_use as bu

tok = open(r"D:\Users\a3614\Documents\默认项目\.preview\token.txt").read().strip()
bu.navigate("http://103.236.92.193:20757/admin/contest-manage.html")
bu.wait_for_load()
bu.js('localStorage.setItem("btr_access_token", "' + tok + '"); localStorage.setItem("btr_user", JSON.stringify({account:"admin",role:"ADMIN",nickname:"\u7ba1\u7406\u5458"}));')
bu.navigate("reload")
bu.wait_for_load(timeout=15)
frame = bu.screenshot()
print(frame.path)
