/* ============================================================
   SafeNet — shared visual effects
   Purely additive: does not touch app/auth logic or state.
   ============================================================ */
(function () {
  'use strict';

  /* ---------- Live pulse / ECG trace renderer ----------
     Draws a looping heartbeat waveform onto any <canvas class="pulse-trace">.
     data-color / data-height attributes optionally override defaults. */
  function initPulseTraces() {
    document.querySelectorAll('canvas.pulse-trace').forEach((canvas) => {
      const ctx = canvas.getContext('2d');
      const color = canvas.dataset.color || '#55D9C9';
      const dpr = window.devicePixelRatio || 1;
      let width, height, t = Math.random() * 1000;

      function resize() {
        width = canvas.clientWidth;
        height = canvas.clientHeight;
        canvas.width = width * dpr;
        canvas.height = height * dpr;
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      }
      resize();
      window.addEventListener('resize', resize);

      // One heartbeat "unit" shape sampled as relative points across x in [0,1]
      function beat(x) {
        // flat baseline with a P-QRS-T style blip repeating every unit
        const local = x % 1;
        if (local < 0.55) return 0;
        if (local < 0.60) return -0.15;
        if (local < 0.64) return 0.9;
        if (local < 0.68) return -0.55;
        if (local < 0.76) return 0.12;
        if (local < 0.84) return 0.22;
        return 0;
      }

      function draw() {
        ctx.clearRect(0, 0, width, height);
        const midY = height * 0.55;
        const amp = height * 0.38;
        const speed = 55; // px per frame-unit
        const unitPx = 90;

        ctx.beginPath();
        ctx.lineWidth = 2;
        ctx.strokeStyle = color;
        ctx.shadowColor = color;
        ctx.shadowBlur = 8;
        ctx.lineJoin = 'round';
        ctx.lineCap = 'round';

        for (let px = 0; px <= width; px += 2) {
          const x = (px + t) / unitPx;
          const y = midY - beat(x) * amp;
          if (px === 0) ctx.moveTo(px, y);
          else ctx.lineTo(px, y);
        }
        ctx.stroke();

        // traveling dot at the leading edge
        const leadX = width - 2;
        const leadVal = beat((leadX + t) / unitPx);
        ctx.beginPath();
        ctx.fillStyle = color;
        ctx.shadowBlur = 14;
        ctx.arc(leadX, midY - leadVal * amp, 3, 0, Math.PI * 2);
        ctx.fill();

        t += speed / 30;
        requestAnimationFrame(draw);
      }
      draw();
    });
  }

  /* ---------- Ripple effect on .rippleable elements ---------- */
  function initRipples() {
    document.addEventListener('click', (e) => {
      const el = e.target.closest('.rippleable, button[class*="btn"], a.btn-primary, a.btn-login, a[class*="btn-"]');
      if (!el || el.disabled) return;
      const rect = el.getBoundingClientRect();
      const size = Math.max(rect.width, rect.height) * 1.4;
      const span = document.createElement('span');
      span.className = 'ripple-el';
      span.style.width = span.style.height = size + 'px';
      span.style.left = (e.clientX - rect.left - size / 2) + 'px';
      span.style.top = (e.clientY - rect.top - size / 2) + 'px';
      const prevPos = getComputedStyle(el).position;
      if (prevPos === 'static') el.style.position = 'relative';
      el.style.overflow = 'hidden';
      el.appendChild(span);
      span.addEventListener('animationend', () => span.remove());
    });
  }

  /* ---------- Reveal-on-scroll ---------- */
  function initReveal() {
    const items = document.querySelectorAll('.reveal');
    if (!items.length) return;
    const io = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('in-view');
          io.unobserve(entry.target);
        }
      });
    }, { threshold: 0.12 });
    items.forEach((el) => io.observe(el));
  }

  /* ---------- Count-up numbers: <span data-count-to="128">0</span> ---------- */
  function initCountUp() {
    document.querySelectorAll('[data-count-to]').forEach((el) => {
      const to = parseFloat(el.dataset.countTo);
      const decimals = (el.dataset.countTo.split('.')[1] || '').length;
      const dur = 900;
      const start = performance.now();
      function tick(now) {
        const p = Math.min(1, (now - start) / dur);
        const eased = 1 - Math.pow(1 - p, 3);
        el.textContent = (to * eased).toFixed(decimals);
        if (p < 1) requestAnimationFrame(tick);
      }
      requestAnimationFrame(tick);
    });
  }

  /* ---------- Live clock: <span data-live-clock></span> ---------- */
  function initLiveClock() {
    const els = document.querySelectorAll('[data-live-clock]');
    if (!els.length) return;
    function tick() {
      const now = new Date();
      const str = now.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
      els.forEach((el) => (el.textContent = str));
    }
    tick();
    setInterval(tick, 1000);
  }

  function boot() {
    initPulseTraces();
    initRipples();
    initReveal();
    initCountUp();
    initLiveClock();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }
})();
