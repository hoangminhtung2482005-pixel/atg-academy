const attributeState = {
    attributes: [],
    filter: '',
    editingAttributeId: null
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
    console.error('[AdminAttributes API error]', {
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

function postJson(url, body) {
    return apiFetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    });
}

function updateError(message) {
    const target = byId('attributes-error');
    if (!target) return;
    if (!message) {
        target.textContent = '';
        target.classList.add('hidden');
        return;
    }
    target.textContent = message;
    target.classList.remove('hidden');
}

function showLoadingRows(message) {
    const tbody = byId('attributes-tbody');
    if (tbody) tbody.innerHTML = `<tr><td colspan="6" class="table-state">${escapeHtml(message)}</td></tr>`;
}

function filteredAttributes() {
    const search = normalizeText(attributeState.filter);
    if (!search) return attributeState.attributes;
    return attributeState.attributes.filter(attribute => [
        attribute.name,
        attribute.description,
        attribute.iconUrl,
        attribute.id
    ].some(value => normalizeText(value).includes(search)));
}

function updateAttributeHeader(visibleCount) {
    const pill = byId('attribute-count-pill');
    const total = attributeState.attributes.length;
    if (pill) pill.textContent = `${visibleCount} / ${total} đặc điểm`;
}

function renderAttributesTable() {
    const tbody = byId('attributes-tbody');
    if (!tbody) return;

    const attributes = filteredAttributes();
    updateAttributeHeader(attributes.length);

    if (!attributeState.attributes.length) {
        tbody.innerHTML = '<tr><td colspan="6" class="table-state">Chưa có đặc điểm nào.</td></tr>';
        return;
    }

    if (!attributes.length) {
        tbody.innerHTML = '<tr><td colspan="6" class="table-state">Không tìm thấy đặc điểm phù hợp.</td></tr>';
        return;
    }

    tbody.innerHTML = attributes.map(attribute => {
        const icon = attribute.iconUrl
            ? `<img class="attribute-icon" src="${escapeHtml(normalizeAssetUrl(attribute.iconUrl))}" alt="${escapeHtml(attribute.name)}" onerror="this.style.display='none'">`
            : '<span class="empty-muted">-</span>';
        return `
            <tr>
                <td><span class="slug">#${escapeHtml(attribute.id)}</span></td>
                <td><strong>${escapeHtml(attribute.name || '-')}</strong></td>
                <td>${escapeHtml(attribute.description || '-')}</td>
                <td>${icon}</td>
                <td>${escapeHtml(attribute.usageCount == null ? '-' : attribute.usageCount)}</td>
                <td>
                    <div class="row-actions">
                        <button type="button" class="btn btn-light btn-small" data-action="edit-attribute" data-attribute-id="${Number(attribute.id)}">Sửa</button>
                        <button type="button" class="btn btn-danger btn-small" data-action="delete-attribute" data-attribute-id="${Number(attribute.id)}">Xóa</button>
                    </div>
                </td>
            </tr>`;
    }).join('');
}

async function loadAttributes() {
    showLoadingRows('Đang tải đặc điểm...');
    updateError('');
    try {
        const attributes = await apiFetch('/api/admin/wiki/attributes', { headers: { Accept: 'application/json' }, cache: 'no-store' });
        attributeState.attributes = Array.isArray(attributes) ? attributes : [];
        renderAttributesTable();
    } catch (error) {
        updateError(`Không tải được đặc điểm.\n${error.message}`);
        showLoadingRows('Không tải được đặc điểm. Xem chi tiết lỗi phía trên và console.');
        updateAttributeHeader(0);
    }
}

function openAttributeModal(attributeId) {
    const attribute = attributeState.attributes.find(item => Number(item.id) === Number(attributeId)) || {};
    attributeState.editingAttributeId = attributeId ? Number(attributeId) : null;
    byId('attribute-modal-title').textContent = attributeState.editingAttributeId ? 'Sửa đặc điểm' : 'Thêm đặc điểm';
    byId('af-name').value = attribute.name || '';
    byId('af-description').value = attribute.description || '';
    byId('af-icon-url').value = attribute.iconUrl || '';
    byId('af-sort-order').value = attribute.sortOrder == null ? '' : attribute.sortOrder;
    byId('attribute-modal').classList.remove('hidden');
    byId('attribute-modal').setAttribute('aria-hidden', 'false');
}

function closeAttributeModal() {
    attributeState.editingAttributeId = null;
    const modal = byId('attribute-modal');
    if (modal) {
        modal.classList.add('hidden');
        modal.setAttribute('aria-hidden', 'true');
    }
}

async function saveAttribute(event) {
    event.preventDefault();
    const body = {
        name: byId('af-name').value.trim(),
        description: byId('af-description').value.trim(),
        iconUrl: byId('af-icon-url').value.trim(),
        sortOrder: byId('af-sort-order').value === '' ? null : Number(byId('af-sort-order').value)
    };

    setButtonLoading('btn-attribute-submit', true);
    try {
        if (attributeState.editingAttributeId) {
            await putJson(`/api/admin/wiki/attributes/${attributeState.editingAttributeId}`, body);
        } else {
            await postJson('/api/admin/wiki/attributes', body);
        }
        showToast('Đã lưu đặc điểm.', 'ok');
        closeAttributeModal();
        await loadAttributes();
    } catch (error) {
        console.error('[AdminAttributes saveAttribute failed]', error);
        showToast(error.message, 'err');
    } finally {
        setButtonLoading('btn-attribute-submit', false);
    }
}

async function deleteAttribute(attributeId) {
    const attribute = attributeState.attributes.find(item => Number(item.id) === Number(attributeId));
    const name = attribute && attribute.name ? attribute.name : `#${attributeId}`;
    if (!window.confirm(`Xóa đặc điểm "${name}"?`)) return;

    try {
        await apiFetch(`/api/admin/wiki/attributes/${Number(attributeId)}`, { method: 'DELETE' });
        showToast('Đã xóa đặc điểm.', 'ok');
        await loadAttributes();
    } catch (error) {
        console.error('[AdminAttributes deleteAttribute failed]', error);
        updateError(`Không xóa được đặc điểm.\n${error.message}`);
        showToast(error.message, 'err');
    }
}

function bindEvents() {
    byId('btn-refresh-attributes')?.addEventListener('click', loadAttributes);
    byId('btn-add-attribute')?.addEventListener('click', () => openAttributeModal());
    byId('btn-close-attribute-modal')?.addEventListener('click', closeAttributeModal);
    byId('btn-cancel-attribute')?.addEventListener('click', closeAttributeModal);
    byId('attribute-form')?.addEventListener('submit', saveAttribute);

    byId('attribute-search')?.addEventListener('input', event => {
        attributeState.filter = event.target.value;
        renderAttributesTable();
    });
    byId('btn-attribute-reset')?.addEventListener('click', () => {
        attributeState.filter = '';
        byId('attribute-search').value = '';
        renderAttributesTable();
    });

    byId('attributes-tbody')?.addEventListener('click', event => {
        const button = event.target.closest('[data-action]');
        if (!button) return;
        if (button.dataset.action === 'edit-attribute') openAttributeModal(button.dataset.attributeId);
        if (button.dataset.action === 'delete-attribute') deleteAttribute(button.dataset.attributeId);
    });

    byId('attribute-modal')?.addEventListener('click', event => {
        if (event.target === byId('attribute-modal')) closeAttributeModal();
    });

    document.addEventListener('keydown', event => {
        if (event.key === 'Escape') closeAttributeModal();
    });

    document.addEventListener('authExpired', () => {
        window.location.replace('/html/index.html');
    });
}

function initAdminAttributesPage() {
    if (!document.querySelector('[data-page="admin-attributes"]')) return;
    if (typeof requireRoleAccess === 'function' && !requireRoleAccess('Admin', '/html/index.html')) {
        return;
    }
    bindEvents();
    updateClock();
    window.setInterval(updateClock, 1000);
    loadAttributes();
}

window.AdminAttributesPage = {
    state: attributeState,
    loadAttributes,
    openAttributeModal
};

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initAdminAttributesPage);
} else {
    initAdminAttributesPage();
}
