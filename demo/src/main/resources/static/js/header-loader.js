/**
 * header-loader.js - Tải và inject header dùng chung cho toàn bộ dự án.
 * Dùng fetch() để lấy nội dung header.html rồi chèn vào #header-placeholder.
 * Sau khi inject xong, tự động highlight link active, bật dropdown nav
 * và kích hoạt lại Google Sign-In nếu có.
 */
(function () {
    function getCurrentPage() {
        const path = window.location.pathname;
        const filename = path.substring(path.lastIndexOf('/') + 1).replace('.html', '');
        if (filename === 'tactics-guides') return 'giao-an';
        if (filename === 'esports-leaderboard') return 'esports';
        return filename || 'index';
    }

    function setActiveLink() {
        const currentPage = getCurrentPage();
        const isBanPickPage = currentPage.startsWith('ban-pick');
        const navLinks = document.querySelectorAll('nav [data-page]');

        navLinks.forEach(link => {
            const page = link.getAttribute('data-page');
            const isActive = page === currentPage || (page === 'ban-pick' && isBanPickPage);
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

        fetch('/html/header.html?v=20260505-banpick-dropdown', { cache: 'no-store' })
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
