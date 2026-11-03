/* ==========================================================================
   BTR 赛事平台 · 关于页（平台介绍 / 规则说明 / 联系我们）
   内容来自公开配置接口 /api/v1/config/public-site，由管理员在后台维护。
   ========================================================================== */
(function () {
  const B = window.BTR;

  const TABS = {
    intro: { title: "平台介绍", key: "aboutIntro", fallback: "平台介绍内容暂未维护。" },
    rules: { title: "规则说明", key: "aboutRules", fallback: "规则说明内容暂未维护。" },
    contact: { title: "联系我们", key: "aboutContact", fallback: "联系方式暂未维护。" },
  };

  const DEFAULT_DATA = {
    aboutIntro: "BTR 赛事平台面向赛事组织者、选手与评委，提供赛事发布、在线报名、作品提交、评委打分与榜单公示的一站式服务。",
    aboutRules: "1. 参赛者须为本人真实信息报名，线下赛事实行实名制；\n2. 线上作品须为本人原创，禁止抄袭、代打与违规合作；\n3. 评委按分配任务独立打分，评语全程留痕；\n4. 榜单以公示结果为准，获奖证书与奖金按公示名单发放；\n5. 违规行为一经查实，平台有权取消成绩并保留追责权利。",
    aboutContact: "平台邮箱：support@btr.dev\n工作时间：周一至周五 9:00 - 18:00\n商务合作：business@btr.dev",
  };

  let siteData = null;
  let currentTab = "intro";

  function init() {
    const fromUrl = new URLSearchParams(location.search).get("tab");
    if (fromUrl && TABS[fromUrl]) {
      currentTab = fromUrl;
    }
    document.querySelectorAll("[data-about-tab]").forEach((btn) => {
      btn.addEventListener("click", () => {
        currentTab = btn.dataset.aboutTab;
        render();
        history.replaceState(null, "", "?tab=" + currentTab);
      });
    });
    load();
  }

  async function load() {
    try {
      const res = await fetch("/api/v1/config/public-site", { cache: "no-store" });
      if (res.ok) {
        const body = await res.json();
        siteData = (body && body.data) || null;
      }
    } catch (e) {
      /* 接口不可用，使用默认文案 */
    }
    render();
  }

  function render() {
    const tab = TABS[currentTab] || TABS.intro;
    const content = siteData && siteData[tab.key] ? String(siteData[tab.key]).trim() : "";
    const body = content || tab.fallback;
    const el = document.getElementById("aboutContent");
    el.innerHTML = "<h2>" + B.escapeHtml(tab.title) + "</h2><div>" + body + "</div>";
    document.querySelectorAll("[data-about-tab]").forEach((btn) => {
      btn.classList.toggle("is-active", btn.dataset.aboutTab === currentTab);
    });
    document.title = tab.title + " · BTR 赛事平台";
  }

  document.addEventListener("DOMContentLoaded", init);
})();
