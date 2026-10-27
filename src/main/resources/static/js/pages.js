// ChildShield Page Controllers & Views
function message(text, type = "error") {
  const el = $("#message");
  if (!el) return;
  el.className = `alert alert-${type}`;
  el.innerHTML = escapeHtml(text);
}

function goRole(role) {
  location.href = ROLE_HOME[role] || "admin-dashboard.html";
}

// -------------------------------------------------------------
// Authentication Forms
// -------------------------------------------------------------
async function initLogin() {
  const form = $("#loginForm");
  if (!form) return;

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const btn = $("#loginBtn");
    const username = $("#username")?.value.trim();
    const password = $("#password")?.value;

    if (!username || !password) {
      message("Please enter both username and password.");
      return;
    }

    setLoading(btn, true, "Logging in...");
    try {
      const result = await API.login(username, password);
      if (result?.success === false) {
        throw new Error(result.message || "Invalid credentials.");
      }
      const user = result?.user || result;
      if (!user?.role) {
        throw new Error("Login succeeded but user profile has no role.");
      }
      saveUser(user);
      toast("Welcome back, " + (user.fullName || user.username) + "!", "success");
      setTimeout(() => goRole(user.role), 300);
    } catch (err) {
      message(err.message || "Invalid username or password.");
    } finally {
      setLoading(btn, false);
    }
  });
}

async function initRegister() {
  const form = $("#registerForm");
  if (!form) return;

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const btn = $("#registerBtn");
    const fullName = $("#fullName")?.value.trim();
    const username = $("#username")?.value.trim();
    const password = $("#password")?.value;
    const role = $("#role")?.value || "PARENT";

    if (!fullName || !username || !password) {
      message("All fields are required.");
      return;
    }

    setLoading(btn, true, "Creating account...");
    try {
      await API.register({ fullName, username, password, role });
      message("Account created successfully! Redirecting to login...", "success");
      setTimeout(() => (location.href = "index.html"), 1200);
    } catch (err) {
      message(err.message || "Registration failed. Try a different username.");
    } finally {
      setLoading(btn, false);
    }
  });
}

// -------------------------------------------------------------
// Common View Components
// -------------------------------------------------------------
function statCards(items) {
  return `
  <div class="grid grid-4">
    ${items
      .map(
        (x) => `
      <div class="card stat">
        <div class="stat-top">
          <div class="stat-label">${escapeHtml(x.label)}</div>
          ${x.icon ? `<div class="stat-icon">${x.icon}</div>` : ""}
        </div>
        <div class="stat-value">${x.value ?? 0}</div>
        <div class="stat-hint">${escapeHtml(x.hint || "")}</div>
      </div>`
      )
      .join("")}
  </div>`;
}

function statusBadge(status) {
  const s = String(status || "").toUpperCase();
  let cls = "badge-gray";
  if (s.includes("OVERDUE")) cls = "badge-red";
  else if (s.includes("DUE") || s.includes("TODAY")) cls = "badge-amber";
  else if (s.includes("COMPLETED")) cls = "badge-green";
  else if (s.includes("UPCOMING")) cls = "badge-blue";

  return `<span class="badge ${cls}"><span class="badge-dot"></span>${escapeHtml(s.replaceAll("_", " ") || "UNKNOWN")}</span>`;
}

function shellPage(title, subtitle, content) {
  const root = $("#appRoot");
  if (!root) return;
  root.innerHTML = shell(
    title,
    `
    <div class="page-head">
      <div>
        <h1 class="page-title">${escapeHtml(title)}</h1>
        <p class="muted page-subtitle">${escapeHtml(subtitle)}</p>
      </div>
    </div>
    ${content}
  `
  );
  renderShell(getUser());
}

function safeVal(obj, ...keys) {
  for (const k of keys) {
    if (obj && obj[k] !== undefined && obj[k] !== null) return obj[k];
  }
  return "";
}

// -------------------------------------------------------------
// Page Router
// -------------------------------------------------------------
async function renderPage(pageName) {
  const normalized = normalizePageName(pageName || location.pathname);

  if (normalized === "index.html") {
    await initLogin();
    return;
  }
  if (normalized === "register.html") {
    await initRegister();
    return;
  }

  const user = await requireAuth();
  if (!user) return;

  try {
    switch (normalized) {
      case "admin-dashboard.html":
      case "worker-dashboard.html":
      case "mother-dashboard.html":
        await dashboardPage(user);
        break;
      case "vaccinations.html":
        await vaccinationsPage(user);
        break;
      case "beneficiaries.html":
        await beneficiariesPage(user);
        break;
      case "maternal-visits.html":
        await visitsPage(user);
        break;
      case "reports.html":
        await reportsPage(user);
        break;
      case "villages.html":
        await villagesPage(user);
        break;
      case "workers.html":
        await workersPage(user);
        break;
      case "notifications.html":
        await notificationsPage(user);
        break;
      case "profile.html":
        await profilePage(user);
        break;
      default:
        await dashboardPage(user);
        break;
    }
  } catch (err) {
    console.error("Page render error:", err);
    shellPage(
      "Service Notice",
      "Unable to fetch the latest data.",
      `<div class="alert alert-error"><strong>Error:</strong> ${escapeHtml(err.message)}</div>
       <button class="btn btn-primary" onclick="location.reload()">Retry</button>`
    );
  }
}

