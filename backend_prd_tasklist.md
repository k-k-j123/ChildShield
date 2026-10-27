# Rural Mother & Child Vaccination Tracker

## Backend PRD and AI-Agent Task List

**Document version:** 1.0 (Modified: Flyway and Unit Testing removed in favor of functional testing and Hibernate DDL management)\
**Status:** Implementation-ready baseline\
**Backend stack:** Java 21, Spring Boot 3.x, Spring MVC, Spring Security, Spring Data JPA, MySQL\
**Audience:** Backend developer or autonomous AI coding agent

------------------------------------------------------------------------

## 1. Purpose

Build the backend for a local-first Rural Mother & Child Vaccination Tracker. The backend is responsible for authentication, authorization, persistence, data validation, vaccination scheduling, dashboard queries, maternal visit records, and simulated reminder generation.

The frontend is server-rendered using Thymeleaf templates in the same Spring Boot application. Do not build a separate frontend server or require React/Node.js.

Three roles:
- `ADMIN`: manages all villages, workers, assignments and records.
- `HEALTH_WORKER`: manages beneficiaries and vaccination records in assigned villages.
- `PARENT`: views only their linked children, schedules, and notifications.

## 2. Product goals

1. Securely manage users and role-based access.
2. Scope worker access to assigned villages on every request.
3. Register guardians and children and generate planned vaccine-dose records.
4. Track scheduled, completed, due-today, upcoming, overdue, and cancelled doses.
5. Support basic vaccination record corrections.
6. Provide dashboard summary and village reports.
7. Record maternal health visits.
8. Generate simulated in-app reminders using a scheduled backend task.
9. Keep modules small, cohesive, and changeable.

## 3. Non-goals

Do not implement:
- Real SMS, OTP, email or push delivery.
- Mobile apps or offline synchronization.
- Government health-system integration.
- Clinical decision support or automatic catch-up schedules.
- Microservices, message brokers, Redis, or cloud deployment.
- A separate React SPA or public REST API requirement.
- Automatic marking of a vaccine as administered.
- Full immutable audit history in the initial version; basic created/updated metadata is sufficient.
- Automated unit or integration testing suites (testing will be handled functionally).

## 4. Technical baseline

- Java 21 LTS.
- Spring Boot 3.x using a compatible current maintenance release.
- Maven.
- Spring MVC and Thymeleaf.
- Spring Security with server-side sessions.
- Spring Data JPA and Hibernate (with ddl-auto configuration for schema management).
- MySQL 8.4 LTS, InnoDB.
- Jakarta Bean Validation.
- BCrypt password encoding or another approved Spring Security password encoder.
- Spring Scheduling for reminder generation.
- Use compatible versions managed by the Spring Boot dependency BOM.

## 5. Architecture and modularity

Use a **feature-first, layer-separated modular monolith**. Each business feature owns its related classes, and within that feature the code is separated into `model`, `dto`, `repository`, `service`, and `controller` packages.

### 5.1 Recommended package structure

```text
com.kaushik.ChildShield/
├── ChildShieldApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── SchedulingConfig.java
│   └── WebConfig.java
├── common/
│   ├── exception/
│   ├── validation/
│   └── util/
├── auth/
├── user/
├── village/
├── guardian/
├── child/
├── vaccination/
├── maternalvisit/
├── notification/
└── dashboard/
```

### 5.2 Responsibilities by layer

- **`model`**: JPA entities and domain enums only.
- **`dto`**: form objects, validation inputs, and view models.
- **`repository`**: Spring Data JPA repositories and focused custom queries/projections.
- **`service`**: business rules, transactions, schedule generation, object-level authorization checks.
- **`controller`**: Spring MVC request mapping, form binding, validation handling, redirects, flash messages.
- **`config`**: application-wide Spring configuration, security, scheduling, web settings.
- **`common`**: cross-cutting exceptions, validation helpers, and utilities.

### 5.3 Dependency rules

1. Controllers may depend on DTOs and services, not directly on repositories.
2. Services may depend on repositories and other services through clear interfaces.
3. Repositories depend on models and persistence abstractions only.
4. DTOs must not extend JPA entities or expose password hashes/internal-only fields.
5. Thymeleaf templates receive explicit view models or carefully selected DTOs.
6. Put `@Transactional` on service methods at appropriate use-case boundaries.
7. Use constructor injection and avoid static service access.
8. Cross-feature calls should use the owning feature's service.

