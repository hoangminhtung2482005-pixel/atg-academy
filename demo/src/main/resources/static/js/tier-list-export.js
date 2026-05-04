const TIER_EXPORT_FALLBACK_IMAGE = '/images/ui/default.png';
const TIER_EXPORT_LOGO_IMAGE = '/images/ui/logo.png';

function getTierVisualKey(label) {
    const normalized = String(label || '').trim().toUpperCase();
    if (!normalized) return '';
    if (normalized === 'S') return 's';
    if (normalized === 'A') return 'a';
    if (normalized === 'B') return 'b';
    if (normalized === 'C') return 'c';
    if (normalized === 'D') return 'd';
    if (normalized.startsWith('SIT')) return 'situational';
    return '';
}

function slugifyTierExport(value) {
    return String(value || '')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '')
        || 'tier-list';
}

function formatTierExportDate(value) {
    if (!value) return 'Chưa rõ ngày tạo';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return 'Chưa rõ ngày tạo';
    return new Intl.DateTimeFormat('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
    }).format(date);
}

function formatTierExportRating(value) {
    const number = Number(value);
    if (!Number.isFinite(number) || number <= 0) return '0';
    return Number.isInteger(number) ? String(number) : number.toFixed(1).replace(/\.0$/, '');
}

function escapeTierExportHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function isSameDomainTierExportUrl(url) {
    if (!url) return false;
    if (url.startsWith('/')) return true;
    try {
        return new URL(url, window.location.origin).origin === window.location.origin;
    } catch (error) {
        return false;
    }
}

function getTierExportHeroName(hero) {
    if (typeof getHeroNameFromValue === 'function') {
        return getHeroNameFromValue(hero);
    }
    if (hero && typeof hero === 'object') {
        return hero.name || hero.heroName || `Hero #${hero.heroId || hero.id || ''}`.trim();
    }
    return String(hero || '').trim();
}

function getTierExportHeroImage(hero) {
    if (typeof resolveHeroImageUrl === 'function') {
        return resolveHeroImageUrl(hero, { absolute: true });
    }

    return TIER_EXPORT_FALLBACK_IMAGE;
}

function getTierExportFallbackImage() {
    if (typeof normalizeHeroImageUrl === 'function') {
        return normalizeHeroImageUrl(TIER_EXPORT_FALLBACK_IMAGE, { absolute: true });
    }
    return new URL(TIER_EXPORT_FALLBACK_IMAGE, window.location.origin).href;
}

function getTierExportLogoImage() {
    if (typeof normalizeHeroImageUrl === 'function') {
        return normalizeHeroImageUrl(TIER_EXPORT_LOGO_IMAGE, { absolute: true });
    }
    return new URL(TIER_EXPORT_LOGO_IMAGE, window.location.origin).href;
}

function normalizeTierExportRows(contentData) {
    if (!contentData) return { columns: [], rows: [] };
    let data = contentData;
    if (typeof data === 'string') {
        try {
            data = JSON.parse(data);
        } catch (error) {
            return { columns: [], rows: [] };
        }
    }
    if (typeof normalizeTierRoleColumnOrder === 'function') {
        data = normalizeTierRoleColumnOrder(data);
    }
    if (Array.isArray(data.rows)) {
        return {
            columns: Array.isArray(data.columns) ? data.columns : [],
            rows: data.rows
        };
    }
    if (Array.isArray(data.tiers)) {
        return {
            columns: [{ label: 'Tướng' }],
            rows: data.tiers.map(tier => ({
                label: tier.label,
                color: tier.color,
                cells: [tier.heroes || []]
            }))
        };
    }
    return { columns: [], rows: [] };
}

function getTierExportAdminRating(payload) {
    return payload?.adminRatingDetail?.ratingValue
        ?? payload?.adminRating
        ?? null;
}

function getTierExportAverageRating(payload) {
    return payload?.averageUserRating
        ?? payload?.communityRating
        ?? payload?.average
        ?? 0;
}

function getTierExportRatingCount(payload) {
    return payload?.userRatingCount
        ?? payload?.totalRatings
        ?? payload?.count
        ?? 0;
}

