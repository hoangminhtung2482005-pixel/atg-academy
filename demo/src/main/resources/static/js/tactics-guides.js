(function() {
    var guideFilterState = {
        guides: [],
        heroOptionsInitialized: false,
        debounceTimer: null
    };

    function escapeGuideHtml(value) {
        return String(value || '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function normalizeGuideDate(value) {
        if (!value) return '';
        var date = new Date(value);
        if (Number.isNaN(date.getTime())) return '';
        return date.toLocaleDateString('vi-VN');
    }

    function getGuideAuthorName(guide) {
        return guide?.author?.name || guide?.authorName || guide?.contentData?.authorName || 'ATG Member';
    }

    function getGuideHeroName(guide) {
        return guide?.hero?.name || guide?.heroName || guide?.contentData?.heroName || 'Chưa chọn tướng';
    }

    function getGuideLane(guide) {
        return guide?.lane || guide?.contentData?.lane || 'Tổng quan';
    }

    function getGuideCategory(guide) {
        return guide?.category || guide?.contentData?.category || 'Chiến thuật';
    }

    function getGuideReadingTime(guide) {
        return guide?.readingTime || guide?.readingTimeMinutes || 5;
    }

    function getGuideHeroImage(heroName, guide) {
        if (guide?.coverImageUrl) return guide.coverImageUrl;
        if (guide?.hero?.avatarUrl) return guide.hero.avatarUrl;
        if (typeof getHeroImgUrl === 'function' && heroName) return getHeroImgUrl(heroName);
        return '/images/backgrounds/bg-map.jpg';
    }

    function showGuideToast(message) {
        var toast = document.getElementById('guide-toast');
        if (!toast) return;
        toast.textContent = message;
        toast.classList.add('is-visible');
        window.clearTimeout(showGuideToast.timer);
        showGuideToast.timer = window.setTimeout(function() {
            toast.classList.remove('is-visible');
        }, 3200);
    }

    function setGuidesState(message, isError) {
        var grid = document.getElementById('guides-grid');
        if (!grid) return;
        grid.innerHTML = '<div class="guide-state ' + (isError ? 'text-red-500' : '') + '">' + escapeGuideHtml(message) + '</div>';
    }

    function renderGuideCards(guides) {
        var grid = document.getElementById('guides-grid');
        var countPill = document.getElementById('guide-count-pill');
        if (!grid) return;

        if (countPill) {
            countPill.textContent = guides.length + ' giáo án';
        }

        if (!guides.length) {
            grid.innerHTML = '<div class="guide-state">Không tìm thấy giáo án phù hợp.</div>';
            return;
        }

        grid.innerHTML = guides.map(function(guide) {
            var heroName = getGuideHeroName(guide);
            var coverImage = getGuideHeroImage(heroName, guide);
            var dateLabel = normalizeGuideDate(guide.publishedAt || guide.createdAt);
            var viewCount = Number(guide.viewCount || 0).toLocaleString('vi-VN');
            var readingTime = getGuideReadingTime(guide);
            var lane = getGuideLane(guide);
            var category = getGuideCategory(guide);
            var excerpt = guide.excerpt || guide.contentData?.excerpt || 'Giáo án cộng đồng ATG với các ghi chú chiến thuật và cách vận hành trong trận.';

            return '<article class="guide-card" tabindex="0" role="button" data-guide-id="' + escapeGuideHtml(guide.id) + '">' +
                '<div class="guide-card-cover">' +
                    '<img src="' + escapeGuideHtml(coverImage) + '" alt="' + escapeGuideHtml(guide.title) + '" onerror="this.src=\'/images/backgrounds/bg-map.jpg\'">' +
                '</div>' +
                '<div class="guide-card-body">' +
                    '<div class="guide-card-tags">' +
                        '<span class="guide-tag primary">' + escapeGuideHtml(heroName) + '</span>' +
                        '<span class="guide-tag">' + escapeGuideHtml(lane) + '</span>' +
                        '<span class="guide-tag">' + escapeGuideHtml(category) + '</span>' +
                    '</div>' +
                    '<h2 class="guide-card-title">' + escapeGuideHtml(guide.title) + '</h2>' +
                    '<p class="guide-card-excerpt">' + escapeGuideHtml(excerpt) + '</p>' +
                    '<div class="guide-card-meta">' +
                        '<span>' + escapeGuideHtml(getGuideAuthorName(guide)) + '</span>' +
                        '<span>' + escapeGuideHtml(dateLabel || 'Chưa rõ ngày') + '</span>' +
                        '<span>' + viewCount + ' lượt xem</span>' +
                        '<span>' + escapeGuideHtml(readingTime) + ' phút đọc</span>' +
                    '</div>' +
                '</div>' +
            '</article>';
        }).join('');

        grid.querySelectorAll('.guide-card').forEach(function(card) {
            function openGuide() {
                var guideId = card.getAttribute('data-guide-id');
                if (guideId) {
                    window.location.href = '/html/guide-detail.html?id=' + encodeURIComponent(guideId);
                }
            }
            card.addEventListener('click', openGuide);
            card.addEventListener('keydown', function(event) {
                if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    openGuide();
                }
            });
        });
    }

    function syncHeroFilterOptions(guides) {
        var select = document.getElementById('guide-hero-filter');
        if (!select || guideFilterState.heroOptionsInitialized) return;

        var selectedValue = select.value;
        var seen = new Set();
        var options = [];

        guides.forEach(function(guide) {
            var hero = guide.hero || {};
            if (!hero.id || !hero.name || seen.has(String(hero.id))) return;
            seen.add(String(hero.id));
            options.push({ id: hero.id, name: hero.name });
        });

        options.sort(function(a, b) {
            return a.name.localeCompare(b.name, 'vi');
        });

        select.innerHTML = '<option value="">Tất cả tướng</option>' + options.map(function(option) {
            return '<option value="' + escapeGuideHtml(option.id) + '">' + escapeGuideHtml(option.name) + '</option>';
        }).join('');

        if (selectedValue && Array.from(select.options).some(function(option) { return option.value === selectedValue; })) {
            select.value = selectedValue;
        }
        guideFilterState.heroOptionsInitialized = true;
    }

    function buildGuideQuery() {
        var params = new URLSearchParams();
        var search = document.getElementById('guide-search')?.value.trim() || '';
        var heroId = document.getElementById('guide-hero-filter')?.value || '';
        var lane = document.getElementById('guide-lane-filter')?.value || '';
        var category = document.getElementById('guide-category-filter')?.value || '';
        var sort = document.getElementById('guide-sort-filter')?.value || 'newest';

        params.set('status', 'PUBLISHED');
        if (search) params.set('search', search);
        if (heroId) params.set('heroId', heroId);
        if (lane) params.set('lane', lane);
        if (category) params.set('category', category);
        params.set('sort', sort);

        return params.toString();
    }

    async function loadGuides() {
        setGuidesState('Đang tải giáo án đã xuất bản...', false);
        try {
            var response = await fetch('/api/guides?' + buildGuideQuery(), { headers: { Accept: 'application/json' } });
            if (!response.ok) throw new Error('HTTP ' + response.status);

            var guides = await response.json();
            guideFilterState.guides = Array.isArray(guides) ? guides : [];
            syncHeroFilterOptions(guideFilterState.guides);
            renderGuideCards(guideFilterState.guides);
        } catch (error) {
            console.error('Guide list load error:', error);
            setGuidesState('Không thể tải danh sách giáo án. Vui lòng thử lại sau.', true);
        }
    }

    function applyGuideFilters() {
        window.clearTimeout(guideFilterState.debounceTimer);
        guideFilterState.debounceTimer = window.setTimeout(loadGuides, 300);
    }

    function handleCreateGuideClick() {
        window.location.href = '/html/create-guide.html';
    }

    function bindGuideFilters() {
        ['guide-search', 'guide-hero-filter', 'guide-category-filter', 'guide-lane-filter', 'guide-sort-filter'].forEach(function(id) {
            var element = document.getElementById(id);
            if (!element) return;
            element.addEventListener(id === 'guide-search' ? 'input' : 'change', applyGuideFilters);
        });
    }

    window.loadGuides = loadGuides;
    window.renderGuideCards = renderGuideCards;
    window.applyGuideFilters = applyGuideFilters;
    window.handleCreateGuideClick = handleCreateGuideClick;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function() {
            bindGuideFilters();
            loadGuides();
        });
    } else {
        bindGuideFilters();
        loadGuides();
    }
})();