### 5.4 Naming and modularity rules for AI agents

- Use singular feature names and consistent class names (`ChildController`, `ChildService`, `ChildRepository`, `Child`).
- Prefer small use-case methods such as `registerChild`, `getChildDetails`, `recordAdministration`.
- Keep schedule calculation in a dedicated service within `vaccination/service`.
- Keep role and village authorization checks centralized in service-level policy/helper methods.
- Manage database schema via Hibernate JPA (`spring.jpa.hibernate.ddl-auto=update` or `create-drop`/`create`).

## 6. Database specification

Use database `vaccination_tracker`. Use BIGINT identity primary keys, DATE for calendar dates, DATETIME for timestamps, foreign keys, indexes, and appropriate unique constraints.

### 6.1 `users`
- `id`: BIGINT PK.
- `full_name`: VARCHAR(120), required.
- `username`: VARCHAR(80), required, unique.
- `password_hash`: VARCHAR(255), required.
- `role`: VARCHAR(20), required enum/string: ADMIN, HEALTH_WORKER, PARENT.
- `enabled`: BOOLEAN, default true.
- `created_at`, `updated_at`: DATETIME, required.

### 6.2 `villages`
- `id`: BIGINT PK.
- `name`: VARCHAR(120), required.
- `district`: VARCHAR(120), required.
- `state`: VARCHAR(120), required.
- `active`: BOOLEAN, default true.
- Unique constraint on `(name, district, state)`.

### 6.3 `health_worker_villages`
- `health_worker_id`: FK → users.id.
- `village_id`: FK → villages.id.
- Composite primary key `(health_worker_id, village_id)`.

### 6.4 `guardians`
- `id`: BIGINT PK.
- `user_id`: nullable unique FK → users.id.
- `full_name`: VARCHAR(120), required.
- `phone`: VARCHAR(20), required.
- `village_id`: FK → villages.id, required.
- `created_by`: FK → users.id.
- `created_at`, `updated_at`: DATETIME, required.

### 6.5 `children`
- `id`: BIGINT PK.
- `guardian_id`: FK → guardians.id, required.
- `full_name`: VARCHAR(120), required.
- `date_of_birth`: DATE, required.
- `gender`: VARCHAR(20), optional.
- `birth_weight_kg`: DECIMAL(4,2), optional.
- `village_id`: FK → villages.id, required.
- `created_by`: FK → users.id.
- `created_at`, `updated_at`: DATETIME, required.

### 6.6 `vaccines`
- `id`: BIGINT PK.
- `code`: VARCHAR(40), required unique.
- `name`: VARCHAR(120), required.
- `description`: VARCHAR(500), optional.
- `active`: BOOLEAN, default true.

### 6.7 `schedule_versions`
- `id`: BIGINT PK.
- `name`: VARCHAR(120), required.
- `version`: VARCHAR(40), required.
- `source_url`: VARCHAR(500).
- `effective_from`: DATE, required.
- `effective_to`: DATE, nullable.
- `status`: DRAFT, ACTIVE, RETIRED.

### 6.8 `schedule_doses`
- `id`: BIGINT PK.
- `schedule_version_id`: FK → schedule_versions.id.
- `vaccine_id`: FK → vaccines.id.
- `dose_code`: VARCHAR(40), required.
- `display_name`: VARCHAR(120), required.
- `age_value`: INT, required.
- `age_unit`: DAYS, WEEKS, MONTHS, YEARS.
- `region_rule`: NATIONAL or REGION_SPECIFIC.
- `region_code`: VARCHAR(40), nullable.
- `sequence_number`: INT, required.
- `active`: BOOLEAN, default true.
- Unique constraint `(schedule_version_id, dose_code)`.

### 6.9 `child_vaccinations`
- `id`: BIGINT PK.
- `child_id`: FK → children.id.
- `schedule_dose_id`: FK → schedule_doses.id.
- `due_date`: DATE, required.
- `status`: PENDING, COMPLETED, CANCELLED.
- `administered_date`: DATE, nullable.
- `recorded_by`: nullable FK → users.id.
- `notes`: VARCHAR(500), optional.
- `created_at`, `updated_at`: DATETIME, required.
- Unique constraint `(child_id, schedule_dose_id)`.

