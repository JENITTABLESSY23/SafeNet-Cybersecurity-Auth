# SafeNet — Full Project Audit

You uploaded the project fresh for a check, but it turned out to be an
earlier snapshot — missing the backend security fixes and frontend wiring
from recent work (no `js/api.js`, no `@JsonIgnore` on the password field,
etc.). Rather than review a stale copy, this merges that upload with the
current state and re-audits the whole thing from scratch. Everything below
reflects what's actually in this zip.

## Portal-by-portal status

| Portal | Status | Notes |
|---|---|---|
| Login | ✅ Working | Real auth against `/api/auth/login`, routes by actual department |
| Register | ✅ Working | Real multipart submission with ID proof upload |
| Patient Records | ✅ Working | Full CRUD against `/api/patients` |
| Admin Console | ✅ Working | Approvals/staff/stats/anomalies all real, role-gated |
| Logout | ✅ Working | Actually invalidates the token server-side now |
| Forgot / Reset Password | ✅ Working | New — real emailed reset link, 30-min single-use token |
| ICU Dashboard | ✅ Working | Vitals poll the real `/api/iot/vitals/{bedId}` endpoint |
| Cardiology Dashboard | ✅ Working | Now polls real vitals too; ECG animation speed tracks the real BPM |
| Gynecology Dashboard | ✅ Working (partial) | Maternal HR and post-op HR are real; fetal HR is honestly left simulated — the backend has no obstetric-specific telemetry to source it from |
| Settings | ✅ Working | Profile, password, and designation now persist via new `/api/users/me` endpoints |
| Help | ✅ Working | Tickets persist via new `/api/support/tickets` endpoint |

