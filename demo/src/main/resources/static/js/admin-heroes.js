const DEFAULT_ROLE_OPTIONS = [
    { id: null, code: 'DSL', name: 'Đường Tà Thần' },
    { id: null, code: 'JGL', name: 'Đi Rừng' },
    { id: null, code: 'MID', name: 'Đường Giữa' },
    { id: null, code: 'ADL', name: 'Đường Rồng' },
    { id: null, code: 'SUP', name: 'Trợ Thủ' }
];

const DEFAULT_CLASS_OPTIONS = ['Đấu sĩ', 'Sát thủ', 'Pháp sư', 'Xạ thủ', 'Đỡ đòn', 'Trợ thủ'];

const state = {
    heroes: [],
    roles: DEFAULT_ROLE_OPTIONS.slice(),
    classes: DEFAULT_CLASS_OPTIONS.slice(),
    attributes: [],
    attributeLoadError: '',
    filters: {
        search: '',
        className: '',
        primaryRole: ''
    },
    editingHero: null
};

function byId(id) {
    return document.getElementById(id);
}

function escapeHtml(value) {
    return String(value == null ? '' : value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function normalizeText(value) {
    return String(value == null ? '' : value)
        .trim()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/[đĐ]/g, 'd')
        .toLowerCase();
}

function normalizeAssetUrl(url) {
    if (!url) return '';
    if (/^(https?:)?\/\//i.test(url) || url.startsWith('data:') || url.startsWith('/')) {
        return url;
    }
    return '/' + url.replace(/^\.?\//, '');
}

function normalizeRole(role) {
    if (!role) return null;
    if (typeof role === 'string') return { id: null, code: role, name: role };
    return {
        id: role.id == null ? null : Number(role.id),
        code: role.code || '',
        name: role.name || role.code || ''
    };
}

function roleLabel(role) {
    if (!role) return '-';
    const code = role.code || '';
    const name = role.name || '';
    return code && name && code !== name ? `${code} - ${name}` : code || name || '-';
}

function getHeroPrimaryRole(hero) {
    const primary = normalizeRole(hero && hero.primaryRole);
    if (primary && (primary.code || primary.name)) return primary;
    const legacyRoles = Array.isArray(hero && hero.roles) ? hero.roles.map(normalizeRole).filter(Boolean) : [];
    return legacyRoles.length ? legacyRoles[0] : null;
}

function getHeroSubRoles(hero) {
    if (Array.isArray(hero && hero.subRoles)) {
        return hero.subRoles.map(normalizeRole).filter(Boolean);
    }
    const primary = getHeroPrimaryRole(hero);
    const primaryCode = primary && primary.code;
    return (Array.isArray(hero && hero.roles) ? hero.roles : [])
        .map(normalizeRole)
        .filter(role => role && role.code && role.code !== primaryCode);
}

function getHeroPrimaryRoleCode(hero) {
    const primary = getHeroPrimaryRole(hero);
    return primary && primary.code ? primary.code : '';
}

function getHeroClasses(hero) {
    if (Array.isArray(hero && hero.classes) && hero.classes.length) {
        return hero.classes.filter(Boolean);
    }
    return hero && hero.heroClass ? [hero.heroClass] : [];
}

function getHeroAttributes(hero) {
    return Array.isArray(hero && hero.attributes) ? hero.attributes.filter(Boolean) : [];
}

function getHeroBanPickScoreValue(hero) {
    const score = Number(hero && hero.banPickScore);
    return Number.isFinite(score) && score >= 0 ? score : 0;
}

function formatBanPickScoreDisplay(value) {
    const score = Number(value);
    const safeScore = Number.isFinite(score) && score >= 0 ? score : 0;
    return safeScore.toLocaleString('vi-VN', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
    });
}

function formatBanPickScoreInputValue(value) {
    const safeScore = getHeroBanPickScoreValue({ banPickScore: value });
    if (Number.isInteger(safeScore)) return String(safeScore);
    return safeScore.toFixed(2).replace(/0+$/, '').replace(/\.$/, '');
}

function validateBanPickScoreInput() {
    const input = byId('hf-ban-pick-score');
    if (!input) return { valid: true, value: 0 };

    const rawValue = String(input.value == null ? '' : input.value).trim();
    if (!rawValue) {
        updateError('hf-ban-pick-score-error', 'Điểm Ban/Pick là bắt buộc.');
        return { valid: false, value: 0 };
    }

    const normalizedValue = rawValue.replace(',', '.');
    if (!/^\d+(?:\.\d{1,2})?$/.test(normalizedValue)) {
        updateError('hf-ban-pick-score-error', 'Điểm Ban/Pick phải là số và chỉ có tối đa 2 chữ số thập phân.');
        return { valid: false, value: 0 };
    }

    const score = Number(normalizedValue);
    if (!Number.isFinite(score)) {
        updateError('hf-ban-pick-score-error', 'Điểm Ban/Pick không hợp lệ.');
        return { valid: false, value: 0 };
    }
    if (score < 0 || score > 10) {
        updateError('hf-ban-pick-score-error', 'Điểm Ban/Pick phải nằm trong khoảng từ 0 đến 10.');
        return { valid: false, value: 0 };
    }

    updateError('hf-ban-pick-score-error', '');
    input.value = normalizedValue;
    return { valid: true, value: score };
}

function updateClock() {
    const clock = byId('header-clock');
    if (clock) clock.textContent = new Date().toLocaleString('vi-VN');
}

function showToast(message, type) {
    const toast = byId('toast');
    if (!toast) return;
    toast.textContent = `${type === 'ok' ? 'OK' : 'ERR'}: ${message}`;
    toast.className = `toast-box ${type === 'ok' ? 'toast-ok' : 'toast-err'}`;
    window.setTimeout(() => toast.classList.add('show'), 20);
    window.setTimeout(() => toast.classList.remove('show'), 3600);
}

function setButtonLoading(buttonId, loading) {
    const button = byId(buttonId);
    if (!button) return;
    if (loading) {
        button.dataset.originalText = button.textContent;
        button.textContent = 'Đang xử lý...';
        button.disabled = true;
    } else {
        button.textContent = button.dataset.originalText || button.textContent;
        button.disabled = false;
    }
}

function parseApiErrorBody(text) {
    if (!text) return '';
    try {
        const json = JSON.parse(text);
        return json.error || json.detail || json.message || json.title || text;
    } catch (error) {
        return text;
    }
}

async function apiFetch(url, options) {
    const response = await fetch(url, options || {});
    if (response.ok) {
        const text = await response.text();
        return text ? JSON.parse(text) : null;
    }

    const body = await response.text();
    const message = parseApiErrorBody(body) || `${response.status} ${response.statusText}`;
    console.error('[AdminHeroes API error]', {
        url,
        status: response.status,
        statusText: response.statusText,
        body
    });
    throw new Error(`${response.status} ${response.statusText}: ${message}`);
}

function putJson(url, body) {
    return apiFetch(url, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    });
}

