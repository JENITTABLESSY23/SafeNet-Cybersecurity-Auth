-- ============================================================
-- SafeNet Seed Data (Development Only)
-- ============================================================

-- ── Departments ───────────────────────────────────────────────
INSERT INTO departments (code, name, default_role) VALUES
    ('icu',        'ICU / Critical Care', 'Doc_ICU'),
    ('cardiology', 'Cardiology',          'Doc_Cardio'),
    ('gynecology', 'Gynecology',          'Doc_Gyno'),
    ('admin',      'Administration',      'Admin_Ops');

-- ── Staff Users (passwords are real BCrypt hashes of 'SafeNet@123' — generated,
--    not placeholder text; the originals here weren't valid bcrypt and would have
--    made every seeded account permanently unable to log in) ──────
-- Hospital IDs follow AuthService.buildHospitalId()'s actual format:
-- SN-<2-letter dept code><2-letter initials>-<3-digit number>
INSERT INTO users (first_name, last_name, username, hospital_id, password, email, phone,
                   department, role, approval_status, is_active) VALUES
    ('Anjali', 'Mehta',  'a.mehta',   'SN-ICAM-104', '$2b$10$QqVMH1L5KnJ8HK1gx/RoL.Q8VeWqCoaLwh2YS/OVHUFwyIQxH00fO', 'a.mehta@hospital.in',   '+91 98001 11234', 'icu',        'Doc_ICU',    'APPROVED', TRUE),
    ('James',  'Okafor', 'j.okafor',  'SN-CAJO-205', '$2b$10$ucQa1R0Ez5QUuFYZtufkOeMpil/6MQmmBH.GMGnyOAxK9T1r2H63O', 'j.okafor@hospital.in',  '+91 98001 22345', 'cardiology', 'Doc_Cardio', 'APPROVED', TRUE),
    ('Priya',  'Nair',   'p.nair',    'SN-GYPN-118', '$2b$10$PB5M3NVU/XejIpV9wYBtJe5vWFPP8KuLKox1wY/Oa8D0fVDKMiQpK', 'p.nair@hospital.in',    '+91 98001 33456', 'gynecology', 'Doc_Gyno',   'APPROVED', TRUE),
    ('Vijay',  'Rao',    'v.rao',     'SN-ADVR-233', '$2b$10$kYLT23u9PA516u608oi4reIDgkr2dIuVE8a4d5rWPEIfcod73RAna', 'v.rao@hospital.in',     '+91 98001 44567', 'admin',      'Admin_Ops',  'APPROVED', TRUE),
    ('Robert', 'Chen',   'r.chen',    'SN-CARC-391', '$2b$10$0lBSAhSOq75ykgOxe2nEwOp.TExnXHQoi3K04uoktrlHvkQRpK0Ee', 'r.chen@med.net',        '+91 98001 55678', 'cardiology', 'Doc_Cardio', 'PENDING',  FALSE),
    ('Sunita', 'Patel',  's.patel',   'SN-ICSP-276', '$2b$10$inJJuvps/ExVxKYjJcronedSScGzjSF6DWzAPCeNs0vdxLPK9tV66', 's.patel@hospital.in',   '+91 98001 66789', 'icu',        'Doc_ICU',    'PENDING',  FALSE);

