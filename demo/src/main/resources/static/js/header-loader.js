/**
 * header-loader.js - Tải và inject header dùng chung cho toàn bộ dự án.
 * Dùng fetch() để lấy nội dung header.html rồi chèn vào #header-placeholder.
 * Sau khi inject xong, tự động highlight link active, bật dropdown nav
 * và kích hoạt lại Google Sign-In nếu có.
 */
(function () {
    function getWikiPageKey() {
        const path = window.location.pathname.toLowerCase();
        const filename = path.substring(path.lastIndexOf('/') + 1).replace('.html', '');
        if (filename !== 'wiki') return '';

        const rawTab = (new URL(window.location.href).searchParams.get('tab') || '').trim().toLowerCase();
        switch (rawTab) {
            case 'spells':
                return 'wiki-spells';
            case 'enchantments':
                return 'wiki-enchantments';
            case '':
            case 'heroes':
            default:
                return 'wiki-home';
        }
    }

    function getCurrentPage() {
        const path = window.location.pathname.toLowerCase();
        const filename = path.substring(path.lastIndexOf('/') + 1).replace('.html', '');
        if (filename === 'esports-leaderboard') return 'esports';
        const wikiPageKey = getWikiPageKey();
        if (wikiPageKey) return wikiPageKey;
        if (path === '/esports/data' || path === '/esports/data/' || filename === 'esports-data') return 'esports-data';
        if (path === '/tier-list' || path === '/tier-list/' || filename === 'tier-list') return 'tier-list-meta';
        if (path.endsWith('/tier-list/all') || filename === 'tier-list-all') return 'tier-list-all';
        if (path.endsWith('/tier-list/mine') || filename === 'tier-list-mine') return 'tier-list-mine';
        if (/\/tier-list\/\d+$/.test(path) || filename === 'tier-list-detail') return 'tier-list-detail';
        if (filename.startsWith('tier-list')) return 'tier-list';
        return filename || 'index';
    }

    function setActiveLink() {
        const currentPage = getCurrentPage();
        const isBanPickPage = currentPage.startsWith('ban-pick');
        const isTierListPage = currentPage.startsWith('tier-list');
        const isWikiPage = currentPage.startsWith('wiki-') || currentPage === 'esports-data';
        const navLinks = document.querySelectorAll('nav [data-page]');

        navLinks.forEach(link => {
            const page = link.getAttribute('data-page');
            const isActive = page === currentPage
                || (page === 'ban-pick' && isBanPickPage)
                || (page === 'tier-list' && isTierListPage)
                || (page === 'wiki' && isWikiPage);
            link.classList.toggle('active', isActive);
        });
    }

    function setupDropdowns() {
        const dropdowns = document.querySelectorAll('.nav-dropdown');
        if (!dropdowns.length) return;

        function closeAllDropdowns() {
            dropdowns.forEach(dropdown => {
                dropdown.classList.remove('is-open');
                const trigger = dropdown.querySelector('.nav-dropdown-trigger');
                if (trigger) trigger.setAttribute('aria-expanded', 'false');
            });
        }

        dropdowns.forEach(dropdown => {
            const trigger = dropdown.querySelector('.nav-dropdown-trigger');
            if (!trigger) return;

            trigger.addEventListener('click', event => {
                event.preventDefault();
                const willOpen = !dropdown.classList.contains('is-open');
                closeAllDropdowns();
                dropdown.classList.toggle('is-open', willOpen);
                trigger.setAttribute('aria-expanded', willOpen ? 'true' : 'false');
            });

            trigger.addEventListener('keydown', event => {
                if (event.key === 'Escape') {
                    closeAllDropdowns();
                    trigger.blur();
                    return;
                }

                if (event.key === 'ArrowDown') {
                    event.preventDefault();
                    dropdown.classList.add('is-open');
                    trigger.setAttribute('aria-expanded', 'true');
                    dropdown.querySelector('.nav-dropdown-item')?.focus();
                }
            });
        });

        document.addEventListener('click', event => {
            if (event.target.closest('.nav-dropdown')) return;
            closeAllDropdowns();
        });

        document.addEventListener('keydown', event => {
            if (event.key === 'Escape') closeAllDropdowns();
        });
    }

    function loadHeader() {
        const placeholder = document.getElementById('header-placeholder');
        if (!placeholder) return;

        fetch('/html/header.html?v=20260508-wiki-dropdown', { cache: 'no-store' })
            .then(response => {
                if (!response.ok) throw new Error('Không thể tải header.html');
                return response.text();
            })
            .then(html => {
                placeholder.innerHTML = html;
                setActiveLink();
                setupDropdowns();
                if (typeof renderGoogleButton === 'function') {
                    renderGoogleButton();
                }
                document.dispatchEvent(new CustomEvent('headerLoaded'));
            })
            .catch(err => {
                console.error('Header load error:', err);
            });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', loadHeader);
    } else {
        loadHeader();
    }
})();