function updateError(targetId, message) {
    const target = byId(targetId);
    if (!target) return;
    if (!message) {
        target.textContent = '';
        target.classList.add('hidden');
        return;
    }
    target.textContent = message;
    target.classList.remove('hidden');
}

function chip(value, className) {
    if (!value) return '';
    return `<span class="chip ${className || ''}">${escapeHtml(value)}</span>`;
}

function chipList(values, className) {
    const list = Array.isArray(values) ? values.filter(Boolean) : [];
    if (!list.length) return '<span class="empty-muted">Chưa có</span>';
    return `<div class="chip-row">${list.map(value => chip(value, className)).join('')}</div>`;
}

function roleChip(role, className) {
    if (!role) return '<span class="empty-muted">Chưa có</span>';
    return chip(roleLabel(role), className || 'primary-role-chip');
}

function heroAvatar(hero) {
    const name = hero && hero.name ? hero.name : '?';
    const initial = escapeHtml(name.charAt(0).toUpperCase() || '?');
    if (!hero || !hero.avatarUrl) {
        return `<div class="hero-avatar hero-avatar-fallback">${initial}</div>`;
    }
    return `<img class="hero-avatar" src="${escapeHtml(normalizeAssetUrl(hero.avatarUrl))}" alt="${escapeHtml(name)}" onerror="this.replaceWith(Object.assign(document.createElement('div'), {className:'hero-avatar hero-avatar-fallback', textContent:'${initial}'}))">`;
}

