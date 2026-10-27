// ChildShield Application Shell & Navigation
const ROLE_HOME = {
  ADMIN: "admin-dashboard.html",
  HEALTH_WORKER: "worker-dashboard.html",
  PARENT: "mother-dashboard.html"
};

const PAGE_ACCESS = {
  "admin-dashboard.html": ["ADMIN"],
  "worker-dashboard.html": ["HEALTH_WORKER"],
  "mother-dashboard.html": ["PARENT"],
  "beneficiaries.html": ["HEALTH_WORKER"],
  "villages.html": ["ADMIN"],
  "workers.html": ["ADMIN"],
  "vaccinations.html": ["ADMIN", "HEALTH_WORKER", "PARENT"],
  "maternal-visits.html": ["HEALTH_WORKER", "ADMIN"],
  "reports.html": ["HEALTH_WORKER", "ADMIN"],
  "notifications.html": ["ADMIN", "HEALTH_WORKER", "PARENT"],
  "profile.html": ["ADMIN", "HEALTH_WORKER", "PARENT"]
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];

function escapeHtml(val = "") {
  return String(val ?? "").replace(/[&<>"']/g, (m) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#039;"
  }[m]));
}

function normalizePageName(path = "") {
  let p = path.split("/").pop().split("?")[0].split("#")[0] || "index.html";
  if (!p || p === "index" || p === "login") p = "index.html";
  if (p === "register") p = "register.html";
  if (p === "health-worker-dashboard" || p === "health-worker-dashboard.html") p = "worker-dashboard.html";
  if (p === "user-dashboard" || p === "user-dashboard.html") p = "mother-dashboard.html";
  if (p === "healthcare-workers" || p === "healthcare-workers.html") p = "workers.html";
  if (p === "vaccination-schedule" || p === "vaccination-schedule.html") p = "vaccinations.html";
  if (p === "visit-records" || p === "visit-records.html") p = "maternal-visits.html";
  if (!p.endsWith(".html")) p += ".html";
  return p;
}

function currentPage() {
  return normalizePageName(location.pathname);
}

function roleLabel(role) {
  const labels = {
    ADMIN: "Administrator",
    HEALTH_WORKER: "Health Worker",
    PARENT: "Mother / Guardian"
  };
  return labels[role] || role || "User";
}

function initials(name = "User") {
  return (name || "U")
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((x) => x[0])
    .join("")
    .toUpperCase();
}

function saveUser(user) {
  const data = JSON.stringify(user);
  sessionStorage.setItem("childshield_user", data);
  localStorage.setItem("childshield_user", data);
}

