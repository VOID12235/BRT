(function () {
  const B = window.BTR;
  let contestId = Number(B.qs("contestId"));
  let detail = null;

  async function init() {
    const user = B.requireAuth(["PLAYER"]);
    if (!user) return;

    if (!contestId) {
      await loadOpenContests();
      return;
    }
    await loadContest();
  }

  async function loadOpenContests() {
    try {
      const data = await B.api.get("/contests?status=SIGNUP&page=1&size=50");
      const list = data.records || [];
      if (!list.length) {
        showClosed("暂无可报名赛事", "当前没有处于报名阶段的赛事，请关注后续公告。");
        return;
      }
      if (list.length === 1) {
        contestId = list[0].id;
        history.replaceState(null, "", "/signup.html?contestId=" + contestId);
        await loadContest();
        return;
      }
      const select = document.createElement("select");
      select.className = "select";
      select.innerHTML = '<option value="">请选择赛事</option>' + list.map((c) => `<option value="${c.id}">${B.escapeHtml(c.title)}</option>`).join("");
      select.addEventListener("change", () => {
        if (!select.value) return;
        contestId = Number(select.value);
        history.replaceState(null, "", "/signup.html?contestId=" + contestId);
        loadContest();
      });
      document.getElementById("summaryCard").innerHTML = `<h3>选择要报名的赛事</h3><p class="small muted" style="margin:10px 0 16px">当前有多场赛事开放报名。</p>`;
      document.getElementById("summaryCard").appendChild(select);
    } catch (e) {
      showClosed("赛事加载失败", e.message || "请稍后重试");
    }
  }

  async function loadContest() {
    try {
      detail = await B.api.get("/contests/" + contestId);
    } catch (e) {
      showClosed("赛事不存在", e.message || "请返回赛事列表");
      return;
    }
    const c = detail.contest;
    renderSummary(c);
    document.getElementById("cancelLink").href = "/contest-detail.html?id=" + c.id;
    if (c.status !== "SIGNUP") {
      showClosed("报名已结束", "该赛事当前状态为「" + B.statusInfo(c.status).label + "」，无法继续报名。");
      return;
    }
    if (detail.mySignup && detail.mySignup.status !== "CANCELED") {
      showClosed(
        "你已报名该赛事",
        "无需重复报名，可直接提交或修改作品。",
        detail.mySignup.status === "APPROVED"
          ? `<a class="btn btn-primary" href="/work-submit.html?contestId=${c.id}">去提交作品</a>`
          : `<a class="btn btn-outline" href="/user-center.html">查看报名状态</a>`
      );
    }
  }

  function renderSummary(c) {
    const host = document.getElementById("summaryCard");
    host.innerHTML = `
      <div class="summary-cover"><img src="${B.escapeHtml(c.cover || "/assets/img/cover-1.svg")}" alt="${B.escapeHtml(c.title)}" /></div>
      <span class="badge ${B.statusInfo(c.status).cls}">${B.statusInfo(c.status).label}</span>
      <h3 style="margin-top:10px">${B.escapeHtml(c.title)}</h3>
      <p class="small muted">${B.escapeHtml(c.summary || "")}</p>
      <div class="summary-list">
        <div><span>报名时间</span><b>${B.formatDate(c.signupStart)} — ${B.formatDate(c.signupEnd)}</b></div>
        <div><span>评审时间</span><b>${B.formatDate(c.reviewStart)} — ${B.formatDate(c.reviewEnd)}</b></div>
        <div><span>当前报名</span><b>${c.signupCount || 0} 人</b></div>
        <div><span>奖项设置</span><b>${(detail.awards || []).length} 项</b></div>
      </div>
      <div class="alert alert-info" style="margin-top:16px">报名成功后，请在截止前完成作品提交。</div>`;
  }

  function showClosed(title, desc, action) {
    const card = document.getElementById("formCard");
    card.innerHTML = `<div class="success-panel" style="padding:40px 20px">
      <div class="empty-icon" style="margin-bottom:14px">${iconSvg()}</div>
      <h2 style="font-size:20px">${B.escapeHtml(title)}</h2>
      <p>${B.escapeHtml(desc)}</p>
      <div class="row" style="justify-content:center">
        ${action || '<a class="btn btn-outline" href="/contest-list.html">返回赛事列表</a>'}
      </div>
    </div>`;
  }

  function iconSvg() {
    return `<svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><circle cx="12" cy="12" r="9"/><path d="M12 8v4l2.5 2.5"/></svg>`;
  }

  async function submit(e) {
    e.preventDefault();
    if (!contestId) {
      B.toast("请先选择赛事", "warn");
      return;
    }
    const realName = document.getElementById("realName").value.trim();
    const school = document.getElementById("school").value.trim();
    const contact = document.getElementById("contact").value.trim();
    const intro = document.getElementById("intro").value.trim();
    if (!realName || !contact) {
      B.toast("请填写真实姓名和联系方式", "warn");
      return;
    }
    if (!document.getElementById("agree").checked) {
      B.toast("请先阅读并同意赛事规则", "warn");
      return;
    }
    const btn = document.getElementById("submitBtn");
    btn.disabled = true;
    btn.textContent = "提交中…";
    try {
      await B.api.post("/signups", { contestId, realName, school, contact, intro });
      document.getElementById("formCard").classList.add("hidden");
      document.getElementById("summaryCard").classList.add("hidden");
      document.getElementById("successPanel").classList.remove("hidden");
      const goWork = document.getElementById("goWork");
      goWork.href = "/work-submit.html?contestId=" + contestId;
      B.toast("报名成功", "success");
    } catch (err) {
      B.toast(err.message || "报名失败", "error");
      btn.disabled = false;
      btn.textContent = "提交报名";
    }
  }

  document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("signupForm").addEventListener("submit", submit);
    init();
  });
})();
