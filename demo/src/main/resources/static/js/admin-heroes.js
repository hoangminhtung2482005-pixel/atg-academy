function showToast(msg, type) {
    var t = document.getElementById('toast');
    t.textContent = (type === 'ok' ? '✅ ' : '❌ ') + msg;
    t.className = 'toast-box ' + (type === 'ok' ? 'toast-ok' : 'toast-err');
    setTimeout(function () { t.classList.add('show'); }, 30);
    setTimeout(function () { t.classList.remove('show'); }, 3500);
}

function updateClock() {
    document.getElementById('header-clock').textContent = new Date().toLocaleString('vi-VN');
}
setInterval(updateClock, 1000);
updateClock();

function setBtnLoading(btnId, loading) {
    var btn = document.getElementById(btnId);
    if (!btn) return;
    if (loading) {
        btn.dataset.originalText = btn.innerHTML;
        btn.innerHTML = '<span class="spinner"></span> Đang xử lý...';
        btn.disabled = true;
        btn.style.opacity = '0.6';
    } else {
        btn.innerHTML = btn.dataset.originalText || btn.innerHTML;
        btn.disabled = false;
        btn.style.opacity = '1';
    }
}

function escapeAdminHtml(value) {
    return String(value == null ? '' : value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

async function readAdminApiError(response) {
    var fallback = 'Thao tác thất bại';
    try {
        var text = await response.text();
        if (!text) return fallback;
        var json = JSON.parse(text);
        return json.error || json.detail || json.message || json.title || fallback;
    } catch (ignored) {
        return fallback;
    }
}

function normalizeAssetUrl(url) {
    if (!url) return '';
    if (/^(https?:)?\/\//i.test(url) || url.startsWith('data:') || url.startsWith('/')) {
        return url;
    }
    return '/' + url.replace(/^\.?\//, '');
}

var adminHeroes = [];
var adminAttributes = [];
var currentHeroDetail = null;
var editingAttributeId = null;
var HERO_SUBTITLE_DEFAULT = 'Chỉnh sửa chất tướng, vị trí, đặc điểm và mô tả Wiki';

function normalizeHeroFilterText(value) {
    return String(value == null ? '' : value)
        .trim()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/[đĐ]/g, 'd')
        .toLowerCase();
}

function heroClassesOf(hero) {
    if (Array.isArray(hero && hero.classes) && hero.classes.length) {
        return hero.classes.filter(Boolean);
    }
    return hero && hero.heroClass ? [hero.heroClass] : [];
}

function heroAttributesOf(hero) {
    return Array.isArray(hero && hero.attributes) ? hero.attributes.filter(Boolean) : [];
}

function getHeroFilterState() {
    return {
        search: normalizeHeroFilterText(document.getElementById('hero-search') && document.getElementById('hero-search').value),
        heroClass: document.getElementById('hero-class-filter') ? document.getElementById('hero-class-filter').value : '',
        role: document.getElementById('hero-role-filter') ? document.getElementById('hero-role-filter').value : ''
    };
}

function hasActiveHeroFilters(state) {
    return !!(state.search || state.heroClass || state.role);
}

function heroMatchesSearch(hero, query) {
    if (!query) return true;
    var values = [
        hero.name,
        hero.slug,
        hero.heroClass
    ]
        .concat(heroClassesOf(hero))
        .concat(Array.isArray(hero.roles) ? hero.roles : [])
        .concat(heroAttributesOf(hero));

    return values.some(function (value) {
        return normalizeHeroFilterText(value).includes(query);
    });
}

function heroMatchesFilters(hero, state) {
    var roles = Array.isArray(hero.roles) ? hero.roles : [];
    var classes = heroClassesOf(hero);
    return heroMatchesSearch(hero, state.search)
        && (!state.heroClass || classes.includes(state.heroClass))
        && (!state.role || roles.includes(state.role));
}

function updateHeroSubtitle(visibleCount, totalCount, filtersActive) {
    var subtitle = document.getElementById('hero-subtitle');
    if (!subtitle) return;
    if (filtersActive) {
        subtitle.textContent = 'Đang hiển thị ' + visibleCount + ' / ' + totalCount + ' tướng';
        return;
    }
    subtitle.innerHTML = HERO_SUBTITLE_DEFAULT + ' — <span id="hero-total-count" class="text-accent-blue">' + totalCount + '</span> tướng';
}

function updateHeroFilterStyles(state) {
    [
        ['hero-search', state.search],
        ['hero-class-filter', state.heroClass],
        ['hero-role-filter', state.role]
    ].forEach(function (item) {
        var element = document.getElementById(item[0]);
        if (!element) return;
        element.classList.toggle('is-active', !!item[1]);
    });

    var resetButton = document.getElementById('btn-hero-reset');
    if (resetButton) {
        resetButton.classList.toggle('is-active', hasActiveHeroFilters(state));
    }
}

function badgeList(values, emptyText) {
    var list = Array.isArray(values) ? values.filter(Boolean) : [];
    if (list.length === 0) {
        return '<span class="text-xs text-slate-400">' + escapeAdminHtml(emptyText || '-') + '</span>';
    }
    return list.map(function (value) {
        return '<span class="inline-flex items-center px-2 py-1 rounded-md border border-slate-200 bg-slate-50 text-[0.68rem] font-semibold text-slate-600">' + escapeAdminHtml(value) + '</span>';
    }).join(' ');
}

function heroAvatarHtml(hero) {
    var label = (hero.name || '?').trim();
    var initial = escapeAdminHtml((label.charAt(0) || '?').toUpperCase());
    var fallback = '<div class="w-11 h-11 rounded-lg bg-slate-100 border border-slate-200 flex items-center justify-center text-xs font-bold text-slate-500 mx-auto">' + initial + '</div>';
    if (!hero.avatarUrl) return fallback;
    var src = escapeAdminHtml(normalizeAssetUrl(hero.avatarUrl));
    var alt = escapeAdminHtml(label);
    return '<div class="relative w-11 h-11 mx-auto">' +
        '<img src="' + src + '" alt="' + alt + '" class="w-11 h-11 rounded-lg object-cover bg-slate-50 border border-slate-200" onerror="this.style.display=\'none\'; this.nextElementSibling.style.display=\'flex\'">' +
        '<div style="display:none" class="absolute inset-0 rounded-lg bg-slate-100 border border-slate-200 items-center justify-center text-xs font-bold text-slate-500">' + initial + '</div>' +
        '</div>';
}

function renderAdminHeroes(heroes, filtersActive) {
    var tbody = document.getElementById('heroes-tbody');
    if (!tbody) return;
    var visibleHeroes = Array.isArray(heroes) ? heroes : [];
    updateHeroSubtitle(visibleHeroes.length, adminHeroes.length, !!filtersActive);

    if (adminHeroes.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-16 text-slate-400 text-sm">Chưa có dữ liệu tướng.</td></tr>';
        return;
    }

    if (visibleHeroes.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-16 text-slate-400 text-sm">Không tìm thấy tướng phù hợp.</td></tr>';
        return;
    }

    tbody.innerHTML = visibleHeroes.map(function (hero) {
        return '<tr class="match-row border-b border-slate-200">' +
            '<td class="px-4 py-3 text-center">' + heroAvatarHtml(hero) + '</td>' +
            '<td class="px-4 py-3"><div class="text-sm font-semibold text-slate-900">' + escapeAdminHtml(hero.name || '-') + '</div>' +
            '<div class="text-[0.7rem] text-slate-400">ID #' + escapeAdminHtml(hero.id) + '</div></td>' +
            '<td class="px-4 py-3 text-xs text-slate-500 font-mono">' + escapeAdminHtml(hero.slug || '-') + '</td>' +
            '<td class="px-4 py-3 text-center"><div class="flex flex-wrap justify-center gap-1.5">' + badgeList(heroClassesOf(hero), '-') + '</div></td>' +
            '<td class="px-4 py-3"><div class="flex flex-wrap gap-1.5">' + badgeList(hero.roles, '-') + '</div></td>' +
            '<td class="px-4 py-3"><div class="flex flex-wrap gap-1.5">' + badgeList(heroAttributesOf(hero), '-') + '</div></td>' +
            '<td class="px-4 py-3 text-center">' +
            '<button type="button" onclick="openHeroModal(' + Number(hero.id) + ')" class="btn-press bg-blue-50 hover:bg-blue-100 text-blue-700 border border-blue-100 font-semibold text-xs px-3 py-2 rounded-lg transition-all">Chỉnh sửa</button>' +
            '</td>' +
            '</tr>';
    }).join('');
}

function applyHeroFilters() {
    var state = getHeroFilterState();
    updateHeroFilterStyles(state);
    renderAdminHeroes(adminHeroes.filter(function (hero) {
        return heroMatchesFilters(hero, state);
    }), hasActiveHeroFilters(state));
}

function resetHeroFilters() {
    var searchInput = document.getElementById('hero-search');
    var classFilter = document.getElementById('hero-class-filter');
    var roleFilter = document.getElementById('hero-role-filter');
    if (searchInput) searchInput.value = '';
    if (classFilter) classFilter.value = '';
    if (roleFilter) roleFilter.value = '';
    applyHeroFilters();
}

async function loadAdminHeroes() {
    var tbody = document.getElementById('heroes-tbody');
    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="7" class="text-center py-16 text-slate-400"><div class="spinner mx-auto mb-3"></div><p class="text-sm">Đang tải danh sách tướng...</p></td></tr>';
    try {
        var response = await fetch('/api/admin/wiki/heroes');
        if (!response.ok) throw new Error(await readAdminApiError(response));
        adminHeroes = await response.json();
        if (!Array.isArray(adminHeroes)) adminHeroes = [];
        applyHeroFilters();
    } catch (error) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-16 text-accent-red/70 text-sm">⚠ ' + escapeAdminHtml(error.message) + '</td></tr>';
    }
}

function optionHtml(value, label, selected) {
    return '<option value="' + escapeAdminHtml(value) + '"' + (selected ? ' selected' : '') + '>' + escapeAdminHtml(label) + '</option>';
}

function checkboxHtml(name, value, label, checked) {
    var id = name + '-' + encodeURIComponent(String(value)).replace(/%/g, '-');
    return '<label for="' + escapeAdminHtml(id) + '" class="flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 cursor-pointer hover:border-blue-200 hover:bg-blue-50/50 transition-colors">' +
        '<input type="checkbox" id="' + escapeAdminHtml(id) + '" name="' + escapeAdminHtml(name) + '" value="' + escapeAdminHtml(value) + '"' + (checked ? ' checked' : '') + ' class="rounded border-slate-300 text-blue-600 focus:ring-blue-500">' +
        '<span>' + escapeAdminHtml(label) + '</span>' +
        '</label>';
}

function checkedValues(name) {
    return Array.from(document.querySelectorAll('input[name="' + name + '"]:checked')).map(function (input) {
        return input.value;
    });
}

function renderHeroRoleSuggestion(detail) {
    var suggestion = document.getElementById('hero-role-suggestion');
    if (!suggestion) return;
    var roles = Array.isArray(detail && detail.suggestedRoles) ? detail.suggestedRoles : [];
    if (!roles.length) {
        suggestion.textContent = '';
        suggestion.classList.add('hidden');
        return;
    }
    suggestion.textContent = 'Gợi ý vị trí: ' + roles.join(', ');
    suggestion.classList.remove('hidden');
}

async function openHeroModal(heroId) {
    try {
        var response = await fetch('/api/admin/wiki/heroes/' + heroId);
        if (!response.ok) throw new Error(await readAdminApiError(response));
        currentHeroDetail = await response.json();
        var hero = currentHeroDetail.hero || {};

        document.getElementById('hf-id').value = hero.id || '';
        document.getElementById('hf-name').value = hero.name || '';
        document.getElementById('hf-slug').value = hero.slug || '';
        document.getElementById('hf-description').value = hero.description || '';
        document.getElementById('hf-avatar').value = hero.avatarUrl || '';
        document.getElementById('hf-portrait').value = hero.portraitUrl || '';
        document.getElementById('hf-banner').value = hero.bannerUrl || '';
        document.getElementById('hero-modal-title').textContent = 'Chỉnh sửa tướng';
        document.getElementById('hero-modal-subtitle').textContent = hero.name ? (hero.name + ' #' + hero.id) : '';

        var difficultySelect = document.getElementById('hf-difficulty');
        var difficultyOptions = currentHeroDetail.difficulties || [];
        difficultySelect.innerHTML = optionHtml('', '-', !hero.difficulty) + difficultyOptions.map(function (value) {
            return optionHtml(value, value, value === hero.difficulty);
        }).join('');

        var selectedClasses = new Set(heroClassesOf(hero));
        document.getElementById('hero-class-options').innerHTML = (currentHeroDetail.availableClasses || []).map(function (heroClass) {
            return checkboxHtml('hero-class', heroClass, heroClass, selectedClasses.has(heroClass));
        }).join('');

        var selectedRoles = new Set(Array.isArray(hero.roles) ? hero.roles : []);
        document.getElementById('hero-role-options').innerHTML = (currentHeroDetail.availableRoles || []).map(function (role) {
            return checkboxHtml('hero-role', role.code, role.code + (role.name ? ' - ' + role.name : ''), selectedRoles.has(role.code));
        }).join('');

        var selectedAttributes = new Set(heroAttributesOf(hero));
        document.getElementById('hero-attribute-options').innerHTML = (currentHeroDetail.availableAttributes || []).map(function (attribute) {
            return checkboxHtml('hero-attribute', attribute.name, attribute.name, selectedAttributes.has(attribute.name));
        }).join('');

        renderHeroRoleSuggestion(currentHeroDetail);
        document.getElementById('hero-modal').style.display = 'flex';
    } catch (error) {
        showToast(error.message, 'err');
    }
}

function closeHeroModal() {
    document.getElementById('hero-modal').style.display = 'none';
    currentHeroDetail = null;
}

async function putJson(url, body) {
    var response = await fetch(url, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    });
    if (!response.ok) throw new Error(await readAdminApiError(response));
    return response.json();
}

