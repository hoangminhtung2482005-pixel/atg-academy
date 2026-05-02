/**
 * header-loader.js - Tải và inject header dùng chung cho toàn bộ dự án.
 * Sử dụng fetch() để lấy nội dung header.html rồi chèn vào #header-placeholder.
 * Sau khi inject xong, tự động highlight link active và kích hoạt Google Sign-In.
 */
(function () {
    // Xác định trang hiện tại từ URL
    function getCurrentPage() {
        const path = window.location.pathname;
        const filename = path.substring(path.lastIndexOf('/') + 1).replace('.html', '');
        if (filename === 'tactics-guides') return 'giao-an';
        if (filename === 'esports-leaderboard') return 'esports';
        // Nếu đang ở root hoặc rỗng, mặc định là index
        return filename || 'index';
    }

    // Highlight link active dựa trên data-page attribute
    function setActiveLink() {
        const currentPage = getCurrentPage();
        const navLinks = document.querySelectorAll('nav a[data-page]');
        navLinks.forEach(link => {
            if (link.getAttribute('data-page') === currentPage) {
                link.classList.add('active');
            } else {
                link.classList.remove('active');
            }
        });
    }

    // Tải header và inject vào placeholder
    function loadHeader() {
        const placeholder = document.getElementById('header-placeholder');
        if (!placeholder) return;

        fetch('/html/header.html?v=20260424-account-profile', { cache: 'no-store' })
            .then(response => {
                if (!response.ok) throw new Error('Không thể tải header.html');
                return response.text();
            })
            .then(html => {
                placeholder.innerHTML = html;
                // 1. Set active link
                setActiveLink();
                // 2. Kích hoạt lại Google Sign-In
                if (typeof renderGoogleButton === 'function') {
                    renderGoogleButton();
                }
                // 3. Dispatch custom event để các trang khác biết header đã load xong
                document.dispatchEvent(new CustomEvent('headerLoaded'));
            })
            .catch(err => {
                console.error('Header load error:', err);
            });
    }

    // Chạy khi DOM sẵn sàng
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', loadHeader);
    } else {
        loadHeader();
    }
})();
