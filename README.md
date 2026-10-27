# ChildShield - Rural Mother & Child Vaccination Tracker

A Spring Boot 3.x REST API backend with static HTML/JS frontend for tracking maternal and child vaccinations in rural communities.

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Maven 3.9+
- MySQL 8.4 LTS running on localhost:3306 (database: `vaccination_tracker`)

### Run Application
```bash
# Build
./mvnw clean compile

# Run (starts on http://localhost:8080)
./mvnw spring-boot:run
```

### Run API Integration Tests
```bash
# Full test suite (builds, starts app, runs tests, stops app)
./run_tests.sh

# Or just run tests against already-running app
python3 test_apis.py
```

## 🏗️ Architecture

```
ChildShield/
├── Backend (REST API)
│   ├── Spring Boot 3.x + Java 21
│   ├── Spring Data JPA + Hibernate
│   ├── MySQL 8.4 LTS
│   └── Spring Security (permissive for prototype)
├── Frontend (Static)
│   ├── HTML5 + CSS3 + Vanilla JS (ES6+)
│   ├── Served from src/main/resources/static/
│   └── Communicates via REST API (api-contract.js)
└── Database
    └── Hibernate DDL auto-update (no Flyway)
```

## 🔐 Security (Prototype Mode)

> **⚠️ Current Configuration: Fully Permissive**
> - CORS: `*` (all origins, methods, headers)
> - CSRF: Disabled
> - Authorization: All endpoints public (`/**`)
> - Authentication: Stateless REST (no sessions required)
> - Password Hashing: BCrypt

**For Production**: Re-enable role-based authorization, CSRF protection, and restrict CORS origins in `SecurityConfig.java`.

## 👥 Default Credentials

| Role | Username | Password |
|------|----------|----------|
| Admin | `admin` | `admin123` |

## 📋 REST API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/login` | Login with username/password |
| `POST` | `/register` | Register new user |
| `POST` | `/logout` | Logout |

### Dashboard & Reports
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/dashboard` | Dashboard summary counts |
| `GET` | `/reports` | Village reports and statistics |
| `GET` | `/profile` | Current user profile |

### Villages
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/villages` | List all active villages |
| `POST` | `/api/villages` | Create new village |
| `GET` | `/api/villages/{id}` | Get village by ID |
| `PUT` | `/api/villages/{id}` | Update village |
| `DELETE` | `/api/villages/{id}` | Delete village |

### Health Workers (Admin)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/admin/health-workers` | List all health workers |
| `POST` | `/api/admin/health-workers` | Create health worker |
| `GET` | `/api/admin/health-workers/{id}` | Get health worker by ID |
| `PUT` | `/api/admin/health-workers/{id}` | Update health worker |
| `DELETE` | `/api/admin/health-workers/{id}` | Deactivate health worker |

### Guardians (Health Worker)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/health-worker/guardians` | List guardians |
| `POST` | `/api/health-worker/guardians` | Create guardian |
| `GET` | `/api/health-worker/guardians/{id}` | Get guardian by ID |
| `PUT` | `/api/health-worker/guardians/{id}` | Update guardian |
| `DELETE` | `/api/health-worker/guardians/{id}` | Delete guardian |

### Children (Health Worker)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/health-worker/children` | List children |
| `POST` | `/api/health-worker/children` | Create child |
| `GET` | `/api/health-worker/children/{id}` | Get child by ID |
| `PUT` | `/api/health-worker/children/{id}` | Update child |
| `DELETE` | `/api/health-worker/children/{id}` | Delete child |

### Vaccinations
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/vaccinations` | List all vaccination records |
| `GET` | `/api/vaccinations/child/{childId}` | Get vaccinations for a child |
| `POST` | `/api/vaccinations/{id}/administer` | Administer a vaccine dose |

### Maternal Visits
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/maternal-visits` | List all maternal visits |
| `POST` | `/api/maternal-visits` | Create maternal visit |

