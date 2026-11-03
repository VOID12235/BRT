(function () {
  const B = window.BTR;
  const C = window.BTRCommon;
  let detail = null;
  let contestId = Number(B.qs("id"));

  async function init() {
    if (!contestId) {
      contestId = 1;
    }
    try {
      detail = await B.api.get("/contests/" + contestId);
    } catch (e) {
      document.getElementById("detailBanner").innerHTML = "";
      document.getElementById("detailBanner").classList.remove("skeleton");
      document.getElementById("detailBanner").style.background = "transparent";
      document.getElementById("detailBanner").innerHTML = B.emptyState("赛事不存在", e.message || "请返回赛事列表");
      return;
    }
    render();
    loadWorks();
    C.initTabs();
  }

  function render() {
    const c = detail.contest;
    document.title = c.title + " · BTR 赛事平台";
    document.getElementById("crumbTitle").textContent = c.title;
    renderBanner(c);
    renderIntro(c);
    renderRules(c);
    renderAwards();
    renderSide(c);
    renderTimeline(c);
    renderStats(c);
  }

  function renderBanner(c) {
    const banner = document.getElementById("detailBanner");
    banner.classList.remove("skeleton");
    banner.innerHTML = `
      <img src="${B.escapeHtml(c.cover || "/assets/img/cover-1.svg")}" alt="${B.escapeHtml(c.title)}" />
      <div class="detail-banner-copy">
        <span class="badge badge-plain">${B.statusInfo(c.status).label}</span>
        <h1>${B.escapeHtml(c.title)}</h1>
        <p>${B.escapeHtml(c.summary || "")}</p>
        <div class="detail-banner-meta">
          <div>报名时间<b>${B.formatDate(c.signupStart)} — ${B.formatDate(c.signupEnd)}</b></div>
          <div>评审时间<b>${B.formatDate(c.reviewStart)} — ${B.formatDate(c.reviewEnd)}</b></div>
          <div>参赛规模<b>${c.signupCount || 0} 人报名</b></div>
        </div>
      </div>`;
  }

  function formatText(text) {
    if (!text) return '<p class="muted">暂无内容。</p>';
    return String(text)
      .split(/\n+/)
      .filter(Boolean)
      .map((line) => {
        const clean = B.escapeHtml(line);
        if (/^\d+[.、]/.test(line)) return `<p style="padding-left: 18px; position: relative"><span style="position:absolute;left:0;color:var(--brand)">•</span>${clean.replace(/^\d+[.、]\s*/, "")}</p>`;
        return `<p>${clean}</p>`;
      })
      .join("");
  }

  function renderIntro(c) {
    document.getElementById("panelIntro").innerHTML = `
      <h3 style="margin-top:0">关于这场赛事</h3>
      ${formatText(c.description || c.summary)}
      <h3>参赛对象</h3>
      <p>面向所有对赛事主题感兴趣的创作者与选手，具体资格以参赛规则为准。</p>`;
  }

  function renderRules(c) {
    document.getElementById("panelRules").innerHTML = formatText(c.rules);
  }

  function renderAwards() {
    const awards = detail.awards || [];
    const host = document.getElementById("panelAwards");
    if (!awards.length) {
      host.innerHTML = B.emptyState("暂未设置奖项", "管理员配置奖项后会在这里展示");
      return;
    }
    host.innerHTML = `<div class="stack">${awards
      .map(
        (a, i) => `<div class="work-item" style="display:flex;align-items:center;gap:16px">
          <span class="award-pill ${i === 0 ? "gold" : i === 1 ? "silver" : "bronze"}">${B.escapeHtml(a.name)}</span>
          <div style="flex:1">
            <h4 style="margin:0">${B.escapeHtml(a.name)}</h4>
            <p>名额 ${a.quota} 个 · 最低分 ${Number(a.minScore || 0).toFixed(0)} 分</p>
          </div>
          <span class="muted small">按综合评分排序</span>
        </div>`
      )
      .join("")}</div>`;
  }

  function renderSide(c) {
    const user = B.storage.user;
    const mySignup = detail.mySignup;
    const myWork = detail.myWork;
    const actions = [];
    const isPublished = c.status === "PUBLISHED" || c.status === "FINISHED";

    if (!user) {
      actions.push(`<a class="btn btn-primary" href="/login.html?redirect=${encodeURIComponent(
        "/contest-detail.html?id=" + c.id
      )}">登录后报名</a>`);
    } else if (user.role === "ADMIN") {
      actions.push(`<a class="btn btn-dark" href="/admin/contest-manage.html">管理赛事</a>`);
      actions.push(`<a class="btn btn-outline" href="/rank.html?contestId=${c.id}">查看榜单</a>`);
    } else if (user.role === "JUDGE") {
      actions.push(`<a class="btn btn-dark" href="/judge/judge-index.html">评委工作台</a>`);
      actions.push(`<a class="btn btn-outline" href="/rank.html?contestId=${c.id}">查看榜单</a>`);
    } else if (mySignup && mySignup.status === "APPROVED") {
      if (c.status === "SIGNUP") {
        actions.push(
          myWork
            ? `<a class="btn btn-primary" href="/work-submit.html?contestId=${c.id}">修改我的作品</a>`
            : `<a class="btn btn-primary" href="/work-submit.html?contestId=${c.id}">提交作品</a>`
        );
      } else {
        actions.push(`<a class="btn btn-outline" href="/user-center.html">查看我的作品</a>`);
      }
      actions.push(`<a class="btn btn-ghost" href="/user-center.html">我的报名</a>`);
    } else if (c.status === "SIGNUP") {
      actions.push(`<a class="btn btn-primary" href="/signup.html?contestId=${c.id}">立即报名</a>`);
    } else if (isPublished) {
      actions.push(`<a class="btn btn-primary" href="/rank.html?contestId=${c.id}">查看榜单</a>`);
    } else {
      actions.push(`<button class="btn btn-outline" disabled>${c.status === "REVIEW" ? "评审进行中" : "暂未开放报名"}</button>`);
    }

    if (!isPublished && c.status !== "SIGNUP") {
      actions.push(`<p class="tiny muted" style="text-align:center;margin-top:2px">榜单将在公示后开放</p>`);
    }
    document.getElementById("sideActions").innerHTML = actions.join("");

    const prize = Number(c.prizeAmount || 0);
    document.getElementById("sideMeta").innerHTML = `
      <div><span>赛事状态</span><b>${B.statusInfo(c.status).label}</b></div>
      ${prize > 0 ? `<div><span>赛事奖金</span><b class="prize-amount">¥${prize.toLocaleString("zh-CN", { maximumFractionDigits: 0 })}</b></div>` : ""}
      <div><span>报名截止</span><b>${B.formatDate(c.signupEnd)}</b></div>
      <div><span>评审时间</span><b>${B.formatDate(c.reviewStart)} 起</b></div>
      <div><span>报名人数</span><b>${c.signupCount || 0} 人</b></div>`;
  }

  function renderTimeline(c) {
    const now = new Date();
    const items = [
      { label: "报名开始", time: c.signupStart, state: "done" },
      { label: "报名截止", time: c.signupEnd, state: "done" },
      { label: "评审开始", time: c.reviewStart, state: "current" },
      { label: "结果公示", time: c.reviewEnd, state: "" },
    ];
    const currentStatus = c.status;
    const host = document.getElementById("timeline");
    host.innerHTML = items
      .map((item) => {
        let cls = "";
        const date = B.parseDate(item.time);
        if (date && date < now) cls = "is-done";
        if (
          (item.label === "评审开始" && currentStatus === "REVIEW") ||
          (item.label === "结果公示" && (currentStatus === "PUBLISHED" || currentStatus === "FINISHED"))
        ) {
          cls = "is-current";
        }
        return `<div class="timeline-item ${cls}">
          <b>${item.label}</b>
          <span>${B.formatDate(item.time, true)}</span>
        </div>`;
      })
      .join("");
  }

  function renderStats(c) {
    document.getElementById("sideStats").innerHTML = `
      <div><span>报名人数</span><b>${c.signupCount || 0}</b></div>
      <div><span>提交作品</span><b>${c.workCount || 0}</b></div>
      <div><span>参与评委</span><b>${detail.judgeCount || 0}</b></div>
      <div><span>奖项数量</span><b>${(detail.awards || []).length}</b></div>`;
  }

  async function loadWorks() {
    const host = document.getElementById("panelWorks");
    host.innerHTML = '<div class="skeleton" style="height: 90px"></div>';
    try {
      const data = await B.api.get("/works?contestId=" + contestId + "&page=1&size=20");
      const list = data.records || [];
      if (!list.length) {
        host.innerHTML = B.emptyState("还没有作品", "作品提交后会在这里展示");
        return;
      }
      const showScore = detail.contest.status === "PUBLISHED" || detail.contest.status === "FINISHED";
      host.innerHTML = `<div class="work-grid">${list
        .map(
          (w) => `<div class="work-item">
            <h4>${B.escapeHtml(w.title)}</h4>
            <p>${B.escapeHtml(w.description || "暂无作品描述")}</p>
            <div class="meta">
              <span>${B.escapeHtml(w.author || "匿名选手")}${w.school ? " · " + B.escapeHtml(w.school) : ""}</span>
              ${showScore && w.avgScore ? `<b class="mono">${Number(w.avgScore).toFixed(2)} 分</b>` : "<span>评审中</span>"}
            </div>
          </div>`
        )
        .join("")}</div>`;
    } catch (e) {
      host.innerHTML = B.emptyState("作品加载失败", e.message || "请稍后重试");
    }
  }

  document.addEventListener("DOMContentLoaded", init);
})();