// -------------------------------------------------------------
// Dashboard Page
// -------------------------------------------------------------
async function dashboardPage(user) {
  let d = { totalVillages: 0, totalWorkers: 0, totalGuardians: 0, totalChildren: 0, totalDoses: 0 };
  try {
    d = await API.dashboard();
  } catch (e) {
    console.warn("Dashboard stats error:", e);
  }

  const isAdmin = user.role === "ADMIN";
  const isWorker = user.role === "HEALTH_WORKER";

  if (isAdmin || isWorker) {
    const stats = [
      { label: "Active Villages", value: d.totalVillages, hint: "Locations covered", icon: "🏘️" },
      { label: "Health Workers", value: d.totalWorkers, hint: "Registered personnel", icon: "👨‍⚕️" },
      { label: "Mothers & Guardians", value: d.totalGuardians, hint: "Enrolled beneficiaries", icon: "👩" },
      { label: "Children Registered", value: d.totalChildren, hint: "Under tracking", icon: "👶" },
      { label: "Vaccination Records", value: d.totalDoses, hint: "Scheduled & administered", icon: "💉" }
    ];

    shellPage(
      isAdmin ? "Administrator Dashboard" : "Health Worker Dashboard",
      `Welcome, ${escapeHtml(user.fullName || user.username)}. Operations overview.`,
      statCards(stats) +
        `
      <div class="grid grid-2" style="margin-top:20px">
        <div class="card">
          <div class="card-head">
            <h2>Operations & Management</h2>
          </div>
          <div class="actions">
            ${
              isAdmin
                ? `
              <a class="btn btn-primary" href="workers.html"><span>👨‍⚕️</span> Manage Health Workers</a>
              <a class="btn btn-primary" href="villages.html"><span>🏘️</span> Manage Villages</a>
              <a class="btn btn-secondary" href="reports.html"><span>📊</span> View Coverage Reports</a>
              <a class="btn btn-secondary" href="vaccinations.html"><span>💉</span> Vaccination Records</a>
            `
                : `
              <a class="btn btn-primary" href="beneficiaries.html"><span>👥</span> Manage Beneficiaries</a>
              <a class="btn btn-secondary" href="maternal-visits.html"><span>🩺</span> Record Maternal Visit</a>
              <a class="btn btn-secondary" href="vaccinations.html"><span>💉</span> Update Vaccinations</a>
            `
            }
          </div>
        </div>

        <div class="card">
          <div class="card-head">
            <h2>System Highlights</h2>
          </div>
          <p class="muted" style="line-height:1.6">
            ChildShield enables rural health workers and administrators to monitor mother and child health milestones,
            prevent missed vaccination doses, and schedule maternal care checkups seamlessly.
          </p>
          <div class="actions" style="margin-top:16px">
            <a class="btn btn-outline" href="notifications.html"><span>🔔</span> Check Reminders</a>
            <a class="btn btn-outline" href="profile.html"><span>👤</span> Account Details</a>
          </div>
        </div>
      </div>
      `
    );
  } else {
    // Mother / Parent dashboard
    let notifs = [];
    try {
      notifs = await API.notifications();
    } catch {}
    const unread = (notifs || []).filter((n) => !n.readAt).length;

    const stats = [
      { label: "Total Vaccinations", value: d.totalDoses, hint: "Tracked in ChildShield", icon: "💉" },
      { label: "Unread Reminders", value: unread, hint: "Important notices", icon: "🔔" },
      { label: "My Account", value: "Verified Mother", hint: user.username, icon: "👩" }
    ];

    shellPage(
      "Mother & Child Dashboard",
      `Welcome, ${escapeHtml(user.fullName || user.username)}. Stay updated on your child's vaccination schedule.`,
      statCards(stats) +
        `
      <div class="grid grid-2" style="margin-top:20px">
        <div class="card">
          <div class="card-head">
            <h2>Vaccination Schedule</h2>
          </div>
          <p class="muted" style="line-height:1.6">
            View the complete immunization timeline for your child, including birth doses, OPV, Pentavalent, Rotavirus, and Measles-Rubella.
          </p>
          <div class="actions" style="margin-top:14px">
            <a class="btn btn-primary" href="vaccinations.html"><span>💉</span> View Child Schedule</a>
          </div>
        </div>

        <div class="card">
          <div class="card-head">
            <h2>Reminders & Alerts</h2>
          </div>
          <p class="muted" style="line-height:1.6">
            Receive automated notifications before scheduled dose dates and follow up on any missed visits.
          </p>
          <div class="actions" style="margin-top:14px">
            <a class="btn btn-secondary" href="notifications.html"><span>🔔</span> View Reminders (${unread} new)</a>
          </div>
        </div>
      </div>
      `
    );
  }
}

