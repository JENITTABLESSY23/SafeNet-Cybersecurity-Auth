/* ============================================================
   SafeNet — AI Assistant
   Floating widget with predefined intelligent hospital
   responses. No external AI/LLM calls — pure rule-based intent
   matching, wired to real endpoints (/patients, /admin/stats)
   where SafeNet already has the data. Anything without a real
   backing endpoint (bed capacity, medicine stock) is answered
   with clearly-labeled demo data rather than invented as if
   real — same standard as the rest of the app.
   Only mounts inside the authenticated shell (pages with an
   <aside> sidebar), same as the global toolbar.
   ============================================================ */
(function () {
  'use strict';

  if (!document.querySelector('aside')) return;

  var ICONS = {
    bot: '<svg viewBox="0 0 20 20" fill="none"><rect x="4" y="6" width="12" height="9" rx="2.5" stroke="currentColor" stroke-width="1.5"/><circle cx="7.5" cy="10.5" r="1" fill="currentColor"/><circle cx="12.5" cy="10.5" r="1" fill="currentColor"/><path d="M10 6V3M7 3h6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>',
    close: '<svg viewBox="0 0 16 16" fill="none"><path d="M4 4l8 8M12 4l-8 8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>',
    send: '<svg viewBox="0 0 16 16" fill="none"><path d="M2 8h11M9 4l4 4-4 4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>'
  };

  var CHIPS = [
    { label: 'Find patient', intent: 'find_patient' },
    { label: 'Critical patients', intent: 'critical' },
    { label: 'Bed availability', intent: 'beds' },
    { label: 'Medicine stock', intent: 'meds' },
    { label: 'Dashboard summary', intent: 'summary' },
    { label: 'Generate report', intent: 'report' }
  ];

  var DEPT_CAPACITY = { ICU: 12, Cardiology: 16, Gynecology: 14, General: 20 };

  function el(html) {
    var d = document.createElement('div');
    d.innerHTML = html.trim();
    return d.firstChild;
  }
  function escapeHtml(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }

  async function fetchPatients() {
    if (typeof SafeNetAPI === 'undefined') throw new Error('The SafeNet API client hasn\u2019t loaded on this page.');
    return await SafeNetAPI.get('/patients');
  }

  // Surfaces the *actual* reason a call failed (api.js already throws
  // specific messages — e.g. "backend not running" vs "404" vs a real
  // server error) instead of masking everything as one generic string.
  function describeError(e, fallbackAction) {
    var msg = (e && e.message) ? e.message : 'Something went wrong.';
    return escapeHtml(msg) + (fallbackAction ? ' ' + fallbackAction : '');
  }

  // ---------- Intent handlers — each returns HTML for a bot bubble ----------

  async function findPatient(query) {
    try {
      var patients = await fetchPatients();
      var q = (query || '').trim().toLowerCase();
      if (!q) return 'Tell me a name, patient ID, or bed number to search for — e.g. <em>"find patient Rao"</em>.';
      var matches = patients.filter(function (p) {
        var full = (p.firstName + ' ' + p.lastName).toLowerCase();
        return full.indexOf(q) !== -1 ||
          (p.patientId || '').toLowerCase().indexOf(q) !== -1 ||
          (p.bed || '').toLowerCase().indexOf(q) !== -1;
      }).slice(0, 5);
      if (!matches.length) return 'No patients matched &ldquo;' + escapeHtml(query) + '&rdquo;.';
      return matches.map(function (p) {
        return '<strong>' + escapeHtml(p.firstName + ' ' + p.lastName) + '</strong> — ' +
          escapeHtml(p.department) + ', bed ' + escapeHtml(p.bed) + ', status ' + escapeHtml(p.status) + '<br>';
      }).join('');
    } catch (e) {
      console.error('[SafeNet AI] findPatient failed:', e);
      return describeError(e, 'You can also check the Patient Records page directly.');
    }
  }

  async function criticalPatients() {
    try {
      var patients = await fetchPatients();
      var critical = patients.filter(function (p) { return p.status === 'Critical'; });
      if (!critical.length) return 'No patients currently marked <strong>Critical</strong>. All clear.';
      return '<strong>' + critical.length + ' critical patient(s):</strong><br>' +
        critical.map(function (p) {
          return escapeHtml(p.firstName + ' ' + p.lastName) + ' — ' + escapeHtml(p.department) + ', bed ' + escapeHtml(p.bed);
        }).join('<br>');
    } catch (e) {
      console.error('[SafeNet AI] criticalPatients failed:', e);
      return describeError(e);
    }
  }

  async function bedAvailability() {
    try {
      var patients = await fetchPatients();
      var occupied = {};
      patients.forEach(function (p) {
        if (p.status === 'Discharged') return;
        occupied[p.department] = (occupied[p.department] || 0) + 1;
      });
      var rows = Object.keys(DEPT_CAPACITY).map(function (dept) {
        var used = occupied[dept] || 0;
        var cap = DEPT_CAPACITY[dept];
        return '<tr><td>' + dept + '</td><td>' + used + ' / ' + cap + '</td><td>' + Math.max(cap - used, 0) + ' free</td></tr>';
      }).join('');
      return 'Occupancy based on current non-discharged patient records:' +
        '<table><tr><th>Dept</th><th>Occupied</th><th>Available</th></tr>' + rows + '</table>' +
        '<span class="ai-demo-tag">Capacity figures are configured demo values — no bed-management module yet</span>';
    } catch (e) {
      console.error('[SafeNet AI] bedAvailability failed:', e);
      return describeError(e);
    }
  }

  function medicineStock() {
    var rows = [
      ['Paracetamol 500mg', '1,240 units'],
      ['Amoxicillin 250mg', '380 units'],
      ['IV Saline 0.9%', '95 units'],
      ['Insulin (Regular)', '62 units']
    ];
    return 'Pharmacy snapshot:' +
      '<table><tr><th>Item</th><th>Stock</th></tr>' +
      rows.map(function (r) { return '<tr><td>' + r[0] + '</td><td>' + r[1] + '</td></tr>'; }).join('') +
      '</table><span class="ai-demo-tag">Demo data — pharmacy module not yet connected to a live inventory endpoint</span>';
  }

  async function dashboardSummary() {
    try {
      var patients = await fetchPatients();
      var byStatus = {};
      patients.forEach(function (p) { byStatus[p.status] = (byStatus[p.status] || 0) + 1; });
      var lines = 'Total patients: <strong>' + patients.length + '</strong><br>' +
        Object.keys(byStatus).map(function (s) { return s + ': ' + byStatus[s]; }).join(' &middot; ');

      if (typeof SafeNetAPI !== 'undefined' && SafeNetAPI.getUser && SafeNetAPI.getUser() && SafeNetAPI.getUser().department === 'Admin') {
        try {
          var stats = await SafeNetAPI.get('/admin/stats');
          lines += '<br>Pending approvals: ' + stats.pendingCount +
            ' &middot; Critical alerts: ' + stats.criticalAlerts +
            ' &middot; Warnings: ' + stats.warningAlerts;
        } catch (e) { /* non-admin or endpoint unavailable — skip silently */ }
      }
      return lines;
    } catch (e) {
      console.error('[SafeNet AI] dashboardSummary failed:', e);
      return describeError(e);
    }
  }

  async function generateReport() {
    try {
      var patients = await fetchPatients();
      var byDept = {};
      patients.forEach(function (p) { byDept[p.department] = (byDept[p.department] || 0) + 1; });
      var critical = patients.filter(function (p) { return p.status === 'Critical'; }).length;
      var rows = Object.keys(byDept).map(function (d) { return '<tr><td>' + d + '</td><td>' + byDept[d] + '</td></tr>'; }).join('');
      return '<strong>Snapshot report — ' + new Date().toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) + '</strong>' +
        '<table><tr><th>Department</th><th>Patients</th></tr>' + rows + '</table>' +
        'Critical: ' + critical + ' &middot; Total: ' + patients.length +
        '<br><span class="ai-demo-tag">In-chat snapshot only — export/print isn&rsquo;t wired up yet</span>';
    } catch (e) {
      console.error('[SafeNet AI] generateReport failed:', e);
      return describeError(e);
    }
  }

  // Casual messages ("hi", "thanks", "ok") get a friendly canned reply
  // and never touch the backend. Checked before intent matching.
  function smallTalkReply(text) {
    var t = text.toLowerCase().trim();
    if (/^(hi|hello|hey|hola|good\s?(morning|afternoon|evening))\b/.test(t)) {
      return 'Hi there! Ask me to find a patient, check critical cases, bed availability, medicine stock, a dashboard summary, or a report.';
    }
    if (/^(thanks|thank you|thankyou|ty|great|cool|nice|ok|okay)\b/.test(t)) {
      return 'Anytime! Let me know if you need anything else.';
    }
    if (/^(bye|goodbye|see ya|see you)\b/.test(t)) {
      return 'Take care!';
    }
    return null;
  }

  // Intent keywords require an actual action word, not just an
  // incidental mention — e.g. bare "patient" in a sentence used to
  // trigger a live /patients call on every message; now it needs
  // "find"/"search"/"look up" alongside it.
  function matchIntent(text) {
    var t = text.toLowerCase();
    if (/\bcritical\b/.test(t)) return 'critical';
    if (/\bbeds?\b/.test(t)) return 'beds';
    if (/\b(medicine|medicines|stock|pharmacy|drugs?)\b/.test(t)) return 'meds';
    if (/\b(summary|overview)\b/.test(t) || /how.*(doing|today)/.test(t)) return 'summary';
    if (/\breports?\b/.test(t)) return 'report';
    if (/\b(find|search|look\s?up)\b/.test(t)) return 'find_patient';
    return null;
  }

  async function runIntent(intent, rawText) {
    switch (intent) {
      case 'find_patient': return await findPatient(rawText.replace(/\b(find|search|look\s?up|patient|patients)\b/gi, '').trim());
      case 'critical': return await criticalPatients();
      case 'beds': return await bedAvailability();
      case 'meds': return medicineStock();
      case 'summary': return await dashboardSummary();
      case 'report': return await generateReport();
      default:
        return 'I can help with: finding a patient, critical patients, bed availability, medicine stock, a dashboard summary, or a quick report. Try one of the buttons below, or ask directly — e.g. &ldquo;find patient Rao&rdquo;.';
    }
  }

  // ---------- UI ----------

  function build() {
    var fab = el('<button class="ai-fab" id="aiFab" title="SafeNet Assistant">' + ICONS.bot + '</button>');
    var panel = el(
      '<div class="ai-panel" id="aiPanel">' +
        '<div class="ai-head">' +
          '<div class="ai-head-icon">' + ICONS.bot + '</div>' +
          '<div><div class="ai-head-title">SafeNet Assistant</div><div class="ai-head-sub">Hospital ops helper</div></div>' +
          '<button class="ai-head-close" id="aiClose">' + ICONS.close + '</button>' +
        '</div>' +
        '<div class="ai-body" id="aiBody"></div>' +
        '<div class="ai-chips" id="aiChips">' +
          CHIPS.map(function (c) { return '<button class="ai-chip" data-intent="' + c.intent + '">' + c.label + '</button>'; }).join('') +
        '</div>' +
        '<div class="ai-input-row">' +
          '<input class="ai-input" id="aiInput" placeholder="Ask about patients, beds, reports…" autocomplete="off">' +
          '<button class="ai-send" id="aiSend">' + ICONS.send + '</button>' +
        '</div>' +
      '</div>'
    );
    document.body.appendChild(fab);
    document.body.appendChild(panel);
    return { fab: fab, panel: panel, body: panel.querySelector('#aiBody'), chips: panel.querySelector('#aiChips'), input: panel.querySelector('#aiInput') };
  }

  function addMsg(body, html, who) {
    var msg = el('<div class="ai-msg ai-msg-' + who + '"><div class="ai-bubble">' + html + '</div></div>');
    body.appendChild(msg);
    body.scrollTop = body.scrollHeight;
  }

  function wire(nodes) {
    var greeted = false;

    function open() {
      nodes.panel.classList.add('open');
      if (!greeted) {
        addMsg(nodes.body, 'Hi, I&rsquo;m the SafeNet Assistant. Ask me about patients, beds, or today&rsquo;s summary — or tap a quick action below.', 'bot');
        greeted = true;
      }
      nodes.input.focus();
    }
    function close() { nodes.panel.classList.remove('open'); }

    nodes.fab.addEventListener('click', function () {
      nodes.panel.classList.contains('open') ? close() : open();
    });
    nodes.panel.querySelector('#aiClose').addEventListener('click', close);

    async function handle(text, intentOverride) {
      addMsg(nodes.body, escapeHtml(text), 'user');
      var thinking = el('<div class="ai-msg ai-msg-bot"><div class="ai-bubble">&hellip;</div></div>');
      nodes.body.appendChild(thinking);
      nodes.body.scrollTop = nodes.body.scrollHeight;

      var reply;
      var smallTalk = intentOverride ? null : smallTalkReply(text);
      if (smallTalk) {
        reply = smallTalk;
      } else {
        var intent = intentOverride || matchIntent(text);
        reply = await runIntent(intent, text);
      }
      thinking.remove();
      addMsg(nodes.body, reply, 'bot');
    }

    nodes.chips.addEventListener('click', function (e) {
      var btn = e.target.closest('.ai-chip');
      if (!btn) return;
      var label = btn.textContent;
      handle(label, btn.dataset.intent);
    });

    nodes.panel.querySelector('#aiSend').addEventListener('click', function () {
      var text = nodes.input.value.trim();
      if (!text) return;
      nodes.input.value = '';
      handle(text);
    });
    nodes.input.addEventListener('keydown', function (e) {
      if (e.key === 'Enter') {
        e.preventDefault();
        nodes.panel.querySelector('#aiSend').click();
      }
    });
  }

  function boot() {
    wire(build());
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }
})();
