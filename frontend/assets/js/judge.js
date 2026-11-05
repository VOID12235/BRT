(function () {
  const B = window.BTR;
  const state = {
    tasks: [],
    progress: [],
    projects: [],
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
    // 打分页支持 ?contestId= 直达项目
    const queryContestId = B.qs("contestId");
    if (isScorePage() && queryContestId) {
      state.contestId = queryContestId;
    }
    bindFilter();
    if (isScorePage()) {
      initScorePage();
    } else {
      renderIndex();
    }
  }

  async function loadAll() {
    const [tasks, progress, projects, contests] = await Promise.allSettled([
      B.api.get("/judge/tasks"),
      B.api.get("/judge/progress"),
      B.api.get("/judge/projects"),
      B.api.get("/judge/contests"),
    ]);
    state.tasks = tasks.status === "fulfilled" ? tasks.value || [] : [];
    state.progress = progress.status === "fulfilled" ? progress.value || [] : [];
    state.projects = projects.status === "fulfilled" ? projects.value || [] : [];
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
        if (first) selectTask(taskKey(first));
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

  function statusLabel(status) {
    if (status === "REVIEW") return "进行中";
    if (status === "FINISHED") return "已结束";
    return "报名中";
  }

  /* ------------------------------ 评审任务页（项目列表） ------------------------------ */
  function renderIndex() {
    const projects = state.projects;
    const totalAssigned = projects.reduce((s, p) => s + p.assigned, 0);
    const totalScored = projects.reduce((s, p) => s + p.scored, 0);
    const stats = document.querySelectorAll("#judgeStats .stat-card .value");
    if (stats.length === 3) {
      stats[0].textContent = projects.length;
      stats[1].textContent = totalScored;
      stats[2].textContent = totalAssigned - totalScored;
    }

    const host = document.getElementById("projectGrid");
    if (!projects.length) {
      host.innerHTML = B.emptyState(
        "暂无评审项目",
        "管理员按项目整体分配后，项目会出现在这里。"
      );
      return;
    }
    host.innerHTML = projects
      .map((p) => {
        const percent = p.assigned ? Math.round((p.scored / p.assigned) * 100) : 0;
        const done = p.assigned > 0 && p.scored >= p.assigned;
        return `<div class="judge-project-card" data-contest="${p.contestId}">
          ${p.cover ? `<div class="jp-cover"><img src="${B.escapeHtml(p.cover)}" alt="" loading="lazy" /></div>` : ""}
          <div class="jp-body">
            <div class="row-between wrap">
              <b class="jp-title">${B.escapeHtml(p.contestTitle || "赛事 " + p.contestId)}</b>
              <span class="badge ${p.contestStatus === "REVIEW" ? "badge-review" : ""}">${statusLabel(p.contestStatus)}</span>
            </div>
            <div class="jp-meta">
              <span>${state.contests[p.contestId] && state.contests[p.contestId].mode === "OFFLINE" ? "选手" : "作品"} ${p.assigned} 件</span>
              <span>已评 ${p.scored}</span>
              <span>待评 ${p.pending}</span>
            </div>
            <div class="progress"><i style="width:${percent}%" class="${done ? "green" : ""}"></i></div>
            <div class="row-between" style="margin-top: 14px">
              <span class="tiny muted">${percent}% 已完成</span>
              <a class="btn btn-primary btn-xs" href="/judge/judge-score.html?contestId=${p.contestId}">进入评审 →</a>
            </div>
          </div>
        </div>`;
      })
      .join("");
    host.querySelectorAll("[data-contest]").forEach((card) => {
      card.style.cursor = "pointer";
      card.addEventListener("click", (e) => {
        if (e.target.closest("a")) return;
        location.href = "/judge/judge-score.html?contestId=" + card.dataset.contest;
      });
    });

    const progressHost = document.getElementById("progressList");
    if (!state.progress.length) {
      progressHost.innerHTML = B.emptyState("暂无进度", "分配项目后开始统计");
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
  function taskKey(task) {
    return task.workId != null ? task.workId : task.signupId;
  }
  function taskHasWork(task) {
    return task.workId != null;
  }
  function initScorePage() {
    const queryKey = Number(B.qs("workId") || B.qs("signupId"));
    // 若带任务 id 但不在当前赛事过滤内，清空过滤以包含该任务
    if (queryKey && state.contestId) {
      const exists = state.tasks.some(
        (t) => taskKey(t) === queryKey && String(t.contestId) === String(state.contestId)
      );
      if (!exists) state.contestId = "";
      const filter = document.getElementById("contestFilter");
      if (filter) filter.value = state.contestId;
    }
    renderTaskList();
    const target =
      state.tasks.find((t) => taskKey(t) === queryKey) || filteredTasks()[0] || state.tasks[0];
    if (target) {
      selectTask(taskKey(target));
    } else {
      document.getElementById("workPanel").innerHTML = `<div class="card">${B.emptyState(
        "暂无可评任务",
        "该赛事暂无已分配任务，或管理员尚未分配。"
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
        const key = taskKey(task);
        return `<button type="button" class="task-item ${
          key === state.activeWorkId ? "is-active" : ""
        }" data-work="${key}">
          <b>${B.escapeHtml(task.workTitle)}</b>
          <span>${B.escapeHtml(task.contestTitle)}</span>
          <span class="${done ? "done" : ""}" style="float:right">${
          done ? Number(task.score).toFixed(1) + " 分" : "待评分"
        }</span>
        </button>`;
      })
      .join("");
    host.querySelectorAll("[data-work]").forEach((btn) => {
      btn.addEventListener("click", () => selectTask(Number(btn.dataset.work)));
    });
  }

  function selectTask(key) {
    state.activeWorkId = key;
    document.querySelectorAll("#taskList [data-work]").forEach((btn) => {
      btn.classList.toggle("is-active", Number(btn.dataset.work) === key);
    });
    const task = state.tasks.find((t) => taskKey(t) === key);
    if (!task) return;
    const param = taskHasWork(task) ? "workId=" + task.workId : "signupId=" + task.signupId;
    history.replaceState(null, "", "/judge/judge-score.html?contestId=" + task.contestId + "&" + param);
    renderWork(task);
  }

  function renderWork(task) {
    const contest = state.contests[task.contestId];
    const contestStatus = contest ? contest.status : "";
    // 进行中 / 已结束（评审收尾）阶段均可打分
    const locked = contestStatus && !(contestStatus === "REVIEW" || contestStatus === "FINISHED");
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
      task.school ? "  · " + B.escapeHtml(task.school) : ""
    }</span>
              ${taskHasWork(task) ? `<span>提交于 ${B.formatDate(task.submittedAt, true)}</span>` : '<span class="muted">线下比赛 · 无作品</span>'}
            </div>
          </div>
          ${
            taskHasWork(task)
              ? task.fileUrl
                ? `<a class="btn btn-outline btn-sm" href="${B.escapeHtml(
                    task.fileUrl
                  )}" target="_blank" rel="noopener">查看作品文件</a>`
                : '<span class="muted tiny">未上传文件</span>'
              : '<span class="muted tiny">线下比赛按选手表现打分</span>'
          }
        </div>

        <div class="desc">${B.escapeHtml(
          taskHasWork(task) ? task.workDescription || "作者未填写作品简介。" : task.workDescription || "选手未填写自我介绍。"
        )}</div>

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
        const body = taskHasWork(task)
          ? { workId: task.workId, score, comment: text }
          : { signupId: task.signupId, score, comment: text };
        await B.api.post("/judge/scores", body);
        B.toast("评分已提交", "success");
        await loadAll();
        const updated = state.tasks.find((t) => taskKey(t) === taskKey(task));
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
    const index = tasks.findIndex((t) => taskKey(t) === state.activeWorkId);
    const next = tasks[(index + step + tasks.length) % tasks.length];
    if (next) selectTask(taskKey(next));
  }

  document.addEventListener("DOMContentLoaded", init);
})();
