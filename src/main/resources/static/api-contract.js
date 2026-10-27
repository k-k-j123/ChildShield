/* ChildShield API contract placeholders — align with backend_prd_tasklist.md.
 * Default mode is demo/localStorage. Set window.CHILD_SHIELD_CONFIG.useApi=true
 * and apiBaseUrl to the running Spring Boot origin when endpoints are implemented.
 * These are proposed routes; do not assume they exist until backend controllers are ready.
 */
window.CHILD_SHIELD_CONFIG = window.CHILD_SHIELD_CONFIG || {
  useApi: false,
  apiBaseUrl: '',
  credentials: 'same-origin',
  routes: {
    authLogin: '/login', authLogout: '/logout', register: '/register',
    // Admin-only healthcare-worker management. Backend must enforce ADMIN role.
    users: '/api/admin/health-workers', userById: id => `/api/admin/health-workers/${encodeURIComponent(id)}`,
    userEdit: id => `/api/admin/health-workers/${encodeURIComponent(id)}`,
    userStatus: id => `/api/admin/health-workers/${encodeURIComponent(id)}/status`,
    villages: '/api/villages', workerAssignments: id => `/api/admin/health-workers/${encodeURIComponent(id)}/villages`,
    // Health-worker mother/child workflow; server scopes records to assigned villages.
    guardians: '/api/health-worker/guardians', guardianById: id => `/api/health-worker/guardians/${encodeURIComponent(id)}`,
    guardianChildren: id => `/api/health-worker/guardians/${encodeURIComponent(id)}/children`,
    children: '/api/health-worker/children', childById: id => `/api/health-worker/children/${encodeURIComponent(id)}`,
    vaccinations: '/vaccinations', vaccinationById: id => `/vaccinations/${encodeURIComponent(id)}`,
    administerVaccination: id => `/vaccinations/${encodeURIComponent(id)}/administer`,
    maternalVisits: '/maternal-visits', maternalVisitById: id => `/maternal-visits/${encodeURIComponent(id)}`,
    notifications: '/notifications', notificationById: id => `/notifications/${encodeURIComponent(id)}`,
    dashboard: '/dashboard', parentDashboard: '/parent', reports: '/reports', profile: '/profile'
  },
  // DB contract reference: frontend form fields -> exact schema table/columns.
  schema: {
    users: ['id','full_name','username','password_hash','role','enabled','created_at','updated_at'],
    villages: ['id','name','district','state','active'],
    health_worker_villages: ['health_worker_id','village_id'],
    guardians: ['id','user_id','full_name','phone','village_id','created_by','created_at','updated_at'],
    children: ['id','guardian_id','full_name','date_of_birth','gender','birth_weight_kg','village_id','created_by','created_at','updated_at'],
    vaccines: ['id','code','name','description','active'],
    schedule_versions: ['id','name','version','source_url','effective_from','effective_to','status'],
    schedule_doses: ['id','schedule_version_id','vaccine_id','dose_code','display_name','age_value','age_unit','region_rule','region_code','sequence_number','active'],
    child_vaccinations: ['id','child_id','schedule_dose_id','due_date','status','administered_date','recorded_by','notes','created_at','updated_at'],
    maternal_visits: ['id','guardian_id','visit_date','visit_type','notes','next_visit_date','recorded_by','created_at','updated_at'],
    notifications: ['id','recipient_user_id','child_vaccination_id','notification_type','message','scheduled_for','created_at','read_at','reminder_key']
  }
};
window.ChildShieldApi = {
  url(path) { return `${window.CHILD_SHIELD_CONFIG.apiBaseUrl || ''}${path}`; },
  async request(path, options = {}) {
    if (!window.CHILD_SHIELD_CONFIG.useApi) throw new Error('API mode is disabled. Use the local demo mode or enable API mode after implementing the Spring Boot route.');
    const headers = { 'Accept': 'application/json', ...(options.body ? {'Content-Type':'application/json'} : {}), ...(options.headers || {}) };
    const response = await fetch(this.url(path), { credentials: window.CHILD_SHIELD_CONFIG.credentials || 'same-origin', ...options, headers });
    if (!response.ok) { const message = await response.text().catch(()=> ''); throw new Error(message || `API request failed (${response.status})`); }
    if (response.status === 204) return null;
    const type = response.headers.get('content-type') || '';
    return type.includes('application/json') ? response.json() : response.text();
  }
};
