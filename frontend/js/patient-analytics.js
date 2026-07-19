/**
 * SafeNet — patient health-status analytics.
 *
 * Renders a small donut chart + legend breaking patients down by clinical
 * status: Stable ("Normal"), Watch ("Under Supervision"), Critical, and
 * anything else (e.g. Discharged) grouped as "Other".
 *
 * Data always comes from GET /api/patients, which is department-scoped
 * server-side (see PatientController.callerDepartment()) — so a Cardiology
 * dashboard's chart can only ever reflect Cardiology patients, and the
 * admin console (Admin_Ops, unrestricted) sees the hospital-wide picture.
 * This module never re-implements or second-guesses that scoping; it just
 * renders whatever the endpoint legitimately returns.
 */
const PatientAnalytics = (() => {

    const STATUS_META = {
        Stable:   { label: 'Normal',            color: '#17A184' },
        Watch:    { label: 'Under Supervision', color: '#DB9A2C' },
        Critical: { label: 'Critical',           color: '#E3435F' },
    };
    const OTHER_COLOR = '#5B6B82';

    function statusMeta(key) {
        return STATUS_META[key] || { label: key, color: OTHER_COLOR };
    }

    function computeCounts(patients) {
        const counts = {};
        patients.forEach(p => { counts[p.status] = (counts[p.status] || 0) + 1; });
        return counts;
    }

    function orderedKeys(counts) {
        const known = Object.keys(STATUS_META).filter(k => counts[k]);
        const other = Object.keys(counts).filter(k => !STATUS_META[k]);
        return known.concat(other);
    }

    function drawDonut(canvas, counts, total) {
        const dpr = window.devicePixelRatio || 1;
        const size = canvas.clientWidth || 120;
        canvas.width = size * dpr;
        canvas.height = size * dpr;
        const ctx = canvas.getContext('2d');
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        ctx.clearRect(0, 0, size, size);

        const cx = size / 2, cy = size / 2;
        const lineWidth = size * 0.17;
        const r = size / 2 - lineWidth / 2 - 2;

        // Track ring
        ctx.beginPath();
        ctx.arc(cx, cy, r, 0, Math.PI * 2);
        ctx.strokeStyle = '#EDF1F7';
        ctx.lineWidth = lineWidth;
        ctx.stroke();

        let start = -Math.PI / 2;
        orderedKeys(counts).forEach(key => {
            const val = counts[key] || 0;
            if (!val) return;
            const frac = val / total;
            const end = start + frac * Math.PI * 2;
            ctx.beginPath();
            ctx.arc(cx, cy, r, start, end);
            ctx.strokeStyle = statusMeta(key).color;
            ctx.lineWidth = lineWidth;
            ctx.lineCap = total === val ? 'round' : 'butt';
            ctx.stroke();
            start = end;
        });

        ctx.fillStyle = '#0B1626';
        ctx.font = '700 21px Inter, sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(String(total), cx, cy - 7);
        ctx.font = '500 10px Inter, sans-serif';
        ctx.fillStyle = '#5B6B82';
        ctx.fillText(total === 1 ? 'patient' : 'patients', cx, cy + 11);
    }

    function renderLegend(container, counts, total) {
        container.innerHTML = orderedKeys(counts).map(key => {
            const val = counts[key] || 0;
            const meta = statusMeta(key);
            const pct = total ? Math.round((val / total) * 100) : 0;
            return `<div class="ph-legend-row">
                <span class="ph-dot" style="background:${meta.color}"></span>
                <span class="ph-legend-label">${meta.label}</span>
                <span class="ph-legend-val">${val} <span class="ph-legend-pct">(${pct}%)</span></span>
            </div>`;
        }).join('');
    }

    /**
     * ids: { canvas, legend, empty, skeleton, assignedLabel? }
     * getPatients: () => Promise<Patient[]> | Patient[]  (defaults to GET /patients)
     */
    async function render(ids, getPatients) {
        const canvas   = document.getElementById(ids.canvas);
        const legend   = document.getElementById(ids.legend);
        const empty    = ids.empty ? document.getElementById(ids.empty) : null;
        const skeleton = ids.skeleton ? document.getElementById(ids.skeleton) : null;

        try {
            const fetcher = getPatients || (() => SafeNetAPI.get('/patients'));
            const result = fetcher();
            const patients = result instanceof Promise ? await result : result;

            if (skeleton) skeleton.style.display = 'none';

            if (!patients || !patients.length) {
                if (canvas) canvas.style.display = 'none';
                if (legend) legend.innerHTML = '';
                if (empty) empty.style.display = 'block';
                if (ids.assignedLabel) document.getElementById(ids.assignedLabel).textContent = '0';
                return;
            }

            if (empty) empty.style.display = 'none';
            const counts = computeCounts(patients);
            if (canvas) { canvas.style.display = 'block'; drawDonut(canvas, counts, patients.length); }
            if (legend) renderLegend(legend, counts, patients.length);
            if (ids.assignedLabel) document.getElementById(ids.assignedLabel).textContent = String(patients.length);
        } catch (err) {
            if (skeleton) { skeleton.style.display = 'block'; skeleton.textContent = 'Could not load analytics: ' + err.message; }
            console.error('Patient analytics failed:', err.message);
        }
    }

    return { render };
})();
