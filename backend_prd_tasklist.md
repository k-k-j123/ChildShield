# Rural Mother & Child Vaccination Tracker

## Backend PRD and AI-Agent Task List

**Document version:** 1.0\
**Status:** Implementation-ready baseline\
**Backend stack:** Java 21, Spring Boot 3.x, Spring MVC, Spring
Security, Spring Data JPA, MySQL\
**Audience:** Backend developer or autonomous AI coding agent

------------------------------------------------------------------------

## 1. Purpose

Build the backend for a local-first Rural Mother & Child Vaccination
Tracker. The backend is responsible for authentication, authorization,
persistence, data validation, vaccination scheduling, dashboard queries,
maternal visit records, and simulated reminder generation.

The frontend is server-rendered using Thymeleaf templates in the same
Spring Boot application. Do not build a separate frontend server or
require React/Node.js.

Three roles: - `ADMIN`: manages all villages, workers, assignments and
records. - `HEALTH_WORKER`: manages beneficiaries and vaccination
records in assigned villages. - `PARENT`: views only their linked
children, schedules, and notifications.

## 2. Product goals

1.  Securely manage users and role-based access.
2.  Scope worker access to assigned villages on every request.
3.  Register guardians and children and generate planned vaccine-dose
    records.
4.  Track scheduled, completed, due-today, upcoming, overdue, and
    cancelled doses.
5.  Support basic vaccination record corrections.
6.  Provide dashboard summary and village reports.
7.  Record maternal health visits.
8.  Generate simulated in-app reminders using a scheduled backend task.
9.  Keep modules small, cohesive, testable, and changeable.

## 3. Non-goals

Do not implement: - Real SMS, OTP, email or push delivery. - Mobile apps
or offline synchronization. - Government health-system integration. -
Clinical decision support or automatic catch-up schedules. -
Microservices, message brokers, Redis, or cloud deployment. - A separate
React SPA or public REST API requirement. - Automatic marking of a
vaccine as administered. - Full immutable audit history in the initial
version; basic created/updated metadata is sufficient.

## 4. Technical baseline

-   Java 21 LTS.
-   Spring Boot 3.x using a compatible current maintenance release.
-   Maven.
-   Spring MVC and Thymeleaf.
-   Spring Security with server-side sessions.
-   Spring Data JPA and Hibernate.
-   MySQL 8.4 LTS, InnoDB.
-   Flyway for schema migrations.
-   Jakarta Bean Validation.
-   BCrypt password encoding or another approved Spring Security
    password encoder.
-   Spring Scheduling for reminder generation.
-   JUnit, Spring Boot Test, MockMvc, and Testcontainers if
    available/approved.
-   Use compatible versions managed by the Spring Boot dependency BOM;
    do not pin arbitrary incompatible transitive versions.

## 5. Architecture and modularity

Use a **feature-first, layer-separated modular monolith**. Each business feature owns its related classes, and within that feature the code is separated into `model`, `dto`, `repository`, `service`, and `controller` packages. This combines clear layer boundaries with feature cohesion: developers can change the child workflow without searching through one global package containing every controller, service, repository, and model.

### 5.1 Recommended package structure

```text
com.example.vaccinationtracker/
├── VaccinationTrackerApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── SchedulingConfig.java
│   └── WebConfig.java
├── common/
│   ├── exception/
│   ├── validation/
│   └── util/
├── auth/
│   ├── controller/
│   ├── dto/
│   └── service/
├── user/
│   ├── controller/
│   ├── dto/
│   ├── model/
│   ├── repository/
│   └── service/
├── village/
│   ├── controller/
│   ├── dto/
│   ├── model/
│   ├── repository/
│   └── service/
├── guardian/
│   ├── controller/
│   ├── dto/
│   ├── model/
│   ├── repository/
│   └── service/
├── child/
│   ├── controller/
│   ├── dto/
│   ├── model/
│   ├── repository/
│   └── service/
├── vaccination/
│   ├── controller/
│   ├── dto/
│   ├── model/
│   ├── repository/
│   └── service/
├── maternalvisit/
│   ├── controller/
│   ├── dto/
│   ├── model/
│   ├── repository/
│   └── service/
├── notification/
│   ├── controller/
│   ├── dto/
│   ├── model/
│   ├── repository/
│   └── service/
└── dashboard/
    ├── controller/
    ├── dto/
    ├── repository/
    └── service/
```