-- ── Patients ──────────────────────────────────────────────────
INSERT INTO patients (patient_id, first_name, last_name, age, gender,
                      department, bed, diagnosis, status, contact, notes) VALUES
    ('SN-0041', 'Maya',     'Krishnan',  41, 'Female', 'ICU',        '04-A', 'Post-surgery monitoring',       'Stable',   '+91 98001 11234', 'Recovering well. No complications.'),
    ('SN-0042', 'Rajan',    'Pillai',    67, 'Male',   'ICU',        '07-B', 'Acute Myocardial Infarction',   'Critical', '+91 98001 22345', 'Under close monitoring. Family notified.'),
    ('SN-0043', 'Arthur',   'Pendelton', 58, 'Male',   'Cardiology', '01-A', 'Chronic Hypertension',          'Watch',    '+91 98001 33456', 'BP elevated above threshold.'),
    ('SN-0044', 'Beatrice', 'Vance',     64, 'Female', 'Cardiology', '01-B', 'Arrhythmia — Pacemaker',        'Stable',   '+91 98001 44567', 'Pacemaker X-7 functioning normally.'),
    ('SN-0045', 'Samuel',   'Iyer',      72, 'Male',   'Cardiology', '02-A', 'Heart Failure (Stage II)',       'Watch',    '+91 98001 55678', 'ICD threshold review pending.'),
    ('SN-0046', 'Clara',    'Mendes',    45, 'Female', 'Cardiology', '02-B', 'Post-angioplasty',              'Stable',   '+91 98001 66789', 'Routine monitoring. Discharge planned Friday.'),

    -- Gynecology previously had zero seeded patients even though the ward
    -- dashboard already references two of these by name/bed — added here
    -- so patient-record lookups actually resolve instead of 404ing.
    ('SN-0047', 'Preeti',   'Nair',      29, 'Female', 'Gynecology', '12-A', 'Antenatal, 36 weeks',           'Stable',   '+91 98001 77890', 'No signs of preeclampsia. Routine antenatal monitoring.'),
    ('SN-0048', 'Fathima',  'Rasheed',   34, 'Female', 'Gynecology', '15-C', 'Post-op Day 1 (Hysterectomy)',  'Watch',    '+91 98001 88901', 'Slightly elevated HR and temp — monitoring for post-op infection.'),
    ('SN-0049', 'Lakshmi',  'Subramaniam', 31, 'Female', 'Gynecology', '13-A', 'Antenatal, 28 weeks — Gestational diabetes', 'Watch', '+91 98001 99012', 'On dietary management. Weekly glucose monitoring.'),
    ('SN-0050', 'Divya',    'Krishnamurthy', 38, 'Female', 'Gynecology', '14-B', 'Ovarian cystectomy — Post-op Day 3', 'Stable', '+91 98002 00123', 'Recovering well. Discharge planned tomorrow.'),

    -- Additional ICU patients
    ('SN-0051', 'Vikram',   'Choudhary', 55, 'Male',   'ICU',        '05-A', 'Septic shock',                  'Critical', '+91 98002 11234', 'On vasopressors. Blood cultures pending.'),
    ('SN-0052', 'Anita',    'Desai',     49, 'Female', 'ICU',        '06-B', 'Post-craniotomy monitoring',    'Watch',    '+91 98002 22345', 'Neuro checks every 2 hours.'),
    ('SN-0053', 'Farhan',   'Sheikh',    62, 'Male',   'ICU',        '08-A', 'COPD exacerbation',             'Stable',   '+91 98002 33456', 'Weaning off supplemental oxygen.'),

    -- Additional Cardiology patients
    ('SN-0054', 'Meera',    'Bhatt',     51, 'Female', 'Cardiology', '03-A', 'Atrial fibrillation',           'Stable',   '+91 98002 44567', 'Rate-controlled on beta-blocker.'),
    ('SN-0055', 'Deepak',   'Kulkarni',  69, 'Male',   'Cardiology', '03-B', 'Post-CABG recovery, Day 4',     'Stable',   '+91 98002 55678', 'Ambulating independently. Discharge planning underway.'),

    -- General/unassigned department — used to exercise the RBAC boundary for
    -- the generic "Staff" role, which isn't tied to a clinical department.
    ('SN-0056', 'Rohit',    'Verma',     33, 'Male',   'General',    '20-A', 'Routine pre-employment health screening', 'Stable', '+91 98002 66789', 'No abnormal findings.');

-- ── Sample Anomalies ──────────────────────────────────────────
INSERT INTO iot_anomalies (node_id, severity, message, classification,
                           xgb_confidence, if_anomaly, combined_score) VALUES
    ('Node_ICU_Bed4',    'WARNING',  'Respiratory rate spike outside cluster bounds.',     'Sensor Drift / Device Malfunction',      0.61, TRUE,  0.54),
    ('Node_Cardio_Sens1','CRITICAL', 'Cross-role telemetry read attempt by User_ID 1092.', 'Privilege Escalation / Insider Threat',  0.94, TRUE,  0.91);

-- ── Sample Audit Log ──────────────────────────────────────────
INSERT INTO audit_log (user_id, action, resource_type, resource_id, details, node_id) VALUES
    (1, 'LOGIN',   'USER',    '1', 'Successful login from Chrome/Windows 11', 'IN-MH-007'),
    (4, 'APPROVE', 'USER',    '2', 'Staff Dr. J. Okafor approved as Doc_Cardio', 'IN-MH-007'),
    (1, 'BTG',     'SYSTEM',  NULL,'Emergency override activated. Reason: Patient cardiac arrest — Bed 07B', 'IN-MH-007'),
    (2, 'VIEW',    'PATIENT', '3', 'Patient SN-0043 record accessed', 'IN-MH-007');

-- ── Sample Orders ─────────────────────────────────────────────
INSERT INTO orders (order_number, patient_id, bed, department,
                    order_type, priority, status, requested_by) VALUES
    ('ORD-882', 2, '07-B', 'ICU', 'IV Epinephrine 0.5mg', 'STAT',    'PENDING', 1),
    ('ORD-883', 1, '04-A', 'ICU', 'CBC Blood Draw',        'URGENT',  'PENDING', 1),
    ('ORD-884', 2, '07-B', 'ICU', 'Chest X-Ray (portable)','ROUTINE', 'PENDING', 1);
