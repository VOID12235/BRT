/* ==========================================================================
   BTR 赛事平台 · 管理后台
   ========================================================================== */
(function () {
  const B = window.BTR;
  const state = {
    user: null,
    contests: [],
    page: {},
  };

  const ICONS = {
    dashboard:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>',
    contest:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M8 21h8M12 17v4"/><path d="M7 4h10v5a5 5 0 0 1-10 0z"/><path d="M17 5h3v2a3 3 0 0 1-3 3M7 5H4v2a3 3 0 0 0 3 3"/></svg>',
    signup:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M19 8v6M22 11h-6"/></svg>',
    work:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 4h16v16H4z"/><path d="M8 9h8M8 13h5"/></svg>',
    judge:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>',
    award:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="8" r="6"/><path d="M8.2 13.2 7 22l5-3 5 3-1.2-8.8"/></svg>',
    notice:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 5h16v14H4z"/><path d="M8 9h8M8 13h8M8 17h5"/></svg>',
    export:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 3v12"/><path d="m7 10 5 5 5-5"/><path d="M5 21h14"/></svg>',
    settings:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09a1.65 1.65 0 0 0-1-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09a1.65 1.65 0 0 0 1.51-1 1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33h0a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51h0a1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82v0a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>',
    users:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/></svg>',
    logs:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/><path d="M8 13h8M8 17h5"/></svg>',
    mails:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-10 6L2 7"/></svg>',
  };

  const NAV = [
    { group: "总览", items: [{ key: "dashboard", label: "仪表盘", href: "/admin/admin-index.html", icon: "dashboard" }] },
    {
      group: "赛事",
      items: [
        { key: "contests", label: "赛事管理", href: "/admin/contest-manage.html", icon: "contest" },
        { key: "signups", label: "报名管理", href: "/admin/signup-manage.html", icon: "signup" },
        { key: "works", label: "作品管理", href: "/admin/work-manage.html", icon: "work" },
        { key: "awards", label: "奖项管理", href: "/admin/award-manage.html", icon: "award" },
      ],
    },
    {
      group: "评审与内容",
      items: [
        { key: "judges", label: "评委分配", href: "/admin/judge-manage.html", icon: "judge" },
        { key: "notices", label: "公告管理", href: "/admin/notice-manage.html", icon: "notice" },
        { key: "export", label: "数据导出", href: "/admin/export.html", icon: "export" },
      ],
    },
    {
      group: "系统",
      items: [
        { key: "settings", label: "基础配置", href: "/admin/settings.html", icon: "settings" },
        { key: "userManage", label: "账号权限", href: "/admin/user-manage.html", icon: "users" },
        { key: "logs", label: "操作日志", href: "/admin/logs.html", icon: "logs" },
        { key: "mails", label: "邮件记录", href: "/admin/mails.html", icon: "mails" },
      ],
    },
  ];

  /* ------------------------------- 框架 ------------------------------- */
  function mountShell() {
    const page = document.body.dataset.page;
    const title = document.body.dataset.title || "管理后台";
    const crumb = document.body.dataset.crumb || "";
    const sidebar = document.getElementById("adminSidebar");
    const topbar = document.getElementById("adminTopbar");

    if (sidebar) {
      sidebar.innerHTML = `
        <div class="admin-brand">
          <span class="brand-mark">B</span>
          <div><span>BTR</span><small>Admin Console</small></div>
        </div>
        <nav class="admin-nav">
          ${NAV.map(
            (group) => `
            <div class="group-label">${group.group}</div>
            ${group.items
              .map(
                (item) => `<a href="${item.href}" class="${item.key === page ? "is-active" : ""}">
                  ${ICONS[item.icon] || ""}<span>${item.label}</span>
                </a>`
              )
              .join("")}`
          ).join("")}
        </nav>
        <div class="admin-sidebar-foot">
          当前版本 v1.0.0<br />© 2026 BTR Platform
        </div>`;
    }

    if (topbar) {
      topbar.innerHTML = `
        <button class="btn btn-ghost btn-icon admin-menu-btn" id="adminMenuBtn" aria-label="打开菜单" style="display:none">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M4 7h16M4 12h16M4 17h16"/></svg>
        </button>
        <div>
          <h1>${B.escapeHtml(title)}</h1>
          <div class="crumb">${B.escapeHtml(crumb)}</div>
        </div>
        <div class="admin-topbar-actions">
          <a class="btn btn-ghost btn-sm" href="/index.html" target="_blank" rel="noopener">前台首页</a>
          <div class="mini-user">
            <span class="avatar">${B.escapeHtml((state.user.nickname || "A").slice(0, 1))}</span>
            <span>${B.escapeHtml(state.user.nickname || state.user.account || "管理员")}</span>
          </div>
          <button class="btn btn-outline btn-sm" id="adminLogout">退出</button>
        </div>`;

      document.getElementById("adminLogout").addEventListener("click", async () => {
        try {
          await B.api.post("/auth/logout");
        } catch (e) {
          /* ignore */
        }
        B.storage.clear();
        location.href = "/login.html";
      });

      const menuBtn = document.getElementById("adminMenuBtn");
      const backdrop = document.getElementById("adminBackdrop");
      if (menuBtn && sidebar && backdrop) {
        menuBtn.addEventListener("click", () => {
          sidebar.classList.add("is-open");
          backdrop.classList.add("is-open");
        });
        backdrop.addEventListener("click", () => {
          sidebar.classList.remove("is-open");
          backdrop.classList.remove("is-open");
        });
      }
    }
  }

  /* ------------------------------- 通用 ------------------------------- */
  async function loadContests() {
    try {
      const data = await B.api.get("/contests?all=true&page=1&size=100");
      state.contests = data.records || [];
    } catch (e) {
      state.contests = [];
    }
  }

  function fillContestSelect(select, options = {}) {
    if (!select) return;
    const { placeholder = "全部赛事", only = null } = options;
    const current = select.value;
    const list = only ? state.contests.filter((c) => only.includes(c.status)) : state.contests;
    select.innerHTML =
      `<option value="">${B.escapeHtml(placeholder)}</option>` +
      list
        .map((c) => `<option value="${c.id}">${B.escapeHtml(c.title)}</option>`)
        .join("");
    if (current && list.some((c) => String(c.id) === current)) select.value = current;
  }

  function contestTitle(id) {
    const contest = state.contests.find((c) => String(c.id) === String(id));
    return contest ? contest.title : "—";
  }

  function renderPagination(container, page, total, size, onChange) {
    if (!container) return;
    const pages = Math.max(1, Math.ceil(total / size));
    const current = Math.min(Math.max(1, page), pages);
    const numbers = new Set([1, pages, current, current - 1, current + 1]);
    const sorted = [...numbers].filter((n) => n >= 1 && n <= pages).sort((a, b) => a - b);
    const html = [`<button type="button" data-page="${current - 1}" ${current <= 1 ? "disabled" : ""}>‹</button>`];
    let last = 0;
    for (const n of sorted) {
      if (last && n - last > 1) html.push('<span class="ellipsis">…</span>');
      html.push(`<button type="button" data-page="${n}" class="${n === current ? "is-active" : ""}">${n}</button>`);
      last = n;
    }
    html.push(`<button type="button" data-page="${current + 1}" ${current >= pages ? "disabled" : ""}>›</button>`);
    container.innerHTML = html.join("");
    container.querySelectorAll("button[data-page]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const p = Number(btn.dataset.page);
        if (p >= 1 && p <= pages && p !== current) onChange(p);
      });
    });
  }

  function openModal(opts) {
    const backdrop = document.createElement("div");
    backdrop.className = "modal-backdrop";
    backdrop.innerHTML = `
      <div class="modal ${opts.size || ""}">
        <div class="modal-head">
          <h3>${B.escapeHtml(opts.title || "")}</h3>
          <button type="button" class="modal-close" data-close aria-label="关闭">×</button>
        </div>
        <form class="modal-body" id="adminModalForm">${opts.body || ""}</form>
        <div class="modal-foot">
          <button type="button" class="btn btn-ghost" data-close>取消</button>
          <button type="submit" form="adminModalForm" class="btn btn-primary" id="adminModalSubmit">${
      B.escapeHtml(opts.submitText || "保存")
    }</button>
        </div>
      </div>`;
    document.body.appendChild(backdrop);
    document.body.style.overflow = "hidden";
    requestAnimationFrame(() => backdrop.classList.add("is-open"));

    const form = backdrop.querySelector("#adminModalForm");
    const submit = backdrop.querySelector("#adminModalSubmit");
    const close = () => {
      backdrop.classList.remove("is-open");
      document.body.style.overflow = "";
      setTimeout(() => backdrop.remove(), 220);
    };

    backdrop.querySelectorAll("[data-close]").forEach((btn) => btn.addEventListener("click", close));
    backdrop.addEventListener("click", (e) => {
      if (e.target === backdrop) close();
    });
    document.addEventListener("keydown", function esc(e) {
      if (e.key === "Escape") {
        close();
        document.removeEventListener("keydown", esc);
      }
    });

    form.addEventListener("submit", async (e) => {
      e.preventDefault();
      submit.disabled = true;
      const label = submit.textContent;
      submit.textContent = "提交中…";
      try {
        await opts.onSubmit(form);
        close();
      } catch (err) {
        B.toast(err.message || "操作失败", "error");
        submit.disabled = false;
        submit.textContent = label;
      }
    });

    if (opts.onMount) opts.onMount(form, backdrop);
    return { close, form, backdrop };
  }

  function datetimeValue(value) {
    if (!value) return "";
    return String(value).replace(" ", "T").slice(0, 16);
  }

  function formValue(form, name) {
    const value = form.querySelector('[name="' + name + '"]');
    return value ? value.value : "";
  }

  function confirmAction(message, action) {
    if (confirm(message)) action();
  }

  /* ------------------------------- 仪表盘 ------------------------------- */
  async function initDashboard() {
    document.getElementById("adminGreeting").textContent =
      state.user.nickname || state.user.account || "管理员";
    try {
      const data = await B.api.get("/admin/dashboard");
      renderDashboardStats(data);
      renderTrend(data.trend || []);
      renderRecentSignups(data.recentSignups || []);
      renderPending(data);
    } catch (e) {
      B.toast(e.message || "仪表盘加载失败", "error");
    }
  }

  function renderDashboardStats(data) {
    const values = document.querySelectorAll("#dashStats .stat-card .value");
    const list = [data.users, data.contests, data.signups, data.works];
    values.forEach((el, i) => (el.textContent = Number(list[i] || 0).toLocaleString("zh-CN")));
  }

  function renderTrend(trend) {
    const host = document.getElementById("trendBars");
    if (!host) return;
    const max = Math.max(1, ...trend.map((p) => p.count));
    const total = trend.reduce((sum, p) => sum + p.count, 0);
    document.getElementById("trendTotal").textContent = "近 7 日 " + total + " 人";
    host.innerHTML = trend
      .map((point) => {
        const date = B.parseDate(point.date);
        const label = date ? date.getMonth() + 1 + "/" + date.getDate() : point.date;
        const height = Math.max(4, Math.round((point.count / max) * 100));
        return `<div class="trend-bar">
          <div class="bar ${point.count === max && max > 0 ? "is-peak" : ""}" style="height:${height}%">
            <span>${point.count}</span>
          </div>
          <em>${label}</em>
        </div>`;
      })
      .join("");
  }

  function renderRecentSignups(list) {
    const tbody = document.getElementById("recentSignups");
    if (!tbody) return;
    if (!list.length) {
      tbody.innerHTML = '<tr><td colspan="5" class="muted center" style="padding:28px">暂无报名记录</td></tr>';
      return;
    }
    tbody.innerHTML = list
      .map((s) => {
        const info = B.signupStatusInfo(s.status);
        return `<tr>
          <td class="title-cell">${B.escapeHtml(s.realName || s.nickname || "—")}</td>
          <td>${B.escapeHtml(s.contestTitle || "—")}</td>
          <td>${B.escapeHtml(s.school || "—")}</td>
          <td>${B.formatDate(s.createdAt)}</td>
          <td><span class="badge ${info.cls}">${info.label}</span></td>
        </tr>`;
      })
      .join("");
  }

  function renderPending(data) {
    const host = document.getElementById("dashPending");
    if (!host) return;
    host.innerHTML = `
      <div><span>待评审作品</span><b>${data.pendingReviews || 0}</b></div>
      <div><span>评委人数</span><b>${data.judges || 0}</b></div>
      <div><span>选手人数</span><b>${data.players || 0}</b></div>
      <div><span>公告数量</span><b>${data.notices || 0}</b></div>`;
  }

  /* ------------------------------- 赛事管理 ------------------------------- */
  const contestState = { page: 1, size: 10, keyword: "", status: "" };

  async function initContests() {
    fillContestSelect(document.getElementById("contestStatusFilter"));
    document.getElementById("newContestBtn").addEventListener("click", () => openContestModal());
    const keyword = document.getElementById("contestKeyword");
    keyword.addEventListener(
      "input",
      B.debounce(() => {
        contestState.keyword = keyword.value.trim();
        contestState.page = 1;
        loadContestTable();
      }, 320)
    );
    const status = document.getElementById("contestStatusFilter");
    if (status) {
      status.addEventListener("change", () => {
        contestState.status = status.value;
        contestState.page = 1;
        loadContestTable();
      });
    }
    loadContestTable();
  }

  async function loadContestTable() {
    const tbody = document.getElementById("contestTable");
    tbody.innerHTML = '<tr><td colspan="8"><div class="skeleton" style="height: 60px"></div></td></tr>';
    try {
      const params = new URLSearchParams({
        all: "true",
        page: contestState.page,
        size: contestState.size,
      });
      if (contestState.keyword) params.set("keyword", contestState.keyword);
      if (contestState.status) params.set("status", contestState.status);
      const data = await B.api.get("/contests?" + params.toString());
      const list = data.records || [];
      document.getElementById("contestCount").textContent = "共 " + data.total + " 场赛事";
      document.getElementById("contestFoot").textContent =
        "第 " + contestState.page + " / " + Math.max(1, Math.ceil(data.total / contestState.size)) + " 页";
      renderContestRows(list);
      renderPagination(
        document.getElementById("contestPagination"),
        contestState.page,
        data.total,
        contestState.size,
        (page) => {
          contestState.page = page;
          loadContestTable();
        }
      );
    } catch (e) {
      tbody.innerHTML = `<tr><td colspan="8" class="muted center" style="padding:28px">${B.escapeHtml(
        e.message || "加载失败"
      )}</td></tr>`;
    }
  }

  function renderContestRows(list) {
    const tbody = document.getElementById("contestTable");
    if (!list.length) {
      tbody.innerHTML = '<tr><td colspan="8" class="muted center" style="padding:28px">暂无赛事</td></tr>';
      return;
    }
    tbody.innerHTML = list
      .map((c) => {
        const info = B.statusInfo(c.status);
        const prize = Number(c.prizeAmount || 0);
        const prizeHtml =
          prize > 0
            ? `¥${prize.toLocaleString("zh-CN", { maximumFractionDigits: 0 })}`
            : '<span class="muted">—</span>';
        return `<tr>
          <td class="title-cell">
            ${B.escapeHtml(c.title)}
            ${c.isPublic ? "" : '<span class="badge badge-soft badge-plain" style="margin-left:6px">未公开</span>'}
          </td>
          <td><span class="badge ${info.cls}">${info.label}</span></td>
          <td class="num">${prizeHtml}</td>
          <td class="num">${c.signupCount || 0}</td>
          <td class="num">${c.workCount || 0}</td>
          <td>${B.formatDate(c.signupStart)} — ${B.formatDate(c.signupEnd)}</td>
          <td>${B.formatDate(c.reviewStart)} — ${B.formatDate(c.reviewEnd)}</td>
          <td>
            <div class="actions-cell">
              <button class="btn btn-outline btn-xs" data-edit="${c.id}">编辑</button>
              <button class="btn btn-ghost btn-xs" data-status="${c.id}">状态</button>
              <a class="btn btn-ghost btn-xs" href="/admin/award-manage.html?contestId=${c.id}">奖项</a>
              <button class="btn btn-ghost btn-xs" data-delete="${c.id}" style="color:var(--brand)">删除</button>
            </div>
          </td>
        </tr>`;
      })
      .join("");

    tbody.querySelectorAll("[data-edit]").forEach((btn) => {
      const contest = list.find((c) => c.id === Number(btn.dataset.edit));
      btn.addEventListener("click", () => openContestModal(contest));
    });
    tbody.querySelectorAll("[data-status]").forEach((btn) => {
      const contest = list.find((c) => c.id === Number(btn.dataset.status));
      btn.addEventListener("click", () => openStatusModal(contest));
    });
    tbody.querySelectorAll("[data-delete]").forEach((btn) => {
      const id = Number(btn.dataset.delete);
      btn.addEventListener("click", () =>
        confirmAction("确定删除该赛事吗？已有报名记录的赛事无法删除。", async () => {
          try {
            await B.api.del("/contests/" + id);
            B.toast("赛事已删除", "success");
            await loadContests();
            loadContestTable();
          } catch (e) {
            B.toast(e.message || "删除失败", "error");
          }
        })
      );
    });
  }

  function openContestModal(contest) {
    const isEdit = Boolean(contest);
    const c = contest || {};
    openModal({
      title: isEdit ? "编辑赛事" : "新建赛事",
      size: "modal-lg",
      submitText: isEdit ? "保存修改" : "创建赛事",
      body: `
        <div class="form-grid">
          <div class="field span-2">
            <label>赛事名称 *</label>
            <input class="input" name="title" maxlength="160" required value="${B.escapeHtml(c.title || "")}" placeholder="例如：2026 BTR 全国大学生算法挑战赛" />
          </div>
          <div class="field">
            <label>封面</label>
            <select class="select" name="cover">
              ${[1, 2, 3, 4]
                .map(
                  (i) =>
                    `<option value="/assets/img/cover-${i}.svg" ${
                      (c.cover || "/assets/img/cover-1.svg") === "/assets/img/cover-" + i + ".svg" ? "selected" : ""
                    }>封面 ${i}</option>`
                )
                .join("")}
            </select>
          </div>
          <div class="field">
            <label>赛事状态</label>
            <select class="select" name="status">
              ${[
                ["DRAFT", "筹备中"],
                ["SIGNUP", "报名中"],
                ["REVIEW", "评审中"],
                ["PUBLISHED", "已公示"],
                ["FINISHED", "已结束"],
              ]
                .map(([v, l]) => `<option value="${v}" ${(c.status || "DRAFT") === v ? "selected" : ""}>${l}</option>`)
                .join("")}
            </select>
          </div>
          <div class="field">
            <label>奖金总额（元）</label>
            <input class="input" type="number" name="prizeAmount" min="0" max="999999999.99" step="0.01" placeholder="例如 10000，留空为无" value="${
              c.prizeAmount != null && Number(c.prizeAmount) > 0 ? Number(c.prizeAmount) : ""
            }" />
          </div>
          <div class="field span-2">
            <label>赛事简介</label>
            <textarea class="textarea" name="summary" maxlength="500" style="min-height:70px" placeholder="一句话说明赛事主题">${B.escapeHtml(
              c.summary || ""
            )}</textarea>
          </div>
          <div class="field span-2">
            <label>赛事介绍</label>
            <textarea class="textarea" name="description" style="min-height:110px" placeholder="详细介绍赛事背景、赛制与亮点">${B.escapeHtml(
              c.description || ""
            )}</textarea>
          </div>
          <div class="field span-2">
            <label>参赛规则</label>
            <textarea class="textarea" name="rules" style="min-height:110px" placeholder="每条规则一行">${B.escapeHtml(
              c.rules || ""
            )}</textarea>
          </div>
          <div class="field"><label>报名开始</label><input class="input" type="datetime-local" name="signupStart" value="${datetimeValue(
            c.signupStart
          )}" /></div>
          <div class="field"><label>报名截止</label><input class="input" type="datetime-local" name="signupEnd" value="${datetimeValue(
            c.signupEnd
          )}" /></div>
          <div class="field"><label>评审开始</label><input class="input" type="datetime-local" name="reviewStart" value="${datetimeValue(
            c.reviewStart
          )}" /></div>
          <div class="field"><label>评审结束</label><input class="input" type="datetime-local" name="reviewEnd" value="${datetimeValue(
            c.reviewEnd
          )}" /></div>
          <label class="check span-2"><input type="checkbox" name="isPublic" ${
            c.isPublic === false ? "" : "checked"
          } /> 在公开站展示该赛事</label>
        </div>`,
      onSubmit: async (form) => {
        const payload = {
          title: formValue(form, "title").trim(),
          cover: formValue(form, "cover"),
          summary: formValue(form, "summary"),
          description: formValue(form, "description"),
          rules: formValue(form, "rules"),
          prizeAmount: formValue(form, "prizeAmount") === "" ? null : Number(formValue(form, "prizeAmount")),
          signupStart: formValue(form, "signupStart") || null,
          signupEnd: formValue(form, "signupEnd") || null,
          reviewStart: formValue(form, "reviewStart") || null,
          reviewEnd: formValue(form, "reviewEnd") || null,
          status: formValue(form, "status"),
          isPublic: form.querySelector('[name="isPublic"]').checked,
        };
        if (!payload.title) throw new Error("请输入赛事名称");
        if (isEdit) {
          await B.api.put("/contests/" + c.id, payload);
          B.toast("赛事已更新", "success");
        } else {
          await B.api.post("/contests", payload);
          B.toast("赛事已创建", "success");
        }
        await loadContests();
        loadContestTable();
      },
    });
  }

  function openStatusModal(contest) {
    const options = [
      ["DRAFT", "筹备中", "不对公众开放，可继续完善信息"],
      ["SIGNUP", "报名中", "开放报名与作品提交"],
      ["REVIEW", "评审中", "锁定全部作品，评委开始打分"],
      ["PUBLISHED", "已公示", "生成并固化获奖名单，发送通知"],
      ["FINISHED", "已结束", "赛事归档，保留榜单展示"],
    ];
    openModal({
      title: "变更赛事状态 · " + contest.title,
      submitText: "确认变更",
      body: `
        <div class="field">
          <label>目标状态</label>
          <select class="select" name="status">
            ${options
              .map(
                ([v, l, desc]) =>
                  `<option value="${v}" ${contest.status === v ? "selected" : ""}>${l} — ${desc}</option>`
              )
              .join("")}
          </select>
        </div>
        <div class="alert alert-warn" style="margin-top: 16px">
          进入「评审中」会锁定全部作品；进入「已公示」会按当前榜单固化获奖名单。
        </div>`,
      onSubmit: async (form) => {
        const status = formValue(form, "status");
        if (status === contest.status) {
          B.toast("状态未变化", "info");
          return;
        }
        await B.api.post("/contests/" + contest.id + "/status", { status });
        B.toast("状态已更新", "success");
        await loadContests();
        loadContestTable();
      },
    });
  }

  /* ------------------------------- 报名管理 ------------------------------- */
  const signupState = { page: 1, size: 12, contestId: "", status: "" };

  async function initSignups() {
    fillContestSelect(document.getElementById("signupContestFilter"));
    document.getElementById("signupContestFilter").addEventListener("change", (e) => {
      signupState.contestId = e.target.value;
      signupState.page = 1;
      loadSignupTable();
    });
    document.getElementById("signupStatusFilter").addEventListener("change", (e) => {
      signupState.status = e.target.value;
      signupState.page = 1;
      loadSignupTable();
    });
    document.getElementById("exportSignupsBtn").addEventListener("click", () => {
      const suffix = signupState.contestId ? "?contestId=" + signupState.contestId : "";
      B.download("/admin/export/signups" + suffix, "报名名单.xlsx").catch((e) =>
        B.toast(e.message || "导出失败", "error")
      );
    });
    loadSignupTable();
  }

  async function loadSignupTable() {
    const tbody = document.getElementById("signupTable");
    tbody.innerHTML = '<tr><td colspan="9"><div class="skeleton" style="height: 60px"></div></td></tr>';
    try {
      const params = new URLSearchParams({ page: signupState.page, size: signupState.size });
      if (signupState.contestId) params.set("contestId", signupState.contestId);
      if (signupState.status) params.set("status", signupState.status);
      const data = await B.api.get("/admin/signups?" + params.toString());
      const list = data.records || [];
      document.getElementById("signupCount").textContent = "共 " + data.total + " 条报名";
      document.getElementById("signupFoot").textContent =
        "第 " + signupState.page + " / " + Math.max(1, Math.ceil(data.total / signupState.size)) + " 页";

      if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="9" class="muted center" style="padding:28px">暂无报名记录</td></tr>';
      } else {
        tbody.innerHTML = list
          .map((s) => {
            const info = B.signupStatusInfo(s.status);
            return `<tr>
              <td class="title-cell">${B.escapeHtml(s.realName || "—")}<span class="tiny muted" style="display:block">${B.escapeHtml(
              s.nickname || ""
            )}</span></td>
              <td>${B.escapeHtml(s.account || "—")}</td>
              <td>${B.escapeHtml(s.contestTitle || "—")}</td>
              <td>${B.escapeHtml(s.school || "—")}</td>
              <td>${B.escapeHtml(s.contact || "—")}</td>
              <td>${s.workTitle ? B.escapeHtml(s.workTitle) : '<span class="muted">未提交</span>'}</td>
              <td>${B.formatDate(s.createdAt, true)}</td>
              <td><span class="badge ${info.cls}">${info.label}</span></td>
              <td>
                <div class="actions-cell">
                  ${
                    s.status !== "APPROVED"
                      ? `<button class="btn btn-outline btn-xs" data-approve="${s.id}">通过</button>`
                      : ""
                  }
                  ${
                    s.status !== "CANCELED"
                      ? `<button class="btn btn-ghost btn-xs" data-cancel="${s.id}" style="color:var(--brand)">取消</button>`
                      : ""
                  }
                </div>
              </td>
            </tr>`;
          })
          .join("");
        bindSignupActions(tbody);
      }

      renderPagination(
        document.getElementById("signupPagination"),
        signupState.page,
        data.total,
        signupState.size,
        (page) => {
          signupState.page = page;
          loadSignupTable();
        }
      );
    } catch (e) {
      tbody.innerHTML = `<tr><td colspan="9" class="muted center" style="padding:28px">${B.escapeHtml(
        e.message || "加载失败"
      )}</td></tr>`;
    }
  }

  function bindSignupActions(tbody) {
    tbody.querySelectorAll("[data-approve]").forEach((btn) =>
      btn.addEventListener("click", () => updateSignupStatus(Number(btn.dataset.approve), "APPROVED")))
    ;
    tbody.querySelectorAll("[data-cancel]").forEach((btn) =>
      btn.addEventListener("click", () =>
        confirmAction("确定取消这条报名吗？", () => updateSignupStatus(Number(btn.dataset.cancel), "CANCELED")))
    );
  }

  async function updateSignupStatus(id, status) {
    try {
      await B.api.put("/admin/signups/" + id + "/status", { status });
      B.toast("报名状态已更新", "success");
      loadSignupTable();
    } catch (e) {
      B.toast(e.message || "更新失败", "error");
    }
  }

  /* ------------------------------- 作品管理 ------------------------------- */
  const workState = { page: 1, size: 12, contestId: "", keyword: "" };

  async function initWorks() {
    fillContestSelect(document.getElementById("workContestFilter"));
    document.getElementById("workContestFilter").addEventListener("change", (e) => {
      workState.contestId = e.target.value;
      workState.page = 1;
      loadWorkTable();
    });
    const keyword = document.getElementById("workKeyword");
    keyword.addEventListener(
      "input",
      B.debounce(() => {
        workState.keyword = keyword.value.trim();
        workState.page = 1;
        loadWorkTable();
      }, 320)
    );
    loadWorkTable();
  }

  async function loadWorkTable() {
    const tbody = document.getElementById("workTable");
    tbody.innerHTML = '<tr><td colspan="8"><div class="skeleton" style="height: 60px"></div></td></tr>';
    try {
      const params = new URLSearchParams({ page: workState.page, size: workState.size });
      if (workState.contestId) params.set("contestId", workState.contestId);
      if (workState.keyword) params.set("keyword", workState.keyword);
      const data = await B.api.get("/admin/works?" + params.toString());
      const list = data.records || [];
      document.getElementById("workCount").textContent = "共 " + data.total + " 件作品";
      document.getElementById("workFoot").textContent =
        "第 " + workState.page + " / " + Math.max(1, Math.ceil(data.total / workState.size)) + " 页";

      if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="8" class="muted center" style="padding:28px">暂无作品</td></tr>';
      } else {
        tbody.innerHTML = list
          .map((w) => {
            const scored = Number(w.scoreCount) > 0;
            return `<tr>
              <td class="title-cell">${B.escapeHtml(w.title)}${
              w.description ? `<span class="tiny muted" style="display:block">${B.escapeHtml(w.description)}</span>` : ""
            }</td>
              <td>${B.escapeHtml(w.contestTitle || "—")}</td>
              <td>${B.escapeHtml(w.author || "—")}</td>
              <td>${B.escapeHtml(w.school || "—")}</td>
              <td>${scored ? `<b class="mono">${Number(w.avgScore).toFixed(2)}</b> <span class="tiny muted">/ ${w.scoreCount}</span>` : '<span class="muted">—</span>'}</td>
              <td>${B.formatDate(w.submittedAt, true)}</td>
              <td>${
                w.locked
                  ? '<span class="badge badge-finished">已锁定</span>'
                  : '<span class="badge badge-signup">可修改</span>'
              }</td>
              <td>
                <div class="actions-cell">
                  ${
                    w.fileUrl
                      ? `<a class="btn btn-ghost btn-xs" href="${B.escapeHtml(w.fileUrl)}" target="_blank" rel="noopener">查看文件</a>`
                      : ""
                  }
                  <button class="btn btn-outline btn-xs" data-lock="${w.id}" data-locked="${w.locked}">${
              w.locked ? "解锁" : "锁定"
            }</button>
                </div>
              </td>
            </tr>`;
          })
          .join("");
        tbody.querySelectorAll("[data-lock]").forEach((btn) => {
          btn.addEventListener("click", async () => {
            const locked = btn.dataset.locked !== "true";
            try {
              await B.api.put("/admin/works/" + btn.dataset.lock + "/lock", { locked });
              B.toast(locked ? "作品已锁定" : "作品已解锁", "success");
              loadWorkTable();
            } catch (e) {
              B.toast(e.message || "操作失败", "error");
            }
          });
        });
      }

      renderPagination(
        document.getElementById("workPagination"),
        workState.page,
        data.total,
        workState.size,
        (page) => {
          workState.page = page;
          loadWorkTable();
        }
      );
    } catch (e) {
      tbody.innerHTML = `<tr><td colspan="8" class="muted center" style="padding:28px">${B.escapeHtml(
        e.message || "加载失败"
      )}</td></tr>`;
    }
  }

  /* ------------------------------- 评委分配 ------------------------------- */
  const assignState = { contestId: "" };
  let judgesCache = [];

  async function initJudges() {
    fillContestSelect(document.getElementById("assignContestFilter"));
    document.getElementById("assignContestFilter").addEventListener("change", (e) => {
      assignState.contestId = e.target.value;
      loadAssignments();
    });
    document.getElementById("assignBtn").addEventListener("click", openAssignModal);
    await loadJudges();
    loadAssignments();
  }

  async function loadJudges() {
    const host = document.getElementById("judgeCards");
    try {
      judgesCache = await B.api.get("/admin/judges");
      if (!judgesCache.length) {
        host.innerHTML = '<div class="card side-card muted small" style="grid-column:1/-1">暂无评委账号</div>';
        return;
      }
      host.innerHTML = judgesCache
        .map(
          (j) => `<div class="card side-card" style="padding:16px">
            <div class="mini-user">
              <span class="avatar">${B.escapeHtml((j.nickname || "J").slice(0, 1))}</span>
              <div>
                <b>${B.escapeHtml(j.nickname || j.account)}</b>
                <div class="tiny muted">@${B.escapeHtml(j.account)}</div>
              </div>
            </div>
            <div class="meta-list" style="margin-top:12px">
              <div><span>邮箱</span><b>${B.escapeHtml(j.email || "—")}</b></div>
              <div><span>状态</span><b>${j.status === "ACTIVE" ? "正常" : "已禁用"}</b></div>
            </div>
          </div>`
        )
        .join("");
    } catch (e) {
      host.innerHTML = `<div class="card side-card muted small" style="grid-column:1/-1">${B.escapeHtml(
        e.message || "评委加载失败"
      )}</div>`;
    }
  }

  async function loadAssignments() {
    const tbody = document.getElementById("assignTable");
    tbody.innerHTML = '<tr><td colspan="5"><div class="skeleton" style="height: 60px"></div></td></tr>';
    try {
      const suffix = assignState.contestId ? "?contestId=" + assignState.contestId : "";
      const list = await B.api.get("/admin/judge-assign" + suffix);
      document.getElementById("assignCount").textContent = "共 " + list.length + " 条分配";
      document.getElementById("assignFoot").textContent = "共 " + list.length + " 条分配记录";
      if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="5" class="muted center" style="padding:28px">暂无分配记录</td></tr>';
        return;
      }
      tbody.innerHTML = list
        .map(
          (a) => `<tr>
            <td class="title-cell">${B.escapeHtml(a.judgeName || "—")}</td>
            <td>${B.escapeHtml(a.contestTitle || "—")}</td>
            <td>${B.escapeHtml(a.workTitle || "—")}</td>
            <td>${B.formatDate(a.createdAt, true)}</td>
            <td><button class="btn btn-ghost btn-xs" data-unassign="${a.id}" style="color:var(--brand)">移除</button></td>
          </tr>`
        )
        .join("");
      tbody.querySelectorAll("[data-unassign]").forEach((btn) => {
        btn.addEventListener("click", () =>
          confirmAction("确定移除这条分配吗？", async () => {
            try {
              await B.api.del("/admin/judge-assign/" + btn.dataset.unassign);
              B.toast("已移除分配", "success");
              loadAssignments();
            } catch (e) {
              B.toast(e.message || "移除失败", "error");
            }
          })
        );
      });
    } catch (e) {
      tbody.innerHTML = `<tr><td colspan="5" class="muted center" style="padding:28px">${B.escapeHtml(
        e.message || "加载失败"
      )}</td></tr>`;
    }
  }

  function openAssignModal() {
    if (!judgesCache.length) {
      B.toast("暂无评委账号，请先创建评委用户", "warn");
      return;
    }
    openModal({
      title: "分配作品给评委",
      size: "modal-lg",
      submitText: "确认分配",
      body: `
        <div class="form-grid">
          <div class="field">
            <label>赛事 *</label>
            <select class="select" name="contestId" id="assignContestSelect">
              <option value="">请选择赛事</option>
              ${state.contests
                .map((c) => `<option value="${c.id}">${B.escapeHtml(c.title)}</option>`)
                .join("")}
            </select>
          </div>
          <div class="field">
            <label>评委 *</label>
            <select class="select" name="judgeId">
              ${judgesCache
                .map((j) => `<option value="${j.id}">${B.escapeHtml(j.nickname || j.account)}</option>`)
                .join("")}
            </select>
          </div>
          <div class="field span-2">
            <label>作品范围</label>
            <div class="check-list" id="assignWorkList">
              <div class="empty-mini">请先选择赛事</div>
            </div>
            <span class="hint">不勾选任何作品时，默认分配该赛事全部已提交作品。</span>
          </div>
        </div>`,
      onMount: (form) => {
        const select = form.querySelector("#assignContestSelect");
        const list = form.querySelector("#assignWorkList");
        const loadWorks = async () => {
          if (!select.value) {
            list.innerHTML = '<div class="empty-mini">请先选择赛事</div>';
            return;
          }
          list.innerHTML = '<div class="empty-mini">加载作品中…</div>';
          try {
            const data = await B.api.get("/admin/works?contestId=" + select.value + "&page=1&size=100");
            const works = data.records || [];
            if (!works.length) {
              list.innerHTML = '<div class="empty-mini">该赛事暂无作品</div>';
              return;
            }
            list.innerHTML = works
              .map(
                (w) => `<label><input type="checkbox" name="workIds" value="${w.id}" />
                  <span>${B.escapeHtml(w.title)} <span class="muted tiny">· ${B.escapeHtml(w.author || "")}</span></span>
                </label>`
              )
              .join("");
          } catch (e) {
            list.innerHTML = `<div class="empty-mini">${B.escapeHtml(e.message || "加载失败")}</div>`;
          }
        };
        select.addEventListener("change", loadWorks);
      },
      onSubmit: async (form) => {
        const contestId = Number(formValue(form, "contestId"));
        const judgeId = Number(formValue(form, "judgeId"));
        if (!contestId) throw new Error("请选择赛事");
        if (!judgeId) throw new Error("请选择评委");
        const workIds = [...form.querySelectorAll('[name="workIds"]:checked')].map((el) => Number(el.value));
        await B.api.post("/admin/judge-assign", {
          contestId,
          judgeId,
          workIds: workIds.length ? workIds : null,
        });
        B.toast("分配完成", "success");
        loadAssignments();
      },
    });
  }

  /* ------------------------------- 奖项管理 ------------------------------- */
  let awardContestId = null;

  async function initAwards() {
    const select = document.getElementById("awardContestFilter");
    select.innerHTML = state.contests
      .map((c) => `<option value="${c.id}">${B.escapeHtml(c.title)}</option>`)
      .join("");
    if (!state.contests.length) {
      document.getElementById("awardTable").innerHTML =
        '<tr><td colspan="5" class="muted center" style="padding:28px">暂无赛事</td></tr>';
      return;
    }
    const queryId = Number(B.qs("contestId"));
    awardContestId = state.contests.some((c) => c.id === queryId) ? queryId : state.contests[0].id;
    select.value = String(awardContestId);
    select.addEventListener("change", () => {
      awardContestId = Number(select.value);
      history.replaceState(null, "", "/admin/award-manage.html?contestId=" + awardContestId);
      loadAwards();
    });
    document.getElementById("newAwardBtn").addEventListener("click", () => openAwardModal());
    loadAwards();
  }

  async function loadAwards() {
    const tbody = document.getElementById("awardTable");
    tbody.innerHTML = '<tr><td colspan="5"><div class="skeleton" style="height: 60px"></div></td></tr>';
    try {
      const list = await B.api.get("/contests/" + awardContestId + "/awards");
      document.getElementById("awardCount").textContent = "共 " + list.length + " 个奖项";
      if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="5" class="muted center" style="padding:28px">尚未设置奖项</td></tr>';
        return;
      }
      tbody.innerHTML = list
        .map(
          (a) => `<tr>
            <td class="num">${a.sortOrder}</td>
            <td class="title-cell">${B.escapeHtml(a.name)}</td>
            <td class="num">${a.quota}</td>
            <td class="num">${
              Number(a.prizeAmount || 0) > 0
                ? "¥" + Number(a.prizeAmount).toLocaleString("zh-CN", { maximumFractionDigits: 0 })
                : '<span class="muted">—</span>'
            }</td>
            <td>
              <div class="actions-cell">
                <button class="btn btn-outline btn-xs" data-edit-award="${a.id}">编辑</button>
                <button class="btn btn-ghost btn-xs" data-del-award="${a.id}" style="color:var(--brand)">删除</button>
              </div>
            </td>
          </tr>`
        )
        .join("");
      tbody.querySelectorAll("[data-edit-award]").forEach((btn) => {
        const award = list.find((a) => a.id === Number(btn.dataset.editAward));
        btn.addEventListener("click", () => openAwardModal(award));
      });
      tbody.querySelectorAll("[data-del-award]").forEach((btn) => {
        btn.addEventListener("click", () =>
          confirmAction("确定删除该奖项吗？", async () => {
            try {
              await B.api.del("/contests/" + awardContestId + "/awards/" + btn.dataset.delAward);
              B.toast("奖项已删除", "success");
              loadAwards();
            } catch (e) {
              B.toast(e.message || "删除失败", "error");
            }
          })
        );
      });
    } catch (e) {
      tbody.innerHTML = `<tr><td colspan="5" class="muted center" style="padding:28px">${B.escapeHtml(
        e.message || "加载失败"
      )}</td></tr>`;
    }
  }

  function openAwardModal(award) {
    const isEdit = Boolean(award);
    const a = award || {};
    openModal({
      title: isEdit ? "编辑奖项" : "新增奖项",
      submitText: isEdit ? "保存修改" : "添加奖项",
      body: `
        <div class="form-grid">
          <div class="field span-2">
            <label>奖项名称 *</label>
            <input class="input" name="name" maxlength="64" required value="${B.escapeHtml(a.name || "")}" placeholder="例如：一等奖" />
          </div>
          <div class="field">
            <label>名额 *</label>
            <input class="input" type="number" name="quota" min="1" max="100" required value="${a.quota || 1}" />
          </div>
          <div class="field">
            <label>奖金（元）</label>
            <input class="input" type="number" name="prizeAmount" min="0" max="999999999.99" step="0.01" placeholder="例如 5000" value="${
              Number(a.prizeAmount || 0) > 0 ? Number(a.prizeAmount) : ""
            }" />
          </div>
          <div class="field">
            <label>排序</label>
            <input class="input" type="number" name="sortOrder" min="0" value="${a.sortOrder || 0}" />
          </div>
        </div>`,
      onSubmit: async (form) => {
        const payload = {
          name: formValue(form, "name").trim(),
          quota: Number(formValue(form, "quota")),
          prizeAmount: formValue(form, "prizeAmount") === "" ? null : Number(formValue(form, "prizeAmount")),
          sortOrder: Number(formValue(form, "sortOrder") || 0),
        };
        if (!payload.name) throw new Error("请输入奖项名称");
        if (!payload.quota || payload.quota < 1) throw new Error("名额至少为 1");
        if (isEdit) {
          await B.api.put("/contests/" + awardContestId + "/awards/" + a.id, payload);
          B.toast("奖项已更新", "success");
        } else {
          await B.api.post("/contests/" + awardContestId + "/awards", payload);
          B.toast("奖项已添加", "success");
        }
        loadAwards();
      },
    });
  }

  /* ------------------------------- 公告管理 ------------------------------- */
  const noticeState = { page: 1, size: 10, keyword: "" };

  async function initNotices() {
    document.getElementById("newNoticeBtn").addEventListener("click", () => openNoticeModal());
    const keyword = document.getElementById("noticeKeyword");
    keyword.addEventListener(
      "input",
      B.debounce(() => {
        noticeState.keyword = keyword.value.trim();
        noticeState.page = 1;
        loadNoticeTable();
      }, 320)
    );
    loadNoticeTable();
  }

  async function loadNoticeTable() {
    const tbody = document.getElementById("noticeTable");
    tbody.innerHTML = '<tr><td colspan="5"><div class="skeleton" style="height: 60px"></div></td></tr>';
    try {
      const params = new URLSearchParams({ page: noticeState.page, size: noticeState.size });
      if (noticeState.keyword) params.set("keyword", noticeState.keyword);
      const data = await B.api.get("/admin/notices?" + params.toString());
      const list = data.records || [];
      document.getElementById("noticeCount").textContent = "共 " + data.total + " 条公告";
      document.getElementById("noticeFoot").textContent =
        "第 " + noticeState.page + " / " + Math.max(1, Math.ceil(data.total / noticeState.size)) + " 页";
      if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="5" class="muted center" style="padding:28px">暂无公告</td></tr>';
      } else {
        tbody.innerHTML = list
          .map(
            (n) => `<tr>
              <td class="title-cell">${B.escapeHtml(n.title)}</td>
              <td style="max-width: 380px; white-space: normal">${B.escapeHtml(
                String(n.content || "").slice(0, 80)
              )}${String(n.content || "").length > 80 ? "…" : ""}</td>
              <td>${n.pinned ? '<span class="pin">置顶</span>' : '<span class="muted">—</span>'}</td>
              <td>${B.formatDate(n.publishedAt, true)}</td>
              <td>
                <div class="actions-cell">
                  <button class="btn btn-outline btn-xs" data-edit-notice="${n.id}">编辑</button>
                  <button class="btn btn-ghost btn-xs" data-del-notice="${n.id}" style="color:var(--brand)">删除</button>
                </div>
              </td>
            </tr>`
          )
          .join("");
        tbody.querySelectorAll("[data-edit-notice]").forEach((btn) => {
          const notice = list.find((n) => n.id === Number(btn.dataset.editNotice));
          btn.addEventListener("click", () => openNoticeModal(notice));
        });
        tbody.querySelectorAll("[data-del-notice]").forEach((btn) => {
          btn.addEventListener("click", () =>
            confirmAction("确定删除该公告吗？", async () => {
              try {
                await B.api.del("/admin/notices/" + btn.dataset.delNotice);
                B.toast("公告已删除", "success");
                loadNoticeTable();
              } catch (e) {
                B.toast(e.message || "删除失败", "error");
              }
            })
          );
        });
      }
      renderPagination(
        document.getElementById("noticePagination"),
        noticeState.page,
        data.total,
        noticeState.size,
        (page) => {
          noticeState.page = page;
          loadNoticeTable();
        }
      );
    } catch (e) {
      tbody.innerHTML = `<tr><td colspan="5" class="muted center" style="padding:28px">${B.escapeHtml(
        e.message || "加载失败"
      )}</td></tr>`;
    }
  }

  function openNoticeModal(notice) {
    const isEdit = Boolean(notice);
    const n = notice || {};
    openModal({
      title: isEdit ? "编辑公告" : "发布公告",
      size: "modal-lg",
      submitText: isEdit ? "保存修改" : "发布公告",
      body: `
        <div class="field">
          <label>公告标题 *</label>
          <input class="input" name="title" maxlength="160" required value="${B.escapeHtml(n.title || "")}" />
        </div>
        <div class="field" style="margin-top:16px">
          <label>公告内容 *</label>
          <textarea class="textarea" name="content" style="min-height:220px" required>${B.escapeHtml(
            n.content || ""
          )}</textarea>
        </div>
        <label class="check" style="margin-top:14px"><input type="checkbox" name="pinned" ${
          n.pinned ? "checked" : ""
        } /> 置顶显示</label>`,
      onSubmit: async (form) => {
        const payload = {
          title: formValue(form, "title").trim(),
          content: formValue(form, "content").trim(),
          pinned: form.querySelector('[name="pinned"]').checked,
        };
        if (!payload.title) throw new Error("请输入公告标题");
        if (!payload.content) throw new Error("请输入公告内容");
        if (isEdit) {
          await B.api.put("/admin/notices/" + n.id, payload);
          B.toast("公告已更新", "success");
        } else {
          await B.api.post("/admin/notices", payload);
          B.toast("公告已发布", "success");
        }
        loadNoticeTable();
      },
    });
  }

  /* ------------------------------- 数据导出 ------------------------------- */
  function initExport() {
    const signupSelect = document.getElementById("exportSignupContest");
    const winnerSelect = document.getElementById("exportWinnerContest");
    fillContestSelect(signupSelect);
    fillContestSelect(winnerSelect, { placeholder: "请选择赛事" });

    document.getElementById("exportSignupBtn").addEventListener("click", () => {
      const suffix = signupSelect.value ? "?contestId=" + signupSelect.value : "";
      B.download("/admin/export/signups" + suffix, "报名名单.xlsx").catch((e) =>
        B.toast(e.message || "导出失败", "error")
      );
    });

    document.getElementById("exportWinnerBtn").addEventListener("click", () => {
      if (!winnerSelect.value) {
        B.toast("请先选择要导出的赛事", "warn");
        return;
      }
      B.download("/admin/export/winners?contestId=" + winnerSelect.value, "获奖名单.xlsx").catch((e) =>
        B.toast(e.message || "导出失败", "error")
      );
    });
  }

  /* ------------------------------- 基础配置 ------------------------------- */
  function bindSettingsTabs(host, panels, onSwitch) {
    const buttons = host.querySelectorAll("button[data-tab]");
    buttons.forEach((btn) => {
      btn.addEventListener("click", () => {
        buttons.forEach((b) => b.classList.remove("is-active"));
        btn.classList.add("is-active");
        const target = btn.dataset.tab;
        panels.forEach((panel) => panel.classList.toggle("is-active", panel.id === "panel-" + target));
        if (onSwitch) onSwitch(target);
      });
    });
  }

  async function initSettings() {
    const host = document.getElementById("settingsTabs");
    const panels = host.parentElement.querySelectorAll(".tab-panel");
    bindSettingsTabs(host, panels, (tab) => {
      if (tab === "mail") loadMailConfig();
      if (tab === "site") loadSiteConfig();
      if (tab === "switches") loadSwitchesConfig();
      if (tab === "templates") loadTemplates();
      if (tab === "uploads") loadUploadConfig();
      if (tab === "security") loadSecurityConfig();
    });

    document.getElementById("saveMailBtn").addEventListener("click", saveMailConfig);
    document.getElementById("testMailBtn").addEventListener("click", testMail);
    document.getElementById("saveSiteBtn").addEventListener("click", saveSiteConfig);
    document.getElementById("saveSwitchesBtn").addEventListener("click", saveSwitchesConfig);
    document.getElementById("saveTemplateBtn").addEventListener("click", saveTemplate);
    document.getElementById("saveUploadsBtn").addEventListener("click", saveUploadConfig);
    document.getElementById("saveSecurityBtn").addEventListener("click", saveSecurityConfig);

    const tplTabs = document.getElementById("templateTabs");
    tplTabs.querySelectorAll("button[data-tpl]").forEach((btn) => {
      btn.addEventListener("click", () => {
        tplTabs.querySelectorAll("button").forEach((b) => b.classList.remove("is-active"));
        btn.classList.add("is-active");
        setTemplateFields(btn.dataset.tpl);
      });
    });

    loadMailConfig();
  }

  function setSaving(btn, saving) {
    if (!btn) return;
    btn.disabled = saving;
    btn.textContent = saving ? "保存中…" : btn.dataset.origin || btn.textContent;
  }

  async function loadMailConfig() {
    try {
      const data = await B.api.get("/admin/config/mail");
      document.getElementById("mailHost").value = data.host || "";
      document.getElementById("mailPort").value = data.port || 465;
      document.getElementById("mailFrom").value = data.from || "";
      document.getElementById("mailUsername").value = data.username || "";
      document.getElementById("mailPassword").value = "";
      document.getElementById("mailPassword").placeholder = data.password ? "已设置（留空不修改）" : "未设置";
      document.getElementById("mailSsl").value = String(data.ssl ?? true);
      document.getElementById("mailEnabled").value = String(!!data.enabled);
    } catch (e) {
      B.toast(e.message || "邮件配置加载失败", "error");
    }
  }

  async function saveMailConfig() {
    const btn = document.getElementById("saveMailBtn");
    btn.dataset.origin = btn.textContent;
    setSaving(btn, true);
    try {
      await B.api.put("/admin/config/mail", {
        host: document.getElementById("mailHost").value.trim(),
        port: Number(document.getElementById("mailPort").value) || 465,
        username: document.getElementById("mailUsername").value.trim(),
        password: document.getElementById("mailPassword").value,
        from: document.getElementById("mailFrom").value.trim(),
        ssl: document.getElementById("mailSsl").value === "true",
        enabled: document.getElementById("mailEnabled").value === "true",
      });
      B.toast("邮件配置已保存", "success");
      loadMailConfig();
    } catch (e) {
      B.toast(e.message || "保存失败", "error");
    } finally {
      setSaving(btn, false);
    }
  }

  async function testMail() {
    const to = document.getElementById("mailTestTo").value.trim();
    if (!to) {
      B.toast("请输入测试收件邮箱", "warn");
      return;
    }
    const btn = document.getElementById("testMailBtn");
    btn.disabled = true;
    btn.textContent = "发送中…";
    try {
      const data = await B.api.post("/admin/config/mail/test", { to });
      B.toast(data.message || (data.success ? "测试邮件已发送" : "发送失败"), data.success ? "success" : "error");
    } catch (e) {
      B.toast(e.message || "发送失败", "error");
    } finally {
      btn.disabled = false;
      btn.textContent = "发送测试邮件";
    }
  }

  async function loadSiteConfig() {
    try {
      const data = await B.api.get("/admin/config/site");
      document.getElementById("siteName").value = data.name || "";
      document.getElementById("siteLogo").value = data.logoUrl || "";
      document.getElementById("siteFooter").value = data.footer || "";
    } catch (e) {
      B.toast(e.message || "站点信息加载失败", "error");
    }
  }

  async function saveSiteConfig() {
    const btn = document.getElementById("saveSiteBtn");
    btn.dataset.origin = btn.textContent;
    setSaving(btn, true);
    try {
      await B.api.put("/admin/config/site", {
        name: document.getElementById("siteName").value.trim(),
        logoUrl: document.getElementById("siteLogo").value.trim(),
        footer: document.getElementById("siteFooter").value.trim(),
      });
      B.toast("站点信息已保存", "success");
    } catch (e) {
      B.toast(e.message || "保存失败", "error");
    } finally {
      setSaving(btn, false);
    }
  }

  async function loadSwitchesConfig() {
    try {
      const data = await B.api.get("/admin/config/switches");
      document.getElementById("switchRegister").value = String(!!data.registerEnabled);
      document.getElementById("switchCaptcha").value =
        data.captchaEnabled === null || data.captchaEnabled === undefined ? "" : String(!!data.captchaEnabled);
    } catch (e) {
      B.toast(e.message || "开关设置加载失败", "error");
    }
  }

  async function saveSwitchesConfig() {
    const btn = document.getElementById("saveSwitchesBtn");
    btn.dataset.origin = btn.textContent;
    setSaving(btn, true);
    try {
      const captcha = document.getElementById("switchCaptcha").value;
      await B.api.put("/admin/config/switches", {
        registerEnabled: document.getElementById("switchRegister").value === "true",
        captchaEnabled: captcha === "" ? null : captcha === "true",
      });
      B.toast("开关设置已保存", "success");
    } catch (e) {
      B.toast(e.message || "保存失败", "error");
    } finally {
      setSaving(btn, false);
    }
  }

  const templateTitles = {
    "template.signup_receipt": "报名成功邮件",
    "template.review_reminder": "评审提醒邮件",
    "template.winner_notice": "获奖通知邮件",
    "template.verify_code": "验证码邮件",
  };
  let currentTemplateKey = "template.signup_receipt";

  function setTemplateFields(key) {
    currentTemplateKey = key;
    const panel = document.getElementById("panel-templates");
    const content = panel.querySelector('textarea[name="tpl_' + key + '"]');
    document.getElementById("templateName").textContent = templateTitles[key] || key;
    document.getElementById("templateContent").value = content ? content.value : "";
  }

  async function loadTemplates() {
    try {
      const list = await B.api.get("/admin/config/templates");
      const panel = document.getElementById("panel-templates");
      list.forEach((tpl) => {
        let area = panel.querySelector('textarea[name="tpl_' + tpl.key + '"]');
        if (!area) {
          area = document.createElement("textarea");
          area.style.display = "none";
          area.name = "tpl_" + tpl.key;
          panel.appendChild(area);
        }
        area.value = tpl.content || "";
      });
      setTemplateFields(currentTemplateKey);
    } catch (e) {
      B.toast(e.message || "模板加载失败", "error");
    }
  }

  async function saveTemplate() {
    const btn = document.getElementById("saveTemplateBtn");
    btn.dataset.origin = btn.textContent;
    setSaving(btn, true);
    try {
      const panel = document.getElementById("panel-templates");
      const area = panel.querySelector('textarea[name="tpl_' + currentTemplateKey + '"]');
      await B.api.put("/admin/config/templates/" + encodeURIComponent(currentTemplateKey), {
        content: document.getElementById("templateContent").value,
      });
      if (area) area.value = document.getElementById("templateContent").value;
      B.toast("模板已保存", "success");
    } catch (e) {
      B.toast(e.message || "保存失败", "error");
    } finally {
      setSaving(btn, false);
    }
  }

  async function loadUploadConfig() {
    try {
      const data = await B.api.get("/admin/config/uploads");
      document.getElementById("uploadMaxMb").value = data.maxSizeMb || 50;
      document.getElementById("uploadTypes").value = data.allowedTypes || "";
    } catch (e) {
      B.toast(e.message || "上传参数加载失败", "error");
    }
  }

  async function saveUploadConfig() {
    const btn = document.getElementById("saveUploadsBtn");
    btn.dataset.origin = btn.textContent;
    setSaving(btn, true);
    try {
      await B.api.put("/admin/config/uploads", {
        maxSizeMb: Number(document.getElementById("uploadMaxMb").value) || 50,
        allowedTypes: document.getElementById("uploadTypes").value.trim(),
      });
      B.toast("上传参数已保存", "success");
    } catch (e) {
      B.toast(e.message || "保存失败", "error");
    } finally {
      setSaving(btn, false);
    }
  }

  async function loadSecurityConfig() {
    try {
      const data = await B.api.get("/admin/config/security");
      document.getElementById("secFailLimit").value = data.loginFailLimit || 5;
      document.getElementById("secLockMinutes").value = data.loginLockMinutes || 10;
    } catch (e) {
      B.toast(e.message || "安全策略加载失败", "error");
    }
  }

  async function saveSecurityConfig() {
    const btn = document.getElementById("saveSecurityBtn");
    btn.dataset.origin = btn.textContent;
    setSaving(btn, true);
    try {
      await B.api.put("/admin/config/security", {
        loginFailLimit: Number(document.getElementById("secFailLimit").value) || 5,
        loginLockMinutes: Number(document.getElementById("secLockMinutes").value) || 10,
      });
      B.toast("安全策略已保存", "success");
    } catch (e) {
      B.toast(e.message || "保存失败", "error");
    } finally {
      setSaving(btn, false);
    }
  }

  /* ------------------------------- 账号权限 ------------------------------- */
  const userState = { page: 1, size: 10, role: "", keyword: "" };
  const ROLE_LABEL = { ADMIN: "管理员", JUDGE: "评委", PLAYER: "选手" };

  async function initUserManage() {
    document.getElementById("newUserBtn").addEventListener("click", openCreateUserModal);
    const role = document.getElementById("userRoleFilter");
    role.addEventListener("change", () => {
      userState.role = role.value;
      userState.page = 1;
      loadUserTable();
    });
    const keyword = document.getElementById("userKeyword");
    keyword.addEventListener(
      "input",
      B.debounce(() => {
        userState.keyword = keyword.value.trim();
        userState.page = 1;
        loadUserTable();
      }, 320)
    );
    loadUserTable();
  }

  async function loadUserTable() {
    const tbody = document.getElementById("userTable");
    tbody.innerHTML = '<tr><td colspan="7"><div class="skeleton" style="height: 60px"></div></td></tr>';
    try {
      const params = new URLSearchParams({ page: userState.page, size: userState.size });
      if (userState.role) params.set("role", userState.role);
      if (userState.keyword) params.set("keyword", userState.keyword);
      const data = await B.api.get("/admin/users?" + params.toString());
      const list = data.records || [];
      document.getElementById("userCount").textContent = "共 " + data.total + " 个账号";
      document.getElementById("userFoot").textContent =
        "第 " + userState.page + " / " + Math.max(1, Math.ceil(data.total / userState.size)) + " 页";
      if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="muted center" style="padding:28px">暂无账号</td></tr>';
      } else {
        tbody.innerHTML = list
          .map(
            (u) => `<tr>
              <td class="title-cell">${B.escapeHtml(u.account)}</td>
              <td>${B.escapeHtml(u.nickname || "—")}</td>
              <td><span class="badge badge-soft ${
                u.role === "ADMIN" ? "badge-warn" : u.role === "JUDGE" ? "" : "badge-plain"
              }">${ROLE_LABEL[u.role] || u.role}</span></td>
              <td><span class="badge ${u.status === "ACTIVE" ? "badge-ok" : ""}">${
              u.status === "ACTIVE" ? "启用" : "禁用"
            }</span></td>
              <td>${B.escapeHtml(u.email || "—")}</td>
              <td>${B.formatDate(u.createdAt)}</td>
              <td>
                <div class="actions-cell">
                  <button class="btn btn-outline btn-xs" data-role="${u.id}">角色</button>
                  <button class="btn btn-ghost btn-xs" data-pwd="${u.id}">重置密码</button>
                  <button class="btn btn-ghost btn-xs" data-toggle="${u.id}" data-status="${u.status}">${
              u.status === "ACTIVE" ? "禁用" : "启用"
            }</button>
                </div>
              </td>
            </tr>`
          )
          .join("");
        tbody.querySelectorAll("[data-role]").forEach((btn) => {
          const user = list.find((u) => u.id === Number(btn.dataset.role));
          btn.addEventListener("click", () => openRoleModal(user));
        });
        tbody.querySelectorAll("[data-pwd]").forEach((btn) => {
          const user = list.find((u) => u.id === Number(btn.dataset.pwd));
          btn.addEventListener("click", () => openResetPasswordModal(user));
        });
        tbody.querySelectorAll("[data-toggle]").forEach((btn) => {
          const user = list.find((u) => u.id === Number(btn.dataset.toggle));
          btn.addEventListener("click", () =>
            confirmAction("确定" + (user.status === "ACTIVE" ? "禁用" : "启用") + "账号 " + user.account + " 吗？", async () => {
              try {
                await B.api.put("/admin/users/" + user.id + "/status", {
                  status: user.status === "ACTIVE" ? "DISABLED" : "ACTIVE",
                });
                B.toast("账号状态已更新", "success");
                loadUserTable();
              } catch (e) {
                B.toast(e.message || "操作失败", "error");
              }
            })
          );
        });
      }
      renderPagination(
        document.getElementById("userPagination"),
        userState.page,
        data.total,
        userState.size,
        (page) => {
          userState.page = page;
          loadUserTable();
        }
      );
    } catch (e) {
      tbody.innerHTML = `<tr><td colspan="7" class="muted center" style="padding:28px">${B.escapeHtml(
        e.message || "加载失败"
      )}</td></tr>`;
    }
  }

  function openCreateUserModal() {
    openModal({
      title: "创建账号",
      submitText: "创建账号",
      body: `
        <div class="form-grid">
          <div class="field">
            <label>账号 *</label>
            <input class="input" name="account" maxlength="32" required placeholder="4-32 位字母、数字或下划线" />
          </div>
          <div class="field">
            <label>密码 *</label>
            <input class="input" name="password" type="password" maxlength="32" required autocomplete="new-password" placeholder="6-32 位" />
          </div>
          <div class="field">
            <label>昵称 *</label>
            <input class="input" name="nickname" maxlength="32" required />
          </div>
          <div class="field">
            <label>邮箱</label>
            <input class="input" name="email" type="email" maxlength="64" placeholder="选填" />
          </div>
          <div class="field span-2">
            <label>角色 *</label>
            <select class="select" name="role">
              <option value="PLAYER">选手</option>
              <option value="JUDGE">评委</option>
              <option value="ADMIN">管理员</option>
            </select>
          </div>
        </div>`,
      onSubmit: async (form) => {
        await B.api.post("/admin/users", {
          account: formValue(form, "account").trim(),
          password: formValue(form, "password"),
          nickname: formValue(form, "nickname").trim(),
          email: formValue(form, "email").trim() || null,
          role: formValue(form, "role"),
        });
        B.toast("账号已创建", "success");
        loadUserTable();
      },
    });
  }

  function openRoleModal(user) {
    openModal({
      title: "调整角色 · " + user.account,
      submitText: "保存",
      body: `
        <div class="field">
          <label>新角色 *</label>
          <select class="select" name="role">
            <option value="PLAYER" ${user.role === "PLAYER" ? "selected" : ""}>选手</option>
            <option value="JUDGE" ${user.role === "JUDGE" ? "selected" : ""}>评委</option>
            <option value="ADMIN" ${user.role === "ADMIN" ? "selected" : ""}>管理员</option>
          </select>
        </div>
        <p class="tiny muted" style="margin-top:10px">系统会阻止将最后一个启用中的管理员降级或禁用。</p>`,
      onSubmit: async (form) => {
        await B.api.put("/admin/users/" + user.id + "/role", { role: formValue(form, "role") });
        B.toast("角色已更新", "success");
        loadUserTable();
      },
    });
  }

  function openResetPasswordModal(user) {
    openModal({
      title: "重置密码 · " + user.account,
      submitText: "重置密码",
      body: `
        <div class="field">
          <label>新密码 *</label>
          <input class="input" name="newPassword" type="password" maxlength="32" required autocomplete="new-password" placeholder="6-32 位" />
        </div>
        <p class="tiny muted" style="margin-top:10px">重置后该账号需使用新密码登录。</p>`,
      onSubmit: async (form) => {
        const pwd = formValue(form, "newPassword");
        if (pwd.length < 6) throw new Error("密码至少 6 位");
        await B.api.put("/admin/users/" + user.id + "/password", { newPassword: pwd });
        B.toast("密码已重置", "success");
        loadUserTable();
      },
    });
  }

  /* ------------------------------- 操作日志 ------------------------------- */
  const logState = { page: 1, size: 20, action: "", keyword: "" };

  async function initLogs() {
    const action = document.getElementById("logActionFilter");
    const keyword = document.getElementById("logKeyword");
    const search = () => {
      logState.action = action.value;
      logState.keyword = keyword.value.trim();
      logState.page = 1;
      loadLogTable();
    };
    action.addEventListener("change", search);
    keyword.addEventListener("keydown", (e) => {
      if (e.key === "Enter") search();
    });
    document.getElementById("logSearchBtn").addEventListener("click", search);
    loadLogTable();
  }

  async function loadLogTable() {
    const tbody = document.getElementById("logTable");
    tbody.innerHTML = '<tr><td colspan="5"><div class="skeleton" style="height: 60px"></div></td></tr>';
    try {
      const params = new URLSearchParams({ page: logState.page, size: logState.size });
      if (logState.action) params.set("action", logState.action);
      if (logState.keyword) params.set("keyword", logState.keyword);
      const data = await B.api.get("/admin/logs?" + params.toString());
      const list = data.records || [];
      document.getElementById("logCount").textContent = "共 " + data.total + " 条";
      document.getElementById("logFoot").textContent =
        "第 " + logState.page + " / " + Math.max(1, Math.ceil(data.total / logState.size)) + " 页";
      if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="5" class="muted center" style="padding:28px">暂无日志</td></tr>';
      } else {
        tbody.innerHTML = list
          .map(
            (l) => `<tr>
              <td style="white-space:nowrap">${B.formatDate(l.createdAt, true)}</td>
              <td>${B.escapeHtml(l.nickname || l.account || "—")}</td>
              <td><code>${B.escapeHtml(l.action || "")}</code></td>
              <td class="muted" style="max-width: 320px; white-space: normal">${B.escapeHtml(l.target || "—")}</td>
              <td class="muted">${B.escapeHtml(l.ip || "—")}</td>
            </tr>`
          )
          .join("");
      }
      renderPagination(
        document.getElementById("logPagination"),
        logState.page,
        data.total,
        logState.size,
        (page) => {
          logState.page = page;
          loadLogTable();
        }
      );
    } catch (e) {
      tbody.innerHTML = `<tr><td colspan="5" class="muted center" style="padding:28px">${B.escapeHtml(
        e.message || "加载失败"
      )}</td></tr>`;
    }
  }

  /* ------------------------------- 邮件记录 ------------------------------- */
  const mailState = { page: 1, size: 20, status: "", recipient: "" };
  const MAIL_STATUS = { PENDING: "待发送", SENT: "已发送", SKIPPED: "已跳过", FAILED: "失败" };

  async function initMails() {
    const status = document.getElementById("mailStatusFilter");
    const recipient = document.getElementById("mailRecipient");
    const search = () => {
      mailState.status = status.value;
      mailState.recipient = recipient.value.trim();
      mailState.page = 1;
      loadMailTable();
    };
    status.addEventListener("change", search);
    recipient.addEventListener("keydown", (e) => {
      if (e.key === "Enter") search();
    });
    document.getElementById("mailSearchBtn").addEventListener("click", search);
    loadMailTable();
  }

  async function loadMailTable() {
    const tbody = document.getElementById("mailTable");
    tbody.innerHTML = '<tr><td colspan="6"><div class="skeleton" style="height: 60px"></div></td></tr>';
    try {
      const params = new URLSearchParams({ page: mailState.page, size: mailState.size });
      if (mailState.status) params.set("status", mailState.status);
      if (mailState.recipient) params.set("recipient", mailState.recipient);
      const data = await B.api.get("/admin/mails?" + params.toString());
      const list = data.records || [];
      document.getElementById("mailCount").textContent = "共 " + data.total + " 封";
      document.getElementById("mailFoot").textContent =
        "第 " + mailState.page + " / " + Math.max(1, Math.ceil(data.total / mailState.size)) + " 页";
      if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="6" class="muted center" style="padding:28px">暂无邮件记录</td></tr>';
      } else {
        tbody.innerHTML = list
          .map(
            (m) => `<tr>
              <td style="white-space:nowrap">${B.formatDate(m.createdAt, true)}</td>
              <td>${B.escapeHtml(m.recipient || "—")}</td>
              <td><code>${B.escapeHtml(m.template || "")}</code></td>
              <td class="muted" style="max-width: 240px; white-space: normal">${B.escapeHtml(m.subject || "—")}</td>
              <td><span class="badge ${
                m.status === "SENT" ? "badge-ok" : m.status === "FAILED" ? "badge-danger" : "badge-plain"
              }">${MAIL_STATUS[m.status] || m.status}</span></td>
              <td class="muted" style="max-width: 200px; white-space: normal">${B.escapeHtml(m.error || "—")}</td>
            </tr>`
          )
          .join("");
      }
      renderPagination(
        document.getElementById("mailPagination"),
        mailState.page,
        data.total,
        mailState.size,
        (page) => {
          mailState.page = page;
          loadMailTable();
        }
      );
    } catch (e) {
      tbody.innerHTML = `<tr><td colspan="6" class="muted center" style="padding:28px">${B.escapeHtml(
        e.message || "加载失败"
      )}</td></tr>`;
    }
  }

  /* ------------------------------- 启动 ------------------------------- */
  async function init() {
    const user = B.requireAuth(["ADMIN"]);
    if (!user) return;
    state.user = user;
    mountShell();
    await loadContests();

    const pages = {
      dashboard: initDashboard,
      contests: initContests,
      signups: initSignups,
      works: initWorks,
      judges: initJudges,
      awards: initAwards,
      notices: initNotices,
      export: initExport,
      settings: initSettings,
      userManage: initUserManage,
      logs: initLogs,
      mails: initMails,
    };
    const handler = pages[document.body.dataset.page];
    if (handler) handler();
  }

  document.addEventListener("DOMContentLoaded", init);
})();