function mergeRolesFromHeroes() {
    const byCode = new Map(DEFAULT_ROLE_OPTIONS.map(role => [role.code, { ...role }]));
    state.heroes.forEach(hero => {
        [getHeroPrimaryRole(hero), ...getHeroSubRoles(hero)].forEach(role => {
            const normalized = normalizeRole(role);
            if (!normalized || !normalized.code) return;
            const existing = byCode.get(normalized.code) || {};
            byCode.set(normalized.code, {
                id: normalized.id != null ? normalized.id : existing.id ?? null,
                code: normalized.code,
                name: normalized.name || existing.name || normalized.code
            });
        });
    });
    state.roles = Array.from(byCode.values());
}

function renderClassFilter() {
    const select = byId('hero-class-filter');
    if (!select) return;
    const current = select.value || state.filters.className;
    select.innerHTML = '<option value="">Tất cả class</option>' + state.classes
        .map(name => `<option value="${escapeHtml(name)}">${escapeHtml(name)}</option>`)
        .join('');
    select.value = current;
}

function renderRoleFilter() {
    const select = byId('hero-role-filter');
    if (!select) return;
    const current = select.value || state.filters.primaryRole;
    select.innerHTML = '<option value="">Tất cả Primary Role</option>' + state.roles
        .map(role => `<option value="${escapeHtml(role.code)}">${escapeHtml(roleLabel(role))}</option>`)
        .join('');
    select.value = current;
}

function renderFilters() {
    renderClassFilter();
    renderRoleFilter();
}

function heroMatchesFilters(hero) {
    const search = normalizeText(state.filters.search);
    const classes = getHeroClasses(hero);
    const primaryRoleCode = getHeroPrimaryRoleCode(hero);
    const searchValues = [
        hero.name,
        hero.slug,
        hero.heroClass,
        primaryRoleCode,
        getHeroPrimaryRole(hero)?.name
    ]
        .concat(classes)
        .concat(getHeroSubRoles(hero).flatMap(role => [role.code, role.name]))
        .concat(getHeroAttributes(hero));

    const matchesSearch = !search || searchValues.some(value => normalizeText(value).includes(search));
    const matchesClass = !state.filters.className || classes.includes(state.filters.className);
    const matchesRole = !state.filters.primaryRole || primaryRoleCode === state.filters.primaryRole;
    return matchesSearch && matchesClass && matchesRole;
}

function filteredHeroes() {
    return state.heroes.filter(heroMatchesFilters);
}

function updateHeroHeader(visibleCount) {
    const total = state.heroes.length;
    const subtitle = byId('hero-subtitle');
    const pill = byId('hero-count-pill');
    if (subtitle) {
        subtitle.textContent = visibleCount === total
            ? `Chỉnh sửa tướng, class, primary role, sub roles, điểm Ban/Pick, đặc điểm và mô tả wiki. ${total} tướng.`
            : `Đang hiển thị ${visibleCount} / ${total} tướng.`;
    }
    if (pill) pill.textContent = `${visibleCount} / ${total} tướng`;
}

function renderHeroesTable() {
    const tbody = byId('heroes-tbody');
    if (!tbody) return;

    const heroes = filteredHeroes();
    updateHeroHeader(heroes.length);

    if (!state.heroes.length) {
        tbody.innerHTML = '<tr><td colspan="9" class="table-state">Chưa có dữ liệu tướng.</td></tr>';
        return;
    }

    if (!heroes.length) {
        tbody.innerHTML = '<tr><td colspan="9" class="table-state">Không tìm thấy tướng phù hợp.</td></tr>';
        return;
    }

    tbody.innerHTML = heroes.map(hero => {
        const primaryRole = getHeroPrimaryRole(hero);
        const subRoles = getHeroSubRoles(hero);
        return `
            <tr>
                <td>${heroAvatar(hero)}</td>
                <td>
                    <div class="hero-name">${escapeHtml(hero.name || '-')}</div>
                    <div class="hero-id">ID #${escapeHtml(hero.id)}</div>
                </td>
                <td><span class="slug">${escapeHtml(hero.slug || '-')}</span></td>
                <td>${chipList(getHeroClasses(hero))}</td>
                <td>${roleChip(primaryRole, 'primary-role-chip')}</td>
                <td>${chipList(subRoles.map(roleLabel), 'sub-role-chip')}</td>
                <td>${chipList(getHeroAttributes(hero))}</td>
                <td><span class="score-chip">${escapeHtml(formatBanPickScoreDisplay(hero.banPickScore))}</span></td>
                <td>
                    <div class="row-actions">
                        <button type="button" class="btn btn-light btn-small" data-action="edit-hero" data-hero-id="${Number(hero.id)}">Sửa</button>
                    </div>
                </td>
            </tr>`;
    }).join('');
}