async function saveHeroChanges(event) {
    event.preventDefault();
    var id = document.getElementById('hf-id').value;
    if (!id) return;

    var body = {
        name: document.getElementById('hf-name').value.trim(),
        slug: document.getElementById('hf-slug').value.trim(),
        classes: checkedValues('hero-class'),
        description: document.getElementById('hf-description').value.trim(),
        avatarUrl: document.getElementById('hf-avatar').value.trim(),
        portraitUrl: document.getElementById('hf-portrait').value.trim(),
        bannerUrl: document.getElementById('hf-banner').value.trim(),
        difficulty: document.getElementById('hf-difficulty').value
    };

    setBtnLoading('btn-hero-submit', true);
    try {
        await putJson('/api/admin/wiki/heroes/' + id, body);
        await putJson('/api/admin/wiki/heroes/' + id + '/roles', { roles: checkedValues('hero-role') });
        await putJson('/api/admin/wiki/heroes/' + id + '/attributes', { attributes: checkedValues('hero-attribute') });
        showToast('Đã lưu thông tin tướng.', 'ok');
        closeHeroModal();
        await loadAdminHeroes();
    } catch (error) {
        showToast(error.message, 'err');
    } finally {
        setBtnLoading('btn-hero-submit', false);
    }
}

function renderAdminAttributes(attributes) {
    var tbody = document.getElementById('attributes-tbody');
    if (!tbody) return;

    if (!Array.isArray(attributes) || attributes.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="px-4 py-8 text-center text-sm text-slate-400">Chưa có đặc điểm nào.</td></tr>';
        return;
    }

    tbody.innerHTML = attributes.map(function (attribute) {
        var icon = attribute.iconUrl
            ? '<img src="' + escapeAdminHtml(normalizeAssetUrl(attribute.iconUrl)) + '" alt="' + escapeAdminHtml(attribute.name) + '" class="w-8 h-8 rounded-lg object-cover border border-slate-200 bg-slate-50" onerror="this.style.display=\'none\'">'
            : '<span class="text-xs text-slate-400">-</span>';

        return '<tr class="border-b border-slate-200">' +
            '<td class="px-4 py-3 text-sm font-semibold text-slate-900">' + escapeAdminHtml(attribute.name) + '</td>' +
            '<td class="px-4 py-3 text-sm text-slate-500">' + escapeAdminHtml(attribute.description || '-') + '</td>' +
            '<td class="px-4 py-3 text-center">' + icon + '</td>' +
            '<td class="px-4 py-3 text-center text-sm text-slate-500">' + escapeAdminHtml(attribute.usageCount) + '</td>' +
            '<td class="px-4 py-3 text-center">' +
            '<div class="inline-flex gap-2">' +
            '<button type="button" onclick="openAttributeModal(' + Number(attribute.id) + ')" class="btn-press bg-blue-50 hover:bg-blue-100 text-blue-700 border border-blue-100 font-semibold text-xs px-3 py-2 rounded-lg transition-all">Sửa</button>' +
            '<button type="button" onclick="deleteAttribute(' + Number(attribute.id) + ')" class="btn-press bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-100 font-semibold text-xs px-3 py-2 rounded-lg transition-all">Xóa</button>' +
            '</div>' +
            '</td>' +
            '</tr>';
    }).join('');
}

