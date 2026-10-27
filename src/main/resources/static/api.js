// ChildShield API Client
const API_BASE = "";

async function api(path, options = {}) {
  const opts = {
    credentials: "include",
    headers: {},
    ...options
  };

  try {
    const rawUser = localStorage.getItem("childshield_user") || sessionStorage.getItem("childshield_user") || localStorage.getItem("user");
    if (rawUser) {
      const u = JSON.parse(rawUser);
      if (u && u.id) opts.headers["X-User-Id"] = String(u.id);
      if (u && u.username) opts.headers["X-User-Username"] = String(u.username);
    }
  } catch {}

  if (opts.body && typeof opts.body !== "string" && !(opts.body instanceof FormData)) {
    opts.headers["Content-Type"] = "application/json";
    opts.body = JSON.stringify(opts.body);
  }

  try {
    const response = await fetch(`${API_BASE}${path}`, opts);
    const contentType = response.headers.get("content-type") || "";
    let data = null;

    if (contentType.includes("application/json")) {
      data = await response.json();
    } else {
      const text = await response.text();
      try {
        data = text ? JSON.parse(text) : null;
      } catch {
        data = text;
      }
    }

    if (!response.ok) {
      const message = data?.message || data?.error || `Request failed with status ${response.status}`;
      const error = new Error(message);
      error.status = response.status;
      error.data = data;
      throw error;
    }
    return data;
  } catch (err) {
    if (err.status) throw err;
    const error = new Error("Unable to connect to the server. Please ensure the backend is running.");
    error.status = 0;
    throw error;
  }
}

const API = {
  // Authentication
  login: (username, password) => api("/api/auth/login", { method: "POST", body: { username, password } }),
  register: (payload) => api("/api/auth/register", { method: "POST", body: payload }),
  logout: () => api("/api/auth/logout", { method: "POST" }),

  // Dashboard & Profile
  dashboard: () => api("/api/dashboard"),
  reports: () => api("/api/reports"),
  profile: () => api("/api/profile"),

  // Villages
  villages: () => api("/api/villages"),
  village: (id) => api(`/api/villages/${id}`),
  createVillage: (payload) => api("/api/villages", { method: "POST", body: payload }),
  updateVillage: (id, payload) => api(`/api/villages/${id}`, { method: "PUT", body: payload }),
  deleteVillage: (id) => api(`/api/villages/${id}`, { method: "DELETE" }),

  // Health Workers (Admin)
  workers: () => api("/api/admin/health-workers"),
  worker: (id) => api(`/api/admin/health-workers/${id}`),
  createWorker: (payload) => api("/api/admin/health-workers", { method: "POST", body: payload }),
  updateWorker: (id, payload) => api(`/api/admin/health-workers/${id}`, { method: "PUT", body: payload }),
  deleteWorker: (id) => api(`/api/admin/health-workers/${id}`, { method: "DELETE" }),

  // Beneficiaries: Guardians & Children
  guardians: () => api("/api/health-worker/guardians"),
  guardian: (id) => api(`/api/health-worker/guardians/${id}`),
  createGuardian: (payload) => api("/api/health-worker/guardians", { method: "POST", body: payload }),
  updateGuardian: (id, payload) => api(`/api/health-worker/guardians/${id}`, { method: "PUT", body: payload }),
  deleteGuardian: (id) => api(`/api/health-worker/guardians/${id}`, { method: "DELETE" }),

  children: () => api("/api/health-worker/children"),
  child: (id) => api(`/api/health-worker/children/${id}`),
  createChild: (payload) => api("/api/health-worker/children", { method: "POST", body: payload }),
  updateChild: (id, payload) => api(`/api/health-worker/children/${id}`, { method: "PUT", body: payload }),
  deleteChild: (id) => api(`/api/health-worker/children/${id}`, { method: "DELETE" }),

  // Vaccinations
  vaccinations: () => api("/api/vaccinations"),
  childVaccinations: (childId) => api(`/api/vaccinations/child/${childId}`),
  administer: (id, payload) => api(`/api/vaccinations/${id}/administer`, { method: "POST", body: payload }),

  // Maternal Visits
  visits: () => api("/api/maternal-visits"),
  createVisit: (payload) => api("/api/maternal-visits", { method: "POST", body: payload }),

  // Notifications
  notifications: () => api("/api/notifications"),
  readNotification: (id) => api(`/api/notifications/${id}/read`, { method: "POST" })
};

// Make available globally
window.API = API;
