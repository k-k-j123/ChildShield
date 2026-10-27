# ChildShield - Complete REST API & Database Schema Documentation

Rural Mother & Child Vaccination Tracker (ChildShield) provides a robust RESTful JSON API backed by Spring Boot 3.x, Spring Data JPA, and MySQL 8.4 LTS.

---

## 🌐 1. API Overview & General Conventions

- **Base URL:** `http://localhost:8080`
- **Request Content-Type:** `application/json` or `application/x-www-form-urlencoded`
- **Response Content-Type:** `application/json` (HTML pages served separately via MVC routes)
- **CORS:** Fully enabled (`*`) for cross-origin integration.
- **Interactive OpenAPI / Swagger Docs:** Available at `http://localhost:8080/swagger-ui/index.html` (JSON spec at `/v3/api-docs`).

---

## 📋 2. Detailed API Endpoints Reference

### 2.1 Authentication & Session

#### **Login**
- **Method:** `POST`
- **Endpoints:** `/login`, `/api/auth/login`
- **Description:** Authenticates user credentials (username/email and password).
- **Request Body (JSON) / Form Parameters:**
  ```json
  {
    "username": "admin",
    "password": "admin123"
  }
  ```
- **Response (200 OK):**
  ```json
  {
    "success": true,
    "user": {
      "id": 1,
      "fullName": "System Administrator",
      "username": "admin",
      "role": "ADMIN",
      "enabled": true
    }
  }
  ```
- **Error Response (401 Unauthorized):**
  ```json
  {
    "success": false,
    "message": "Invalid credentials"
  }
  ```

#### **Register User**
- **Method:** `POST`
- **Endpoints:** `/register`, `/api/auth/register`
- **Description:** Registers a new user account.
- **Request Body (JSON) / Form Parameters:**
  ```json
  {
    "fullName": "Dr. Sunita Rao",
    "username": "sunita",
    "password": "password123",
    "role": "HEALTH_WORKER"
  }
  ```
- **Response (200 OK / 201 Created):** Returns the created `User` object (excluding password hash).

#### **Logout**
- **Method:** `POST`
- **Endpoints:** `/logout`, `/api/auth/logout`
- **Description:** Terminates the current session.
- **Response (200 OK):**
  ```json
  {
    "success": true
  }
  ```

---

### 2.2 Dashboard, Reports & Profile

#### **Get Dashboard Summary**
- **Method:** `GET`
- **Endpoints:** `/dashboard`, `/api/dashboard`
- **Description:** Returns aggregate count statistics across the system.
- **Response (200 OK):**
  ```json
  {
    "totalVillages": 5,
    "totalWorkers": 12,
    "totalGuardians": 140,
    "totalChildren": 215,
    "totalDoses": 1250
  }
  ```

#### **Get Village Reports & Statistics**
- **Method:** `GET`
- **Endpoints:** `/reports`, `/api/reports`
- **Description:** Returns village details and aggregate health metrics.
- **Response (200 OK):**
  ```json
  {
    "villages": [
      {
        "id": 1,
        "name": "Rampur",
        "district": "Lucknow",
        "state": "Uttar Pradesh",
        "active": true
      }
    ],
    "beneficiariesCount": 355,
    "completedDoses": 890
  }
  ```

#### **Get Current User Profile**
- **Method:** `GET`
- **Endpoints:** `/profile`, `/api/profile`
- **Description:** Returns profile information for the currently active user.
- **Response (200 OK):** Returns `User` object JSON.

---

### 2.3 Villages Management

#### **List Active Villages**
- **Method:** `GET`
- **Endpoints:** `/api/villages`, `/villages`
- **Description:** Returns all active villages.
- **Response (200 OK):** Array of Village objects.

#### **Create Village**
- **Method:** `POST`
- **Endpoints:** `/api/villages`, `/villages`
- **Request Body / Form:**
  ```json
  {
    "name": "Rampur",
    "district": "Lucknow",
    "state": "Uttar Pradesh"
  }
  ```
- **Response (200 OK):** Created Village object.

#### **Get Village by ID**
- **Method:** `GET`
- **Endpoint:** `/api/villages/{id}`
- **Response (200 OK):** Village object.

#### **Update Village**
- **Method:** `PUT`
- **Endpoint:** `/api/villages/{id}`
- **Request Body / Form:** VillageDTO fields.
- **Response (200 OK):** Updated Village object.

#### **Delete Village**
- **Method:** `DELETE`
- **Endpoint:** `/api/villages/{id}`
- **Response (204 No Content):** Empty.

---

### 2.4 Health Workers (Admin Only)

#### **List Health Workers**
- **Method:** `GET`
- **Endpoints:** `/api/admin/health-workers`, `/workers`, `/healthcare-workers`
- **Response (200 OK):** Array of User objects with `role: "HEALTH_WORKER"`.

#### **Create Health Worker**
- **Method:** `POST`
- **Endpoints:** `/api/admin/health-workers`, `/workers`, `/healthcare-workers`
- **Request Body / Form:**
  ```json
  {
    "fullName": "Worker Name",
    "username": "worker1",
    "password": "password123",
    "villageIds": [1, 2]
  }
  ```
- **Response (200 OK):** Created User object.

#### **Get / Update / Deactivate Health Worker**
- **Methods:** `GET`, `PUT`, `DELETE`
- **Endpoints:** `/api/admin/health-workers/{id}`

---

### 2.5 Guardians & Beneficiaries (Health Worker)

#### **List Guardians**
- **Method:** `GET`
- **Endpoints:** `/api/health-worker/guardians`, `/guardians`
- **Response (200 OK):** Array of Guardian objects.

