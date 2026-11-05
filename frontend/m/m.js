/* ==========================================================================
   BTR 赛事平台 · 移动端公共脚本（/m/）
   ========================================================================== */
(function () {
  const B = window.BTR;
  const page = document.body.dataset.page || "";
  const esc = B.escapeHtml;

  /* ---------- App 内环境：隐藏 APP 安装入口（网页版保留） ---------- */
  (function () {
    try {
      const inApp = /BTRApp/i.test(navigator.userAgent);
      if (inApp) {
        const bars = document.querySelectorAll(".install-bar");
        bars.forEach((el) => el.remove());
      }
    } catch (e) { /* 忽略 */ }
  })();

  /** 文件 URL 转为下载 URL：/api/v1/files/{id} -> /api/v1/files/{id}/download */
  function downloadFile(url) {
    if (!url) return "";
    if (/\/download$/i.test(url)) return url;
    return url.replace(/\/files\/([^/]+(?:\/[^/]+)?)$/, "/files/$1/download");
  }

  /** 奖品/奖金文本（不显示奖项名称，显示具体奖品与奖金） */
  function prizeText(row) {
    const parts = [];
    if (row.prizeAmount != null && row.prizeAmount !== "" && Number(row.prizeAmount) > 0) {
      parts.push("奖金 ¥" + Number(row.prizeAmount).toLocaleString("zh-CN"));
    }
    if (row.gift) parts.push("奖品 " + row.gift);
    return parts.join(" · ");
  }

  /** 在线预览：图片/PDF/音视频可在浏览器直接预览，其他类型弹窗提示不可预览 */
  function previewWork(btn) {
    const url = btn && btn.getAttribute("data-preview");
    const name = (btn && btn.getAttribute("data-name")) || "";
    const ext = name.split(".").pop().toLowerCase();
    const ok = ["jpg", "jpeg", "png", "gif", "webp", "svg", "bmp", "pdf",
      "mp4", "webm", "ogg", "mov", "mp3", "wav", "m4a", "aac", "txt"].includes(ext);
    if (url && ok) {
      window.open(url, "_blank", "noopener");
    } else {
      alert("此文件不可预览");
    }
  }
  window.previewWork = previewWork;

  /* ---------- Toast ---------- */
  let toastWrap = null;
  function toast(msg) {
    if (!toastWrap) {
      toastWrap = document.createElement("div");
      toastWrap.className = "toast-wrap";
      document.body.appendChild(toastWrap);
    }
    const t = document.createElement("div");
    t.className = "toast";
    t.textContent = msg;
    toastWrap.appendChild(t);
    setTimeout(() => {
      t.style.opacity = "0";
      t.style.transition = "opacity .3s ease";
      setTimeout(() => t.remove(), 320);
    }, 2200);
  }

  /* ---------- 通用 ---------- */
  function markTab() {
    const map = { index: "home", contests: "contests", rank: "rank", center: "mine" };
    const a = document.querySelector('.m-tabbar a[data-tab="' + map[page] + '"]');
    if (a) a.classList.add("is-active");
  }

  function statusBadge(status) {
    return B.statusBadge(status);
  }

  function contestItem(c, opts) {
    const cover = c.cover || "/assets/img/cover-1.svg";
    const info = B.statusInfo(c.status);
    const prize = Number(c.prizeAmount || 0);
    const mode = c.mode === "OFFLINE" ? '<span class="badge badge-plain">线下</span>' : '<span class="badge badge-plain">线上</span>';
    const prizeHtml =
      prize > 0
        ? "¥" + prize.toLocaleString("zh-CN", { maximumFractionDigits: 0 })
        : "暂无";
    const deadline = c.signupEnd ? B.formatDate(c.signupEnd) : "待定";
    const arrow = '<svg viewBox="0 0 24 24" width="14" height="14" style="flex:none;color:var(--muted)"><path d="M9 6l6 6-6 6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>';
    return `<a class="contest-item" href="/m/contest.html?id=${c.id}" style="display:flex">
      <img class="cover" src="${esc(cover)}" alt="${esc(c.title)}" loading="lazy" />
      <div class="body">
        <div class="tags"><span class="badge ${info.cls}">${info.label}</span>${mode}</div>
        <h3>${esc(c.title)}</h3>
        <p class="summary">${esc(c.summary || "")}</p>
        <div class="foot">
          <span class="deadline">报名截止 <em>${deadline}</em></span>
          <span style="font-size:12px;color:var(--muted)">奖金 ${prizeHtml}</span>
        </div>
      </div>
      ${arrow}
    </a>`;
  }

  function empty(msg, sub) {
    return `<div class="empty"><div class="ico">◌</div><p>${esc(msg || "暂无内容")}</p><span>${esc(sub || "")}</span></div>`;
  }

  function skeleton(n) {
    let s = "";
    for (let i = 0; i < n; i++) {
      s += '<div class="card pad" style="height:86px"><div class="skeleton" style="height:16px;width:60%"></div><div class="skeleton" style="height:12px;width:80%;margin-top:10px"></div><div class="skeleton" style="height:12px;width:40%;margin-top:8px"></div></div>';
    }
    return s;
  }

  /* ================= 首页 ================= */
  async function homeInit() {
    const slidesBox = document.getElementById("heroSlides");
    const dotsBox = document.getElementById("heroDots");
    const contestsBox = document.getElementById("homeContests");
    const noticesBox = document.getElementById("homeNotices");
    const rankBox = document.getElementById("homeRank");

    let list = [];
    try {
      const res = await B.api.get("/contests?page=1&size=8");
      list = res.records || [];
    } catch (e) {
      /* keep empty */
    }

    /* 统计 */
    try {
      const stats = await B.api.get("/stats");
      document.getElementById("stContests").textContent = Number(stats.contests || 0).toLocaleString("zh-CN");
      document.getElementById("stSignups").textContent = Number(stats.signups || 0).toLocaleString("zh-CN");
      document.getElementById("stWorks").textContent = Number(stats.works || 0).toLocaleString("zh-CN");
    } catch (e) {
      document.getElementById("stContests").textContent = list.length;
      document.getElementById("stSignups").textContent = list.reduce((s, c) => s + (c.signupCount || 0), 0);
      document.getElementById("stWorks").textContent = list.reduce((s, c) => s + (c.workCount || 0), 0);
    }

    /* 轮播 */
    const slides = list.filter((c) => c.status !== "DRAFT").slice(0, 3);
    if (slides.length) {
      slidesBox.innerHTML = slides
        .map(
          (c, i) => `<a class="hero-slide${i === 0 ? " is-on" : ""}" href="/m/contest.html?id=${c.id}" style="position:absolute;inset:0;opacity:${i === 0 ? 1 : 0};transition:opacity .5s ease">
            <img src="${esc(c.cover || "/assets/img/cover-1.svg")}" alt="${esc(c.title)}" />
            <div class="copy"><span class="badge ${B.statusInfo(c.status).cls}" style="background:rgba(255,255,255,.92);color:#17181c">${B.statusInfo(c.status).label}</span><h3>${esc(c.title)}</h3><p>${esc(c.summary || "")}</p></div>
          </a>`
        )
        .join("");
      dotsBox.innerHTML = slides.map((_, i) => `<i class="${i === 0 ? "on" : ""}" data-i="${i}"></i>`).join("");
      let idx = 0;
      function updateHero() {
        const nodes = slidesBox.querySelectorAll(".hero-slide");
        const dots = dotsBox.querySelectorAll("i");
        nodes.forEach((n, i) => {
          n.style.opacity = i === idx ? 1 : 0;
          n.classList.toggle("is-on", i === idx);
        });
        dots.forEach((d, i) => d.classList.toggle("on", i === idx));
      }
      updateHero();
      setInterval(() => {
        idx = (idx + 1) % slides.length;
        updateHero();
      }, 4500);
      dotsBox.addEventListener("click", (e) => {
        const di = e.target.dataset && e.target.dataset.i;
        if (di !== undefined) {
          idx = Number(di);
          updateHero();
        }
      });
      /* 触摸滑动切换 */
      let sx = 0;
      slidesBox.addEventListener("touchstart", (e) => {
        sx = e.changedTouches[0].clientX;
      }, { passive: true });
      slidesBox.addEventListener("touchmove", (e) => {
        if (Math.abs(e.changedTouches[0].clientX - sx) > 10) e.preventDefault();
      }, { passive: false });
      slidesBox.addEventListener("touchend", (e) => {
        const dx = e.changedTouches[0].clientX - sx;
        if (Math.abs(dx) > 40) {
          idx = dx > 0 ? (idx - 1 + slides.length) % slides.length : (idx + 1) % slides.length;
          updateHero();
        }
      });
    } else {
      slidesBox.innerHTML = '<img src="/assets/img/cover-1.svg" alt="BTR" style="width:100%;height:190px;object-fit:cover" />';
      dotsBox.innerHTML = "";
    }

    /* 精选赛事 */
    const hot = list.slice(0, 3);
    contestsBox.innerHTML = hot.length
      ? hot.map((c) => contestItem(c)).join("")
      : empty("暂时没有公开赛事", "管理员发布赛事后会在这里展示");

    /* 公告 */
    try {
      const nres = await B.api.get("/notices?page=1&size=3");
      const items = nres.records || [];
      noticesBox.innerHTML = items.length
        ? items
            .map(
              (n) => `<a class="notice-item" href="/m/notices.html?id=${n.id}">
                <div class="head"><b>${esc(n.title)}</b>${n.pinned ? '<span class="badge badge-plain">置顶</span>' : ""}<span class="date">${B.formatDate(n.publishedAt)}</span></div>
                <p class="content">${esc(n.content || "")}</p>
                <span class="more-tip">查看全文</span>
              </a>`
            )
            .join("")
        : empty("暂无公告", "平台公告会在这里更新");
    } catch (e) {
      noticesBox.innerHTML = empty("暂无公告", "稍后再来看看");
    }

    /* 榜单摘要 */
    const ranked = list.find((c) => c.status === "PUBLISHED") || list.find((c) => c.status === "FINISHED");
    if (!ranked) {
      rankBox.innerHTML = empty("榜单尚未公示", "赛事公示后会在这里展示");
      return;
    }
    document.getElementById("rankTitle").textContent = ranked.title;
    try {
      const rres = await B.api.get("/ranks?contestId=" + ranked.id);
      const rows = (rres.rows || []).slice(0, 3);
      rankBox.innerHTML = rows.length
        ? `<div class="card">
            ${rows
              .map(
                (r) => `<div class="rank-row">
                  <span class="rank-no ${r.rank === 1 ? "top" : ""}">${r.rank}</span>
                  <div class="info"><b>${esc(r.workTitle)}</b><span>${esc(r.author || "")}${r.school ? " · " + esc(r.school) : ""}</span></div>
                  <span class="score">${Number(r.avgScore).toFixed(1)}</span>
                </div>`
              )
              .join("")}
          </div>`
        : empty("暂无排名", "等待评委完成打分");
    } catch (e) {
      rankBox.innerHTML = empty("榜单暂不可用", "请稍后再试");
    }
  }

  /* ================= 赛事列表 ================= */
  async function contestsInit() {
    const listBox = document.getElementById("contestList");
    const countBox = document.getElementById("resultCount");
    const chips = document.querySelectorAll(".filter-scroll .chip");
    const kwInput = document.getElementById("searchKw");
    let status = "";
    let kw = "";

    async function load() {
      listBox.innerHTML = skeleton(3);
      let query = "/contests?page=1&size=50";
      if (status) query += "&status=" + status;
      if (kw) query += "&keyword=" + encodeURIComponent(kw);
      try {
        const res = await B.api.get(query);
        const list = res.records || [];
        countBox.textContent = "共 " + res.total + " 场赛事";
        listBox.innerHTML = list.length
          ? list.map((c) => contestItem(c)).join("")
          : empty("没有找到相关赛事", "换个筛选条件试试");
      } catch (e) {
        listBox.innerHTML = empty("加载失败", e.message || "请稍后再试");
      }
    }

    chips.forEach((chip) => {
      chip.addEventListener("click", () => {
        chips.forEach((c) => c.classList.remove("is-active"));
        chip.classList.add("is-active");
        status = chip.dataset.status || "";
        load();
      });
    });

    let t = null;
    kwInput.addEventListener("input", () => {
      clearTimeout(t);
      t = setTimeout(() => {
        kw = kwInput.value.trim();
        load();
      }, 350);
    });

    load();
  }

  /* ================= 赛事详情 ================= */
  async function contestInit() {
    const id = Number(B.qs("id"));
    if (!id) {
      location.href = "/m/contests.html";
      return;
    }
    let detail;
    try {
      detail = await B.api.get("/contests/" + id);
    } catch (e) {
      document.getElementById("detailHero").innerHTML = empty("赛事不存在", "请返回赛事列表");
      return;
    }
    const c = detail.contest;
    document.title = c.title + " · BTR";
    document.getElementById("pageTitle").textContent = c.title;

    /* hero */
    const hero = document.getElementById("detailHero");
    hero.innerHTML = `
      <img src="${esc(c.cover || "/assets/img/cover-1.svg")}" alt="${esc(c.title)}" />
      <div class="overlay"></div>
      <div class="info">
        <span class="badge ${B.statusInfo(c.status).cls}" style="background:rgba(255,255,255,.92);color:#17181c">${B.statusInfo(c.status).label}</span>
        <span class="badge badge-plain" style="background:rgba(255,255,255,.3);color:#fff">${c.mode === "OFFLINE" ? "线下比赛" : "线上比赛"}</span>
        <h2>${esc(c.title)}</h2>
        <p>${esc(c.summary || "")}</p>
      </div>`;

    /* meta */
    document.getElementById("mSignup").innerHTML = `<span>报名时间</span><b>${B.formatDate(c.signupStart)} — ${B.formatDate(c.signupEnd)}</b>`;
    document.getElementById("mReview").innerHTML = `<span>评审时间</span><b>${B.formatDate(c.reviewStart)} — ${B.formatDate(c.reviewEnd)}</b>`;
    document.getElementById("mCount").innerHTML = `<span>参赛规模</span><b>${c.signupCount || 0} 人报名</b>`;
    const prize = Number(c.prizeAmount || 0);
    document.getElementById("mPrize").textContent = prize > 0 ? "¥" + prize.toLocaleString("zh-CN", { maximumFractionDigits: 0 }) : "暂无";

    /* 赛事视频（线下赛事录像，管理员上传，所有人可观看/下载） */
    const videoBox = document.getElementById("contestVideo");
    if (videoBox && c.videoUrl) {
      videoBox.style.display = "block";
      videoBox.innerHTML = `
        <div class="card" style="padding:12px">
          <div style="font-size:13.5px;font-weight:600;margin-bottom:8px">赛事视频${c.videoName ? " · " + esc(c.videoName) : ""}</div>
          <video controls preload="metadata" style="width:100%;border-radius:10px;background:#000;max-height:260px" src="${esc(c.videoUrl)}"></video>
          <a class="btn btn-outline btn-block" style="margin-top:10px" href="${esc(downloadFile(c.videoUrl))}" download="${esc(c.videoName || "赛事视频.mp4")}">下载视频</a>
        </div>`;
    }

    /* tabs 内容 */
    document.getElementById("panelIntro").innerHTML = formatText(c.description || c.summary);
    document.getElementById("panelRules").innerHTML = formatText(c.rules);

    /* awards */
    const awards = detail.awards || [];
    document.getElementById("panelAwards").innerHTML = awards.length
      ? `<div class="card">${awards
          .map(
            (a) => `<div class="work-item" style="align-items:flex-start">
              <div class="meta"><b>${esc(a.name)}</b><span>名额 ${a.quota || 0} 个${B.formatPrize(a) ? " · " + esc(B.formatPrize(a)) : ""}</span></div>
            </div>`
          )
          .join("")}</div>`
      : empty("暂未设置奖项", "管理员配置奖项后会在这里展示");

    /* works */
    loadDetailWorks(c, document.getElementById("panelWorks"));

    /* 报名动作 */
    renderAction(c, detail);

    /* tabs 切换 */
    const tabBtns = document.querySelectorAll(".detail-tabs button");
    tabBtns.forEach((btn) => {
      btn.addEventListener("click", () => {
        tabBtns.forEach((x) => x.classList.remove("is-active"));
        btn.classList.add("is-active");
        document.querySelectorAll("div[data-panel]").forEach((p) => {
          p.style.display = p.id === btn.dataset.panel ? "block" : "none";
        });
      });
    });
  }

  function formatText(text) {
    if (!text) return '<p class="muted">暂无内容。</p>';
    return String(text)
      .split(/\n+/)
      .filter(Boolean)
      .map((line) => esc(line))
      .join("\n");
  }

  async function loadDetailWorks(c, host) {
    try {
      const res = await B.api.get("/works?contestId=" + c.id);
      const list = res.records || [];
      const showScore = c.status === "PUBLISHED" || c.status === "FINISHED";
      host.innerHTML = list.length
        ? list
            .map(
              (w) => `<div class="work-item">
                <img class="thumb" src="${esc(w.cover || "/assets/img/cover-1.svg")}" alt="" loading="lazy" />
                <div class="meta"><b>${esc(w.title || "未命名作品")}</b><span>${w.contestTitle ? "【" + esc(w.contestTitle) + "】" : ""}${esc(w.author || "选手")}${w.school ? " · " + esc(w.school) : ""}</span></div>
                <span class="score" style="color:${showScore && w.avgScore ? "var(--brand)" : "var(--muted)"}">${showScore && w.avgScore ? Number(w.avgScore).toFixed(2) + " 分" : "进行中"}</span>
                ${w.fileUrl ? `<a class="btn-download" href="${esc(downloadFile(w.fileUrl))}" download="${esc(w.fileName || "作品文件")}">下载</a><a class="btn-download" href="${esc(w.fileUrl)}" target="_blank" rel="noopener">观看</a>` : ""}
              </div>`
            )
            .join("")
        : empty("暂无作品", c.mode === "OFFLINE" ? "线下赛事不展示作品" : "作品提交后展示在这里");
    } catch (e) {
      host.innerHTML = empty("作品暂不可用", "请稍后再试");
    }
  }

  function renderAction(c, detail) {
    const user = B.storage.user;
    const mySignup = detail.mySignup;
    const myWork = detail.myWork;
    const host = document.getElementById("actionArea");
    const isPublished = c.status === "PUBLISHED" || c.status === "FINISHED";

    let html = "";
    if (!user) {
      html = `<a class="btn btn-primary btn-block" href="/m/login.html?redirect=${encodeURIComponent("/m/contest.html?id=" + c.id)}">登录后报名</a>`;
    } else if (user.role === "ADMIN") {
      html = `<a class="btn btn-primary btn-block" href="/admin/contest-manage.html">管理赛事</a>`;
    } else if (user.role === "JUDGE") {
      html = `<a class="btn btn-primary btn-block" href="/judge/judge-index.html">评委工作台</a>`;
    } else if (mySignup && mySignup.status === "APPROVED") {
      const inProgress = c.status === "SIGNUP" || c.status === "REVIEW";
      if (inProgress && c.mode === "OFFLINE") {
        html = `<button class="btn btn-primary btn-block" disabled>已报名 · 赛事进行中</button>`;
      } else if (inProgress) {
        html = myWork
          ? `<a class="btn btn-primary btn-block" href="/m/work.html?contestId=${c.id}">修改我的作品</a>`
          : `<a class="btn btn-primary btn-block" href="/m/work.html?contestId=${c.id}">提交作品</a>`;
      } else if (c.mode === "OFFLINE") {
        html = `<a class="btn btn-primary btn-block" href="/m/rank.html?contestId=${c.id}">查看榜单</a>`;
      } else {
        html = `<a class="btn btn-outline btn-block" href="/m/center.html">查看我的作品</a>`;
      }
    } else if (c.status === "SIGNUP") {
      html = `<a class="btn btn-primary btn-block" href="/m/signup.html?contestId=${c.id}">立即报名</a>`;
    } else if (isPublished) {
      html = `<a class="btn btn-primary btn-block" href="/m/rank.html?contestId=${c.id}">查看榜单</a>`;
    } else {
      html = `<button class="btn btn-outline btn-block" disabled>${c.status === "REVIEW" ? "赛事进行中 · 报名已截止" : "暂未开放报名"}</button>`;
    }
    host.innerHTML = html;
  }

  /* ================= 登录 / 注册 ================= */
  async function loginInit() {
    const tabs = document.querySelectorAll(".auth-tabs button");
    const loginForm = document.getElementById("loginForm");
    const regForm = document.getElementById("regForm");
    let captchaEnabled = false;

    async function loadCaptcha() {
      try {
        const data = await B.api.get("/auth/captcha");
        captchaEnabled = Boolean(data.enabled);
        document.getElementById("captchaRow").style.display = captchaEnabled ? "flex" : "none";
        if (captchaEnabled) {
          document.getElementById("captchaImg").src = data.image;
          document.getElementById("captchaImg").dataset.key = data.key;
        }
      } catch (e) {
        captchaEnabled = false;
        document.getElementById("captchaRow").style.display = "none";
      }
    }

    tabs.forEach((t) => {
      t.addEventListener("click", () => {
        tabs.forEach((x) => x.classList.remove("is-active"));
        t.classList.add("is-active");
        const mode = t.dataset.mode;
        loginForm.style.display = mode === "login" ? "block" : "none";
        regForm.style.display = mode === "reg" ? "block" : "none";
        if (mode === "reg") loadCaptcha();
      });
    });

    document.getElementById("captchaImg").addEventListener("click", loadCaptcha);

    loginForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      const account = document.getElementById("loginAccount").value.trim();
      const password = document.getElementById("loginPassword").value;
      if (!account || !password) return toast("请输入账号和密码");
      const btn = loginForm.querySelector("button[type=submit]");
      btn.disabled = true;
      const body = { account, password };
      if (captchaEnabled) {
        body.captchaKey = document.getElementById("captchaImg").dataset.key;
        body.captchaCode = document.getElementById("loginCaptcha").value.trim();
      }
      try {
        const data = await B.api.post("/auth/login", body);
        B.storage.save(data);
        const redirect = B.qs("redirect") || "/m/index.html";
        location.href = redirect;
      } catch (err) {
        toast(err.message || "登录失败");
        if (captchaEnabled) loadCaptcha();
      } finally {
        btn.disabled = false;
      }
    });

    regForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      const email = document.getElementById("regEmail").value.trim();
      const nickname = document.getElementById("regNickname").value.trim();
      const code = document.getElementById("regCode").value.trim();
      const password = document.getElementById("regPassword").value;
      if (!email || !nickname || !code || !password) return toast("请填写完整信息");
      const btn = regForm.querySelector("button[type=submit]");
      btn.disabled = true;
      try {
        const data = await B.api.post("/auth/register", { email, code, password, nickname });
        B.storage.save(data);
        toast("注册成功");
        setTimeout(() => (location.href = B.qs("redirect") || "/m/index.html"), 800);
      } catch (err) {
        toast(err.message || "注册失败");
      } finally {
        btn.disabled = false;
      }
    });

    document.getElementById("sendCodeBtn").addEventListener("click", async () => {
      const email = document.getElementById("regEmail").value.trim();
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return toast("请先填写正确的邮箱");
      const btn = document.getElementById("sendCodeBtn");
      btn.disabled = true;
      try {
        await B.api.post("/auth/send-register-code", { email });
        toast("验证码已发送");
        let left = 60;
        const timer = setInterval(() => {
          left--;
          btn.textContent = left + "s 后重发";
          if (left <= 0) {
            clearInterval(timer);
            btn.textContent = "发送验证码";
            btn.disabled = false;
          }
        }, 1000);
      } catch (err) {
        toast(err.message || "发送失败");
        btn.disabled = false;
      }
    });

    loadCaptcha();
  }

  /* ================= 报名 ================= */
  async function signupInit() {
    const id = Number(B.qs("contestId"));
    if (!id) {
      location.href = "/m/contests.html";
      return;
    }
    let c = null;
    try {
      const d = await B.api.get("/contests/" + id);
      c = d.contest;
    } catch (e) {
      /* ignore */
    }
    if (c) {
      document.getElementById("pageTitle").textContent = "报名 · " + c.title;
      document.getElementById("contestBrief").innerHTML =
        '<span class="badge ' + B.statusInfo(c.status).cls + '">' + B.statusInfo(c.status).label + "</span> " + esc(c.title);
      if (c.status !== "SIGNUP") {
        document.getElementById("submitBtn").disabled = true;
        document.getElementById("submitBtn").textContent = c.status === "REVIEW" ? "报名已截止（赛事进行中）" : "报名已截止";
      }
    }

    document.getElementById("signupForm").addEventListener("submit", async (e) => {
      e.preventDefault();
      const realName = document.getElementById("fRealName").value.trim();
      const school = document.getElementById("fSchool").value.trim();
      const contact = document.getElementById("fContact").value.trim();
      const intro = document.getElementById("fIntro").value.trim();
      if (!realName || !school || !contact) return toast("请填写姓名、学校与联系方式");
      const btn = document.getElementById("submitBtn");
      btn.disabled = true;
      try {
        await B.api.post("/signups", { contestId: id, realName, school, contact, intro });
        toast("报名成功");
        setTimeout(() => (location.href = "/m/center.html"), 900);
      } catch (err) {
        toast(err.message || "报名失败");
        btn.disabled = false;
      }
    });
  }

  /* ================= 个人中心 ================= */
  async function centerInit() {
    const user = B.storage.user;
    if (!user || !B.storage.token) {
      location.href = "/m/login.html?redirect=" + encodeURIComponent("/m/center.html");
      return;
    }
    document.getElementById("nickname").textContent = user.nickname || user.account || "选手";
    document.getElementById("avatar").textContent = (user.nickname || user.account || "U").slice(0, 1);
    document.getElementById("roleLabel").textContent = B.roleLabel(user.role);
    const accountEl = document.getElementById("accountLabel");
    if (accountEl) accountEl.textContent = user.account || "";
    const pfLink = document.getElementById("myPortfolioLink");
    if (pfLink && user.id) pfLink.href = "/m/portfolio.html?userId=" + user.id;

    if (user.role !== "PLAYER") {
      const playerMenus = document.getElementById("playerMenus");
      if (playerMenus) playerMenus.style.display = "none";
      const playerStats = document.getElementById("playerStats");
      if (playerStats) playerStats.style.display = "none";
      document.getElementById("roleMenus").style.display = "block";
      const roleLink = user.role === "ADMIN" ? "/admin/admin-index.html" : "/judge/judge-index.html";
      document.getElementById("roleEntry").href = roleLink;
      document.getElementById("roleEntryLabel").textContent = user.role === "ADMIN" ? "进入管理后台" : "进入评委工作台";
      document.getElementById("logoutBtn").addEventListener("click", logout);
      return;
    }

    document.getElementById("logoutBtn").addEventListener("click", logout);

    let s = [];
    let w = [];
    let a = [];
    try {
      const [signups, works, awards] = await Promise.all([
        B.api.get("/signups/mine"),
        B.api.get("/works/mine"),
        B.api.get("/awards/mine"),
      ]);
      // B.api.get 返回的已是数组本体，不要再取 .data
      s = signups || [];
      w = works || [];
      a = awards || [];
      document.getElementById("stSignup").textContent = s.length;
      document.getElementById("stWork").textContent = w.length;
      document.getElementById("stAward").textContent = a.length;
    } catch (e) {
      document.getElementById("stSignup").textContent = "0";
      document.getElementById("stWork").textContent = "0";
      document.getElementById("stAward").textContent = "0";
    }

    try {
      const box = document.getElementById("myLists");
      let html = '<div class="line-list" style="margin-bottom:14px">';
      html += '<div class="li" style="background:#fafbfc;font-weight:600;font-size:13px"><span style="flex:1">我的报名</span></div>';
      if (s.length) {
        html += s
          .map(
            (x) => `<a class="li" href="/m/contest.html?id=${x.contestId}" style="text-decoration:none;color:inherit">
              <div class="main"><b>${esc(x.contestTitle || "赛事")}</b><span>${B.formatDate(x.createdAt)}</span></div>
              <span class="badge ${B.signupStatusInfo(x.status).cls}">${B.signupStatusInfo(x.status).label}</span>
            </a>`
          )
          .join("");
      } else {
        html += '<div class="li"><span style="color:var(--muted);font-size:13px">还没有报名记录</span></div>';
      }
      html += '</div><div class="line-list"><div class="li" style="background:#fafbfc;font-weight:600;font-size:13px"><span style="flex:1">我的作品</span></div>';
      if (w.length) {
        html += w
          .map(
            (x) => `<a class="li" href="/m/contest.html?id=${x.contestId}" style="text-decoration:none;color:inherit">
              <div class="main"><b>${esc(x.title || "未命名作品")}</b><span>${esc(x.contestTitle || "")}</span></div>
            </a>`
          )
          .join("");
      } else {
        html += '<div class="li"><span style="color:var(--muted);font-size:13px">还没有提交作品</span></div>';
      }
      html += "</div>";
      if (a.length) {
        html += '<div class="line-list" style="margin-top:14px"><div class="li" style="background:#fafbfc;font-weight:600;font-size:13px"><span style="flex:1">我的获奖</span></div>';
        html += a
          .map(
            (x) => `<div class="li">
              <div class="main"><b>${esc(x.contestTitle || "赛事")} · ${esc(x.awardName || "获奖")}</b><span>${esc(x.rankNo ? "第 " + x.rankNo + " 名" : "")}${B.formatPrize(x) ? " · " + esc(B.formatPrize(x)) : ""}</span></div>
              <button class="mini" data-cert="${x.contestId}">下载证书</button>
            </div>`
          )
          .join("");
        html += "</div>";
      }
      box.innerHTML = html;
      box.querySelectorAll("[data-cert]").forEach((btn) => {
        btn.addEventListener("click", async () => {
          btn.disabled = true;
          btn.textContent = "下载中…";
          try {
            await B.api.download("/awards/mine/pdf?contestId=" + btn.dataset.cert, "获奖证书.pdf");
          } catch (err) {
            toast(err.message || "证书下载失败");
          } finally {
            btn.disabled = false;
            btn.textContent = "下载证书";
          }
        });
      });
    } catch (e) {
      document.getElementById("myLists").innerHTML = empty("加载失败", e.message || "请稍后再试");
    }
  }

  async function logout() {
    // 先调用服务端注销（需要 Authorization），再清理本地存储，避免 token 残留有效
    try {
      await B.api.post("/auth/logout");
    } catch (e) {
      /* ignore */
    }
    B.storage.clear();
    location.href = "/m/index.html";
  }

  /* ================= 榜单 ================= */
  async function rankInit() {
    const selBox = document.getElementById("rankSelect");
    const listBox = document.getElementById("rankList");
    const qsId = B.qs("contestId");
    let contests = [];
    try {
      const res = await B.api.get("/contests?page=1&size=50");
      contests = res.records || [];
    } catch (e) {
      listBox.innerHTML = empty("加载失败", "请稍后再试");
      return;
    }
    const candidates = contests.filter((c) => c.status === "PUBLISHED" || c.status === "FINISHED");
    if (!candidates.length) {
      selBox.innerHTML = "";
      listBox.innerHTML = empty("暂无已公示赛事", "赛事公示后开放榜单");
      return;
    }
    selBox.innerHTML = candidates
      .map(
        (c, i) =>
          `<button class="chip${i === 0 ? " is-active" : ""}" data-id="${c.id}">${esc(c.title)}</button>`
      )
      .join("");
    selBox.querySelectorAll(".chip").forEach((chip) => {
      chip.addEventListener("click", () => {
        selBox.querySelectorAll(".chip").forEach((x) => x.classList.remove("is-active"));
        chip.classList.add("is-active");
        loadRank(chip.dataset.id);
      });
    });
    const first = candidates.find((c) => String(c.id) === qsId) || candidates[0];
    if (qsId) {
      const match = selBox.querySelector('[data-id="' + qsId + '"]');
      if (match) {
        selBox.querySelectorAll(".chip").forEach((x) => x.classList.remove("is-active"));
        match.classList.add("is-active");
      }
    }
    loadRank(first.id);

    async function loadRank(id) {
      listBox.innerHTML = skeleton(4);
      try {
        const r = await B.api.get("/ranks?contestId=" + id);
        const rows = r.rows || [];
        listBox.innerHTML = rows.length
          ? `<div class="card" style="overflow:hidden">${rows
              .map(
                (row) => `<div class="rank-row">
                  <span class="rank-no ${row.rank === 1 ? "top" : ""}">${row.rank || "—"}</span>
                  <div class="info"><b>${esc(row.workTitle)}</b><span>${esc(row.author || "选手")}${row.school ? " · " + esc(row.school) : ""}</span>${row.fileUrl ? `<div style="display:flex;gap:8px;margin-top:6px"><button class="btn-download" data-preview="${esc(row.fileUrl)}" data-name="${esc(row.fileName || "")}" onclick="previewWork(this)">在线预览</button></div>` : ""}</div>
                  <span class="prize-box ${row.prizeAmount || row.gift ? "" : "plain"}">${(row.prizeAmount || row.gift) ? esc(prizeText(row)) : "未获奖"}</span>
                </div>`
              )
              .join("")}</div>`
          : empty("暂无参赛作品", "等待选手提交作品");
      } catch (e) {
        listBox.innerHTML = empty("榜单暂不可用", "请稍后再试");
      }
    }
  }

  /* ================= 公告 ================= */
  async function noticesInit() {
    const id = B.qs("id");
    const listBox = document.getElementById("noticeList");
    const detailBox = document.getElementById("noticeDetail");
    if (id) {
      listBox.style.display = "none";
      document.getElementById("pageTitle").textContent = "公告详情";
      try {
        const n = await B.api.get("/notices/" + id);
        detailBox.innerHTML = `<div class="notice-item" style="box-shadow:var(--shadow)">
          <div class="head"><b style="font-size:17px">${esc(n.title)}</b>${n.pinned ? '<span class="badge badge-plain">置顶</span>' : ""}</div>
          <span class="date" style="font-size:12px;color:var(--muted);display:block;margin:6px 0 8px">${B.formatDate(n.publishedAt)}</span>
          <div style="font-size:14px;line-height:1.9;white-space:pre-wrap;color:var(--ink-2)">${esc(n.content || "")}</div>
        </div>`;
      } catch (e) {
        detailBox.innerHTML = empty("公告不存在", "请返回公告列表");
      }
      return;
    }
    try {
      const res = await B.api.get("/notices?page=1&size=50");
      const items = res.records || [];
      listBox.innerHTML = items.length
        ? items
            .map(
              (n) => `<a class="notice-item" href="/m/notices.html?id=${n.id}">
                <div class="head"><b>${esc(n.title)}</b>${n.pinned ? '<span class="badge badge-plain">置顶</span>' : ""}<span class="date">${B.formatDate(n.publishedAt)}</span></div>
                <p class="content">${esc(n.content || "")}</p>
                <span class="more-tip">查看全文</span>
              </a>`
            )
            .join("")
        : empty("暂无公告", "平台公告会在这里更新");
    } catch (e) {
      listBox.innerHTML = empty("加载失败", "请稍后再试");
    }
  }

  /* ================= 我的作品提交 ================= */
  window.__workState = { currentSignup: null, uploaded: null, uploadedCover: null };

  async function workInit() {
    const user = B.storage.user;
    if (!user || !B.storage.token) {
      location.href = "/m/login.html?redirect=" + encodeURIComponent(location.pathname + location.search);
      return;
    }
    const qsId = Number(B.qs("contestId"));
    const form = document.getElementById("workForm");
    const st = window.__workState;
    let signups = [];
    let works = [];
    const contests = {};
    let currentWork = null;
    try {
      const [sRes, wRes, cRes] = await Promise.allSettled([
        B.api.get("/signups/mine"),
        B.api.get("/works/mine"),
        B.api.get("/contests?page=1&size=50"),
      ]);
      signups = sRes.status === "fulfilled" ? sRes.value || [] : [];
      works = wRes.status === "fulfilled" ? wRes.value || [] : [];
      const crecords = cRes.status === "fulfilled" ? cRes.value.records || [] : [];
      crecords.forEach((x) => (contests[x.id] = x));
    } catch (e) {
      /* ignore */
    }
    const candidates = signups.filter((s) => {
      if (s.status !== "APPROVED") return false;
      const c = contests[s.contestId];
      return !c || ((c.status === "SIGNUP" || c.status === "REVIEW") && c.mode !== "OFFLINE");
    });
    if (!candidates.length) {
      form.innerHTML =
        '<div class="empty"><div class="ico">\u25CC</div><p>没有可提交作品的赛事</p><span>线下赛事不提交作品，线上赛事需先报名并通过审核</span></div>';
      return;
    }
    const sel = document.getElementById("workSignup");
    sel.innerHTML = candidates
      .map((s) => `<option value="${s.id}">${esc(s.contestTitle)}</option>`)
      .join("");
    function selectSignup(signup) {
      st.currentSignup = signup;
      currentWork = works.find((w) => w.signupId === signup.id && w.status === "SUBMITTED") || null;
      st.uploaded = currentWork && currentWork.fileUrl ? { url: currentWork.fileUrl, name: currentWork.fileName } : null;
      st.uploadedCover = currentWork && currentWork.cover ? currentWork.cover : null;
      document.getElementById("wTitle").value = currentWork ? currentWork.title : "";
      document.getElementById("wDesc").value = currentWork ? currentWork.description || "" : "";
      const fi = document.getElementById("wFileInfo");
      if (fi) {
        fi.classList.toggle("hidden", !st.uploaded);
        fi.textContent = st.uploaded ? st.uploaded.name : "";
      }
      const ci = document.getElementById("wCoverInfo");
      if (ci) ci.classList.toggle("hidden", !st.uploadedCover);
      document.getElementById("wSubmitBtn").textContent = currentWork ? "保存修改" : "提交作品";
    }
    const initial = candidates.find((s) => s.contestId === qsId) || candidates[0];
    sel.value = String(initial.id);
    sel.addEventListener("change", () => {
      const s = candidates.find((x) => x.id === Number(sel.value));
      if (s) selectSignup(s);
    });
    selectSignup(initial);

    const fileInput = document.getElementById("wFile");
    fileInput.addEventListener("change", async () => {
      if (!fileInput.files.length) return;
      const fd = new FormData();
      fd.append("file", fileInput.files[0]);
      try {
        const data = await B.api.upload("/files?purpose=work", fd);
        st.uploaded = { url: data.url, name: data.name };
        const fi = document.getElementById("wFileInfo");
        fi.classList.remove("hidden");
        fi.textContent = data.name;
        toast("文件上传成功");
      } catch (err) {
        toast(err.message || "文件上传失败");
        fileInput.value = "";
      }
    });
    const coverInput = document.getElementById("wCover");
    coverInput.addEventListener("change", async () => {
      if (!coverInput.files.length) return;
      const fd = new FormData();
      fd.append("file", coverInput.files[0]);
      try {
        const data = await B.api.upload("/files?purpose=cover", fd);
        st.uploadedCover = data.url;
        document.getElementById("wCoverInfo").classList.remove("hidden");
        toast("封面上传成功");
      } catch (err) {
        toast(err.message || "封面上传失败");
        coverInput.value = "";
      }
    });

    async function submitWork() {
      const title = document.getElementById("wTitle").value.trim();
      const desc = document.getElementById("wDesc").value.trim();
      if (!title || !desc) return toast("请填写作品名称与描述");
      if (!st.currentSignup) return toast("请选择参赛报名");
      const btn = document.getElementById("wSubmitBtn");
      btn.disabled = true;
      try {
        await B.api.post("/works", {
          signupId: st.currentSignup.id,
          title,
          description: desc,
          fileUrl: st.uploaded ? st.uploaded.url : null,
          fileName: st.uploaded ? st.uploaded.name : null,
          cover: st.uploadedCover || null,
        });
        toast("作品提交成功");
        setTimeout(() => (location.href = "/m/center.html"), 900);
      } catch (err) {
        toast(err.message || "提交失败");
        btn.disabled = false;
      }
    }
    window.__workSubmit = submitWork;
    const submitBtn = document.getElementById("wSubmitBtn");
    if (submitBtn) submitBtn.addEventListener("click", submitWork);
    form.addEventListener("submit", (e) => {
      e.preventDefault();
      submitWork();
    });
  }

  /* ================= 忘记密码 ================= */
  async function forgotInit() {
    const sendBtn = document.getElementById("fpSendBtn");
    let countdown = 0;
    sendBtn.addEventListener("click", async () => {
      const email = document.getElementById("fpEmail").value.trim();
      if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return toast("请输入正确的邮箱地址");
      sendBtn.disabled = true;
      sendBtn.textContent = "发送中…";
      try {
        const data = await B.api.post("/auth/forgot-password", { email });
        toast((data && data.message) || "验证码已发送，请查收邮件");
        countdown = 60;
        sendBtn.textContent = countdown + "s 后重发";
        const timer = setInterval(() => {
          countdown--;
          if (countdown <= 0) {
            clearInterval(timer);
            sendBtn.disabled = false;
            sendBtn.textContent = "获取验证码";
          } else {
            sendBtn.textContent = countdown + "s 后重发";
          }
        }, 1000);
      } catch (err) {
        toast(err.message || "发送失败");
        sendBtn.disabled = false;
        sendBtn.textContent = "获取验证码";
      }
    });

    document.getElementById("forgotForm").addEventListener("submit", async (e) => {
      e.preventDefault();
      const email = document.getElementById("fpEmail").value.trim();
      const code = document.getElementById("fpCode").value.trim();
      const pwd = document.getElementById("fpPassword").value;
      const confirm = document.getElementById("fpConfirm").value;
      if (!email || !code) return toast("请填写邮箱与验证码");
      if (pwd.length < 6 || pwd.length > 32) return toast("密码长度需为 6-32 位");
      if (pwd !== confirm) return toast("两次输入的密码不一致");
      const btn = document.getElementById("fpSubmit");
      btn.disabled = true;
      try {
        await B.api.post("/auth/reset-password", { email, code, newPassword: pwd });
        toast("密码已重置，请使用新密码登录");
        setTimeout(() => (location.href = "/m/login.html"), 1200);
      } catch (err) {
        toast(err.message || "重置失败");
        btn.disabled = false;
      }
    });
  }

  /* ================= 作品集 ================= */
  async function portfolioInit() {
    const userId = Number(B.qs("userId"));
    const worksBox = document.getElementById("pfWorks");
    const awardsBox = document.getElementById("pfAwards");
    if (!userId) {
      document.getElementById("pfName").textContent = "作品集不存在";
      document.getElementById("pfSub").textContent = "缺少用户参数";
      worksBox.innerHTML = empty("无法加载", "链接缺少 userId 参数");
      awardsBox.innerHTML = "";
      return;
    }
    try {
      const d = await B.api.get("/users/" + userId + "/portfolio");
      const u = d.user || {};
      document.getElementById("pfAvatar").textContent = (u.nickname || "U").slice(0, 1);
      document.getElementById("pfName").textContent = u.nickname || "选手";
      const subBits = [];
      if (u.school) subBits.push(esc(u.school));
      if (u.role) subBits.push(B.roleLabel(u.role));
      document.getElementById("pfSub").innerHTML = subBits.join(" · ") || "BTR 选手";
      document.title = (u.nickname || "选手") + " 的作品集 · BTR";
      document.getElementById("stSignup").textContent = Number(d.signupCount || 0);
      document.getElementById("stWork").textContent = Number(d.workCount || (d.works || []).length);
      document.getElementById("stAward").textContent = Number(d.awardCount || (d.awards || []).length);

      const works = d.works || [];
      worksBox.innerHTML = works.length
        ? works
            .map(
              (w) => `<div class="pf-work">
                <img class="thumb" src="${esc(w.cover || "/assets/img/cover-1.svg")}" alt="${esc(w.title || "")}" loading="lazy" />
                <div class="meta">
                  <b>${esc(w.title || "未命名作品")}</b>
                  <div class="sc">
                    <span>${esc(w.contestTitle || "")}</span>
                    ${w.avgScore ? `<em style="font-style:normal;color:var(--brand);font-weight:600">${Number(w.avgScore).toFixed(1)} 分</em>` : ""}
                  </div>
                  ${w.awardName ? `<span class="badge badge-plain">${esc(w.awardName)}</span>` : ""}
                </div>
              </div>`
            )
            .join("")
        : empty("暂无参赛作品", "该选手尚未提交公开作品");

      const awards = d.awards || [];
      awardsBox.innerHTML = awards.length
        ? awards
            .map(
              (a) => `<div class="pf-award">
                <span class="rank-no">${a.rankNo || "—"}</span>
                <div class="info">
                  <b>${esc(a.awardName || "获奖")} · ${esc(a.contestTitle || "赛事")}</b>
                  <span>${B.formatDate(a.publishedAt)}${a.contestMode === "OFFLINE" ? " · 线下赛事" : ""}</span>
                </div>
                ${B.formatPrize(a) ? `<span class="prize">${esc(B.formatPrize(a))}</span>` : ""}
              </div>`
            )
            .join("")
        : empty("暂无获奖记录", "保持创作，期待上榜");
    } catch (e) {
      document.getElementById("pfName").textContent = "作品集不存在";
      document.getElementById("pfSub").textContent = e.message || "请稍后再试";
      worksBox.innerHTML = "";
      awardsBox.innerHTML = empty("无法加载", e.message || "该用户可能未公开作品集");
    }
  }

  /* ================= 评委工作台 ================= */
  async function judgeInit() {
    const user = B.storage.user;
    if (!user || !B.storage.token) {
      location.href = "/m/login.html?redirect=" + encodeURIComponent(location.pathname);
      return;
    }
    if (user.role !== "JUDGE" && user.role !== "ADMIN") {
      toast("仅评委可访问工作台");
      setTimeout(() => (location.href = "/m/index.html"), 700);
      return;
    }
    const projectsBox = document.getElementById("jdProjects");
    const tasksBox = document.getElementById("jdTasks");
    const sheet = document.getElementById("jdSheet");
    let projects = [];
    let currentTasks = [];
    let currentContestId = null;

    function renderProg(list) {
      const done = list.filter((t) => t.scored).length;
      const total = list.length;
      document.getElementById("pAssigned").textContent = total;
      document.getElementById("pScored").textContent = done;
      document.getElementById("pPending").textContent = total - done;
    }

    async function loadProjects() {
      try {
        const res = await B.api.get("/judge/projects");
        projects = Array.isArray(res) ? res : res.records || [];
      } catch (e) {
        projects = [];
      }
      if (!projects.length) {
        projectsBox.innerHTML = "";
        tasksBox.innerHTML = empty("暂无评审任务", "赛事分配作品后这里会显示评审项目");
        renderProg([]);
        return;
      }
      projectsBox.innerHTML = projects
        .map(
          (p, i) => `<button class="chip${i === 0 ? " is-active" : ""}" data-id="${p.contestId}">${esc(p.contestTitle)}</button>`
        )
        .join("");
      projectsBox.querySelectorAll(".chip").forEach((chip) => {
        chip.addEventListener("click", () => {
          projectsBox.querySelectorAll(".chip").forEach((x) => x.classList.remove("is-active"));
          chip.classList.add("is-active");
          loadTasks(chip.dataset.id);
        });
      });
      const first = projects.find((p) => String(p.contestId) === B.qs("contestId")) || projects[0];
      loadTasks(first.contestId);
    }

    async function loadTasks(contestId) {
      currentContestId = contestId;
      tasksBox.innerHTML = skeleton(3);
      try {
        const res = await B.api.get("/judge/tasks?contestId=" + contestId);
        currentTasks = Array.isArray(res) ? res : res.records || [];
      } catch (e) {
        currentTasks = [];
      }
      renderProg(currentTasks);
      const proj = projects.find((p) => String(p.contestId) === String(contestId));
      // 进行中 / 已结束（评审收尾）阶段均可打分
      const locked = !proj || !(proj.contestStatus === "REVIEW" || proj.contestStatus === "FINISHED");
      tasksBox.innerHTML = currentTasks.length
        ? currentTasks
            .map(
              (t) => `<div class="jd-task">
                <img class="thumb" src="${esc(t.cover || "/assets/img/cover-1.svg")}" alt="" loading="lazy" />
                <div class="info">
                  <b>${esc(t.workTitle || "未命名作品")}</b>
                  <span>${esc(t.realName || "选手")}${t.school ? " · " + esc(t.school) : ""}${t.workDescription ? " · " + esc(String(t.workDescription).slice(0, 14)) + "…" : ""}</span>
                </div>
                <span class="st ${t.scored ? "done" : "todo"}">${t.scored ? t.score + " 分" : "待评分"}</span>
                <button class="jd-score-btn" data-tid="${t.assignId || t.id}" data-wid="${t.workId || ""}" data-sid="${t.signupId || ""}" ${t.scored || locked ? "disabled" : ""}>${t.scored ? "已评分" : locked ? "非评审期" : "评分"}</button>
              </div>`
            )
            .join("")
        : empty("暂无评审任务", "该赛事尚未分配作品给你");

      tasksBox.querySelectorAll(".jd-score-btn").forEach((btn) => {
        btn.addEventListener("click", () => openSheet(btn.dataset));
      });
    }

    /* 打分面板 */
    let currentTask = null;
    function openSheet(ds) {
      const task = currentTasks.find((t) => String(t.assignId || t.id) === ds.tid);
      if (!task) return;
      currentTask = task;
      document.getElementById("jdScoreTitle").textContent = task.workTitle || "作品评分";
      document.getElementById("jdScoreSub").textContent = (task.realName || "选手") + (task.school ? " · " + task.school : "");
      const scoreInput = document.getElementById("jdScore");
      scoreInput.value = task.score != null ? Number(task.score) : 80;
      const quicks = document.querySelectorAll("#jdQuick .box");
      quicks.forEach((b) => {
        b.classList.toggle("is-on", task.score != null && Number(b.dataset.n) === Number(task.score));
      });
      quicks.forEach((b) => {
        b.addEventListener("click", () => {
          scoreInput.value = b.dataset.n;
          quicks.forEach((x) => x.classList.remove("is-on"));
          b.classList.add("is-on");
        });
      });
      document.getElementById("jdComment").value = task.comment || "";
      sheet.classList.add("is-open");
    }
    document.getElementById("jdSave").addEventListener("click", async () => {
      if (!currentTask) return;
      const score = Number(document.getElementById("jdScore").value);
      const comment = document.getElementById("jdComment").value.trim();
      if (!Number.isFinite(score) || score < 0 || score > 100) return toast("分数需在 0-100 之间");
      if (!comment) return toast("请填写评语");
      const btn = document.getElementById("jdSave");
      btn.disabled = true;
      const body = { score, comment };
      if (currentTask.workId) body.workId = currentTask.workId;
      else if (currentTask.signupId) body.signupId = currentTask.signupId;
      try {
        await B.api.post("/judge/scores", body);
        toast("评分已保存");
        sheet.classList.remove("is-open");
        loadTasks(currentContestId);
      } catch (err) {
        toast(err.message || "保存失败");
        btn.disabled = false;
      }
    });
    sheet.querySelectorAll("[data-close-sheet]").forEach((el) => {
      el.addEventListener("click", () => sheet.classList.remove("is-open"));
    });

    loadProjects();
  }

  /* ================= 管理后台 ================= */
  const STATUS_FLOW = ["DRAFT", "SIGNUP", "REVIEW", "PUBLISHED", "FINISHED"];
  const STATUS_LABEL = {
    DRAFT: "草稿",
    SIGNUP: "报名中",
    REVIEW: "进行中",
    PUBLISHED: "已公示",
    FINISHED: "已结束",
  };
  const ROLE_LABEL = { PLAYER: "选手", JUDGE: "评委", ADMIN: "管理员" };

  async function adminInit() {
    const user = B.storage.user;
    if (!user || !B.storage.token) {
      location.href = "/m/login.html?redirect=" + encodeURIComponent(location.pathname);
      return;
    }
    if (user.role !== "ADMIN") {
      toast("仅管理员可访问后台");
      setTimeout(() => (location.href = "/m/index.html"), 700);
      return;
    }
    const listBox = document.getElementById("adList");
    const sheet = document.getElementById("adSheet");
    let section = "contests";
    let rankContests = [];
    let rankContestId = null;

    try {
      const d = await B.api.get("/admin/dashboard");
      const set = (id, v) => {
        const el = document.getElementById(id);
        if (el) el.textContent = Number(v || 0).toLocaleString("zh-CN");
      };
      set("dUsers", d.users); set("dContests", d.contests); set("dSignups", d.signups);
      set("dWorks", d.works); set("dNotices", d.notices); set("dReviews", d.pendingReviews);
    } catch (e) { /* 统计失败不阻塞 */ }

    document.querySelectorAll("#adTabs .chip").forEach((chip) => {
      chip.addEventListener("click", () => {
        document.querySelectorAll("#adTabs .chip").forEach((x) => x.classList.remove("is-active"));
        chip.classList.add("is-active");
        section = chip.dataset.sec;
        loadSection();
      });
    });

    function badge(status) {
      const map = { DRAFT: "badge-plain", SIGNUP: "", REVIEW: "", PUBLISHED: "badge-plain", FINISHED: "badge-plain" };
      const cls = status === "SIGNUP" || status === "REVIEW" ? "" : "badge-plain";
      return `<span class="badge ${cls}">${STATUS_LABEL[status] || status}</span>`;
    }

    async function loadSection() {
      listBox.innerHTML = skeleton(3);
      if (section === "contests") {
        try {
          const res = await B.api.get("/contests?all=true&page=1&size=50");
          const list = res.records || [];
          listBox.innerHTML = list.length
            ? list
                .map(
                  (c) => `<div class="ad-row">
                    <div class="t1"><b>${esc(c.title)}</b>${badge(c.status)}</div>
                    <div class="t2"><span>${c.mode === "OFFLINE" ? "线下" : "线上"}</span><span>${c.signupCount || 0} 报名</span><span>${c.workCount || 0} 作品</span><span>奖金 ${Number(c.prizeAmount || 0).toLocaleString("zh-CN")} 元</span></div>
                    <div class="acts">
                      <button class="mini" data-view-rank="${c.id}">榜单</button>
                      ${c.mode === "OFFLINE" ? `<button class="mini" data-video="${c.id}">${c.videoUrl ? "更新视频" : "上传视频"}</button>` : ""}
                      <button class="mini" data-edit-status="${c.id}">${STATUS_LABEL[c.status] || c.status} · 改状态</button>
                    </div>
                  </div>`
                )
                .join("")
            : empty("暂无赛事", "发布赛事后在这里管理");
        } catch (e) {
          listBox.innerHTML = empty("加载失败", e.message || "请稍后再试");
        }
      } else if (section === "notices") {
        try {
          const res = await B.api.get("/admin/notices?page=1&size=50");
          const list = res.records || [];
          listBox.innerHTML =
            '<button class="chip is-active" data-new-notice style="margin-bottom:10px">＋ 新建公告</button>' +
            (list.length
              ? list
                  .map(
                    (n) => `<div class="ad-row">
                    <div class="t1"><b>${esc(n.title)}</b>${n.pinned ? '<span class="badge badge-plain">置顶</span>' : ""}</div>
                    <div class="t2"><span>${B.formatDate(n.publishedAt)}</span>${n.status ? `<span>${n.status}</span>` : ""}</div>
                    <div class="acts">
                      <button class="mini" data-edit-notice='${JSON.stringify({ id: n.id, title: n.title, content: n.content || "", pinned: !!n.pinned })}'>编辑</button>
                      <button class="danger" data-del-notice="${n.id}">删除</button>
                    </div>
                  </div>`
                  )
                  .join("")
              : "");
        } catch (e) {
          listBox.innerHTML = empty("加载失败", e.message || "请稍后再试");
        }
      } else if (section === "users") {
        try {
          const res = await B.api.get("/admin/users?page=1&size=50");
          const list = Array.isArray(res) ? res : res.records || [];
          listBox.innerHTML = list.length
            ? list
                .map(
                  (u) => `<div class="ad-row">
                    <div class="t1"><b>${esc(u.nickname || u.account)}</b>${u.status === "DISABLED" ? '<span class="badge badge-plain">已禁用</span>' : ""}</div>
                    <div class="t2"><span>${esc(u.account || "")}</span><span>${ROLE_LABEL[u.role] || u.role}</span><span>${u.school ? esc(u.school) : ""}</span></div>
                    <div class="acts">
                      <button class="${u.status === "DISABLED" ? "ok" : "mini"}" data-user-toggle="${u.id}" data-status="${u.status === "DISABLED" ? "ACTIVE" : "DISABLED"}">${u.status === "DISABLED" ? "启用" : "禁用"}</button>
                    </div>
                  </div>`
                )
                .join("")
            : empty("暂无用户", "");
        } catch (e) {
          listBox.innerHTML = empty("加载失败", e.message || "请稍后再试");
        }
      } else if (section === "rank") {
        try {
          const res = await B.api.get("/contests?all=true&page=1&size=50");
          rankContests = (res.records || []).filter((c) => c.status === "REVIEW" || c.status === "PUBLISHED" || c.status === "FINISHED");
          const sel = rankContests.find((c) => String(c.id) === String(rankContestId)) || rankContests[0];
          rankContestId = sel ? sel.id : null;
          listBox.innerHTML =
            `<select id="rankSel" style="width:100%;height:50px;border:1.5px solid var(--line);border-radius:14px;padding:0 12px;font-size:15px;background:var(--surface);margin-bottom:10px">
              ${rankContests.map((c) => `<option value="${c.id}" ${sel && sel.id === c.id ? "selected" : ""}>${esc(c.title)}</option>`).join("")}
            </select>` +
            (rankContestId ? `<button class="ad-recalc" id="adRecalc">按当前分数重算获奖名单</button><div id="adRankBox"></div>` : empty("暂无进行中的赛事", "赛事进入进行中后可维护榜单"));
          const selEl = document.getElementById("rankSel");
          if (selEl) selEl.addEventListener("change", () => { rankContestId = Number(selEl.value); loadSection(); });
          const recalc = document.getElementById("adRecalc");
          if (recalc) recalc.addEventListener("click", () => recompute(rankContestId));
          loadRankBox(rankContestId);
        } catch (e) {
          listBox.innerHTML = empty("加载失败", e.message || "请稍后再试");
        }
      }
      bindRowEvents();
    }

    async function loadRankBox(id) {
      const box = document.getElementById("adRankBox");
      if (!box || !id) return;
      box.innerHTML = skeleton(3);
      try {
        const d = await B.api.get("/admin/contests/" + id + "/ranking");
        const rows = (d.ranking && d.ranking.rows) || [];
        const awards = d.awards || [];
        box.innerHTML =
          (awards.length ? `<div style="font-size:12.5px;color:var(--ink-2);margin-bottom:8px">奖项：${awards.map((a) => esc(a.name)).join(" / ")}</div>` : "") +
          (rows.length
            ? rows
                .map(
                  (r) => `<div class="ad-rank-row">
                    <span class="rank-no">${r.rank}</span>
                    <div class="info"><b>${esc(r.workTitle)}</b><span>${esc(r.author || "")}${r.awardName ? " · " + esc(r.awardName) : ""}</span></div>
                    <span class="score">${Number(r.avgScore).toFixed(2)}</span>
                  </div>`
                )
                .join("")
            : empty("暂无排名", "等待评委完成打分"));
      } catch (e) {
        box.innerHTML = empty("榜单暂不可用", e.message || "请稍后再试");
      }
    }

    async function recompute(id) {
      try {
        const d = await B.api.post("/admin/contests/" + id + "/winners/recompute");
        toast((d && d.message) || "获奖名单已按当前分数重算");
        loadRankBox(id);
      } catch (err) {
        toast(err.message || "重算失败");
      }
    }

    function bindRowEvents() {
      listBox.querySelectorAll("[data-view-rank]").forEach((el) => {
        el.addEventListener("click", () => {
          rankContestId = Number(el.dataset.viewRank);
          document.querySelectorAll("#adTabs .chip").forEach((x) => x.classList.remove("is-active"));
          document.querySelector('#adTabs .chip[data-sec="rank"]').classList.add("is-active");
          section = "rank";
          loadSection();
        });
      });
      listBox.querySelectorAll("[data-edit-status]").forEach((el) => {
        el.addEventListener("click", () => openStatusSheet(Number(el.dataset.editStatus)));
      });
      listBox.querySelectorAll("[data-video]").forEach((el) => {
        el.addEventListener("click", () => uploadVideo(Number(el.dataset.video)));
      });
      listBox.querySelectorAll("[data-new-notice]").forEach((el) => {
        el.addEventListener("click", () => openNoticeSheet(null));
      });
      listBox.querySelectorAll("[data-edit-notice]").forEach((el) => {
        el.addEventListener("click", () => {
          try { openNoticeSheet(JSON.parse(el.dataset.editNotice)); } catch (e) { /* ignore */ }
        });
      });
      listBox.querySelectorAll("[data-del-notice]").forEach((el) => {
        el.addEventListener("click", async () => {
          if (!confirm("确认删除这条公告？")) return;
          try {
            await B.api.del("/admin/notices/" + el.dataset.delNotice);
            toast("公告已删除");
            loadSection();
          } catch (err) {
            toast(err.message || "删除失败");
          }
        });
      });
      listBox.querySelectorAll("[data-user-toggle]").forEach((el) => {
        el.addEventListener("click", async () => {
          const uid = el.dataset.userToggle;
          const status = el.dataset.status;
          try {
            await B.api.put("/admin/users/" + uid + "/status", { status });
            toast(status === "ACTIVE" ? "已启用该用户" : "已禁用该用户");
            loadSection();
          } catch (err) {
            toast(err.message || "操作失败");
          }
        });
      });
    }

    /* 线下赛事视频上传（管理员） */
    function uploadVideo(id) {
      const input = document.createElement("input");
      input.type = "file";
      input.accept = "video/mp4,video/quicktime,.mp4,.mov";
      input.style.display = "none";
      document.body.appendChild(input);
      input.addEventListener("change", async () => {
        const file = input.files && input.files[0];
        if (!file) {
          input.remove();
          return;
        }
        const fd = new FormData();
        fd.append("file", file);
        const btn = listBox.querySelector('[data-video="' + id + '"]');
        const old = btn ? btn.textContent : "上传视频";
        if (btn) {
          btn.disabled = true;
          btn.textContent = "上传中…";
        }
        try {
          const data = await B.api.upload("/files?purpose=work", fd);
          const d = await B.api.get("/contests/" + id);
          const c = d.contest || d;
          await B.api.put("/contests/" + id, {
            title: c.title,
            cover: c.cover || null,
            summary: c.summary || "",
            description: c.description || "",
            rules: c.rules || "",
            prizeAmount: c.prizeAmount != null ? Number(c.prizeAmount) : null,
            mode: c.mode || "ONLINE",
            videoUrl: data.url,
            videoName: file.name,
            signupStart: c.signupStart || null,
            signupEnd: c.signupEnd || null,
            reviewStart: c.reviewStart || null,
            reviewEnd: c.reviewEnd || null,
            status: c.status,
            isPublic: c.isPublic != null ? !!c.isPublic : true,
          });
          toast("赛事视频已更新，所有人可下载");
          loadSection();
        } catch (err) {
          toast(err.message || "视频上传失败");
          if (btn) {
            btn.disabled = false;
            btn.textContent = old;
          }
        } finally {
          input.remove();
        }
      });
      input.click();
    }

    /* 赛事状态弹层 */
    function openStatusSheet(id) {
      document.getElementById("adSheetTitle").textContent = "修改赛事状态";
      document.getElementById("adSheetBody").innerHTML = `
        <div class="field"><select id="stStatus">
          ${STATUS_FLOW.map((s) => `<option value="${s}">${STATUS_LABEL[s]}</option>`).join("")}
        </select></div>
        <div style="font-size:12px;color:var(--muted);line-height:1.6">状态按「草稿 → 报名 → 进行中 → 已结束 → 已公示」推进，后端会校验流转合法性（进行中可反复修改作品，进入已结束即锁定）。</div>`;
      document.getElementById("adSheetSave").onclick = async () => {
        const status = document.getElementById("stStatus").value;
        const btn = document.getElementById("adSheetSave");
        btn.disabled = true;
        try {
          /* 状态经完整赛事更新接口提交：先取当前详情回填，避免覆盖其他字段 */
          const d = await B.api.get("/contests/" + id);
          const c = d.contest || d;
          await B.api.put("/contests/" + id, {
            title: c.title,
            cover: c.cover || null,
            summary: c.summary || "",
            description: c.description || "",
            rules: c.rules || "",
            prizeAmount: c.prizeAmount != null ? Number(c.prizeAmount) : null,
            mode: c.mode || "ONLINE",
            signupStart: c.signupStart || null,
            signupEnd: c.signupEnd || null,
            reviewStart: c.reviewStart || null,
            reviewEnd: c.reviewEnd || null,
            status,
            isPublic: c.isPublic != null ? !!c.isPublic : true,
          });
          toast("状态已更新为「" + STATUS_LABEL[status] + "」");
          sheet.classList.remove("is-open");
          loadSection();
        } catch (err) {
          toast(err.message || "状态更新失败");
          btn.disabled = false;
        }
      };
      sheet.classList.add("is-open");
    }

    /* 公告弹层 */
    function openNoticeSheet(n) {
      const isEdit = !!n;
      document.getElementById("adSheetTitle").textContent = isEdit ? "编辑公告" : "新建公告";
      document.getElementById("adSheetBody").innerHTML = `
        <div class="field"><input id="nTitle" type="text" placeholder="公告标题" value="${isEdit ? esc(n.title) : ""}" /></div>
        <textarea id="nContent" placeholder="公告内容（支持换行）">${isEdit ? esc(n.content) : ""}</textarea>
        <div class="field" style="margin-top:10px"><select id="nPinned"><option value="false">普通公告</option><option value="true" ${isEdit && n.pinned ? "selected" : ""}>置顶公告</option></select></div>`;
      document.getElementById("adSheetSave").onclick = async () => {
        const title = document.getElementById("nTitle").value.trim();
        const content = document.getElementById("nContent").value.trim();
        const pinned = document.getElementById("nPinned").value === "true";
        if (!title || !content) return toast("请填写标题与内容");
        const btn = document.getElementById("adSheetSave");
        btn.disabled = true;
        const body = { title, content, pinned };
        try {
          if (isEdit) await B.api.put("/admin/notices/" + n.id, body);
          else await B.api.post("/admin/notices", body);
          toast(isEdit ? "公告已更新" : "公告已发布");
          sheet.classList.remove("is-open");
          loadSection();
        } catch (err) {
          toast(err.message || "保存失败");
          btn.disabled = false;
        }
      };
      sheet.classList.add("is-open");
    }

    sheet.querySelectorAll("[data-close-sheet]").forEach((el) => {
      el.addEventListener("click", () => sheet.classList.remove("is-open"));
    });

    loadSection();
  }

  /* ================= 启动 ================= */
  function init() {
    markTab();
    const back = document.querySelector(".m-header .back");
    if (back) back.addEventListener("click", (e) => {
      e.preventDefault();
      if (history.length > 1) history.back();
      else location.href = "/m/index.html";
    });

    if (page === "index") homeInit();
    else if (page === "contests") contestsInit();
    else if (page === "contest") contestInit();
    else if (page === "login") loginInit();
    else if (page === "signup") signupInit();
    else if (page === "center") centerInit();
    else if (page === "rank") rankInit();
    else if (page === "notices") noticesInit();
    else if (page === "work") workInit();
    else if (page === "forgot") forgotInit();
    else if (page === "portfolio") portfolioInit();
    else if (page === "judge") judgeInit();
    else if (page === "admin") adminInit();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }

  window.M = { toast, logout };
})();
