(function () {
  const B = window.BTR;
  const C = window.BTRCommon;
  const state = { keyword: B.qs("keyword") || "", page: Number(B.qs("page") || 1), size: 8 };

  function syncUrl() {
    const params = new URLSearchParams();
    if (state.keyword) params.set("keyword", state.keyword);
    if (state.page > 1) params.set("page", String(state.page));
    const query = params.toString();
    history.replaceState(null, "", location.pathname + (query ? "?" + query : ""));
  }

  async function load() {
    const host = document.getElementById("noticeList");
    host.innerHTML = Array.from({ length: 3 })
      .map(() => '<div class="card skeleton" style="height: 112px"></div>')
      .join("");
    try {
      const params = new URLSearchParams({ page: state.page, size: state.size });
      if (state.keyword) params.set("keyword", state.keyword);
      const data = await B.api.get("/notices?" + params.toString());
      render(data);
    } catch (e) {
      host.innerHTML = `<div class="card">${B.emptyState("公告加载失败", e.message || "请稍后重试")}</div>`;
      document.getElementById("noticePagination").innerHTML = "";
    }
  }

  function render(data) {
    const host = document.getElementById("noticeList");
    const list = data.records || [];
    if (!list.length) {
      host.innerHTML = `<div class="card">${B.emptyState(
        "没有找到公告",
        state.keyword ? "换个关键词试试" : "平台公告会在这里更新"
      )}</div>`;
      document.getElementById("noticePagination").innerHTML = "";
      return;
    }
    host.innerHTML = list
      .map(
        (n) => `<article class="notice-card" data-notice="${n.id}" role="button" tabindex="0">
          <h3>${B.escapeHtml(n.title)} ${n.pinned ? '<span class="pin">置顶</span>' : ""}</h3>
          <p>${B.escapeHtml(n.content || "")}</p>
          <div class="meta">发布于 ${B.formatDate(n.publishedAt, true)} · 点击查看全文</div>
        </article>`
      )
      .join("");

    host.querySelectorAll("[data-notice]").forEach((card) => {
      card.addEventListener("click", () => openNotice(Number(card.dataset.notice)));
      card.addEventListener("keydown", (e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          openNotice(Number(card.dataset.notice));
        }
      });
    });

    C.pagination(
      document.getElementById("noticePagination"),
      state.page,
      data.total,
      state.size,
      (page) => {
        state.page = page;
        syncUrl();
        load();
        window.scrollTo({ top: 0, behavior: "smooth" });
      }
    );
  }

  async function openNotice(id) {
    const modal = document.getElementById("noticeModal");
    document.getElementById("noticeModalTitle").textContent = "加载中…";
    document.getElementById("noticeModalMeta").textContent = "";
    document.getElementById("noticeModalBody").innerHTML =
      '<div class="skeleton" style="height: 120px"></div>';
    modal.classList.add("is-open");
    document.body.style.overflow = "hidden";
    try {
      const notice = await B.api.get("/notices/" + id);
      document.getElementById("noticeModalTitle").textContent = notice.title;
      document.getElementById("noticeModalMeta").textContent =
        "发布于 " + B.formatDate(notice.publishedAt, true) + (notice.pinned ? " · 置顶公告" : "");
      document.getElementById("noticeModalBody").innerHTML = formatContent(notice.content);
      history.replaceState(null, "", "/notices.html?id=" + id);
    } catch (e) {
      document.getElementById("noticeModalTitle").textContent = "公告不存在";
      document.getElementById("noticeModalBody").innerHTML =
        '<p class="muted">' + B.escapeHtml(e.message || "请稍后重试") + "</p>";
    }
  }

  function formatContent(text) {
    if (!text) return '<p class="muted">暂无内容。</p>';
    return String(text)
      .split(/\n+/)
      .filter(Boolean)
      .map((line) => `<p>${B.escapeHtml(line)}</p>`)
      .join("");
  }

  function closeModal() {
    document.getElementById("noticeModal").classList.remove("is-open");
    document.body.style.overflow = "";
    if (B.qs("id")) history.replaceState(null, "", "/notices.html");
  }

  document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("noticeModalClose").addEventListener("click", closeModal);
    document.getElementById("noticeModalDone").addEventListener("click", closeModal);
    document.getElementById("noticeModal").addEventListener("click", (e) => {
      if (e.target.id === "noticeModal") closeModal();
    });
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") closeModal();
    });

    const keyword = document.getElementById("noticeKeyword");
    keyword.value = state.keyword;
    keyword.addEventListener(
      "input",
      B.debounce(() => {
        state.keyword = keyword.value.trim();
        state.page = 1;
        syncUrl();
        load();
      }, 320)
    );

    load();
    const id = Number(B.qs("id"));
    if (id) openNotice(id);
  });
})();
