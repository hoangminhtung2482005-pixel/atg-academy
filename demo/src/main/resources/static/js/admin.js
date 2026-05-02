(function () {
    var ATTRIBUTE_API = '/api/admin/attributes';
    var heroAttributes = [];
    var editingHeroAttributeId = null;
    var deletingHeroAttributeId = null;

    function safeHtml(value) {
        if (typeof escapeAdminHtml === 'function') {
            return escapeAdminHtml(value);
        }
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    async function readError(response) {
        if (typeof readAdminApiError === 'function') {
            return readAdminApiError(response);
        }
        return 'Thao tác thất bại';
    }

    async function apiJson(url, options) {
        var response = await fetch(url, options || {});
        if (!response.ok) {
            throw new Error(await readError(response));
        }
        return response.status === 204 ? null : response.json();
    }

    function notify(message, type) {
        if (typeof showToast === 'function') {
            showToast(message, type);
            return;
        }
        window.alert(message);
    }

    function setLoading(buttonId, loading) {
        if (typeof setBtnLoading === 'function') {
            setBtnLoading(buttonId, loading);
        }
    }

    function getAttributeById(id) {
        return heroAttributes.find(function (item) {
            return Number(item.id) === Number(id);
        }) || null;
    }

    function renderHeroAttributes(attributes) {
        var tbody = document.getElementById('hero-attributes-tbody');
        if (!tbody) return;

        if (!Array.isArray(attributes) || attributes.length === 0) {
            tbody.innerHTML = '<tr><td colspan="3" class="text-center py-16 text-slate-400 text-sm">Chưa có đặc điểm nào.</td></tr>';
            return;
        }

        tbody.innerHTML = attributes.map(function (attribute) {
            var id = Number(attribute.id);
            var isDeleting = deletingHeroAttributeId === id;
            var disabled = isDeleting ? ' disabled style="opacity:0.55;cursor:not-allowed"' : '';
            var deleteLabel = isDeleting ? 'Đang xóa...' : 'Xóa';

            return '<tr class="match-row border-b border-slate-200">' +
                '<td class="px-4 py-3 text-sm text-slate-500 font-mono">#' + safeHtml(attribute.id) + '</td>' +
                '<td class="px-4 py-3 text-sm font-semibold text-slate-900">' + safeHtml(attribute.name || '-') + '</td>' +
                '<td class="px-4 py-3 text-center">' +
                    '<div class="inline-flex gap-2">' +
                        '<button type="button" data-attribute-action="edit" data-attribute-id="' + safeHtml(id) + '" class="btn-press bg-blue-50 hover:bg-blue-100 text-blue-700 border border-blue-100 font-semibold text-xs px-3 py-2 rounded-lg transition-all"' + disabled + '>Sửa</button>' +
                        '<button type="button" data-attribute-action="delete" data-attribute-id="' + safeHtml(id) + '" class="btn-press bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-100 font-semibold text-xs px-3 py-2 rounded-lg transition-all"' + disabled + '>' + safeHtml(deleteLabel) + '</button>' +
                    '</div>' +
                '</td>' +
                '</tr>';
        }).join('');
    }

    async function loadHeroAttributes() {
        var tbody = document.getElementById('hero-attributes-tbody');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="3" class="text-center py-16 text-slate-400"><div class="spinner mx-auto mb-3"></div><p class="text-sm">Đang tải danh sách đặc điểm...</p></td></tr>';
        }

        try {
            var attributes = await apiJson(ATTRIBUTE_API);
            heroAttributes = Array.isArray(attributes) ? attributes : [];
            renderHeroAttributes(heroAttributes);
        } catch (error) {
            if (tbody) {
                tbody.innerHTML = '<tr><td colspan="3" class="text-center py-16 text-rose-600 text-sm">' + safeHtml(error.message) + '</td></tr>';
            }
        }
    }

    function openHeroAttributeModal(attributeId) {
        editingHeroAttributeId = attributeId || null;
        var attribute = getAttributeById(attributeId) || {};

        document.getElementById('hero-attribute-modal-title').textContent = editingHeroAttributeId ? 'Sửa đặc điểm' : 'Thêm đặc điểm';
        document.getElementById('haf-id').value = attribute.id || '';
        document.getElementById('haf-name').value = attribute.name || '';
        document.getElementById('hero-attribute-modal').style.display = 'flex';
        document.getElementById('haf-name').focus();
    }

    function closeHeroAttributeModal() {
        editingHeroAttributeId = null;
        document.getElementById('hero-attribute-form').reset();
        document.getElementById('haf-id').value = '';
        document.getElementById('hero-attribute-modal').style.display = 'none';
    }

    async function submitHeroAttributeForm(event) {
        event.preventDefault();
        var name = document.getElementById('haf-name').value.trim();
        if (!name) {
            notify('Vui lòng nhập tên đặc điểm.', 'err');
            return;
        }

        setLoading('btn-hero-attribute-submit', true);
        try {
            var wasEditing = !!editingHeroAttributeId;
            await apiJson(editingHeroAttributeId ? ATTRIBUTE_API + '/' + editingHeroAttributeId : ATTRIBUTE_API, {
                method: wasEditing ? 'PATCH' : 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name: name })
            });

            closeHeroAttributeModal();
            await loadHeroAttributes();
            notify(wasEditing ? 'Đã cập nhật đặc điểm.' : 'Đã thêm đặc điểm.', 'ok');
        } catch (error) {
            notify(error.message, 'err');
        } finally {
            setLoading('btn-hero-attribute-submit', false);
        }
    }

    async function deleteHeroAttribute(id) {
        if (deletingHeroAttributeId != null) return;

        var attribute = getAttributeById(id) || {};
        var name = attribute.name || ('#' + id);
        if (!window.confirm('Xóa đặc điểm "' + name + '"? Các liên kết tướng với đặc điểm này cũng sẽ được gỡ.')) {
            return;
        }

        deletingHeroAttributeId = Number(id);
        renderHeroAttributes(heroAttributes);
        try {
            await apiJson(ATTRIBUTE_API + '/' + id, { method: 'DELETE' });
            await loadHeroAttributes();
            notify('Đã xóa đặc điểm.', 'ok');
        } catch (error) {
            notify(error.message, 'err');
        } finally {
            deletingHeroAttributeId = null;
            renderHeroAttributes(heroAttributes);
        }
    }

    function bindHeroAttributeTable() {
        var tbody = document.getElementById('hero-attributes-tbody');
        if (!tbody) return;

        tbody.addEventListener('click', function (event) {
            var button = event.target.closest('[data-attribute-action][data-attribute-id]');
            if (!button || button.disabled) return;

            var id = Number(button.dataset.attributeId);
            if (button.dataset.attributeAction === 'edit') {
                openHeroAttributeModal(id);
            }
            if (button.dataset.attributeAction === 'delete') {
                deleteHeroAttribute(id);
            }
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        bindHeroAttributeTable();
        var activeAttributesPage = document.getElementById('page-attributes');
        if (activeAttributesPage && activeAttributesPage.classList.contains('active')) {
            loadHeroAttributes();
        }
    });

    window.loadHeroAttributes = loadHeroAttributes;
    window.openHeroAttributeModal = openHeroAttributeModal;
    window.closeHeroAttributeModal = closeHeroAttributeModal;
    window.submitHeroAttributeForm = submitHeroAttributeForm;
})();
