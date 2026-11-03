/* ==========================================================================
   BTR 赛事平台 · API 与通用工具
   ========================================================================== */
(function () {
  const API_BASE = "/api/v1";
  const TOKEN_KEY = "btr_access_token";
  const REFRESH_KEY = "btr_refresh_token";
  const USER_KEY = "btr_user";

  const STATUS = {
    DRAFT: { label: "筹备中", cls: "badge-draft" },
    SIGNUP: { label: "报名中", cls: "badge-signup" },
    REVIEW: { label: "评审中", cls: "badge-review" },
    PUBLISHED: { label: "已公示", cls: "badge-published" },
    FINISHED: { label: "已结束", cls: "badge-finished" },
  };

  const SIGNUP_STATUS = {
    PENDING: { label: "待审核", cls: "badge-review" },
    APPROVED: { label: "已报名", cls: "badge-signup" },
    CANCELED: { label: "已取消", cls: "badge-finished" },
  };

  const ROLE = {
    PLAYER: "选手",
    JUDGE: "评委",
    ADMIN: "管理员",
  };

  class ApiError extends Error {
    constructor(message, status, code) {
      super(message);
      this.name = "ApiError";
      this.status = status;
      this.code = code;
    }
  }

  const storage = {
    get token() {
      return localStorage.getItem(TOKEN_KEY) || "";
    },
    get refresh() {
      return localStorage.getItem(REFRESH_KEY) || "";
    },
    get user() {
      try {
        return JSON.parse(localStorage.getItem(USER_KEY) || "null");
      } catch (e) {
        return null;
      }
    },
    save(data) {
      if (!data) return;
      if (data.accessToken) localStorage.setItem(TOKEN_KEY, data.accessToken);
      if (data.refreshToken) localStorage.setItem(REFRESH_KEY, data.refreshToken);
      if (data.user) localStorage.setItem(USER_KEY, JSON.stringify(data.user));
    },
    saveUser(user) {
      if (user) localStorage.setItem(USER_KEY, JSON.stringify(user));
    },
    clear() {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(REFRESH_KEY);
      localStorage.removeItem(USER_KEY);
    },
  };

  let refreshing = null;

  async function refreshAccessToken() {
    const refresh = storage.refresh;
    if (!refresh) return false;
    if (!refreshing) {
      refreshing = fetch(API_BASE + "/auth/refresh", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken: refresh }),
      })
        .then(async (res) => {
          const body = await res.json().catch(() => null);
          if (!res.ok || !body || body.code !== 0) return false;
          storage.save(body.data);
          return true;
        })
        .catch(() => false)
        .finally(() => {
          refreshing = null;
        });
    }
    return refreshing;
  }

  async function request(path, options = {}) {
    const { method = "GET", body, headers = {}, retry = true, raw = false } = options;
    const isForm = body instanceof FormData;
    const finalHeaders = { ...headers };
    if (body && !isForm) finalHeaders["Content-Type"] = "application/json";
    if (storage.token) finalHeaders.Authorization = "Bearer " + storage.token;

    let res;
    try {
      res = await fetch(API_BASE + path, {
        method,
        headers: finalHeaders,
        body: body ? (isForm ? body : JSON.stringify(body)) : undefined,
      });
    } catch (e) {
      throw new ApiError("网络连接失败，请检查服务是否启动", 0, -1);
    }

    if (res.status === 401 && retry && storage.refresh) {
      const ok = await refreshAccessToken();
      if (ok) return request(path, { ...options, retry: false });
      storage.clear();
      if (!isPublicPage()) {
        const redirect = encodeURIComponent(location.pathname + location.search);
        location.href = "/login.html?redirect=" + redirect;
      }
    }

    if (raw) {
      if (!res.ok) {
        const body = await res.json().catch(() => null);
        throw new ApiError(body?.message || "请求失败", res.status, body?.code);
      }
      return res;
    }

    const contentType = res.headers.get("content-type") || "";
    if (!contentType.includes("application/json")) {
      if (!res.ok) throw new ApiError("请求失败（" + res.status + "）", res.status, res.status);
      return res;
    }

    const payload = await res.json().catch(() => null);
    if (!res.ok || !payload || payload.code !== 0) {
      throw new ApiError(payload?.message || "请求失败", res.status, payload?.code);
    }
    return payload.data;
  }

  function isPublicPage() {
    const path = location.pathname;
    return (
      path === "/" ||
      /\/(index|contest-list|contest-detail|rank|notices|login)\.html$/.test(path)
    );
  }

  const api = {
    get: (path, options) => request(path, { ...options, method: "GET" }),
    post: (path, body, options) => request(path, { ...options, method: "POST", body }),
    put: (path, body, options) => request(path, { ...options, method: "PUT", body }),
    del: (path, options) => request(path, { ...options, method: "DELETE" }),
    upload: (path, formData) => request(path, { method: "POST", body: formData }),
    raw: (path, options) => request(path, { ...options, raw: true }),
  };

  async function download(path, filename) {
    const res = await api.raw(path);
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename || "download";
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 2000);
  }

  function toast(message, type = "info", duration = 2800) {
    let stack = document.querySelector(".toast-stack");
    if (!stack) {
      stack = document.createElement("div");
      stack.className = "toast-stack";
      document.body.appendChild(stack);
    }
    const el = document.createElement("div");
    el.className = "toast toast-" + type;
    el.textContent = message;
    stack.appendChild(el);
    setTimeout(() => {
      el.classList.add("is-out");
      setTimeout(() => el.remove(), 220);
    }, duration);
  }

  function escapeHtml(value) {
    return String(value ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function parseDate(value) {
    if (!value) return null;
    const date = new Date(String(value).replace(" ", "T"));
    return Number.isNaN(date.getTime()) ? null : date;
  }

  function formatDate(value, withTime = false) {
    const d = parseDate(value);
    if (!d) return "—";
    const pad = (n) => String(n).padStart(2, "0");
    const base = d.getFullYear() + "." + pad(d.getMonth() + 1) + "." + pad(d.getDate());
    return withTime ? base + " " + pad(d.getHours()) + ":" + pad(d.getMinutes()) : base;
  }

  function formatShortDate(value) {
    const d = parseDate(value);
    if (!d) return "—";
    return d.getMonth() + 1 + "月" + d.getDate() + "日";
  }

  function fromNow(value) {
    const d = parseDate(value);
    if (!d) return "";
    const diff = d.getTime() - Date.now();
    const abs = Math.abs(diff);
    const day = 86400000;
    const hour = 3600000;
    let text;
    if (abs < hour) text = Math.max(1, Math.round(abs / 60000)) + " 分钟";
    else if (abs < day) text = Math.round(abs / hour) + " 小时";
    else text = Math.round(abs / day) + " 天";
    return diff >= 0 ? text + "后" : text + "前";
  }

  function statusInfo(status) {
    return STATUS[status] || { label: status || "未知", cls: "badge-soft" };
  }

  function signupStatusInfo(status) {
    return SIGNUP_STATUS[status] || { label: status || "未知", cls: "badge-soft" };
  }

  function statusBadge(status) {
    const info = statusInfo(status);
    return `<span class="badge ${info.cls}">${escapeHtml(info.label)}</span>`;
  }

  function roleLabel(role) {
    return ROLE[role] || role || "用户";
  }

  function qs(name, url = location.search) {
    return new URLSearchParams(url).get(name);
  }

  function requireAuth(roles) {
    const user = storage.user;
    if (!storage.token || !user) {
      const redirect = encodeURIComponent(location.pathname + location.search);
      location.href = "/login.html?redirect=" + redirect;
      return null;
    }
    if (roles && !roles.includes(user.role)) {
      location.href = homeForRole(user.role);
      return null;
    }
    return user;
  }

  function homeForRole(role) {
    if (role === "ADMIN") return "/admin/admin-index.html";
    if (role === "JUDGE") return "/judge/judge-index.html";
    return "/user-center.html";
  }

  function emptyState(title, desc, icon) {
    return `<div class="empty">
      <div class="empty-icon">${icon || iconSvg()}</div>
      <h4>${escapeHtml(title)}</h4>
      <p>${escapeHtml(desc || "")}</p>
    </div>`;
  }

  function iconSvg() {
    return `<svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M4 7h16M4 12h10M4 17h7"/></svg>`;
  }

  function debounce(fn, wait = 300) {
    let timer;
    return function (...args) {
      clearTimeout(timer);
      timer = setTimeout(() => fn.apply(this, args), wait);
    };
  }

  window.BTR = {
    api,
    storage,
    toast,
    download,
    escapeHtml,
    formatDate,
    formatShortDate,
    fromNow,
    parseDate,
    statusInfo,
    signupStatusInfo,
    statusBadge,
    roleLabel,
    qs,
    requireAuth,
    homeForRole,
    emptyState,
    debounce,
    ApiError,
    API_BASE,
  };
})();