function showLoadingRows(tbodyId, colspan, message) {
    const tbody = byId(tbodyId);
    if (tbody) tbody.innerHTML = `<tr><td colspan="${colspan}" class="table-state">${escapeHtml(message)}</td></tr>`;
}

async function loadHeroes() {
    showLoadingRows('heroes-tbody', 9, 'Đang tải danh sách tướng...');
    updateError('heroes-error', '');
    try {
        const heroes = await apiFetch('/api/admin/wiki/heroes', { headers: { Accept: 'application/json' }, cache: 'no-store' });
        state.heroes = Array.isArray(heroes) ? heroes : [];
        mergeRolesFromHeroes();
        renderRoleFilter();
        renderHeroesTable();
    } catch (error) {
        updateError('heroes-error', `Không tải được danh sách tướng.\n${error.message}`);
        showLoadingRows('heroes-tbody', 9, 'Không tải được danh sách tướng. Xem chi tiết lỗi phía trên và console.');
        updateHeroHeader(0);
    }
}

async function loadAttributes() {
    state.attributeLoadError = '';
    try {
        const attributes = await apiFetch('/api/admin/wiki/attributes', { headers: { Accept: 'application/json' }, cache: 'no-store' });
        state.attributes = Array.isArray(attributes) ? attributes : [];
    } catch (error) {
        state.attributes = [];
        state.attributeLoadError = error.message;
        console.error('[AdminHeroes loadAttributes failed]', error);
    }
}

function loadClasses() {
    state.classes = DEFAULT_CLASS_OPTIONS.slice();
    renderClassFilter();
}

async function loadInitialData() {
    loadClasses();
    renderRoleFilter();
    await Promise.allSettled([loadHeroes(), loadAttributes()]);
}

function checkbox(name, value, label, checked, disabled) {
    const id = `${name}-${String(value).replace(/[^a-zA-Z0-9_-]/g, '-')}`;
    return `
        <label class="checkbox-label ${disabled ? 'disabled' : ''}" for="${escapeHtml(id)}">
            <input id="${escapeHtml(id)}" type="checkbox" name="${escapeHtml(name)}" value="${escapeHtml(value)}" ${checked ? 'checked' : ''} ${disabled ? 'disabled' : ''}>
            <span>${escapeHtml(label)}</span>
        </label>`;
}

function checkedValues(name) {
    return Array.from(document.querySelectorAll(`input[name="${name}"]:checked`)).map(input => input.value);
}

function renderPrimaryRoleSelect(hero) {
    const select = byId('hero-primary-role');
    if (!select) return;
    const primaryRole = getHeroPrimaryRole(hero);
    const selectedId = primaryRole && primaryRole.id != null ? String(primaryRole.id) : '';
    select.innerHTML = '<option value="">Chọn Primary Role</option>' + state.roles
        .filter(role => role.id != null)
        .map(role => `<option value="${escapeHtml(role.id)}" ${String(role.id) === selectedId ? 'selected' : ''}>${escapeHtml(roleLabel(role))}</option>`)
        .join('');
}

function renderSubRoleCheckboxes(hero) {
    const container = byId('hero-sub-role-options');
    if (!container) return;
    const selected = new Set(getHeroSubRoles(hero).map(role => role.id == null ? '' : String(role.id)).filter(Boolean));
    container.innerHTML = state.roles
        .filter(role => role.id != null)
        .map(role => checkbox('hero-sub-role', role.id, roleLabel(role), selected.has(String(role.id)), false))
        .join('');
    syncPrimaryAndSubRoles();
}

function syncPrimaryAndSubRoles() {
    const primaryRoleId = byId('hero-primary-role') ? byId('hero-primary-role').value : '';
    document.querySelectorAll('input[name="hero-sub-role"]').forEach(input => {
        const isPrimary = primaryRoleId && input.value === primaryRoleId;
        if (isPrimary) input.checked = false;
        input.disabled = !!isPrimary;
        const label = input.closest('.checkbox-label');
        if (label) label.classList.toggle('disabled', !!isPrimary);
    });
}

