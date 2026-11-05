(function () {
  const B = window.BTR;
  let contests = [];
  let contestId = Number(B.qs("contestId"));

  function playerLink(row) {
    const name = B.escapeHtml(row.author || row.realName || "选手");
    if (!row.userId) return name;
    return `<a href="/portfolio.html?id=${row.userId}" class="player-link" style="color:var(--brand,#e8a33d);text-decoration:underline;text-underline-offset:2px">${name}</a>`;
  }

  function downloadUrl(url) {
    if (!url) return "";
    return /\/download$/i.test(url) ? url : url.replace(/\/files\/([^/]+(?:\/[^/]+)?)$/, "/files/$1/download");
  }

  function workActions(row) {
    if (!row.fileUrl) return "";
    const url = B.escapeHtml(row.fileUrl);
    const name = B.escapeHtml(row.fileName || "作品文件");
    return `<div style="display:flex;gap:6px;margin-top:8px;flex-wrap:wrap">
      <a class="btn btn-outline btn-xs" href="${B.escapeHtml(downloadUrl(row.fileUrl))}" download="${name}">下载作品</a>
      <button class="btn btn-outline btn-xs" data-preview="${url}" data-name="${name}" onclick="window.previewWork && previewWork(this)">在线预览</button>
    </div>`;
  }

  /** 动态加载开源播放器 DPlayer（MIT），避免重复注入 */
  function ensureDPlayer(cb) {
    if (window.DPlayer) { cb && cb(); return; }
    const s = document.createElement("script");
    s.src = "/assets/js/dplayer.min.js";
    s.onload = function () { cb && cb(); };
    s.onerror = function () { alert("播放器加载失败，请使用下载作品"); };
    document.head.appendChild(s);
  }

  /** 通用预览弹窗：创建遮罩+内容框，mountFn(box, close) 渲染内容 */
  function openPreviewModal(title, mountFn, minHeight) {
    const modal = document.createElement("div");
    modal.style.cssText = "position:fixed;inset:0;z-index:9999;background:rgba(0,0,0,.72);display:flex;align-items:center;justify-content:center;padding:10px;box-sizing:border-box;";
    modal.innerHTML =
      '<div style="background:#fff;border-radius:12px;width:min(94vw,860px);max-width:860px;padding:14px;box-sizing:border-box;position:relative;box-shadow:0 8px 40px rgba(0,0,0,.35)' + (minHeight ? ";min-height:" + minHeight : "") + '">' +
      '<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;gap:10px">' +
      '<b style="font-size:15px;color:#222;word-break:break-all;line-height:1.4">' + B.escapeHtml(title || "作品预览") + '</b>' +
      '<button id="pm-close" style="border:none;background:#f1f2f4;border-radius:50%;width:34px;height:34px;font-size:18px;line-height:1;cursor:pointer;flex:none;color:#555">×</button></div>' +
      '<div id="pm-body" style="width:100%"></div></div>';
    document.body.appendChild(modal);
    function close() { modal.remove(); }
    const closeBtn = modal.querySelector("#pm-close");
    if (closeBtn) closeBtn.onclick = close;
    modal.addEventListener("click", function (ev) { if (ev.target === modal) close(); });
    const box = modal.querySelector("#pm-body");
    try { mountFn(box, close); } catch (e) { box.innerHTML = '<p style="padding:20px;color:#888">预览失败，请使用下载作品</p>'; }
  }

  /** 通用按需加载 JS 库（开源库本地托管） */
  function ensureLib(names, cb) {
    const need = names.filter(function (n) { return !document.querySelector('script[data-lib="' + n + '"]'); });
    if (!need.length) { cb && cb(); return; }
    let done = 0;
    need.forEach(function (n) {
      const s = document.createElement("script");
      s.src = "/assets/js/" + n;
      s.dataset.lib = n;
      s.onload = function () { done++; if (done === need.length) cb && cb(); };
      s.onerror = function () { alert("预览组件加载失败，请使用下载作品"); };
      document.head.appendChild(s);
    });
  }

  /** 音视频预览：DPlayer 开源播放器 */
  function previewMedia(url, name) {
    openPreviewModal(name, function (box) {
      ensureDPlayer(function () {
        if (!box.isConnected) return;
        const opt = { container: box, autoplay: false, lang: "zh-cn" };
        opt.video = { url: url, type: "auto" };
        try { new DPlayer(opt); } catch (e) { box.innerHTML = '<p style="padding:20px;color:#888">播放器初始化失败，请使用下载作品</p>'; }
      });
    });
  }

  /** Word 文档预览（docx-preview 开源库，MIT） */
  function previewDocx(url, name) {
    openPreviewModal(name, function (box) {
      box.style.minHeight = "70vh";
      box.style.overflow = "auto";
      box.style.background = "#f7f8fa";
      box.innerHTML = '<p style="padding:24px;color:#888">文档加载中…</p>';
      ensureLib(["jszip.min.js", "docx-preview.min.js"], function () {
        if (!box.isConnected) return;
        fetch(url).then(function (r) { return r.arrayBuffer(); }).then(function (buf) {
          box.innerHTML = "";
          return window.docx.renderAsync(buf, box, null, { inWrapper: false });
        }).catch(function () { box.innerHTML = '<p style="padding:20px;color:#888">文档解析失败，请使用下载作品</p>'; });
      });
    });
  }

  /** Excel 预览（SheetJS 开源库，Apache 2.0） */
  function previewXlsx(url, name) {
    openPreviewModal(name, function (box) {
      box.style.minHeight = "60vh";
      box.innerHTML = '<p style="padding:24px;color:#888">表格加载中…</p>';
      ensureLib(["xlsx.full.min.js"], function () {
        if (!box.isConnected) return;
        fetch(url).then(function (r) { return r.arrayBuffer(); }).then(function (buf) {
          const wb = window.XLSX.read(buf, { type: "array" });
          const sheet = wb.Sheets[wb.SheetNames[0]];
          const html = window.XLSX.utils.sheet_to_html(sheet);
          box.innerHTML = '<div style="overflow:auto;max-height:70vh">' + html + '</div>';
        }).catch(function () { box.innerHTML = '<p style="padding:20px;color:#888">表格解析失败，请使用下载作品</p>'; });
      });
    });
  }

  function previewWork(btn) {
    const url = btn && btn.getAttribute("data-preview");
    const name = (btn && btn.getAttribute("data-name")) || "";
    const ext = name.split(".").pop().toLowerCase();
    if (!url) { alert("此文件不可预览"); return; }
    const media = ["mp4", "mov", "webm", "ogg", "mp3", "wav", "m4a", "aac"];
    const inline = ["jpg", "jpeg", "png", "gif", "webp", "bmp", "pdf", "txt", "md"];
    if (media.includes(ext)) return previewMedia(url, name);
    if (ext === "docx") return previewDocx(url, name);
    if (ext === "xlsx" || ext === "xls") return previewXlsx(url, name);
    if (inline.includes(ext)) return window.open(url, "_blank", "noopener");
    alert("此文件不可预览");
  }
  window.previewWork = previewWork;

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
          ${workActions(row)}
        </div>`;
      })
      .join("");

    const groups = rank.groups || [];
    const table = `<div class="admin-table-card" style="margin-top: 24px">
      <div class="table-wrap">
        <table class="table">
          <thead><tr><th>排名</th><th>作品</th><th>选手</th><th>学校</th><th>奖品 / 奖金</th><th>评审人数</th></tr></thead>
          <tbody>${rows
            .map(
              (row) => `<tr>
                <td><span class="rank-no ${row.rank <= 3 ? "top" : ""}">${row.rank || "—"}</span></td>
                <td class="title-cell">${B.escapeHtml(row.workTitle)}${workActions(row)}</td>
                <td>${playerLink(row)}</td>
                <td>${B.escapeHtml(row.school || "—")}</td>
                <td>${
                  B.formatPrize(row)
                    ? `<b class="mono" style="color:var(--brand,#e8a33d)">${B.escapeHtml(B.formatPrize(row))}</b>`
                    : '<span class="muted">未获奖</span>'
                }</td>
                <td class="num">${row.scoreCount}</td>
              </tr>`
            )
            .join("")}</tbody>
        </table>
      </div>
    </div>`;

    host.innerHTML = `
      <div class="result-hero" style="padding-top: 8px">
        <span class="eyebrow">Final Ranking</span>
        <h1>${B.escapeHtml(rank.contestTitle)}</h1>
        <p>共 ${rank.workCount} 件作品 · ${rank.judgeCount} 位评委参与 · 更新于 ${B.formatDate(rank.updatedAt, true)}</p>
      </div>
      <div class="podium">${podium}</div>
      <div style="margin-top: 24px">${table}</div>`;
  }

  document.addEventListener("DOMContentLoaded", init);
})();
