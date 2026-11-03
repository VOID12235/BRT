(function () {
  const B = window.BTR;
  let contests = [];
  let contestId = Number(B.qs("contestId"));

  function playerLink(row) {
    const name = B.escapeHtml(row.author || row.realName || "匿名选手");
    if (!row.userId) return name;
    return `<a href="/portfolio.html?id=${row.userId}" class="player-link" style="color:var(--brand,#e8a33d);text-decoration:underline;text-underline-offset:2px">${name}</a>`;
  }

  async function init() {
    try {
      const data = await B.api.get("/contests?page=1&size=50");
      contests = (data.records || []).filter(
        (c) => c.status === "PUBLISHED" || c.status === "FINISHED"
      );
      if (!contests.length) {
        contests = data.records || [];
      }
    } catch (e) {
      contests = [];
    }

    const select = document.getElementById("contestSelect");
    if (!contests.length) {
      select.innerHTML = '<option value="">暂无可查看的赛事</option>';
      renderEmpty("暂无已公示赛事", "赛事进入公示阶段后，榜单会在这里展示");
      return;
    }

    select.innerHTML = contests
      .map((c) => `<option value="${c.id}">${B.escapeHtml(c.title)}</option>`)
      .join("");
    if (!contestId || !contests.some((c) => c.id === contestId)) {
      contestId = contests[0].id;
    }
    select.value = String(contestId);
    select.addEventListener("change", () => {
      contestId = Number(select.value);
      history.replaceState(null, "", "/rank.html?contestId=" + contestId);
      load();
    });
    load();
  }

  async function load() {
    const host = document.getElementById("rankContent");
    host.innerHTML = '<div class="card skeleton" style="height: 420px"></div>';
    try {
      const rank = await B.api.get("/ranks?contestId=" + contestId);
      render(rank);
    } catch (e) {
      renderEmpty("榜单加载失败", e.message || "请稍后重试");
    }
  }

  function renderEmpty(title, desc) {
    document.getElementById("rankContent").innerHTML = `<div class="card">${B.emptyState(title, desc)}</div>`;
  }

  function render(rank) {
    const host = document.getElementById("rankContent");
    if (!rank.published) {
      host.innerHTML = `<div class="card">${B.emptyState(
        "榜单尚未公示",
        "赛事当前状态为「" + B.statusInfo(rank.status).label + "」，评审结束后将统一公示"
      )}</div>`;
      return;
    }
    const rows = rank.rows || [];
    if (!rows.length) {
      host.innerHTML = `<div class="card">${B.emptyState("暂无排名数据", "等待评委完成打分")}</div>`;
      return;
    }

    const top = rows.slice(0, 3);
    const podiumOrder = [top[1], top[0], top[2]].filter(Boolean);
    const podium = podiumOrder
      .map((row) => {
        const isFirst = row.rank === 1;
        const prize = B.formatPrize(row);
        return `<div class="podium-card ${isFirst ? "first" : ""}">
          <div class="podium-rank">${row.rank}</div>
          <h4>${B.escapeHtml(row.workTitle)}</h4>
          <p>${playerLink(row)}${row.school ? " · " + B.escapeHtml(row.school) : ""}</p>
          <div class="podium-score">${
            prize ? B.escapeHtml(prize) : '<span class="muted">—</span>'
          }</div>
          ${row.awardName ? `<span class="award-pill ${isFirst ? "gold" : "silver"}" style="margin-top:10px">${B.escapeHtml(row.awardName)}</span>` : ""}
        </div>`;
      })
      .join("");

    const groups = rank.groups || [];
    const table = `<div class="admin-table-card" style="margin-top: 24px">
      <div class="table-wrap">
        <table class="table">
          <thead><tr><th>排名</th><th>作品</th><th>选手</th><th>学校</th><th>奖励</th><th>评审人数</th><th>奖项</th></tr></thead>
          <tbody>${rows
            .map(
              (row) => `<tr>
                <td><span class="rank-no ${row.rank <= 3 ? "top" : ""}">${row.rank}</span></td>
                <td class="title-cell">${B.escapeHtml(row.workTitle)}</td>
                <td>${playerLink(row)}</td>
                <td>${B.escapeHtml(row.school || "—")}</td>
                <td>${
                  B.formatPrize(row)
                    ? `<b class="mono" style="color:var(--brand,#e8a33d)">${B.escapeHtml(B.formatPrize(row))}</b>`
                    : '<span class="muted">—</span>'
                }</td>
                <td class="num">${row.scoreCount}</td>
                <td>${row.awardName ? `<span class="award-pill">${B.escapeHtml(row.awardName)}</span>` : '<span class="muted">—</span>'}</td>
              </tr>`
            )
            .join("")}</tbody>
        </table>
      </div>
    </div>`;

    const groupPanel = groups.length
      ? `<div class="card side-card" style="margin-top: 20px">
          <h3>奖项结果</h3>
          <div class="stack">${groups
            .map(
              (g) => `<div>
                <div class="row-between" style="margin-bottom: 8px">
                  <span class="award-pill ${g.awardName.includes("一") ? "gold" : g.awardName.includes("二") ? "silver" : "bronze"}">${B.escapeHtml(g.awardName)}</span>
                  <span class="tiny muted">${
                    B.formatPrize(g)
                      ? B.escapeHtml(B.formatPrize(g)) + " · "
                      : ""
                  }${g.items.length} / ${g.quota} 个名额</span>
                </div>
                ${g.items
                  .map(
                    (item) => `<div class="row" style="padding: 6px 0; font-size: 12px">
                      <span class="muted mono">#${item.rank}</span>
                      <span class="grow" style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${B.escapeHtml(item.workTitle)}</span>
                      <b class="mono" style="color:var(--brand,#e8a33d)">${
                        B.formatPrize(item)
                          ? B.escapeHtml(B.formatPrize(item))
                          : "—"
                      }</b>
                    </div>`
                  )
                  .join("")}
              </div>`
            )
            .join("")}</div>
        </div>`
      : "";

    host.innerHTML = `
      <div class="result-hero" style="padding-top: 8px">
        <span class="eyebrow">Final Ranking</span>
        <h1>${B.escapeHtml(rank.contestTitle)}</h1>
        <p>共 ${rank.workCount} 件作品 · ${rank.judgeCount} 位评委参与 · 更新于 ${B.formatDate(rank.updatedAt, true)}</p>
      </div>
      <div class="podium">${podium}</div>
      <div class="split" style="margin-top: 8px">
        <div>${table}</div>
        <aside>
          <div class="card side-card">
            <h3>赛事信息</h3>
            <div class="meta-list">
              <div><span>赛事状态</span><b>${B.statusInfo(rank.status).label}</b></div>
              <div><span>提交作品</span><b>${rank.workCount}</b></div>
              <div><span>参与评委</span><b>${rank.judgeCount}</b></div>
              <div><span>已评分作品</span><b>${rank.scoredCount}</b></div>
            </div>
          </div>
          ${groupPanel}
        </aside>
      </div>`;
  }

  document.addEventListener("DOMContentLoaded", init);
})();
