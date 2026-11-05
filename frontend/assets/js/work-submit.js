(function () {
  const B = window.BTR;
  let mySignups = [];
  let contests = {};
  let works = [];
  let currentSignup = null;
  let currentWork = null;
  let uploaded = null;
  let uploadedCover = null;

  async function init() {
    const user = B.requireAuth(["PLAYER"]);
    if (!user) return;

    const [signupsRes, worksRes, contestsRes] = await Promise.allSettled([
      B.api.get("/signups/mine"),
      B.api.get("/works/mine"),
      B.api.get("/contests?page=1&size=50"),
    ]);
    mySignups = signupsRes.status === "fulfilled" ? signupsRes.value || [] : [];
    works = worksRes.status === "fulfilled" ? worksRes.value || [] : [];
    if (contestsRes.status === "fulfilled") {
      (contestsRes.value.records || []).forEach((c) => (contests[c.id] = c));
    }

    const queryContestId = Number(B.qs("contestId"));
    const candidates = mySignups.filter((s) => {
      if (s.status !== "APPROVED") return false;
      const c = contests[s.contestId];
      return !c || ((c.status === "SIGNUP" || c.status === "REVIEW") && c.mode !== "OFFLINE");
    });
    if (!candidates.length) {
      showEmpty();
      return;
    }

    const select = document.getElementById("signupSelect");
    select.innerHTML = candidates
      .map((s) => {
        const c = contests[s.contestId];
        const state = c ? B.statusInfo(c.status).label : "";
        return `<option value="${s.id}">${B.escapeHtml(s.contestTitle)}${state ? " · " + state : ""}</option>`;
      })
      .join("");

    let initial = candidates.find((s) => s.contestId === queryContestId) || candidates[0];
    select.value = String(initial.id);
    select.addEventListener("change", () => {
      const signup = candidates.find((s) => s.id === Number(select.value));
      if (signup) selectSignup(signup);
    });
    selectSignup(initial);
  }

  function selectSignup(signup) {
    currentSignup = signup;
    const c = contests[signup.contestId];
    currentWork = works.find((w) => w.signupId === signup.id && w.status === "SUBMITTED") || null;
    uploaded = currentWork && currentWork.fileUrl ? { url: currentWork.fileUrl, name: currentWork.fileName } : null;
    uploadedCover = currentWork && currentWork.cover ? currentWork.cover : null;
    fillForm();
    renderSummary(c, currentWork);
  }

  function fillForm() {
    document.getElementById("workTitle").value = currentWork ? currentWork.title : "";
    document.getElementById("workDesc").value = currentWork ? currentWork.description || "" : "";
    const fileInfo = document.getElementById("fileInfo");
    const fileName = document.getElementById("fileName");
    if (uploaded) {
      fileInfo.classList.remove("hidden");
      fileName.textContent = uploaded.name || "已上传文件";
    } else {
      fileInfo.classList.add("hidden");
    }
    const coverInfo = document.getElementById("coverInfo");
    if (uploadedCover) {
      coverInfo.classList.remove("hidden");
    } else {
      coverInfo.classList.add("hidden");
    }
    const saveBtn = document.getElementById("saveBtn");
    const c = currentSignup ? contests[currentSignup.contestId] : null;
    const locked = currentWork && currentWork.locked;
    saveBtn.disabled = Boolean(locked) || (c && !(c.status === "SIGNUP" || c.status === "REVIEW"));
    saveBtn.textContent = locked ? "作品已锁定" : currentWork ? "保存修改" : "提交作品";
  }

  function renderSummary(c, work) {
    const host = document.getElementById("workSummary");
    if (!c) {
      host.innerHTML = "<h3>赛事信息</h3><p class='small muted'>未找到赛事信息</p>";
      return;
    }
    const locked = work && work.locked;
    host.innerHTML = `
      <div class="summary-cover"><img src="${B.escapeHtml(c.cover || "/assets/img/cover-1.svg")}" alt="${B.escapeHtml(c.title)}" /></div>
      <span class="badge ${B.statusInfo(c.status).cls}">${B.statusInfo(c.status).label}</span>
      <h3 style="margin-top:10px">${B.escapeHtml(c.title)}</h3>
      <div class="summary-list">
        <div><span>报名截止</span><b>${B.formatDate(c.signupEnd)}</b></div>
        <div><span>评审开始</span><b>${B.formatDate(c.reviewStart)}</b></div>
        <div><span>当前作品</span><b>${work ? B.escapeHtml(work.title) : "尚未提交"}</b></div>
        <div><span>作品状态</span><b>${locked ? "已锁定" : work ? "可修改" : "待提交"}</b></div>
      </div>
      ${
        locked
          ? '<div class="alert alert-warn" style="margin-top:16px">评审已开始，作品已锁定，无法继续修改。</div>'
          : '<div class="alert alert-info" style="margin-top:16px">截止前可随时修改作品，评审开始后自动锁定。</div>'
      }`;
  }

  function showEmpty() {
    document.getElementById("workCard").innerHTML = `<div class="success-panel" style="padding:40px 20px">
      <div class="empty-icon" style="margin-bottom:14px">
        <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M4 7h16M4 12h10M4 17h7"/></svg>
      </div>
      <h2 style="font-size:20px">还没有可提交的报名</h2>
      <p>请先报名一场处于报名阶段的赛事，再回来提交作品。</p>
      <a class="btn btn-primary" href="/contest-list.html">去报名</a>
    </div>`;
    document.getElementById("workSummary").innerHTML = "";
  }

  async function uploadFile(file) {
    if (!file) return;
    const form = new FormData();
    form.append("file", file);
    B.toast("正在上传…", "info", 1500);
    try {
      const data = await B.api.upload("/files?purpose=work", form);
      uploaded = { url: data.url, name: data.name };
      document.getElementById("fileName").textContent = data.name;
      document.getElementById("fileInfo").classList.remove("hidden");
      B.toast("文件上传成功", "success");
    } catch (e) {
      B.toast(e.message || "上传失败", "error");
    }
  }

  async function submit(e) {
    e.preventDefault();
    if (!currentSignup) {
      B.toast("请选择参赛报名", "warn");
      return;
    }
    const title = document.getElementById("workTitle").value.trim();
    const description = document.getElementById("workDesc").value.trim();
    if (!title) {
      B.toast("请输入作品名称", "warn");
      return;
    }
    const btn = document.getElementById("saveBtn");
    btn.disabled = true;
    btn.textContent = "保存中…";
    try {
      await B.api.post("/works", {
        signupId: currentSignup.id,
        title,
        description,
        fileUrl: uploaded ? uploaded.url : null,
        fileName: uploaded ? uploaded.name : null,
        cover: uploadedCover || null,
      });
      B.toast("作品已保存", "success");
      setTimeout(() => (location.href = "/user-center.html"), 600);
    } catch (err) {
      B.toast(err.message || "保存失败", "error");
      btn.disabled = false;
      btn.textContent = currentWork ? "保存修改" : "提交作品";
    }
  }

  function initUpload() {
    const box = document.getElementById("uploadBox");
    const input = document.getElementById("fileInput");
    box.addEventListener("click", () => input.click());
    input.addEventListener("change", () => uploadFile(input.files[0]));
    box.addEventListener("dragover", (e) => {
      e.preventDefault();
      box.classList.add("is-drag");
    });
    box.addEventListener("dragleave", () => box.classList.remove("is-drag"));
    box.addEventListener("drop", (e) => {
      e.preventDefault();
      box.classList.remove("is-drag");
      uploadFile(e.dataTransfer.files[0]);
    });
    document.getElementById("removeFile").addEventListener("click", () => {
      uploaded = null;
      input.value = "";
      document.getElementById("fileInfo").classList.add("hidden");
    });

    /* 封面 */
    const coverBox = document.getElementById("coverUploadBox");
    const coverInput = document.getElementById("coverInput");
    coverBox.addEventListener("click", () => coverInput.click());
    coverBox.addEventListener("dragover", (e) => {
      e.preventDefault();
      coverBox.classList.add("is-drag");
    });
    coverBox.addEventListener("dragleave", () => coverBox.classList.remove("is-drag"));
    coverBox.addEventListener("drop", (e) => {
      e.preventDefault();
      coverBox.classList.remove("is-drag");
      uploadCover(e.dataTransfer.files[0]);
    });
    coverInput.addEventListener("change", () => uploadCover(coverInput.files[0]));
    document.getElementById("removeCover").addEventListener("click", () => {
      uploadedCover = null;
      coverInput.value = "";
      document.getElementById("coverInfo").classList.add("hidden");
    });
  }

  async function uploadCover(file) {
    if (!file) return;
    if (!/^image\//.test(file.type)) {
      B.toast("封面请上传图片文件", "warn");
      return;
    }
    const form = new FormData();
    form.append("file", file);
    B.toast("正在上传封面…", "info", 1500);
    try {
      const data = await B.api.upload("/files?purpose=cover", form);
      uploadedCover = data.url;
      document.getElementById("coverInfo").classList.remove("hidden");
      B.toast("封面上传成功", "success");
    } catch (e) {
      B.toast(e.message || "封面上传失败", "error");
    }
  }

  document.addEventListener("DOMContentLoaded", () => {
    initUpload();
    document.getElementById("workForm").addEventListener("submit", submit);
    init();
  });
})();
