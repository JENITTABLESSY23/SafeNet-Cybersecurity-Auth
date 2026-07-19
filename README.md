# 🏥 SafeNet — Clinical Access Control & Authentication System

> **VSB Engineering College, Karur — 2028/2029 Batch**
> 2nd Year & 3rd Year · Project Implementation Phase

A full-stack hospital information security system featuring **Role-Based Access Control (RBAC)**, **IoT anomaly detection using Isolation Forest**, and a multi-department clinical portal.

---

## 📋 Project Overview

SafeNet is a clinical access management platform designed for hospital environments, demonstrating the access-control patterns (RBAC, audit logging, break-the-glass override) that regulations like HIPAA require — without claiming certified compliance it hasn't been audited for. It provides department-wise authenticated dashboards, IoT anomaly detection backed by a trained ML model, and an admin console for staff approvals and access governance.

---

## 🗂️ Repository Structure

```
safenet/
│
├── frontend/                       # Static HTML/CSS/JS frontend
│   ├── js/api.js                   # Shared API client (auth, fetch wrapper, session handling)
│   ├── login.html                  # Login — authenticates against the real backend
│   ├── forgot-password.html        # Request a password reset link by email
│   ├── reset-password.html         # Set a new password via the emailed token
│   ├── register.html               # 4-step staff credential registration wizard
│   ├── logout.html                 # Real sign-out (invalidates token server-side)
│   ├── patients.html               # Patient Records — full CRUD against the backend
│   ├── settings.html               # Profile, security, notification preferences
│   ├── help.html                   # FAQ, support tickets, system status
│   ├── dashboard_icu.html          # ICU vitals (polls backend), Break-the-Glass, orders
│   ├── dashboard_cardio.html       # Cardiology ECG, patient matrix, implant telemetry
│   ├── dashboard_gynecology.html   # Maternal & gynecological ward monitoring
│   └── admin.html                  # Admin: approvals, anomaly log, RBAC table
│
├── backend/                        # Spring Boot REST API
│   ├── src/
│   │   ├── main/java/com/safenet/
│   │   │   ├── controller/         # REST controllers (Auth, Patient, Admin, IoT)
│   │   │   ├── service/            # Business logic layer
│   │   │   ├── repository/         # Spring Data JPA repositories
│   │   │   ├── entity/             # JPA entity classes (User, Patient, AuditLog…)
│   │   │   ├── security/           # Spring Security + JWT config
│   │   │   └── SafeNetApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── pom.xml
│
├── ml/                              # Anomaly-detection model + serving API
│   ├── safenet_model.py             # Trains XGBoost + Isolation Forest on CICIoT2023
│   ├── ml_api.py                    # Flask API the backend calls for live detection
│   ├── CICIoT2023/                  # Dataset CSVs go here (not checked in)
│   └── outputs/                     # Trained model artifacts land here
│
├── database/
│   ├── schema.sql                  # MySQL schema (production reference)
│   ├── schema_sqlite.sql           # Same schema, SQLite-compatible — use this for local dev
│   └── seed_data.sql               # Sample data for development
│
├── docs/
│   ├── SafeNet_Project_Report.docx
│   └── SafeNet_Abstract_Benefits_FutureScope.docx
│
└── README.md
```

---

## ✅ Deliverable Checklist (per submission schedule)

| Date | Activity | Deliverable | Status |
|------|----------|-------------|--------|
| Jun 01 | Project Title Finalization | Approved Project Title | ✅ |
| Jun 02 | Requirement Gathering | Problem Statement | ✅ |
| Jun 03 | Objective Definition | Project Objectives | ✅ |
| Jun 04 | User & Module Identification | Module List | ✅ |
| Jun 05 | Use Case Diagram | `docs/use_case_diagram.png` | ✅ |
| Jun 06 | Database Requirement Analysis | Table List | ✅ |
| Jun 07 | ER Diagram Design | `database/er_diagram.png` | ✅ |
| Jun 08 | Database Schema | `database/schema.sql` | ✅ |
| Jun 09 | UI Wireframe Design | `docs/wireframes/` | ✅ |
| Jun 10 | Login & Dashboard UI | `login.html`, dashboards | ✅ |
| Jun 11 | Navigation & Form Design | All pages linked | ✅ |
| Jun 12 | Design Review | Design Approved | ✅ |
| Jun 13 | Frontend Environment Setup | React / static project setup | ✅ |
| Jun 14 | Login Page Development | `login.html` | ✅ |
| Jun 15 | Registration Page Development | `register.html` | ✅ |
| Jun 16 | Dashboard Development | `dashboard_icu.html`, `dashboard_cardio.html` | ✅ |
| Jun 17 | CRUD Form Development | `patients.html` (Add/Edit/Delete) | ✅ |
| Jun 18 | Table & Search Features | Patient table with filter/search | ✅ |
| Jun 19 | Frontend Testing | Frontend Review | ✅ |
| Jun 20 | Spring Boot Project Setup | `backend/` scaffold | ✅ |
| Jun 21 | Database Connectivity | DB Connection | ✅ |
| Jun 22 | Entity & Repository Creation | Entity Classes | ✅ |
| Jun 23 | REST API Development | CRUD APIs | ✅ |
| Jun 24 | Authentication Module | Login API + JWT | ✅ |
| Jun 25 | Backend Business Logic | Service Layer | ✅ |
| Jun 26 | API Testing | Manual + scripted verification | ✅ |
| Jun 27 | Frontend–Backend Integration | Working Application | ✅ |
| Jun 28 | Bug Fixing & Validation | Tested Project | ✅ |
| Jun 29 | Documentation & PPT | `docs/SafeNet_Project_Report.docx` | ✅ |
| Jun 30 | Final Demo & Submission | **Final Project Submission** | ⬜ |

