/* 移动端登录页交互（独立文件以满足严格 CSP） */
(function () {
  document.querySelectorAll(".eye").forEach(function (eye) {
    eye.addEventListener("click", function () {
      var input = document.getElementById(eye.dataset.eye);
      if (!input) return;
      input.type = input.type === "password" ? "text" : "password";
      eye.style.color = input.type === "text" ? "var(--brand)" : "var(--muted)";
    });
  });
})();
