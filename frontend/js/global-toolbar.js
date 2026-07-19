/* ============================================================
   SafeNet — Global Toolbar
   Injects the fixed search/notification/clock/dark-mode cluster
   into every page. Layout-independent (works whether a page has
   a .topbar, a settings header, or nothing) so no per-page
   markup edits were needed beyond loading this script.
   Depends on: darkmode.js, notification.js, search.js, and
   effects.js (for the live clock tick via [data-live-clock]).
   ============================================================ */
(function () {
  'use strict';

  var ICONS = {
    search: '<svg viewBox="0 0 16 16" fill="none"><circle cx="6.5" cy="6.5" r="4" stroke="currentColor" stroke-width="1.5"/><path d="M11 11l3 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>',
    bell: '<svg viewBox="0 0 16 16" fill="none"><path d="M8 2a3 3 0 00-3 3v2.2c0 .5-.16 1-.46 1.4L3.5 10.5h9L11.46 8.6a2.3 2.3 0 01-.46-1.4V5a3 3 0 00-3-3z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/><path d="M6.3 13a1.8 1.8 0 003.4 0" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/></svg>',
    moon: '<svg class="gt-icon-moon" viewBox="0 0 16 16" fill="none"><path d="M13.5 9.5A5.5 5.5 0 016.5 2.5a5.5 5.5 0 105.6 7.9z" fill="currentColor"/></svg>',
    sun: '<svg class="gt-icon-sun" viewBox="0 0 16 16" fill="none"><circle cx="8" cy="8" r="3" fill="currentColor"/><path d="M8 1v2M8 13v2M1 8h2M13 8h2M3 3l1.4 1.4M11.6 11.6L13 13M3 13l1.4-1.4M11.6 4.4L13 3" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>',
    close: '<svg viewBox="0 0 16 16" fill="none"><path d="M4 4l8 8M12 4l-8 8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>'
  };

  function el(html) {
    var d = document.createElement('div');
    d.innerHTML = html.trim();
    return d.firstChild;
  }

  // Search + notifications only make sense once inside the authenticated
  // app shell (every such page has a sidebar). Pre-login pages — login,
  // register, forgot/reset password, logout — get just the clock + dark
  // toggle, so we never show hospital-ops data before sign-in.
  var isAppShell = !!document.querySelector('aside');

  function build() {
    var bar = el(
      '<div class="global-toolbar">' +
        '<span class="gt-clock" data-live-clock></span>' +
        (isAppShell ? '<button class="gt-btn" id="gtSearchBtn" title="Search (Ctrl+K)">' + ICONS.search + '</button>' : '') +
        (isAppShell ? '<button class="gt-btn" id="gtBellBtn" title="Notifications">' + ICONS.bell + '<span class="gt-dot" id="gtDot"></span></button>' : '') +
        '<button class="gt-btn" id="gtThemeBtn" title="Toggle dark mode">' + ICONS.moon + ICONS.sun + '</button>' +
      '</div>'
    );
    document.body.appendChild(bar);

    if (!isAppShell) {
      return { bar: bar, notifOverlay: null, notifPanel: null, searchOverlay: null };
    }

    // Notification panel
    var notifOverlay = el('<div class="gt-overlay" id="notifOverlay"></div>');
    var notifPanel = el(
      '<div class="notif-panel" id="notifPanel">' +
        '<div class="notif-head"><h3>Notifications</h3>' +
          '<div class="notif-head-actions">' +
            '<button id="notifMarkRead">Mark all read</button>' +
            '<button id="notifClear">Clear</button>' +
          '</div>' +
        '</div>' +
        '<div class="notif-list" id="notifList"></div>' +
      '</div>'
    );
    document.body.appendChild(notifOverlay);
    document.body.appendChild(notifPanel);

    // Search overlay
    var searchOverlay = el(
      '<div class="gsearch-overlay" id="gsearchOverlay">' +
        '<div class="gsearch-box">' +
          '<div class="gsearch-input-wrap">' + ICONS.search +
            '<input class="gsearch-input" id="gsearchInput" placeholder="Search dashboards, patients, settings…" autocomplete="off">' +
            '<span class="gsearch-hint">Esc</span>' +
          '</div>' +
          '<div class="gsearch-results" id="gsearchResults"></div>' +
        '</div>' +
      '</div>'
    );
    document.body.appendChild(searchOverlay);

    return { bar: bar, notifOverlay: notifOverlay, notifPanel: notifPanel, searchOverlay: searchOverlay };
  }

  function wire(nodes) {
    // Dark mode toggle — always present, regardless of shell.
    nodes.bar.querySelector('#gtThemeBtn').addEventListener('click', function () {
      if (window.SafeNetTheme) window.SafeNetTheme.toggle();
    });

    if (!isAppShell) return;

    var dot = nodes.bar.querySelector('#gtDot');

    function refreshDot() {
      if (window.SafeNetNotifications && window.SafeNetNotifications.unreadCount() > 0) {
        dot.classList.add('show');
      } else {
        dot.classList.remove('show');
      }
    }
    refreshDot();

    // Notifications
    var notifList = nodes.notifPanel.querySelector('#notifList');
    function openNotifs() {
      if (window.SafeNetNotifications) window.SafeNetNotifications.render(notifList);
      nodes.notifOverlay.classList.add('open');
      nodes.notifPanel.classList.add('open');
    }
    function closeNotifs() {
      nodes.notifOverlay.classList.remove('open');
      nodes.notifPanel.classList.remove('open');
    }
    nodes.bar.querySelector('#gtBellBtn').addEventListener('click', openNotifs);
    nodes.notifOverlay.addEventListener('click', closeNotifs);
    nodes.notifPanel.querySelector('#notifMarkRead').addEventListener('click', function () {
      window.SafeNetNotifications.markAllRead();
      window.SafeNetNotifications.render(notifList);
      refreshDot();
    });
    nodes.notifPanel.querySelector('#notifClear').addEventListener('click', function () {
      window.SafeNetNotifications.clearAll();
      window.SafeNetNotifications.render(notifList);
      refreshDot();
    });

    // Search
    var input = nodes.searchOverlay.querySelector('#gsearchInput');
    var results = nodes.searchOverlay.querySelector('#gsearchResults');
    var activeIdx = 0;
    var currentList = [];

    function openSearch() {
      nodes.searchOverlay.classList.add('open');
      input.value = '';
      activeIdx = 0;
      currentList = window.SafeNetSearch ? window.SafeNetSearch.render(results, '', 0) : [];
      setTimeout(function () { input.focus(); }, 50);
    }
    function closeSearch() {
      nodes.searchOverlay.classList.remove('open');
    }
    function goTo(idx) {
      var item = currentList[idx];
      if (item) window.location.href = item.href;
    }

    nodes.bar.querySelector('#gtSearchBtn').addEventListener('click', openSearch);
    nodes.searchOverlay.addEventListener('click', function (e) {
      if (e.target === nodes.searchOverlay) closeSearch();
    });
    input.addEventListener('input', function () {
      activeIdx = 0;
      currentList = window.SafeNetSearch.render(results, input.value, activeIdx);
    });
    results.addEventListener('click', function (e) {
      var item = e.target.closest('.gsearch-item');
      if (item) window.location.href = item.dataset.href;
    });
    input.addEventListener('keydown', function (e) {
      if (e.key === 'ArrowDown') { e.preventDefault(); activeIdx = Math.min(activeIdx + 1, currentList.length - 1); currentList = window.SafeNetSearch.render(results, input.value, activeIdx); }
      else if (e.key === 'ArrowUp') { e.preventDefault(); activeIdx = Math.max(activeIdx - 1, 0); currentList = window.SafeNetSearch.render(results, input.value, activeIdx); }
      else if (e.key === 'Enter') { e.preventDefault(); goTo(activeIdx); }
      else if (e.key === 'Escape') { closeSearch(); }
    });

    // Global shortcuts
    document.addEventListener('keydown', function (e) {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        openSearch();
      } else if (e.key === 'Escape') {
        closeNotifs();
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
