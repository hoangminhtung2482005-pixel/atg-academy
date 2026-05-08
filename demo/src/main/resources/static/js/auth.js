/**
 * Shared Google Sign-In client and API auth wrapper.
 * - Persists the Google ID token in localStorage.
 * - Restores the UI session on every page load.
 * - Automatically attaches Authorization: Bearer <token> to same-origin /api/** requests.
 */

const GOOGLE_CLIENT_ID = '422848404584-2r1fgoo4uh06p10fbskvtrapvr0pgh28.apps.googleusercontent.com';
const GOOGLE_GSI_SCRIPT_SRC = 'https://accounts.google.com/gsi/client';
const STAFF_EMAILS = [];
const AUTH_STORAGE_KEY = 'aov_user';
const PROFILE_STORAGE_PREFIX = 'aov_profile_';
const AUTH_EXPIRY_SKEW_MS = 30 * 1000;
const ACCOUNT_LEVELS = ['Normal', 'Vip'];

const originalFetch = window.fetch ? window.fetch.bind(window) : null;
let googleIdentityInitialized = false;
let googleButtonRetryTimer = null;
let googleIdentityScriptPromise = null;
let authProfileRefreshPromise = null;

function escapeHtml(value) {
    return String(value || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function decodeJwtPayload(token) {
    if (!token || token.split('.').length < 2) {
        throw new Error('Google credential is not a valid JWT.');
    }

    const payloadBase64Url = token.split('.')[1];
    const payloadBase64 = payloadBase64Url
        .replace(/-/g, '+')
        .replace(/_/g, '/')
        .padEnd(payloadBase64Url.length + ((4 - payloadBase64Url.length % 4) % 4), '=');
    const payloadJson = decodeURIComponent(
        atob(payloadBase64)
            .split('')
            .map(char => '%' + ('00' + char.charCodeAt(0).toString(16)).slice(-2))
            .join('')
    );
    return JSON.parse(payloadJson);
}

function normalizeEmail(email) {
    return (email || '').trim().toLowerCase();
}

function getProfileStorageKey(email) {
    const normalizedEmail = normalizeEmail(email);
    return normalizedEmail ? `${PROFILE_STORAGE_PREFIX}${normalizedEmail}` : null;
}

function readStoredProfile(email) {
    const key = getProfileStorageKey(email);
    if (!key) return null;

    try {
        const rawProfile = localStorage.getItem(key);
        return rawProfile ? JSON.parse(rawProfile) : null;
    } catch (error) {
        localStorage.removeItem(key);
        return null;
    }
}

function writeStoredProfile(user) {
    const key = getProfileStorageKey(user?.email);
    if (!key) return;

    localStorage.setItem(key, JSON.stringify({
        displayName: user.displayName || user.name || 'User',
        level: normalizeAccountLevel(user.level)
    }));
}

function checkUserRole(userEmail) {
    const normalizedEmail = normalizeEmail(userEmail);
    if (!normalizedEmail) return 'Custom';
    if (normalizedEmail === 'hoangminhtung.2482005@gmail.com') return 'Admin';
    if (STAFF_EMAILS.map(normalizeEmail).includes(normalizedEmail)) return 'Staff';
    return 'User';
}

function normalizeAccountLevel(level) {
    return ACCOUNT_LEVELS.includes(level) ? level : 'Normal';
}

function normalizeAuthUser(user) {
    if (!user) return null;

    const displayName = (user.displayName || user.name || 'User').trim() || 'User';
    return {
        ...user,
        displayName,
        level: normalizeAccountLevel(user.level)
    };
}

function formatAccountDisplayName(user) {
    const normalizedUser = normalizeAuthUser(user);
    if (!normalizedUser) return 'User';

    const levelPrefix = normalizedUser.level === 'Normal' ? '' : `[${normalizedUser.level}] `;
    return `${levelPrefix}${normalizedUser.displayName}`;
}

function readStoredAuthUser() {
    const rawUser = localStorage.getItem(AUTH_STORAGE_KEY);
    if (!rawUser) return null;

    try {
        const user = normalizeAuthUser(JSON.parse(rawUser));
        if (user) {
            localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(user));
        }
        return user;
    } catch (error) {
        localStorage.removeItem(AUTH_STORAGE_KEY);
        return null;
    }
}

function isTokenExpired(user) {
    if (!user || !user.exp) return true;
    return (user.exp * 1000) <= (Date.now() + AUTH_EXPIRY_SKEW_MS);
}

function clearStoredAuthUser() {
    localStorage.removeItem(AUTH_STORAGE_KEY);
}

function getAuthUser() {
    const user = readStoredAuthUser();
    if (!user) return null;

    if (isTokenExpired(user)) {
        clearStoredAuthUser();
        return null;
    }

    return user;
}

function getAuthToken() {
    const user = getAuthUser();
    return user ? user.token : null;
}

function updateAuthProfile(updates) {
    const user = getAuthUser();
    if (!user) return null;

    const nextUser = normalizeAuthUser({
        ...user,
        displayName: typeof updates.displayName === 'string'
            ? updates.displayName.trim() || user.name || 'User'
            : user.displayName,
        level: updates.level || user.level
    });

    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(nextUser));
    writeStoredProfile(nextUser);
    syncAuthUiFromStorage();
    document.dispatchEvent(new CustomEvent('authChanged', { detail: { user: nextUser } }));
    return nextUser;
}