function getUser() {
  try {
    const raw = sessionStorage.getItem("childshield_user") || localStorage.getItem("childshield_user");
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function clearUser() {
  sessionStorage.removeItem("childshield_user");
  localStorage.removeItem("childshield_user");
}

function toast(message, type = "info") {
  let toastEl = $("#toast");
  if (!toastEl) {
    toastEl = document.createElement("div");
    toastEl.id = "toast";
    toastEl.className = "toast";
    document.body.appendChild(toastEl);
  }
  toastEl.className = `toast toast-${type}`;
  toastEl.innerHTML = `<span class="toast-icon">${type === "error" ? "✕" : type === "success" ? "✓" : "ℹ"}</span><span>${escapeHtml(message)}</span>`;
  clearTimeout(window.__toastTimer);
  window.__toastTimer = setTimeout(() => {
    toastEl.remove();
  }, 4000);
}

function setLoading(btn, loading, text) {
  if (!btn) return;
  btn.disabled = loading;
  if (loading) {
    btn.dataset.oldText = btn.innerHTML;
    btn.innerHTML = `<span class="spinner"></span> ${text || "Please wait..."}`;
  } else {
    btn.innerHTML = btn.dataset.oldText || btn.textContent;
  }
}

function showModal({ title, content, onConfirm, confirmText = "Save", cancelText = "Cancel" }) {
  closeModal();
  const modal = document.createElement("div");
  modal.id = "activeModal";
  modal.className = "modal";
  modal.innerHTML = `
    <div class="modal-box">
      <div class="modal-head">
        <h2>${escapeHtml(title)}</h2>
        <button class="modal-close" onclick="closeModal()">✕</button>
      </div>
      <div class="modal-body">${content}</div>
      <div class="modal-foot">
        <button class="btn btn-secondary" onclick="closeModal()">${escapeHtml(cancelText)}</button>
        ${onConfirm ? `<button class="btn btn-primary" id="modalConfirmBtn">${escapeHtml(confirmText)}</button>` : ""}
      </div>
    </div>
  `;
  document.body.appendChild(modal);

  if (onConfirm) {
    $("#modalConfirmBtn")?.addEventListener("click", async () => {
      const btn = $("#modalConfirmBtn");
      setLoading(btn, true);
      try {
        await onConfirm();
        closeModal();
      } catch (err) {
        toast(err.message, "error");
      } finally {
        setLoading(btn, false);
      }
    });
  }
}

function closeModal() {
  $("#activeModal")?.remove();
}

async function requireAuth() {
  const page = currentPage();
  const isAuthPage = page === "index.html" || page === "register.html";
  const user = getUser();

  if (isAuthPage) {
    if (user && user.role) {
      location.href = ROLE_HOME[user.role] || "admin-dashboard.html";
      return null;
    }
    return null;
  }

  if (!user) {
    location.href = "index.html";
    return null;
  }

  const allowedRoles = PAGE_ACCESS[page];
  if (allowedRoles && !allowedRoles.includes(user.role)) {
    location.href = ROLE_HOME[user.role] || "index.html";
    return null;
  }

  return user;
}

function navMarkup() {
  return `
  <aside class="sidebar" id="sidebar">
    <div class="side-brand">
      <div class="logo">CS</div>
      <div>
        <strong>ChildShield</strong>
        <div class="side-subtitle">Vaccination Tracker</div>
      </div>
    </div>
    <div class="nav-scroll">
      <div class="nav-section">Dashboard</div>
      <a class="nav-link" data-page="admin-dashboard.html" data-roles="ADMIN" href="admin-dashboard.html">
        <span class="nav-icon">⌂</span>Dashboard
      </a>
      <a class="nav-link" data-page="worker-dashboard.html" data-roles="HEALTH_WORKER" href="worker-dashboard.html">
        <span class="nav-icon">⌂</span>Dashboard
      </a>
      <a class="nav-link" data-page="mother-dashboard.html" data-roles="PARENT" href="mother-dashboard.html">
        <span class="nav-icon">⌂</span>Dashboard
      </a>

      <div class="nav-section">Clinical Care</div>
      <a class="nav-link" data-page="vaccinations.html" data-roles="ADMIN,HEALTH_WORKER,PARENT" href="vaccinations.html">
        <span class="nav-icon">💉</span>Vaccinations
      </a>
      <a class="nav-link" data-page="maternal-visits.html" data-roles="HEALTH_WORKER,ADMIN" href="maternal-visits.html">
        <span class="nav-icon">🩺</span>Maternal Visits
      </a>
      <a class="nav-link" data-page="notifications.html" data-roles="ADMIN,HEALTH_WORKER,PARENT" href="notifications.html">
        <span class="nav-icon">🔔</span>Notifications
      </a>

      <div class="nav-section">Operations</div>
      <a class="nav-link" data-page="villages.html" data-roles="ADMIN" href="villages.html">
        <span class="nav-icon">🏘️</span>Villages
      </a>
      <a class="nav-link" data-page="workers.html" data-roles="ADMIN" href="workers.html">
        <span class="nav-icon">👨‍⚕️</span>Health Workers
      </a>
      <a class="nav-link" data-page="beneficiaries.html" data-roles="HEALTH_WORKER" href="beneficiaries.html">
        <span class="nav-icon">👥</span>Beneficiaries
      </a>
      <a class="nav-link" data-page="reports.html" data-roles="HEALTH_WORKER,ADMIN" href="reports.html">
        <span class="nav-icon">📊</span>Reports & Stats
      </a>

      <div class="nav-section">Account</div>
      <a class="nav-link" data-page="profile.html" data-roles="ADMIN,HEALTH_WORKER,PARENT" href="profile.html">
        <span class="nav-icon">👤</span>My Profile
      </a>
    </div>
    <div class="sidebar-foot">
      <button class="btn btn-secondary full logout-btn" id="logoutBtn">
        <span>🚪</span> Logout
      </button>
    </div>
  </aside>
  <div class="sidebar-overlay" id="sidebarOverlay"></div>`;
}

function shell(title, body) {
  return `
  <div class="app">
    ${navMarkup()}
    <main class="main">
      <header class="topbar">
        <div class="topbar-left">
          <button class="menu-btn" id="menuBtn" aria-label="Toggle Navigation">☰</button>
          <div class="page-title-wrap">
            <h2 class="topbar-title">${escapeHtml(title)}</h2>
          </div>
        </div>
        <div class="topbar-right">
          <div class="user-chip">
            <div class="user-meta">
              <strong class="js-user-name">Loading...</strong>
              <small class="muted js-user-role"></small>
            </div>
            <div class="avatar js-avatar">CS</div>
          </div>
        </div>
      </header>
      <section class="page">${body}</section>
    </main>
  </div>`;
}

function renderShell(user) {
  if (!user) return;
  const name = user.fullName || user.username || "User";
  $$(".js-user-name").forEach((e) => (e.textContent = name));
  $$(".js-user-role").forEach((e) => (e.textContent = roleLabel(user.role)));
  $$(".js-avatar").forEach((e) => (e.textContent = initials(name)));

  const page = currentPage();
  $$(".nav-link[data-page]").forEach((link) => {
    const roles = (link.dataset.roles || "").split(",").map((s) => s.trim()).filter(Boolean);
    const visible = !roles.length || roles.includes(user.role);
    link.classList.toggle("hidden", !visible);
    link.classList.toggle("active", link.dataset.page === page);
  });

  const sidebar = $("#sidebar");
  const overlay = $("#sidebarOverlay");
  const menuBtn = $("#menuBtn");

  const toggleSidebar = () => {
    sidebar?.classList.toggle("open");
    overlay?.classList.toggle("open");
  };

  menuBtn?.addEventListener("click", toggleSidebar);
  overlay?.addEventListener("click", toggleSidebar);

  $("#logoutBtn")?.addEventListener("click", async () => {
    try {
      await API.logout();
    } catch {}
    clearUser();
    location.href = "index.html";
  });
}
