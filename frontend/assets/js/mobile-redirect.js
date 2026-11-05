/* 移动端跳转：独立文件以满足严格 CSP（禁止内联脚本） */
(function () {
  try {
    var ua = navigator.userAgent;
    var isMobile = /Android|iPhone|iPod|Windows Phone|Mobile/i.test(ua);
    var wantDesktop = /desktop/i.test(location.search) || /desktop/i.test(location.hash);
    if (isMobile && !wantDesktop) location.replace("/m/index.html");
  } catch (e) {
    /* ignore */
  }
})();