The `dashboard` feature generally needs DTOs/projections, a service, and a controller, but does not need its own JPA entity unless a real persisted dashboard concept is introduced. Likewise, `auth` does not need a separate model package if it uses the `user` feature's user model and Spring Security types.

### 5.2 Responsibilities by layer

- **`model`**: JPA entities and domain enums only. Examples: `User`, `Village`, `Guardian`, `Child`, `Vaccine`, `ScheduleVersion`, `ScheduleDose`, `ChildVaccination`, `MaternalVisit`, `Notification`.
- **`dto`**: form objects, validation inputs, and view models. Use names that express purpose, such as `CreateChildForm`, `UpdateChildForm`, `ChildDetailsView`, and `VaccinationRowView`. Do not bind web forms directly to JPA entities.
- **`repository`**: Spring Data JPA repositories and focused custom queries/projections. No business workflows or authorization decisions should live only here.
- **`service`**: business rules, transactions, schedule generation, object-level authorization checks, and coordination between repositories/features. Keep services cohesive; avoid one giant service per application.
- **`controller`**: Spring MVC request mapping, form binding, validation result handling, model attributes, view names, redirects, and flash messages. Controllers should delegate business work to services and should not contain database logic.
- **`config`**: application-wide Spring configuration, security, scheduling, and web settings.
- **`common`**: genuinely cross-cutting exceptions, validation helpers, and utilities. Do not turn it into a dumping ground for feature-specific logic.

### 5.3 Dependency rules

1. Controllers may depend on DTOs and services, not directly on repositories.
2. Services may depend on repositories and other services through clear interfaces; avoid circular dependencies.
3. Repositories depend on models and persistence abstractions only.
4. DTOs must not extend JPA entities or expose password hashes/internal-only fields.
5. Thymeleaf templates receive explicit view models or carefully selected DTOs, not broad entity graphs.
6. Put `@Transactional` on service methods at appropriate use-case boundaries.
7. Use constructor injection and avoid static service access.
8. Keep package-private helpers private to a feature unless another feature has a real, stable need for them.
9. Cross-feature calls should use the owning feature's service, not reach into its repository directly.
10. Do not create interfaces for every class automatically. Add interfaces when they clarify a boundary, enable a meaningful alternative, or improve testing.

### 5.4 Naming and modularity rules for AI agents

- Use singular feature names (`child`, `guardian`, `vaccination`) and consistent class names (`ChildController`, `ChildService`, `ChildRepository`, `Child` entity).
- Prefer small use-case methods such as `registerChild`, `getChildDetails`, `recordAdministration`, and `correctVaccinationRecord`.
- Keep schedule calculation in a dedicated service within `vaccination/service`, not in a controller, entity callback, or Thymeleaf template.
- Keep role and village authorization checks centralized in service-level policy/helper methods and test them explicitly.
- Do not create a global `BaseController`, `BaseService`, or generic repository abstraction without a demonstrated need.
- Keep migrations in `src/main/resources/db/migration/`; do not scatter schema creation across Java code.
- Add tests alongside each feature's behavior, using the project's agreed test package convention.

This structure is preferred over a purely global layer structure such as `controller/`, `service/`, `repository/`, `model/`, and `dto/` at the application root. A global layer structure is initially simple, but as the project grows, each package becomes a mixed collection of unrelated features and parallel agents are more likely to edit or conflict over the same files. The recommended feature-first structure still gives every feature the layer separation requested.

## 6. Database specification

Use database `vaccination_tracker`. Use BIGINT identity primary keys,
DATE for calendar dates, DATETIME for timestamps, foreign keys, indexes,
and appropriate unique constraints. Store timestamps consistently and
configure the application time zone deliberately.

