/* ============================================================
   SafeNet — Dark Mode
   Loaded synchronously in <head>, right after design-system.css,
   so the saved theme is applied before first paint (no flash).
   Exposes window.SafeNetTheme for the toolbar toggle button.
   ============================================================ */
(function () {
  'use strict';
  var KEY = 'safenet-theme';

  function get() {
    return localStorage.getItem(KEY) || 'light';
  }

  function apply(theme) {
    document.documentElement.setAttribute('data-theme', theme);
  }

  function set(theme) {
    localStorage.setItem(KEY, theme);
    apply(theme);
  }

  function toggle() {
    var next = get() === 'dark' ? 'light' : 'dark';
    set(next);
    return next;
  }

  // Apply immediately — this script must load before <body> renders.
  apply(get());

  window.SafeNetTheme = { get: get, set: set, toggle: toggle };
})();