async function loadAdminAttributes() {
    var tbody = document.getElementById('attributes-tbody');
    if (tbody) {
        tbody.innerHTML = '<tr><td colspan="5" class="px-4 py-8 text-center text-sm text-slate-400"><div class="spinner mx-auto mb-3"></div>Đang tải đặc điểm...</td></tr>';
    }
    try {
        var response = await fetch('/api/admin/wiki/attributes');
        if (!response.ok) throw new Error(await readAdminApiError(response));
        adminAttributes = await response.json();
        if (!Array.isArray(adminAttributes)) adminAttributes = [];
        renderAdminAttributes(adminAttributes);
    } catch (error) {
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="5" class="px-4 py-8 text-center text-sm text-rose-600">' + escapeAdminHtml(error.message) + '</td></tr>';
        }
    }
}

function openAttributeModal(attributeId) {
    editingAttributeId = attributeId || null;
    var attribute = adminAttributes.find(function (item) { return Number(item.id) === Number(attributeId); }) || {};
    document.getElementById('attribute-modal-title').textContent = editingAttributeId ? 'Sửa đặc điểm' : 'Thêm đặc điểm';
    document.getElementById('af-name').value = attribute.name || '';
    document.getElementById('af-description').value = attribute.description || '';
    document.getElementById('af-icon-url').value = attribute.iconUrl || '';
    document.getElementById('af-sort-order').value = attribute.sortOrder == null ? '' : attribute.sortOrder;
    document.getElementById('attribute-modal').style.display = 'flex';
}

