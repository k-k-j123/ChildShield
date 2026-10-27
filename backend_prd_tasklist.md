# Rural Mother & Child Vaccination Tracker (ChildShield)

## Backend PRD and AI-Agent Task List

**Document version:** 2.0 (Updated: REST API + JSON architecture, global CORS, Thymeleaf removed)\
**Status:** Implementation complete - REST API backend ready\
**Backend stack:** Java 21, Spring Boot 3.x, Spring Data JPA, MySQL 8.4 LTS\
**API Style:** REST + JSON\
**Audience:** Backend developer or autonomous AI coding agent

------------------------------------------------------------------------

## 1. Purpose

Build the **backend REST API** for a local-first Rural Mother & Child Vaccination Tracker (ChildShield). The backend provides JSON endpoints for authentication, authorization, persistence, data validation, vaccination scheduling, dashboard queries, maternal visit records, and simulated reminder generation.

The frontend is served as static HTML/JS from `src/main/resources/static/` and communicates with the backend via REST API calls (see `api-contract.js`).

Three roles:
- `ADMIN`: manages all villages, workers, assignments and records.
- `HEALTH_WORKER`: manages beneficiaries and vaccination records in assigned villages.
- `PARENT`: views only their linked children, schedules, and notifications.

## 2. Product goals

1. Securely manage users and role-based access via REST endpoints.
2. Scope worker access to assigned villages on every request.
3. Register guardians and children and generate planned vaccine-dose records.
4. Track scheduled, completed, due-today, upcoming, overdue, and cancelled doses.
5. Support basic vaccination record corrections.
6. Provide dashboard summary and village reports via JSON.
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
- Server-side rendered Thymeleaf templates (served as static HTML now).
- Automatic marking of a vaccine as administered.
- Full immutable audit history in the initial version; basic created/updated metadata is sufficient.
- Automated unit or integration testing suites (testing will be handled functionally).

## 4. Technical baseline

- Java 21 LTS.
- Spring Boot 3.x using a compatible current maintenance release.
- Maven.
- Spring Data JPA and Hibernate (with ddl-auto configuration for schema management).
- MySQL 8.4 LTS, InnoDB.
- Jakarta Bean Validation.
- BCrypt password encoding or another approved Spring Security password encoder.
- Spring Scheduling for reminder generation.
- **REST Controllers returning JSON** (`@RestController`).
- **Global CORS enabled** (`*`) for frontend integration.
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
- **`dto`**: form objects, validation inputs, and API request/response models.
- **`repository`**: Spring Data JPA repositories and focused custom queries/projections.
- **`service`**: business rules, transactions, schedule generation, object-level authorization checks.
- **`controller`**: **REST endpoints** (`@RestController`), JSON request/response binding, validation handling, HTTP status codes.
- **`config`**: application-wide Spring configuration, security (CORS + permissive), scheduling, web settings.
- **`common`**: cross-cutting exceptions, validation helpers, and utilities.

### 5.3 Dependency rules

1. Controllers may depend on DTOs and services, not directly on repositories.
2. Services may depend on repositories and other services through clear interfaces.
3. Repositories depend on models and persistence abstractions only.
4. DTOs must not extend JPA entities or expose password hashes/internal-only fields.
5. JSON responses receive explicit DTOs or carefully selected entity projections.
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

### 7.2 Security behavior (Current)
- **CORS**: Globally open (`*`) - all origins, all methods, all headers.
- **CSRF**: Disabled (prototype mode, stateless REST API).
- **Authorization**: Permissive (`/**`) - all endpoints publicly accessible for API testing.
- **Authentication**: Stateless - no session cookies required for API calls.
- **Password Hashing**: BCrypt via Spring Security.

> **Note**: For production, re-enable role-based authorization, CSRF protection, and restrict CORS origins.

## 8. REST API route contract

### 8.1 Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login`, `/login` | Login with username/password |
| POST | `/api/auth/register`, `/register` | Register new user |
| POST | `/api/auth/logout`, `/logout` | Logout |

### 8.2 Dashboard & Reports
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/dashboard`, `/dashboard` | Dashboard summary counts |
| GET | `/api/reports`, `/reports` | Village reports and statistics |
| GET | `/api/profile`, `/profile` | Current user profile |

### 8.3 Villages
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/villages`, `/villages` | List all active villages |
| POST | `/api/villages`, `/villages` | Create new village |
| GET | `/api/villages/{id}` | Get village by ID |
| PUT | `/api/villages/{id}` | Update village |
| DELETE | `/api/villages/{id}` | Delete village |

