const ESPORTS_DATA_API = {
    tournaments: '/api/esports/data/tournaments',
    dashboard: '/api/esports/data/dashboard',
    topBanned: '/api/esports/data/top-banned-heroes',
    topBlueBanned: '/api/esports/data/top-blue-banned-heroes',
    topRedBanned: '/api/esports/data/top-red-banned-heroes'
};

const ESPORTS_DEFAULT_TABLE_ROWS = 5;
const HERO_AVATAR_FALLBACK = '/images/heroes/default.jpg';

function esportsCreateTableExpansionState() {
    return {
        topBanned: false,
        topBlueBanned: false,
        topRedBanned: false,
        topPicked: false
    };
}

const esportsDataState = {
    tournamentScope: '',
    tournaments: [],
    dashboard: null,
    topBanned: [],
    topBlueBanned: [],
    topRedBanned: [],
    tableExpansion: esportsCreateTableExpansionState()
};

function esportsById(id) {
    return document.getElementById(id);
}

function esportsEscapeHtml(value) {
    return String(value == null ? '' : value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function esportsBuildUrl(baseUrl, params) {
    const url = new URL(baseUrl, window.location.origin);
    Object.entries(params || {}).forEach(([key, value]) => {
        if (value != null && value !== '') {
            url.searchParams.set(key, value);
        }
    });
    return url.toString();
}

function esportsParseApiError(rawText, response) {
    if (!rawText) {
        return response.status + ' ' + response.statusText;
    }
    try {
        const payload = JSON.parse(rawText);
        return payload.error || payload.message || payload.detail || rawText;
    } catch (error) {
        return rawText;
    }
}

async function esportsFetchJson(url) {
    const response = await fetch(url);
    const rawText = await response.text();
    if (!response.ok) {
        throw new Error(esportsParseApiError(rawText, response));
    }
    return rawText ? JSON.parse(rawText) : null;
}

function esportsFormatCount(value) {
    const numeric = Number(value || 0);
    return Number.isFinite(numeric) ? numeric.toLocaleString('vi-VN') : '0';
}

function esportsFormatRate(value) {
    const numeric = Number(value || 0);
    if (!Number.isFinite(numeric)) {
        return '0%';
    }
    return numeric.toLocaleString('vi-VN', {
        minimumFractionDigits: numeric % 1 === 0 ? 0 : 1,
        maximumFractionDigits: 1
    }) + '%';
}

function esportsSafeNumber(value, fallback = 0) {
    const numeric = Number(value);
    return Number.isFinite(numeric) ? numeric : fallback;
}

function esportsFormatRateOrDash(value, denominator) {
    const total = esportsSafeNumber(denominator);
    if (total <= 0) {
        return '—';
    }
    return esportsFormatRate(value);
}

function esportsResolveRate(value, numerator, denominator) {
    if (value != null && value !== '') {
        return esportsSafeNumber(value);
    }
    const total = esportsSafeNumber(denominator);
    if (total <= 0) {
        return 0;
    }
    return (esportsSafeNumber(numerator) / total) * 100;
}

function esportsResolveHeroIconUrl(item) {
    if (item && (item.heroIconUrl || item.heroAvatarUrl)) {
        return item.heroIconUrl || item.heroAvatarUrl;
    }
    return HERO_AVATAR_FALLBACK;
}

function esportsRenderStatus(message, type) {
    const node = esportsById('page-status');
    if (!node) return;
    node.textContent = message || '';
    node.className = 'ed-status' + (message ? '' : ' hidden') + (type ? ' ' + type : '');
}

function esportsBuildHeroCell(heroName, avatarUrl) {
    const name = heroName || 'Unknown hero';
    const safeName = esportsEscapeHtml(name);
    const safeAvatar = esportsEscapeHtml(avatarUrl || HERO_AVATAR_FALLBACK);
    return `
        <div class="ed-hero-cell">
            <img src="${safeAvatar}" alt="${safeName}" onerror="this.onerror=null;this.src='${HERO_AVATAR_FALLBACK}';">
            <div>
                <strong>${safeName}</strong>
            </div>
        </div>
    `;
}

function esportsCurrentScopeLabel() {
    const scope = (esportsDataState.tournaments || []).find(item => esportsScopeValue(item) === esportsDataState.tournamentScope);
    return scope ? (scope.tournamentName || 'Tất cả giải đấu') : 'Tất cả giải đấu';
}

function esportsScopeValue(option) {
    if (!option) return '';
    return option.tournamentId != null ? `official:${option.tournamentId}` : `legacy:${option.tournamentTier || ''}`;
}

function esportsResolveScopeQuery() {
    const scopeValue = String(esportsDataState.tournamentScope || '').trim();
    if (!scopeValue) {
        return { tournamentId: null, tournamentName: '' };
    }
    if (scopeValue.startsWith('official:')) {
        return { tournamentId: Number(scopeValue.slice('official:'.length)), tournamentName: '' };
    }
    if (scopeValue.startsWith('legacy:')) {
        return { tournamentId: null, tournamentName: scopeValue.slice('legacy:'.length) };
    }
    return { tournamentId: null, tournamentName: scopeValue };
}

function esportsRenderTournamentFilter() {
    const select = esportsById('tournament-filter');
    if (!select) return;

    select.innerHTML = ['<option value="">Tất cả giải đấu</option>']
        .concat((esportsDataState.tournaments || []).map(option => (
            `<option value="${esportsEscapeHtml(esportsScopeValue(option))}"${esportsScopeValue(option) === esportsDataState.tournamentScope ? ' selected' : ''}>${esportsEscapeHtml(option.tournamentName)}${option.legacyScope ? ' [legacy]' : (option.franchiseCode ? ` · ${option.franchiseCode}` : '')}</option>`
        )))
        .join('');
}

function esportsGetHeroStats() {
    return Array.isArray(esportsDataState.dashboard && esportsDataState.dashboard.heroStats)
        ? esportsDataState.dashboard.heroStats
        : [];
}

function esportsGetTopPickedHeroes(limit) {
    const rows = esportsGetHeroStats()
        .slice()
        .sort((left, right) => {
            const pickDelta = Number(right.pickCount || 0) - Number(left.pickCount || 0);
            if (pickDelta !== 0) return pickDelta;
            const winRateDelta = Number(right.pickWinRate || 0) - Number(left.pickWinRate || 0);
            if (winRateDelta !== 0) return winRateDelta;
            return String(left.heroName || '').localeCompare(String(right.heroName || ''), 'vi');
        })
        .filter(item => Number(item.pickCount || 0) > 0);

    if (typeof limit === 'number') {
        return rows.slice(0, limit);
    }
    return rows;
}

function esportsRenderScopeSummary() {
    const scopeLabel = esportsCurrentScopeLabel();
    const card = esportsById('scope-card');
    const label = esportsById('scope-label');

    if (label) {
        label.textContent = scopeLabel;
    }
    if (card) {
        const title = 'Giải đấu đang xem: ' + scopeLabel;
        card.title = title;
        card.setAttribute('aria-label', title);
    }
}

function esportsRenderKpis() {
    const summary = esportsDataState.dashboard && esportsDataState.dashboard.summary
        ? esportsDataState.dashboard.summary
        : {};
    const mostBanned = (esportsDataState.topBanned || [])[0] || null;
    const mostPicked = esportsGetTopPickedHeroes(1)[0] || null;

    esportsById('kpi-total-games').textContent = esportsFormatCount(summary.totalGames);
    esportsById('kpi-total-matches').textContent = esportsFormatCount(summary.totalMatches);
    esportsById('kpi-blue-win-rate').textContent = esportsFormatRate(summary.blueSideWinRate);
    esportsById('kpi-most-banned').textContent = mostBanned ? mostBanned.heroName : 'N/A';
    esportsById('kpi-most-picked').textContent = mostPicked ? mostPicked.heroName : 'N/A';
}

function esportsIsTableExpanded(tableKey) {
    return Boolean(esportsDataState.tableExpansion?.[tableKey]);
}

function esportsSetTableExpanded(tableKey, expanded) {
    if (!esportsDataState.tableExpansion) {
        esportsDataState.tableExpansion = esportsCreateTableExpansionState();
    }
    esportsDataState.tableExpansion[tableKey] = Boolean(expanded);
}

function esportsResetTableExpansion() {
    esportsDataState.tableExpansion = esportsCreateTableExpansionState();
}

function esportsRenderTableToggle(toggleId, tableKey, totalRows) {
    const button = esportsById(toggleId);
    if (!button) return;

    const isExpanded = esportsIsTableExpanded(tableKey);
    const canExpand = totalRows > ESPORTS_DEFAULT_TABLE_ROWS;
    button.hidden = !canExpand;
    button.textContent = isExpanded ? 'Thu g\u1ecdn' : 'Xem t\u1ea5t c\u1ea3';
    button.setAttribute('aria-expanded', canExpand && isExpanded ? 'true' : 'false');
}

function esportsRenderExpandableTable(options) {
    const body = esportsById(options.bodyId);
    if (!body) return;

    const rows = Array.isArray(options.rows) ? options.rows : [];
    esportsRenderTableToggle(options.toggleId, options.tableKey, rows.length);

    if (!rows.length) {
        body.innerHTML = `<tr><td colspan="${options.colspan}" class="ed-empty-row">${options.emptyMessage}</td></tr>`;
        return;
    }

    const visibleRows = esportsIsTableExpanded(options.tableKey)
        ? rows
        : rows.slice(0, ESPORTS_DEFAULT_TABLE_ROWS);

    body.innerHTML = visibleRows.map((item, index) => options.renderRow(item, index)).join('');
}

function esportsRenderTopBanned() {
    const body = esportsById('top-banned-body');
    if (!body) return;

    const totalGames = Number(esportsDataState.dashboard?.summary?.totalGames || 0);
    const rows = Array.isArray(esportsDataState.topBanned) ? esportsDataState.topBanned : [];
    if (!rows.length) {
        body.innerHTML = '<tr><td colspan="4" class="ed-empty-row">Chưa có dữ liệu ban cho bộ lọc hiện tại.</td></tr>';
        return;
    }

    body.innerHTML = rows.map((item, index) => {
        const banRate = totalGames > 0 ? (Number(item.banCount || 0) / totalGames) * 100 : 0;
        return `
            <tr>
                <td>${index + 1}</td>
                <td>${esportsBuildHeroCell(item.heroName, item.heroAvatarUrl)}</td>
                <td>${esportsFormatCount(item.banCount)}</td>
                <td>${esportsFormatRate(banRate)}</td>
            </tr>
        `;
    }).join('');
}

function esportsRenderTopBlueBanned() {
    const body = esportsById('top-blue-banned-body');
    if (!body) return;

    const rows = Array.isArray(esportsDataState.topBlueBanned) ? esportsDataState.topBlueBanned : [];
    const totalBlueBans = rows.reduce((sum, item) => sum + Number(item.banCount || 0), 0);
    if (!rows.length) {
        body.innerHTML = '<tr><td colspan="4" class="ed-empty-row">Chưa có dữ liệu cấm phía xanh cho bộ lọc hiện tại.</td></tr>';
        return;
    }

    body.innerHTML = rows.map((item, index) => {
        const share = totalBlueBans > 0 ? (Number(item.banCount || 0) / totalBlueBans) * 100 : 0;
        return `
            <tr>
                <td>${index + 1}</td>
                <td>${esportsBuildHeroCell(item.heroName, item.heroAvatarUrl)}</td>
                <td>${esportsFormatCount(item.banCount)}</td>
                <td>${esportsFormatRate(share)}</td>
            </tr>
        `;
    }).join('');
}

function esportsRenderTopPicked() {
    const body = esportsById('top-picked-body');
    if (!body) return;

    const rows = esportsGetTopPickedHeroes(10);
    if (!rows.length) {
        body.innerHTML = '<tr><td colspan="4" class="ed-empty-row">Chưa có dữ liệu pick cho bộ lọc hiện tại.</td></tr>';
        return;
    }

    body.innerHTML = rows.map((item, index) => `
        <tr>
            <td>${index + 1}</td>
            <td>${esportsBuildHeroCell(item.heroName, item.heroAvatarUrl)}</td>
            <td>${esportsFormatCount(item.pickCount)}</td>
            <td>${esportsFormatRate(item.pickWinRate)}</td>
        </tr>
    `).join('');
}

function esportsRenderTopBanned() {
    const totalGames = Number(esportsDataState.dashboard?.summary?.totalGames || 0);
    esportsRenderExpandableTable({
        bodyId: 'top-banned-body',
        toggleId: 'top-banned-toggle',
        tableKey: 'topBanned',
        rows: esportsDataState.topBanned,
        emptyMessage: 'Ch\u01b0a c\u00f3 d\u1eef li\u1ec7u ban cho b\u1ed9 l\u1ecdc hi\u1ec7n t\u1ea1i.',
        colspan: 4,
        renderRow: (item, index) => {
            const banRate = totalGames > 0 ? (Number(item.banCount || 0) / totalGames) * 100 : 0;
            return `
                <tr>
                    <td>${index + 1}</td>
                    <td>${esportsBuildHeroCell(item.heroName, item.heroAvatarUrl)}</td>
                    <td>${esportsFormatCount(item.banCount)}</td>
                    <td>${esportsFormatRate(banRate)}</td>
                </tr>
            `;
        }
    });
}

function esportsRenderSideBanTable(options) {
    const rows = Array.isArray(options.rows) ? options.rows : [];
    const totalSideBans = rows.reduce((sum, item) => sum + Number(item.banCount || 0), 0);
    esportsRenderExpandableTable({
        bodyId: options.bodyId,
        toggleId: options.toggleId,
        tableKey: options.tableKey,
        rows,
        emptyMessage: options.emptyMessage,
        colspan: 4,
        renderRow: (item, index) => {
            const share = totalSideBans > 0 ? (Number(item.banCount || 0) / totalSideBans) * 100 : 0;
            return `
                <tr>
                    <td>${index + 1}</td>
                    <td>${esportsBuildHeroCell(item.heroName, item.heroAvatarUrl)}</td>
                    <td>${esportsFormatCount(item.banCount)}</td>
                    <td>${esportsFormatRate(share)}</td>
                </tr>
            `;
        }
    });
}

function esportsRenderTopBlueBanned() {
    esportsRenderSideBanTable({
        bodyId: 'top-blue-banned-body',
        toggleId: 'top-blue-banned-toggle',
        tableKey: 'topBlueBanned',
        rows: esportsDataState.topBlueBanned,
        emptyMessage: 'Ch\u01b0a c\u00f3 d\u1eef li\u1ec7u c\u1ea5m ph\u00eda xanh cho b\u1ed9 l\u1ecdc hi\u1ec7n t\u1ea1i.'
    });
}

function esportsRenderTopRedBanned() {
    esportsRenderSideBanTable({
        bodyId: 'top-red-banned-body',
        toggleId: 'top-red-banned-toggle',
        tableKey: 'topRedBanned',
        rows: esportsDataState.topRedBanned,
        emptyMessage: 'Ch\u01b0a c\u00f3 d\u1eef li\u1ec7u c\u1ea5m ph\u00eda \u0111\u1ecf cho b\u1ed9 l\u1ecdc hi\u1ec7n t\u1ea1i.'
    });
}

function esportsRenderTopPicked() {
    esportsRenderExpandableTable({
        bodyId: 'top-picked-body',
        toggleId: 'top-picked-toggle',
        tableKey: 'topPicked',
        rows: esportsGetTopPickedHeroes(),
        emptyMessage: 'Ch\u01b0a c\u00f3 d\u1eef li\u1ec7u pick cho b\u1ed9 l\u1ecdc hi\u1ec7n t\u1ea1i.',
        colspan: 4,
        renderRow: (item, index) => `
            <tr>
                <td>${index + 1}</td>
                <td>${esportsBuildHeroCell(item.heroName, item.heroAvatarUrl)}</td>
                <td>${esportsFormatCount(item.pickCount)}</td>
                <td>${esportsFormatRate(item.pickWinRate)}</td>
            </tr>
        `
    });
}

function esportsRenderSideAdvantage() {
    const payload = esportsDataState.dashboard && esportsDataState.dashboard.sideAdvantage
        ? esportsDataState.dashboard.sideAdvantage
        : {};
    const blueRate = Number(payload.blueWinRate || 0);
    const redRate = Number(payload.redWinRate || 0);
    const completedGames = Number(payload.completedGames || 0);

    esportsById('blue-win-count').textContent = esportsFormatCount(payload.blueWins);
    esportsById('red-win-count').textContent = esportsFormatCount(payload.redWins);
    esportsById('blue-win-note').textContent = esportsFormatRate(blueRate) + ' win rate';
    esportsById('red-win-note').textContent = esportsFormatRate(redRate) + ' win rate';
    esportsById('blue-win-bar').style.width = Math.max(0, Math.min(100, blueRate)) + '%';
    esportsById('red-win-bar').style.width = Math.max(0, Math.min(100, redRate)) + '%';

    const summary = esportsById('side-summary');
    if (summary) {
        if (completedGames > 0) {
            summary.textContent = '';
            summary.hidden = true;
        } else {
            summary.textContent = 'Chưa có đủ game có winner.';
            summary.hidden = false;
        }
    }
}

function esportsRenderHeroStats() {
    const body = esportsById('hero-stats-body');
    if (!body) return;

    const rows = esportsGetHeroStats();
    if (!rows.length) {
        body.innerHTML = '<tr><td colspan="8" class="ed-empty-row">Chưa có thống kê tướng cho bộ lọc hiện tại.</td></tr>';
        return;
    }

    body.innerHTML = rows.map(item => `
        <tr>
            <td>${esportsBuildHeroCell(item.heroName, item.heroAvatarUrl)}</td>
            <td>${esportsFormatCount(item.pickCount)}</td>
            <td>${esportsFormatCount(item.banCount)}</td>
            <td>${esportsFormatCount(item.presenceCount)}</td>
            <td>${esportsFormatCount(item.pickWins)}</td>
            <td>${esportsFormatRate(item.pickWinRate)}</td>
            <td>${esportsFormatCount(item.blueBanCount)}</td>
            <td>${esportsFormatCount(item.redBanCount)}</td>
        </tr>
    `).join('');
}

function esportsRenderHeroStatsTable() {
    const body = esportsById('hero-stats-body');
    if (!body) return;

    const rows = esportsGetHeroStats();
    const totalGames = esportsSafeNumber(esportsDataState.dashboard?.summary?.totalGames);
    if (!rows.length) {
        body.innerHTML = '<tr><td colspan="20" class="ed-empty-row">Chưa có thống kê tướng cho bộ lọc hiện tại.</td></tr>';
        return;
    }

    body.innerHTML = rows.map((item, index) => {
        const heroName = item.heroName || 'Unknown hero';
        const pickCount = esportsSafeNumber(item.pickCount);
        const pickWins = esportsSafeNumber(item.pickWins);
        const pickLosses = esportsSafeNumber(item.pickLosses);
        const pickWinRate = esportsResolveRate(item.pickWinRate, pickWins, pickCount);
        const pickRate = esportsResolveRate(item.pickRate, pickCount, totalGames);

        const bluePickCount = esportsSafeNumber(item.bluePickCount);
        const bluePickWins = esportsSafeNumber(item.bluePickWins ?? item.blueWins);
        const bluePickLosses = esportsSafeNumber(item.bluePickLosses ?? item.blueLosses);
        const bluePickWinRate = esportsResolveRate(item.bluePickWinRate ?? item.blueWinRate, bluePickWins, bluePickCount);

        const redPickCount = esportsSafeNumber(item.redPickCount);
        const redPickWins = esportsSafeNumber(item.redPickWins ?? item.redWins);
        const redPickLosses = esportsSafeNumber(item.redPickLosses ?? item.redLosses);
        const redPickWinRate = esportsResolveRate(item.redPickWinRate ?? item.redWinRate, redPickWins, redPickCount);

        const banCount = esportsSafeNumber(item.banCount);
        const banRate = esportsResolveRate(item.banRate, banCount, totalGames);
        const presenceCount = esportsSafeNumber(item.presenceCount ?? (pickCount + banCount));
        const presenceRate = esportsResolveRate(item.presenceRate, presenceCount, totalGames);

        const detailTitle = esportsEscapeHtml('Hero details are not available yet.');
        const detailLabel = esportsEscapeHtml(`Show details for ${heroName}`);

        return `
            <tr>
                <td class="ed-hero-stats-rank">${index + 1}</td>
                <td class="ed-hero-stats-hero-cell">${esportsBuildHeroCell(heroName, esportsResolveHeroIconUrl(item))}</td>
                <td><span class="ed-hero-stats-value ed-hero-stats-value-strong">${esportsFormatCount(pickCount)}</span></td>
                <td><span class="ed-hero-stats-value ed-hero-stats-value-win">${esportsFormatCount(pickWins)}</span></td>
                <td><span class="ed-hero-stats-value ed-hero-stats-value-loss">${esportsFormatCount(pickLosses)}</span></td>
                <td><span class="ed-hero-stats-value ed-hero-stats-value-rate">${esportsFormatRateOrDash(pickWinRate, pickCount)}</span></td>
                <td class="ed-hero-stats-divider"><span class="ed-hero-stats-value ed-hero-stats-value-rate">${esportsFormatRateOrDash(pickRate, totalGames)}</span></td>
                <td><span class="ed-hero-stats-value ed-hero-stats-value-strong">${esportsFormatCount(bluePickCount)}</span></td>
                <td><span class="ed-hero-stats-value ed-hero-stats-value-win">${esportsFormatCount(bluePickWins)}</span></td>
                <td><span class="ed-hero-stats-value ed-hero-stats-value-loss">${esportsFormatCount(bluePickLosses)}</span></td>
                <td class="ed-hero-stats-divider"><span class="ed-hero-stats-value ed-hero-stats-value-rate">${esportsFormatRateOrDash(bluePickWinRate, bluePickCount)}</span></td>
                <td><span class="ed-hero-stats-value ed-hero-stats-value-strong">${esportsFormatCount(redPickCount)}</span></td>
                <td><span class="ed-hero-stats-value ed-hero-stats-value-win">${esportsFormatCount(redPickWins)}</span></td>
                <td><span class="ed-hero-stats-value ed-hero-stats-value-loss">${esportsFormatCount(redPickLosses)}</span></td>
                <td class="ed-hero-stats-divider"><span class="ed-hero-stats-value ed-hero-stats-value-rate">${esportsFormatRateOrDash(redPickWinRate, redPickCount)}</span></td>
                <td><span class="ed-hero-stats-value ed-hero-stats-value-strong">${esportsFormatCount(banCount)}</span></td>
                <td class="ed-hero-stats-divider"><span class="ed-hero-stats-value ed-hero-stats-value-rate">${esportsFormatRateOrDash(banRate, totalGames)}</span></td>
                <td><span class="ed-hero-stats-value ed-hero-stats-value-strong">${esportsFormatCount(presenceCount)}</span></td>
                <td class="ed-hero-stats-divider"><span class="ed-hero-stats-value ed-hero-stats-value-rate">${esportsFormatRateOrDash(presenceRate, totalGames)}</span></td>
                <td>
                    <button
                        type="button"
                        class="ed-secondary-button ed-hero-stats-show-button"
                        title="${detailTitle}"
                        aria-label="${detailLabel}"
                        disabled
                        aria-disabled="true"
                    >
                        Show
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function esportsRenderAll() {
    esportsRenderScopeSummary();
    esportsRenderTournamentFilter();
    esportsRenderKpis();
    esportsRenderTopBanned();
    esportsRenderTopBlueBanned();
    esportsRenderTopRedBanned();
    esportsRenderTopPicked();
    esportsRenderSideAdvantage();
    esportsRenderHeroStatsTable();
}

async function esportsLoadTournaments() {
    const payload = await esportsFetchJson(ESPORTS_DATA_API.tournaments);
    esportsDataState.tournaments = Array.isArray(payload) ? payload : [];
    esportsRenderTournamentFilter();
}

async function esportsLoadAnalytics() {
    const scopeQuery = esportsResolveScopeQuery();
    const params = {};
    if (scopeQuery.tournamentId != null) {
        params.tournamentId = scopeQuery.tournamentId;
    } else if (scopeQuery.tournamentName) {
        params.tournamentName = scopeQuery.tournamentName;
    }

    esportsRenderStatus('', '');
    try {
        const dashboard = await esportsFetchJson(esportsBuildUrl(ESPORTS_DATA_API.dashboard, params));
        const topHeroLimit = Math.max(
            ESPORTS_DEFAULT_TABLE_ROWS,
            Array.isArray(dashboard?.heroStats) ? dashboard.heroStats.length : 0
        );
        const [topBanned, topBlueBanned, topRedBanned] = await Promise.all([
            esportsFetchJson(esportsBuildUrl(ESPORTS_DATA_API.topBanned, {
                tournamentId: scopeQuery.tournamentId,
                tournamentName: scopeQuery.tournamentName,
                limit: topHeroLimit
            })),
            esportsFetchJson(esportsBuildUrl(ESPORTS_DATA_API.topBlueBanned, {
                tournamentId: scopeQuery.tournamentId,
                tournamentName: scopeQuery.tournamentName,
                limit: topHeroLimit
            })),
            esportsFetchJson(esportsBuildUrl(ESPORTS_DATA_API.topRedBanned, {
                tournamentId: scopeQuery.tournamentId,
                tournamentName: scopeQuery.tournamentName,
                limit: topHeroLimit
            }))
        ]);

        esportsDataState.dashboard = dashboard || null;
        esportsDataState.topBanned = Array.isArray(topBanned) ? topBanned : [];
        esportsDataState.topBlueBanned = Array.isArray(topBlueBanned) ? topBlueBanned : [];
        esportsDataState.topRedBanned = Array.isArray(topRedBanned) ? topRedBanned : [];
        esportsRenderAll();
    } catch (error) {
        console.error('Esports Data load error:', error);
        esportsRenderStatus(error.message || 'Không thể tải Esports Data.', 'error');
    }
}

function esportsBindEvents() {
    esportsById('tournament-filter')?.addEventListener('change', event => {
        esportsDataState.tournamentScope = event.target.value || '';
        esportsResetTableExpansion();
        esportsLoadAnalytics();
    });

    esportsById('tournament-reset-button')?.addEventListener('click', () => {
        esportsDataState.tournamentScope = '';
        esportsResetTableExpansion();
        const select = esportsById('tournament-filter');
        if (select) {
            select.value = '';
        }
        esportsLoadAnalytics();
    });

    document.querySelectorAll('[data-table-toggle]').forEach(button => {
        button.addEventListener('click', event => {
            const tableKey = event.currentTarget.dataset.tableToggle;
            if (!tableKey) {
                return;
            }
            esportsSetTableExpanded(tableKey, !esportsIsTableExpanded(tableKey));
            esportsRenderAll();
        });
    });
}

async function initEsportsDataPage() {
    if (!document.querySelector('[data-page="esports-data"]')) {
        return;
    }

    const heroStatsBody = esportsById('hero-stats-body');
    if (heroStatsBody) {
        heroStatsBody.innerHTML = '<tr><td colspan="20" class="ed-empty-row">Loading data...</td></tr>';
    }

    esportsBindEvents();
    await esportsLoadTournaments();
    await esportsLoadAnalytics();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initEsportsDataPage);
} else {
    initEsportsDataPage();
}
