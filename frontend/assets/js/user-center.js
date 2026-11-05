(function () {
  const B = window.BTR;
  const state = {
    user: null,
    signups: [],
    works: [],
    contests: {},
    awards: { total: 0, items: [] },
  };

  async function init() {
    const user = B.requireAuth();
    if (!user) return;
    state.user = user;

    bindTabs();
    bindModals();
    bindEditProfile();
    renderProfile(user);

    const [me, signups, works, contests, awards] = await Promise.allSettled([
      B.api.get("/auth/me"),
      B.api.get("/signups/mine"),
      B.api.get("/works/mine"),
      B.api.get("/contests?page=1&size=50"),
      B.api.get("/awards/mine"),
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
    if (awards.status === "fulfilled") {
      state.awards = awards.value || { total: 0, items: [] };
    }

    renderSignupCount();
    renderSignups();
    renderWorks();
    renderOverview();
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

  /* ------------------------- 弹窗通用 ------------------------- */

  function bindModals() {
    document.querySelectorAll("[data-close-modal]").forEach((btn) => {
      btn.addEventListener("click", () => closeModal(btn.dataset.closeModal));
    });
    document.querySelectorAll(".uc-modal-overlay").forEach((overlay) => {
      overlay.addEventListener("click", (e) => {
        if (e.target === overlay) overlay.hidden = true;
      });
    });
  }

  function openModal(id) {
    const el = document.getElementById(id);
    if (el) el.hidden = false;
  }

  function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.hidden = true;
  }

  /* ------------------------- 个人信息 ------------------------- */

  function renderProfile(user) {
    const name = user.nickname || user.account || "选手";
    document.getElementById("profileAvatar").textContent = name.slice(0, 1);
    document.getElementById("profileName").textContent = name;
    document.getElementById("profileAccount").textContent = "@" + (user.account || "—");
    document.getElementById("profileRole").textContent = B.roleLabel(user.role);

    const link = document.getElementById("portfolioLink");
    if (link) {
      link.href = "/portfolio.html?id=" + user.id;
      link.style.display = "";
    }

    document.getElementById("profileMeta").innerHTML = `
      <div><span>账号</span><b>${B.escapeHtml(user.account || "—")}</b></div>
      <div><span>昵称</span><b>${B.escapeHtml(user.nickname || "—")}</b></div>
      <div><span>邮箱</span><b>${B.escapeHtml(user.email || "未填写")}</b></div>
      <div><span>手机号</span><b>${B.escapeHtml(user.phone || "未填写")}</b></div>`;
  }

  function bindEditProfile() {
    const modalId = "editProfileModal";
    document.getElementById("editProfileBtn").addEventListener("click", () => {
      document.getElementById("editNickname").value = state.user.nickname || "";
      document.getElementById("editPhone").value = state.user.phone || "";
      openModal(modalId);
      document.getElementById("editNickname").focus();
    });
    document.getElementById("saveProfileBtn").addEventListener("click", async () => {
      const nickname = document.getElementById("editNickname").value.trim();
      const phone = document.getElementById("editPhone").value.trim();
      if (!nickname) {
        B.toast("昵称不能为空", "error");
        return;
      }
      try {
        const me = await B.api.put("/auth/me", { nickname, phone });
        state.user = me;
        B.storage.saveUser(me);
        renderProfile(me);
        closeModal(modalId);
        B.toast("个人信息已更新", "success");
      } catch (e) {
        B.toast(e.message || "保存失败", "error");
      }
    });
  }

  /* ------------------------- 统计与概览 ------------------------- */

  function renderSignupCount() {
    const active = state.signups.filter((s) => s.status !== "CANCELED").length;
    document.getElementById("navSignupCount").textContent = active || "";
    document.getElementById("navWorkCount").textContent =
      state.works.filter((w) => w.status === "SUBMITTED").length || "";
    document.getElementById("statSignups").textContent = active;
    document.getElementById("statWorks").textContent = state.works.filter(
      (w) => w.status === "SUBMITTED"
    ).length;
    document.getElementById("statAwards").textContent = state.awards.total;
  }

  function renderOverview() {
    const host = document.getElementById("overviewGrid");
    const submitted = state.works.filter((w) => w.status === "SUBMITTED");

    host.innerHTML = `
      <div class="stat-card">
        <div class="label"><span class="dot"></span>报名赛事</div>
        <div class="value">${state.signups.filter((s) => s.status !== "CANCELED").length}</div>
        <div class="foot">共 ${state.signups.length} 条报名记录</div>
      </div>
      <div class="stat-card">
        <div class="label"><span class="dot"></span>提交作品</div>
        <div class="value">${submitted.length}</div>
        <div class="foot">${state.works.length} 件作品</div>
      </div>
      <div class="stat-card accent" style="cursor:pointer" data-open-awards>
        <div class="label"><span class="dot"></span>得奖次数</div>
        <div class="value">${state.awards.total}</div>
        <div class="foot">点击查看获奖明细</div>
      </div>`;

    host.querySelector("[data-open-awards]").addEventListener("click", openAwardModal);
  }

  /* ------------------------- 获奖明细与 PDF ------------------------- */

  function openAwardModal() {
    const host = document.getElementById("awardModalBody");
    const items = state.awards.items || [];
    if (!items.length) {
      host.innerHTML = B.emptyState("暂无获奖记录", "作品获奖并公示后，这里会展示你的获奖证书");
    } else {
      host.innerHTML = items
        .map((a) => {
          const medalCls =
            a.rankNo === 1 ? "gold" : a.rankNo === 2 ? "silver" : a.rankNo === 3 ? "bronze" : "other";
          const medalText =
            a.rankNo && a.rankNo <= 3 ? a.rankNo : (a.awardName || "奖").slice(0, 1);
          const prize = B.formatPrize(a) ? " · " + B.escapeHtml(B.formatPrize(a)) : "";
          return `<div class="award-item">
            <div class="award-medal ${medalCls}">${B.escapeHtml(medalText)}</div>
            <div class="award-info">
              <h4>${B.escapeHtml(a.awardName || "获奖")}${
                a.rankNo ? ` <span class="award-tag">第 ${a.rankNo} 名</span>` : ""
              }</h4>
              <p>${B.escapeHtml(a.contestTitle || "未知赛事")} · 作品《${B.escapeHtml(a.workTitle || "—")}》${prize}</p>
              <p style="margin-top:2px">颁发于 ${B.formatDate(a.publishedAt)}</p>
            </div>
            <button type="button" class="btn btn-outline btn-sm" data-download-pdf="${a.contestId}" data-title="${B.escapeHtml(a.contestTitle || "证书")}">下载 PDF</button>
          </div>`;
        })
        .join("");
    }
    host.querySelectorAll("[data-download-pdf]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const contestId = btn.dataset.downloadPdf;
        const title = btn.dataset.title || "获奖证书";
        B.toast("正在生成获奖证书…");
        B.download("/awards/mine/pdf?contestId=" + contestId, "获奖证书-" + title + ".pdf").catch(
          (e) => B.toast(e.message || "下载失败", "error")
        );
      });
    });
    openModal("awardModal");
  }

  /* ------------------------- 报名 / 作品（沿用原逻辑） ------------------------- */

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
          : contest && (contest.status === "SIGNUP" || contest.status === "REVIEW") && s.status === "APPROVED"
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
