# Rural Mother & Child Vaccination Tracker

## Frontend PRD and AI-Agent Task List

**Document version:** 1.0\
**Status:** Implementation-ready baseline\
**Frontend stack:** Thymeleaf, HTML5, CSS3, vanilla JavaScript\
**Application style:** Server-rendered Spring MVC application\
**Audience:** Frontend developer or autonomous AI coding agent

------------------------------------------------------------------------

## 1. Purpose

Build the complete user interface for a local-first Rural Mother & Child
Vaccination Tracker. The frontend is server-rendered by Spring Boot
using Thymeleaf. It must not introduce React, Vue, Angular, a frontend
SPA, Node-based runtime dependencies, or a UI component framework.

The interface supports three roles:

-   `ADMIN`: manages villages, health workers, assignments, and can
    access all records.
-   `HEALTH_WORKER`: manages guardians, children, maternal visits, and
    vaccination records within assigned villages.
-   `PARENT`: views only their linked children, vaccination schedule,
    and own notifications.

The frontend must be modular, accessible, responsive, secure by default,
and straightforward to restyle or extend.

## 2. Product goals

1.  Enable a health worker to register a guardian and child quickly.
2.  Present each child's planned vaccination schedule and actual
    administration history clearly.
3.  Make upcoming and overdue vaccinations easy to identify.
4.  Give administrators a straightforward way to manage workers and
    village assignments.
5.  Let parents view their linked children's information without editing
    clinical records.
6.  Provide consistent navigation, validation, error messages, and page
    layouts.
7.  Keep presentation logic separate from business rules; the backend
    remains authoritative.

## 3. Non-goals

Do not implement:

-   React or another frontend framework.
-   Real SMS, OTP, email, or push delivery.
-   Offline synchronization or a PWA.
-   Clinical decision support or automatic catch-up scheduling.
-   Direct database access from the browser.
-   Authentication logic in JavaScript.
-   Client-side authorization as a substitute for server authorization.
-   A separate frontend server or build pipeline.
-   A dashboard charting library unless explicitly approved later.

## 4. Technical constraints

-   Templates live under `src/main/resources/templates/`.
-   Static assets live under `src/main/resources/static/`.
-   Use Thymeleaf attributes such as `th:each`, `th:if`, `th:object`,
    `th:field`, `th:href`, and `th:action`.
-   Use standard HTML forms and server-side POST handlers.
-   Use POST-Redirect-GET after successful form submissions.
-   Every state-changing form must include the Spring Security CSRF
    token. Prefer Thymeleaf/Spring Security integration rather than
    manually inventing a token.
-   Do not use inline JavaScript for application logic. Keep scripts in
    static JS files and pass server data through safely escaped HTML
    attributes or JSON serialization supported by the backend.
-   Do not place secrets or sensitive health information in URLs, local
    storage, session storage, or console logs.
-   Use semantic HTML and labels for all form controls.
-   Keep CSS namespaced and component-oriented. Avoid global selectors
    that make future changes risky.
-   All UI routes and model attribute names must match the backend
    integration contract.
-   Do not create mock production behavior that pretends data was saved.
    If a backend dependency is not ready, use clearly identified
    temporary fixtures only in a local development profile.

## 5. Information architecture and routes

The backend owns routes and renders HTML. These are the expected UI
routes; coordinate final mappings with the backend agent.

  -------------------------------------------------------------------------------
  Page                    Route                           Roles
  ----------------------- ------------------------------- -----------------------
  Login                   `GET /login`                    Public

  Dashboard               `GET /dashboard`                Admin, health worker

  Guardian list           `GET /guardians`                Admin, health worker

  New guardian            `GET /guardians/new`            Admin, health worker

  Guardian detail         `GET /guardians/{id}`           Admin, authorized
                                                          worker

  Edit guardian           `GET /guardians/{id}/edit`      Admin, authorized
                                                          worker

  Child list              `GET /children`                 Admin, health worker

  New child               `GET /children/new`             Admin, health worker

  Child detail            `GET /children/{id}`            Admin, authorized
                                                          worker, linked parent

  Edit child              `GET /children/{id}/edit`       Admin, authorized
                                                          worker

  Vaccination list        `GET /vaccinations`             Admin, health worker

  Vaccination correction  `GET /vaccinations/{id}/edit`   Admin, authorized
                                                          worker

  Villages                `GET /villages`                 Admin, health worker

  Workers                 `GET /workers`                  Admin only

  New worker              `GET /workers/new`              Admin only

  Notifications           `GET /notifications`            Authenticated users

  Parent home             `GET /parent`                   Parent

  Error pages             `403`, `404`, `500`             As applicable
  -------------------------------------------------------------------------------

