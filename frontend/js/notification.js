/* ============================================================
   SafeNet — Notification Center
   Client-side notification list, persisted in localStorage so
   read/unread + clears survive a refresh. Seeded with realistic
   hospital-ops items on first run. Not wired to a backend feed
   yet — swap `seed()` for a real API call when one exists.
   ============================================================ */
(function () {
  'use strict';
  var KEY = 'safenet-notifications';

  function seed() {
    var now = Date.now();
    return [
      { id: 'n1', type: 'critical', title: 'ICU Bed 4 — vitals anomaly flagged by IoT monitor', time: now - 6 * 60000, unread: true },
      { id: 'n2', type: 'warn', title: 'Cardiology — patient discharge pending review', time: now - 45 * 60000, unread: true },
      { id: 'n3', type: 'info', title: 'Nightly audit report generated (AUDIT.md)', time: now - 3 * 3600000, unread: true },
      { id: 'n4', type: 'info', title: 'New user registered: Gynecology ward staff', time: now - 26 * 3600000, unread: false }
    ];
  }

  function load() {
    try {
      var raw = localStorage.getItem(KEY);
      if (!raw) { var s = seed(); localStorage.setItem(KEY, JSON.stringify(s)); return s; }
      return JSON.parse(raw);
    } catch (e) { return seed(); }
  }

  function save(list) {
    localStorage.setItem(KEY, JSON.stringify(list));
  }

  function unreadCount() {
    return load().filter(function (n) { return n.unread; }).length;
  }

  function markAllRead() {
    var list = load().map(function (n) { n.unread = false; return n; });
    save(list);
  }

  function clearAll() {
    save([]);
  }

  function timeAgo(ts) {
    var diff = Math.max(0, Date.now() - ts);
    var mins = Math.round(diff / 60000);
    if (mins < 1) return 'just now';
    if (mins < 60) return mins + 'm ago';
    var hrs = Math.round(mins / 60);
    if (hrs < 24) return hrs + 'h ago';
    return Math.round(hrs / 24) + 'd ago';
  }

  var ICONS = {
    critical: '&#9888;',
    warn: '&#9679;',
    info: '&#8505;'
  };

  function render(container) {
    var list = load();
    if (!list.length) {
      container.innerHTML = '<div class="notif-empty">You&rsquo;re all caught up.</div>';
      return;
    }
    container.innerHTML = list.map(function (n) {
      return '' +
        '<div class="notif-item' + (n.unread ? ' unread' : '') + '">' +
          '<div class="notif-icon ' + n.type + '">' + ICONS[n.type] + '</div>' +
          '<div class="notif-body">' +
            '<div class="notif-title">' + n.title + '</div>' +
            '<div class="notif-meta">' + timeAgo(n.time) + '</div>' +
          '</div>' +
        '</div>';
    }).join('');
  }

  window.SafeNetNotifications = {
    load: load,
    unreadCount: unreadCount,
    markAllRead: markAllRead,
    clearAll: clearAll,
    render: render
  };
})();