All ten portals are now backed by real endpoints where a real endpoint
makes sense. The one deliberate exception (Gynecology's fetal heart rate)
is documented in the code rather than faked.

## New backend endpoints added this pass

Settings and Help had nowhere to actually send data — closed that gap:

- `GET /api/users/me` / `PUT /api/users/me` — view and update your own
  profile (name, email, phone, designation). Department and role are
  intentionally *not* self-editable — those are RBAC-managed and require
  admin approval, so the frontend's department field is now honestly
  read-only instead of a dropdown that silently did nothing.
- `PUT /api/users/me/password` — change your own password, with real
  current-password verification and the same strength rules the frontend
  already displayed but never actually enforced server-side.
- `POST /api/support/tickets` / `GET /api/support/tickets` — submit and
  list support tickets, backed by a new `support_tickets` table.
- Added a `designation` column to `User` (e.g. "Consultant Physician") —
  distinct from `role`, which is the RBAC string. The Settings page had an
  editable field for this that was never persisted anywhere; now it is.

## Forgot Password (new feature)

The project had no account-recovery path at all — if you forgot your
password, there was nothing to do about it. Added a standard email-based
reset flow:

- `POST /api/auth/forgot-password` — takes an email, and if it matches an
  account, generates a single-use token (30 min expiry) and emails a reset
  link via `spring-boot-starter-mail`. Returns the *same* response either
  way — "if an account exists, a link has been sent" — rather than
  confirming or denying that an email is registered, which is a standard
  account-enumeration defense.
- `POST /api/auth/reset-password` — takes the token + new password,
  validates the token hasn't expired or already been used, enforces the
  same password strength rule used elsewhere (8+ chars, uppercase, number,
  special character), and marks the token used so it can't be replayed.
- Both endpoints are `permitAll()` in `SecurityConfig` — has to be, since
  someone who forgot their password by definition can't authenticate
  first.
- Added a basic per-email cooldown (max 3 requests per 30 minutes) so the
  endpoint can't be used to spam an inbox — same in-memory-tracker pattern
  as the existing login lockout in `AuthService`.
- New `forgot-password.html` and `reset-password.html` pages, plus a
  "Forgot password?" link on the login page. `reset-password.html` reads
  the token from the URL query string and shows a "missing reset link"
  state if someone lands there without one (e.g. bookmarked the page,
  didn't come from the actual email).

**Setup required:** this needs real SMTP credentials in
`application.properties` to actually send email — placeholder values are
there by default with comments on how to get a Gmail app password
(regular Gmail passwords don't work for SMTP). Without real credentials
configured, the reset token still gets created and logged correctly, but
`mailSender.send()` will throw and no email will arrive — worth testing
this end-to-end once configured, not just trusting it compiles.



## New bugs found and fixed in this pass

**No page except three actually required login.** `patients.html`,
`admin.html`, and `dashboard_icu.html` had the auth guard from earlier
work. `dashboard_cardio.html`, `dashboard_gynecology.html`,
`settings.html`, and `help.html` did not — you could open any of them
directly with no session at all, and they'd render fine with hardcoded
names ("Dr. James Okafor", "Dr. Fathima Rahman") instead of whoever's
actually logged in. Added the same guard to all four; verified each one
now redirects to login without a session and shows the real logged-in
user's name with one.

**Password hashes weren't valid — seed accounts couldn't log in.**
`database/seed_data.sql` had literal placeholder text
(`$2a$10$xyzBCryptHashPlaceholder1`) in the password column, not an actual
BCrypt hash. `BCryptPasswordEncoder.matches()` would fail against this
immediately — every seeded account was permanently locked out. Generated
real BCrypt hashes for `SafeNet@123` for all six.

**Seed data's Hospital IDs didn't match the format the backend actually
generates.** `AuthService.buildHospitalId()` produces `SN-<2-letter dept
code><2-letter initials>-<3-digit number>` — e.g. `SN-ICAM-104`. The seed
data had things like `SN-AMICU-001` (5-character middle segment) and
`SN-PNGYNO-03` (6 characters, 2-digit suffix). Neither matches the
generator's output, and neither would pass the frontend's own hospital-ID
format validation. Regenerated all six to the real format.

**`database/schema.sql` was missing `first_name`/`last_name` on `users`.**
The actual `User` entity requires both (`nullable = false`). This doesn't
break local dev — `application.properties` has
`hibernate.ddl-auto=update`, so Hibernate generates the real schema
straight from the entities and never touches `schema.sql`. But
`schema.sql` is what the README tells you to run for a real MySQL setup,
and that would fail on a NOT NULL violation the moment you tried to insert
a user. Added the missing columns; also had to add matching values to the
seed INSERT, which didn't have them either.

## Documentation cleanup

`README.md` had several claims that don't hold up if anyone reads the code
closely — worth fixing regardless of whether this project gets evaluated
by someone technical, because these are the kind of specific, checkable
claims that erode trust once one of them turns out to be false:

- Claimed **two-factor authentication**, **AES-256-GCM encryption at
  rest**, **TLS 1.3 in transit**, and **HIPAA / ISO 27001 / DPDP Act
  compliance**. None of these exist anywhere in the codebase — no TOTP/SMS
  logic, no field-level encryption, no TLS configuration, and compliance
  certification isn't something a project claims for itself. Rewrote the
  security section to separate what's actually implemented (RBAC, JWT,
  BCrypt, audit trail, BTG, brute-force lockout) from what's planned, and
  removed the compliance claims entirely — the honest framing is that this
  demonstrates the *patterns* those frameworks require, not certified
  compliance.
- The ML section had specific, precise-sounding benchmark numbers
  (**"+2.1% accuracy vs Base SVM (98.42% total)," "12.4ms latency"**) that
  predate the real dataset ever being trained on — they were invented, not
  measured. Replaced with a description of what the script actually
  outputs when you run it, rather than a number decided in advance of
  running it.
- The deliverable checklist marked backend/integration work as incomplete
  (`⬜`) when it's fully built — stale from before that work happened.
  Updated to reflect actual status.
- A four-person placeholder team table (`Team Member 1`, `Team Member 2`,
  ...) — doesn't match what looks like an individual project. Replaced
  with a single-author line; fill in your name.
- Repo structure diagram was missing `js/api.js`, `dashboard_gynecology.html`,
  and the entire `ml/` folder. Updated.

### The two `.docx` reports had the same problem, and it's more serious there

These are presumably what actually gets graded, so the same category of
issue matters more here than in the README:

- **`SafeNet_Project_Report.docx`** — Section 6.1 (Results) correctly
  marks SafeNet's own model performance as `TBD*` pending a real training
  run. But Section 2.2 (Literature Comparison Table), a few pages earlier,
  lists SafeNet in the same table as real published papers with a claimed
  accuracy of **"> 99.55%"** and limitation **"None identified"** — an
  unverified number presented as an established fact, contradicting the
  document's own honesty two sections later, and a "no limitations" claim
  no legitimate paper makes about its own unproven method. Changed both
  cells to point back to the honest TBD in §6.1.
- The Security Features table (Section 7) claimed **TLS 1.3**,
  **AES-256-GCM**, and **2FA (TOTP + SMS)** as implemented, each cited
  against a specific standard (RFC 8446, FIPS 140-2, RFC 6238) — citing a
  formal spec number next to a feature that doesn't exist is a very
  specific, very checkable false claim. Changed all three to "Not yet
  implemented" / "HTTP only" with "Planned" in place of the standard
  citation.
  - The **File Upload Security** row claimed "UUID renaming + MIME
    validation," and the UUID part was true but MIME validation wasn't —
    `AuthService.register()` accepted any file type for the ID proof
    upload with zero content-type checking. Rather than water down the
    claim, fixed the code instead: added a real MIME allowlist (JPEG/PNG/
    PDF) so the claim is actually true now. Also found and fixed a path-
    traversal risk in the same method — the uploaded file's original
    filename was used unsanitized when constructing the save path, so a
    filename containing `../` sequences could theoretically write outside
    the intended upload directory.
  - Softened flat "HIPAA-compliant" phrasing throughout (abstract,
    objectives, conclusion) to "HIPAA-aligned," consistent with the
    Security Features fix.
- **`SafeNet_Abstract_Benefits_FutureScope.docx`** — same HIPAA/AES/TLS/2FA
  claims repeated (fixed the same way), plus:
  - The Gynecology dashboard was described as a *future* item ("placeholder
    ... pending clinical review") — it's actually built now. Updated to
    reflect what's real (maternal/fetal heart rate, BP, post-op vitals)
    versus what's still future (labour progress tracking).
  - Frontend page count said "9 HTML pages" listing Login/Register/ICU/
    Cardiology/Admin/Patients/Settings/Help/Logout — missing Gynecology
    (now 10) and the shared `js/api.js` client. Updated.
  - Closing paragraph said the system was "ready for Spring Boot + MySQL
    backend integration" — stale; it's already fully integrated. Updated.



## Suggestions for what's next

Roughly in priority order:

1. **Pagination on Patient Records.** `/api/patients` returns everything
   in one response — fine for a demo with 6 patients, not fine for a real
   hospital's patient list. Spring Data's `Pageable` handles this with
   minimal controller changes.
2. **Server-side search/filter for patients**, replacing the current
   fetch-everything-then-filter-in-JS approach — same scaling concern as
   pagination.
3. **Refresh tokens.** The JWT is long-lived with no rotation — fine for a
   demo, a real system would want short-lived access tokens plus a refresh
   flow.
4. **Persist the JWT blacklist and IoT monitoring state to the database**
   instead of in-memory (`ConcurrentHashMap`s in `AuthService`/`JwtUtil`) —
   currently anyone logged in loses that state on a backend restart, and it
   wouldn't work correctly if you ever ran more than one backend instance.
5. **Automated tests.** There currently aren't any — not unit tests on the
   service layer, not integration tests on the controllers. Given how many
   of the bugs in this audit were format/contract mismatches between
   layers (frontend validation vs. backend generation, schema vs. entity),
   a handful of tests asserting those contracts would have caught several
   of them automatically.
6. **Real database instead of SQLite** if this ever needs concurrent
   writers or runs anywhere besides a single dev machine — `schema.sql`
   already claims MySQL compatibility, worth actually verifying it.
7. **Admin visibility into support tickets.** Right now tickets save to
   the database but nothing ever reads them back except the submitter —
   there's no admin-side inbox to actually action them. Would need a
   `GET /api/support/tickets` variant scoped to all tickets (Admin_Ops
   only) plus a small admin.html panel.
8. **Real fetal heart rate telemetry** for the Gynecology dashboard, if
   this ever needs to be fully real rather than partially — would mean
   extending `IoTService` with obstetric-specific simulated (or real)
   readings, not just generic bedside vitals.

## Found but not fully fixed: more false security claims, this time in the UI itself

The docs and README had fabricated 2FA/AES-256/TLS 1.3/HIPAA claims,
fixed in an earlier pass. While adding the "Forgot password?" link to
`login.html`, found the *same* pattern still sitting in the actual
frontend UI copy — text a user actually sees, not just documentation:

- `login.html`'s status bar said "All systems operational — TLS 1.3
  encrypted" — fixed (removed the false claim).
- Still unfixed, found while looking:
  - `login.html`'s footer: "HIPAA compliant · AES-256 encrypted · ISO
    27001 certified"
  - `settings.html`: displays "AES-256-GCM · TLS 1.3" as if it were a live
    status readout
  - `help.html`'s FAQ literally answers "yes" to a compliance question
    with "...the system undergoes quarterly third-party security audits"
    — this one's worse than the others, since it's not just an unbuilt
    feature but an entirely invented operational claim about audits that
    have never happened.

Didn't fix these three in this pass since the task was specifically the
forgot-password feature and this was scope creep beyond it — flagging
clearly rather than leaving it for someone to discover on their own.

## Bug found during actual hands-on testing: `schema.sql` doesn't run on SQLite at all

This one wasn't caught by reading the code — it only showed up when
actually trying to seed a real local database, which is exactly why that
kind of testing matters more than code review alone.

`schema.sql` uses `BIGINT PRIMARY KEY AUTO_INCREMENT` on every single
table. That's valid MySQL, but SQLite has no `AUTO_INCREMENT` keyword at
all — its equivalent is `INTEGER PRIMARY KEY AUTOINCREMENT` (different
type, no underscore). Running `schema.sql` against a SQLite database
fails immediately with `sqlite3.OperationalError: near "AUTO_INCREMENT":
syntax error` on the very first `CREATE TABLE`.

This had been invisible up to now because the running backend never
executes `schema.sql` at all — `hibernate.ddl-auto=update` generates the
real schema straight from the JPA entities. `schema.sql` only matters when
someone tries to seed a database by hand, which is exactly what happened
here.

**Fixed by adding `database/schema_sqlite.sql`** — the same schema with
only the primary-key syntax translated for SQLite. `schema.sql` stays as
the MySQL/production reference (accurate now, since that's genuinely valid
MySQL). Verified end-to-end: ran `schema_sqlite.sql` then `seed_data.sql`
against a real SQLite file and confirmed all 6 users land correctly with
the right hospital IDs and approval statuses — not just that it doesn't
error, but that the data that comes out the other end is actually correct.

**Separately caught but not yet an issue in practice:** `seed_data.sql`
inserts into a `departments` table that has no backing JPA entity. If
someone ever seeds by starting the backend first (letting Hibernate
auto-create tables) instead of running a schema file, `departments` won't
exist and that INSERT will fail. Not a problem as long as
`schema_sqlite.sql` is run first (it creates `departments` directly,
schema-file tables aren't tied to entities), but worth knowing if the
setup order ever changes.

## Another hands-on bug: safenet_model.py couldn't find the dataset even when it was there

Same root cause as the schema.sql issue above — a relative path that only
works if you happen to be standing in the right directory when you run
the script. `DATA_PATH = "CICIoT2023/"` gets resolved against whatever
folder you're in when you type `python safenet_model.py`, not against
where the script file actually lives. Run it from the project root instead
of from inside `ml/`, and it looks for `CICIoT2023/` in the wrong place,
finds nothing, and silently falls back to demo mode — which is exactly
what happened.

**Fixed properly this time** — `DATA_PATH` and `OUTPUT_DIR` are now
resolved from the script's own file location
(`os.path.dirname(os.path.abspath(__file__))`), so it works the same
regardless of which directory you run it from.

Also hardened two more things while testing this:
- CSV detection is now case-insensitive (`.CSV` as well as `.csv` — some
  dataset mirrors and Windows zip extractions preserve different casing).
- If the folder exists but has no CSVs, it now prints the exact absolute
  path it looked in and lists what's actually in that folder — so if the
  zip extracted into a nested subfolder (`CICIoT2023/CICIoT2023/*.csv`),
  that's immediately visible instead of a bare "not found."

Verified by actually running the script from an unrelated directory with
an uppercase `.CSV` file — confirmed it found and used the real data
correctly, not just that it didn't crash.

## Real memory crash on the full dataset — and a regression I introduced fixing it

Ran into this via an actual screenshot of the error, not by inspection:
`numpy._core._exceptions._ArrayMemoryError: Unable to allocate 357. MiB for
an array with shape (1, 46776700)`. All 309 files loaded individually
without issue, then crashed on `pd.concat(frames)` — the step that merges
every loaded file into one DataFrame before any sampling happens.

The math: ~46.7 million rows × ~47 columns × 8 bytes ≈ **16+ GB** just to
hold one copy of the full dataset, and `pd.concat` needs headroom beyond
that to do the merge itself. The per-class sampling added earlier
(`N_SAMPLES_PER_CLASS`) only ran *after* this — meaning it could never
help, because the crash happens before sampling ever gets a chance to
shrink anything. This is the full, un-partial CICIoT2023 release (all 309
files), not the smaller subset some mirrors distribute.

**Fixed properly**: replaced `load_dataset()` + `sample_per_class()` with
a single `load_dataset_sampled()` that samples each class's running total
*as each file is read*, one file at a time — so peak memory is one file's
rows plus the small accumulated sample (bounded at
`N_SAMPLES_PER_CLASS × num_classes`, a few hundred thousand rows at most),
never the full 46.7M-row dataset. As a bonus, it also stops reading
further files once every class's quota is filled — in testing against a
smaller synthetic multi-file dataset, it only needed 6 of 30 files before
all quotas were met.

Verified by actually running the full pipeline end-to-end after the fix,
not just checking the loader in isolation.

**Regression caught in the same pass**: fixing the `DATA_PATH`
working-directory bug (see above) changed `OUTPUT_DIR` from the string
`"outputs/"` to `os.path.join(_SCRIPT_DIR, "outputs")` — which, correctly,
has no trailing slash. But every single place the code wrote or loaded a
model file did `f"{OUTPUT_DIR}scaler.pkl"` — string concatenation that
depended on that trailing slash. Every output file was silently landing
in `ml/` itself with a mangled name (`outputsscaler.pkl` instead of
`outputs/scaler.pkl`) instead of inside `outputs/` — which would have
broken `ml_api.py`, since it loads models from inside `outputs/`
specifically. Caught this by actually inspecting where the files landed
after a real run, not just checking the console output looked fine — the
console text had the same bug and looked plausible at a glance despite
being wrong. Fixed all 11 call sites (model dump/load, plot save, and the
print statements reporting the paths) to use `os.path.join()` consistently.
