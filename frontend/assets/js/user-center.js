(function () {
  const B = window.BTR;
  const state = {
    user: null,
    signups: [],
    works: [],
    contests: {},
  };

  async function init() {
    const user = B.requireAuth();
    if (!user) return;
    state.user = user;

    bindTabs();
    renderProfile(user);

    const [me, signups, works, contests] = await Promise.allSettled([
      B.api.get("/auth/me"),
      B.api.get("/signups/mine"),
      B.api.get("/works/mine"),
      B.api.get("/contests?page=1&size=50"),
    ]);

    if (me.status === "fulfilled") {
      state.user = me.value;
      B.storage.saveUser(me.value);
      renderProfile(me.value);
    }
    state.signups = signups.status === "fulfilled" ? signups.value || [] : [];
    state.works = works.status === "fulfilled" ? works.value || [] : [];
    if (contests.status === "fulfilled") {
      (contests.value.records || []).forEach((c) => (state.contests[c.id] = c));
    }

    renderSignupCount();
    renderSignups();
    renderWorks();
    renderOverview();
    loadAwards();
  }

  function bindTabs() {
    const buttons = document.querySelectorAll("#userNav [data-tab]");
    buttons.forEach((btn) => {
      btn.addEventListener("click", () => {
        const key = btn.dataset.tab;
        buttons.forEach((b) => b.classList.toggle("is-active", b === btn));
        document.querySelectorAll("[data-panel]").forEach((panel) => {
          panel.classList.toggle("is-active", panel.dataset.panel === key);
        });
        history.replaceState(null, "", "#" + key);
      });
    });
    const hash = (location.hash || "").replace("#", "");
    if (hash && document.querySelector('[data-tab="' + hash + '"]')) {
      document.querySelector('[data-tab="' + hash + '"]').click();
    }
  }

  function renderProfile(user) {
    const name = user.nickname || user.account || "选手";
    document.getElementById("profileAvatar").textContent = name.slice(0, 1);
    document.getElementById("profileName").textContent = name;
    document.getElementById("profileAccount").textContent = "@" + (user.account || "—");
    document.getElementById("profileRole").textContent = B.roleLabel(user.role);
    document.getElementById("profileStatus").textContent =
      user.status === "ACTIVE" ? "账号正常" : "账号已禁用";
    document.getElementById("profileStatus").className =
      "badge badge-plain " + (user.status === "ACTIVE" ? "badge-signup" : "badge-review");

    document.getElementById("profileMeta").innerHTML = `
      <div><span>账号</span><b>${B.escapeHtml(user.account || "—")}</b></div>
      <div><span>昵称</span><b>${B.escapeHtml(user.nickname || "—")}</b></div>
      <div><span>邮箱</span><b>${B.escapeHtml(user.email || "未填写")}</b></div>
      <div><span>手机号</span><b>${B.escapeHtml(user.phone || "未填写")}</b></div>
      <div><span>角色</span><b>${B.roleLabel(user.role)}</b></div>
      <div><span>注册时间</span><b>${B.formatDate(user.createdAt)}</b></div>`;
  }

  function renderSignupCount() {
    const active = state.signups.filter((s) => s.status !== "CANCELED").length;
    document.getElementById("navSignupCount").textContent = active || "";
    document.getElementById("navWorkCount").textContent =
      state.works.filter((w) => w.status === "SUBMITTED").length || "";
    document.getElementById("statSignups").textContent = active;
    document.getElementById("statWorks").textContent = state.works.filter(
      (w) => w.status === "SUBMITTED"
    ).length;
  }

  function renderOverview() {
    const host = document.getElementById("overviewGrid");
    const submitted = state.works.filter((w) => w.status === "SUBMITTED");
    const scored = submitted.filter((w) => Number(w.scoreCount) > 0);
    const avg =
      scored.length > 0
        ? scored.reduce((sum, w) => sum + Number(w.avgScore || 0), 0) / scored.length
        : 0;

    host.innerHTML = `
      <div class="stat-card">
        <div class="label"><span class="dot"></span>报名赛事</div>
        <div class="value">${state.signups.filter((s) => s.status !== "CANCELED").length}</div>
        <div class="foot">共 ${state.signups.length} 条报名记录</div>
      </div>
      <div class="stat-card">
        <div class="label"><span class="dot"></span>提交作品</div>
        <div class="value">${submitted.length}</div>
        <div class="foot">${scored.length} 件已被评分</div>
      </div>
      <div class="stat-card accent">
        <div class="label"><span class="dot"></span>平均得分</div>
        <div class="value">${scored.length ? avg.toFixed(2) : "—"}</div>
        <div class="foot">${scored.length ? "按已评分作品计算" : "等待评委打分"}</div>
      </div>`;
  }

  async function loadAwards() {
    const contests = Object.values(state.contests).filter(
      (c) => c.status === "PUBLISHED" || c.status === "FINISHED"
    );
    const myWorkIds = new Set(state.works.map((w) => w.id));
    let awards = 0;
    await Promise.all(
      contests.map(async (c) => {
        try {
          const rank = await B.api.get("/ranks?contestId=" + c.id);
          (rank.rows || []).forEach((row) => {
            if (myWorkIds.has(row.workId) && row.awardName) awards++;
          });
        } catch (e) {
          /* ignore */
        }
      })
    );
    document.getElementById("statAwards").textContent = awards;
  }

  function renderSignups() {
    const host = document.getElementById("signupList");
    if (!state.signups.length) {
      host.innerHTML = B.emptyState("还没有报名任何赛事", "去赛事列表挑一场感兴趣的比赛吧");
      return;
    }
    host.innerHTML = state.signups
      .map((s) => {
        const contest = state.contests[s.contestId];
        const info = B.signupStatusInfo(s.status);
        const canCancel = s.status !== "CANCELED" && contest && contest.status === "SIGNUP" && !s.workId;
        const workAction = s.workId
          ? `<a class="btn btn-ghost btn-sm" href="/work-submit.html?contestId=${s.contestId}">查看作品</a>`
          : contest && contest.status === "SIGNUP" && s.status === "APPROVED"
          ? `<a class="btn btn-primary btn-sm" href="/work-submit.html?contestId=${s.contestId}">提交作品</a>`
          : "";
        return `<div class="list-item">
          <div class="cover">
            <img src="${B.escapeHtml(
              (contest && contest.cover) || "/assets/img/cover-1.svg"
            )}" alt="" loading="lazy" />
          </div>
          <div class="info">
            <h4>${B.escapeHtml(s.contestTitle || "未知赛事")}</h4>
            <p>报名于 ${B.formatDate(s.createdAt, true)}${s.workTitle ? " · 作品：" + B.escapeHtml(s.workTitle) : ""}</p>
          </div>
          <span class="badge ${info.cls}">${info.label}</span>
          <div class="actions">
            <a class="btn btn-ghost btn-sm" href="/contest-detail.html?id=${s.contestId}">详情</a>
            ${workAction}
            ${canCancel ? `<button class="btn btn-outline btn-sm" data-cancel-signup="${s.id}">取消报名</button>` : ""}
          </div>
        </div>`;
      })
      .join("");

    host.querySelectorAll("[data-cancel-signup]").forEach((btn) => {
      btn.addEventListener("click", () => cancelSignup(Number(btn.dataset.cancelSignup)));
    });
  }

  async function cancelSignup(id) {
    if (!confirm("确定取消这条报名吗？取消后可重新报名。")) return;
    try {
      await B.api.del("/signups/" + id);
      B.toast("已取消报名", "success");
      const signups = await B.api.get("/signups/mine");
      state.signups = signups || [];
      renderSignupCount();
      renderSignups();
      renderOverview();
    } catch (e) {
      B.toast(e.message || "取消失败", "error");
    }
  }

  function renderWorks() {
    const host = document.getElementById("workList");
    if (!state.works.length) {
      host.innerHTML = B.emptyState("还没有提交作品", "报名赛事后即可提交你的作品");
      return;
    }
    host.innerHTML = state.works
      .map((w) => {
        const contest = state.contests[w.contestId];
        const canWithdraw = w.status === "SUBMITTED" && !w.locked;
        const scored = Number(w.scoreCount) > 0;
        return `<div class="list-item">
          <div class="cover">
            <img src="${B.escapeHtml(
              (contest && contest.cover) || "/assets/img/cover-1.svg"
            )}" alt="" loading="lazy" />
          </div>
          <div class="info">
            <h4>${B.escapeHtml(w.title)}</h4>
            <p>${B.escapeHtml(w.contestTitle || "未知赛事")} · ${
          w.status === "SUBMITTED" ? "已提交" : w.status === "WITHDRAWN" ? "已撤回" : w.status
        }${w.locked ? " · 已锁定" : ""}</p>
          </div>
          ${
            scored
              ? `<span class="badge badge-published">${Number(w.avgScore).toFixed(2)} 分 / ${w.scoreCount} 人</span>`
              : `<span class="badge badge-soft">待评分</span>`
          }
          <div class="actions">
            ${
              w.fileUrl
                ? `<a class="btn btn-ghost btn-sm" href="${B.escapeHtml(w.fileUrl)}" target="_blank" rel="noopener">查看文件</a>`
                : ""
            }
            ${
              w.status === "SUBMITTED" && !w.locked
                ? `<a class="btn btn-outline btn-sm" href="/work-submit.html?contestId=${w.contestId}">修改</a>`
                : ""
            }
            ${canWithdraw ? `<button class="btn btn-ghost btn-sm" data-withdraw="${w.id}">撤回</button>` : ""}
          </div>
        </div>`;
      })
      .join("");

    host.querySelectorAll("[data-withdraw]").forEach((btn) => {
      btn.addEventListener("click", () => withdraw(Number(btn.dataset.withdraw)));
    });
  }

  async function withdraw(id) {
    if (!confirm("撤回后作品将不再参与评审，确定继续吗？")) return;
    try {
      await B.api.del("/works/" + id);
      B.toast("作品已撤回", "success");
      const works = await B.api.get("/works/mine");
      state.works = works || [];
      renderSignupCount();
      renderWorks();
      renderOverview();
    } catch (e) {
      B.toast(e.message || "撤回失败", "error");
    }
  }

  document.addEventListener("DOMContentLoaded", init);
})();
