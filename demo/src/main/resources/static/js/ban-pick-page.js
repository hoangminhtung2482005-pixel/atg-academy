(function () {
    const SHELL_URL = '/html/ban-pick-shell.html?v=20260518-rank-mode';
    const APP_SCRIPT_URL = '/js/ban-pick.js?v=20260518-rank-mode';

    async function loadBanPickShell() {
        const root = document.getElementById('ban-pick-page-root');
        if (!root) return;

        try {
            const response = await fetch(SHELL_URL, { cache: 'no-store' });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            root.innerHTML = await response.text();

            const script = document.createElement('script');
            script.src = APP_SCRIPT_URL;
            script.defer = true;
            document.body.appendChild(script);
        } catch (error) {
            root.innerHTML = `
                <main id="ban-pick-section">
                    <section class="mode-selector-panel">
                        <div class="mode-selector-header">
                            <span class="phase-progress">Ban/Pick Lab</span>
                            <h1>Không thể tải giao diện ban/pick</h1>
                        </div>
                        <div class="draft-warning error">Chi tiết: ${String(error.message || error)}</div>
                    </section>
                </main>
            `;
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', loadBanPickShell, { once: true });
    } else {
        loadBanPickShell();
    }
})();
