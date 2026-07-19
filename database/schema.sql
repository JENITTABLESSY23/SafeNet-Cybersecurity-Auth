-- ============================================================
-- SafeNet Database Schema
-- Compatible: SQLite (dev) / MySQL 8.x (prod)
-- ============================================================

-- ── Users (Staff) ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100) NOT NULL,
    username          VARCHAR(100) NOT NULL UNIQUE,
    hospital_id       VARCHAR(20)  NOT NULL UNIQUE,
    password          VARCHAR(255) NOT NULL,
    email             VARCHAR(150) NOT NULL UNIQUE,
    phone             VARCHAR(20)  NOT NULL,
    designation       VARCHAR(100),
    department        VARCHAR(50)  NOT NULL,
    role              VARCHAR(50)  NOT NULL,
    approval_status   VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    id_proof_path     VARCHAR(255),
    is_active         BOOLEAN      NOT NULL DEFAULT FALSE,
    last_login        DATETIME,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── Patients ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS patients (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    patient_id        VARCHAR(20)  NOT NULL UNIQUE,
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100) NOT NULL,
    age               INT          NOT NULL,
    gender            VARCHAR(10)  NOT NULL,
    department        VARCHAR(50)  NOT NULL,
    bed               VARCHAR(10)  NOT NULL,
    diagnosis         VARCHAR(255) NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'Stable',
    contact           VARCHAR(20),
    notes             TEXT,
    admitted_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── Departments ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS departments (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    code              VARCHAR(20)  NOT NULL UNIQUE,
    name              VARCHAR(100) NOT NULL,
    default_role      VARCHAR(50)  NOT NULL,
    head_user_id      BIGINT,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ── Audit Log (immutable) ────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_log (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id           BIGINT,
    action            VARCHAR(50)  NOT NULL,
    resource_type     VARCHAR(50),
    resource_id       VARCHAR(50),
    details           TEXT,
    ip_address        VARCHAR(45),
    node_id           VARCHAR(30),
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── Support Tickets ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS support_tickets (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    category          VARCHAR(50)  NOT NULL,
    subject           VARCHAR(255) NOT NULL,
    details           TEXT         NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ── Password Reset Tokens ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    token             VARCHAR(64)  NOT NULL UNIQUE,
    expires_at        DATETIME     NOT NULL,
    used              BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ── IoT Anomalies ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS iot_anomalies (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    node_id           VARCHAR(50)  NOT NULL,
    severity          VARCHAR(20)  NOT NULL,
    message           TEXT         NOT NULL,
    classification    VARCHAR(100),
    xgb_confidence    DOUBLE,
    if_anomaly        BOOLEAN,
    combined_score    DOUBLE,
    resolved          BOOLEAN      NOT NULL DEFAULT FALSE,
    resolved_by       BIGINT,
    resolved_at       DATETIME,
    detected_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── Sessions ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sessions (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    token_hash        VARCHAR(255) NOT NULL,
    device_info       VARCHAR(255),
    ip_address        VARCHAR(45),
    node_id           VARCHAR(30),
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at        DATETIME     NOT NULL,
    revoked_at        DATETIME
);

-- ── Clinical Orders ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_number      VARCHAR(20)  NOT NULL UNIQUE,
    patient_id        BIGINT       NOT NULL,
    bed               VARCHAR(10)  NOT NULL,
    department        VARCHAR(50)  NOT NULL,
    order_type        VARCHAR(100) NOT NULL,
    description       TEXT,
    priority          VARCHAR(20)  NOT NULL DEFAULT 'ROUTINE',
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    requested_by      BIGINT,
    confirmed_by      BIGINT,
    requested_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at      DATETIME
);

-- ── Foreign Keys (MySQL only — comment out for SQLite) ───────
-- ALTER TABLE audit_log   ADD CONSTRAINT fk_audit_user    FOREIGN KEY (user_id)      REFERENCES users(id);
-- ALTER TABLE iot_anomalies ADD CONSTRAINT fk_anomaly_resolver FOREIGN KEY (resolved_by) REFERENCES users(id);
-- ALTER TABLE sessions    ADD CONSTRAINT fk_session_user  FOREIGN KEY (user_id)      REFERENCES users(id);
-- ALTER TABLE orders      ADD CONSTRAINT fk_order_patient FOREIGN KEY (patient_id)   REFERENCES patients(id);
-- ALTER TABLE orders      ADD CONSTRAINT fk_order_req     FOREIGN KEY (requested_by) REFERENCES users(id);
-- ALTER TABLE orders      ADD CONSTRAINT fk_order_conf    FOREIGN KEY (confirmed_by) REFERENCES users(id);
-- ALTER TABLE departments ADD CONSTRAINT fk_dept_head     FOREIGN KEY (head_user_id) REFERENCES users(id);

-- ── Indexes ───────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_users_dept        ON users(department);
CREATE INDEX IF NOT EXISTS idx_users_status      ON users(approval_status);
CREATE INDEX IF NOT EXISTS idx_patients_dept     ON patients(department);
CREATE INDEX IF NOT EXISTS idx_patients_status   ON patients(status);
CREATE INDEX IF NOT EXISTS idx_audit_user        ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_action      ON audit_log(action);
CREATE INDEX IF NOT EXISTS idx_anomaly_severity  ON iot_anomalies(severity);
CREATE INDEX IF NOT EXISTS idx_anomaly_resolved  ON iot_anomalies(resolved);
CREATE INDEX IF NOT EXISTS idx_orders_priority   ON orders(priority);
CREATE INDEX IF NOT EXISTS idx_orders_status     ON orders(status);