---

## 🖥️ Pages & Modules

### Authentication
| Page | File | Description |
|------|------|-------------|
| Login | `login.html` | Username + Hospital ID + password against the real backend; routes by the account's actual department |
| Forgot Password | `forgot-password.html` | Requests an emailed reset link; same response whether or not the email exists |
| Reset Password | `reset-password.html` | Sets a new password via the token from the emailed link (expires in 30 min) |
| Register | `register.html` | 4-step wizard: Identity → Department → ID Upload → Review |
| Logout | `logout.html` | Session-end audit summary with auto-redirect countdown |

### Dashboards
| Page | File | Description |
|------|------|-------------|
| ICU Command | `dashboard_icu.html` | Live vitals (Beds 04 & 07), urgent orders, Break-the-Glass modal, patient health analytics, downloadable patient report |
| Cardiology | `dashboard_cardio.html` | Animated ECG canvas, BP trends, patient matrix, implant telemetry, patient health analytics, downloadable patient report |
| Gynecology & Maternity | `dashboard_gynecology.html` | Live vitals (Beds 12 & 15), urgent orders, patient health analytics, downloadable patient report |
| Admin Control | `admin.html` | Staff approvals, IoT anomaly log, active RBAC access table, hospital-wide patient health analytics |

### Core Portals
| Page | File | Description |
|------|------|-------------|
| Patient Records | `patients.html` | Full CRUD — Add, View (drawer), Edit, Delete, Search & Filter, downloadable patient report (CSV / printable PDF) |
| Settings | `settings.html` | Profile, password strength, 2FA, sessions, notification prefs |
| Help | `help.html` | Searchable FAQ accordion, system status, support ticket form |

### Patient health analytics
Every dashboard has a **Patient Health Overview** panel (`js/patient-analytics.js`) — a live donut chart + legend breaking the patient list down by clinical status:
- **Normal** (`Stable`)
- **Under Supervision** (`Watch`)
- **Critical** (`Critical`)

Like the download feature, this reads from `GET /api/patients`, so a department dashboard only ever charts its own patients; the admin console is the one place that shows a hospital-wide breakdown, since `Admin_Ops` is the unrestricted role. The chart updates on page load — it isn't real-time push, so a change made in one tab won't animate into another tab's chart until it's reloaded.

### Downloadable patient reports
Every dashboard's patient list, and the Patient Records page, has a **Download Report** button (`js/patient-report.js`) with two formats:
- **CSV** — every visible field, opens straight in Excel/Sheets
- **Printable report** — opens a formatted report in a new tab; use the browser's Print → Save as PDF to export it

Both are generated entirely client-side from whatever `GET /api/patients` already returned — since that endpoint is department-scoped server-side (see Security Features below), the export can never contain more than the account is already authorized to see. Nothing is rendered or stored server-side for this feature.

---

## 🔐 Security Features

**Implemented:**
- **RBAC** — role-based routing and endpoint authorization (`Doc_ICU`, `Doc_Cardio`, `Doc_Gyno`, `Admin_Ops`); `/api/admin/**` is restricted server-side to `Admin_Ops`
- **Department-scoped patient access** — `/api/patients/**` is filtered server-side by the caller's department (`PatientController.callerDepartment()`, derived from their role): a Cardiology account can only read, create, edit, or delete Cardiology patients, and the same for ICU/Gynecology. `Admin_Ops` is the only role with cross-department visibility. This is enforced in the controller, not just hidden in the UI, so it holds regardless of how the request is made
- **JWT authentication** — stateless auth with a server-side blacklist on logout
- **Password hashing** — BCrypt, never stored or returned in plaintext
- **Forgot password** — email-based reset flow: a time-limited (30 min), single-use token is emailed to the account's address; the endpoint gives the same response whether or not the email exists, to avoid account enumeration
- **Break-the-Glass (BTG)** — emergency access override with mandatory reason, logged to the audit trail, deliberately *not* restricted to admins since it exists for clinical staff to bypass RBAC in an emergency
- **Audit trail** — logins, BTG events, and admin actions are logged with actor attribution
- **Brute-force lockout** — accounts lock temporarily after repeated failed logins

