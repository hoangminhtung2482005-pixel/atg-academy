(function () {
    const SHELL_URL = '/html/tier-list-community-shell.html?v=20260507-community-pages';
    const APP_SCRIPT_URL = '/js/tier-list-app.js?v=20260507-community-pages';
    const PAGE_META = Object.freeze({
        recommended: {
            documentTitle: 'Tier list đề xuất - AoV Tactics & Guides',
            title: 'Tier list đề xuất',
            subtitle: 'Hiển thị các tier list cộng đồng nổi bật theo logic đề xuất hiện tại.'
        },
        all: {
            documentTitle: 'Tất cả tier list - AoV Tactics & Guides',
            title: 'Tất cả tier list',
            subtitle: 'Xem toàn bộ Community Tier List đang có trên ATG Academy.'
        },
        mine: {
            documentTitle: 'Tier list bản thân - AoV Tactics & Guides',
            title: 'Tier list bản thân',
            subtitle: 'Danh sách Tier List cộng đồng do chính bạn tạo.'
        }
    });

    function normalizeCommunityView(value) {
        const normalized = String(value || '').trim().toLowerCase();
        if (normalized === 'featured') return 'recommended';
        return PAGE_META[normalized] ? normalized : 'recommended';
    }

    function applyCommunityPageMeta(root, view) {
        const meta = PAGE_META[view] || PAGE_META.recommended;
        document.title = meta.documentTitle;

        const title = root.querySelector('[data-community-page-title]');
        if (title) title.textContent = meta.title;

        const subtitle = root.querySelector('[data-community-page-subtitle]');
        if (subtitle) subtitle.textContent = meta.subtitle;

        const select = root.querySelector('#community-nav-select');
        if (select) select.value = view;
    }

    async function loadCommunityShell() {
        const root = document.getElementById('tier-list-community-root');
        if (!root) return;

        const view = normalizeCommunityView(document.body?.dataset?.communityView);

        try {
            const response = await fetch(SHELL_URL, { cache: 'no-store' });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);

            root.innerHTML = await response.text();
            applyCommunityPageMeta(root, view);

            const script = document.createElement('script');
            script.src = APP_SCRIPT_URL;
            script.defer = true;
            document.body.appendChild(script);
        } catch (error) {
            root.innerHTML = `
                <section id="tier-list-section">
                    <div class="community-section community-section--standalone">
                        <div class="community-status is-error">Không thể tải trang Community Tier List: ${String(error.message || error)}</div>
                    </div>
                </section>
            `;
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', loadCommunityShell, { once: true });
    } else {
        loadCommunityShell();
    }
})();