function applyAuthUserProfile(profile) {
    const user = getAuthUser();
    if (!user || !profile) return null;

    const nextUser = normalizeAuthUser({
        ...user,
        id: profile.id != null ? profile.id : user.id,
        email: profile.email || user.email,
        displayName: typeof profile.displayName === 'string' ? profile.displayName : user.displayName,
        role: profile.role || user.role,
        level: profile.level || user.level
    });

    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(nextUser));
    writeStoredProfile(nextUser);
    syncAuthUiFromStorage();
    document.dispatchEvent(new CustomEvent('authChanged', { detail: { user: nextUser } }));
    return nextUser;
}

async function readApiErrorMessage(response, fallbackMessage) {
    try {
        const payload = await response.json();
        if (payload && typeof payload.message === 'string' && payload.message.trim()) {
            return payload.message.trim();
        }
    } catch (error) {
        // Ignore invalid JSON error payloads and use the fallback message.
    }
    return fallbackMessage;
}

function refreshAuthUserProfile(options) {
    const user = getAuthUser();
    if (!user) {
        return Promise.resolve(null);
    }

    if (authProfileRefreshPromise) {
        return authProfileRefreshPromise;
    }

    const settings = options || {};
    authProfileRefreshPromise = fetch('/api/users/me/profile', { cache: 'no-store' })
        .then(async response => {
            if (!response.ok) {
                if (response.status === 401) {
                    return null;
                }
                const message = await readApiErrorMessage(response, 'Không thể tải thông tin tài khoản.');
                if (!settings.silent) {
                    console.error(message);
                }
                return null;
            }
            const profile = await response.json();
            return applyAuthUserProfile(profile);
        })
        .catch(error => {
            if (!settings.silent) {
                console.error('Cannot refresh authenticated user profile:', error);
            }
            return null;
        })
        .finally(() => {
            authProfileRefreshPromise = null;
        });

    return authProfileRefreshPromise;
}

function isAuthenticated() {
    return !!getAuthUser();
}

function userHasRole(role) {
    const user = getAuthUser();
    if (!user) return false;
    if (user.role === 'Admin') return true;
    return user.role === role;
}

function requireRoleAccess(requiredRoles, redirectUrl) {
    const roles = Array.isArray(requiredRoles) ? requiredRoles : [requiredRoles];
    const user = getAuthUser();
    const hasAccess = !!user && (user.role === 'Admin' || roles.includes(user.role));

    if (!hasAccess) {
        window.location.replace(redirectUrl || '/html/index.html');
        return false;
    }

    return true;
}

function applyRoleUI(role) {
    document.body.setAttribute('data-user-role', role);

    const protectedElements = document.querySelectorAll('[data-require-role]');
    protectedElements.forEach(element => {
        const requiredRoles = element.getAttribute('data-require-role').split(',');
        if (role === 'Admin' || requiredRoles.includes(role)) {
            element.style.display = '';
        } else {
            element.style.display = 'none';
        }
    });

    const customOnlyElements = document.querySelectorAll('[data-show-custom-only="true"]');
    customOnlyElements.forEach(element => {
        element.style.display = role === 'Custom' ? '' : 'none';
    });
}