**Planned, not yet built** — listed here honestly rather than claimed as done: two-factor authentication, at-rest encryption, and a real TLS/HTTPS setup (current setup is plain HTTP for local development). Any claim of formal compliance (HIPAA, ISO 27001, etc.) would require an actual audit and shouldn't be asserted without one — this project demonstrates the access-control *patterns* those frameworks require, not certified compliance.

---

## 🤖 Anomaly Detection (Research Component)

A hybrid **XGBoost + Isolation Forest** model trained on the CICIoT2023 IoT intrusion dataset, served via a small Flask API that the Spring Boot backend calls for live detection.

| | |
|--|--|
| Algorithm | XGBoost (supervised) + Isolation Forest (unsupervised), fused | 
| Alert types | `WARNING` (sensor drift / device malfunction) · `CRITICAL` (privilege escalation / insider threat) |
| Data source | CICIoT2023 (real dataset) for training; synthetic per-bed telemetry at runtime (no physical sensors attached yet) |

Run `python ml/safenet_model.py` after placing the dataset CSVs in `ml/CICIoT2023/` — it prints real accuracy/precision/recall/F1 for each model to the terminal and saves confusion-matrix and SHAP feature-importance plots to `ml/outputs/`. There are no fixed benchmark numbers here on purpose — they depend on your actual training run, not a claim made in advance of one.

---

## 🗃️ Database Tables

| Table | Purpose |
|-------|---------|
| `users` | Staff credentials, roles, department, approval status |
| `patients` | Patient records — demographics, diagnosis, bed, status |
| `audit_log` | Log of access events and BTG overrides |
| `iot_anomalies` | Flagged anomaly records with classification and timestamp |
| `orders` | Clinical orders (urgent queue per ward) |
| `departments`, `sessions` | Defined in the schema for a future iteration — not yet wired to any entity/repository. JWT invalidation currently uses an in-memory blacklist instead of the `sessions` table. |

---

## ⚙️ Tech Stack

### Frontend
- HTML5 · CSS3 · Vanilla JavaScript
- Google Fonts: **Inter** + **JetBrains Mono**
- Canvas API (ECG animation)
- No external UI library dependency

### Backend
- **Java 17** + **Spring Boot 3.x**
- **Spring Security** + **JWT** for stateless auth
- **Spring Data JPA** + **Hibernate**
- **SQLite** (dev) / **MySQL** (prod)

### ML
- **XGBoost** + **Isolation Forest** (scikit-learn) + **SHAP** for explainability
- **Flask** serving API, called by the Spring Boot backend

---

## 🚀 Running the Project

### 1. Database setup
Run this from inside `backend/` — `application.properties` points at
`jdbc:sqlite:safenet.db`, a relative path, so the database file has to
sit right next to wherever you run `mvn spring-boot:run` from.

```bash
cd backend
sqlite3 safenet.db < ../database/schema_sqlite.sql
sqlite3 safenet.db < ../database/seed_data.sql
# seeded accounts all use password: SafeNet@123
```

`sqlite3` isn't a built-in command on Windows (it is on macOS/Linux). If
you get "not recognized," use Python instead — it ships with `sqlite3`
as a built-in module, no separate install needed:
```powershell
cd backend
python -c "import sqlite3; c = sqlite3.connect('safenet.db'); c.executescript(open('../database/schema_sqlite.sql').read()); c.executescript(open('../database/seed_data.sql').read()); c.commit(); print('Seeded successfully')"
```

Use `schema_sqlite.sql`, not `schema.sql` — the latter uses MySQL's
`AUTO_INCREMENT` syntax, which SQLite doesn't understand and will fail
with a syntax error. `schema.sql` is kept as the reference for an actual
MySQL production deployment; `schema_sqlite.sql` is the same schema
translated for local SQLite dev.

### 2. Backend
Before starting, set real SMTP credentials in `application.properties` if
you want "Forgot Password" emails to actually send — see the comments
above `spring.mail.*` there for how to get a Gmail app password. Without
this configured, the reset flow still runs (token generated, saved,
logged) but `mailSender.send()` will throw, so the email itself won't
arrive.
```bash
cd backend
mvn spring-boot:run
# API available at http://localhost:8080
```

### 3. Frontend
The frontend now makes real API calls, so the backend must already be running.
```bash
cd frontend
open login.html
# or serve statically: python -m http.server 5500
```
Opening the file directly works because `application.properties` allows the
`null` origin browsers send for `file://` pages — convenient for local
testing, but replace it with your real origin before deploying anywhere.

---

## 👥 Author

> VSB Engineering College, Karur
> B.Tech, Artificial Intelligence and Data Science

Developed as an individual project. *(Add your name here.)*

---

## 📄 License

This project is submitted as an academic deliverable to VSB Engineering College, Karur under the 2028/2029 batch project implementation schedule. All rights reserved.