function buildTierExportBoardHtml(contentData) {
    const normalized = normalizeTierExportRows(contentData);
    const rows = normalized.rows;
    let columns = normalized.columns;
    if (!columns.length && rows.length) {
        const maxCells = Math.max(...rows.map(row => Array.isArray(row.cells) ? row.cells.length : 0), 1);
        columns = Array.from({ length: maxCells }, (_, index) => ({ label: `Cột ${index + 1}` }));
    }
    if (!rows.length) {
        return '<div class="tier-export-empty">Tier List chưa có dữ liệu.</div>';
    }

    let html = `<div class="tier-export-board tier-board" style="grid-template-columns:100px repeat(${Math.max(columns.length, 1)}, minmax(160px, 1fr));">`;
    html += '<div class="tier-export-cell tier-export-header-cell empty"></div>';
    columns.forEach(column => {
        html += `<div class="tier-export-cell tier-export-header-cell">${escapeTierExportHtml(column.label || '')}</div>`;
    });

    rows.forEach(row => {
        const tierKey = getTierVisualKey(row.label);
        const tierClass = tierKey ? ` tier-${tierKey}` : '';
        const rowClass = tierKey ? ` tier-export-row tier-row-${tierKey}${tierClass}` : '';
        const labelClass = tierKey ? ` tier-export-label-cell tier-label-${tierKey}${tierClass}` : '';
        const labelStyle = tierKey ? '' : ` style="background:${escapeTierExportHtml(row.color || '#95a5a6')}"`;
        html += `<div class="tier-export-cell tier-export-label${rowClass}${labelClass}"${labelStyle}>${escapeTierExportHtml(row.label || '')}</div>`;
        for (let index = 0; index < columns.length; index += 1) {
            const heroes = Array.isArray(row.cells?.[index]) ? row.cells[index] : [];
            const heroHtml = heroes.map(hero => {
                const name = getTierExportHeroName(hero);
                const image = getTierExportHeroImage(hero);
                return `<img class="hero-avatar-chip tier-export-hero" src="${escapeTierExportHtml(image)}" alt="${escapeTierExportHtml(name)}" title="${escapeTierExportHtml(name)}" data-hero-name="${escapeTierExportHtml(name)}" data-fallback-src="${escapeTierExportHtml(getTierExportFallbackImage())}" loading="eager" crossorigin="anonymous" referrerpolicy="no-referrer">`;
            }).join('');
            html += `<div class="tier-export-cell tier-export-heroes tier-export-content${rowClass}">${heroHtml || '<span class="tier-export-placeholder">-</span>'}</div>`;
        }
    });
    html += '</div>';
    return html;
}

function ensureTierListExportArea() {
    let area = document.getElementById('tierListExportArea');
    if (!area) {
        area = document.createElement('div');
        area.id = 'tierListExportArea';
        area.className = 'tier-export-area';
        area.setAttribute('aria-hidden', 'true');
        document.body.appendChild(area);
    }
    return area;
}

function buildTierExportPayload(payload) {
    const title = payload?.title || 'Tier List';
    const creatorName = payload?.author?.name || payload?.creatorName || 'ATG Academy';
    const createdAt = payload?.createdAt || payload?.updatedAt || new Date().toISOString();
    const averageRating = getTierExportAverageRating(payload);
    const ratingCount = getTierExportRatingCount(payload);
    const adminRating = getTierExportAdminRating(payload);
    return {
        id: payload?.id,
        slug: payload?.slug,
        title,
        creatorName,
        createdAt,
        averageRating,
        ratingCount,
        adminRating,
        contentData: payload?.contentData
    };
}

function renderTierListExportArea(payload) {
    const exportPayload = buildTierExportPayload(payload);
    const area = ensureTierListExportArea();
    const adminRatingHtml = exportPayload.adminRating
        ? `<span>Admin: ${formatTierExportRating(exportPayload.adminRating)}/5</span>`
        : '';

    area.innerHTML = `
        <div class="tier-export-card">
            <header class="tier-export-header">
                <div class="tier-export-brand">
                    <img src="${getTierExportLogoImage()}" alt="ATG Academy" data-fallback-src="${escapeTierExportHtml(getTierExportFallbackImage())}" loading="eager" crossorigin="anonymous" referrerpolicy="no-referrer">
                    <strong>ATG Academy</strong>
                </div>
                <div class="tier-export-watermark">ATG Academy</div>
            </header>
            <section class="tier-export-meta">
                <h1>${escapeTierExportHtml(exportPayload.title)}</h1>
                <div class="tier-export-info">
                    <span>Người tạo: ${escapeTierExportHtml(exportPayload.creatorName)}</span>
                    <span>Ngày tạo: ${escapeTierExportHtml(formatTierExportDate(exportPayload.createdAt))}</span>
                    <span>Cộng đồng: ${formatTierExportRating(exportPayload.averageRating)}/5 (${Number(exportPayload.ratingCount || 0)} đánh giá)</span>
                    ${adminRatingHtml}
                </div>
            </section>
            ${buildTierExportBoardHtml(exportPayload.contentData)}
            <footer class="tier-export-footer">ATG Academy</footer>
        </div>
    `;
    return area;
}