function renderHeroRoleSuggestion(detail) {
    const target = byId('hero-role-suggestion');
    if (!target) return;
    const suggestions = Array.isArray(detail && detail.suggestedRoles) ? detail.suggestedRoles : [];
    if (!suggestions.length) {
        target.textContent = '';
        target.classList.add('hidden');
        return;
    }
    target.textContent = `Gợi ý vị trí: ${suggestions.join(', ')}`;
    target.classList.remove('hidden');
}

function renderHeroClassCheckboxes(hero) {
    const selected = new Set(getHeroClasses(hero));
    const container = byId('hero-class-options');
    if (!container) return;
    container.innerHTML = state.classes
        .map(name => checkbox('hero-class', name, name, selected.has(name), false))
        .join('');
}

function renderHeroAttributeCheckboxes(hero) {
    const selected = new Set(getHeroAttributes(hero));
    const container = byId('hero-attribute-options');
    if (!container) return;
    if (state.attributeLoadError) {
        container.innerHTML = `<div class="inline-error">Không tải được danh sách đặc điểm. ${escapeHtml(state.attributeLoadError)}</div>`;
        return;
    }
    if (!state.attributes.length) {
        container.innerHTML = '<div class="empty-muted">Chưa có đặc điểm nào.</div>';
        return;
    }
    container.innerHTML = state.attributes
        .map(attribute => checkbox('hero-attribute', attribute.name, attribute.name, selected.has(attribute.name), false))
        .join('');
}

function renderDifficultySelect(detail, hero) {
    const select = byId('hf-difficulty');
    if (!select) return;
    const options = Array.isArray(detail && detail.difficulties) ? detail.difficulties : [];
    select.innerHTML = '<option value="">-</option>' + options
        .map(value => `<option value="${escapeHtml(value)}" ${value === hero.difficulty ? 'selected' : ''}>${escapeHtml(value)}</option>`)
        .join('');
}

function fillHeroForm(detail) {
    const hero = detail.hero || {};
    state.editingHero = hero;
    if (Array.isArray(detail.availableRoles) && detail.availableRoles.length) {
        state.roles = detail.availableRoles.map(normalizeRole).filter(role => role && role.id != null);
        renderRoleFilter();
    }
    if (Array.isArray(detail.availableClasses) && detail.availableClasses.length) {
        state.classes = detail.availableClasses.slice();
        renderClassFilter();
    }
    if (Array.isArray(detail.availableAttributes)) {
        state.attributes = detail.availableAttributes.slice();
        state.attributeLoadError = '';
    }

    byId('hf-id').value = hero.id || '';
    byId('hf-name').value = hero.name || '';
    byId('hf-slug').value = hero.slug || '';
    byId('hf-avatar').value = hero.avatarUrl || '';
    byId('hf-portrait').value = hero.portraitUrl || '';
    byId('hf-banner').value = hero.bannerUrl || '';
    byId('hf-description').value = hero.description || '';
    byId('hf-ban-pick-score').value = formatBanPickScoreInputValue(hero.banPickScore);
    updateError('hf-ban-pick-score-error', '');
    byId('hero-modal-subtitle').textContent = hero.name ? `${hero.name} #${hero.id}` : '';

    renderDifficultySelect(detail, hero);
    renderHeroClassCheckboxes(hero);
    renderPrimaryRoleSelect(hero);
    renderSubRoleCheckboxes(hero);
    renderHeroAttributeCheckboxes(hero);
    renderHeroRoleSuggestion(detail);
}

async function openHeroModal(heroId) {
    updateError('heroes-error', '');
    try {
        const detail = await apiFetch(`/api/admin/wiki/heroes/${Number(heroId)}`, { headers: { Accept: 'application/json' }, cache: 'no-store' });
        fillHeroForm(detail || {});
        byId('hero-modal').classList.remove('hidden');
        byId('hero-modal').setAttribute('aria-hidden', 'false');
    } catch (error) {
        updateError('heroes-error', `Không mở được form tướng.\n${error.message}`);
        showToast(error.message, 'err');
    }
}

function closeHeroModal() {
    state.editingHero = null;
    const modal = byId('hero-modal');
    if (modal) {
        modal.classList.add('hidden');
        modal.setAttribute('aria-hidden', 'true');
    }
}