### 8.4 Health Workers (Admin only)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/health-workers`, `/workers`, `/healthcare-workers` | List all health workers |
| POST | `/api/admin/health-workers`, `/workers`, `/healthcare-workers` | Create health worker |
| GET | `/api/admin/health-workers/{id}` | Get health worker by ID |
| PUT | `/api/admin/health-workers/{id}` | Update health worker |
| DELETE | `/api/admin/health-workers/{id}` | Deactivate health worker |

### 8.5 Guardians (Health Worker)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health-worker/guardians`, `/guardians` | List guardians |
| POST | `/api/health-worker/guardians`, `/guardians` | Create guardian |
| GET | `/api/health-worker/guardians/{id}` | Get guardian by ID |
| PUT | `/api/health-worker/guardians/{id}` | Update guardian |
| DELETE | `/api/health-worker/guardians/{id}` | Delete guardian |

### 8.6 Children (Health Worker)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health-worker/children`, `/children` | List children |
| POST | `/api/health-worker/children`, `/children` | Create child |
| GET | `/api/health-worker/children/{id}` | Get child by ID |
| PUT | `/api/health-worker/children/{id}` | Update child |
| DELETE | `/api/health-worker/children/{id}` | Delete child |

### 8.7 Vaccinations
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/vaccinations`, `/vaccinations` | List all vaccination records |
| GET | `/api/vaccinations/child/{childId}` | Get vaccinations for a child |
| POST | `/api/vaccinations/{id}/administer`, `/vaccinations/{id}/administer` | Administer a vaccine dose |

### 8.8 Maternal Visits
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/maternal-visits`, `/maternal-visits` | List all maternal visits |
| POST | `/api/maternal-visits`, `/maternal-visits` | Create maternal visit |

### 8.9 Notifications
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/notifications`, `/notifications` | List all notifications |
| POST | `/api/notifications/{id}/read`, `/notifications/{id}/read` | Mark notification as read |

## 9. Form objects, DTOs and validation

Use dedicated form/DTO classes and Jakarta Bean Validation. On validation failure, return 400 with JSON error details. On success, return 200/201 with created/updated entity.

## 10. Vaccination schedule engine

Use Government of India National Immunization Schedule.
- Load/seed schedule versions and doses via startup data initialization or service seeding.
- When child is created, generate planned `child_vaccinations` transactionally.
- Status logic: COMPLETED, CANCELLED, OVERDUE, DUE_TODAY, UPCOMING, SCHEDULED.

## 11. Reminder scheduler

Spring Scheduling task running daily to generate upcoming/overdue in-app notifications with idempotent `reminder_key`.

## 12. Dashboard and reporting

Repository projections returning scoped counts and summary statistics via JSON endpoints.

## 13. Error handling and observability & API Documentation

Centralized exception handling returning JSON error responses without leaking stack traces or SQL details in production. OpenAPI 3.0 (Swagger UI) integration via `springdoc-openapi` available at `/swagger-ui/index.html`.

## 14. Testing strategy (Functional Testing)

Unit and automated integration test suites are omitted. Testing is performed via functional verification of API endpoints using the provided `test_apis.py` and `run_tests.sh` scripts.

## 15. Agile delivery plan

### Sprint 0 — Scaffolding & Setup
- Create Spring Boot project, database connection, Hibernate DDL configuration, REST controllers.

### Sprint 1 — Authentication & Users
- Implement users, roles, villages, worker assignments, REST auth endpoints, global CORS.

### Sprint 2 — Guardian & Child Management
- Implement guardian and child CRUD REST endpoints, assigned-village scoping.

### Sprint 3 — Vaccination Schedule
- Seed schedule and doses, implement due-date calculation and transactional child dose generation.

### Sprint 4 — Tracking & Dashboard
- Implement administration/correction, status queries, dashboard summary JSON.

### Sprint 5 — Maternal Visits, Parents & Notifications
- Implement maternal visits, parent read-only views, scheduled notifications.

### Sprint 6 — Integration & Release
- Functional end-to-end verification of all API workflows using test scripts.

## 16. Definition of done

A task is done when business rules are implemented in services, inputs are validated server-side, authorization is correctly enforced (when enabled), Hibernate DDL successfully persists entities, functional testing via API test scripts passes, and error handling is clean and secure.