### 6.1 `users`

-   `id`: BIGINT PK.
-   `full_name`: VARCHAR(120), required.
-   `username`: VARCHAR(80), required, unique.
-   `password_hash`: VARCHAR(255), required.
-   `role`: VARCHAR(20), required enum/string: ADMIN, HEALTH_WORKER,
    PARENT.
-   `enabled`: BOOLEAN, default true.
-   `created_at`, `updated_at`: DATETIME, required.

For parent accounts, username may be a phone number. Phone numbers are
strings, not integers.

### 6.2 `villages`

-   `id`: BIGINT PK.
-   `name`: VARCHAR(120), required.
-   `district`: VARCHAR(120), required.
-   `state`: VARCHAR(120), required.
-   `active`: BOOLEAN, default true.
-   Unique constraint on `(name, district, state)`.

### 6.3 `health_worker_villages`

-   `health_worker_id`: FK → users.id.
-   `village_id`: FK → villages.id.
-   Composite primary key `(health_worker_id, village_id)`.

Validate that assigned users have the HEALTH_WORKER role.

### 6.4 `guardians`

-   `id`: BIGINT PK.
-   `user_id`: nullable unique FK → users.id.
-   `full_name`: VARCHAR(120), required.
-   `phone`: VARCHAR(20), required.
-   `village_id`: FK → villages.id, required.
-   `created_by`: FK → users.id.
-   `created_at`, `updated_at`: DATETIME, required.

Guardian profiles may exist without parent login accounts. A parent
account must be explicitly linked to one guardian profile.

### 6.5 `children`

-   `id`: BIGINT PK.
-   `guardian_id`: FK → guardians.id, required.
-   `full_name`: VARCHAR(120), required.
-   `date_of_birth`: DATE, required.
-   `gender`: VARCHAR(20), optional.
-   `birth_weight_kg`: DECIMAL(4,2), optional.
-   `village_id`: FK → villages.id, required.
-   `created_by`: FK → users.id.
-   `created_at`, `updated_at`: DATETIME, required.

A guardian can have multiple children. In the MVP, a child has one
guardian. Default village to guardian's village during creation unless
an authorized worker explicitly selects another permitted village.

### 6.6 `vaccines`

-   `id`: BIGINT PK.
-   `code`: VARCHAR(40), required unique.
-   `name`: VARCHAR(120), required.
-   `description`: VARCHAR(500), optional.
-   `active`: BOOLEAN, default true.

### 6.7 `schedule_versions`

-   `id`: BIGINT PK.
-   `name`: VARCHAR(120), required.
-   `version`: VARCHAR(40), required.
-   `source_url`: VARCHAR(500).
-   `effective_from`: DATE, required.
-   `effective_to`: DATE, nullable.
-   `status`: DRAFT, ACTIVE, RETIRED.

The data model should support schedule versioning. Enforce one
applicable active schedule version for the selected region/date through
service validation and, where feasible, database constraints.

### 6.8 `schedule_doses`

-   `id`: BIGINT PK.
-   `schedule_version_id`: FK → schedule_versions.id.
-   `vaccine_id`: FK → vaccines.id.
-   `dose_code`: VARCHAR(40), required.
-   `display_name`: VARCHAR(120), required.
-   `age_value`: INT, required.
-   `age_unit`: DAYS, WEEKS, MONTHS, YEARS.
-   `region_rule`: NATIONAL or REGION_SPECIFIC.
-   `region_code`: VARCHAR(40), nullable.
-   `sequence_number`: INT, required.
-   `active`: BOOLEAN, default true.
-   Unique constraint `(schedule_version_id, dose_code)`.

If official guidance provides an age window, add explicit
minimum/maximum age fields rather than encoding ranges in free text. The
first release may use target age plus documented display windows where
verified.

### 6.9 `child_vaccinations`