// -------------------------------------------------------------
// Vaccinations Page
// -------------------------------------------------------------
async function vaccinationsPage(user) {
  let records = [];
  try {
    records = await API.vaccinations();
    if (!Array.isArray(records)) records = records?.content || records?.vaccinations || [];
  } catch (e) {
    records = [];
  }

  const isParent = user.role === "PARENT";

  shellPage(
    "Vaccination Schedule & Tracking",
    "Monitor planned vaccine doses, check due dates, and record administrations.",
    `
    <div class="card">
      <div class="toolbar">
        <div class="toolbar-search">
          <input class="search-input" id="vaxSearch" placeholder="Search by child, vaccine, or date..." />
        </div>
        <div class="toolbar-filters">
          <select class="filter-select" id="vaxStatusFilter">
            <option value="">All Statuses</option>
            <option value="OVERDUE">Overdue</option>
            <option value="DUE_TODAY">Due Today</option>
            <option value="UPCOMING">Upcoming</option>
            <option value="COMPLETED">Completed</option>
            <option value="PENDING">Pending</option>
          </select>
          <span class="count-badge" id="vaxCount">${records.length} records</span>
        </div>
      </div>

      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Child Name</th>
              <th>Vaccine Dose</th>
              <th>Due Date</th>
              <th>Status</th>
              <th>Administered Date</th>
              <th>Notes</th>
              ${!isParent ? "<th>Action</th>" : ""}
            </tr>
          </thead>
          <tbody id="vaxTableBody"></tbody>
        </table>
      </div>
    </div>
  `
  );

  const renderRows = () => {
    const query = ($("#vaxSearch")?.value || "").toLowerCase().trim();
    const statusFilter = $("#vaxStatusFilter")?.value || "";

    const filtered = records.filter((r) => {
      const childName = (r.childName || safeVal(r, "childName", "childFullName") || r.child?.fullName || `Child #${r.child?.id || r.childId || ""}`).toLowerCase();
      const vaccineName = (r.vaccineName || safeVal(r, "vaccineName", "displayName", "doseName") || r.scheduleDose?.displayName || r.scheduleDose?.doseCode || "").toLowerCase();
      const status = (r.status || "").toUpperCase();

      const matchesQuery = !query || childName.includes(query) || vaccineName.includes(query) || String(r.dueDate || "").includes(query);
      const matchesStatus = !statusFilter || status === statusFilter;
      return matchesQuery && matchesStatus;
    });

    $("#vaxCount").textContent = `${filtered.length} of ${records.length} record(s)`;

    if (!filtered.length) {
      $("#vaxTableBody").innerHTML = `<tr><td colspan="${!isParent ? 8 : 7}" class="empty">No matching vaccination records found.</td></tr>`;
      return;
    }

    $("#vaxTableBody").innerHTML = filtered
      .map((r) => {
        const childName = r.childName || safeVal(r, "childName", "childFullName") || r.child?.fullName || `Child #${r.child?.id || r.childId || ""}`;
        const vaccine = r.vaccineName || safeVal(r, "vaccineName", "displayName", "doseName") || r.scheduleDose?.displayName || r.scheduleDose?.doseCode || "Vaccine Dose";
        const isCompleted = r.status === "COMPLETED";

        return `
        <tr>
          <td><span class="text-mono">#${r.id}</span></td>
          <td><strong>${escapeHtml(childName)}</strong></td>
          <td>${escapeHtml(vaccine)}</td>
          <td>${escapeHtml(r.dueDate || "-")}</td>
          <td>${statusBadge(r.status)}</td>
          <td>${escapeHtml(r.administeredDate || "-")}</td>
          <td class="text-truncate" title="${escapeHtml(r.notes || "")}">${escapeHtml(r.notes || "-")}</td>
          ${
            !isParent
              ? `
            <td>
              ${
                isCompleted
                  ? `<span class="badge badge-green">✓ Recorded</span>`
                  : `<button class="btn btn-success btn-small" data-administer="${r.id}" data-child="${escapeHtml(childName)}" data-vaccine="${escapeHtml(vaccine)}" data-due="${r.dueDate || ""}">Administer</button>`
              }
            </td>
          `
              : ""
          }
        </tr>`;
      })
      .join("");
  };

  renderRows();
  $("#vaxSearch")?.addEventListener("input", renderRows);
  $("#vaxStatusFilter")?.addEventListener("change", renderRows);

  // Administer dose click handler
  $("#vaxTableBody")?.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-administer]");
    if (!btn) return;

    const id = btn.dataset.administer;
    const childName = btn.dataset.child;
    const vaccine = btn.dataset.vaccine;
    const dueDate = btn.dataset.due;
    const today = new Date().toISOString().slice(0, 10);

    showModal({
      title: "Administer Vaccine Dose",
      confirmText: "Record Administration",
      content: `
        <div class="form-group">
          <label>Child</label>
          <input type="text" value="${escapeHtml(childName)}" disabled />
        </div>
        <div class="form-group">
          <label>Vaccine Dose</label>
          <input type="text" value="${escapeHtml(vaccine)}" disabled />
        </div>
        <div class="form-group">
          <label>Scheduled Due Date</label>
          <input type="text" value="${escapeHtml(dueDate || "N/A")}" disabled />
        </div>
        <div class="form-group">
          <label>Administered Date *</label>
          <input type="date" id="administerDate" value="${today}" required />
        </div>
        <div class="form-group">
          <label>Clinical Notes / Batch No. (Optional)</label>
          <textarea id="administerNotes" placeholder="e.g. Batch #VX-2024, given in left arm, no adverse reaction"></textarea>
        </div>
      `,
      onConfirm: async () => {
        const administeredDate = $("#administerDate")?.value;
        const notes = $("#administerNotes")?.value.trim() || "";
        if (!administeredDate) throw new Error("Administered date is required.");

        await API.administer(id, { administeredDate, notes });
        toast("Vaccine dose administered successfully!", "success");
        setTimeout(() => location.reload(), 400);
      }
    });
  });
}

