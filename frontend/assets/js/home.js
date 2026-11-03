(function () {
  const B = window.BTR;
  const C = window.BTRCommon;

  let slides = [];
  let slideIndex = 0;
  let timer = null;

  async function init() {
    const [contests, notices] = await Promise.allSettled([
      B.api.get("/contests?page=1&size=8"),
      B.api.get("/notices?page=1&size=4"),
    ]);

    const list = contests.status === "fulfilled" ? contests.value.records || [] : [];
    renderContests(list);
    renderPoster(list);

    if (notices.status === "fulfilled") {
      renderNotices(notices.value.records || []);
    } else {
      document.getElementById("homeNotices").innerHTML = B.emptyState("暂无公告", "稍后再来看看");
    }

    loadStats(list);
    loadRank(list);
  }

  function renderContests(list) {
    const host = document.getElementById("homeContests");
    if (!list.length) {
      host.innerHTML = `<div class="card" style="grid-column: 1/-1">${B.emptyState(
        "暂时没有公开赛事",
        "管理员发布赛事后会在这里展示"
      )}</div>`;
      return;
    }
    host.innerHTML = list.slice(0, 3).map((c) => C.contestCard(c)).join("");
  }

  function renderPoster(list) {
    slides = list.filter((c) => c.status !== "DRAFT").slice(0, 3);
    const frame = document.getElementById("posterFrame");
    const dots = document.getElementById("posterDots");
    if (!slides.length) {
      frame.innerHTML = `<img src="/assets/img/cover-1.svg" alt="BTR" />
        <div class="poster-copy"><span class="badge badge-plain">BTR</span><h3>新赛季即将开始</h3><p>关注公告，第一时间获取赛事信息。</p></div>
        <span class="poster-tag"><i></i> 2026 赛季</span>`;
      dots.innerHTML = "";
      return;
    }
    frame.innerHTML =
      slides
        .map(
          (c, i) => `<div class="poster-slide ${i === 0 ? "is-active" : ""}">
            <img src="${B.escapeHtml(c.cover || "/assets/img/cover-1.svg")}" alt="${B.escapeHtml(c.title)}" />
            <div class="poster-copy">
              <span class="badge badge-plain">${B.statusInfo(c.status).label}</span>
              <h3>${B.escapeHtml(c.title)}</h3>
              <p>${B.escapeHtml(c.summary || "")}</p>
            </div>
          </div>`
        )
        .join("") + `<span class="poster-tag"><i></i> 2026 赛季</span>`;

    dots.innerHTML = slides
      .map((_, i) => `<button type="button" class="${i === 0 ? "is-active" : ""}" data-slide="${i}" aria-label="第 ${i + 1} 张"></button>`)
      .join("");
    dots.querySelectorAll("button").forEach((btn) => {
      btn.addEventListener("click", () => goTo(Number(btn.dataset.slide)));
    });

    if (timer) clearInterval(timer);
    if (slides.length > 1) {
      timer = setInterval(() => goTo((slideIndex + 1) % slides.length), 5000);
    }
  }

  function goTo(index) {
    const frames = document.querySelectorAll(".poster-slide");
    const dots = document.querySelectorAll("#posterDots button");
    if (!frames.length) return;
    slideIndex = (index + frames.length) % frames.length;
    frames.forEach((f, i) => f.classList.toggle("is-active", i === slideIndex));
    dots.forEach((d, i) => d.classList.toggle("is-active", i === slideIndex));
  }

  function renderNotices(list) {
    const host = document.getElementById("homeNotices");
    if (!list.length) {
      host.innerHTML = B.emptyState("暂无公告", "平台公告会在这里更新");
      return;
    }
    host.innerHTML = list.map((n) => C.noticeItem(n)).join("");
  }

  async function loadStats(list) {
    try {
      const stats = await B.api.get("/stats");
      setStat("statContests", stats.contests);
      setStat("statSignups", stats.signups);
      setStat("statWorks", stats.works);
    } catch (e) {
      setStat("statContests", list.length);
      setStat("statSignups", list.reduce((sum, c) => sum + (c.signupCount || 0), 0));
      setStat("statWorks", list.reduce((sum, c) => sum + (c.workCount || 0), 0));
    }
  }

  function setStat(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = Number(value || 0).toLocaleString("zh-CN");
  }

  async function loadRank(list) {
    const contest = list.find((c) => c.status === "PUBLISHED") || list.find((c) => c.status === "FINISHED");
    const host = document.getElementById("homeRank");
    if (!contest) {
      host.innerHTML = B.emptyState("榜单尚未公示", "赛事公示后会在这里展示");
      return;
    }
    document.getElementById("rankSubtitle").textContent = contest.title;
    try {
      const rank = await B.api.get("/ranks?contestId=" + contest.id);
      const rows = (rank.rows || []).slice(0, 4);
      if (!rows.length) {
        host.innerHTML = B.emptyState("暂无排名", "等待评委完成打分");
        return;
      }
      host.innerHTML = rows
        .map(
          (row) => `<div class="rank-mini-row">
            <span class="rank-no ${row.rank === 1 ? "top" : ""}">${row.rank}</span>
            <div class="info"><b>${B.escapeHtml(row.workTitle)}</b><span>${B.escapeHtml(
            row.author || ""
          )} · ${B.escapeHtml(row.school || "")}</span></div>
            <span class="score">${Number(row.avgScore).toFixed(1)}</span>
          </div>`
        )
        .join("");
    } catch (e) {
      host.innerHTML = B.emptyState("榜单暂不可用", "请稍后再试");
    }
  }

  document.addEventListener("DOMContentLoaded", init);
})();