function closeAttributeModal() {
    editingAttributeId = null;
    document.getElementById('attribute-modal').style.display = 'none';
}

async function saveAttribute(event) {
    event.preventDefault();
    var body = {
        name: document.getElementById('af-name').value.trim(),
        description: document.getElementById('af-description').value.trim(),
        iconUrl: document.getElementById('af-icon-url').value.trim(),
        sortOrder: document.getElementById('af-sort-order').value === '' ? null : Number(document.getElementById('af-sort-order').value)
    };

    setBtnLoading('btn-attribute-submit', true);
    try {
        var response = await fetch(editingAttributeId ? '/api/admin/wiki/attributes/' + editingAttributeId : '/api/admin/wiki/attributes', {
            method: editingAttributeId ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        if (!response.ok) throw new Error(await readAdminApiError(response));
        closeAttributeModal();
        await loadAdminAttributes();
        if (currentHeroDetail && currentHeroDetail.hero && currentHeroDetail.hero.id) {
            await openHeroModal(currentHeroDetail.hero.id);
        }
        showToast('Đã lưu đặc điểm.', 'ok');
    } catch (error) {
        showToast(error.message, 'err');
    } finally {
        setBtnLoading('btn-attribute-submit', false);
    }
}

async function deleteAttribute(id) {
    var attribute = adminAttributes.find(function (item) { return Number(item.id) === Number(id); }) || {};
    var name = attribute.name || ('#' + id);
    if (!window.confirm('Xóa đặc điểm "' + name + '"?')) {
        return;
    }
    try {
        var response = await fetch('/api/admin/wiki/attributes/' + id, { method: 'DELETE' });
        if (!response.ok) throw new Error(await readAdminApiError(response));
        await loadAdminAttributes();
        if (currentHeroDetail && currentHeroDetail.hero && currentHeroDetail.hero.id) {
            await openHeroModal(currentHeroDetail.hero.id);
        }
        showToast('Đã xóa đặc điểm.', 'ok');
    } catch (error) {
        showToast(error.message, 'err');
    }
}

loadAdminHeroes();
loadAdminAttributes();