// -------------------------------------------------------------
// Beneficiaries Page (Health Worker Only for Creation)
// -------------------------------------------------------------
async function beneficiariesPage(user) {
  let [guardians, children, villages] = await Promise.all([
    API.guardians().catch(() => []),
    API.children().catch(() => []),
    API.villages().catch(() => [])
  ]);

  if (!Array.isArray(guardians)) guardians = [];
  if (!Array.isArray(children)) children = [];
  if (!Array.isArray(villages)) villages = [];

  const isHealthWorker = user.role === "HEALTH_WORKER";

  shellPage(
    "Beneficiaries Directory",
    isHealthWorker
      ? "Enroll mothers/guardians and register children with automated vaccination schedule generation."
      : "View enrolled mothers/guardians and registered children.",
    `
    ${
      isHealthWorker
        ? `
      <div class="actions" style="margin-bottom:20px">
        <button class="btn btn-primary" id="btnNewGuardian"><span>➕</span> Register Guardian</button>
        <button class="btn btn-secondary" id="btnNewChild"><span>➕</span> Register Child</button>
      </div>`
        : ""
    }

    <div class="grid grid-2">
      <div class="card">
        <div class="card-head">
          <h2>Mothers & Guardians (${guardians.length})</h2>
        </div>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Full Name</th>
                <th>Phone</th>
                <th>Village</th>
                <th>Login Username</th>
              </tr>
            </thead>
            <tbody>
              ${
                guardians.length
                  ? guardians
                      .map(
                        (g) => `
                    <tr>
                      <td><span class="text-mono">#${g.id}</span></td>
                      <td><strong>${escapeHtml(g.fullName)}</strong></td>
                      <td>${escapeHtml(g.phone || "-")}</td>
                      <td>${escapeHtml(g.village?.name || g.villageName || `Village #${g.villageId || "-"}`)}</td>
                      <td><span class="badge badge-blue">${escapeHtml(g.user?.username || g.username || "-")}</span></td>
                    </tr>`
                      )
                      .join("")
                  : `<tr><td colspan="5" class="empty">No guardians enrolled yet.</td></tr>`
              }
            </tbody>
          </table>
        </div>
      </div>

      <div class="card">
        <div class="card-head">
          <h2>Registered Children (${children.length})</h2>
        </div>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Child Name</th>
                <th>Date of Birth</th>
                <th>Gender</th>
                <th>Guardian</th>
              </tr>
            </thead>
            <tbody>
              ${
                children.length
                  ? children
                      .map(
                        (c) => `
                    <tr>
                      <td><span class="text-mono">#${c.id}</span></td>
                      <td><strong>${escapeHtml(c.fullName)}</strong></td>
                      <td>${escapeHtml(c.dateOfBirth || "-")}</td>
                      <td>${escapeHtml(c.gender || "-")}</td>
                      <td>${escapeHtml(c.guardian?.fullName || c.guardianName || `Guardian #${c.guardianId || "-"}`)}</td>
                    </tr>`
                      )
                      .join("")
                  : `<tr><td colspan="5" class="empty">No children registered yet.</td></tr>`
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `
  );

  if (isHealthWorker) {
    // New Guardian Modal
    $("#btnNewGuardian")?.addEventListener("click", () => {
      const villageOptions = villages.map((v) => `<option value="${v.id}">${escapeHtml(v.name)} (${escapeHtml(v.district || "")})</option>`).join("");

      showModal({
        title: "Register Mother / Guardian",
        confirmText: "Register Guardian & Create User Account",
        content: `
          <div class="form-group">
            <label>Full Name *</label>
            <input type="text" id="gName" placeholder="e.g. Priya Devi" required />
          </div>
          <div class="grid grid-2">
            <div class="form-group">
              <label>Mobile Phone *</label>
              <input type="tel" id="gPhone" placeholder="10-digit mobile number" required />
            </div>
            <div class="form-group">
              <label>Village *</label>
              <select id="gVillage" required>
                <option value="">Select Village</option>
                ${villageOptions}
              </select>
            </div>
          </div>
          <div class="grid grid-2">
            <div class="form-group">
              <label>Login Username (Optional)</label>
              <input type="text" id="gUsername" placeholder="Auto-created if empty (e.g. priya123)" />
            </div>
            <div class="form-group">
              <label>Login Password (Optional)</label>
              <input type="password" id="gPassword" placeholder="Defaults to mobile number" />
            </div>
          </div>
          <small class="muted" style="display:block; margin-top:-4px; margin-bottom:12px; line-height:1.4;">
            ℹ️ A Mother/Guardian account will automatically be created in the system with these credentials so she can log in and view vaccine schedules.
          </small>
        `,
        onConfirm: async () => {
          const fullName = $("#gName")?.value.trim();
          const phone = $("#gPhone")?.value.trim();
          const villageId = $("#gVillage")?.value;
          const username = $("#gUsername")?.value.trim() || undefined;
          const password = $("#gPassword")?.value || undefined;

          if (!fullName || !phone || !villageId) {
            throw new Error("Full name, phone, and village are required.");
          }

          const res = await API.createGuardian({
            fullName,
            phone,
            villageId: Number(villageId),
            username,
            password
          });
          const createdUsername = res?.user?.username || username || `phone: ${phone}`;
          toast(`Guardian registered! Mother account created with username: ${createdUsername}`, "success");
          setTimeout(() => location.reload(), 600);
        }
      });
    });

    // New Child Modal
    $("#btnNewChild")?.addEventListener("click", () => {
      if (!guardians.length) {
        toast("Please register at least one Guardian before registering a child.", "error");
        return;
      }

      const guardianOptions = guardians.map((g) => {
        const vName = g.village?.name || g.villageName || "";
        const suffix = vName ? ` — Village: ${vName}` : "";
        return `<option value="${g.id}">${escapeHtml(g.fullName)} (Phone: ${escapeHtml(g.phone)})${escapeHtml(suffix)}</option>`;
      }).join("");

      showModal({
        title: "Register Child & Generate Schedule",
        confirmText: "Save Child & Schedule",
        content: `
          <div class="form-group">
            <label>Guardian / Mother *</label>
            <select id="cGuardian" required>
              <option value="">-- Select Mother / Guardian --</option>
              ${guardianOptions}
            </select>
          </div>
          <div class="form-group">
            <label>Child Full Name *</label>
            <input type="text" id="cName" placeholder="e.g. Aarav Kumar" required />
          </div>
          <div class="grid grid-2">
            <div class="form-group">
              <label>Date of Birth *</label>
              <input type="date" id="cDob" required />
            </div>
            <div class="form-group">
              <label>Gender *</label>
              <select id="cGender">
                <option value="Male">Male</option>
                <option value="Female">Female</option>
                <option value="Other">Other</option>
              </select>
            </div>
          </div>
          <div class="grid grid-2">
            <div class="form-group">
              <label>Birth Weight (kg)</label>
              <input type="number" id="cWeight" step="0.01" min="0.5" max="10.0" placeholder="e.g. 3.20" />
            </div>
            <div class="form-group">
              <label>Assigned Village (Auto-detected)</label>
              <input type="text" id="cVillageDisplay" readonly placeholder="Auto-detected from Guardian" style="background:#f1f5f9; cursor:not-allowed;" />
              <input type="hidden" id="cVillage" />
            </div>
          </div>
        `,
        onConfirm: async () => {
          const guardianId = $("#cGuardian")?.value;
          const fullName = $("#cName")?.value.trim();
          const dateOfBirth = $("#cDob")?.value;
          const gender = $("#cGender")?.value;
          const birthWeightKg = $("#cWeight")?.value;

          if (!guardianId || !fullName || !dateOfBirth) {
            throw new Error("Guardian, child name, and date of birth are required.");
          }

          const selectedG = guardians.find((g) => String(g.id) === String(guardianId));
          const villageId = $("#cVillage")?.value || selectedG?.village?.id || selectedG?.villageId;

          await API.createChild({
            guardianId: Number(guardianId),
            fullName,
            dateOfBirth,
            gender,
            birthWeightKg: birthWeightKg ? Number(birthWeightKg) : null,
            villageId: villageId ? Number(villageId) : null
          });

          toast("Child enrolled and vaccination schedule generated!", "success");
          setTimeout(() => location.reload(), 400);
        }
      });

      // Hook up change listener to auto-populate village
      const gSelect = $("#cGuardian");
      const vDisplay = $("#cVillageDisplay");
      const vHidden = $("#cVillage");

      const updateVillageField = () => {
        const val = gSelect?.value;
        const selectedG = guardians.find((g) => String(g.id) === String(val));
        if (selectedG) {
          const vId = selectedG.village?.id || selectedG.villageId;
          const vName = selectedG.village?.name || selectedG.villageName || (villages.find((v) => String(v.id) === String(vId))?.name) || `Village #${vId || "-"}`;
          if (vHidden) vHidden.value = vId || "";
          if (vDisplay) vDisplay.value = vName || "Assigned Village";
        } else {
          if (vHidden) vHidden.value = "";
          if (vDisplay) vDisplay.value = "";
        }
      };

      gSelect?.addEventListener("change", updateVillageField);
    });
  }
}