-   `id`: BIGINT PK.
-   `child_id`: FK → children.id.
-   `schedule_dose_id`: FK → schedule_doses.id.
-   `due_date`: DATE, required.
-   `status`: PENDING, COMPLETED, CANCELLED.
-   `administered_date`: DATE, nullable.
-   `recorded_by`: nullable FK → users.id.
-   `notes`: VARCHAR(500), optional.
-   `created_at`, `updated_at`: DATETIME, required.
-   Unique constraint `(child_id, schedule_dose_id)`.

Validation: - COMPLETED requires administered date and recorded_by. -
PENDING has no administered date. - Administered date cannot be in the
future. - A completed dose must not be silently deleted through the
UI. - Record corrections are allowed in the MVP and update
`updated_at`. - A due date is not proof of administration.

### 6.10 `maternal_visits`

-   `id`: BIGINT PK.
-   `guardian_id`: FK → guardians.id.
-   `visit_date`: DATE, required.
-   `visit_type`: VARCHAR(40), required.
-   `notes`: VARCHAR(500), optional.
-   `next_visit_date`: DATE, nullable.
-   `recorded_by`: FK → users.id.
-   `created_at`, `updated_at`: DATETIME, required.

This is basic maternal health visit tracking, not a full
pregnancy-management subsystem. Do not imply maternal vaccination
schedules are implemented unless a separate requirement is approved.

### 6.11 `notifications`

-   `id`: BIGINT PK.
-   `recipient_user_id`: FK → users.id.
-   `child_vaccination_id`: nullable FK → child_vaccinations.id.
-   `notification_type`: UPCOMING or OVERDUE.
-   `message`: VARCHAR(500), required.
-   `scheduled_for`: DATETIME, required.
-   `created_at`: DATETIME, required.
-   `read_at`: DATETIME, nullable.
-   Add a unique `reminder_key` or equivalent unique constraint that
    prevents duplicate reminders for the same recipient, vaccination,
    and reminder window.

All schema changes must be committed as Flyway migrations. Do not rely
on Hibernate `ddl-auto=update` as the production schema management
strategy.

## 7. Security and authorization

### 7.1 Roles and access

  ------------------------------------------------------------------------
  Capability         ADMIN             HEALTH_WORKER     PARENT
  ------------------ ----------------- ----------------- -----------------
  Manage all         Yes               No                No
  villages                                               

  Manage workers and Yes               No                No
  assignments                                            

  View all records   Yes               No                No

  View/edit          Yes               Yes               No
  assigned-village                                       
  records                                                

  Register           Yes               Yes               No
  guardian/child                                         

  Record/correct     Yes               Yes               No
  vaccinations                                           

  Record maternal    Yes               Yes               No
  visits                                                 

  View linked child  Yes               Yes within        Yes, own linked
  records                              assigned villages children

  View own           Yes               Yes               Yes
  notifications                                          
  ------------------------------------------------------------------------

### 7.2 Mandatory security behavior

-   Use Spring Security server-side sessions.
-   Use secure password hashing.
-   Configure session fixation protection and invalidate sessions on
    logout.
-   Enable CSRF protection for all state-changing forms.
-   Use HTTP-only cookies; use Secure cookies when HTTPS is enabled and
    choose an appropriate SameSite setting.
-   Do not disable CSRF globally to make form submissions work.
-   Restrict `/workers/**` and village mutation routes to ADMIN.
-   Enforce role and object-level authorization in services/controllers
    for every record identifier.
-   Health workers must be checked against assigned villages for
    guardian, child, vaccination, and maternal visit access.
-   Parent authorization must verify `guardian.user_id` matches the
    authenticated parent, not merely accept a child ID supplied in a
    URL.
-   Return appropriate 403/404 behavior without leaking whether another
    user's protected record exists.
-   Do not expose password hashes or internal entity relationships to
    views.
-   Avoid logging passwords, session identifiers, or unnecessary
    personal/health data.

### 7.3 Authentication endpoints/pages

