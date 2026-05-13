const ADMIN_SPELLS_API = '/api/admin/spells';
const ADMIN_ENCHANTMENTS_API = '/api/admin/enchantments';

const DEFAULT_BRANCH_OPTIONS = [
    { value: 'thanh-khoi-nguyen', label: 'Thành khởi nguyên' },
    { value: 'thap-quang-minh', label: 'Tháp quang minh' },
    { value: 'vuc-hon-mang', label: 'Vực hỗn mang' },
    { value: 'rung-nguyen-sinh', label: 'Rừng nguyên sinh' }
];

const state = {
    activeTab: 'spells',
    spells: [],
    enchantments: [],
    spellFilter: '',
    enchantmentFilter: '',
    enchantmentBranchFilter: '',
    editingSpellSlug: '',
    editingEnchantmentSlug: '',
    spellSlugAuto: true,
    spellIconAuto: true,
    enchantmentSlugAuto: true,
    enchantmentIconAuto: true,
    enchantmentBranchNameAuto: true
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

function slugify(value) {
    return normalizeText(value).replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '');
}

function normalizeAssetUrl(url) {
    if (!url) return '';
    if (/^(https?:)?\/\//i.test(url) || url.startsWith('data:') || url.startsWith('/')) {
        return url.trim();
    }
    return '/' + url.trim().replace(/^\.?\//, '');
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
    throw new Error(parseApiErrorBody(body) || `${response.status} ${response.statusText}`);
}

function getBranchLabel(branchValue) {
    const branch = DEFAULT_BRANCH_OPTIONS.find(option => option.value === branchValue);
    return branch ? branch.label : branchValue;
}

function branchOptions() {
    const lookup = new Map(DEFAULT_BRANCH_OPTIONS.map(option => [option.value, option.label]));
    state.enchantments.forEach(item => {
        if (item.branch) {
            lookup.set(item.branch, item.branchName || lookup.get(item.branch) || item.branch);
        }
    });
    return Array.from(lookup.entries()).map(([value, label]) => ({ value, label }));
}

function updatePreview(imageId, fallbackId, pathId, url, label) {
    const image = byId(imageId);
    const fallback = byId(fallbackId);
    const path = byId(pathId);
    const safeLabel = String(label || '?').trim().charAt(0).toUpperCase() || '?';

    if (path) {
        path.textContent = url || 'Chưa có iconUrl';
    }
    if (fallback) {
        fallback.textContent = safeLabel;
    }
    if (!image || !fallback) return;

    if (!url) {
        image.removeAttribute('src');
        image.style.display = 'none';
        fallback.style.display = 'grid';
        return;
    }

    image.style.display = 'block';
    fallback.style.display = 'none';
    image.onerror = function onPreviewError() {
        image.style.display = 'none';
        fallback.style.display = 'grid';
    };
    image.onload = function onPreviewLoad() {
        image.style.display = 'block';
        fallback.style.display = 'none';
    };
    image.src = url;
}

function updateError(id, message) {
    const target = byId(id);
    if (!target) return;
    if (!message) {
        target.textContent = '';
        target.classList.add('hidden');
        return;
    }
    target.textContent = message;
    target.classList.remove('hidden');
}

function switchTab(tab) {
    state.activeTab = tab === 'enchantments' ? 'enchantments' : 'spells';
    ['spells', 'enchantments'].forEach(value => {
        byId(`tab-button-${value}`)?.classList.toggle('active', value === state.activeTab);
        byId(`tab-panel-${value}`)?.classList.toggle('active', value === state.activeTab);
    });
}

function filteredSpells() {
    const search = normalizeText(state.spellFilter);
    if (!search) return state.spells;
    return state.spells.filter(item => [item.name, item.slug, item.iconUrl, item.description]
        .some(value => normalizeText(value).includes(search)));
}

function filteredEnchantments() {
    const search = normalizeText(state.enchantmentFilter);
    return state.enchantments.filter(item => {
        const matchesSearch = !search || [item.name, item.slug, item.branch, item.branchName, item.iconUrl, item.description]
            .some(value => normalizeText(value).includes(search));
        const matchesBranch = !state.enchantmentBranchFilter || item.branch === state.enchantmentBranchFilter;
        return matchesSearch && matchesBranch;
    });
}

function renderSpellTable() {
    const tbody = byId('spells-tbody');
    if (!tbody) return;

    const spells = filteredSpells();
    byId('spell-count-pill').textContent = `${spells.length} / ${state.spells.length} bổ trợ`;

    if (!state.spells.length) {
        tbody.innerHTML = '<tr><td colspan="5" class="table-state">Chưa có dữ liệu Bổ trợ.</td></tr>';
        return;
    }
    if (!spells.length) {
        tbody.innerHTML = '<tr><td colspan="5" class="table-state">Không tìm thấy Bổ trợ phù hợp.</td></tr>';
        return;
    }

    tbody.innerHTML = spells.map(spell => {
        const selected = spell.slug === state.editingSpellSlug ? 'row-selected' : '';
        const icon = spell.iconUrl
            ? `<img class="wiki-mini-icon" src="${escapeHtml(spell.iconUrl)}" alt="${escapeHtml(spell.name)}" onerror="this.style.display='none';this.nextElementSibling.style.display='grid'">`
                + `<span class="wiki-mini-fallback" style="display:none">${escapeHtml((spell.name || '?').charAt(0))}</span>`
            : `<span class="wiki-mini-fallback">${escapeHtml((spell.name || '?').charAt(0))}</span>`;
        return `
            <tr class="${selected}">
                <td><div class="wiki-mini-icon-wrap">${icon}</div></td>
                <td><strong>${escapeHtml(spell.name || '-')}</strong></td>
                <td><span class="slug">${escapeHtml(spell.slug || '-')}</span></td>
                <td><span class="wiki-url-text">${escapeHtml(spell.iconUrl || '-')}</span></td>
                <td>
                    <div class="row-actions">
                        <button type="button" class="btn btn-light btn-small" data-action="edit-spell" data-slug="${escapeHtml(spell.slug)}">Sửa</button>
                    </div>
                </td>
            </tr>`;
    }).join('');
}

function renderEnchantmentBranchFilter() {
    const select = byId('enchantment-filter-branch');
    if (!select) return;
    const current = state.enchantmentBranchFilter;
    select.innerHTML = '<option value="">Tất cả branch</option>' + branchOptions()
        .map(option => `<option value="${escapeHtml(option.value)}">${escapeHtml(option.label)}</option>`)
        .join('');
    select.value = current;
}

function renderEnchantmentBranchSelect() {
    const select = byId('ef-branch');
    if (!select) return;
    const current = select.value;
    select.innerHTML = branchOptions()
        .map(option => `<option value="${escapeHtml(option.value)}">${escapeHtml(option.label)}</option>`)
        .join('');
    if (current && select.querySelector(`option[value="${CSS.escape(current)}"]`)) {
        select.value = current;
    }
}

function renderEnchantmentTable() {
    const tbody = byId('enchantments-tbody');
    if (!tbody) return;

    renderEnchantmentBranchFilter();
    renderEnchantmentBranchSelect();

    const enchantments = filteredEnchantments();
    byId('enchantment-count-pill').textContent = `${enchantments.length} / ${state.enchantments.length} phù hiệu`;

    if (!state.enchantments.length) {
        tbody.innerHTML = '<tr><td colspan="6" class="table-state">Chưa có dữ liệu Phù hiệu.</td></tr>';
        return;
    }
    if (!enchantments.length) {
        tbody.innerHTML = '<tr><td colspan="6" class="table-state">Không tìm thấy Phù hiệu phù hợp.</td></tr>';
        return;
    }

    tbody.innerHTML = enchantments.map(item => {
        const selected = item.slug === state.editingEnchantmentSlug ? 'row-selected' : '';
        const icon = item.iconUrl
            ? `<img class="wiki-mini-icon" src="${escapeHtml(item.iconUrl)}" alt="${escapeHtml(item.name)}" onerror="this.style.display='none';this.nextElementSibling.style.display='grid'">`
                + `<span class="wiki-mini-fallback" style="display:none">${escapeHtml((item.name || '?').charAt(0))}</span>`
            : `<span class="wiki-mini-fallback">${escapeHtml((item.name || '?').charAt(0))}</span>`;
        return `
            <tr class="${selected}">
                <td><div class="wiki-mini-icon-wrap">${icon}</div></td>
                <td><strong>${escapeHtml(item.name || '-')}</strong></td>
                <td>${escapeHtml(item.branchName || item.branch || '-')}</td>
                <td>${item.level == null ? '-' : escapeHtml(item.level)}</td>
                <td><span class="slug">${escapeHtml(item.slug || '-')}</span></td>
                <td>
                    <div class="row-actions">
                        <button type="button" class="btn btn-light btn-small" data-action="edit-enchantment" data-slug="${escapeHtml(item.slug)}">Sửa</button>
                    </div>
                </td>
            </tr>`;
    }).join('');
}

function resetSpellForm() {
    state.editingSpellSlug = '';
    state.spellSlugAuto = true;
    state.spellIconAuto = true;
    byId('spell-form-title').textContent = 'Thêm Bổ trợ';
    byId('spell-form-subtitle').textContent = 'Lưu vào JSON, không dùng bảng DB spells.';
    byId('sf-current-slug').value = '';
    byId('sf-name').value = '';
    byId('sf-slug').value = '';
    byId('sf-icon-url').value = '';
    byId('sf-description').value = '';
    byId('btn-spell-delete').disabled = true;
    updateError('spell-form-error', '');
    updatePreview('spell-preview-image', 'spell-preview-fallback', 'spell-preview-path', '', '');
    renderSpellTable();
}

function fillSpellForm(spell) {
    if (!spell) {
        resetSpellForm();
        return;
    }
    state.editingSpellSlug = spell.slug || '';
    state.spellSlugAuto = false;
    state.spellIconAuto = false;
    byId('spell-form-title').textContent = `Sửa Bổ trợ: ${spell.name || spell.slug}`;
    byId('spell-form-subtitle').textContent = 'PUT sẽ cập nhật item theo slug hiện tại trong JSON.';
    byId('sf-current-slug').value = spell.slug || '';
    byId('sf-name').value = spell.name || '';
    byId('sf-slug').value = spell.slug || '';
    byId('sf-icon-url').value = spell.iconUrl || '';
    byId('sf-description').value = spell.description || '';
    byId('btn-spell-delete').disabled = false;
    updateError('spell-form-error', '');
    updatePreview('spell-preview-image', 'spell-preview-fallback', 'spell-preview-path', spell.iconUrl || '', spell.name || spell.slug || '?');
    renderSpellTable();
}

function resetEnchantmentForm() {
    state.editingEnchantmentSlug = '';
    state.enchantmentSlugAuto = true;
    state.enchantmentIconAuto = true;
    state.enchantmentBranchNameAuto = true;
    byId('enchantment-form-title').textContent = 'Thêm Phù hiệu';
    byId('enchantment-form-subtitle').textContent = 'Lưu vào JSON, không dùng DB runtime cho phu_hieu.';
    byId('ef-current-slug').value = '';
    byId('ef-name').value = '';
    byId('ef-slug').value = '';
    renderEnchantmentBranchSelect();
    if (byId('ef-branch').options.length) {
        byId('ef-branch').selectedIndex = 0;
        byId('ef-branch-name').value = getBranchLabel(byId('ef-branch').value);
    } else {
        byId('ef-branch-name').value = '';
    }
    byId('ef-level').value = '';
    byId('ef-icon-url').value = '';
    byId('ef-description').value = '';
    byId('btn-enchantment-delete').disabled = true;
    updateError('enchantment-form-error', '');
    updatePreview('enchantment-preview-image', 'enchantment-preview-fallback', 'enchantment-preview-path', '', '');
    renderEnchantmentTable();
}

function fillEnchantmentForm(item) {
    if (!item) {
        resetEnchantmentForm();
        return;
    }
    state.editingEnchantmentSlug = item.slug || '';
    state.enchantmentSlugAuto = false;
    state.enchantmentIconAuto = false;
    state.enchantmentBranchNameAuto = false;
    byId('enchantment-form-title').textContent = `Sửa Phù hiệu: ${item.name || item.slug}`;
    byId('enchantment-form-subtitle').textContent = 'PUT sẽ cập nhật item theo slug hiện tại trong JSON.';
    byId('ef-current-slug').value = item.slug || '';
    byId('ef-name').value = item.name || '';
    byId('ef-slug').value = item.slug || '';
    renderEnchantmentBranchSelect();
    byId('ef-branch').value = item.branch || branchOptions()[0]?.value || '';
    byId('ef-branch-name').value = item.branchName || getBranchLabel(item.branch || '');
    byId('ef-level').value = item.level == null ? '' : item.level;
    byId('ef-icon-url').value = item.iconUrl || '';
    byId('ef-description').value = item.description || '';
    byId('btn-enchantment-delete').disabled = false;
    updateError('enchantment-form-error', '');
    updatePreview('enchantment-preview-image', 'enchantment-preview-fallback', 'enchantment-preview-path', item.iconUrl || '', item.name || item.slug || '?');
    renderEnchantmentTable();
}

function buildSpellPayload() {
    const payload = {
        name: byId('sf-name').value.trim(),
        slug: byId('sf-slug').value.trim(),
        iconUrl: normalizeAssetUrl(byId('sf-icon-url').value.trim()),
        description: byId('sf-description').value.trim() || null
    };
    if (!payload.name || !payload.slug || !payload.iconUrl) {
        throw new Error('Tên, slug và iconUrl của Bổ trợ là bắt buộc.');
    }
    return payload;
}

function buildEnchantmentPayload() {
    const levelValue = byId('ef-level').value.trim();
    const payload = {
        name: byId('ef-name').value.trim(),
        slug: byId('ef-slug').value.trim(),
        branch: byId('ef-branch').value,
        branchName: byId('ef-branch-name').value.trim(),
        level: levelValue ? Number(levelValue) : null,
        iconUrl: normalizeAssetUrl(byId('ef-icon-url').value.trim()),
        description: byId('ef-description').value.trim() || null
    };
    if (!payload.name || !payload.slug || !payload.branch || !payload.branchName || !payload.iconUrl) {
        throw new Error('Tên, slug, branch, branchName và iconUrl của Phù hiệu là bắt buộc.');
    }
    if (payload.level != null && (!Number.isFinite(payload.level) || payload.level < 1)) {
        throw new Error('Level của Phù hiệu phải lớn hơn hoặc bằng 1.');
    }
    return payload;
}

async function loadSpells() {
    updateError('spells-error', '');
    try {
        const spells = await apiFetch(ADMIN_SPELLS_API, { headers: { Accept: 'application/json' }, cache: 'no-store' });
        state.spells = Array.isArray(spells) ? spells : [];
        renderSpellTable();
        if (!state.editingSpellSlug) {
            resetSpellForm();
            return;
        }
        fillSpellForm(state.spells.find(item => item.slug === state.editingSpellSlug) || null);
    } catch (error) {
        updateError('spells-error', error.message);
        byId('spells-tbody').innerHTML = '<tr><td colspan="5" class="table-state">Không thể tải dữ liệu Bổ trợ.</td></tr>';
    }
}

async function loadEnchantments() {
    updateError('enchantments-error', '');
    try {
        const enchantments = await apiFetch(ADMIN_ENCHANTMENTS_API, { headers: { Accept: 'application/json' }, cache: 'no-store' });
        state.enchantments = Array.isArray(enchantments) ? enchantments : [];
        renderEnchantmentTable();
        if (!state.editingEnchantmentSlug) {
            resetEnchantmentForm();
            return;
        }
        fillEnchantmentForm(state.enchantments.find(item => item.slug === state.editingEnchantmentSlug) || null);
    } catch (error) {
        updateError('enchantments-error', error.message);
        byId('enchantments-tbody').innerHTML = '<tr><td colspan="6" class="table-state">Không thể tải dữ liệu Phù hiệu.</td></tr>';
    }
}

async function refreshAllData() {
    await Promise.allSettled([loadSpells(), loadEnchantments()]);
}

async function saveSpell(event) {
    event.preventDefault();
    updateError('spell-form-error', '');
    let payload;
    try {
        payload = buildSpellPayload();
    } catch (error) {
        updateError('spell-form-error', error.message);
        showToast(error.message, 'err');
        return;
    }

    setButtonLoading('btn-spell-submit', true);
    try {
        if (state.editingSpellSlug) {
            await apiFetch(`${ADMIN_SPELLS_API}/${encodeURIComponent(state.editingSpellSlug)}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            showToast('Đã cập nhật Bổ trợ.', 'ok');
        } else {
            await apiFetch(ADMIN_SPELLS_API, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            showToast('Đã thêm Bổ trợ mới.', 'ok');
        }
        state.editingSpellSlug = payload.slug;
        await loadSpells();
    } catch (error) {
        updateError('spell-form-error', error.message);
        showToast(error.message, 'err');
    } finally {
        setButtonLoading('btn-spell-submit', false);
    }
}

async function deleteSpell() {
    if (!state.editingSpellSlug) return;
    if (!window.confirm(`Xóa Bổ trợ "${state.editingSpellSlug}"?`)) return;
    setButtonLoading('btn-spell-delete', true);
    try {
        await apiFetch(`${ADMIN_SPELLS_API}/${encodeURIComponent(state.editingSpellSlug)}`, { method: 'DELETE' });
        showToast('Đã xóa Bổ trợ.', 'ok');
        resetSpellForm();
        await loadSpells();
    } catch (error) {
        updateError('spell-form-error', error.message);
        showToast(error.message, 'err');
    } finally {
        setButtonLoading('btn-spell-delete', false);
    }
}

async function saveEnchantment(event) {
    event.preventDefault();
    updateError('enchantment-form-error', '');
    let payload;
    try {
        payload = buildEnchantmentPayload();
    } catch (error) {
        updateError('enchantment-form-error', error.message);
        showToast(error.message, 'err');
        return;
    }

    setButtonLoading('btn-enchantment-submit', true);
    try {
        if (state.editingEnchantmentSlug) {
            await apiFetch(`${ADMIN_ENCHANTMENTS_API}/${encodeURIComponent(state.editingEnchantmentSlug)}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            showToast('Đã cập nhật Phù hiệu.', 'ok');
        } else {
            await apiFetch(ADMIN_ENCHANTMENTS_API, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            showToast('Đã thêm Phù hiệu mới.', 'ok');
        }
        state.editingEnchantmentSlug = payload.slug;
        await loadEnchantments();
    } catch (error) {
        updateError('enchantment-form-error', error.message);
        showToast(error.message, 'err');
    } finally {
        setButtonLoading('btn-enchantment-submit', false);
    }
}

async function deleteEnchantment() {
    if (!state.editingEnchantmentSlug) return;
    const message = [
        `Xóa Phù hiệu "${state.editingEnchantmentSlug}"?`,
        'Cảnh báo: flow create-guide hiện không dùng DB phu_hieu, nhưng DB legacy có thể vẫn còn dữ liệu chờ migration riêng.'
    ].join('\n');
    if (!window.confirm(message)) return;
    setButtonLoading('btn-enchantment-delete', true);
    try {
        await apiFetch(`${ADMIN_ENCHANTMENTS_API}/${encodeURIComponent(state.editingEnchantmentSlug)}`, { method: 'DELETE' });
        showToast('Đã xóa Phù hiệu.', 'ok');
        resetEnchantmentForm();
        await loadEnchantments();
    } catch (error) {
        updateError('enchantment-form-error', error.message);
        showToast(error.message, 'err');
    } finally {
        setButtonLoading('btn-enchantment-delete', false);
    }
}

function maybeAutofillSpellFields() {
    const name = byId('sf-name').value.trim();
    if (state.spellSlugAuto) {
        byId('sf-slug').value = slugify(name);
    }
    if (state.spellIconAuto) {
        const slug = slugify(byId('sf-slug').value.trim() || name);
        byId('sf-icon-url').value = slug ? `/images/spells/${slug}.png` : '';
    }
    updatePreview('spell-preview-image', 'spell-preview-fallback', 'spell-preview-path', normalizeAssetUrl(byId('sf-icon-url').value.trim()), name || byId('sf-slug').value.trim());
}

function maybeAutofillEnchantmentFields() {
    const name = byId('ef-name').value.trim();
    const branch = byId('ef-branch').value;
    if (state.enchantmentSlugAuto) {
        byId('ef-slug').value = slugify(name);
    }
    if (state.enchantmentBranchNameAuto && branch) {
        byId('ef-branch-name').value = getBranchLabel(branch);
    }
    if (state.enchantmentIconAuto) {
        const slug = slugify(byId('ef-slug').value.trim() || name);
        byId('ef-icon-url').value = slug && branch ? `/images/enchantments/${branch}/${slug}.webp` : '';
    }
    updatePreview(
        'enchantment-preview-image',
        'enchantment-preview-fallback',
        'enchantment-preview-path',
        normalizeAssetUrl(byId('ef-icon-url').value.trim()),
        name || byId('ef-slug').value.trim()
    );
}

function bindEvents() {
    byId('btn-refresh-wiki-data')?.addEventListener('click', refreshAllData);

    document.querySelectorAll('.admin-tab-button[data-tab]').forEach(button => {
        button.addEventListener('click', () => switchTab(button.dataset.tab));
    });

    byId('spell-search')?.addEventListener('input', event => {
        state.spellFilter = event.target.value;
        renderSpellTable();
    });
    byId('btn-spell-clear-search')?.addEventListener('click', () => {
        state.spellFilter = '';
        byId('spell-search').value = '';
        renderSpellTable();
    });
    byId('btn-new-spell')?.addEventListener('click', resetSpellForm);
    byId('btn-spell-reset-form')?.addEventListener('click', resetSpellForm);
    byId('spell-form')?.addEventListener('submit', saveSpell);
    byId('btn-spell-delete')?.addEventListener('click', deleteSpell);

    byId('sf-name')?.addEventListener('input', maybeAutofillSpellFields);
    byId('sf-slug')?.addEventListener('input', () => {
        state.spellSlugAuto = false;
        maybeAutofillSpellFields();
    });
    byId('sf-icon-url')?.addEventListener('input', () => {
        state.spellIconAuto = false;
        updatePreview('spell-preview-image', 'spell-preview-fallback', 'spell-preview-path', normalizeAssetUrl(byId('sf-icon-url').value.trim()), byId('sf-name').value.trim() || byId('sf-slug').value.trim());
    });

    byId('spells-tbody')?.addEventListener('click', event => {
        const button = event.target.closest('[data-action="edit-spell"]');
        if (!button) return;
        fillSpellForm(state.spells.find(item => item.slug === button.dataset.slug) || null);
    });

    byId('enchantment-search')?.addEventListener('input', event => {
        state.enchantmentFilter = event.target.value;
        renderEnchantmentTable();
    });
    byId('enchantment-filter-branch')?.addEventListener('change', event => {
        state.enchantmentBranchFilter = event.target.value;
        renderEnchantmentTable();
    });
    byId('btn-enchantment-clear-search')?.addEventListener('click', () => {
        state.enchantmentFilter = '';
        state.enchantmentBranchFilter = '';
        byId('enchantment-search').value = '';
        byId('enchantment-filter-branch').value = '';
        renderEnchantmentTable();
    });
    byId('btn-new-enchantment')?.addEventListener('click', resetEnchantmentForm);
    byId('btn-enchantment-reset-form')?.addEventListener('click', resetEnchantmentForm);
    byId('enchantment-form')?.addEventListener('submit', saveEnchantment);
    byId('btn-enchantment-delete')?.addEventListener('click', deleteEnchantment);

    byId('ef-name')?.addEventListener('input', maybeAutofillEnchantmentFields);
    byId('ef-slug')?.addEventListener('input', () => {
        state.enchantmentSlugAuto = false;
        maybeAutofillEnchantmentFields();
    });
    byId('ef-branch')?.addEventListener('change', maybeAutofillEnchantmentFields);
    byId('ef-branch-name')?.addEventListener('input', () => {
        state.enchantmentBranchNameAuto = false;
    });
    byId('ef-icon-url')?.addEventListener('input', () => {
        state.enchantmentIconAuto = false;
        updatePreview('enchantment-preview-image', 'enchantment-preview-fallback', 'enchantment-preview-path', normalizeAssetUrl(byId('ef-icon-url').value.trim()), byId('ef-name').value.trim() || byId('ef-slug').value.trim());
    });

    byId('enchantments-tbody')?.addEventListener('click', event => {
        const button = event.target.closest('[data-action="edit-enchantment"]');
        if (!button) return;
        fillEnchantmentForm(state.enchantments.find(item => item.slug === button.dataset.slug) || null);
    });

    document.addEventListener('authExpired', () => {
        window.location.replace('/html/index.html');
    });
}

function initAdminWikiDataPage() {
    if (!document.querySelector('[data-page="admin-wiki-data"]')) return;
    if (typeof requireRoleAccess === 'function' && !requireRoleAccess('Admin', '/html/index.html')) {
        return;
    }
    bindEvents();
    updateClock();
    window.setInterval(updateClock, 1000);
    renderEnchantmentBranchFilter();
    renderEnchantmentBranchSelect();
    resetSpellForm();
    resetEnchantmentForm();
    refreshAllData();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initAdminWikiDataPage);
} else {
    initAdminWikiDataPage();
}