function renderAuthenticatedUser(user) {
    const userSection = document.getElementById('user-section');
    if (!userSection) return;

    const buttonTarget = document.getElementById('google-login-button-target');
    if (buttonTarget) buttonTarget.style.display = 'none';

    const existingInfo = userSection.querySelector('.user-info');
    if (existingInfo) existingInfo.remove();

    const safeName = escapeHtml(formatAccountDisplayName(user));
    const safeEmail = escapeHtml(user.email || '');
    const safeRole = escapeHtml(user.role || 'User');
    const isAdmin = user.role === 'Admin';

    const userInfo = document.createElement('div');
    userInfo.className = 'user-info account-menu';
    userInfo.innerHTML = `
        <button class="account-trigger" type="button" onclick="toggleAccountMenu(event)" aria-haspopup="menu" aria-expanded="false" aria-label="Mở menu tài khoản">
            <span class="account-trigger-icon" aria-hidden="true">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                    <path d="M20 21a8 8 0 0 0-16 0" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                    <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2"/>
                </svg>
            </span>
            <span class="account-trigger-text">
                <span class="account-trigger-name">${safeName}</span>
                <span class="account-trigger-role">${safeRole}</span>
            </span>
            <span class="account-trigger-caret" aria-hidden="true">▾</span>
        </button>
        <div class="account-popup" role="menu" aria-label="Menu tài khoản">
            <div class="account-popup-profile">
                <span class="account-popup-avatar" aria-hidden="true">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
                        <path d="M20 21a8 8 0 0 0-16 0" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                        <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2"/>
                    </svg>
                </span>
                <span class="account-popup-identity">
                    <strong>${safeName}</strong>
                    <small>${safeEmail}</small>
                </span>
                <span class="account-popup-role">${safeRole}</span>
            </div>
            <div class="account-popup-actions">
                <a href="/html/account.html" class="account-popup-item" role="menuitem">
                    <span class="account-popup-item-icon">AC</span>
                    <span>
                        <strong>Account Dashboard</strong>
                        <small>Thông tin tài khoản và cấp độ</small>
                    </span>
                </a>
                ${isAdmin ? `
                <a href="/html/admin.html" class="account-popup-item account-popup-admin" role="menuitem">
                    <span class="account-popup-item-icon">AD</span>
                    <span>
                        <strong>Quản Trị</strong>
                        <small>Quản lý dữ liệu hệ thống</small>
                    </span>
                </a>` : ''}
                <button class="account-popup-item account-popup-logout" type="button" onclick="handleLogout()" role="menuitem">
                    <span class="account-popup-item-icon">LO</span>
                    <span>
                        <strong>Đăng xuất</strong>
                        <small>Kết thúc phiên hiện tại</small>
                    </span>
                </button>
            </div>
        </div>
    `;
    userSection.appendChild(userInfo);
}

function syncAuthUiFromStorage() {
    const user = getAuthUser();
    if (!user) {
        const userSection = document.getElementById('user-section');
        const existingInfo = userSection ? userSection.querySelector('.user-info') : null;
        if (existingInfo) existingInfo.remove();
        closeAccountMenu();
        const buttonTarget = document.getElementById('google-login-button-target');
        if (buttonTarget) buttonTarget.style.display = 'block';
        applyRoleUI('Custom');
        return false;
    }

    renderAuthenticatedUser(user);
    applyRoleUI(user.role || 'Custom');
    return true;
}

function handleGoogleLogin(response) {
    if (!response || !response.credential) {
        console.error('Google Sign-In did not return a credential.');
        return;
    }

    try {
        const payload = decodeJwtPayload(response.credential);
        const existingUser = readStoredAuthUser();
        const storedProfile = readStoredProfile(payload.email);
        const shouldPreserveProfile = existingUser && normalizeEmail(existingUser.email) === normalizeEmail(payload.email);
        const user = {
            name: payload.name,
            displayName: storedProfile?.displayName || (shouldPreserveProfile ? existingUser.displayName : payload.name),
            level: storedProfile?.level || (shouldPreserveProfile ? existingUser.level : 'Normal'),
            picture: payload.picture,
            email: payload.email,
            role: checkUserRole(payload.email),
            token: response.credential,
            exp: payload.exp
        };

        localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(user));
        writeStoredProfile(user);
        syncAuthUiFromStorage();
        document.dispatchEvent(new CustomEvent('authChanged', { detail: { user } }));
        refreshAuthUserProfile({ silent: true });
    } catch (error) {
        console.error('Cannot process Google credential:', error);
        clearStoredAuthUser();
        syncAuthUiFromStorage();
    }
}