### 6.10 `maternal_visits`
- `id`: BIGINT PK.
- `guardian_id`: FK → guardians.id.
- `visit_date`: DATE, required.
- `visit_type`: VARCHAR(40), required.
- `notes`: VARCHAR(500), optional.
- `next_visit_date`: DATE, nullable.
- `recorded_by`: FK → users.id.
- `created_at`, `updated_at`: DATETIME, required.

### 6.11 `notifications`
- `id`: BIGINT PK.
- `recipient_user_id`: FK → users.id.
- `child_vaccination_id`: nullable FK → child_vaccinations.id.
- `notification_type`: UPCOMING or OVERDUE.
- `message`: VARCHAR(500), required.
- `scheduled_for`: DATETIME, required.
- `created_at`: DATETIME, required.
- `read_at`: DATETIME, nullable.
- Unique `reminder_key` constraint.

Schema management is handled via Hibernate JPA configuration (`spring.jpa.hibernate.ddl-auto=update`).

## 7. Security and authorization

### 7.1 Roles and access
- `ADMIN`: Manage all villages, workers, assignments, all records.
- `HEALTH_WORKER`: Manage assigned-village beneficiaries, vaccination records, maternal visits.
- `PARENT`: View own linked children, schedules, and notifications.

### 7.2 Mandatory security behavior
- Spring Security server-side sessions, secure password hashing, CSRF protection, HTTP-only cookies.
- Restrict `/workers/**` and village mutation routes to ADMIN.
- Enforce role and object-level authorization in services/controllers for every record identifier.
- Health workers checked against assigned villages; parents checked against guardian link.

## 8. MVC route contract

- Guardian routes (`/guardians`, `/guardians/new`, `/guardians/{id}`, etc.)
- Child routes (`/children`, `/children/new`, `/children/{id}`, etc.)
- Vaccination routes (`/vaccinations`, `/vaccinations/{id}/administer`, etc.)
- Admin routes (`/workers`, `/villages`)
- Notifications and dashboards (`/dashboard`, `/parent`, `/notifications`)

## 9. Form objects, view models and validation

Use dedicated form/DTO classes and Jakarta Bean Validation. On validation failure, re-render the form with submitted values and errors. On success, redirect with flash messages.

## 10. Vaccination schedule engine

Use Government of India National Immunization Schedule.
- Load/seed schedule versions and doses via startup data initialization or service seeding.
- When child is created, generate planned `child_vaccinations` transactionally.
- Status logic: COMPLETED, CANCELLED, OVERDUE, DUE_TODAY, UPCOMING, SCHEDULED.

## 11. Reminder scheduler

Spring Scheduling task running daily to generate upcoming/overdue in-app notifications with idempotent `reminder_key`.

## 12. Dashboard and reporting

Repository projections returning scoped counts and summary statistics.

## 13. Error handling and observability

Centralized exception handling, clean user-facing error pages without leaking stack traces or SQL details.

## 14. Testing strategy (Functional Testing)

Unit and automated integration test suites are omitted. Testing is performed via functional verification of UI flows, login/logout, role permissions, and data persistence in the running application.

## 15. Agile delivery plan

### Sprint 0 — Scaffolding & Setup
- Create Spring Boot project, database connection, Hibernate DDL configuration, basic Thymeleaf layout.

### Sprint 1 — Authentication & Users
- Implement users, roles, villages, worker assignments, Spring Security form login/logout, CSRF, admin bootstrap.

### Sprint 2 — Guardian & Child Management
- Implement guardian and child CRUD, validation, assigned-village scoping.

### Sprint 3 — Vaccination Schedule
- Seed schedule and doses, implement due-date calculation and transactional child dose generation.

### Sprint 4 — Tracking & Dashboard
- Implement administration/correction, status queries, dashboard summary.

### Sprint 5 — Maternal Visits, Parents & Notifications
- Implement maternal visits, parent-only read views, scheduled notifications.

### Sprint 6 — Integration & Release
- Functional end-to-end verification of all user workflows.

## 16. Definition of done

A task is done when business rules are implemented in services, inputs are validated server-side, authorization is correctly enforced, Hibernate DDL successfully persists entities, functional testing via UI passes, and error handling is clean and secure.
