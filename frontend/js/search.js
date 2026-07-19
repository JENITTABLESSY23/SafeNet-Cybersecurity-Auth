/* ============================================================
   SafeNet — Global Search
   Command-palette style search over app destinations. Scoped to
   static navigation for now (patients/doctors/medicines search
   would hit real endpoints once those APIs exist — see brief).
   ============================================================ */
(function () {
  'use strict';

  var INDEX = [
    { label: 'ICU Dashboard', sub: 'Ward monitoring & bed vitals', href: 'dashboard_icu.html', icon: 'grid' },
    { label: 'Cardiology Dashboard', sub: 'Ward monitoring & bed vitals', href: 'dashboard_cardio.html', icon: 'grid' },
    { label: 'Gynecology Dashboard', sub: 'Ward monitoring & bed vitals', href: 'dashboard_gynecology.html', icon: 'grid' },
    { label: 'Patient Records', sub: 'Search, add, and manage patients', href: 'patients.html', icon: 'user' },
    { label: 'Admin Console', sub: 'Access control & security', href: 'admin.html', icon: 'shield' },
    { label: 'Settings', sub: 'Profile, password, preferences', href: 'settings.html', icon: 'gear' },
    { label: 'Help & Support', sub: 'FAQs, contact, about SafeNet', href: 'help.html', icon: 'help' }
  ];

  function iconSvg() {
    return '<svg width="14" height="14" viewBox="0 0 16 16" fill="none"><circle cx="6.5" cy="6.5" r="4" stroke="currentColor" stroke-width="1.5"/><path d="M11 11l3 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>';
  }

  function filter(query) {
    var q = (query || '').trim().toLowerCase();
    if (!q) return INDEX;
    return INDEX.filter(function (item) {
      return item.label.toLowerCase().indexOf(q) !== -1 || item.sub.toLowerCase().indexOf(q) !== -1;
    });
  }

  function render(container, query, activeIndex) {
    var results = filter(query);
    if (!results.length) {
      container.innerHTML = '<div class="gsearch-empty">No matches for &ldquo;' + (query || '') + '&rdquo;</div>';
      return results;
    }
    container.innerHTML = results.map(function (item, i) {
      return '' +
        '<div class="gsearch-item' + (i === activeIndex ? ' active' : '') + '" data-href="' + item.href + '" data-idx="' + i + '">' +
          '<div class="gsearch-item-icon">' + iconSvg() + '</div>' +
          '<div><div>' + item.label + '</div><div class="gsearch-item-sub">' + item.sub + '</div></div>' +
        '</div>';
    }).join('');
    return results;
  }

  window.SafeNetSearch = { filter: filter, render: render, index: INDEX };
})();
