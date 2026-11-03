/* ==========================================================================
   BTR 赛事平台 · 公共组件
   ========================================================================== */
(function () {
  const B = window.BTR;

  async function mountLayout() {
    const headerHost = document.querySelector("[data-header]");
    const footerHost = document.querySelector("[data-footer]");

    if (headerHost) {
      headerHost.innerHTML = await fetchPartial("/common/header.html", fallbackHeader());
      initHeader();
    }
    if (footerHost) {
      footerHost.innerHTML = await fetchPartial("/common/footer.html", "");
    }
    markActiveNav();
    applySiteConfig();
  }

  /** 从公开配置接口动态注入站点名称 / Logo / 页脚 */
  async function applySiteConfig() {
    try {
      const res = await fetch("/api/v1/config/public-site", { cache: "no-store" });
      if (!res.ok) return;
      const body = await res.json();
      const data = body && body.data ? body.data : null;
      if (!data) return;
      const name = String(data.name || "BTR 赛事平台").trim();
      document.querySelectorAll(".brand-name").forEach((el) => {
        el.textContent = name;
      });
      document.querySelectorAll(".site-header .brand, .site-footer .brand").forEach((el) => {
        el.setAttribute("aria-label", name + "首页");
      });
      if (data.logoUrl && /^https?:\/\//i.test(String(data.logoUrl))) {
        const logo = '<img src="' + B.escapeHtml(String(data.logoUrl)) + '" alt="' +
          B.escapeHtml(name) + '" style="height:30px;width:auto;margin-right:8px;border-radius:6px" />';
        document.querySelectorAll(".site-header .brand, .site-footer .brand").forEach((el) => {
          const mark = el.querySelector(".brand-mark");
          if (mark) mark.style.display = "none";
          el.insertAdjacentHTML("afterbegin", logo);
        });
      }
      if (data.footer) {
        const bottom = document.querySelector(".footer-bottom span:first-child");
        if (bottom) bottom.textContent = String(data.footer);
      }
    } catch (e) {
      /* 接口不可用时保持默认文案 */
    }
  }

  async function fetchPartial(url, fallback) {
    try {
      const res = await fetch(url, { cache: "no-store" });
      if (!res.ok) throw new Error("partial missing");
      return await res.text();
    } catch (e) {
      return fallback;
    }
  }

  function fallbackHeader() {
    return `<header class="site-header"><div class="container header-inner">
      <a class="brand" href="/index.html"><span class="brand-mark">B</span><span class="brand-name">BTR<em>赛事平台</em></span></a>
      <nav class="nav"><a href="/contest-list.html">赛事</a><a href="/rank.html">榜单</a><a href="/notices.html">公告</a></nav>
      <div class="header-actions"><a class="btn btn-ghost btn-sm" href="/login.html">登录 / 注册</a></div>
    </div></header>`;
  }

  function initHeader() {
    const user = B.storage.user;
    const guest = document.querySelector('[data-auth="guest"]');
    const userBox = document.querySelector('[data-auth="user"]');
    const chip = document.getElementById("userChip");
    const dropdown = document.getElementById("userDropdown");
    const logout = document.getElementById("logoutBtn");
    const toggle = document.getElementById("menuToggle");
    const nav = document.getElementById("siteNav");

    if (user && B.storage.token) {
      if (guest) guest.hidden = true;
      if (userBox) userBox.hidden = false;
      const avatar = document.getElementById("userAvatar");
      const name = document.getElementById("userName");
      const role = document.getElementById("userRole");
      if (avatar) avatar.textContent = (user.nickname || user.account || "U").slice(0, 1);
      if (name) name.textContent = user.nickname || user.account;
      if (role) role.textContent = B.roleLabel(user.role);
      document.querySelectorAll("[data-role-link]").forEach((el) => {
        if (el.dataset.roleLink !== user.role) el.remove();
      });
    } else {
      if (guest) guest.hidden = false;
      if (userBox) userBox.hidden = true;
    }

    if (chip && dropdown) {
      chip.addEventListener("click", (e) => {
        e.stopPropagation();
        const open = dropdown.classList.toggle("is-open");
        chip.setAttribute("aria-expanded", String(open));
      });
      document.addEventListener("click", () => {
        dropdown.classList.remove("is-open");
        chip.setAttribute("aria-expanded", "false");
      });
      dropdown.addEventListener("click", (e) => e.stopPropagation());
    }

    if (logout) {
      logout.addEventListener("click", async () => {
        try {
          await B.api.post("/auth/logout");
        } catch (e) {
          /* ignore */
        }
        B.storage.clear();
        location.href = "/index.html";
      });
    }

    if (toggle && nav) {
      toggle.addEventListener("click", () => nav.classList.toggle("is-open"));
      nav.querySelectorAll("a").forEach((a) =>
        a.addEventListener("click", () => nav.classList.remove("is-open"))
      );
    }
  }

  function markActiveNav() {
    const path = location.pathname;
    let key = "home";
    if (path.includes("contest-list") || path.includes("contest-detail")) key = "contests";
    else if (path.includes("rank")) key = "rank";
    else if (path.includes("notices")) key = "notices";
    const active = document.querySelector('[data-nav="' + key + '"]');
    if (active) active.classList.add("is-active");
  }

  function statusBadge(status) {
    return B.statusBadge(status);
  }

  function contestCard(contest) {
    const cover = contest.cover || "/assets/img/cover-1.svg";
    const info = B.statusInfo(contest.status);
    const signupEnd = contest.signupEnd ? B.formatDate(contest.signupEnd) : "待定";
    const prize = Number(contest.prizeAmount || 0);
    const prizeHtml =
      prize > 0
        ? `<span class="stat contest-prize">奖金总额 <b>¥${prize.toLocaleString("zh-CN", { maximumFractionDigits: 0 })}</b></span>`
        : `<span class="stat contest-prize">奖金总额 <b class="no-prize">暂无</b></span>`;
    const signupAction =
      contest.status === "SIGNUP"
        ? `<a class="btn btn-primary btn-sm" href="/signup.html?contestId=${contest.id}">立即报名</a>`
        : contest.status === "REVIEW"
        ? `<a class="btn btn-outline btn-sm" href="/contest-detail.html?id=${contest.id}">查看进度</a>`
        : `<a class="btn btn-outline btn-sm" href="/rank.html?contestId=${contest.id}">查看榜单</a>`;

    return `<article class="contest-card fade-up">
      <div class="flip-inner">
        <div class="flip-face flip-front">
          <div class="contest-cover">
            <img src="${B.escapeHtml(cover)}" alt="${B.escapeHtml(contest.title)}" loading="lazy" />
            <span class="badge ${info.cls}">${info.label}</span>
          </div>
          <div class="contest-body">
            <h3>${B.escapeHtml(contest.title)}</h3>
            <p>${B.escapeHtml(contest.summary || "暂无赛事简介")}</p>
            <div class="contest-meta">
              <span class="stat">报名截止 <b>${signupEnd}</b></span>
              ${prizeHtml}
            </div>
          </div>
        </div>
        <div class="flip-face flip-back">
          <div class="contest-back-top">
            <span class="badge ${info.cls}">${info.label}</span>
            <h3>${B.escapeHtml(contest.title)}</h3>
            <p>${B.escapeHtml(contest.summary || "暂无赛事简介")}</p>
            <div class="contest-back-stats">
              <div><b>${contest.signupCount || 0}</b><span>报名人数</span></div>
              <div><b>${contest.workCount || 0}</b><span>提交作品</span></div>
            </div>
          </div>
          <div class="contest-back-actions">
            <a class="btn btn-outline btn-sm" href="/contest-detail.html?id=${contest.id}">赛事详情</a>
            ${signupAction}
          </div>
        </div>
      </div>
    </article>`;
  }

  function pagination(container, page, total, size, onChange) {
    if (!container) return;
    const pages = Math.max(1, Math.ceil(total / size));
    const current = Math.min(Math.max(1, page), pages);
    const items = [];
    const push = (p, label, active, disabled) => {
      items.push(
        `<button type="button" data-page="${p}" class="${active ? "is-active" : ""}" ${
          disabled ? "disabled" : ""
        }>${label}</button>`
      );
    };
    push(current - 1, "‹", false, current <= 1);
    const numbers = new Set([1, pages, current, current - 1, current + 1]);
    const sorted = [...numbers].filter((n) => n >= 1 && n <= pages).sort((a, b) => a - b);
    let last = 0;
    for (const n of sorted) {
      if (last && n - last > 1) items.push('<span class="ellipsis">…</span>');
      push(n, String(n), n === current, false);
      last = n;
    }
    push(current + 1, "›", false, current >= pages);
    container.innerHTML = items.join("");
    container.querySelectorAll("button[data-page]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const p = Number(btn.dataset.page);
        if (p >= 1 && p <= pages && p !== current) onChange(p);
      });
    });
  }

  function initTabs(root) {
    const scope = root || document;
    scope.querySelectorAll("[data-tabs]").forEach((group) => {
      const buttons = group.querySelectorAll("[data-tab]");
      buttons.forEach((btn) => {
        btn.addEventListener("click", () => {
          const key = btn.dataset.tab;
          buttons.forEach((b) => b.classList.toggle("is-active", b === btn));
          scope.querySelectorAll("[data-panel]").forEach((panel) => {
            panel.classList.toggle("is-active", panel.dataset.panel === key);
          });
        });
      });
    });
  }

  function noticeItem(notice) {
    const date = B.parseDate(notice.publishedAt);
    const day = date ? String(date.getDate()).padStart(2, "0") : "—";
    const month = date ? date.getMonth() + 1 + "月" : "";
    return `<a class="notice-item" href="/notices.html?id=${notice.id}">
      <div class="notice-date"><b>${day}</b><span>${month}</span></div>
      <div class="notice-body">
        <h4>${B.escapeHtml(notice.title)} ${notice.pinned ? '<span class="pin">置顶</span>' : ""}</h4>
        <p>${B.escapeHtml(notice.content || "")}</p>
      </div>
    </a>`;
  }

  function scrollToTop() {
    const btn = document.getElementById("toTop");
    if (!btn) return;
    window.addEventListener("scroll", () => {
      btn.classList.toggle("is-visible", window.scrollY > 500);
    });
    btn.addEventListener("click", () => window.scrollTo({ top: 0, behavior: "smooth" }));
  }

  window.BTRCommon = {
    mountLayout,
    initHeader,
    markActiveNav,
    contestCard,
    pagination,
    initTabs,
    noticeItem,
    scrollToTop,
  };

  document.addEventListener("DOMContentLoaded", () => {
    mountLayout();
    scrollToTop();
  });
})();