-   `GET /login`: Thymeleaf login template.
-   `POST /login`: Spring Security authentication processing.
-   `POST /logout`: invalidate session.
-   `GET /dashboard`: redirect/route to the appropriate role dashboard.
-   Optional `GET /parent`: parent landing page.

Use Spring Security form login unless a JSON login API is explicitly
needed. The frontend and backend share an origin, so no CORS
configuration should be necessary for the normal server-rendered flow.

## 8. MVC route contract

Use normal Spring MVC controller methods returning Thymeleaf view names
and model attributes. State-changing requests use POST. Use
POST-Redirect-GET after successful mutations.

### Guardian

-   `GET /guardians`: list/search guardians.
-   `GET /guardians/new`: new form.
-   `POST /guardians`: create guardian.
-   `GET /guardians/{id}`: detail.
-   `GET /guardians/{id}/edit`: edit form.
-   `POST /guardians/{id}/edit`: update guardian.
-   `GET /guardians/{id}/visits/new`: maternal visit form.
-   `POST /guardians/{id}/visits`: create maternal visit.

### Child

-   `GET /children`: list/search/filter children.
-   `GET /children/new`: new form.
-   `POST /children`: create child and generate scheduled dose records
    transactionally.
-   `GET /children/{id}`: detail and schedule.
-   `GET /children/{id}/edit`: edit form.
-   `POST /children/{id}/edit`: update permitted child details.

### Vaccinations

-   `GET /vaccinations`: list/filter records.
-   `POST /vaccinations/{id}/administer`: record administration.
-   `GET /vaccinations/{id}/edit`: correction form.
-   `POST /vaccinations/{id}/edit`: correct date/notes under
    authorization.

### Admin

-   `GET /workers`, `GET /workers/new`, `POST /workers`.
-   `GET /workers/{id}/villages`, `POST /workers/{id}/villages`.
-   `GET /villages`, `GET /villages/new`, `POST /villages`,
    `GET /villages/{id}/edit`, `POST /villages/{id}/edit`.

### Notifications and dashboards

-   `GET /notifications`.
-   `POST /notifications/{id}/read`.
-   `GET /dashboard`.
-   `GET /parent`.
-   `GET /reports/villages` if a dedicated report page is required.

Final mappings may be adjusted by agreement with the frontend agent, but
all changes must be reflected in a shared route contract.

## 9. Form objects, view models and validation

Use dedicated form/DTO classes rather than binding forms directly to JPA
entities.

Suggested forms: - `LoginForm` only if custom login processing is
needed. - `GuardianForm` - `ChildForm` - `WorkerForm` - `VillageForm` -
`VillageAssignmentForm` - `VaccinationAdministrationForm` -
`VaccinationCorrectionForm` - `MaternalVisitForm` - `ParentAccountForm`

Use Jakarta validation constraints and service-level business
validation. Examples: - Required names and valid lengths. - Valid phone
format according to agreed Indian-number input rules, without coercing
it to a number. - Date of birth cannot be in the future. - Birth weight
must be positive and within an agreed validation range. - Administration
date cannot be in the future. - Parent username must be unique. - All
selected village IDs must exist and be authorized. - A guardian/child
relationship must be valid. - Dose status and administration fields must
be consistent.

On validation failure, re-render the form with submitted values and
field errors. On success, use a flash message and redirect.

## 10. Vaccination schedule engine

Use the current Government of India National Immunization Schedule and
applicable official documentation as the source of truth. Reference: -
NHM National Immunization Schedule:
https://nhm.gov.in/images/pdf/programmes/immunization/manual-formats/Current_UIP_Schedule.pdf -
Routine Immunization Manual for Health Workers:
https://www.mohfw.gov.in/sites/default/files/Routine%20Immunization%20Manual%20For%20Health%20Workers.pdf

The schedule data must be reviewed and verified before being seeded. Do
not invent or assume medical timing values from an unverified sample.

### Configuration

-   Load a versioned schedule and its doses using Flyway seed migrations
    or a clearly documented import mechanism.
-   Store source URL, version, effective dates, region eligibility,
    target age, and any verified age windows.
