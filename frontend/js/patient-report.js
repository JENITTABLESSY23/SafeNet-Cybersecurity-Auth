/**
 * SafeNet — patient details export / report.
 *
 * Two output formats, both generated entirely client-side from patient data
 * the account has already legitimately received (i.e. whatever
 * GET /api/patients returned — already department-scoped server-side by
 * PatientController, so this never has to re-check permissions; it can only
 * ever export what the browser already has):
 *
 *   - CSV  → PatientReport.downloadCSV(patients, label)
 *   - PDF-style printable report → PatientReport.openPrintableReport(patients, meta)
 *     (opens a formatted document in a new tab; the person uses the browser's
 *     own Print → Save as PDF, since generating real PDF bytes would need a
 *     library or a backend endpoint neither of which exist here yet)
 *
 * Also exports PatientReport.wireDownloadMenu(...) which wires up the small
 * "Download Report" button + dropdown used on every page that needs this.
 */
const PatientReport = (() => {

    const COLUMNS = [
        ['patientId',   'Patient ID'],
        ['firstName',   'First Name'],
        ['lastName',    'Last Name'],
        ['age',         'Age'],
        ['gender',      'Gender'],
        ['department',  'Department'],
        ['bed',         'Bed / Ward'],
        ['diagnosis',   'Diagnosis'],
        ['status',      'Status'],
        ['contact',     'Contact'],
        ['notes',       'Notes'],
    ];

    function escapeHtml(s) {
        return String(s == null ? '' : s).replace(/[&<>"']/g, c => (
            { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
        ));
    }

    function csvValue(v) {
        if (v === null || v === undefined) return '';
        const s = String(v);
        return /[",\n]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s;
    }

    function timestamp() {
        return new Date().toISOString().slice(0, 10);
    }

    /** Triggers a browser download of the given patients as a CSV file. */
    function downloadCSV(patients, label) {
        if (!patients || !patients.length) { alert('No patient records to export.'); return; }
        const header = COLUMNS.map(c => c[1]).join(',');
        const rows = patients.map(p => COLUMNS.map(c => csvValue(p[c[0]])).join(','));
        const csv = [header, ...rows].join('\r\n');
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `safenet-${(label || 'patients').toLowerCase().replace(/\s+/g, '-')}-${timestamp()}.csv`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
    }

    const STATUS_COLOR = { Critical: '#DC3545', Watch: '#DB9A2C', Stable: '#17A184' };

    /**
     * Opens a formatted, print-ready patient report in a new tab.
     * meta: { title, subtitle, generatedBy, hospitalId }
     */
    function openPrintableReport(patients, meta) {
        if (!patients || !patients.length) { alert('No patient records to export.'); return; }
        meta = meta || {};
        const title = meta.title || 'Patient Report';
        const subtitle = meta.subtitle || '';
        const generatedBy = meta.generatedBy || '—';
        const generatedAt = new Date().toLocaleString();

        const rows = patients.map(p => `
          <tr>
            <td>${escapeHtml(p.patientId)}</td>
            <td>${escapeHtml((p.firstName || '') + ' ' + (p.lastName || ''))}</td>
            <td>${escapeHtml(p.age)}</td>
            <td>${escapeHtml(p.gender)}</td>
            <td>${escapeHtml(p.department)}</td>
            <td>${escapeHtml(p.bed)}</td>
            <td>${escapeHtml(p.diagnosis)}</td>
            <td><span style="color:${STATUS_COLOR[p.status] || '#5B6B82'};font-weight:700;">${escapeHtml(p.status)}</span></td>
            <td>${escapeHtml(p.contact)}</td>
          </tr>`).join('');

        const html = `<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>${escapeHtml(title)}</title>
<style>
  * { box-sizing: border-box; }
  body { font-family: 'Segoe UI', Helvetica, Arial, sans-serif; color: #0B1626; margin: 0; padding: 36px 42px; }
  .brand { font-size: 13px; font-weight: 700; letter-spacing: 0.06em; color: #2A5FE0; text-transform: uppercase; }
  h1 { font-size: 22px; margin: 6px 0 2px; }
  .subtitle { color: #5B6B82; font-size: 13.5px; margin: 0 0 4px; }
  .meta { color: #8CA0B8; font-size: 12px; margin: 0 0 22px; }
  table { width: 100%; border-collapse: collapse; font-size: 12px; }
  th { text-align: left; background: #F4F7FC; color: #5B6B82; font-weight: 700; text-transform: uppercase;
       letter-spacing: 0.03em; font-size: 10.5px; padding: 9px 10px; border-bottom: 2px solid #E1E7F0; }
  td { padding: 9px 10px; border-bottom: 1px solid #EDF1F7; vertical-align: top; }
  tr:nth-child(even) td { background: #FAFBFD; }
  .print-btn {
    display: inline-flex; align-items: center; gap: 8px; margin-bottom: 22px;
    background: linear-gradient(135deg, #2A5FE0 0%, #1E46B3 100%); color: #fff; border: none;
    padding: 10px 18px; border-radius: 9px; font-size: 13.5px; font-weight: 600; cursor: pointer;
  }
  .footer { color: #8CA0B8; font-size: 11px; margin-top: 24px; }
  @media print {
    .no-print { display: none !important; }
    body { padding: 0 24px; }
  }
</style>
</head>
<body>
  <div class="brand">SafeNet</div>
  <h1>${escapeHtml(title)}</h1>
  <p class="subtitle">${escapeHtml(subtitle)}</p>
  <p class="meta">Generated ${escapeHtml(generatedAt)} by ${escapeHtml(generatedBy)} &middot; ${patients.length} patient record${patients.length === 1 ? '' : 's'}</p>

  <button class="print-btn no-print" onclick="window.print()">
    <svg width="15" height="15" viewBox="0 0 16 16" fill="none"><path d="M4 6V2h8v4M4 11h8v3H4v-3zM2 6h12a1 1 0 011 1v4a1 1 0 01-1 1h-2v-3H4v3H2a1 1 0 01-1-1V7a1 1 0 011-1z" stroke="white" stroke-width="1.3" stroke-linejoin="round"/></svg>
    Print / Save as PDF
  </button>

  <table>
    <thead>
      <tr><th>ID</th><th>Name</th><th>Age</th><th>Gender</th><th>Department</th><th>Bed</th><th>Diagnosis</th><th>Status</th><th>Contact</th></tr>
    </thead>
    <tbody>${rows}</tbody>
  </table>

  <p class="footer no-print">This report was generated in your browser from data your account is authorized to view. It is not stored on the SafeNet server.</p>
</body>
</html>`;

        const win = window.open('', '_blank');
        if (!win) { alert('Please allow pop-ups for this site to view the report.'); return; }
        win.document.write(html);
        win.document.close();
    }

    /**
     * Wires a "Download Report" button + its dropdown menu.
     * opts: {
     *   buttonId, menuId, csvBtnId, pdfBtnId,
     *   getPatients: () => Promise<Patient[]> | Patient[],
     *   label: string,               // used in the CSV filename
     *   reportMeta: {title, subtitle, generatedBy}
     * }
     */
    function wireDownloadMenu(opts) {
        const btn  = document.getElementById(opts.buttonId);
        const menu = document.getElementById(opts.menuId);
        if (!btn || !menu) return;

        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            menu.classList.toggle('open');
        });
        document.addEventListener('click', () => menu.classList.remove('open'));
        menu.addEventListener('click', (e) => e.stopPropagation());

        async function resolvePatients() {
            const result = opts.getPatients();
            return result instanceof Promise ? await result : result;
        }

        const csvBtn = document.getElementById(opts.csvBtnId);
        if (csvBtn) csvBtn.addEventListener('click', async () => {
            menu.classList.remove('open');
            const patients = await resolvePatients();
            downloadCSV(patients, opts.label);
        });

        const pdfBtn = document.getElementById(opts.pdfBtnId);
        if (pdfBtn) pdfBtn.addEventListener('click', async () => {
            menu.classList.remove('open');
            const patients = await resolvePatients();
            openPrintableReport(patients, opts.reportMeta);
        });
    }

    return { downloadCSV, openPrintableReport, wireDownloadMenu };
})();