Expected form POST routes include `/guardians`, `/children`,
`/vaccinations/{id}/administer`, `/vaccinations/{id}/edit`, `/workers`,
`/workers/{id}/villages`, `/notifications/{id}/read`, and
`/guardians/{id}/visits`. The backend agent must publish the final route
table before frontend integration.

## 6. Page requirements

### 6.1 Shared layout

Create reusable fragments:

-   `fragments/head.html`: page title, viewport, stylesheet links.
-   `fragments/sidebar.html`: role-aware navigation.
-   `fragments/navbar.html`: current user, page title/breadcrumb, logout
    form.
-   `fragments/flash-messages.html`: success, validation, warning, and
    error feedback.
-   `fragments/footer.html`: optional shared footer.

Use a consistent layout with: - Sidebar navigation on desktop. -
Accessible navigation toggle on small screens. - Main content area with
a clear heading and primary action. - Consistent spacing, form controls,
buttons, tables, status indicators, and empty states. - Visible focus
states and keyboard-operable controls. - No reliance on color alone to
convey status.

### 6.2 Login page

-   Username/phone and password fields.
-   Submit button, validation feedback, and generic authentication
    failure message.
-   Do not reveal whether a username exists.
-   Do not include a self-registration link.
-   Do not store credentials in JavaScript or browser storage.
-   Show an appropriate disabled/loading state only if it can be
    implemented without breaking standard form submission.

### 6.3 Dashboard

For administrator and health worker: - Summary cards: total children,
completed doses, pending doses, upcoming doses, overdue doses. - Overdue
vaccination table. - Upcoming vaccination table. - Village
summary/coverage table. - Quick links to register a guardian or child. -
Search/filter controls where provided by backend. - Empty states for no
data and error states for failed requests.

Counts must be labeled clearly. Do not infer coverage percentages from
incompatible denominators. The backend supplies the data and determines
scope based on role and assigned villages.

### 6.4 Guardian pages

Guardian list: - Search by name or phone. - Filter by village where
authorized. - Table columns: name, phone, village, number of linked
children, actions. - Pagination if supplied by backend.

Guardian form: - Full name, phone, village. - Field-level validation
errors. - Preserve valid submitted values after validation failure. -
For workers, village choices must be limited to assigned villages.

Guardian detail: - Guardian details. - Linked children. - Maternal visit
history. - Form/action to add a maternal visit. - Edit link where
permitted.

### 6.5 Child pages

Child list: - Search by name. - Filter by village and vaccination
status. - Columns: name, date of birth/age, guardian, village, overdue
count, next due date. - Clearly distinguish no overdue doses from
missing schedule data.

Child form: - Guardian selection/search, name, date of birth, gender,
optional birth weight, village. - Default village from guardian where
the backend supports it. - Never calculate or authoritatively assign the
vaccine schedule in browser JavaScript. - Use native date and number
input validation as a convenience, with backend errors displayed as
authoritative.

Child detail: - Child profile and guardian information. - Vaccination
timeline/table with vaccine, dose, due date, status, administered
date. - Maternal/guardian-related links only when appropriate. -
Record-dose action for authorized workers. - Clear warning that due
dates are tracking information and not a substitute for health-worker
guidance.

### 6.6 Vaccination pages

-   Filters for overdue, due today, upcoming, completed, and cancelled.
-   Columns: child, village, vaccine/dose, due date, status,
    administration date, action.
-   Record administration form: actual date and optional notes.
-   Correction form: only fields supported by backend.
-   Confirm destructive or clinically significant actions; do not
    provide ordinary delete buttons for completed records.
-   Display validation errors and success messages after redirects.
-   Show status text in addition to color.

### 6.7 Worker and village management

Workers page (administrator only): - List workers and enabled/disabled
status. - Create worker form with name, username, initial password. -
Assign one or more villages. - Show assigned villages. - Do not allow
role selection from a free-form user-controlled field.

Villages page: - List permitted villages for health workers and all
villages for administrators. - Administrators can create/edit
villages. - Health workers have read-only access.

### 6.8 Parent pages

-   Parent home lists only the parent's linked children.
-   Child detail shows vaccination timeline, upcoming and overdue
    status, and relevant notifications.