-   Support national doses and region-specific doses, but seed only
    those appropriate to the selected demonstration region.
-   Do not implement a full schedule-admin UI in the first release.

### Generation

When a child is created: 1. Resolve the active schedule version
applicable to the child's village/region and date. 2. Calculate each
planned due date using date-of-birth plus configured age. 3. Insert one
`child_vaccinations` row per applicable schedule dose. 4. Enforce
uniqueness and make generation idempotent. 5. Run generation in a
transaction. 6. Do not mark doses complete automatically.

Use calendar arithmetic for months and years; do not approximate every
month as 30 days. Test month-end, leap-year, and boundary cases. Store
the selected schedule version through the linked `schedule_dose_id`.

When the schedule changes, never silently overwrite completed records or
create duplicates. Any schedule migration must be explicit and reviewed.

### Status logic

-   COMPLETED if saved record status is COMPLETED.
-   CANCELLED if saved record status is CANCELLED.
-   OVERDUE if PENDING and due_date \< today.
-   DUE_TODAY if PENDING and due_date = today.
-   UPCOMING if PENDING and due_date is within the configured next 30
    days.
-   SCHEDULED otherwise.

These derived display states are not persisted unless there is a clear
reason. Due dates are tracking data, not clinical advice or proof of
administration.

## 11. Reminder scheduler

Use Spring Scheduling with a configurable local time zone and reminder
horizon.

Minimum behavior: - Run daily at a configurable time. - Identify pending
vaccinations in the upcoming reminder window and overdue pending
doses. - Create in-app notifications for the appropriate recipients. -
Notify parents only when a linked parent account exists. - Notify
relevant health workers according to the product's agreed notification
policy and assigned village scope. - Use a unique reminder key to
prevent duplicates on retries or repeated scheduler execution. - Use a
transaction around creation batches, with database uniqueness as a final
duplicate guard. - Keep reminder generation separate from delivery; this
MVP creates database records only. - Log aggregate run counts and errors
without logging unnecessary health data. - Provide a way to invoke the
generation service directly in tests.

Suggested reminder windows: upcoming reminders for a configurable number
of days before due date, and overdue reminders at a controlled
frequency. Do not generate a new overdue notification every day
indefinitely. Record the reminder window or cadence in `reminder_key`.

## 12. Dashboard and reporting

Implement repository queries/projections that return only the values
required by views.

Dashboard summary: - Total children in the caller's permitted scope. -
Completed doses. - Pending doses. - Upcoming doses within the configured
window. - Overdue doses. - Village-level child/dose counts and clearly
defined coverage metric.

All counts for a worker must be limited to assigned villages. Parent
pages must not use aggregate queries that reveal other households' data.

Define the coverage denominator before implementation. Recommended
initial definition: eligible scheduled doses due in a selected period,
with completed eligible doses as numerator. Do not mix counts of
children and doses. If eligibility windows cannot be represented
accurately, label the metric as a simple tracking ratio and document its
limitations.

Use projections or DTOs instead of loading full entity graphs for
dashboard tables.

## 13. Error handling and observability

-   Use centralized exception handling for form and API errors where
    relevant.
-   Return/re-render understandable messages for invalid input, not
    found, forbidden, conflict, and unexpected failures.
-   Do not reveal stack traces or SQL errors to users.
-   Log operational failures with enough context to debug without
    dumping sensitive records.
-   Add health endpoint support if needed for local diagnostics.
-   Add database indexes for foreign keys, list filters, due date/status
    queries, usernames, and reminder deduplication.
-   Avoid N+1 query patterns on dashboard/list pages.

## 14. Testing strategy

### Unit tests

-   Schedule due-date calculations.
-   Status derivation.
-   Guardian/child validation.
-   Access-scope service methods.
-   Reminder key/cadence logic.

### Integration tests

