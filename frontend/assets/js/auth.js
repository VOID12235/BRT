(function () {
  const B = window.BTR;
  let captchaEnabled = false;

  function init() {
    const user = B.storage.user;
    if (user && B.storage.token && !B.qs("switch")) {
      location.replace(B.homeForRole(user.role));
      return;
    }

    document.querySelectorAll("[data-auth-tab]").forEach((btn) => {
      btn.addEventListener("click", () => switchTab(btn.dataset.authTab));
    });

    document.querySelectorAll(".demo-accounts button").forEach((btn) => {
      btn.addEventListener("click", () => {
        document.getElementById("loginAccount").value = btn.dataset.account;
        document.getElementById("loginPassword").value = btn.dataset.password;
        document.getElementById("captchaCode").focus();
        B.toast("已填入演示账号，点击登录即可", "info");
      });
    });

    document.getElementById("loginForm").addEventListener("submit", login);
    document.getElementById("registerForm").addEventListener("submit", register);
    document.getElementById("sendRegCodeBtn").addEventListener("click", sendRegisterCode);
    loadCaptcha();
  }

  function switchTab(tab) {
    document.querySelectorAll("[data-auth-tab]").forEach((b) =>
      b.classList.toggle("is-active", b.dataset.authTab === tab)
    );
    document.getElementById("loginPanel").classList.toggle("hidden", tab !== "login");
    document.getElementById("registerPanel").classList.toggle("hidden", tab !== "register");
  }

  async function loadCaptcha() {
    try {
      const data = await B.api.get("/auth/captcha");
      captchaEnabled = Boolean(data.enabled);
      const field = document.getElementById("captchaField");
      field.classList.toggle("hidden", !captchaEnabled);
      if (captchaEnabled) {
        const img = document.getElementById("captchaImg");
        img.src = data.image;
        img.dataset.key = data.key;
        img.onclick = loadCaptcha;
      }
    } catch (e) {
      captchaEnabled = false;
    }
  }

  async function login(e) {
    e.preventDefault();
    const account = document.getElementById("loginAccount").value.trim();
    const password = document.getElementById("loginPassword").value;
    if (!account || !password) {
      B.toast("请输入账号和密码", "warn");
      return;
    }
    const btn = document.getElementById("loginSubmit");
    btn.disabled = true;
    btn.textContent = "登录中…";
    try {
      const body = { account, password };
      if (captchaEnabled) {
        body.captchaKey = document.getElementById("captchaImg").dataset.key;
        body.captchaCode = document.getElementById("captchaCode").value.trim();
      }
      const data = await B.api.post("/auth/login", body);
      B.storage.save(data);
      B.toast("登录成功，正在进入…", "success", 1200);
      const redirect = B.qs("redirect");
      setTimeout(() => {
        location.href = redirect || B.homeForRole(data.user.role);
      }, 350);
    } catch (err) {
      B.toast(err.message || "登录失败", "error");
      if (captchaEnabled) loadCaptcha();
      btn.disabled = false;
      btn.textContent = "登录";
    }
  }

  async function register(e) {
    e.preventDefault();
    const email = document.getElementById("regEmail").value.trim();
    const nickname = document.getElementById("regNickname").value.trim();
    const code = document.getElementById("regCode").value.trim();
    const password = document.getElementById("regPassword").value;
    const confirm = document.getElementById("regConfirm").value;
    if (!email || !nickname || !code || !password) {
      B.toast("请填写邮箱、昵称、验证码和密码", "warn");
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      B.toast("邮箱格式不正确", "warn");
      return;
    }
    if (password.length < 6) {
      B.toast("密码至少 6 位", "warn");
      return;
    }
    if (password !== confirm) {
      B.toast("两次输入的密码不一致", "warn");
      return;
    }
    const btn = document.getElementById("registerSubmit");
    btn.disabled = true;
    btn.textContent = "注册中…";
    try {
      const data = await B.api.post("/auth/register", { email, code, password, nickname });
      B.storage.save(data);
      B.toast("注册成功！登录账号即邮箱 " + email + "，正在进入…", "success", 2600);
      setTimeout(() => {
        location.href = B.qs("redirect") || "/user-center.html";
      }, 500);
    } catch (err) {
      B.toast(err.message || "注册失败", "error");
      btn.disabled = false;
      btn.textContent = "注册并登录";
    }
  }

  async function sendRegisterCode() {
    const email = document.getElementById("regEmail").value.trim();
    if (!email) {
      B.toast("请先填写邮箱", "warn");
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      B.toast("邮箱格式不正确", "warn");
      return;
    }
    const btn = document.getElementById("sendRegCodeBtn");
    btn.disabled = true;
    try {
      const data = await B.api.post("/auth/send-register-code", { email });
      B.toast(data.message || "验证码已发送，请查收邮件", "success");
      countdown(btn, 60);
    } catch (err) {
      B.toast(err.message || "发送失败", "error");
      btn.disabled = false;
    }
  }

  function countdown(btn, seconds) {
    const origin = btn.textContent;
    btn.textContent = seconds + "s 后重发";
    const timer = setInterval(() => {
      seconds--;
      if (seconds <= 0) {
        clearInterval(timer);
        btn.disabled = false;
        btn.textContent = origin;
      } else {
        btn.textContent = seconds + "s 后重发";
      }
    }, 1000);
  }

  document.addEventListener("DOMContentLoaded", init);
})();
