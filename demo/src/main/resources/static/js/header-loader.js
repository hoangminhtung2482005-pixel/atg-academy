/**
 * header-loader.js - Shared layout loader cho header, footer va admin sidebar.
 * Giu nguyen entry script cu de cac page hien tai khong can doi business logic.
 */
(function () {
    const sharedLoader = window.__atgSharedLayoutLoader;
    if (sharedLoader && typeof sharedLoader.loadLayouts === 'function') {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', sharedLoader.loadLayouts, { once: true });
        } else {
            sharedLoader.loadLayouts();
        }
        return;
    }

    const PARTIAL_VERSION = '20260515-esports-nav';
    const partialCache = new Map();
    let dropdownListenersBound = false;
    let mobileMenuListenersBound = false;
    let adminHashListenerBound = false;

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
        if (path === '/esports' || path === '/esports/' || filename === 'esports' || filename === 'esports-leaderboard') {
            return 'esports-ranking';
        }
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

    function setActiveHeaderLink() {
        const currentPage = getCurrentPage();
        const isBanPickPage = currentPage.startsWith('ban-pick');
        const isTierListPage = currentPage.startsWith('tier-list');
        const isWikiPage = currentPage.startsWith('wiki-');
        const isEsportsPage = currentPage === 'esports-ranking' || currentPage === 'esports-data';
        const navLinks = document.querySelectorAll('nav [data-page]');

        navLinks.forEach(link => {
            const page = link.getAttribute('data-page');
            const isActive = page === currentPage
                || (page === 'ban-pick' && isBanPickPage)
                || (page === 'tier-list' && isTierListPage)
                || (page === 'esports' && isEsportsPage)
                || (page === 'wiki' && isWikiPage);
            link.classList.toggle('active', isActive);
        });

        document.querySelectorAll('.mobile-nav-group').forEach(group => {
            const trigger = group.querySelector('.mobile-nav-group-trigger');
            const hasActiveChild = !!group.querySelector('.mobile-nav-sublink.active');
            group.open = Boolean(hasActiveChild || trigger?.classList.contains('active'));
        });
    }

    function setupDropdowns() {
        const dropdowns = document.querySelectorAll('.nav-dropdown');
        if (!dropdowns.length || dropdownListenersBound) return;

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

        dropdownListenersBound = true;
    }

    function setupMobileMenu() {
        const toggle = document.querySelector('[data-mobile-menu-toggle]');
        const panel = document.querySelector('[data-mobile-nav]');
        if (!toggle || !panel || mobileMenuListenersBound) return;

        function setMobileMenuOpen(isOpen) {
            panel.hidden = !isOpen;
            toggle.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
        }

        toggle.addEventListener('click', () => {
            setMobileMenuOpen(panel.hidden);
        });

        panel.addEventListener('click', event => {
            if (event.target.closest('a[href]')) {
                setMobileMenuOpen(false);
            }
        });

        document.addEventListener('click', event => {
            if (panel.hidden) return;
            if (event.target.closest('[data-mobile-menu-toggle]') || event.target.closest('[data-mobile-nav]')) return;
            setMobileMenuOpen(false);
        });

        document.addEventListener('keydown', event => {
            if (event.key === 'Escape' && !panel.hidden) {
                setMobileMenuOpen(false);
            }
        });

        window.addEventListener('resize', () => {
            if (window.innerWidth >= 768) {
                setMobileMenuOpen(false);
            }
        });

        mobileMenuListenersBound = true;
    }

    function getCurrentAdminLinkKey() {
        const path = window.location.pathname.toLowerCase();
        const filename = path.substring(path.lastIndexOf('/') + 1).replace('.html', '');
        const hash = window.location.hash.toLowerCase();

        if (filename === 'admin') {
            switch (hash) {
                case '#users':
                    return 'admin-users';
                case '#guides':
                    return 'admin-guides';
                case '#ban-pick-rating':
                    return 'admin-ban-pick-rating';
                case '#aer-data':
                case '#teams':
                case '#esports':
                    return 'admin-aer-data';
                case '#dashboard':
                case '':
                default:
                    return 'admin-dashboard';
            }
        }

        if (filename === 'admin-esports-data') {
            switch (hash) {
                case '#import-management':
                    return 'admin-esports-import';
                case '#franchise-management':
                    return 'admin-esports-franchise';
                case '#tournament-management':
                    return 'admin-esports-tournament';
                case '#match-management':
                    return 'admin-esports-match';
                default:
                    return 'admin-esports-data';
            }
        }

        if (filename === 'admin-heroes') return 'admin-heroes';
        if (filename === 'admin-attributes') return 'admin-attributes';
        if (filename === 'admin-wiki-data') return 'admin-wiki-data';
        return '';
    }

    function setActiveAdminLink() {
        const currentKey = getCurrentAdminLinkKey();
        if (!currentKey) return;

        document.querySelectorAll('.admin-sidebar [data-admin-link]').forEach(link => {
            link.classList.toggle('active', link.getAttribute('data-admin-link') === currentKey);
        });
    }

    function bindAdminHashTracking() {
        if (adminHashListenerBound) return;
        window.addEventListener('hashchange', setActiveAdminLink);
        adminHashListenerBound = true;
    }

    function fetchPartial(url) {
        if (!partialCache.has(url)) {
            partialCache.set(url, fetch(url + '?v=' + PARTIAL_VERSION, { cache: 'no-store' })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Khong the tai ' + url);
                    }
                    return response.text();
                }));
        }

        return partialCache.get(url);
    }

    function resolveTargets(config) {
        const targets = [];

        if (config.selector) {
            document.querySelectorAll(config.selector).forEach(node => targets.push(node));
        }

        if (config.dataInclude) {
            document.querySelectorAll('[data-include="' + config.dataInclude + '"]').forEach(node => targets.push(node));
        }

        return targets.filter((node, index, list) => node && list.indexOf(node) === index);
    }

    function injectPartial(config) {
        const targets = resolveTargets(config).filter(node => node.dataset.layoutLoaded !== 'true');
        if (!targets.length) return Promise.resolve(false);

        return fetchPartial(config.url)
            .then(html => {
                let injected = false;

                targets.forEach(target => {
                    if (target.dataset.layoutLoaded === 'true') return;

                    target.innerHTML = html;
                    target.dataset.layoutLoaded = 'true';
                    injected = true;

                    if (typeof config.afterInject === 'function') {
                        config.afterInject(target);
                    }
                });

                if (injected && config.eventName) {
                    document.dispatchEvent(new CustomEvent(config.eventName));
                }

                return injected;
            })
            .catch(error => {
                console.error((config.label || 'Layout') + ' load error:', error);
                return false;
            });
    }

    function loadHeader() {
        return injectPartial({
            selector: '#header-placeholder',
            dataInclude: 'header',
            url: '/html/header.html',
            label: 'Header',
            eventName: 'headerLoaded',
            afterInject: () => {
                setActiveHeaderLink();
                setupDropdowns();
                setupMobileMenu();
                if (typeof renderGoogleButton === 'function') {
                    renderGoogleButton();
                }
            }
        });
    }

    function isSharedFooterDisabled() {
        const body = document.body;
        if (!body) return false;

        return body.dataset.disableSharedFooter === 'true'
            || body.classList.contains('no-shared-footer');
    }

    function removeSharedFooter() {
        resolveTargets({
            selector: '#footer-placeholder',
            dataInclude: 'footer'
        }).forEach(target => target.remove());

        document.querySelectorAll('.site-footer').forEach(footer => {
            const placeholder = footer.closest('#footer-placeholder, [data-include="footer"]');
            footer.remove();
            if (placeholder && !placeholder.hasChildNodes()) {
                placeholder.remove();
            }
        });

        document.querySelectorAll('body > footer').forEach(footer => {
            footer.remove();
        });
    }

    function loadFooter() {
        if (isSharedFooterDisabled()) {
            removeSharedFooter();
            return Promise.resolve(false);
        }

        const placeholderTargets = resolveTargets({
            selector: '#footer-placeholder',
            dataInclude: 'footer'
        }).filter(node => node.dataset.layoutLoaded !== 'true');

        if (placeholderTargets.length) {
            return injectPartial({
                selector: '#footer-placeholder',
                dataInclude: 'footer',
                url: '/html/footer.html',
                label: 'Footer',
                eventName: 'footerLoaded'
            });
        }

        const legacyFooter = document.querySelector('body > footer:not(.site-footer)');
        if (!legacyFooter || legacyFooter.querySelector('#last-updated, #leaderboard-updated')) {
            return Promise.resolve(false);
        }

        return fetchPartial('/html/footer.html')
            .then(html => {
                legacyFooter.outerHTML = html;
                document.dispatchEvent(new CustomEvent('footerLoaded'));
                return true;
            })
            .catch(error => {
                console.error('Footer load error:', error);
                return false;
            });
    }

    function loadAdminSidebar() {
        const placeholderTargets = resolveTargets({
            selector: '#admin-sidebar-placeholder',
            dataInclude: 'admin-sidebar'
        }).filter(node => node.dataset.layoutLoaded !== 'true');

        if (placeholderTargets.length) {
            return injectPartial({
                selector: '#admin-sidebar-placeholder',
                dataInclude: 'admin-sidebar',
                url: '/html/admin-sidebar.html',
                label: 'Admin sidebar',
                eventName: 'adminSidebarLoaded',
                afterInject: () => {
                    setActiveAdminLink();
                    bindAdminHashTracking();
                }
            });
        }

        const legacySidebar = document.querySelector('body[data-page^="admin"] .admin-sidebar, body[data-page="admin-dashboard"] aside');
        if (!legacySidebar || legacySidebar.dataset.layoutLoaded === 'true') {
            return Promise.resolve(false);
        }

        return fetchPartial('/html/admin-sidebar.html')
            .then(html => {
                legacySidebar.outerHTML = html;
                const injectedSidebar = document.querySelector('.admin-sidebar');
                if (injectedSidebar) {
                    injectedSidebar.dataset.layoutLoaded = 'true';
                }
                setActiveAdminLink();
                bindAdminHashTracking();
                document.dispatchEvent(new CustomEvent('adminSidebarLoaded'));
                return true;
            })
            .catch(error => {
                console.error('Admin sidebar load error:', error);
                return false;
            });
    }

    function loadLayouts() {
        return Promise.allSettled([
            loadHeader(),
            loadFooter(),
            loadAdminSidebar()
        ]);
    }

    window.__atgSharedLayoutLoader = {
        loadLayouts: loadLayouts,
        setActiveAdminLink: setActiveAdminLink,
        setActiveHeaderLink: setActiveHeaderLink
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', loadLayouts, { once: true });
    } else {
        loadLayouts();
    }
})();
