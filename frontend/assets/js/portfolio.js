(function () {
  const B = window.BTR;
  const userId = Number(B.qs("id"));
  const ROLE_CN = { PLAYER: "选手", JUDGE: "评委", ADMIN: "管理员" };

  function avatarBlock(user) {
    if (user.avatar) {
      return `<img src="${B.escapeHtml(user.avatar)}" alt="头像" style="width:72px;height:72px;border-radius:50%;object-fit:cover" />`;
    }
    return `<div style="width:72px;height:72px;border-radius:50%;background:linear-gradient(135deg,var(--brand),#7c63e8);color:#fff;display:flex;align-items:center;justify-content:center;font-size:28px;font-weight:600">${B.escapeHtml(
      (user.nickname || "U").slice(0, 1)
    )}</div>`;
  }

  async function load() {
    if (!userId) {
      document.getElementById("portfolioTitle").textContent = "作品集不存在";
      document.getElementById("portfolioHero").innerHTML = B.emptyState("缺少选手参数", "请从选手页面进入作品集");
      return;
    }
    try {
      const data = await B.api.get("/users/" + userId + "/portfolio");
      render(data);
    } catch (e) {
      document.getElementById("portfolioTitle").textContent = "作品集不存在";
      document.getElementById("portfolioHero").innerHTML = B.emptyState("未找到该选手", e.message || "请确认选手信息");
      document.getElementById("portfolioStats").innerHTML = "";
      document.getElementById("portfolioWorks").innerHTML = "";
      document.getElementById("portfolioAwards").innerHTML = "";
    }
  }

  function render(data) {
    const user = data.user || {};
    document.title = (user.nickname || "选手") + " 的作品集 · BTR 赛事平台";
    document.getElementById("portfolioTitle").textContent = user.nickname + " 的作品集";

    document.getElementById("portfolioHero").innerHTML = `
      ${avatarBlock(user)}
      <div style="flex:1;min-width:0">
        <h2 style="margin:0 0 4px">${B.escapeHtml(user.nickname || "选手")} <span class="badge badge-soft" style="margin-left:6px">${ROLE_CN[user.role] || user.role || "选手"}</span></h2>
        <div class="muted small">${user.email ? B.escapeHtml(user.email) : ""}${user.phone ? " · " + B.escapeHtml(user.phone) : ""}</div>
      </div>`;

    const cards = [
      ["报名赛事", data.signupCount || 0],
      ["参赛作品", data.workCount || 0],
      ["获奖记录", data.awardCount || 0],
    ];
    document.getElementById("portfolioStats").innerHTML = cards
      .map(
        ([label, value]) =>
          `<div class="card stat-card"><div class="value">${Number(value || 0).toLocaleString("zh-CN")}</div><div class="label">${label}</div></div>`
      )
      .join("");

    const works = data.works || [];
    const worksHost = document.getElementById("portfolioWorks");
    if (!works.length) {
      worksHost.innerHTML = B.emptyState("暂无作品", "该选手尚未提交作品");
    } else {
      worksHost.innerHTML = works
        .map(
          (w) => `<div class="work-item">
            <div style="flex:1;min-width:0">
              <h4 style="margin:0 0 4px">${B.escapeHtml(w.title)}${w.awardName ? ` <span class="pin">${B.escapeHtml(w.awardName)}</span>` : ""}</h4>
              <p class="muted" style="margin:0 0 8px">${B.escapeHtml(w.contestTitle || "—")}${w.contestMode === "OFFLINE" ? " · 线下比赛" : ""}</p>
              ${w.description ? `<p class="small muted" style="margin:0 0 6px">${B.escapeHtml(String(w.description).slice(0, 120))}${String(w.description).length > 120 ? "…" : ""}</p>` : ""}
              <div class="meta small muted">
                ${w.fileName ? "文件 " + B.escapeHtml(w.fileName) : ""}
                ${w.avgScore != null ? " · 平均分 " + w.avgScore : ""}
                ${w.rankNo != null ? " · 第 " + w.rankNo + " 名" : ""}
              </div>
            </div>
          </div>`
        )
        .join("");
    }

    const awards = data.awards || [];
    const awardsHost = document.getElementById("portfolioAwards");
    if (!awards.length) {
      awardsHost.innerHTML = B.emptyState("暂无获奖", "该选手尚未获得奖项");
    } else {
      awardsHost.innerHTML = awards
        .map(
          (a) => `<div class="work-item">
            <div style="flex:1;min-width:0">
              <div style="display:flex;align-items:center;gap:8px">
                <span class="award-pill ${a.rankNo === 1 ? "gold" : a.rankNo === 2 ? "silver" : "bronze"}">${B.escapeHtml(a.awardName || "奖项")}</span>
                ${a.rankNo != null ? `<span class="muted small">第 ${a.rankNo} 名</span>` : ""}
              </div>
              <h4 style="margin:8px 0 4px">${B.escapeHtml(a.contestTitle || "—")}</h4>
              <p class="muted small" style="margin:0">${B.formatPrize(a) ? B.escapeHtml(B.formatPrize(a)) : "荣誉奖项"}${a.publishedAt ? " · " + B.formatDate(a.publishedAt) : ""}</p>
            </div>
          </div>`
        )
        .join("");
    }
  }

  document.addEventListener("DOMContentLoaded", load);
})();