-   Repository mappings and constraints.
-   Child creation generates exactly one row per applicable dose.
-   Duplicate generation does not duplicate rows.
-   Completed dose validation and correction.
-   Scheduled reminder generation is idempotent.
-   Session login/logout.
-   Role restrictions and object-level authorization.
-   Worker A cannot access Worker B's village records.
-   Parent cannot view another parent's child or mark unrelated
    notifications read.
-   CSRF blocks unsafe requests without valid tokens.
-   Form validation re-renders values/errors correctly.

Use MySQL-compatible integration testing where feasible, ideally
Testcontainers. If unavailable, document differences and run manual
verification against the selected MySQL version.

## 15. Agile delivery plan

Use small, reviewable increments. Suggested two-week sprints; shorten
timeboxes if needed without skipping acceptance criteria.

### Sprint 0 --- Architecture and scaffolding

-   Create Spring Boot project.
-   Add dependencies and local configuration.
-   Set up MySQL and Flyway.
-   Establish route, role, template model, and naming contracts with
    frontend agent.
-   Add basic Thymeleaf health/dashboard page.

### Sprint 1 --- Authentication and core data

-   Implement users, roles, villages, worker assignments.
-   Configure Spring Security session login/logout and CSRF.
-   Create initial administrator bootstrap mechanism.
-   Test role and session behavior.

### Sprint 2 --- Guardian and child management

-   Implement guardian and child
    entities/repositories/services/controllers.
-   Implement server-side validation and view models.
-   Enforce assigned-village access.
-   Test CRUD and relationship integrity.

### Sprint 3 --- Vaccination schedule

-   Verify official schedule source and regional applicability.
-   Create vaccine/schedule version/dose seed data.
-   Implement schedule calculation and child dose generation.
-   Test date boundaries, idempotency, and uniqueness.

### Sprint 4 --- Tracking and dashboard

-   Implement administration/correction.
-   Implement status queries, summary projections, village reports.
-   Test status transitions and data scope.

### Sprint 5 --- Maternal visits, parents and notifications

-   Link parent accounts.
-   Implement parent-only read views.
-   Implement maternal visits.
-   Implement scheduled notification generation and deduplication.

### Sprint 6 --- Integration and hardening

-   Pair with frontend agent to verify route/model contract.
-   Run end-to-end scenarios.
-   Improve indexes and query performance.
-   Complete setup docs, seed data, and troubleshooting guide.

## 16. Backend task list and acceptance criteria

### B0 --- Contract and scaffolding

-   [ ] Agree final routes, template names, model attributes and form
    field names with frontend agent.
-   [ ] Create Spring Boot/Maven project with compatible dependencies.
-   [ ] Configure local MySQL properties via environment variables or
    ignored local config.
-   [ ] Add Flyway and first migration.
-   [ ] Add shared exception/flash-message conventions. **Acceptance:**
    app starts and renders a basic Thymeleaf page with database
    connectivity confirmed.

### B1 --- Schema and entities

-   [ ] Create migrations for users, villages, worker assignments,
    guardians, children.
-   [ ] Create vaccine, schedule version, schedule dose, child
    vaccination tables.
-   [ ] Create maternal visit and notification tables.
-   [ ] Add foreign keys, unique constraints, indexes and timestamp
    behavior.
-   [ ] Add repositories and mapping tests. **Acceptance:** schema can
    be created from an empty database using migrations only.

### B2 --- Authentication and authorization

-   [ ] Configure session-based Spring Security.
-   [ ] Configure form login and logout.
-   [ ] Enable CSRF.
-   [ ] Implement secure initial administrator bootstrap.
-   [ ] Implement role restrictions.
-   [ ] Implement assigned-village and parent-object authorization.
    **Acceptance:** cross-role and cross-village integration tests pass.

### B3 --- Guardian/child domain

-   [ ] Implement guardian create/list/detail/edit.
-   [ ] Implement child create/list/detail/edit.
-   [ ] Add form DTOs and validation.
-   [ ] Validate village/guardian relationships.
-   [ ] Add pagination/search/filter query support. **Acceptance:**
    worker can manage beneficiaries only in assigned villages.