function handleLogout() {
    closeAccountMenu();

    const userSection = document.getElementById('user-section');
    if (userSection) {
        const userInfo = userSection.querySelector('.user-info');
        if (userInfo) userInfo.remove();
    }

    clearStoredAuthUser();

    const buttonTarget = document.getElementById('google-login-button-target');
    if (buttonTarget) {
        buttonTarget.style.display = 'block';
        if (buttonTarget.innerHTML === '') {
            renderGoogleButton();
        }
    }

    if (typeof google !== 'undefined' && google.accounts) {
        google.accounts.id.disableAutoSelect();
    }

    applyRoleUI('Custom');
    document.dispatchEvent(new CustomEvent('authChanged', { detail: { user: null } }));
}

function closeAccountMenu() {
    const openMenu = document.querySelector('.account-menu .account-popup.is-open');
    if (!openMenu) return;

    openMenu.classList.remove('is-open');
    const trigger = openMenu.closest('.account-menu')?.querySelector('.account-trigger');
    if (trigger) trigger.setAttribute('aria-expanded', 'false');
}

function toggleAccountMenu(event) {
    if (event) event.stopPropagation();

    const trigger = event?.currentTarget || document.querySelector('.account-menu .account-trigger');
    if (!trigger) return;

    const menu = trigger.closest('.account-menu')?.querySelector('.account-popup');
    if (!menu) return;

    const shouldOpen = !menu.classList.contains('is-open');
    closeAccountMenu();

    if (shouldOpen) {
        menu.classList.add('is-open');
        trigger.setAttribute('aria-expanded', 'true');
    }
}

function getGoogleButtonTarget() {
    return document.getElementById('google-login-button-target');
}

function showGoogleLoginError(message) {
    const buttonTarget = getGoogleButtonTarget();
    if (!buttonTarget) return;

    buttonTarget.style.display = 'block';
    buttonTarget.innerHTML = `
        <button type="button" class="google-login-fallback-btn" onclick="renderGoogleButton()">
            Sign in with Google
        </button>
        <div class="google-login-error">${message}</div>
    `;
}

function waitForGoogleIdentity() {
    return new Promise((resolve, reject) => {
        let attempts = 0;
        const timer = setInterval(() => {
            if (window.google && window.google.accounts && window.google.accounts.id) {
                clearInterval(timer);
                resolve(true);
                return;
            }

            attempts += 1;
            if (attempts >= 80) {
                clearInterval(timer);
                reject(new Error('Google Identity Services did not become available.'));
            }
        }, 100);
    });
}

function loadGoogleIdentityScript() {
    if (window.google && window.google.accounts && window.google.accounts.id) {
        return Promise.resolve(true);
    }

    if (googleIdentityScriptPromise) {
        return googleIdentityScriptPromise;
    }

    googleIdentityScriptPromise = new Promise((resolve, reject) => {
        let settled = false;
        let script = document.querySelector(`script[src^="${GOOGLE_GSI_SCRIPT_SRC}"]`);

        function resolveOnce(value) {
            if (settled) return;
            settled = true;
            clearTimeout(timeout);
            resolve(value);
        }

        function rejectOnce(error) {
            if (settled) return;
            settled = true;
            clearTimeout(timeout);
            googleIdentityScriptPromise = null;
            reject(error);
        }

        const timeout = setTimeout(() => {
            rejectOnce(new Error('Google Identity Services script timed out.'));
        }, 10000);

        if (!script) {
            script = document.createElement('script');
            script.src = GOOGLE_GSI_SCRIPT_SRC;
            script.async = true;
            script.defer = true;
            document.head.appendChild(script);
        }

        script.addEventListener('load', () => {
            waitForGoogleIdentity().then(resolveOnce).catch(rejectOnce);
        }, { once: true });

        script.addEventListener('error', () => {
            rejectOnce(new Error('Cannot load Google Identity Services script.'));
        }, { once: true });

        waitForGoogleIdentity().then(resolveOnce).catch(() => {});
    });

    return googleIdentityScriptPromise;
}

function initializeGoogleIdentity() {
    if (!window.google || !window.google.accounts || !window.google.accounts.id) {
        return false;
    }

    if (!googleIdentityInitialized) {
        window.google.accounts.id.initialize({
            client_id: GOOGLE_CLIENT_ID,
            callback: window.handleGoogleLogin || handleGoogleLogin,
            auto_select: false
        });
        googleIdentityInitialized = true;
    }

    return true;
}