#### **Create Guardian**
- **Method:** `POST`
- **Endpoints:** `/api/health-worker/guardians`, `/guardians`
- **Request Body / Form:**
  ```json
  {
    "fullName": "Priya Devi",
    "phone": "9876543210",
    "villageId": 1
  }
  ```
- **Response (200 OK):** Created Guardian object.

---

### 2.6 Children Management

#### **List Children**
- **Method:** `GET`
- **Endpoints:** `/api/health-worker/children`, `/children`
- **Response (200 OK):** Array of Child objects.

#### **Create Child & Generate Schedule**
- **Method:** `POST`
- **Endpoints:** `/api/health-worker/children`, `/children`
- **Request Body / Form:**
  ```json
  {
    "guardianId": 1,
    "fullName": "Aarav Kumar",
    "dateOfBirth": "2026-01-15",
    "gender": "Male",
    "birthWeightKg": 3.20,
    "villageId": 1
  }
  ```
- **Response (200 OK):** Created Child object (and transactionally generates planned vaccination schedule doses).

---

### 2.7 Vaccinations Tracking

#### **List All Vaccination Records**
- **Method:** `GET`
- **Endpoints:** `/api/vaccinations`, `/vaccinations`

#### **Get Vaccinations for a Child**
- **Method:** `GET`
- **Endpoint:** `/api/vaccinations/child/{childId}`

#### **Administer Vaccine Dose**
- **Method:** `POST`
- **Endpoints:** `/api/vaccinations/{id}/administer`, `/vaccinations/{id}/administer`
- **Request Parameters / Form:** `administeredDate` (YYYY-MM-DD), `notes` (string)
- **Response (200 OK):** Updated `ChildVaccination` object with `status: "COMPLETED"`.

---

### 2.8 Maternal Visits

#### **List Maternal Visits**
- **Method:** `GET`
- **Endpoints:** `/api/maternal-visits`, `/maternal-visits`

#### **Create Maternal Visit**
- **Method:** `POST`
- **Endpoints:** `/api/maternal-visits`, `/maternal-visits`
- **Request Body / Form:**
  ```json
  {
    "guardianId": 1,
    "visitDate": "2026-10-27",
    "visitType": "ANC-1",
    "notes": "Routine checkup normal",
    "nextVisitDate": "2026-11-25"
  }
  ```

---

### 2.9 Notifications & Reminders

#### **List Notifications**
- **Method:** `GET`
- **Endpoints:** `/api/notifications`, `/notifications`

#### **Mark Notification as Read**
- **Method:** `POST`
- **Endpoints:** `/api/notifications/{id}/read`, `/notifications/{id}/read`
- **Response (204 No Content):** Empty.

---

## 🗄️ 3. Database Schema Reference

Managed via Hibernate JPA (`spring.jpa.hibernate.ddl-auto=update`) in MySQL 8.4 LTS (`vaccination_tracker` database).

### `users`
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(120) NOT NULL,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'HEALTH_WORKER', 'PARENT') NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

### `villages`
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

### `health_worker_villages`
```sql
CREATE TABLE health_worker_villages (
    health_worker_id BIGINT NOT NULL,
    village_id BIGINT NOT NULL,
    PRIMARY KEY (health_worker_id, village_id),
    FOREIGN KEY (health_worker_id) REFERENCES users(id),
    FOREIGN KEY (village_id) REFERENCES villages(id)
);
```

### `guardians`
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

### `children`
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

### `vaccines` & `schedule_versions` & `schedule_doses`
```sql
CREATE TABLE vaccines (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE schedule_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    version VARCHAR(40) NOT NULL,
    source_url VARCHAR(500),
    effective_from DATE NOT NULL,
    effective_to DATE,
    status ENUM('DRAFT', 'ACTIVE', 'RETIRED') NOT NULL
);

CREATE TABLE schedule_doses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    schedule_version_id BIGINT NOT NULL,
    vaccine_id BIGINT NOT NULL,
    dose_code VARCHAR(40) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    age_value INT NOT NULL,
    age_unit ENUM('DAYS', 'WEEKS', 'MONTHS', 'YEARS') NOT NULL,
    region_rule ENUM('NATIONAL', 'REGION_SPECIFIC') NOT NULL,
    region_code VARCHAR(40),
    sequence_number INT NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    UNIQUE KEY uk_schedule_dose (schedule_version_id, dose_code),
    FOREIGN KEY (schedule_version_id) REFERENCES schedule_versions(id),
    FOREIGN KEY (vaccine_id) REFERENCES vaccines(id)
);
```

### `child_vaccinations`
```sql
CREATE TABLE child_vaccinations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    child_id BIGINT NOT NULL,
    schedule_dose_id BIGINT NOT NULL,
    due_date DATE NOT NULL,
    status ENUM('PENDING', 'COMPLETED', 'CANCELLED', 'DUE_TODAY', 'OVERDUE', 'UPCOMING') NOT NULL,
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

### `maternal_visits`
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

### `notifications`
```sql
CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipient_user_id BIGINT NOT NULL,
    child_vaccination_id BIGINT,
    notification_type ENUM('UPCOMING', 'OVERDUE') NOT NULL,
    message VARCHAR(500) NOT NULL,
    scheduled_for DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    read_at DATETIME,
    UNIQUE KEY uk_reminder_key (recipient_user_id, child_vaccination_id, notification_type, scheduled_for),
    FOREIGN KEY (recipient_user_id) REFERENCES users(id),
    FOREIGN KEY (child_vaccination_id) REFERENCES child_vaccinations(id)
);
```
