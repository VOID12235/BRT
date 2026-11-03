(function () {
  const B = window.BTR;
  const C = window.BTRCommon;
  const state = {
    status: B.qs("status") || "",
    keyword: B.qs("keyword") || "",
    page: Number(B.qs("page") || 1),
    size: 9,
  };

  function syncUrl() {
    const params = new URLSearchParams();
    if (state.status) params.set("status", state.status);
    if (state.keyword) params.set("keyword", state.keyword);
    if (state.page > 1) params.set("page", String(state.page));
    const query = params.toString();
    history.replaceState(null, "", location.pathname + (query ? "?" + query : ""));
  }

  async function load() {
    const grid = document.getElementById("contestGrid");
    grid.innerHTML = Array.from({ length: 6 })
      .map(() => '<div class="card skeleton" style="height: 356px"></div>')
      .join("");
    try {
      const params = new URLSearchParams({ page: state.page, size: state.size });
      if (state.status) params.set("status", state.status);
      if (state.keyword) params.set("keyword", state.keyword);
      const data = await B.api.get("/contests?" + params.toString());
      render(data);
    } catch (e) {
      grid.innerHTML = `<div class="card" style="grid-column:1/-1">${B.emptyState(
        "赛事加载失败",
        e.message || "请稍后重试"
      )}</div>`;
      document.getElementById("pagination").innerHTML = "";
      document.getElementById("resultCount").textContent = "";
    }
  }

  function render(data) {
    const grid = document.getElementById("contestGrid");
    const list = data.records || [];
    document.getElementById("resultCount").textContent = "共 " + data.total + " 场赛事";
    if (!list.length) {
      grid.innerHTML = `<div class="card" style="grid-column:1/-1">${B.emptyState(
        "没有找到匹配的赛事",
        "换个关键词或状态筛选试试"
      )}</div>`;
      document.getElementById("pagination").innerHTML = "";
      return;
    }
    grid.innerHTML = list.map((c) => C.contestCard(c)).join("");
    C.pagination(document.getElementById("pagination"), state.page, data.total, state.size, (page) => {
      state.page = page;
      syncUrl();
      load();
      window.scrollTo({ top: 0, behavior: "smooth" });
    });
  }

  function initTabs() {
    document.querySelectorAll("#statusTabs button").forEach((btn) => {
      btn.classList.toggle("is-active", btn.dataset.status === state.status);
      btn.addEventListener("click", () => {
        state.status = btn.dataset.status;
        state.page = 1;
        document.querySelectorAll("#statusTabs button").forEach((b) => b.classList.toggle("is-active", b === btn));
        syncUrl();
        load();
      });
    });
  }

  document.addEventListener("DOMContentLoaded", () => {
    initTabs();
    const keyword = document.getElementById("keyword");
    keyword.value = state.keyword;
    keyword.addEventListener(
      "input",
      B.debounce(() => {
        state.keyword = keyword.value.trim();
        state.page = 1;
        syncUrl();
        load();
      }, 350)
    );
    load();
  });
})();