async function renderGoogleButton() {
    const buttonTarget = getGoogleButtonTarget();
    if (!buttonTarget) return;

    if (googleButtonRetryTimer) {
        clearTimeout(googleButtonRetryTimer);
        googleButtonRetryTimer = null;
    }

    if (syncAuthUiFromStorage()) {
        buttonTarget.style.display = 'none';
        return;
    }

    buttonTarget.style.display = 'block';
    applyRoleUI('Custom');

    try {
        await loadGoogleIdentityScript();
    } catch (error) {
        console.error(error);
        showGoogleLoginError('Không tải được Google Sign-In. Kiểm tra kết nối mạng hoặc extension chặn script Google.');
        return;
    }

    if (!initializeGoogleIdentity()) {
        googleButtonRetryTimer = setTimeout(renderGoogleButton, 100);
        return;
    }

    buttonTarget.innerHTML = '';
    window.google.accounts.id.renderButton(buttonTarget, {
        type: 'standard',
        theme: 'outline',
        size: 'medium',
        text: 'signin',
        shape: 'rectangular',
        logo_alignment: 'left',
        width: 120
    });
}

function handleCustomGoogleLogin() {
    loadGoogleIdentityScript()
        .then(() => {
            if (!initializeGoogleIdentity()) {
                console.warn('Google Identity Services chưa load xong. Vui lòng thử lại.');
                return;
            }

            window.google.accounts.id.prompt(notification => {
                if (notification.isNotDisplayed() || notification.isSkippedMoment()) {
                    console.log(
                        'One Tap not displayed, reason:',
                        notification.getNotDisplayedReason?.() || notification.getSkippedReason?.()
                    );
                }
            });
        })
        .catch(error => {
            console.error(error);
            showGoogleLoginError('Không tải được Google Sign-In. Kiểm tra kết nối mạng hoặc extension chặn script Google.');
        });
}

function isApiRequest(input) {
    const requestUrl = typeof input === 'string' || input instanceof URL ? input.toString() : input.url;
    const resolvedUrl = new URL(requestUrl, window.location.origin);
    return resolvedUrl.origin === window.location.origin && resolvedUrl.pathname.startsWith('/api/');
}

function buildAuthorizedRequestInit(input, init) {
    const token = getAuthToken();
    const headers = new Headers(
        init && init.headers
            ? init.headers
            : (input instanceof Request ? input.headers : undefined)
    );

    if (token && !headers.has('Authorization')) {
        headers.set('Authorization', `Bearer ${token}`);
    }

    return { ...(init || {}), headers };
}

if (originalFetch && !window.__aovFetchPatched) {
    window.fetch = async function patchedFetch(input, init) {
        const isApiCall = isApiRequest(input);
        const response = await originalFetch(
            input,
            isApiCall ? buildAuthorizedRequestInit(input, init) : init
        );

        if (isApiCall && response.status === 401 && getAuthUser()) {
            handleLogout();
            document.dispatchEvent(new CustomEvent('authExpired'));
        }

        return response;
    };
    window.__aovFetchPatched = true;
}

window.getAuthUser = getAuthUser;
window.getAuthToken = getAuthToken;
window.formatAccountDisplayName = formatAccountDisplayName;
window.updateAuthProfile = updateAuthProfile;
window.applyAuthUserProfile = applyAuthUserProfile;
window.isAuthenticated = isAuthenticated;
window.userHasRole = userHasRole;
window.requireRoleAccess = requireRoleAccess;
window.syncAuthUiFromStorage = syncAuthUiFromStorage;
window.refreshAuthUserProfile = refreshAuthUserProfile;
window.handleGoogleLogin = handleGoogleLogin;
window.handleCustomGoogleLogin = handleCustomGoogleLogin;
window.handleLogout = handleLogout;
window.renderGoogleButton = renderGoogleButton;
window.toggleAccountMenu = toggleAccountMenu;
window.closeAccountMenu = closeAccountMenu;

document.addEventListener('headerLoaded', renderGoogleButton);
document.addEventListener('click', event => {
    if (!event.target.closest('.account-menu')) {
        closeAccountMenu();
    }
});
document.addEventListener('keydown', event => {
    if (event.key === 'Escape') {
        closeAccountMenu();
    }
});

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
        if (syncAuthUiFromStorage()) {
            refreshAuthUserProfile({ silent: true });
        }
    });
} else {
    if (syncAuthUiFromStorage()) {
        refreshAuthUserProfile({ silent: true });
    }
}
