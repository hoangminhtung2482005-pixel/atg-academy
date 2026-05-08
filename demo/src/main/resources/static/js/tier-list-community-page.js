(function () {
    const SHELL_URLS = Object.freeze({
        all: '/html/tier-list-community-shell.html?v=20260507-tierlist-saved-state',
        mine: '/html/tier-list-mine-shell.html?v=20260507-tierlist-saved-state'
    });
    const APP_SCRIPT_URL = '/js/tier-list-app.js?v=20260507-tierlist-auto-hero-score';
    const PAGE_META = Object.freeze({
        all: {
            documentTitle: 'Tất cả tier list - AoV Tactics & Guides',
            title: 'Tất cả tier list',
            subtitle: 'Xem toàn bộ Community Tier List đang có trên ATG Academy.'
        },
        mine: {
            documentTitle: 'Tier list bản thân - AoV Tactics & Guides',
            title: 'Tier list bản thân',
            subtitle: 'Theo dõi Community Tier List do bạn tạo và các tier list bạn đã lưu dưới dạng bookmark/reference.'
        }
    });

    function normalizeCommunityView(value) {
        const normalized = String(value || '').trim().toLowerCase();
        return PAGE_META[normalized] ? normalized : 'all';
    }

    function applyCommunityPageMeta(root, view) {
        const meta = PAGE_META[view] || PAGE_META.all;
        document.title = meta.documentTitle;

        const title = root.querySelector('[data-community-page-title]');
        if (title) title.textContent = meta.title;

        const subtitle = root.querySelector('[data-community-page-subtitle]');
        if (subtitle) subtitle.textContent = meta.subtitle;
    }

    async function loadCommunityShell() {
        const root = document.getElementById('tier-list-community-root');
        if (!root) return;

        const view = normalizeCommunityView(document.body?.dataset?.communityView);
        const shellUrl = SHELL_URLS[view] || SHELL_URLS.all;

        try {
            const response = await fetch(shellUrl, { cache: 'no-store' });
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