// -------------------------------------------------------------
// Maternal Visits Page
// -------------------------------------------------------------
async function visitsPage(user) {
  let [visits, guardians] = await Promise.all([
    API.visits().catch(() => []),
    API.guardians().catch(() => [])
  ]);

  if (!Array.isArray(visits)) visits = visits?.visits || [];
  if (!Array.isArray(guardians)) guardians = [];

  const isHealthWorker = user.role === "HEALTH_WORKER";

  shellPage(
    "Maternal Health Visits",
    "Record and review Antenatal (ANC) and Postnatal (PNC) checkups for registered mothers.",
    `
    ${
      isHealthWorker
        ? `
      <div class="actions" style="margin-bottom:20px">
        <button class="btn btn-primary" id="btnNewVisit"><span>➕</span> Record New Visit</button>
      </div>`
        : ""
    }

    <div class="card">
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Mother / Guardian</th>
              <th>Visit Date</th>
              <th>Visit Type</th>
              <th>Next Scheduled Visit</th>
              <th>Clinical Notes</th>
            </tr>
          </thead>
          <tbody>
            ${
              visits.length
                ? visits
                    .map(
                      (v) => `
                  <tr>
                    <td><span class="text-mono">#${v.id}</span></td>
                    <td><strong>${escapeHtml(v.guardian?.fullName || v.guardianName || `Guardian #${v.guardianId || "-"}`)}</strong></td>
                    <td>${escapeHtml(v.visitDate || "-")}</td>
                    <td><span class="badge badge-blue">${escapeHtml(v.visitType || "Routine")}</span></td>
                    <td>${escapeHtml(v.nextVisitDate || "None")}</td>
                    <td>${escapeHtml(v.notes || "-")}</td>
                  </tr>`
                    )
                    .join("")
                : `<tr><td colspan="6" class="empty">No maternal checkup visits recorded yet.</td></tr>`
            }
          </tbody>
        </table>
      </div>
    </div>
  `
  );

  if (isHealthWorker) {
    $("#btnNewVisit")?.addEventListener("click", () => {
      if (!guardians.length) {
        toast("Please register a guardian first.", "error");
        return;
      }

      const guardianOptions = guardians.map((g) => `<option value="${g.id}">${escapeHtml(g.fullName)} (Phone: ${escapeHtml(g.phone)})</option>`).join("");
      const today = new Date().toISOString().slice(0, 10);

      showModal({
        title: "Record Maternal Health Visit",
        confirmText: "Save Visit",
        content: `
          <div class="form-group">
            <label>Mother / Guardian *</label>
            <select id="vGuardian" required>
              <option value="">Select Mother / Guardian</option>
              ${guardianOptions}
            </select>
          </div>
          <div class="grid grid-2">
            <div class="form-group">
              <label>Visit Date *</label>
              <input type="date" id="vDate" value="${today}" required />
            </div>
            <div class="form-group">
              <label>Visit Type *</label>
              <select id="vType">
                <option value="ANC-1">ANC-1 (First Trimester)</option>
                <option value="ANC-2">ANC-2 (Second Trimester)</option>
                <option value="ANC-3">ANC-3 (28-34 Weeks)</option>
                <option value="ANC-4">ANC-4 (36 Weeks - Delivery)</option>
                <option value="PNC-1">PNC-1 (Within 48 hours)</option>
                <option value="PNC-2">PNC-2 (Day 7-14)</option>
                <option value="Routine Checkup">Routine Checkup</option>
              </select>
            </div>
          </div>
          <div class="form-group">
            <label>Next Scheduled Visit Date (Optional)</label>
            <input type="date" id="vNextDate" />
          </div>
          <div class="form-group">
            <label>Clinical Notes / Observations</label>
            <textarea id="vNotes" placeholder="e.g. BP 120/80, Hb 11.5 g/dL, IFA tablets distributed"></textarea>
          </div>
        `,
        onConfirm: async () => {
          const guardianId = $("#vGuardian")?.value;
          const visitDate = $("#vDate")?.value;
          const visitType = $("#vType")?.value;
          const nextVisitDate = $("#vNextDate")?.value || null;
          const notes = $("#vNotes")?.value.trim() || "";

          if (!guardianId || !visitDate) {
            throw new Error("Guardian and visit date are required.");
          }

          await API.createVisit({
            guardianId: Number(guardianId),
            visitDate,
            visitType,
            nextVisitDate,
            notes
          });

          toast("Maternal visit recorded successfully!", "success");
          setTimeout(() => location.reload(), 400);
        }
      });
    });
  }
}

