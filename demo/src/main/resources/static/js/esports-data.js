const ESPORTS_DATA_API = {
    tournaments: '/api/esports/data/tournaments',
    dashboard: '/api/esports/data/dashboard',
    topBanned: '/api/esports/data/top-banned-heroes',
    topBlueBanned: '/api/esports/data/top-blue-banned-heroes'
};

const HERO_AVATAR_FALLBACK = '/images/heroes/default.jpg';

const esportsDataState = {
    tournamentScope: '',
    tournaments: [],
    dashboard: null,
    topBanned: [],
    topBlueBanned: []
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
    return scope ? (scope.tournamentName || 'Tat ca giai dau') : 'Tat ca giai dau';
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

    select.innerHTML = ['<option value="">Tat ca giai dau</option>']
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
    return esportsGetHeroStats()
        .slice()
        .sort((left, right) => {
            const pickDelta = Number(right.pickCount || 0) - Number(left.pickCount || 0);
            if (pickDelta !== 0) return pickDelta;
            const winRateDelta = Number(right.pickWinRate || 0) - Number(left.pickWinRate || 0);
            if (winRateDelta !== 0) return winRateDelta;
            return String(left.heroName || '').localeCompare(String(right.heroName || ''), 'vi');
        })
        .filter(item => Number(item.pickCount || 0) > 0)
        .slice(0, limit);
}

function esportsRenderScopeSummary() {
    const scopeLabel = esportsCurrentScopeLabel();
    const label = esportsById('scope-label');
    const note = esportsById('scope-note');

    if (label) {
        label.textContent = scopeLabel;
    }
    if (note) {
        note.textContent = esportsDataState.tournamentScope
            ? 'Dang tong hop game draft records thuoc giai dau da chon.'
            : 'Dang tong hop toan bo game draft records trong he thong.';
    }
}

function esportsRenderKpis() {
    const summary = esportsDataState.dashboard && esportsDataState.dashboard.summary
        ? esportsDataState.dashboard.summary
        : {};
    const sideAdvantage = esportsDataState.dashboard && esportsDataState.dashboard.sideAdvantage
        ? esportsDataState.dashboard.sideAdvantage
        : {};
    const mostBanned = (esportsDataState.topBanned || [])[0] || null;
    const mostPicked = esportsGetTopPickedHeroes(1)[0] || null;

    esportsById('kpi-total-games').textContent = esportsFormatCount(summary.totalGames);
    esportsById('kpi-total-matches').textContent = esportsFormatCount(summary.totalMatches);
    esportsById('kpi-blue-win-rate').textContent = esportsFormatRate(summary.blueSideWinRate);
    esportsById('kpi-most-banned').textContent = mostBanned ? mostBanned.heroName : 'N/A';
    esportsById('kpi-most-picked').textContent = mostPicked ? mostPicked.heroName : 'N/A';

    esportsById('kpi-total-games-note').textContent = esportsFormatCount(summary.completedGames) + ' game da co du winner.';
    esportsById('kpi-total-matches-note').textContent = 'Tong ' + esportsFormatCount(summary.totalMatches) + ' series cha trong scope.';
    esportsById('kpi-blue-win-rate-note').textContent = esportsFormatCount(sideAdvantage.completedGames) + ' game da du dieu kien side win rate.';
    esportsById('kpi-most-banned-note').textContent = mostBanned
        ? esportsFormatCount(mostBanned.banCount) + ' luot ban overall.'
        : 'Chua co du lieu ban.';
    esportsById('kpi-most-picked-note').textContent = mostPicked
        ? esportsFormatCount(mostPicked.pickCount) + ' luot pick tu lineup.'
        : 'Chua co du lieu pick.';
}

function esportsRenderTopBanned() {
    const body = esportsById('top-banned-body');
    if (!body) return;

    const totalGames = Number(esportsDataState.dashboard?.summary?.totalGames || 0);
    const rows = Array.isArray(esportsDataState.topBanned) ? esportsDataState.topBanned : [];
    if (!rows.length) {
        body.innerHTML = '<tr><td colspan="4" class="ed-empty-row">Chua co du lieu ban trong scope hien tai.</td></tr>';
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
        body.innerHTML = '<tr><td colspan="4" class="ed-empty-row">Chua co du lieu blue-side bans trong scope hien tai.</td></tr>';
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
        body.innerHTML = '<tr><td colspan="4" class="ed-empty-row">Chua co du lieu pick tu lineup trong scope hien tai.</td></tr>';
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

    esportsById('side-summary').textContent = completedGames > 0
        ? 'Du lieu side advantage duoc tinh tren ' + esportsFormatCount(completedGames) + ' game da co winner.'
        : 'Chua co du game co winner de tinh side advantage.';
}

function esportsRenderHeroStats() {
    const body = esportsById('hero-stats-body');
    if (!body) return;

    const rows = esportsGetHeroStats();
    if (!rows.length) {
        body.innerHTML = '<tr><td colspan="8" class="ed-empty-row">Chua co hero statistics trong scope hien tai.</td></tr>';
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

function esportsRenderAll() {
    esportsRenderScopeSummary();
    esportsRenderTournamentFilter();
    esportsRenderKpis();
    esportsRenderTopBanned();
    esportsRenderTopBlueBanned();
    esportsRenderTopPicked();
    esportsRenderSideAdvantage();
    esportsRenderHeroStats();
    const lastUpdated = esportsById('last-updated');
    if (lastUpdated) {
        lastUpdated.textContent = 'Cap nhat luc ' + new Date().toLocaleString('vi-VN');
    }
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
        const [dashboard, topBanned, topBlueBanned] = await Promise.all([
            esportsFetchJson(esportsBuildUrl(ESPORTS_DATA_API.dashboard, params)),
            esportsFetchJson(esportsBuildUrl(ESPORTS_DATA_API.topBanned, { tournamentId: scopeQuery.tournamentId, tournamentName: scopeQuery.tournamentName, limit: 10 })),
            esportsFetchJson(esportsBuildUrl(ESPORTS_DATA_API.topBlueBanned, { tournamentId: scopeQuery.tournamentId, tournamentName: scopeQuery.tournamentName, limit: 10 }))
        ]);

        esportsDataState.dashboard = dashboard || null;
        esportsDataState.topBanned = Array.isArray(topBanned) ? topBanned : [];
        esportsDataState.topBlueBanned = Array.isArray(topBlueBanned) ? topBlueBanned : [];
        esportsRenderAll();
    } catch (error) {
        console.error('Esports Data load error:', error);
        esportsRenderStatus(error.message || 'Khong the tai Esports Data.', 'error');
    }
}

function esportsBindEvents() {
    esportsById('tournament-filter')?.addEventListener('change', event => {
        esportsDataState.tournamentScope = event.target.value || '';
        esportsLoadAnalytics();
    });

    esportsById('tournament-reset-button')?.addEventListener('click', () => {
        esportsDataState.tournamentScope = '';
        const select = esportsById('tournament-filter');
        if (select) {
            select.value = '';
        }
        esportsLoadAnalytics();
    });
}

async function initEsportsDataPage() {
    if (!document.querySelector('[data-page="esports-data"]')) {
        return;
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