-   No controls to register/complete/correct vaccinations.
-   Do not show internal health-worker notes unless explicitly approved
    by product requirements.
-   Use neutral language; do not present the app as making clinical
    recommendations.

### 6.9 Notifications

-   List notification message, type, creation/scheduled time, read
    state, and linked child/dose when permitted.
-   Provide a POST form/button to mark as read.
-   The UI must not imply that an SMS was sent. These are in-app
    simulated notifications only.

### 6.10 Error and empty states

Provide friendly pages or reusable fragments for: - `400`: invalid
input. - `401`: unauthenticated/session expired. - `403`: access
denied. - `404`: record not found. - `409`: conflict, duplicate or
invalid state transition. - `500`: unexpected server error.

Do not expose stack traces, SQL, filesystem paths, internal exception
names, or personal information in error pages.

## 7. Design system requirements

Use plain CSS variables for easy global changes:

-   Color tokens: primary, accent, background, surface, text, muted
    text, border, success, warning, danger.
-   Spacing scale.
-   Border radius and shadow tokens.
-   Font stack with sensible system fallbacks.
-   Breakpoints for mobile, tablet, desktop.
-   Component styles for buttons, fields, tables, cards, badges, alerts,
    pagination, and navigation.

Keep status labels accessible: - Completed. - Upcoming. - Due today. -
Overdue. - Cancelled. - Scheduled.

Use text and optional icons alongside colors. Avoid overly decorative
design; optimize for clarity, legibility, and quick data entry.

## 8. Accessibility and usability

-   Every form input has a visible `<label>`.
-   Use proper heading hierarchy and landmarks.
-   Keyboard navigation must work without a mouse.
-   Focus indicators must remain visible.
-   Error messages should identify the affected field and explain how to
    correct it.
-   Tables should have header cells and appropriate scope.
-   Buttons must have clear action labels.
-   Use confirmation for sensitive changes.
-   Support narrow screens without horizontal overflow wherever
    practical.
-   Dates should use one consistent display format while form
    submissions use ISO date values.
-   Avoid tiny text and color-only status indicators.

## 9. Frontend integration contract

The backend agent must provide: 1. Final route/method table. 2.
Thymeleaf template names and model attribute names for every GET route.
3. Form field names and validation error attribute conventions. 4. Flash
message conventions. 5. Role names and navigation rules. 6.
Pagination/filter parameter names. 7. CSRF integration expectations. 8.
DTO/view-model shapes for dashboard and tables. 9. Expected error
handling and redirects. 10. A running local application or agreed
integration branch.

The frontend agent must not invent route names or model fields
independently. If a contract is missing, document the assumption and
request agreement before building dependent templates.

## 10. Modular implementation rules for AI agents

-   Keep each page template focused on one page; extract only genuinely
    shared layout fragments.
-   Avoid putting business logic, authorization decisions, schedule
    calculations, or database logic in templates or JavaScript.
-   Keep JavaScript progressive-enhancement-only; forms should work
    through normal HTTP submission.
-   Avoid duplicate CSS and use shared variables/components.
-   Do not add a new library without explaining the need and getting
    approval.
-   Do not rename backend routes, fields, roles, or templates expected
    by another agent without updating the integration contract.
-   Prefer small, reviewable changes.
-   Never overwrite unrelated work in a shared repository.
-   Add or update tests and documentation with each meaningful change.
-   Keep demo fixtures out of production configuration.
-   Never commit real personal data, passwords, secrets, or production
    records.

## 11. Agile delivery plan

Use two-week sprints where practical; if the project is shorter, keep
the same sprint goals with smaller timeboxes.

### Sprint 0 --- Alignment and scaffolding

-   Agree on routes, model attributes, roles, visual tokens, and
    repository conventions.
-   Create template/static directory structure.
-   Create shared layout and login skeleton.
-   Deliverable: templates render from Spring MVC.

### Sprint 1 --- Authentication and shared layout

-   Login screen, shared fragments, navigation, flash messages, error
    pages.
-   Role-aware navigation driven by authenticated server-side role.
-   Deliverable: each role sees the correct navigation after login.

### Sprint 2 --- Guardian and child workflows

-   Lists, search/filter UI, create/edit forms, detail pages, validation
    errors, empty states.
-   Deliverable: complete beneficiary workflow using backend routes.

### Sprint 3 --- Vaccination and dashboard