// -------------------------------------------------------------
// Villages Management Page (Admin)
// -------------------------------------------------------------
async function villagesPage(user) {
  let list = [];
  try {
    list = await API.villages();
    if (!Array.isArray(list)) list = list?.villages || [];
  } catch {}

  shellPage(
    "Villages & Catchment Areas",
    "Configure rural village operational zones and service areas.",
    `
    <div class="actions" style="margin-bottom:20px">
      <button class="btn btn-primary" id="btnAddVillage"><span>➕</span> Add New Village</button>
    </div>

    <div class="card">
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Village Name</th>
              <th>District</th>
              <th>State</th>
              <th>Status</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            ${
              list.length
                ? list
                    .map(
                      (v) => `
                  <tr>
                    <td><span class="text-mono">#${v.id}</span></td>
                    <td><strong>${escapeHtml(v.name)}</strong></td>
                    <td>${escapeHtml(v.district)}</td>
                    <td>${escapeHtml(v.state)}</td>
                    <td>${v.active !== false ? '<span class="badge badge-green">Active</span>' : '<span class="badge badge-gray">Inactive</span>'}</td>
                    <td>
                      <button class="btn btn-danger btn-small" data-delete-village="${v.id}">Deactivate</button>
                    </td>
                  </tr>`
                    )
                    .join("")
                : `<tr><td colspan="6" class="empty">No villages configured yet.</td></tr>`
            }
          </tbody>
        </table>
      </div>
    </div>
  `
  );

  $("#btnAddVillage")?.addEventListener("click", () => {
    showModal({
      title: "Add New Village",
      confirmText: "Create Village",
      content: `
        <div class="form-group">
          <label>Village Name *</label>
          <input type="text" id="vName" placeholder="e.g. Rampur" required />
        </div>
        <div class="form-group">
          <label>District *</label>
          <input type="text" id="vDistrict" placeholder="e.g. Lucknow" required />
        </div>
        <div class="form-group">
          <label>State *</label>
          <input type="text" id="vState" placeholder="e.g. Uttar Pradesh" required />
        </div>
      `,
      onConfirm: async () => {
        const name = $("#vName")?.value.trim();
        const district = $("#vDistrict")?.value.trim();
        const state = $("#vState")?.value.trim();

        if (!name || !district || !state) {
          throw new Error("All fields are required.");
        }

        await API.createVillage({ name, district, state });
        toast("Village created successfully!", "success");
        setTimeout(() => location.reload(), 400);
      }
    });
  });

  $$("[data-delete-village]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const id = btn.dataset.deleteVillage;
      if (!confirm("Are you sure you want to deactivate this village?")) return;
      try {
        await API.deleteVillage(id);
        toast("Village deactivated successfully.", "success");
        setTimeout(() => location.reload(), 400);
      } catch (e) {
        toast(e.message, "error");
      }
    });
  });
}