### B4 --- Schedule engine

-   [ ] Verify official schedule data and source metadata.
-   [ ] Seed one applicable schedule version and verified dose
    definitions.
-   [ ] Implement calendar-based due-date calculation.
-   [ ] Generate dose rows on child registration.
-   [ ] Enforce idempotency and uniqueness.
-   [ ] Test leap-year/month-end/date boundary cases. **Acceptance:**
    generated rows are correct for reviewed test cases and are not
    duplicated on retries.

### B5 --- Vaccination tracking

-   [ ] Implement administration endpoint/form handling.
-   [ ] Implement correction flow.
-   [ ] Validate status/date consistency.
-   [ ] Implement overdue/upcoming/completed queries.
-   [ ] Add village-scoped dashboard projections. **Acceptance:** record
    updates and dashboard lists reflect persisted data and enforce
    scope.

### B6 --- Maternal visits

-   [ ] Implement visit creation/list/detail.
-   [ ] Validate visit dates and guardian access.
-   [ ] Associate each visit with the recording worker. **Acceptance:**
    authorized workers can record/view visits within their village
    scope.

### B7 --- Parent accounts and views

-   [ ] Implement parent account creation/linking by an authorized staff
    member.
-   [ ] Ensure one parent user maps to the intended guardian profile.
-   [ ] Implement queries for linked children only.
-   [ ] Ensure parents cannot mutate vaccination or maternal visit
    records. **Acceptance:** parent object-authorization tests pass for
    direct URL access as well as list pages.

### B8 --- Reminder scheduler

-   [ ] Implement scheduled task and configurable time zone.
-   [ ] Implement upcoming and overdue reminder windows/cadence.
-   [ ] Implement recipient selection.
-   [ ] Implement idempotent reminder keys and database uniqueness.
-   [ ] Add scheduler service tests and failure logging. **Acceptance:**
    repeated runs do not create duplicate notifications for the same
    reminder window.

### B9 --- Dashboard and reporting

-   [ ] Implement summary counts.
-   [ ] Implement overdue/upcoming tables.
-   [ ] Implement village-level reporting with defined denominator.
-   [ ] Optimize queries and add indexes. **Acceptance:** dashboard
    queries return correct scoped counts without loading unnecessary
    entity graphs.

### B10 --- Integration, documentation and release

-   [ ] Publish route/model/form contract for frontend agent.
-   [ ] Test all required workflows end-to-end.
-   [ ] Add development seed data only for local profile.
-   [ ] Document setup, database initialization, admin bootstrap and
    troubleshooting.
-   [ ] Verify no secrets or personal data are committed.
    **Acceptance:** another developer can clone the project, configure
    local MySQL, start the app, sign in as test roles and complete the
    core workflow using the UI.

## 17. Definition of done

A backend task is done when: - Business rules are implemented in
services, not templates. - Inputs are validated server-side. -
Authorization is enforced for role and specific record scope. - Schema
changes are expressed through Flyway. - Relevant unit/integration tests
pass. - Errors are safe and understandable. - No password hashes or
sensitive entity fields leak to the view. - Routes, form fields, and
model attributes match the shared frontend contract. - README/setup
documentation is updated.

## 18. Handoff and multi-agent coordination

Before work begins: - \[ \] Assign file ownership or use separate
branches to prevent simultaneous edits to the same files. - \[ \] Agree
package names, route names, role constants and template model
attributes. - \[ \] Backend agent publishes the initial MVC contract. -
\[ \] Frontend agent consumes the contract rather than inventing
fields. - \[ \] Agree one integration branch and review cadence.

During development: - Prefer small pull requests/commits tied to task
IDs. - Every task should have a concise change summary, tests run, files
changed, and unresolved assumptions. - Do not overwrite or revert
another agent's work. - If a contract changes, update the shared
contract and notify the other agent before dependent work proceeds. -
Keep schema changes additive where practical and coordinate breaking
migrations. - Do not claim a task is complete unless tests were run or
the inability to run them is clearly reported.