function waitForTierExportImages(root) {
    const images = Array.from(root.querySelectorAll('img'));
    const fallbackImage = getTierExportFallbackImage();
    return Promise.all(images.map(img => new Promise(resolve => {
        const finish = () => resolve();
        const cleanup = (loadHandler, errorHandler) => {
            img.removeEventListener('load', loadHandler);
            img.removeEventListener('error', errorHandler);
        };
        const waitForFallback = () => {
            const onFallbackLoad = () => cleanup(onFallbackLoad, onFallbackError) || finish();
            const onFallbackError = () => cleanup(onFallbackLoad, onFallbackError) || finish();
            img.addEventListener('load', onFallbackLoad, { once: true });
            img.addEventListener('error', onFallbackError, { once: true });
        };
        const handleError = () => {
            const heroName = img.dataset.heroName || img.alt || '';
            const failedUrl = img.currentSrc || img.src || '';
            if (heroName) {
                if (typeof warnHeroImageFailure === 'function') {
                    warnHeroImageFailure(heroName, failedUrl);
                } else {
                    console.warn('Hero image failed:', heroName, failedUrl);
                }
            }

            const targetFallback = img.dataset.fallbackSrc || fallbackImage;
            const normalizedCurrent = typeof normalizeHeroImageUrl === 'function'
                ? normalizeHeroImageUrl(failedUrl, { absolute: true })
                : failedUrl;
            const normalizedFallback = typeof normalizeHeroImageUrl === 'function'
                ? normalizeHeroImageUrl(targetFallback, { absolute: true })
                : targetFallback;

            if (normalizedCurrent === normalizedFallback) {
                finish();
                return;
            }

            waitForFallback();
            img.src = targetFallback;
        };

        if (img.complete) {
            if (img.naturalWidth > 0) {
                finish();
                return;
            }
            handleError();
            return;
        }

        const onLoad = () => cleanup(onLoad, onError) || finish();
        const onError = () => {
            cleanup(onLoad, onError);
            handleError();
        };
        img.addEventListener('load', onLoad, { once: true });
        img.addEventListener('error', onError, { once: true });
    })));
}

function downloadTierExportCanvas(canvas, filename) {
    const link = document.createElement('a');
    link.download = filename;
    link.href = canvas.toDataURL('image/png');
    document.body.appendChild(link);
    link.click();
    link.remove();
}

function setTierExportButtonState(button, text, disabled = false) {
    if (!button) return;
    button.textContent = text;
    button.disabled = disabled;
}

async function exportTierListImage(payload, button) {
    const originalText = button?.textContent || 'Tải ảnh';
    try {
        if (typeof html2canvas !== 'function') {
            throw new Error('html2canvas is not loaded');
        }
        setTierExportButtonState(button, 'Đang tạo ảnh...', true);
        const area = renderTierListExportArea(payload);
        await waitForTierExportImages(area);
        const canvas = await html2canvas(area.firstElementChild, {
            backgroundColor: '#ffffff',
            scale: 2,
            useCORS: true,
            allowTaint: false,
            imageTimeout: 15000,
            logging: false
        });
        const slugOrId = payload?.slug || payload?.id || slugifyTierExport(payload?.title);
        downloadTierExportCanvas(canvas, `atg-tier-list-${slugifyTierExport(slugOrId)}.png`);
        setTierExportButtonState(button, originalText, false);
    } catch (error) {
        console.error('Cannot export tier list image:', error);
        setTierExportButtonState(button, 'Không thể tải ảnh. Vui lòng thử lại.', false);
        setTimeout(() => setTierExportButtonState(button, originalText, false), 2400);
    }
}
