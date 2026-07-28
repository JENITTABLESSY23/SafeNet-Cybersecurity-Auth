/* ============================================================
   SafeNet — Mobile Navigation
   Injects a hamburger toggle + dimmed scrim, and wires them to
   slide the existing <aside> sidebar in/out as an off-canvas
   drawer on small screens (see the .mobile-nav-* rules and the
   aside off-canvas rules in design-system.css).

   Layout-independent, same pattern as global-toolbar.js — no
   per-page markup changes needed beyond loading this script.
   Pages with no <aside> (login, register, etc.) are skipped
   entirely, since there's nothing to open.
   ============================================================ */
(function () {
  'use strict';

  var aside = document.querySelector('aside');
  if (!aside) return;

  var scrim = document.createElement('div');
  scrim.className = 'mobile-nav-scrim';
  document.body.appendChild(scrim);

  var toggle = document.createElement('button');
  toggle.className = 'mobile-nav-toggle';
  toggle.type = 'button';
  toggle.setAttribute('aria-label', 'Open menu');
  toggle.innerHTML = '<svg width="18" height="18" viewBox="0 0 16 16" fill="none"><path d="M2 4h12M2 8h12M2 12h12" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>';
  document.body.appendChild(toggle);

  function openNav() {
    aside.classList.add('mobile-open');
    scrim.classList.add('open');
    document.body.style.overflow = 'hidden'; // don't let the page scroll behind the open drawer
  }
  function closeNav() {
    aside.classList.remove('mobile-open');
    scrim.classList.remove('open');
    document.body.style.overflow = '';
  }

  toggle.addEventListener('click', openNav);
  scrim.addEventListener('click', closeNav);

  // Closing on nav-item tap matters — without this, picking a link leaves
  // the drawer sitting open on top of the page you just navigated to.
  aside.addEventListener('click', function (e) {
    if (e.target.closest('.nav-item')) closeNav();
  });

  // Rotating a phone or resizing a window back past the drawer breakpoint
  // shouldn't leave the drawer stuck mid-transition into the desktop layout.
  window.addEventListener('resize', function () {
    if (window.innerWidth > 900) closeNav();
  });
})();
