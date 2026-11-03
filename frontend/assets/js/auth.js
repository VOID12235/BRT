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
    const account = document.getElementById("regAccount").value.trim();
    const nickname = document.getElementById("regNickname").value.trim();
    const email = document.getElementById("regEmail").value.trim();
    const password = document.getElementById("regPassword").value;
    const confirm = document.getElementById("regConfirm").value;
    if (!account || !nickname || !password) {
      B.toast("请填写账号、昵称和密码", "warn");
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
      const data = await B.api.post("/auth/register", { account, nickname, email, password });
      B.storage.save(data);
      B.toast("注册成功，正在进入个人中心…", "success", 1200);
      setTimeout(() => {
        location.href = B.qs("redirect") || "/user-center.html";
      }, 400);
    } catch (err) {
      B.toast(err.message || "注册失败", "error");
      btn.disabled = false;
      btn.textContent = "注册并登录";
    }
  }

  document.addEventListener("DOMContentLoaded", init);
})();