-   Dashboard cards/tables, vaccination filters, timeline,
    administration and correction forms.
-   Deliverable: worker can operate the principal tracking workflow.

### Sprint 4 --- Admin, parent, reminders and polish

-   Worker/village pages, parent views, notification list, maternal
    visit UI, responsive/accessibility polish.
-   Deliverable: all role workflows complete.

### Sprint 5 --- Integration hardening

-   Cross-role testing, responsive QA, error states, documentation, bug
    fixes.
-   Deliverable: release candidate.

## 12. Frontend task list and acceptance criteria

### F0 --- Integration contract

-   [ ] Obtain approved route table and template model contract.
-   [ ] Confirm role names and route authorization.
-   [ ] Confirm flash message and field-error conventions.
-   [ ] Confirm frontend asset and template naming conventions.
    **Acceptance:** no unresolved assumptions block initial template
    work.

### F1 --- Shared layout and styling

-   [ ] Create shared Thymeleaf fragments.
-   [ ] Create CSS tokens and responsive layout.
-   [ ] Add reusable button, field, table, badge, card and alert styles.
-   [ ] Add accessible navigation and focus states. **Acceptance:** all
    page skeletons share consistent layout and work on mobile/desktop.

### F2 --- Authentication and errors

-   [ ] Implement login template.
-   [ ] Display authentication and validation errors.
-   [ ] Add logout POST form with CSRF.
-   [ ] Add access-denied, not-found and generic error templates.
    **Acceptance:** no credentials are stored client-side and error
    pages reveal no internal details.

### F3 --- Guardian workflows

-   [ ] Guardian list/search/filter.
-   [ ] Guardian create/edit forms.
-   [ ] Guardian detail and linked children.
-   [ ] Maternal visit list/form. **Acceptance:** server-side validation
    errors and submitted values render correctly.

### F4 --- Child workflows

-   [ ] Child list and filters.
-   [ ] Child registration/edit forms.
-   [ ] Child profile and vaccination timeline.
-   [ ] Correct status labels and empty states. **Acceptance:** child
    forms submit to backend and display server-generated schedule
    records.

### F5 --- Vaccination workflows

-   [ ] Vaccination list and status filters.
-   [ ] Administration form.
-   [ ] Correction form.
-   [ ] Confirmation and success/error messages. **Acceptance:** no
    client-only operation can mark a dose complete without server
    confirmation.

### F6 --- Dashboard

-   [ ] Summary cards.
-   [ ] Overdue and upcoming tables.
-   [ ] Village summary.
-   [ ] Quick links and empty states. **Acceptance:** displayed values
    come from backend model data and respect the user's
    server-authorized scope.

### F7 --- Admin and parent pages

-   [ ] Worker list/create page.
-   [ ] Village assignment form.
-   [ ] Village list/create/edit UI as authorized.
-   [ ] Parent home and linked-child pages.
-   [ ] Notification list/read action. **Acceptance:** parent UI
    contains no clinical editing controls; admin-only navigation is
    hidden from other roles.

### F8 --- QA and documentation

-   [ ] Keyboard/accessibility pass.
-   [ ] Responsive layout pass.
-   [ ] Verify CSRF on state-changing forms.
-   [ ] Verify all route/model names against backend contract.
-   [ ] Document how to run and test the UI. **Acceptance:** agreed
    end-to-end scenarios pass on the integrated application.

## 13. Definition of done

A frontend task is done when: - The template and any static assets are
in the correct directories. - It uses agreed backend routes and model
attributes. - It has loading/empty/error/success behavior appropriate to
server-rendered pages. - It is keyboard-accessible and responsive. - It
contains no business logic or authorization assumptions that bypass the
backend. - Relevant tests or manual test steps are documented. - The
change is reviewed and integrated without breaking other agent work.

## 14. Handoff checklist

Before the frontend agent starts: - \[ \] Backend agent confirms route
and model contract. - \[ \] Shared repository branch and file ownership
are agreed. - \[ \] The app can render a basic Thymeleaf template. - \[
\] Role constants and navigation labels are agreed. - \[ \] CSS naming
conventions are agreed.

Before frontend completion: - \[ \] All pages use real backend model
data. - \[ \] All state-changing forms include CSRF. - \[ \]
Role-specific pages are protected server-side. - \[ \] No
React/Vue/Angular dependency was introduced. - \[ \] No real
SMS-delivery claim is shown. - \[ \] Setup and test instructions are
updated.