### Notifications
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/notifications` | List all notifications |
| `POST` | `/api/notifications/{id}/read` | Mark notification as read |

## 🗄️ Database Schema

### users
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(120) NOT NULL,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL, -- ADMIN, HEALTH_WORKER, PARENT
    enabled BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

### villages
```sql
CREATE TABLE villages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    district VARCHAR(120) NOT NULL,
    state VARCHAR(120) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    UNIQUE KEY uk_village (name, district, state)
);
```

### health_worker_villages
```sql
CREATE TABLE health_worker_villages (
    health_worker_id BIGINT NOT NULL,
    village_id BIGINT NOT NULL,
    PRIMARY KEY (health_worker_id, village_id),
    FOREIGN KEY (health_worker_id) REFERENCES users(id),
    FOREIGN KEY (village_id) REFERENCES villages(id)
);
```

### guardians
```sql
CREATE TABLE guardians (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNIQUE,
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    village_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (village_id) REFERENCES villages(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);
```

### children
```sql
CREATE TABLE children (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    guardian_id BIGINT NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20),
    birth_weight_kg DECIMAL(4,2),
    village_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (guardian_id) REFERENCES guardians(id),
    FOREIGN KEY (village_id) REFERENCES villages(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);
```

### vaccines
```sql
CREATE TABLE vaccines (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN DEFAULT TRUE
);
```

### schedule_versions
```sql
CREATE TABLE schedule_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    version VARCHAR(40) NOT NULL,
    source_url VARCHAR(500),
    effective_from DATE NOT NULL,
    effective_to DATE,
    status VARCHAR(20) NOT NULL -- DRAFT, ACTIVE, RETIRED
);
```

### schedule_doses
```sql
CREATE TABLE schedule_doses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    schedule_version_id BIGINT NOT NULL,
    vaccine_id BIGINT NOT NULL,
    dose_code VARCHAR(40) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    age_value INT NOT NULL,
    age_unit VARCHAR(20) NOT NULL, -- DAYS, WEEKS, MONTHS, YEARS
    region_rule VARCHAR(20) NOT NULL, -- NATIONAL, REGION_SPECIFIC
    region_code VARCHAR(40),
    sequence_number INT NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    UNIQUE KEY uk_schedule_dose (schedule_version_id, dose_code),
    FOREIGN KEY (schedule_version_id) REFERENCES schedule_versions(id),
    FOREIGN KEY (vaccine_id) REFERENCES vaccines(id)
);
```

### child_vaccinations
```sql
CREATE TABLE child_vaccinations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    child_id BIGINT NOT NULL,
    schedule_dose_id BIGINT NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, COMPLETED, CANCELLED
    administered_date DATE,
    recorded_by BIGINT,
    notes VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_child_vaccination (child_id, schedule_dose_id),
    FOREIGN KEY (child_id) REFERENCES children(id),
    FOREIGN KEY (schedule_dose_id) REFERENCES schedule_doses(id),
    FOREIGN KEY (recorded_by) REFERENCES users(id)
);
```

### maternal_visits
```sql
CREATE TABLE maternal_visits (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    guardian_id BIGINT NOT NULL,
    visit_date DATE NOT NULL,
    visit_type VARCHAR(40) NOT NULL,
    notes VARCHAR(500),
    next_visit_date DATE,
    recorded_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (guardian_id) REFERENCES guardians(id),
    FOREIGN KEY (recorded_by) REFERENCES users(id)
);
```

### notifications
```sql
CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipient_user_id BIGINT NOT NULL,
    child_vaccination_id BIGINT,
    notification_type VARCHAR(20) NOT NULL, -- UPCOMING, OVERDUE
    message VARCHAR(500) NOT NULL,
    scheduled_for DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    read_at DATETIME,
    UNIQUE KEY uk_reminder_key (recipient_user_id, child_vaccination_id, notification_type, scheduled_for),
    FOREIGN KEY (recipient_user_id) REFERENCES users(id),
    FOREIGN KEY (child_vaccination_id) REFERENCES child_vaccinations(id)
);
```

## 🧪 Testing

### Manual API Testing
```bash
# Login
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Get Dashboard
curl http://localhost:8080/dashboard

# List Villages
curl http://localhost:8080/api/villages

# Create Village
curl -X POST http://localhost:8080/api/villages \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Village","district":"Test District","state":"Test State"}'
```

### Automated Integration Tests
```bash
./run_tests.sh
```

Test output example:
```
=== Starting ChildShield REST API Integration Tests ===
[PASS] Admin Login (/login) -> Status 200
[PASS] Get Dashboard (/dashboard) -> Status 200
[PASS] List Villages (/api/villages) -> Status 200
[PASS] List Health Workers (/api/admin/health-workers) -> Status 200
[PASS] List Guardians (/api/health-worker/guardians) -> Status 200
[PASS] List Children (/api/health-worker/children) -> Status 200
[PASS] List Vaccinations (/api/vaccinations) -> Status 200
[PASS] List Maternal Visits (/api/maternal-visits) -> Status 200
[PASS] List Notifications (/api/notifications) -> Status 200
[PASS] Get Reports (/reports) -> Status 200
[PASS] Get Profile (/profile) -> Status 200

=== Test Report Summary ===
Total: 11 | Passed: 11 | Failed: 0
```

## 📚 Documentation & API Docs

- [Backend PRD & Task List](backend_prd_tasklist.md)
- [Frontend PRD & Task List](frontend_prd_tasklist.md)
- [API Contract](src/main/resources/static/api-contract.js)
- **Swagger / OpenAPI Documentation**: Available at `http://localhost:8080/swagger-ui/index.html` (JSON spec at `/v3/api-docs`) when the application is running.

## 🛠️ Development

### Project Structure
```
src/main/java/com/kaushik/ChildShield/
├── config/          # Security, Scheduling, Web config
├── common/          # Exceptions, utilities
├── user/            # User auth, roles, workers
├── village/         # Village management
├── guardian/        # Guardian/beneficiary management
├── child/           # Child management + vaccination schedule
├── vaccination/     # Vaccination tracking
├── maternalvisit/   # Maternal health visits
├── notification/    # In-app notifications
└── dashboard/       # Dashboard & reports API
```

### Key Configuration Files
- `src/main/resources/application.properties` - Database & JPA config
- `src/main/java/.../config/SecurityConfig.java` - CORS, CSRF, Auth config
- `src/main/java/.../config/DataInitializer.java` - Admin user + schedule seeding

## 📦 Build Artifact

```bash
./mvnw clean package
# Output: target/ChildShield-0.0.1-SNAPSHOT.jar
```

## 📄 License

Internal prototype - Rural Mother & Child Vaccination Tracker (ChildShield)