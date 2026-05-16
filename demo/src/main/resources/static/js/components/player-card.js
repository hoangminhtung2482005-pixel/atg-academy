(function () {
    const PLAYER_CARD_THEMES = {
        S: { code: 'S', label: 'Rank S', dataRank: 'S' },
        A: { code: 'A', label: 'Rank A', dataRank: 'A' },
        B: { code: 'B', label: 'Rank B', dataRank: 'B' },
        C: { code: 'C', label: 'Rank C', dataRank: 'C' },
        D: { code: 'D', label: 'Rank D', dataRank: 'D' },
        UNRANKED: { code: 'UNRANKED', label: 'Unranked', dataRank: 'unranked' }
    };

    function escapeHtml(value) {
        return String(value ?? '').replace(/[&<>"']/g, function (char) {
            return ({
                '&': '&amp;',
                '<': '&lt;',
                '>': '&gt;',
                '"': '&quot;',
                "'": '&#039;'
            })[char];
        });
    }

    function resolveContainer(container) {
        if (typeof container === 'string') {
            return document.querySelector(container);
        }
        return container instanceof Element ? container : null;
    }

    function normalizeText(value) {
        return String(value ?? '').trim().replace(/\s+/g, ' ');
    }

    function normalizeRankCode(value) {
        const normalized = normalizeText(value).toUpperCase();
        return PLAYER_CARD_THEMES[normalized] ? normalized : 'UNRANKED';
    }

    function getRankTheme(rankCode) {
        return PLAYER_CARD_THEMES[normalizeRankCode(rankCode)] || PLAYER_CARD_THEMES.UNRANKED;
    }

    function normalizeVariant(value) {
        const normalized = normalizeText(value).toLowerCase();
        if (normalized === 'solo-ban-pick' || normalized === 'account-preview') {
            return normalized;
        }
        return 'default';
    }

    function formatDisplayName(value) {
        const normalized = normalizeText(value) || 'Người chơi';
        try {
            return normalized.toLocaleUpperCase('vi-VN');
        } catch (error) {
            return normalized.toUpperCase();
        }
    }

    function getDisplayInitial(value) {
        return formatDisplayName(value).charAt(0) || '?';
    }

    function formatElo(value) {
        const numeric = Number(value);
        const normalized = Number.isFinite(numeric) ? Math.round(numeric) : 0;
        return normalized.toLocaleString('vi-VN');
    }

    function normalizePlayerCardData(input) {
        const source = input && typeof input === 'object' ? input : {};
        const rankTheme = getRankTheme(source.rankCode);
        const badgeName = normalizeText(source.badgeName) || 'ATG Player';
        const title = normalizeText(source.title) || '\u2726 T\u00e2n Binh Ban/Pick \u2726';
        return {
            avatarUrl: normalizeText(source.avatarUrl),
            displayName: normalizeText(source.displayName) || 'Người chơi',
            elo: Number.isFinite(Number(source.elo)) ? Math.round(Number(source.elo)) : 0,
            rankCode: rankTheme.code,
            rankLabel: normalizeText(source.rankLabel) || rankTheme.label,
            badgeCode: normalizeText(source.badgeCode) || 'default',
            badgeName: badgeName,
            badgeIconUrl: normalizeText(source.badgeIconUrl),
            title: title,
            variant: normalizeVariant(source.variant)
        };
    }

    function renderBadgeIcon(data) {
        if (data.badgeIconUrl) {
            return `<img class="atg-player-card__badge-icon" src="${escapeHtml(data.badgeIconUrl)}" alt="${escapeHtml(data.badgeName)}" loading="lazy" referrerpolicy="no-referrer">`;
        }

        return `
            <span class="atg-player-card__badge-icon atg-player-card__badge-icon--fallback" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none">
                    <path d="M12 3.5l2.63 5.33 5.88.86-4.25 4.14 1 5.84L12 17.1l-5.26 2.57 1-5.84-4.25-4.14 5.88-.86L12 3.5z" fill="currentColor"></path>
                </svg>
            </span>
        `;
    }

    function renderAvatar(data) {
        if (data.avatarUrl) {
            return `
                <img class="atg-player-card__avatar" src="${escapeHtml(data.avatarUrl)}" alt="${escapeHtml(data.displayName)}" loading="lazy" referrerpolicy="no-referrer" onerror="this.onerror=null;this.style.display='none';this.nextElementSibling.hidden=false;">
                <span class="atg-player-card__avatar-fallback" hidden>${escapeHtml(getDisplayInitial(data.displayName))}</span>
            `;
        }

        return `<span class="atg-player-card__avatar-fallback">${escapeHtml(getDisplayInitial(data.displayName))}</span>`;
    }

    function renderPlayerCard(container, input) {
        const target = resolveContainer(container);
        if (!target) return null;

        const data = normalizePlayerCardData(input);
        const theme = getRankTheme(data.rankCode);
        const rankCodeDisplay = data.rankCode === 'UNRANKED' ? '—' : data.rankCode;
        const safeDisplayName = escapeHtml(formatDisplayName(data.displayName));
        const safeOriginalName = escapeHtml(data.displayName);

        target.innerHTML = `
            <article class="atg-player-card" data-rank="${escapeHtml(theme.dataRank)}" data-variant="${escapeHtml(data.variant)}" aria-label="Player Card của ${safeOriginalName}">
                <div class="atg-player-card__head">
                    <span class="atg-player-card__eyebrow">Player Card</span>
                    <div class="atg-player-card__badge" title="${escapeHtml(data.badgeName)}">
                        ${renderBadgeIcon(data)}
                        <span>${escapeHtml(data.badgeName)}</span>
                    </div>
                </div>
                <div class="atg-player-card__rank-pill" aria-label="${escapeHtml(data.rankLabel)}">
                    <small>Rank</small>
                    <strong>${escapeHtml(rankCodeDisplay)}</strong>
                    <span>${escapeHtml(data.rankLabel)}</span>
                </div>
                <div class="atg-player-card__avatar-frame">
                    ${renderAvatar(data)}
                </div>
                <div class="atg-player-card__identity">
                    <h3 class="atg-player-card__name" title="${safeOriginalName}">${safeDisplayName}</h3>
                    <p class="atg-player-card__title">${escapeHtml(data.title)}</p>
                </div>
                <div class="atg-player-card__elo-box">
                    <span class="atg-player-card__elo-label">ELO</span>
                    <strong>${escapeHtml(formatElo(data.elo))}</strong>
                    <small>${escapeHtml(data.rankLabel)}</small>
                </div>
            </article>
        `;

        return target.firstElementChild;
    }

    window.ATGPlayerCard = {
        renderPlayerCard: renderPlayerCard,
        normalizePlayerCardData: normalizePlayerCardData,
        getRankTheme: getRankTheme
    };
})();
