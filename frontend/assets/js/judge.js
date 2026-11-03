(function () {
  const B = window.BTR;
  const state = {
    tasks: [],
    progress: [],
    contests: {},
    contestId: "",
    activeWorkId: null,
  };

  function isScorePage() {
    return /judge-score\.html$/.test(location.pathname);
  }

  async function init() {
    const user = B.requireAuth(["JUDGE", "ADMIN"]);
    if (!user) return;
    const nameEl = document.getElementById("judgeName");
    if (nameEl) nameEl.textContent = user.nickname || user.account || "评委";

    const logout = document.getElementById("logoutBtn");
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

    await loadAll();
    bindFilter();
    if (isScorePage()) {
      initScorePage();
    } else {
      renderIndex();
    }
  }

  async function loadAll() {
    const [tasks, progress, contests] = await Promise.allSettled([
      B.api.get("/judge/tasks"),
      B.api.get("/judge/progress"),
      B.api.get("/judge/contests"),
    ]);
    state.tasks = tasks.status === "fulfilled" ? tasks.value || [] : [];
    state.progress = progress.status === "fulfilled" ? progress.value || [] : [];
    if (contests.status === "fulfilled") {
      (contests.value || []).forEach((c) => (state.contests[c.id] = c));
    }
  }

  function bindFilter() {
    const select = document.getElementById("contestFilter");
    if (!select) return;
    const ids = [...new Set(state.tasks.map((t) => t.contestId))];
    select.innerHTML =
      '<option value="">全部赛事</option>' +
      ids
        .map((id) => {
          const task = state.tasks.find((t) => t.contestId === id);
          return `<option value="${id}">${B.escapeHtml(task ? task.contestTitle : "赛事 " + id)}</option>`;
        })
        .join("");
    select.value = state.contestId;
    select.addEventListener("change", () => {
      state.contestId = select.value;
      if (isScorePage()) {
        renderTaskList();
        const first = filteredTasks()[0];
        if (first) selectWork(first.workId);
      } else {
        renderIndex();
      }
    });
  }

  function filteredTasks() {
    if (!state.contestId) return state.tasks;
    return state.tasks.filter((t) => String(t.contestId) === String(state.contestId));
  }

  function isScored(task) {
    return task.score !== null && task.score !== undefined;
  }

  /* ------------------------------ 任务列表页 ------------------------------ */
  function renderIndex() {
    const tasks = filteredTasks();
    const scored = tasks.filter(isScored).length;
    const stats = document.querySelectorAll("#judgeStats .stat-card .value");
    if (stats.length === 3) {
      stats[0].textContent = tasks.length;
      stats[1].textContent = scored;
      stats[2].textContent = tasks.length - scored;
    }

    const tbody = document.getElementById("taskTable");
    if (!tasks.length) {
      tbody.innerHTML = `<tr><td colspan="6">${B.emptyState(
        "暂无评审任务",
        "管理员分配作品后，任务会出现在这里"
      )}</td></tr>`;
    } else {
      tbody.innerHTML = tasks
        .map((task) => {
          const done = isScored(task);
          return `<tr data-work="${task.workId}">
            <td class="title-cell">${B.escapeHtml(task.workTitle)}</td>
            <td>${B.escapeHtml(task.contestTitle)}</td>
            <td>${B.escapeHtml(task.realName || "—")}${task.school ? `<span class="muted tiny"> · ${B.escapeHtml(task.school)}</span>` : ""}</td>
            <td>${done ? '<span class="badge badge-signup">已评分</span>' : '<span class="badge badge-review">待评分</span>'}</td>
            <td>${done ? `<b class="mono">${Number(task.score).toFixed(2)}</b>` : '<span class="muted">—</span>'}</td>
            <td><a class="btn btn-outline btn-xs" href="/judge/judge-score.html?workId=${task.workId}">${done ? "修改" : "打分"}</a></td>
          </tr>`;
        })
        .join("");
      tbody.querySelectorAll("tr[data-work]").forEach((row) => {
        row.style.cursor = "pointer";
        row.addEventListener("click", (e) => {
          if (e.target.closest("a")) return;
          location.href = "/judge/judge-score.html?workId=" + row.dataset.work;
        });
      });
    }

    const progressHost = document.getElementById("progressList");
    if (!state.progress.length) {
      progressHost.innerHTML = B.emptyState("暂无进度", "分配作品后开始统计");
    } else {
      progressHost.innerHTML = state.progress
        .map((p) => {
          const percent = p.assigned ? Math.round((p.scored / p.assigned) * 100) : 0;
          return `<div class="progress-item">
            <div class="top">
              <b>${B.escapeHtml(p.contestTitle)}</b>
              <span>${p.scored} / ${p.assigned}</span>
            </div>
            <div class="progress ${percent === 100 ? "green" : ""}"><i style="width:${percent}%"></i></div>
          </div>`;
        })
        .join("");
    }
  }

  /* ------------------------------ 打分页 ------------------------------ */
  function initScorePage() {
    const queryWorkId = Number(B.qs("workId"));
    renderTaskList();
    const target =
      state.tasks.find((t) => t.workId === queryWorkId) || filteredTasks()[0] || state.tasks[0];
    if (target) {
      selectWork(target.workId);
    } else {
      document.getElementById("workPanel").innerHTML = `<div class="card">${B.emptyState(
        "暂无可评审作品",
        "管理员分配作品后即可开始打分"
      )}</div>`;
    }
  }

  function renderTaskList() {
    const host = document.getElementById("taskList");
    const tasks = filteredTasks();
    const counter = document.getElementById("taskCounter");
    if (counter) counter.textContent = tasks.length + " 件";
    if (!tasks.length) {
      host.innerHTML = B.emptyState("暂无任务", "换个赛事筛选试试");
      return;
    }
    host.innerHTML = tasks
      .map((task) => {
        const done = isScored(task);
        return `<button type="button" class="task-item ${
          task.workId === state.activeWorkId ? "is-active" : ""
        }" data-work="${task.workId}">
          <b>${B.escapeHtml(task.workTitle)}</b>
          <span>${B.escapeHtml(task.contestTitle)}</span>
          <span class="${done ? "done" : ""}" style="float:right">${
          done ? Number(task.score).toFixed(1) + " 分" : "待评分"
        }</span>
        </button>`;
      })
      .join("");
    host.querySelectorAll("[data-work]").forEach((btn) => {
      btn.addEventListener("click", () => selectWork(Number(btn.dataset.work)));
    });
  }

  function selectWork(workId) {
    state.activeWorkId = workId;
    document.querySelectorAll("#taskList [data-work]").forEach((btn) => {
      btn.classList.toggle("is-active", Number(btn.dataset.work) === workId);
    });
    const task = state.tasks.find((t) => t.workId === workId);
    if (!task) return;
    history.replaceState(null, "", "/judge/judge-score.html?workId=" + workId);
    renderWork(task);
  }

  function renderWork(task) {
    const contest = state.contests[task.contestId];
    const contestStatus = contest ? contest.status : "";
    const locked = contestStatus && contestStatus !== "REVIEW";
    const value = isScored(task) ? Number(task.score) : 80;

    document.getElementById("workPanel").innerHTML = `
      <div class="card task-work">
        <div class="row-between wrap" style="align-items:flex-start">
          <div>
            <span class="badge ${isScored(task) ? "badge-signup" : "badge-review"}">${
      isScored(task) ? "已评分" : "待评分"
    }</span>
            <h2 style="margin-top: 12px">${B.escapeHtml(task.workTitle)}</h2>
            <div class="meta">
              <span>${B.escapeHtml(task.contestTitle)}</span>
              <span>选手：${B.escapeHtml(task.realName || "—")}${
      task.school ? " · " + B.escapeHtml(task.school) : ""
    }</span>
              <span>提交于 ${B.formatDate(task.submittedAt, true)}</span>
            </div>
          </div>
          ${
            task.fileUrl
              ? `<a class="btn btn-outline btn-sm" href="${B.escapeHtml(
                  task.fileUrl
                )}" target="_blank" rel="noopener">查看作品文件</a>`
              : '<span class="muted tiny">未上传文件</span>'
          }
        </div>

        <div class="desc">${B.escapeHtml(task.workDescription || "作者未填写作品简介。")}</div>

        ${
          locked
            ? `<div class="alert alert-warn" style="margin-top: 20px">该赛事当前状态为「${B.statusInfo(
                contestStatus
              ).label}」，不在评审阶段，无法提交打分。</div>`
            : ""
        }

        <form class="score-form" id="scoreForm">
          <div class="row-between wrap">
            <label class="strong" for="scoreNumber">综合评分</label>
            <span class="tiny muted">满分 100 分</span>
          </div>
          <div class="score-input-row">
            <input type="number" id="scoreNumber" min="0" max="100" step="0.5" value="${value}" ${
      locked ? "disabled" : ""
    } />
            <input type="range" id="scoreRange" min="0" max="100" step="0.5" value="${value}" ${
      locked ? "disabled" : ""
    } />
          </div>
          <div class="field">
            <label for="scoreComment">评语 *</label>
            <textarea class="textarea" id="scoreComment" maxlength="1000" placeholder="请写下具体、可执行的评审意见" ${
              locked ? "disabled" : ""
            }>${B.escapeHtml(task.comment || "")}</textarea>
            <span class="hint">评语将作为评审记录留存，请客观表达。</span>
          </div>
          <div class="row" style="justify-content:flex-end; margin-top: 18px; gap: 10px">
            <button class="btn btn-ghost" type="button" id="prevTask">上一件</button>
            <button class="btn btn-ghost" type="button" id="nextTask">下一件</button>
            <button class="btn btn-primary" type="submit" id="scoreSubmit" ${
              locked ? "disabled" : ""
            }>${isScored(task) ? "更新评分" : "提交评分"}</button>
          </div>
        </form>
      </div>`;

    bindScoreForm(task);
  }

  function bindScoreForm(task) {
    const number = document.getElementById("scoreNumber");
    const range = document.getElementById("scoreRange");
    const comment = document.getElementById("scoreComment");
    const form = document.getElementById("scoreForm");

    const sync = (from, to) => {
      const v = Math.min(100, Math.max(0, Number(from.value) || 0));
      from.value = v;
      to.value = v;
    };
    number.addEventListener("input", () => sync(number, range));
    range.addEventListener("input", () => sync(range, number));

    form.addEventListener("submit", async (e) => {
      e.preventDefault();
      const score = Number(number.value);
      const text = comment.value.trim();
      if (!Number.isFinite(score) || score < 0 || score > 100) {
        B.toast("分数需在 0–100 之间", "warn");
        return;
      }
      if (!text) {
        B.toast("请填写评语", "warn");
        comment.focus();
        return;
      }
      const btn = document.getElementById("scoreSubmit");
      btn.disabled = true;
      btn.textContent = "提交中…";
      try {
        await B.api.post("/judge/scores", { workId: task.workId, score, comment: text });
        B.toast("评分已提交", "success");
        await loadAll();
        const updated = state.tasks.find((t) => t.workId === task.workId);
        if (updated) renderWork(updated);
        renderTaskList();
      } catch (err) {
        B.toast(err.message || "提交失败", "error");
        btn.disabled = false;
        btn.textContent = isScored(task) ? "更新评分" : "提交评分";
      }
    });

    document.getElementById("prevTask").addEventListener("click", () => move(-1));
    document.getElementById("nextTask").addEventListener("click", () => move(1));
  }

  function move(step) {
    const tasks = filteredTasks();
    if (!tasks.length) return;
    const index = tasks.findIndex((t) => t.workId === state.activeWorkId);
    const next = tasks[(index + step + tasks.length) % tasks.length];
    if (next) selectWork(next.workId);
  }

  document.addEventListener("DOMContentLoaded", init);
})();
