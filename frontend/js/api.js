/**
 * SafeNet frontend API client.
 *
 * Talks to the Spring Boot backend at API_BASE. Handles JWT storage,
 * attaching the Authorization header on every request, redirecting to
 * login on 401/expired sessions, and mapping a user's department to the
 * dashboard they land on after login.
 *
 * Included on every page via <script src="js/api.js"></script>, before
 * that page's own inline script.
 */
// Auto-picks the right backend depending on where these HTML files are
// being served from, so the exact same files work unmodified both on
// your machine and once deployed — no build step needed.
//
//   - Opened locally (file://, or served from localhost/127.0.0.1)
//     -> talks to the backend on localhost:8080, same as always.
//   - Served from anywhere else (your deployed frontend's real URL)
//     -> talks to DEPLOYED_API_BASE below.
//
// >>> After you deploy the backend, put its URL here (with /api on the
//     end), e.g. 'https://safenet-backend.onrender.com/api' <<<
const DEPLOYED_API_BASE = 'https://safenet-backend-0h1n.onrender.com/api';

const API_BASE = (
    location.hostname === 'localhost' ||
    location.hostname === '127.0.0.1' ||
    location.protocol === 'file:'
) ? 'http://localhost:8080/api' : DEPLOYED_API_BASE;

const SafeNetAPI = (() => {

    function getToken() { return sessionStorage.getItem('sn_token'); }

    function getUser() {
        const raw = sessionStorage.getItem('sn_user');
        return raw ? JSON.parse(raw) : null;
    }

    function setSession(token, user) {
        sessionStorage.setItem('sn_token', token);
        sessionStorage.setItem('sn_user', JSON.stringify(user));
    }

    function clearSession() {
        sessionStorage.removeItem('sn_token');
        sessionStorage.removeItem('sn_user');
    }

    /**
     * Wraps fetch() with the API base URL, JSON handling, and the bearer
     * token. Every backend response is wrapped in { success, message, data,
     * error } (see ApiResponse.java) — this unwraps that envelope and
     * throws with the server's own error message on failure, so callers
     * can just `catch (e) { showError(e.message) }`.
     */
    async function request(path, options = {}) {
        const headers = Object.assign(
            { 'Accept': 'application/json' },
            options.headers || {}
        );
        const token = getToken();
        if (token) headers['Authorization'] = 'Bearer ' + token;

        // Don't force a Content-Type when sending FormData — the browser
        // needs to set its own multipart boundary.
        if (options.body && !(options.body instanceof FormData) && !headers['Content-Type']) {
            headers['Content-Type'] = 'application/json';
        }

        let res;
        try {
            res = await fetch(API_BASE + path, { ...options, headers });
        } catch (networkErr) {
            throw new Error('Could not reach the SafeNet server. Is the backend running on localhost:8080?');
        }

        if (res.status === 401) {
            clearSession();
            if (!path.startsWith('/auth/login')) {
                window.location.href = 'login.html?expired=1';
            }
        }

        let body;
        try { body = await res.json(); }
        catch { body = null; }

        if (!res.ok || (body && body.success === false)) {
            const msg = (body && (body.error || body.message)) || `Request failed (${res.status})`;
            throw new Error(msg);
        }
        return body ? body.data : null;
    }

    const get    = (path)         => request(path, { method: 'GET' });
    const del    = (path)         => request(path, { method: 'DELETE' });
    const post   = (path, body)   => request(path, { method: 'POST',  body: body instanceof FormData ? body : JSON.stringify(body) });
    const put    = (path, body)   => request(path, { method: 'PUT',   body: body instanceof FormData ? body : JSON.stringify(body) });

    /**
     * For endpoints that return raw file bytes (e.g. a registrant's uploaded
     * ID proof) rather than the usual { success, data } JSON envelope —
     * request() above always calls res.json(), which would fail on binary
     * content. Fetches the file with the same auth header, then opens it in
     * a new tab via a temporary object URL.
     */
    async function viewFile(path) {
        const token = getToken();
        const headers = token ? { 'Authorization': 'Bearer ' + token } : {};
        let res;
        try {
            res = await fetch(API_BASE + path, { headers });
        } catch (networkErr) {
            throw new Error('Could not reach the SafeNet server.');
        }
        if (!res.ok) {
            let msg = `Request failed (${res.status})`;
            try { const body = await res.json(); msg = body.error || body.message || msg; } catch {}
            throw new Error(msg);
        }
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
        // Revoke well after the new tab has had time to load it — revoking
        // immediately can race the new tab's fetch of the blob: URL.
        setTimeout(() => URL.revokeObjectURL(url), 60000);
    }

    async function login(username, hospitalId, password) {
        const data = await post('/auth/login', { username, hospitalId, password });
        setSession(data.token, {
            username: data.username, hospitalId: data.hospitalId,
            department: data.department, role: data.role, fullName: data.fullName,
        });
        sessionStorage.setItem('sn_login_time', Date.now().toString());
        return data;
    }

    /** Navigates to the logout confirmation screen, which does the actual sign-out work. */
    function logout() {
        window.location.href = 'logout.html';
    }

    /**
     * Does the actual sign-out: invalidates the token server-side (best
     * effort — if the network call fails we still clear locally, since the
     * user's intent to log out shouldn't be blocked by connectivity) and
     * clears the local session. Called from logout.html.
     */
    async function finishLogout() {
        let serverConfirmed = true;
        try { await post('/auth/logout', {}); }
        catch { serverConfirmed = false; /* clear locally regardless */ }
        clearSession();
        sessionStorage.removeItem('sn_login_time');
        return serverConfirmed;
    }

    /** Maps a user's department (as stored by the backend) to their dashboard. */
    function dashboardFor(department) {
        switch ((department || '').toLowerCase()) {
            case 'icu':         return 'dashboard_icu.html';
            case 'cardiology':  return 'dashboard_cardio.html';
            case 'gynecology':  return 'dashboard_gynecology.html';
            case 'admin':       return 'admin.html';
            default:            return 'patients.html';
        }
    }

    /** Call at the top of any protected page. Redirects to login if no session exists. */
    function requireAuth() {
        if (!getToken()) {
            window.location.href = 'login.html';
            return null;
        }
        return getUser();
    }

    async function forgotPassword(email) {
        return post('/auth/forgot-password', { email });
    }

    async function resetPassword(token, newPassword) {
        return post('/auth/reset-password', { token, newPassword });
    }

    return { get, post, put, delete: del, viewFile, login, logout, finishLogout, forgotPassword, resetPassword, requireAuth, getUser, getToken, dashboardFor };
})();
