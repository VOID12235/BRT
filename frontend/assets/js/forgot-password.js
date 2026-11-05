/* 忘记密码页交互（独立文件以满足严格 CSP） */
(function () {
  var B = window.BTR;
  var sendBtn = document.getElementById("fpSendBtn");
  var countdown = 0;

  sendBtn.addEventListener("click", async function () {
    var email = document.getElementById("fpEmail").value.trim();
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      B.toast("请输入正确的邮箱地址", "warn");
      return;
    }
    sendBtn.disabled = true;
    sendBtn.textContent = "发送中…";
    try {
      var data = await B.api.post("/auth/forgot-password", { email: email });
      B.toast((data && data.message) || "验证码已发送，请查收邮件", "success");
      countdown = 60;
      sendBtn.disabled = true;
      sendBtn.textContent = countdown + "s 后重发";
      var timer = setInterval(function () {
        countdown--;
        if (countdown <= 0) {
          clearInterval(timer);
          sendBtn.disabled = false;
          sendBtn.textContent = "获取验证码";
        } else {
          sendBtn.textContent = countdown + "s 后重发";
        }
      }, 1000);
    } catch (e) {
      B.toast(e.message || "发送失败", "error");
      sendBtn.disabled = false;
      sendBtn.textContent = "获取验证码";
    }
  });

  document.getElementById("forgotForm").addEventListener("submit", async function (e) {
    e.preventDefault();
    var email = document.getElementById("fpEmail").value.trim();
    var code = document.getElementById("fpCode").value.trim();
    var pwd = document.getElementById("fpPassword").value;
    var confirm = document.getElementById("fpConfirm").value;
    if (pwd.length < 6 || pwd.length > 32) {
      B.toast("密码长度需为 6-32 位", "warn");
      return;
    }
    if (pwd !== confirm) {
      B.toast("两次输入的密码不一致", "warn");
      return;
    }
    var submit = document.getElementById("fpSubmit");
    submit.disabled = true;
    submit.textContent = "提交中…";
    try {
      await B.api.post("/auth/reset-password", { email: email, code: code, newPassword: pwd });
      B.toast("密码已重置，请使用新密码登录", "success");
      setTimeout(function () {
        location.href = "/login.html";
      }, 1200);
    } catch (err) {
      B.toast(err.message || "重置失败", "error");
      submit.disabled = false;
      submit.textContent = "重置密码";
    }
  });
})();