// -------------------------------------------------------------
// Health Workers Management Page (Admin)
// -------------------------------------------------------------
async function workersPage(user) {
  let [workers, villages] = await Promise.all([
    API.workers().catch(() => []),
    API.villages().catch(() => [])
  ]);

  if (!Array.isArray(workers)) workers = workers?.workers || [];
  if (!Array.isArray(villages)) villages = [];

  shellPage(
    "Health Workers Management",
    "Manage ASHA / ANM workers and assign their designated village coverage.",
    `
    <div class="actions" style="margin-bottom:20px">
      <button class="btn btn-primary" id="btnAddWorker"><span>➕</span> Add Health Worker</button>
    </div>

    <div class="card">
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Full Name</th>
              <th>Username</th>
              <th>Role</th>
              <th>Status</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            ${
              workers.length
                ? workers
                    .map(
                      (w) => `
                  <tr>
                    <td><span class="text-mono">#${w.id}</span></td>
                    <td><strong>${escapeHtml(w.fullName)}</strong></td>
                    <td><span class="text-mono">${escapeHtml(w.username)}</span></td>
                    <td><span class="badge badge-blue">${escapeHtml(w.role || "HEALTH_WORKER")}</span></td>
                    <td>${w.enabled !== false ? '<span class="badge badge-green">Active</span>' : '<span class="badge badge-gray">Disabled</span>'}</td>
                    <td>
                      <button class="btn btn-danger btn-small" data-delete-worker="${w.id}">Deactivate</button>
                    </td>
                  </tr>`
                    )
                    .join("")
                : `<tr><td colspan="6" class="empty">No health workers registered yet.</td></tr>`
            }
          </tbody>
        </table>
      </div>
    </div>
  `
  );

  $("#btnAddWorker")?.addEventListener("click", () => {
    const villageCheckboxes = villages
      .map(
        (v) => `
      <label class="checkbox-label">
        <input type="checkbox" name="vCheck" value="${v.id}" />
        <span>${escapeHtml(v.name)} (${escapeHtml(v.district || "")})</span>
      </label>`
      )
      .join("");

    showModal({
      title: "Add New Health Worker",
      confirmText: "Create Account",
      content: `
        <div class="form-group">
          <label>Full Name *</label>
          <input type="text" id="wName" placeholder="e.g. Dr. Sunita Rao" required />
        </div>
        <div class="form-group">
          <label>Username *</label>
          <input type="text" id="wUsername" placeholder="e.g. sunita" required />
        </div>
        <div class="form-group">
          <label>Initial Password *</label>
          <input type="password" id="wPassword" placeholder="Minimum 6 characters" minlength="6" required />
        </div>
        <div class="form-group">
          <label>Assigned Village Coverage</label>
          <div class="checkbox-group">${villageCheckboxes || '<span class="muted">No villages created yet.</span>'}</div>
        </div>
      `,
      onConfirm: async () => {
        const fullName = $("#wName")?.value.trim();
        const username = $("#wUsername")?.value.trim();
        const password = $("#wPassword")?.value;
        const villageIds = $$('input[name="vCheck"]:checked').map((c) => Number(c.value));

        if (!fullName || !username || !password) {
          throw new Error("Full name, username, and password are required.");
        }

        await API.createWorker({ fullName, username, password, villageIds });
        toast("Health worker registered successfully!", "success");
        setTimeout(() => location.reload(), 400);
      }
    });
  });

  $$("[data-delete-worker]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const id = btn.dataset.deleteWorker;
      if (!confirm("Deactivate this health worker account?")) return;
      try {
        await API.deleteWorker(id);
        toast("Worker account deactivated.", "success");
        setTimeout(() => location.reload(), 400);
      } catch (e) {
        toast(e.message, "error");
      }
    });
  });
}