async function saveHero(event) {
    event.preventDefault();
    const heroId = byId('hf-id').value;
    const primaryRoleId = byId('hero-primary-role').value;
    const scoreValidation = validateBanPickScoreInput();
    if (!heroId) return;
    if (!primaryRoleId) {
        showToast('Primary role is required', 'err');
        return;
    }
    if (!scoreValidation.valid) {
        showToast('Điểm Ban/Pick chưa hợp lệ.', 'err');
        return;
    }

    const basicBody = {
        name: byId('hf-name').value.trim(),
        slug: byId('hf-slug').value.trim(),
        classes: checkedValues('hero-class'),
        banPickScore: scoreValidation.value,
        description: byId('hf-description').value.trim(),
        avatarUrl: byId('hf-avatar').value.trim(),
        portraitUrl: byId('hf-portrait').value.trim(),
        bannerUrl: byId('hf-banner').value.trim(),
        difficulty: byId('hf-difficulty').value
    };

    const roleBody = {
        primaryRoleId: Number(primaryRoleId),
        subRoleIds: checkedValues('hero-sub-role').map(Number)
    };

    setButtonLoading('btn-hero-submit', true);
    try {
        await putJson(`/api/admin/wiki/heroes/${Number(heroId)}`, basicBody);
        await putJson(`/api/admin/wiki/heroes/${Number(heroId)}/roles`, roleBody);
        await putJson(`/api/admin/wiki/heroes/${Number(heroId)}/attributes`, {
            attributes: checkedValues('hero-attribute')
        });
        showToast('Đã lưu thông tin tướng.', 'ok');
        closeHeroModal();
        await loadHeroes();
    } catch (error) {
        console.error('[AdminHeroes saveHero failed]', error);
        showToast(error.message, 'err');
    } finally {
        setButtonLoading('btn-hero-submit', false);
    }
}

function bindEvents() {
    byId('btn-refresh-heroes')?.addEventListener('click', async () => {
        await Promise.allSettled([loadHeroes(), loadAttributes()]);
    });

    byId('hero-search')?.addEventListener('input', event => {
        state.filters.search = event.target.value;
        renderHeroesTable();
    });
    byId('hero-class-filter')?.addEventListener('change', event => {
        state.filters.className = event.target.value;
        renderHeroesTable();
    });
    byId('hero-role-filter')?.addEventListener('change', event => {
        state.filters.primaryRole = event.target.value;
        renderHeroesTable();
    });
    byId('btn-hero-reset')?.addEventListener('click', () => {
        state.filters.search = '';
        state.filters.className = '';
        state.filters.primaryRole = '';
        byId('hero-search').value = '';
        renderFilters();
        renderHeroesTable();
    });

    byId('heroes-tbody')?.addEventListener('click', event => {
        const button = event.target.closest('[data-action="edit-hero"]');
        if (button) openHeroModal(button.dataset.heroId);
    });

    byId('btn-close-hero-modal')?.addEventListener('click', closeHeroModal);
    byId('btn-cancel-hero')?.addEventListener('click', closeHeroModal);
    byId('hero-form')?.addEventListener('submit', saveHero);
    byId('hero-primary-role')?.addEventListener('change', syncPrimaryAndSubRoles);
    byId('hf-ban-pick-score')?.addEventListener('input', () => {
        if (byId('hf-ban-pick-score-error')?.textContent) {
            validateBanPickScoreInput();
        }
    });

    document.querySelectorAll('.modal-backdrop').forEach(backdrop => {
        backdrop.addEventListener('click', event => {
            if (event.target === backdrop && backdrop.id === 'hero-modal') closeHeroModal();
        });
    });

    document.addEventListener('keydown', event => {
        if (event.key === 'Escape') closeHeroModal();
    });

    document.addEventListener('authExpired', () => {
        window.location.replace('/html/index.html');
    });
}

function initAdminHeroesPage() {
    if (!document.querySelector('[data-page="admin-heroes"]')) return;
    if (typeof requireRoleAccess === 'function' && !requireRoleAccess('Admin', '/html/index.html')) {
        return;
    }
    bindEvents();
    renderFilters();
    updateClock();
    window.setInterval(updateClock, 1000);
    loadInitialData();
}

window.AdminHeroesPage = {
    state,
    loadHeroes,
    loadAttributes,
    openHeroModal
};

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initAdminHeroesPage);
} else {
    initAdminHeroesPage();
}