// -------------------------------------------------------------
// Reports Page
// -------------------------------------------------------------
async function reportsPage(user) {
  let rep = { villages: [], beneficiariesCount: 0, completedDoses: 0 };
  try {
    rep = await API.reports();
  } catch {}

  const villages = rep.villages || [];

  shellPage(
    "Reports & Vaccination Analytics",
    "Village-wise health metrics and immunization completion rates.",
    statCards([
      { label: "Enrolled Beneficiaries", value: rep.beneficiariesCount, hint: "Mothers + children", icon: "👥" },
      { label: "Administered Doses", value: rep.completedDoses, hint: "Verified vaccinations", icon: "💉" },
      { label: "Total Villages", value: villages.length, hint: "Operational coverage", icon: "🏘️" }
    ]) +
      `
    <div class="card" style="margin-top:20px">
      <div class="card-head">
        <h2>Village Coverage Directory</h2>
      </div>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Village Name</th>
              <th>District</th>
              <th>State</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            ${
              villages.length
                ? villages
                    .map(
                      (v) => `
                  <tr>
                    <td><span class="text-mono">#${v.id}</span></td>
                    <td><strong>${escapeHtml(v.name)}</strong></td>
                    <td>${escapeHtml(v.district)}</td>
                    <td>${escapeHtml(v.state)}</td>
                    <td>${v.active !== false ? '<span class="badge badge-green">Active</span>' : '<span class="badge badge-gray">Inactive</span>'}</td>
                  </tr>`
                    )
                    .join("")
                : `<tr><td colspan="5" class="empty">No village data available.</td></tr>`
            }
          </tbody>
        </table>
      </div>
    </div>
  `
  );
}

// -------------------------------------------------------------
// Notifications Page
// -------------------------------------------------------------
async function notificationsPage(user) {
  let notifs = [];
  try {
    notifs = await API.notifications();
    if (!Array.isArray(notifs)) notifs = notifs?.notifications || [];
  } catch {}

  shellPage(
    "Notifications & Reminders",
    "Automated alerts for upcoming vaccination dates and overdue milestones.",
    `
    <div class="card">
      <div class="notifications-list" id="notifContainer">
        ${
          notifs.length
            ? notifs
                .map((n) => {
                  const isRead = !!n.readAt;
                  const isOverdue = String(n.notificationType || "").includes("OVERDUE");

                  return `
                <div class="notif-item ${isRead ? "read" : "unread"}">
                  <div class="notif-icon ${isOverdue ? "notif-icon-red" : "notif-icon-blue"}">
                    ${isOverdue ? "⚠️" : "🔔"}
                  </div>
                  <div class="notif-content">
                    <div class="notif-title">
                      <strong>${escapeHtml(n.message || "Vaccination alert")}</strong>
                      <span class="notif-date muted">${escapeHtml(n.scheduledFor || n.createdAt || "")}</span>
                    </div>
                    <div class="notif-badge">
                      <span class="badge ${isOverdue ? "badge-red" : "badge-blue"}">${escapeHtml(n.notificationType || "REMINDER")}</span>
                    </div>
                  </div>
                  <div class="notif-action">
                    ${
                      isRead
                        ? `<span class="badge badge-gray">Read</span>`
                        : `<button class="btn btn-secondary btn-small" data-read-notif="${n.id}">Mark as Read</button>`
                    }
                  </div>
                </div>`;
                })
                .join("")
            : `<div class="empty">No active notifications or reminders found.</div>`
        }
      </div>
    </div>
  `
  );

  $$("[data-read-notif]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const id = btn.dataset.readNotif;
      try {
        await API.readNotification(id);
        toast("Notification marked as read.", "success");
        const item = btn.closest(".notif-item");
        if (item) {
          item.classList.remove("unread");
          item.classList.add("read");
        }
        btn.outerHTML = `<span class="badge badge-gray">Read</span>`;
      } catch (e) {
        toast(e.message, "error");
      }
    });
  });
}

// -------------------------------------------------------------
// Profile Page
// -------------------------------------------------------------
async function profilePage(user) {
  let profile = user || getUser() || {};
  try {
    const fetched = await API.profile();
    if (fetched && fetched.id) {
      if ((!profile.id || fetched.id === profile.id) && (!profile.role || fetched.role === profile.role)) {
        profile = { ...profile, ...fetched };
      }
    }
  } catch {}

  const fields = [
    { label: "Account ID", value: `#${profile.id || "-"}` },
    { label: "Full Name", value: profile.fullName || "-" },
    { label: "Username", value: profile.username || "-" },
    { label: "Designated Role", value: roleLabel(profile.role) },
    { label: "Account Status", value: profile.enabled !== false ? "Active / Enabled" : "Disabled" },
    { label: "Created At", value: profile.createdAt || "System Baseline" }
  ];

  shellPage(
    "User Profile",
    "Account details and permissions.",
    `
    <div class="card" style="max-width:760px">
      <div class="profile-header">
        <div class="avatar avatar-large js-avatar">${initials(profile.fullName || profile.username)}</div>
        <div>
          <h2>${escapeHtml(profile.fullName || profile.username)}</h2>
          <span class="badge badge-blue">${escapeHtml(roleLabel(profile.role))}</span>
        </div>
      </div>

      <div class="profile-details" style="margin-top:20px">
        ${fields
          .map(
            (f) => `
          <div class="profile-row">
            <span class="profile-label">${escapeHtml(f.label)}</span>
            <span class="profile-value"><strong>${escapeHtml(f.value)}</strong></span>
          </div>`
          )
          .join("")}
      </div>

      <div class="actions" style="margin-top:24px">
        <button class="btn btn-secondary" onclick="location.reload()">Refresh Data</button>
      </div>
    </div>
  `
  );
}

// Initialize on DOM load
document.addEventListener("DOMContentLoaded", () => {
  initLogin();
  initRegister();
});
