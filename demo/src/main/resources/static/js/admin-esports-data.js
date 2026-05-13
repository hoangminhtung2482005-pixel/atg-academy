const LEGACY_TOURNAMENT_OPTIONS = [
    { value: '0', label: 'AER International' },
    { value: '1', label: 'AER Pro League' },
    { value: '2', label: 'AER Challenger' }
];

const STAGE_OPTIONS = [
    { value: 'bang', label: 'Vong bang' },
    { value: 'playoff', label: 'Playoff' },
    { value: 'ck', label: 'Chung ket' },
    { value: 'vongloai', label: 'Vong loai' }
];

const LANE_ROLES = ['DSL', 'JGL', 'MID', 'ADL', 'SUP'];
const HERO_SELECT_IDS = [
    'blue-ban-1', 'blue-ban-2', 'blue-ban-3', 'blue-ban-4', 'blue-ban-5',
    'red-ban-1', 'red-ban-2', 'red-ban-3', 'red-ban-4', 'red-ban-5',
    'blue-dsl', 'blue-jgl', 'blue-mid', 'blue-adl', 'blue-sup',
    'red-dsl', 'red-jgl', 'red-mid', 'red-adl', 'red-sup'
];

const state = {
    franchises: [],
    tournaments: [],
    tournamentScopes: [],
    tournamentTeams: [],
    teams: [],
    heroes: [],
    matches: [],
    gameDrafts: [],
    selectedTournamentId: null,
    tournamentFilterFranchiseCode: '',
    selectedMatchId: null,
    selectedGameDraftId: null,
    importPreview: createDefaultImportPreviewState(),
    matchFilters: {
        search: '',
        tournamentScope: '',
        teamCode: '',
        dateFrom: '',
        dateTo: ''
    },
    franchiseForm: createDefaultFranchiseForm(),
    tournamentForm: createDefaultTournamentForm(),
    tournamentTeamForm: createDefaultTournamentTeamForm(),
    matchForm: createDefaultMatchForm(),
    draftForm: createDefaultDraftForm()
};

function byId(id) {
    return document.getElementById(id);
}

function escapeHtml(value) {
    return String(value == null ? '' : value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function normalizeText(value) {
    return String(value == null ? '' : value)
        .trim()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/[đĐ]/g, 'd')
        .toLowerCase();
}

function normalizeTeamCode(code) {
    return String(code == null ? '' : code).trim().toUpperCase();
}

function normalizeStageValue(value) {
    return String(value == null ? '' : value).trim();
}

function toNullableNumber(value) {
    if (value == null || value === '') return null;
    const numeric = Number(value);
    return Number.isFinite(numeric) ? numeric : null;
}

function parseApiErrorMessage(rawText, response) {
    if (!rawText) {
        return `${response.status} ${response.statusText}`;
    }
    try {
        const payload = JSON.parse(rawText);
        return payload.error || payload.message || payload.detail || rawText;
    } catch (error) {
        return rawText;
    }
}

async function apiFetch(url, options) {
    const response = await fetch(url, options || {});
    const rawText = await response.text();
    if (!response.ok) {
        throw new Error(parseApiErrorMessage(rawText, response));
    }
    return rawText ? JSON.parse(rawText) : null;
}

function buildCsvExportUrl() {
    const url = new URL('/api/admin/esports/game-drafts/export', window.location.origin);
    if (state.selectedMatchId) {
        url.searchParams.set('matchId', String(state.selectedMatchId));
        return url.toString();
    }
    if (state.matchFilters.tournamentScope) {
        const scopeQuery = resolveScopeQuery(state.matchFilters.tournamentScope);
        if (scopeQuery.tournamentId != null) {
            url.searchParams.set('tournamentId', String(scopeQuery.tournamentId));
        } else if (scopeQuery.tournamentName) {
            url.searchParams.set('tournamentName', scopeQuery.tournamentName);
        }
    }
    if (state.matchFilters.dateFrom) {
        url.searchParams.set('dateFrom', state.matchFilters.dateFrom);
    }
    if (state.matchFilters.dateTo) {
        url.searchParams.set('dateTo', state.matchFilters.dateTo);
    }
    return url.toString();
}

function resolveDownloadFilename(contentDisposition) {
    const header = String(contentDisposition || '');
    const utf8Match = header.match(/filename\*=UTF-8''([^;]+)/i);
    if (utf8Match && utf8Match[1]) {
        return decodeURIComponent(utf8Match[1]);
    }
    const basicMatch = header.match(/filename="?([^";]+)"?/i);
    return basicMatch && basicMatch[1] ? basicMatch[1] : 'esports-game-drafts.csv';
}

async function downloadGameDraftsCsv() {
    setButtonLoading('btn-export-admin-esports', true, 'Dang xuat CSV...');
    try {
        const response = await fetch(buildCsvExportUrl(), { cache: 'no-store' });
        if (!response.ok) {
            const rawText = await response.text();
            throw new Error(parseApiErrorMessage(rawText, response));
        }

        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = resolveDownloadFilename(response.headers.get('Content-Disposition'));
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.setTimeout(() => window.URL.revokeObjectURL(downloadUrl), 1000);
        showToast('Da bat dau tai file CSV.', 'ok');
    } catch (error) {
        console.error('Cannot export esports game drafts CSV:', error);
        showToast('Khong the xuat du lieu. Vui long thu lai.', 'err');
    } finally {
        setButtonLoading('btn-export-admin-esports', false);
    }
}

function createDefaultMatchForm() {
    return {
        id: null,
        matchDate: toDateTimeLocalInputValue(new Date()),
        team1Code: '',
        team2Code: '',
        tournamentId: '',
        score1: 3,
        score2: 1,
        tier: '1',
        stage: 'bang'
    };
}

function createDefaultFranchiseForm() {
    return {
        id: null,
        code: '',
        name: '',
        tierLevel: 'T1',
        region: '',
        displayOrder: 0,
        active: true,
        logoUrl: '',
        description: ''
    };
}

function createDefaultTournamentForm() {
    return {
        id: null,
        franchiseId: '',
        name: '',
        slug: '',
        seasonYear: '',
        splitName: '',
        tierLevel: 'T1',
        aerTier: 1,
        status: 'UPCOMING',
        startDate: '',
        endDate: '',
        logoUrl: '',
        description: ''
    };
}

function createDefaultTournamentTeamForm() {
    return {
        teamId: '',
        groupName: '',
        seedNumber: '',
        status: 'ACTIVE',
        note: ''
    };
}

function emptyLineup() {
    return { DSL: null, JGL: null, MID: null, ADL: null, SUP: null };
}

function createDefaultDraftForm() {
    return {
        id: null,
        gameNumber: '',
        blueTeamId: '',
        redTeamId: '',
        winnerTeamId: '',
        duration: '',
        draftFormatCode: 'AOV_STANDARD_18',
        source: 'manual',
        blueBans: [null, null, null, null, null],
        redBans: [null, null, null, null, null],
        blueLineup: emptyLineup(),
        redLineup: emptyLineup()
    };
}

function createDefaultImportPreviewState() {
    return {
        file: null,
        fileName: '',
        overwriteExisting: false,
        previewToken: '',
        readyToImport: false,
        summary: null,
        rows: [],
        errors: [],
        warnings: []
    };
}

function setPanelError(targetId, message) {
    const node = byId(targetId);
    if (!node) return;
    node.textContent = message || '';
    node.classList.toggle('hidden', !message);
}

function setButtonLoading(buttonId, loading, loadingText) {
    const button = byId(buttonId);
    if (!button) return;
    if (!button.dataset.originalText) {
        button.dataset.originalText = button.textContent;
    }
    button.disabled = Boolean(loading);
    button.textContent = loading ? (loadingText || 'Dang xu ly...') : button.dataset.originalText;
}

function showToast(message, type) {
    const toast = byId('toast');
    if (!toast) return;
    toast.textContent = message;
    toast.className = `toast-box show ${type || 'ok'}`;
    window.clearTimeout(showToast._timeoutId);
    showToast._timeoutId = window.setTimeout(() => {
        toast.className = 'toast-box';
    }, 2800);
}

function updateClock() {
    const node = byId('header-clock');
    if (!node) return;
    node.textContent = new Date().toLocaleString('vi-VN');
}

function legacyTournamentLabel(tier) {
    const matched = LEGACY_TOURNAMENT_OPTIONS.find(option => option.value === String(tier == null ? '1' : tier));
    return matched ? matched.label : String(tier == null ? '1' : tier);
}

function tournamentScopeLabel(scope) {
    if (!scope) return 'Tat ca giai dau';
    const suffix = scope.legacyScope ? ' [legacy]' : (scope.franchiseCode ? ` · ${scope.franchiseCode}` : '');
    return `${scope.tournamentName || scope.tournamentTier || 'Tournament'}${suffix}`;
}

function matchTournamentLabel(match) {
    if (match && match.tournamentName) {
        return match.tournamentFranchiseCode
            ? `${match.tournamentName} · ${match.tournamentFranchiseCode}`
            : match.tournamentName;
    }
    return legacyTournamentLabel(match ? match.tier : null);
}

function matchTournamentScopeValue(match) {
    if (match && match.tournamentId != null) {
        return `official:${match.tournamentId}`;
    }
    if (match && match.tier != null && match.tier !== '') {
        return `legacy:${match.tier}`;
    }
    return '';
}

function resolveScopeQuery(scopeValue) {
    const raw = String(scopeValue || '').trim();
    if (!raw) {
        return { tournamentId: null, tournamentName: '' };
    }
    if (raw.startsWith('official:')) {
        return { tournamentId: toNullableNumber(raw.slice('official:'.length)), tournamentName: '' };
    }
    if (raw.startsWith('legacy:')) {
        return { tournamentId: null, tournamentName: raw.slice('legacy:'.length) };
    }
    return { tournamentId: null, tournamentName: raw };
}

function buildTournamentScopeOptions(includeAll) {
    const options = state.tournamentScopes.map(scope => ({
        value: scope.tournamentId != null ? `official:${scope.tournamentId}` : `legacy:${scope.tournamentTier || ''}`,
        label: tournamentScopeLabel(scope)
    }));
    if (includeAll) {
        return [{ value: '', label: 'Tat ca giai dau' }].concat(options);
    }
    return options;
}

function buildLegacyTournamentOptions(includeAll) {
    const merged = new Map();
    LEGACY_TOURNAMENT_OPTIONS.forEach(option => merged.set(String(option.value), { value: String(option.value), label: option.label }));
    state.tournamentScopes
        .filter(scope => scope.legacyScope && scope.tournamentTier)
        .forEach(scope => {
            const tier = String(scope.tournamentTier);
            if (!merged.has(tier)) {
                merged.set(tier, { value: tier, label: legacyTournamentLabel(tier) });
            }
        });
    const options = Array.from(merged.values());
    if (includeAll) {
        return [{ value: '', label: 'Tat ca giai / tier legacy' }].concat(options);
    }
    return options;
}

function buildOfficialTournamentOptions(includeEmpty) {
    const options = state.tournaments.slice().map(tournament => ({
        value: tournament.id,
        label: `${tournament.name}${tournament.franchiseCode ? ` · ${tournament.franchiseCode}` : ''}`
    }));
    if (includeEmpty) {
        return [{ value: '', label: 'Chua gan tournament chinh thuc' }].concat(options);
    }
    return options;
}

function stageLabel(stage) {
    const matched = STAGE_OPTIONS.find(option => option.value === String(stage || 'bang'));
    return matched ? matched.label : String(stage || 'bang');
}

function formatDateTime(value) {
    if (!value) return 'Chua co ngay';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }
    return date.toLocaleString('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function formatDuration(seconds) {
    const numeric = Number(seconds);
    if (!Number.isFinite(numeric) || numeric < 0) return 'Chua co';
    const mins = Math.floor(numeric / 60);
    const secs = numeric % 60;
    return `${mins}:${String(secs).padStart(2, '0')}`;
}

function toDateTimeLocalInputValue(value) {
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

function parseDateBoundary(value, boundary) {
    if (!value) return null;
    const date = new Date(value + (boundary === 'end' ? 'T23:59:59' : 'T00:00:00'));
    return Number.isNaN(date.getTime()) ? null : date.getTime();
}

function parseDurationInput(value) {
    const raw = String(value == null ? '' : value).trim();
    if (!raw) {
        return { value: null, error: '' };
    }
    if (/^\d+$/.test(raw)) {
        const seconds = Number(raw);
        if (!Number.isFinite(seconds) || seconds < 0) {
            return { value: null, error: 'Duration phai la so giay khong am.' };
        }
        return { value: seconds, error: '' };
    }
    const matched = raw.match(/^(\d+):(\d{1,2})$/);
    if (!matched) {
        return { value: null, error: 'Duration chi nhan mm:ss hoac so giay.' };
    }
    const minutes = Number(matched[1]);
    const seconds = Number(matched[2]);
    if (!Number.isFinite(minutes) || !Number.isFinite(seconds) || seconds >= 60) {
        return { value: null, error: 'Duration mm:ss khong hop le.' };
    }
    return { value: minutes * 60 + seconds, error: '' };
}

function findTeamById(id) {
    return state.teams.find(team => Number(team.id) === Number(id)) || null;
}

function findFranchiseById(id) {
    return state.franchises.find(franchise => Number(franchise.id) === Number(id)) || null;
}

function findTournamentById(id) {
    return state.tournaments.find(tournament => Number(tournament.id) === Number(id)) || null;
}

function normalizeFranchiseCode(code) {
    return String(code == null ? '' : code).trim().toUpperCase();
}

function resolveTournamentAerTier(tournament) {
    const aerTier = toNullableNumber(tournament && tournament.aerTier);
    return aerTier != null && aerTier > 0 ? aerTier : null;
}

function resolveMatchTierValue(match) {
    const officialAerTier = toNullableNumber(match && match.tournamentAerTier);
    if (officialAerTier != null && officialAerTier > 0) {
        return String(officialAerTier);
    }
    return String(match && match.tier != null ? match.tier : '1');
}

function getTournamentWarningMessages(tournament) {
    const warnings = [];
    if (!tournament || !tournament.franchiseId || !normalizeFranchiseCode(tournament.franchiseCode)) {
        warnings.push('Thieu franchise');
    }
    if (resolveTournamentAerTier(tournament) == null) {
        warnings.push('Thieu AER Tier');
    }
    return warnings;
}

function findTeamByCode(code) {
    const normalized = normalizeTeamCode(code);
    return state.teams.find(team => normalizeTeamCode(team.teamCode) === normalized) || null;
}

function heroById(id) {
    return state.heroes.find(hero => Number(hero.id) === Number(id)) || null;
}

function displayTeamName(team) {
    if (!team) return 'Chua chon team';
    return team.teamName || team.teamCode || 'Unknown team';
}

function getSelectedMatch() {
    return state.matches.find(match => Number(match.id) === Number(state.selectedMatchId)) || null;
}

function getSelectedDraft() {
    return state.gameDrafts.find(draft => Number(draft.id) === Number(state.selectedGameDraftId)) || null;
}

function getSelectedMatchTeams() {
    const match = getSelectedMatch();
    if (!match) return [];
    const team1 = findTeamByCode(match.team1Code);
    const team2 = findTeamByCode(match.team2Code);
    return [team1, team2].filter(Boolean);
}

function compareTeams(left, right) {
    return displayTeamName(left).localeCompare(displayTeamName(right), 'vi', { sensitivity: 'base' });
}

function compareHeroes(left, right) {
    return String(left?.name || '').localeCompare(String(right?.name || ''), 'vi', { sensitivity: 'base' });
}

function renderSelectOptions(targetId, options, selectedValue) {
    const select = byId(targetId);
    if (!select) return;
    const currentValue = selectedValue == null ? '' : String(selectedValue);
    select.innerHTML = options.map(option => (
        `<option value="${escapeHtml(option.value)}"${String(option.value) === currentValue ? ' selected' : ''}>${escapeHtml(option.label)}</option>`
    )).join('');
}

function buildTeamFilterOptions() {
    const options = state.teams.slice().sort(compareTeams).map(team => ({
        value: normalizeTeamCode(team.teamCode),
        label: `${displayTeamName(team)} (${normalizeTeamCode(team.teamCode)})`
    }));
    return [{ value: '', label: 'Tat ca team' }].concat(options);
}

function buildMatchTeamOptions() {
    const teams = getSelectedMatchTeams().slice().sort(compareTeams).map(team => ({
        value: team.id,
        label: `${displayTeamName(team)} (${normalizeTeamCode(team.teamCode)})`
    }));
    return [{ value: '', label: 'Chon team' }].concat(teams);
}

function buildWinnerOptions() {
    const teams = getSelectedMatchTeams().slice().sort(compareTeams).map(team => ({
        value: team.id,
        label: `${displayTeamName(team)} (${normalizeTeamCode(team.teamCode)})`
    }));
    return [{ value: '', label: 'Chua xac dinh winner' }].concat(teams);
}

function buildHeroOptions() {
    return [{ value: '', label: 'Chua chon hero' }].concat(
        state.heroes.slice().sort(compareHeroes).map(hero => ({
            value: hero.id,
            label: hero.name || `Hero #${hero.id}`
        }))
    );
}

function buildFranchiseOptions(includeEmpty) {
    const options = state.franchises.slice().map(franchise => ({
        value: franchise.id,
        label: `${franchise.code} · ${franchise.name}`
    }));
    if (includeEmpty) {
        return [{ value: '', label: 'Chon franchise' }].concat(options);
    }
    return options;
}

function buildTournamentTeamSelectorOptions() {
    return [{ value: '', label: 'Chon tournament de quan ly team' }].concat(
        state.tournaments.slice().map(tournament => ({
            value: tournament.id,
            label: `${tournament.name}${tournament.franchiseCode ? ` · ${tournament.franchiseCode}` : ''}`
        }))
    );
}

function buildTournamentFilterFranchiseOptions() {
    return [{ value: '', label: 'Tat ca franchise' }].concat(
        state.franchises.slice().map(franchise => ({
            value: normalizeFranchiseCode(franchise.code),
            label: `${franchise.code} · ${franchise.name}`
        }))
    );
}

function populateStaticSelects() {
    renderSelectOptions('match-filter-tier', buildTournamentScopeOptions(true), state.matchFilters.tournamentScope);
    renderSelectOptions('mf-tier', buildLegacyTournamentOptions(false), state.matchForm.tier);
    renderSelectOptions('mf-tournament', buildOfficialTournamentOptions(true), state.matchForm.tournamentId);
    renderSelectOptions('match-filter-team', buildTeamFilterOptions(), state.matchFilters.teamCode);
    renderSelectOptions('tf-franchise', buildFranchiseOptions(true), state.tournamentForm.franchiseId);
    renderSelectOptions('tournament-team-selector', buildTournamentTeamSelectorOptions(), state.selectedTournamentId);
    renderSelectOptions('tournament-filter-franchise', buildTournamentFilterFranchiseOptions(), state.tournamentFilterFranchiseCode);

    const teamOptions = [{ value: '', label: 'Chon team' }].concat(
        state.teams.slice().sort(compareTeams).map(team => ({
            value: normalizeTeamCode(team.teamCode),
            label: `${displayTeamName(team)} (${normalizeTeamCode(team.teamCode)})`
        }))
    );
    renderSelectOptions('mf-team1', teamOptions, state.matchForm.team1Code);
    renderSelectOptions('mf-team2', teamOptions, state.matchForm.team2Code);
    renderSelectOptions('ttf-team', [{ value: '', label: 'Chon team' }].concat(
        state.teams.slice().sort(compareTeams).map(team => ({
            value: team.id,
            label: `${displayTeamName(team)} (${normalizeTeamCode(team.teamCode)})`
        }))
    ), state.tournamentTeamForm.teamId);
}

function populateMatchForm() {
    byId('mf-id').value = state.matchForm.id || '';
    byId('mf-date').value = state.matchForm.matchDate || '';
    byId('mf-tournament').value = state.matchForm.tournamentId || '';
    byId('mf-score1').value = state.matchForm.score1;
    byId('mf-score2').value = state.matchForm.score2;
    byId('mf-stage').value = state.matchForm.stage || '';
    byId('mf-tier').value = state.matchForm.tier || '1';
}

function populateFranchiseForm() {
    byId('ff-id').value = state.franchiseForm.id || '';
    byId('ff-code').value = state.franchiseForm.code || '';
    byId('ff-name').value = state.franchiseForm.name || '';
    byId('ff-tier-level').value = state.franchiseForm.tierLevel || 'T1';
    byId('ff-region').value = state.franchiseForm.region || '';
    byId('ff-display-order').value = state.franchiseForm.displayOrder ?? 0;
    byId('ff-active').checked = Boolean(state.franchiseForm.active);
    byId('ff-logo-url').value = state.franchiseForm.logoUrl || '';
    byId('ff-description').value = state.franchiseForm.description || '';
}

function populateTournamentForm() {
    byId('tf-id').value = state.tournamentForm.id || '';
    byId('tf-franchise').value = state.tournamentForm.franchiseId || '';
    byId('tf-name').value = state.tournamentForm.name || '';
    byId('tf-slug').value = state.tournamentForm.slug || '';
    byId('tf-season-year').value = state.tournamentForm.seasonYear || '';
    byId('tf-split-name').value = state.tournamentForm.splitName || '';
    byId('tf-tier-level').value = state.tournamentForm.tierLevel || 'T1';
    byId('tf-aer-tier').value = state.tournamentForm.aerTier ?? 1;
    byId('tf-status').value = state.tournamentForm.status || 'UPCOMING';
    byId('tf-start-date').value = state.tournamentForm.startDate || '';
    byId('tf-end-date').value = state.tournamentForm.endDate || '';
    byId('tf-logo-url').value = state.tournamentForm.logoUrl || '';
    byId('tf-description').value = state.tournamentForm.description || '';
}

function populateTournamentTeamForm() {
    byId('ttf-team').value = state.tournamentTeamForm.teamId || '';
    byId('ttf-group-name').value = state.tournamentTeamForm.groupName || '';
    byId('ttf-seed-number').value = state.tournamentTeamForm.seedNumber || '';
    byId('ttf-status').value = state.tournamentTeamForm.status || 'ACTIVE';
    byId('ttf-note').value = state.tournamentTeamForm.note || '';
}

function populateDraftFormSelects() {
    const teamOptions = buildMatchTeamOptions();
    const winnerOptions = buildWinnerOptions();
    const heroOptions = buildHeroOptions();

    renderSelectOptions('df-blue-team', teamOptions, state.draftForm.blueTeamId);
    renderSelectOptions('df-red-team', teamOptions, state.draftForm.redTeamId);
    renderSelectOptions('df-winner-team', winnerOptions, state.draftForm.winnerTeamId);

    for (let index = 0; index < 5; index += 1) {
        renderSelectOptions(`blue-ban-${index + 1}`, heroOptions, state.draftForm.blueBans[index]);
        renderSelectOptions(`red-ban-${index + 1}`, heroOptions, state.draftForm.redBans[index]);
    }

    LANE_ROLES.forEach(role => {
        renderSelectOptions(`blue-${role.toLowerCase()}`, heroOptions, state.draftForm.blueLineup[role]);
        renderSelectOptions(`red-${role.toLowerCase()}`, heroOptions, state.draftForm.redLineup[role]);
    });
}

function populateDraftForm() {
    byId('df-id').value = state.draftForm.id || '';
    byId('df-game-number').value = state.draftForm.gameNumber || '';
    byId('df-duration').value = state.draftForm.duration || '';
    byId('df-draft-format-code').value = state.draftForm.draftFormatCode || 'AOV_STANDARD_18';
    byId('df-source').value = state.draftForm.source || 'manual';
}

function resetMatchForm() {
    state.matchForm = createDefaultMatchForm();
    byId('match-form-title').textContent = 'Tao match moi';
    byId('match-form-subtitle').textContent = 'CRUD series van giu workflow hien tai de khong anh huong AER va ranking, nhung co the link tournament chinh thuc de export/public uu tien entity moi.';
    byId('btn-match-submit').textContent = 'Luu match';
    setPanelError('match-form-error', '');
    populateStaticSelects();
    populateMatchForm();
}

function resetFranchiseForm() {
    state.franchiseForm = createDefaultFranchiseForm();
    byId('franchise-form-title').textContent = 'Tao franchise moi';
    byId('btn-franchise-submit').textContent = 'Luu franchise';
    setPanelError('franchise-form-error', '');
    populateStaticSelects();
    populateFranchiseForm();
}

function resetTournamentForm() {
    state.tournamentForm = createDefaultTournamentForm();
    byId('tournament-form-title').textContent = 'Tao tournament moi';
    byId('btn-tournament-submit').textContent = 'Luu tournament';
    setPanelError('tournament-form-error', '');
    populateStaticSelects();
    populateTournamentForm();
}

function resetTournamentTeamForm() {
    state.tournamentTeamForm = createDefaultTournamentTeamForm();
    setPanelError('tournament-teams-error', '');
    populateStaticSelects();
    populateTournamentTeamForm();
}

function getNextGameNumber() {
    if (!state.gameDrafts.length) return 1;
    return Math.max(...state.gameDrafts.map(draft => Number(draft.gameNumber) || 0)) + 1;
}

function resetDraftForm() {
    const defaultTeams = getSelectedMatchTeams();
    state.selectedGameDraftId = null;
    state.draftForm = createDefaultDraftForm();
    state.draftForm.gameNumber = state.selectedMatchId ? getNextGameNumber() : '';
    state.draftForm.blueTeamId = defaultTeams[0] ? Number(defaultTeams[0].id) : '';
    state.draftForm.redTeamId = defaultTeams[1] ? Number(defaultTeams[1].id) : '';
    byId('draft-form-title').textContent = 'Them game draft record';
    byId('draft-form-subtitle').textContent = 'Workflow moi luu theo trang thai cuoi cua moi van thay vi 18 phase chi tiet.';
    byId('btn-draft-submit').textContent = 'Luu game draft';
    setPanelError('draft-form-error', '');
    populateDraftFormSelects();
    populateDraftForm();
    renderSelectedDraftCard();
    renderLocalValidation();
}

function resetImportPreview(options) {
    const preserveFile = options && options.preserveFile;
    const existingFile = preserveFile ? state.importPreview.file : null;
    const existingFileName = preserveFile ? state.importPreview.fileName : '';
    const existingOverwrite = preserveFile ? state.importPreview.overwriteExisting : false;
    state.importPreview = createDefaultImportPreviewState();
    state.importPreview.file = existingFile;
    state.importPreview.fileName = existingFileName;
    state.importPreview.overwriteExisting = existingOverwrite;

    if (!preserveFile) {
        const input = byId('import-file-input');
        if (input) {
            input.value = '';
        }
    }

    setPanelError('import-form-error', '');
    renderImportPreview();
}

function openMatchForm(match) {
    state.matchForm = {
        id: match.id,
        matchDate: toDateTimeLocalInputValue(match.matchDate),
        team1Code: normalizeTeamCode(match.team1Code),
        team2Code: normalizeTeamCode(match.team2Code),
        tournamentId: match.tournamentId != null ? Number(match.tournamentId) : '',
        score1: Number(match.score1 == null ? 0 : match.score1),
        score2: Number(match.score2 == null ? 0 : match.score2),
        tier: resolveMatchTierValue(match),
        stage: String(match.stage || 'bang')
    };
    byId('match-form-title').textContent = `Sua match #${match.id}`;
    byId('match-form-subtitle').textContent = 'Cap nhat series hien co ma khong doi ownership cua esports_matches.';
    byId('btn-match-submit').textContent = 'Luu chinh sua';
    setPanelError('match-form-error', '');
    populateStaticSelects();
    populateMatchForm();
}

function openFranchiseForm(franchise) {
    state.franchiseForm = {
        id: franchise.id,
        code: franchise.code || '',
        name: franchise.name || '',
        tierLevel: franchise.tierLevel || 'T1',
        region: franchise.region || '',
        displayOrder: franchise.displayOrder ?? 0,
        active: Boolean(franchise.active),
        logoUrl: franchise.logoUrl || '',
        description: franchise.description || ''
    };
    byId('franchise-form-title').textContent = `Sua franchise #${franchise.id}`;
    byId('btn-franchise-submit').textContent = 'Luu chinh sua';
    setPanelError('franchise-form-error', '');
    populateStaticSelects();
    populateFranchiseForm();
}

function openTournamentForm(tournament) {
    state.tournamentForm = {
        id: tournament.id,
        franchiseId: tournament.franchiseId != null ? Number(tournament.franchiseId) : '',
        name: tournament.name || '',
        slug: tournament.slug || '',
        seasonYear: tournament.seasonYear ?? '',
        splitName: tournament.splitName || '',
        tierLevel: tournament.tierLevel || 'T1',
        aerTier: resolveTournamentAerTier(tournament) ?? 1,
        status: tournament.status || 'UPCOMING',
        startDate: tournament.startDate || '',
        endDate: tournament.endDate || '',
        logoUrl: tournament.logoUrl || '',
        description: tournament.description || ''
    };
    byId('tournament-form-title').textContent = `Sua tournament #${tournament.id}`;
    byId('btn-tournament-submit').textContent = 'Luu chinh sua';
    setPanelError('tournament-form-error', '');
    populateStaticSelects();
    populateTournamentForm();
}

function openDraftForm(draft) {
    const blueBans = Array.isArray(draft.blueBans) ? draft.blueBans : [];
    const redBans = Array.isArray(draft.redBans) ? draft.redBans : [];
    const blueLineup = draft.blueLineup || {};
    const redLineup = draft.redLineup || {};

    state.selectedGameDraftId = Number(draft.id);
    state.draftForm = {
        id: draft.id,
        gameNumber: Number(draft.gameNumber || 1),
        blueTeamId: draft.blueTeam && draft.blueTeam.id != null ? Number(draft.blueTeam.id) : '',
        redTeamId: draft.redTeam && draft.redTeam.id != null ? Number(draft.redTeam.id) : '',
        winnerTeamId: draft.winnerTeam && draft.winnerTeam.id != null ? Number(draft.winnerTeam.id) : '',
        duration: draft.durationSeconds == null ? '' : formatDuration(draft.durationSeconds),
        draftFormatCode: draft.draftFormatCode || 'AOV_STANDARD_18',
        source: draft.source || 'manual',
        blueBans: [0, 1, 2, 3, 4].map(index => blueBans[index] && blueBans[index].id != null ? Number(blueBans[index].id) : null),
        redBans: [0, 1, 2, 3, 4].map(index => redBans[index] && redBans[index].id != null ? Number(redBans[index].id) : null),
        blueLineup: {
            DSL: blueLineup.DSL && blueLineup.DSL.id != null ? Number(blueLineup.DSL.id) : null,
            JGL: blueLineup.JGL && blueLineup.JGL.id != null ? Number(blueLineup.JGL.id) : null,
            MID: blueLineup.MID && blueLineup.MID.id != null ? Number(blueLineup.MID.id) : null,
            ADL: blueLineup.ADL && blueLineup.ADL.id != null ? Number(blueLineup.ADL.id) : null,
            SUP: blueLineup.SUP && blueLineup.SUP.id != null ? Number(blueLineup.SUP.id) : null
        },
        redLineup: {
            DSL: redLineup.DSL && redLineup.DSL.id != null ? Number(redLineup.DSL.id) : null,
            JGL: redLineup.JGL && redLineup.JGL.id != null ? Number(redLineup.JGL.id) : null,
            MID: redLineup.MID && redLineup.MID.id != null ? Number(redLineup.MID.id) : null,
            ADL: redLineup.ADL && redLineup.ADL.id != null ? Number(redLineup.ADL.id) : null,
            SUP: redLineup.SUP && redLineup.SUP.id != null ? Number(redLineup.SUP.id) : null
        }
    };
    byId('draft-form-title').textContent = `Sua game draft #${draft.id}`;
    byId('draft-form-subtitle').textContent = `Match #${draft.matchId} - Game ${draft.gameNumber}. Cap nhat side, bans, lineup va winner theo record tung van.`;
    byId('btn-draft-submit').textContent = 'Luu chinh sua';
    setPanelError('draft-form-error', '');
    populateDraftFormSelects();
    populateDraftForm();
    renderSelectedDraftCard();
    renderLocalValidation();
}

async function selectTournament(tournamentId) {
    state.selectedTournamentId = tournamentId ? Number(tournamentId) : null;
    state.tournamentTeams = [];
    resetTournamentTeamForm();
    renderSelectedTournamentCard();
    renderTournamentTeams();
    populateStaticSelects();
    if (!state.selectedTournamentId) {
        return;
    }
    try {
        await loadTournamentTeams(state.selectedTournamentId);
        renderSelectedTournamentCard();
        renderTournamentTeams();
        populateStaticSelects();
    } catch (error) {
        setPanelError('tournament-teams-error', error.message);
        renderSelectedTournamentCard();
        renderTournamentTeams();
    }
}

function syncTournamentFilterFromDom() {
    state.tournamentFilterFranchiseCode = normalizeFranchiseCode(byId('tournament-filter-franchise')?.value || '');
}

function syncMatchFiltersFromDom() {
    state.matchFilters.search = byId('match-filter-search').value.trim();
    state.matchFilters.tournamentScope = byId('match-filter-tier').value;
    state.matchFilters.teamCode = normalizeTeamCode(byId('match-filter-team').value);
    state.matchFilters.dateFrom = byId('match-filter-date-from').value;
    state.matchFilters.dateTo = byId('match-filter-date-to').value;
}

function resetMatchFilters() {
    state.matchFilters = {
        search: '',
        tournamentScope: '',
        teamCode: '',
        dateFrom: '',
        dateTo: ''
    };
    byId('match-filter-search').value = '';
    byId('match-filter-tier').value = '';
    byId('match-filter-team').value = '';
    byId('match-filter-date-from').value = '';
    byId('match-filter-date-to').value = '';
    renderMatches();
}

function getMatchSearchText(match) {
    return normalizeText([
        displayTeamName(findTeamByCode(match.team1Code)),
        displayTeamName(findTeamByCode(match.team2Code)),
        match.team1Code,
        match.team2Code,
        matchTournamentLabel(match),
        stageLabel(match.stage)
    ].join(' '));
}

function getFilteredMatches() {
    const search = normalizeText(state.matchFilters.search);
    const dateFrom = parseDateBoundary(state.matchFilters.dateFrom, 'start');
    const dateTo = parseDateBoundary(state.matchFilters.dateTo, 'end');

    return state.matches.filter(match => {
        if (state.matchFilters.tournamentScope && matchTournamentScopeValue(match) !== state.matchFilters.tournamentScope) {
            return false;
        }
        if (state.matchFilters.teamCode) {
            const code = normalizeTeamCode(state.matchFilters.teamCode);
            if (normalizeTeamCode(match.team1Code) !== code && normalizeTeamCode(match.team2Code) !== code) {
                return false;
            }
        }
        if (search && !getMatchSearchText(match).includes(search)) {
            return false;
        }
        if (dateFrom || dateTo) {
            const matchDate = new Date(match.matchDate);
            if (Number.isNaN(matchDate.getTime())) return false;
            const time = matchDate.getTime();
            if (dateFrom && time < dateFrom) return false;
            if (dateTo && time > dateTo) return false;
        }
        return true;
    });
}

function renderFranchises() {
    const tbody = byId('franchises-tbody');
    if (!tbody) return;
    byId('franchise-count-pill').textContent = `${state.franchises.length} franchise`;
    if (!state.franchises.length) {
        tbody.innerHTML = '<tr><td colspan="6" class="table-state">Chua co franchise nao.</td></tr>';
        return;
    }

    tbody.innerHTML = state.franchises.map(franchise => `
        <tr>
            <td><strong>${escapeHtml(franchise.code)}</strong></td>
            <td>${escapeHtml(franchise.name)}</td>
            <td><span class="chip">${escapeHtml(franchise.tierLevel || 'N/A')}</span></td>
            <td>${escapeHtml(franchise.region || 'N/A')}</td>
            <td>${franchise.active ? '<span class="chip">Active</span>' : '<span class="chip">Inactive</span>'}</td>
            <td>
                <div class="row-actions wrap">
                    <button type="button" class="btn btn-light btn-small" data-action="edit-franchise" data-id="${franchise.id}">Sua</button>
                    <button type="button" class="btn btn-danger btn-small" data-action="delete-franchise" data-id="${franchise.id}">${franchise.active ? 'Deactivate' : 'Giu inactive'}</button>
                </div>
            </td>
        </tr>
    `).join('');
}

function renderTournaments() {
    const tbody = byId('tournaments-tbody');
    if (!tbody) return;
    const filterCode = normalizeFranchiseCode(state.tournamentFilterFranchiseCode);
    const tournaments = state.tournaments.filter(tournament => !filterCode || normalizeFranchiseCode(tournament.franchiseCode) === filterCode);
    byId('tournament-count-pill').textContent = filterCode
        ? `${tournaments.length} / ${state.tournaments.length} tournament`
        : `${state.tournaments.length} tournament`;
    if (!tournaments.length) {
        tbody.innerHTML = '<tr><td colspan="10" class="table-state">Khong co tournament nao khop franchise filter hien tai.</td></tr>';
        return;
    }

    tbody.innerHTML = tournaments.map(tournament => {
        const selected = Number(tournament.id) === Number(state.selectedTournamentId);
        const aerTier = resolveTournamentAerTier(tournament);
        const warnings = getTournamentWarningMessages(tournament);
        return `
            <tr class="${selected ? 'row-selected' : ''}">
                <td>
                    <strong>${escapeHtml(tournament.name)}</strong>
                    <div class="table-subtext">${escapeHtml(tournament.slug || 'No slug')}</div>
                </td>
                <td>${escapeHtml(tournament.franchiseCode || '')}</td>
                <td><span class="chip">${escapeHtml(aerTier != null ? `AER ${aerTier}` : 'Missing')}</span></td>
                <td><span class="chip">${escapeHtml(tournament.tierLevel || 'N/A')}</span></td>
                <td>${escapeHtml(String(tournament.seasonYear || 'N/A'))}</td>
                <td>${escapeHtml(tournament.splitName || 'N/A')}</td>
                <td><span class="chip">${escapeHtml(tournament.status || 'UPCOMING')}</span></td>
                <td>${escapeHtml(String(tournament.teamCount || 0))}</td>
                <td>${warnings.length ? warnings.map(message => `<span class="chip">${escapeHtml(message)}</span>`).join(' ') : '<span class="chip">OK</span>'}</td>
                <td>
                    <div class="row-actions wrap">
                        <button type="button" class="btn btn-light btn-small" data-action="select-tournament" data-id="${tournament.id}">${selected ? 'Dang xem teams' : 'Teams'}</button>
                        <button type="button" class="btn btn-light btn-small" data-action="edit-tournament" data-id="${tournament.id}">Sua</button>
                        <button type="button" class="btn btn-danger btn-small" data-action="delete-tournament" data-id="${tournament.id}">Xoa</button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function renderSelectedTournamentCard() {
    const card = byId('selected-tournament-card');
    if (!card) return;
    const tournament = findTournamentById(state.selectedTournamentId);

    if (!tournament) {
        card.className = 'selected-summary empty';
        card.textContent = 'Chon tournament de xem franchise, AER Tier va canh bao du lieu.';
        return;
    }

    const aerTier = resolveTournamentAerTier(tournament);
    const warnings = getTournamentWarningMessages(tournament);
    const summaryChips = [
        `<span class="chip">${escapeHtml(tournament.franchiseCode || 'No franchise')}</span>`,
        `<span class="chip">${escapeHtml(aerTier != null ? `AER ${aerTier}` : 'AER missing')}</span>`,
        `<span class="chip">${escapeHtml(tournament.tierLevel || 'Tier level N/A')}</span>`,
        `<span class="chip">${escapeHtml(`${Number(tournament.teamCount || 0)} team`)}</span>`,
        `<span class="chip">${escapeHtml(`${Number(tournament.linkedMatchCount || 0)} match link`)}</span>`
    ];

    card.className = `selected-summary${warnings.length ? ' warning' : ''}`;
    card.innerHTML = `
        <strong>${escapeHtml(tournament.name || 'Tournament')}</strong>
        <div class="table-subtext">${escapeHtml(tournament.description || 'Tournament nay se la nguon tier cho workflow AER JSON o task sau.')}</div>
        <div class="chip-row">${summaryChips.join('')}</div>
        ${warnings.length ? `<div class="chip-row">${warnings.map(message => `<span class="chip">${escapeHtml(message)}</span>`).join('')}</div>` : ''}
    `;
}

function renderTournamentTeams() {
    const tbody = byId('tournament-teams-tbody');
    const selectedTournament = findTournamentById(state.selectedTournamentId);
    if (!tbody) return;

    if (!selectedTournament) {
        byId('tournament-team-count-pill').textContent = '0 team';
        tbody.innerHTML = '<tr><td colspan="6" class="table-state">Chon tournament de quan ly danh sach team tham gia.</td></tr>';
        return;
    }

    byId('tournament-team-count-pill').textContent = `${state.tournamentTeams.length} team`;
    if (!state.tournamentTeams.length) {
        tbody.innerHTML = '<tr><td colspan="6" class="table-state">Tournament nay chua co team nao. Chon team ben duoi de add.</td></tr>';
        return;
    }

    tbody.innerHTML = state.tournamentTeams.map(item => `
        <tr>
            <td><strong>${escapeHtml(item.teamName || item.teamCode || 'Team')}</strong></td>
            <td>${escapeHtml(item.groupName || 'N/A')}</td>
            <td>${escapeHtml(String(item.seedNumber || 'N/A'))}</td>
            <td>${escapeHtml(item.status || 'ACTIVE')}</td>
            <td>${escapeHtml(item.note || '')}</td>
            <td>
                <button type="button" class="btn btn-danger btn-small" data-action="remove-tournament-team" data-id="${item.teamId}">Remove</button>
            </td>
        </tr>
    `).join('');
}

function renderMatches() {
    const tbody = byId('matches-tbody');
    const matches = getFilteredMatches();
    byId('match-count-pill').textContent = `${matches.length} match`;

    if (!matches.length) {
        tbody.innerHTML = '<tr><td colspan="6" class="table-state">Khong tim thay match nao theo bo loc hien tai.</td></tr>';
        return;
    }

    tbody.innerHTML = matches.map(match => {
        const selected = Number(match.id) === Number(state.selectedMatchId);
        return `
            <tr class="${selected ? 'row-selected' : ''}">
                <td>${escapeHtml(formatDateTime(match.matchDate))}</td>
                <td>
                    <div class="match-teams-block">
                        <strong>${escapeHtml(displayTeamName(findTeamByCode(match.team1Code)))}</strong>
                        <span class="match-vs">vs</span>
                        <strong>${escapeHtml(displayTeamName(findTeamByCode(match.team2Code)))}</strong>
                    </div>
                    <div class="table-subtext">${escapeHtml(normalizeTeamCode(match.team1Code))} vs ${escapeHtml(normalizeTeamCode(match.team2Code))}</div>
                </td>
                <td><span class="chip">${escapeHtml(matchTournamentLabel(match))}</span></td>
                <td><span class="chip">${escapeHtml(stageLabel(match.stage))}</span></td>
                <td><span class="score-chip">${escapeHtml(`${match.score1} - ${match.score2}`)}</span></td>
                <td>
                    <div class="row-actions wrap">
                        <button type="button" class="btn btn-light btn-small" data-action="select-match" data-id="${match.id}">${selected ? 'Dang chon' : 'Chon'}</button>
                        <button type="button" class="btn btn-light btn-small" data-action="edit-match" data-id="${match.id}">Sua</button>
                        <button type="button" class="btn btn-danger btn-small" data-action="delete-match" data-id="${match.id}">Xoa</button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function renderSelectedMatchCard() {
    const card = byId('selected-match-card');
    const match = getSelectedMatch();
    if (!card) return;

    if (!match) {
        card.className = 'selected-summary empty';
        card.textContent = 'Chua chon match de quan ly game draft records.';
        return;
    }

    const team1 = findTeamByCode(match.team1Code);
    const team2 = findTeamByCode(match.team2Code);
    const completeCount = state.gameDrafts.filter(draft => draft.draftCompleteness && draft.draftCompleteness.complete).length;
    const incompleteCount = Math.max(0, state.gameDrafts.length - completeCount);

    card.className = 'selected-summary';
    card.innerHTML = `
        <div class="selected-summary-grid">
            <div>
                <span class="summary-label">Series dang chon</span>
                <strong class="summary-value">Match #${match.id} - ${escapeHtml(displayTeamName(team1))} vs ${escapeHtml(displayTeamName(team2))}</strong>
                <p class="summary-note">${escapeHtml(formatDateTime(match.matchDate))} - ${escapeHtml(matchTournamentLabel(match))} - ${escapeHtml(stageLabel(match.stage))}</p>
            </div>
            <div>
                <span class="summary-label">Ty so series</span>
                <strong class="summary-value">${escapeHtml(`${match.score1} - ${match.score2}`)}</strong>
                <p class="summary-note">Match nay dang co ${state.gameDrafts.length} game draft record, ${completeCount} complete va ${incompleteCount} incomplete.</p>
            </div>
            <div>
                <span class="summary-label">Blue / Red options</span>
                <strong class="summary-value">${escapeHtml(displayTeamName(team1))} / ${escapeHtml(displayTeamName(team2))}</strong>
                <p class="summary-note">Editor chi cho phep chon dung 2 team cua series hien tai.</p>
            </div>
            <div>
                <span class="summary-label">Workflow</span>
                <strong class="summary-value">Series -> Game draft records</strong>
                <p class="summary-note">Them van, cap nhat bans va lineup, sau do verify completeness de public analytics doc tu bang moi.</p>
            </div>
        </div>
    `;
}

function renderStatusChip(statusText, complete) {
    return `<span class="verify-status-chip ${complete ? 'ok' : 'warn'}">${escapeHtml(statusText)}</span>`;
}

function renderGameDraftList() {
    const host = byId('game-draft-list');
    if (!host) return;

    if (!state.selectedMatchId) {
        host.innerHTML = '<div class="selected-summary empty">Chon match de tai game draft records.</div>';
        byId('draft-count-pill').textContent = '0 van';
        return;
    }

    byId('draft-count-pill').textContent = `${state.gameDrafts.length} van`;
    if (!state.gameDrafts.length) {
        host.innerHTML = '<div class="selected-summary empty">Match nay chua co van nao. Bam "Them van" de tao game draft record dau tien.</div>';
        return;
    }

    host.innerHTML = state.gameDrafts.map(draft => {
        const selected = Number(draft.id) === Number(state.selectedGameDraftId);
        const completeness = draft.draftCompleteness || {};
        const blueTeamName = draft.blueTeam ? draft.blueTeam.teamName || draft.blueTeam.teamCode : 'Blue';
        const redTeamName = draft.redTeam ? draft.redTeam.teamName || draft.redTeam.teamCode : 'Red';
        const winnerName = draft.winnerTeam ? (draft.winnerTeam.teamName || draft.winnerTeam.teamCode) : 'Chua co winner';

        return `
            <article class="game-draft-card ${selected ? 'selected' : ''}">
                <div class="game-draft-card-head">
                    <div>
                        <span class="summary-label">Game ${escapeHtml(draft.gameNumber)}</span>
                        <strong>${escapeHtml(blueTeamName)} vs ${escapeHtml(redTeamName)}</strong>
                        <p class="summary-note">${escapeHtml(draft.draftFormatCode || 'AOV_STANDARD_18')} - ${escapeHtml(draft.source || 'manual')}</p>
                    </div>
                    ${renderStatusChip(completeness.status || 'Incomplete', Boolean(completeness.complete))}
                </div>
                <div class="game-draft-card-metrics">
                    <div><span>Winner</span><strong>${escapeHtml(winnerName)}</strong></div>
                    <div><span>Duration</span><strong>${escapeHtml(draft.durationText || 'Chua co')}</strong></div>
                    <div><span>Completeness</span><strong>${Number(completeness.banCount || 0)}/${Number(completeness.pickCount || 0)}</strong></div>
                </div>
                <div class="game-draft-card-footer">
                    <small>${escapeHtml((completeness.missingFields || []).length ? `Missing: ${(completeness.missingFields || []).join(', ')}` : 'Du data cho game-level analytics.')}</small>
                    <div class="row-actions">
                        <button type="button" class="btn btn-light btn-small" data-action="select-draft" data-id="${draft.id}">${selected ? 'Dang sua' : 'Sua'}</button>
                        <button type="button" class="btn btn-danger btn-small" data-action="delete-draft" data-id="${draft.id}">Xoa</button>
                    </div>
                </div>
            </article>
        `;
    }).join('');
}

function collectDraftHeroIds() {
    return []
        .concat(state.draftForm.blueBans)
        .concat(state.draftForm.redBans)
        .concat(LANE_ROLES.map(role => state.draftForm.blueLineup[role]))
        .concat(LANE_ROLES.map(role => state.draftForm.redLineup[role]))
        .filter(value => value != null && value !== '');
}

function getDuplicateHeroIds() {
    const counts = new Map();
    collectDraftHeroIds().forEach(heroId => {
        const key = Number(heroId);
        counts.set(key, (counts.get(key) || 0) + 1);
    });
    return Array.from(counts.entries())
        .filter(([, count]) => count > 1)
        .map(([heroId]) => heroId);
}

function countFilledLineup(side) {
    return LANE_ROLES.filter(role => state.draftForm[`${side}Lineup`][role]).length;
}

function countFilledBans(side) {
    return state.draftForm[`${side}Bans`].filter(Boolean).length;
}

function buildLocalValidation() {
    const issues = [];
    const selectedMatch = getSelectedMatch();
    const allowedTeamIds = new Set(getSelectedMatchTeams().map(team => Number(team.id)));
    const durationResult = parseDurationInput(state.draftForm.duration);
    const duplicateHeroIds = getDuplicateHeroIds();
    const blueBans = countFilledBans('blue');
    const redBans = countFilledBans('red');
    const bluePicks = countFilledLineup('blue');
    const redPicks = countFilledLineup('red');

    if (!selectedMatch) {
        issues.push('Chua chon match.');
    }
    if (!state.draftForm.gameNumber || Number(state.draftForm.gameNumber) <= 0) {
        issues.push('Game number phai lon hon 0.');
    }
    if (!state.draftForm.blueTeamId || !state.draftForm.redTeamId) {
        issues.push('Blue team va Red team la bat buoc.');
    }
    if (state.draftForm.blueTeamId && state.draftForm.redTeamId
        && Number(state.draftForm.blueTeamId) === Number(state.draftForm.redTeamId)) {
        issues.push('Blue team va Red team khong duoc trung nhau.');
    }
    if (allowedTeamIds.size && (
        !allowedTeamIds.has(Number(state.draftForm.blueTeamId || 0))
        || !allowedTeamIds.has(Number(state.draftForm.redTeamId || 0))
    )) {
        issues.push('Blue/Red team phai thuoc dung 2 doi cua match.');
    }
    if (state.draftForm.winnerTeamId && (
        Number(state.draftForm.winnerTeamId) !== Number(state.draftForm.blueTeamId || 0)
        && Number(state.draftForm.winnerTeamId) !== Number(state.draftForm.redTeamId || 0)
    )) {
        issues.push('Winner phai la Blue team hoac Red team.');
    }
    if (durationResult.error) {
        issues.push(durationResult.error);
    }
    if (duplicateHeroIds.length) {
        issues.push(`Khong duoc trung hero: ${duplicateHeroIds.map(heroId => heroById(heroId)?.name || `Hero #${heroId}`).join(', ')}.`);
    }

    const isComplete = bluePicks === 5 && redPicks === 5 && (blueBans + redBans) >= 8 && Boolean(state.draftForm.winnerTeamId);
    return {
        issues,
        durationSeconds: durationResult.value,
        blueBans,
        redBans,
        bluePicks,
        redPicks,
        isComplete
    };
}

function renderSelectedDraftCard() {
    const card = byId('selected-draft-card');
    const pill = byId('editor-status-pill');
    if (!card || !pill) return;

    const selectedDraft = getSelectedDraft();
    const validation = buildLocalValidation();
    const blueTeam = findTeamById(state.draftForm.blueTeamId);
    const redTeam = findTeamById(state.draftForm.redTeamId);
    const winnerTeam = findTeamById(state.draftForm.winnerTeamId);
    const winnerText = winnerTeam ? displayTeamName(winnerTeam) : 'Chua co winner';

    pill.textContent = selectedDraft ? `Dang sua game #${selectedDraft.gameNumber}` : 'Dang tao ban moi';

    if (!state.selectedMatchId) {
        card.className = 'selected-summary empty';
        card.textContent = 'Chon mot match truoc khi them hoac sua game draft record.';
        return;
    }

    card.className = `selected-summary${validation.issues.length ? ' warning' : ''}`;
    card.innerHTML = `
        <div class="selected-summary-grid">
            <div>
                <span class="summary-label">Record hien tai</span>
                <strong class="summary-value">${selectedDraft ? `Game draft #${selectedDraft.id}` : 'Ban ghi moi'}</strong>
                <p class="summary-note">Game ${escapeHtml(state.draftForm.gameNumber || '...')} - ${escapeHtml(displayTeamName(blueTeam))} vs ${escapeHtml(displayTeamName(redTeam))}</p>
            </div>
            <div>
                <span class="summary-label">Winner / Duration</span>
                <strong class="summary-value">${escapeHtml(winnerText)}</strong>
                <p class="summary-note">${escapeHtml(state.draftForm.duration ? formatDuration(parseDurationInput(state.draftForm.duration).value) : 'Chua co duration')}</p>
            </div>
            <div>
                <span class="summary-label">Bans</span>
                <strong class="summary-value">${validation.blueBans + validation.redBans} slot</strong>
                <p class="summary-note">Blue ${validation.blueBans}/5 - Red ${validation.redBans}/5</p>
            </div>
            <div>
                <span class="summary-label">Lineup</span>
                <strong class="summary-value">${validation.bluePicks + validation.redPicks}/10 hero</strong>
                <p class="summary-note">Blue ${validation.bluePicks}/5 - Red ${validation.redPicks}/5</p>
            </div>
        </div>
    `;
}

function renderLocalValidation() {
    const host = byId('local-validation-list');
    if (!host) return;

    const validation = buildLocalValidation();
    const items = [
        ['Match', state.selectedMatchId ? renderStatusChip('Da chon match', true) : renderStatusChip('Chua chon match', false)],
        ['Winner', state.draftForm.winnerTeamId ? renderStatusChip('Da co winner', true) : renderStatusChip('Missing winner', false)],
        ['Bans', renderStatusChip(`${validation.blueBans + validation.redBans}/10 slot`, (validation.blueBans + validation.redBans) >= 8)],
        ['Lineup', renderStatusChip(`${validation.bluePicks + validation.redPicks}/10 hero`, validation.bluePicks === 5 && validation.redPicks === 5)],
        ['Duration', renderStatusChip(validation.durationSeconds == null ? 'Chua co duration' : `${validation.durationSeconds}s`, !parseDurationInput(state.draftForm.duration).error)],
        ['Duplicate hero', validation.issues.some(issue => issue.startsWith('Khong duoc trung hero'))
            ? renderStatusChip('Dang bi trung', false)
            : renderStatusChip('Khong trung', true)]
    ];

    const extraIssues = validation.issues.length
        ? `<div class="verify-item full">
                <span class="verify-item-label">Issues</span>
                <span class="verify-item-value">${escapeHtml(validation.issues.join(' | '))}</span>
           </div>`
        : `<div class="verify-item full">
                <span class="verify-item-label">Ready</span>
                <span class="verify-item-value">Form hop le de luu. Record ${validation.isComplete ? 'da du completeness co ban' : 'van co the luu dang incomplete'}.</span>
           </div>`;

    host.innerHTML = items.map(([label, value]) => `
        <div class="verify-item">
            <span class="verify-item-label">${escapeHtml(label)}</span>
            <span class="verify-item-value">${value}</span>
        </div>
    `).join('') + extraIssues;
}

function renderValidationSummary() {
    const grid = byId('draft-validation-grid');
    const details = byId('draft-validation-details');
    if (!grid || !details) return;

    if (!state.selectedMatchId) {
        grid.innerHTML = `
            <article class="summary-card">
                <span class="summary-label">Tong game</span>
                <strong class="summary-value">0</strong>
                <p class="summary-note">Chon match de xem summary.</p>
            </article>
        `;
        details.innerHTML = `
            <article class="verify-card">
                <h4>Missing fields</h4>
                <div class="verify-items">
                    <div class="verify-item">
                        <span class="verify-item-label">Status</span>
                        <span class="verify-item-value">Chua co du lieu.</span>
                    </div>
                </div>
            </article>
        `;
        return;
    }

    const totalGames = state.gameDrafts.length;
    const completeGames = state.gameDrafts.filter(draft => draft.draftCompleteness && draft.draftCompleteness.complete).length;
    const incompleteGames = Math.max(0, totalGames - completeGames);
    const totalBans = state.gameDrafts.reduce((sum, draft) => sum + Number(draft?.draftCompleteness?.banCount || 0), 0);
    const totalPicks = state.gameDrafts.reduce((sum, draft) => sum + Number(draft?.draftCompleteness?.pickCount || 0), 0);
    const missingWinner = state.gameDrafts.filter(draft => (draft?.draftCompleteness?.missingFields || []).includes('winner')).length;
    const missingBans = state.gameDrafts.filter(draft => (draft?.draftCompleteness?.missingFields || []).includes('bans')).length;
    const missingLineup = state.gameDrafts.filter(draft => (draft?.draftCompleteness?.missingFields || []).includes('lineup')).length;

    grid.innerHTML = `
        <article class="summary-card">
            <span class="summary-label">Tong game</span>
            <strong class="summary-value">${totalGames}</strong>
            <p class="summary-note">Tong so van dang gan vao series hien tai.</p>
        </article>
        <article class="summary-card">
            <span class="summary-label">Complete / Incomplete</span>
            <strong class="summary-value">${completeGames} / ${incompleteGames}</strong>
            <p class="summary-note">Complete can co lineup 10/10, bans >= 8 va winner.</p>
        </article>
        <article class="summary-card">
            <span class="summary-label">Tong bans</span>
            <strong class="summary-value">${totalBans}</strong>
            <p class="summary-note">Tong so hero ban dang luu tren match nay.</p>
        </article>
        <article class="summary-card">
            <span class="summary-label">Tong picks</span>
            <strong class="summary-value">${totalPicks}</strong>
            <p class="summary-note">Tong so hero lineup dang luu tren match nay.</p>
        </article>
    `;

    details.innerHTML = `
        <article class="verify-card">
            <h4>Missing fields</h4>
            <div class="verify-items">
                <div class="verify-item"><span class="verify-item-label">Winner</span><span class="verify-item-value">${missingWinner} game</span></div>
                <div class="verify-item"><span class="verify-item-label">Bans</span><span class="verify-item-value">${missingBans} game</span></div>
                <div class="verify-item"><span class="verify-item-label">Lineup</span><span class="verify-item-value">${missingLineup} game</span></div>
                <div class="verify-item full"><span class="verify-item-label">Status</span><span class="verify-item-value">${incompleteGames ? 'Can review cac game co status incomplete truoc khi doi chieu trang public.' : 'Tat ca game draft records trong match nay da co completeness co ban.'}</span></div>
            </div>
        </article>
        <article class="verify-card">
            <h4>Selected draft</h4>
            <div class="verify-items">
                ${(() => {
                    const selectedDraft = getSelectedDraft();
                    if (!selectedDraft) {
                        return '<div class="verify-item"><span class="verify-item-label">Record</span><span class="verify-item-value">Chua chon game draft record.</span></div>';
                    }
                    const completeness = selectedDraft.draftCompleteness || {};
                    return `
                        <div class="verify-item"><span class="verify-item-label">Record</span><span class="verify-item-value">Game ${selectedDraft.gameNumber}</span></div>
                        <div class="verify-item"><span class="verify-item-label">Status</span><span class="verify-item-value">${renderStatusChip(completeness.status || 'Incomplete', Boolean(completeness.complete))}</span></div>
                        <div class="verify-item"><span class="verify-item-label">Missing</span><span class="verify-item-value">${escapeHtml((completeness.missingFields || []).join(', ') || 'Khong co')}</span></div>
                        <div class="verify-item"><span class="verify-item-label">Duration</span><span class="verify-item-value">${escapeHtml(selectedDraft.durationText || 'Chua co')}</span></div>
                    `;
                })()}
            </div>
        </article>
    `;
}

function renderImportPreview() {
    const fileMeta = byId('import-file-meta');
    const statusCard = byId('import-preview-status');
    const confirmButton = byId('btn-import-confirm');
    const overwriteCheckbox = byId('import-overwrite-checkbox');

    if (overwriteCheckbox) {
        overwriteCheckbox.checked = Boolean(state.importPreview.overwriteExisting);
    }

    if (fileMeta) {
        if (!state.importPreview.fileName) {
            fileMeta.className = 'selected-summary empty';
            fileMeta.textContent = 'Chua chon file import.';
        } else {
            fileMeta.className = 'selected-summary';
            fileMeta.innerHTML = `
                <div class="selected-summary-grid">
                    <div>
                        <span class="summary-label">File da chon</span>
                        <strong class="summary-value">${escapeHtml(state.importPreview.fileName)}</strong>
                        <p class="summary-note">${state.importPreview.file ? `${escapeHtml((state.importPreview.file.size / 1024).toFixed(1))} KB` : 'Dang cho preview moi.'}</p>
                    </div>
                    <div>
                        <span class="summary-label">Overwrite mode</span>
                        <strong class="summary-value">${state.importPreview.overwriteExisting ? 'Da bat' : 'Dang tat'}</strong>
                        <p class="summary-note">Neu tat, duplicate <code>(match_id, game_number)</code> se vao danh sach loi va khong cho confirm.</p>
                    </div>
                </div>
            `;
        }
    }

    if (statusCard) {
        const summary = state.importPreview.summary;
        if (!state.importPreview.previewToken) {
            statusCard.className = 'selected-summary empty';
            statusCard.textContent = 'Preview chua duoc tao. Chon file, bam Preview Import, kiem tra loi roi moi Confirm.';
        } else if (state.importPreview.readyToImport) {
            statusCard.className = 'selected-summary';
            statusCard.innerHTML = `
                <div class="selected-summary-grid">
                    <div>
                        <span class="summary-label">Preview status</span>
                        <strong class="summary-value">San sang confirm import</strong>
                        <p class="summary-note">${escapeHtml(`Token: ${state.importPreview.previewToken}`)}</p>
                    </div>
                    <div>
                        <span class="summary-label">Summary</span>
                        <strong class="summary-value">${escapeHtml(`${summary?.validRows || 0}/${summary?.totalRows || 0} dong hop le`)}</strong>
                        <p class="summary-note">Warnings khong chan import, nhung can kiem tra ky truoc khi commit DB.</p>
                    </div>
                </div>
            `;
        } else {
            statusCard.className = 'selected-summary warning';
            statusCard.innerHTML = `
                <div class="selected-summary-grid">
                    <div>
                        <span class="summary-label">Preview status</span>
                        <strong class="summary-value">Preview dang co loi</strong>
                        <p class="summary-note">${escapeHtml(`Token: ${state.importPreview.previewToken}`)}</p>
                    </div>
                    <div>
                        <span class="summary-label">Summary</span>
                        <strong class="summary-value">${escapeHtml(`${summary?.errorRows || 0} dong loi`)}</strong>
                        <p class="summary-note">Hay sua file hoac bat overwrite ro rang roi preview lai.</p>
                    </div>
                </div>
            `;
        }
    }

    if (confirmButton) {
        confirmButton.disabled = !state.importPreview.readyToImport;
    }

    renderImportPreviewSummary();
    renderImportPreviewIssueLists();
    renderImportPreviewTable();
}

function renderImportPreviewSummary() {
    const host = byId('import-preview-summary');
    const summary = state.importPreview.summary;
    if (!host) return;

    if (!summary) {
        host.innerHTML = `
            <article class="summary-card">
                <span class="summary-label">Preview</span>
                <strong class="summary-value">0 dong</strong>
                <p class="summary-note">Upload file de xem thong ke import.</p>
            </article>
        `;
        return;
    }

    host.innerHTML = `
        <article class="summary-card">
            <span class="summary-label">Tong dong</span>
            <strong class="summary-value">${summary.totalRows}</strong>
            <p class="summary-note">So dong du lieu da doc tu file.</p>
        </article>
        <article class="summary-card">
            <span class="summary-label">Hop le / Loi</span>
            <strong class="summary-value">${summary.validRows} / ${summary.errorRows}</strong>
            <p class="summary-note">Chi duoc Confirm khi errorRows = 0.</p>
        </article>
        <article class="summary-card">
            <span class="summary-label">Match create / update</span>
            <strong class="summary-value">${summary.matchesToCreate} / ${summary.matchesToUpdate}</strong>
            <p class="summary-note">Match update co the la gan tournament hoac dong bo ty so series.</p>
        </article>
        <article class="summary-card">
            <span class="summary-label">Draft create / overwrite</span>
            <strong class="summary-value">${summary.draftsToCreate} / ${summary.draftsToOverwrite}</strong>
            <p class="summary-note">Overwrite chi hop le khi admin da bat lua chon overwrite ro rang.</p>
        </article>
    `;
}

function renderImportPreviewIssueLists() {
    const errorsHost = byId('import-preview-errors-list');
    const warningsHost = byId('import-preview-warnings-list');
    const rowErrors = state.importPreview.rows
        .filter(row => Array.isArray(row.errors) && row.errors.length)
        .map(row => `Dong ${row.rowNumber}: ${row.errors.join(' | ')}`);
    const rowWarnings = state.importPreview.rows
        .filter(row => Array.isArray(row.warnings) && row.warnings.length)
        .map(row => `Dong ${row.rowNumber}: ${row.warnings.join(' | ')}`);
    const allErrors = (state.importPreview.errors || []).concat(rowErrors);
    const allWarnings = (state.importPreview.warnings || []).concat(rowWarnings);

    if (errorsHost) {
        if (!allErrors.length) {
            errorsHost.innerHTML = `
                <div class="verify-item">
                    <span class="verify-item-label">Status</span>
                    <span class="verify-item-value">Chua co loi nao de hien thi.</span>
                </div>
            `;
        } else {
            errorsHost.innerHTML = allErrors.map((message, index) => `
                <div class="verify-item full">
                    <span class="verify-item-label">Error ${index + 1}</span>
                    <span class="verify-item-value">${escapeHtml(message)}</span>
                </div>
            `).join('');
        }
    }

    if (warningsHost) {
        if (!allWarnings.length) {
            warningsHost.innerHTML = `
                <div class="verify-item">
                    <span class="verify-item-label">Status</span>
                    <span class="verify-item-value">Chua co canh bao nao.</span>
                </div>
            `;
        } else {
            warningsHost.innerHTML = allWarnings.map((message, index) => `
                <div class="verify-item full">
                    <span class="verify-item-label">Warning ${index + 1}</span>
                    <span class="verify-item-value">${escapeHtml(message)}</span>
                </div>
            `).join('');
        }
    }
}

function renderImportPreviewTable() {
    const tbody = byId('import-preview-tbody');
    if (!tbody) return;

    if (!state.importPreview.rows.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="table-state">Chua co preview import.</td></tr>';
        return;
    }

    tbody.innerHTML = state.importPreview.rows.map(row => {
        const hasErrors = Array.isArray(row.errors) && row.errors.length > 0;
        const hasWarnings = Array.isArray(row.warnings) && row.warnings.length > 0;
        const statusChip = hasErrors
            ? renderStatusChip('Co loi', false)
            : renderStatusChip(hasWarnings ? 'Hop le + warning' : 'Hop le', true);
        const statusText = hasErrors
            ? row.errors.join(' | ')
            : (hasWarnings ? row.warnings.join(' | ') : 'San sang import.');

        return `
            <tr>
                <td><strong>#${escapeHtml(row.rowNumber)}</strong><div class="table-subtext">Game ${escapeHtml(row.gameNumber || 'N/A')}</div></td>
                <td>
                    <strong>${escapeHtml(row.date || 'N/A')}</strong>
                    <div class="table-subtext">${escapeHtml(row.tournament || 'N/A')}</div>
                </td>
                <td>
                    <strong>${escapeHtml(row.team1 || 'N/A')} / ${escapeHtml(row.team2 || 'N/A')}</strong>
                    <div class="table-subtext">${escapeHtml(row.blueTeam || 'N/A')} Blue - ${escapeHtml(row.redTeam || 'N/A')} Red</div>
                </td>
                <td>
                    <strong>${escapeHtml(row.winner || 'Chua co winner')}</strong>
                    <div class="table-subtext">${escapeHtml(row.durationText || 'Chua co length')}</div>
                </td>
                <td>${escapeHtml(row.matchAction || 'N/A')}</td>
                <td>${escapeHtml(row.draftAction || 'N/A')}</td>
                <td>
                    ${statusChip}
                    <div class="table-subtext">${escapeHtml(statusText)}</div>
                </td>
            </tr>
        `;
    }).join('');
}

function renderAll() {
    populateStaticSelects();
    populateFranchiseForm();
    populateTournamentForm();
    populateTournamentTeamForm();
    renderFranchises();
    renderTournaments();
    renderSelectedTournamentCard();
    renderTournamentTeams();
    populateMatchForm();
    renderMatches();
    renderSelectedMatchCard();
    renderGameDraftList();
    populateDraftFormSelects();
    populateDraftForm();
    renderSelectedDraftCard();
    renderLocalValidation();
    renderValidationSummary();
    renderImportPreview();
}

async function loadTeams() {
    state.teams = await apiFetch('/api/admin/esports/teams');
    state.teams.sort(compareTeams);
}

async function loadFranchises() {
    state.franchises = await apiFetch('/api/admin/esports/franchises');
}

async function loadTournaments() {
    state.tournaments = await apiFetch('/api/admin/esports/tournaments');
}

async function loadTournamentScopes() {
    state.tournamentScopes = await apiFetch('/api/esports/data/tournaments');
}

async function loadTournamentTeams(tournamentId) {
    state.tournamentTeams = await apiFetch(`/api/admin/esports/tournaments/${tournamentId}/teams`);
}

async function loadHeroes() {
    state.heroes = await apiFetch('/api/wiki/heroes');
    state.heroes.sort(compareHeroes);
}

async function loadMatches() {
    state.matches = await apiFetch('/api/admin/esports/matches');
}

async function loadGameDrafts(matchId) {
    state.gameDrafts = await apiFetch(`/api/admin/esports/matches/${matchId}/game-drafts`);
}

async function selectMatch(matchId, preserveDraftId) {
    state.selectedMatchId = Number(matchId);
    state.selectedGameDraftId = null;
    state.gameDrafts = [];
    renderAll();

    setPanelError('game-drafts-error', '');
    try {
        await loadGameDrafts(state.selectedMatchId);
        if (preserveDraftId && state.gameDrafts.some(draft => Number(draft.id) === Number(preserveDraftId))) {
            openDraftForm(state.gameDrafts.find(draft => Number(draft.id) === Number(preserveDraftId)));
        } else {
            resetDraftForm();
        }
        renderAll();
    } catch (error) {
        state.gameDrafts = [];
        resetDraftForm();
        renderAll();
        setPanelError('game-drafts-error', error.message);
    }
}

function selectGameDraft(gameDraftId) {
    const draft = state.gameDrafts.find(item => Number(item.id) === Number(gameDraftId));
    if (!draft) return;
    openDraftForm(draft);
    renderAll();
}

async function refreshAllData(options) {
    const previousMatchId = options && options.preserveMatchId ? state.selectedMatchId : null;
    const previousDraftId = options && options.preserveDraftId ? state.selectedGameDraftId : null;
    const previousTournamentId = options && options.preserveTournamentId ? state.selectedTournamentId : null;

    setPanelError('page-error', '');
    setPanelError('matches-error', '');

    try {
        await Promise.all([
            loadTeams(),
            loadHeroes(),
            loadFranchises(),
            loadTournaments(),
            loadTournamentScopes()
        ]);
        await loadMatches();

        if (previousTournamentId && state.tournaments.some(tournament => Number(tournament.id) === Number(previousTournamentId))) {
            await selectTournament(previousTournamentId);
        } else {
            state.selectedTournamentId = null;
            state.tournamentTeams = [];
            resetTournamentTeamForm();
        }

        if (previousMatchId && state.matches.some(match => Number(match.id) === Number(previousMatchId))) {
            await selectMatch(previousMatchId, previousDraftId);
        } else {
            state.selectedMatchId = null;
            state.selectedGameDraftId = null;
            state.gameDrafts = [];
            resetDraftForm();
            renderAll();
        }
    } catch (error) {
        console.error('Admin esports data load error:', error);
        setPanelError('page-error', error.message || 'Khong the tai du lieu admin esports.');
    }
}

function syncImportInputs() {
    const fileInput = byId('import-file-input');
    const overwriteCheckbox = byId('import-overwrite-checkbox');
    const file = fileInput && fileInput.files && fileInput.files.length ? fileInput.files[0] : null;
    state.importPreview.file = file;
    state.importPreview.fileName = file ? file.name : '';
    state.importPreview.overwriteExisting = Boolean(overwriteCheckbox && overwriteCheckbox.checked);
}

function clearImportPreviewResults() {
    const file = state.importPreview.file;
    const fileName = state.importPreview.fileName;
    const overwriteExisting = state.importPreview.overwriteExisting;
    state.importPreview = createDefaultImportPreviewState();
    state.importPreview.file = file;
    state.importPreview.fileName = fileName;
    state.importPreview.overwriteExisting = overwriteExisting;
    setPanelError('import-form-error', '');
    renderImportPreview();
}

function handleImportInputsChanged() {
    syncImportInputs();
    clearImportPreviewResults();
}

async function previewImportFile() {
    syncImportInputs();
    if (!state.importPreview.file) {
        setPanelError('import-form-error', 'Hay chon file Excel/CSV truoc khi preview.');
        return;
    }

    setPanelError('import-form-error', '');
    setButtonLoading('btn-import-preview', true, 'Dang preview...');
    try {
        const formData = new FormData();
        formData.append('file', state.importPreview.file);
        formData.append('overwriteExisting', state.importPreview.overwriteExisting ? 'true' : 'false');

        const response = await fetch('/api/admin/esports/game-drafts/import/preview', {
            method: 'POST',
            body: formData
        });
        const rawText = await response.text();
        if (!response.ok) {
            throw new Error(parseApiErrorMessage(rawText, response));
        }
        const payload = rawText ? JSON.parse(rawText) : null;

        state.importPreview = {
            file: state.importPreview.file,
            fileName: state.importPreview.fileName,
            overwriteExisting: state.importPreview.overwriteExisting,
            previewToken: payload && payload.previewToken ? payload.previewToken : '',
            readyToImport: Boolean(payload && payload.readyToImport),
            summary: payload && payload.summary ? payload.summary : null,
            rows: payload && Array.isArray(payload.rows) ? payload.rows : [],
            errors: payload && Array.isArray(payload.errors) ? payload.errors : [],
            warnings: payload && Array.isArray(payload.warnings) ? payload.warnings : []
        };
        renderImportPreview();
        showToast(
            state.importPreview.readyToImport
                ? 'Preview hop le. Ban co the Confirm Import.'
                : 'Preview da xong, nhung van con loi can xu ly.',
            state.importPreview.readyToImport ? 'ok' : 'err'
        );
    } catch (error) {
        setPanelError('import-form-error', error.message);
    } finally {
        setButtonLoading('btn-import-preview', false);
    }
}

async function confirmImportPreview() {
    if (!state.importPreview.previewToken || !state.importPreview.readyToImport) {
        setPanelError('import-form-error', 'Preview chua hop le de confirm import.');
        return;
    }

    const confirmed = window.confirm(`Confirm import ${state.importPreview.summary?.validRows || 0} dong hop le vao DB?`);
    if (!confirmed) return;

    setPanelError('import-form-error', '');
    setButtonLoading('btn-import-confirm', true, 'Dang import...');
    try {
        const response = await apiFetch('/api/admin/esports/game-drafts/import/confirm', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ previewToken: state.importPreview.previewToken })
        });

        const affectedMatchIds = response && Array.isArray(response.affectedMatchIds)
            ? response.affectedMatchIds.filter(Boolean)
            : [];
        resetImportPreview();
        await refreshAllData({ preserveMatchId: false, preserveDraftId: false, preserveTournamentId: true });
        if (affectedMatchIds.length) {
            await selectMatch(affectedMatchIds[0], null);
        }
        showToast(
            `Da import ${response?.importedRows || 0} dong. Match moi: ${response?.createdMatches || 0}, overwrite: ${response?.overwrittenDrafts || 0}.`,
            'ok'
        );
    } catch (error) {
        setPanelError('import-form-error', error.message);
    } finally {
        setButtonLoading('btn-import-confirm', false);
    }
}

async function submitMatchForm(event) {
    event.preventDefault();

    const payload = {
        matchDate: byId('mf-date').value,
        team1Code: normalizeTeamCode(byId('mf-team1').value),
        team2Code: normalizeTeamCode(byId('mf-team2').value),
        tournamentId: toNullableNumber(byId('mf-tournament').value),
        score1: Number(byId('mf-score1').value),
        score2: Number(byId('mf-score2').value),
        tier: byId('mf-tier').value,
        stage: normalizeStageValue(byId('mf-stage').value)
    };
    const matchId = toNullableNumber(byId('mf-id').value);

    if (!payload.matchDate || !payload.team1Code || !payload.team2Code || !payload.tier || !payload.stage) {
        setPanelError('match-form-error', 'Vui long nhap du ngay thi dau, 2 team, giai/tier va stage.');
        return;
    }
    if (payload.team1Code === payload.team2Code) {
        setPanelError('match-form-error', 'Team 1 va Team 2 khong duoc trung nhau.');
        return;
    }
    if (!findTeamByCode(payload.team1Code) || !findTeamByCode(payload.team2Code)) {
        setPanelError('match-form-error', 'Team 1 va Team 2 phai la team dang co trong DB.');
        return;
    }
    if (!Number.isFinite(payload.score1) || payload.score1 < 0 || !Number.isFinite(payload.score2) || payload.score2 < 0) {
        setPanelError('match-form-error', 'Ty so series phai la so nguyen khong am.');
        return;
    }

    setPanelError('match-form-error', '');
    setButtonLoading('btn-match-submit', true, 'Dang luu match...');
    try {
        const response = await apiFetch(matchId ? `/api/admin/esports/matches/${matchId}` : '/api/admin/esports/matches', {
            method: matchId ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        resetMatchForm();
        await refreshAllData({ preserveMatchId: true, preserveDraftId: true, preserveTournamentId: true });
        if (response && response.id) {
            await selectMatch(response.id, null);
        }
        showToast(matchId ? 'Da cap nhat match thanh cong.' : 'Da tao match moi thanh cong.', 'ok');
    } catch (error) {
        setPanelError('match-form-error', error.message);
    } finally {
        setButtonLoading('btn-match-submit', false);
    }
}

async function deleteMatch(matchId) {
    const match = state.matches.find(item => Number(item.id) === Number(matchId));
    if (!match) return;
    const confirmed = window.confirm(`Xoa match #${match.id} (${normalizeTeamCode(match.team1Code)} vs ${normalizeTeamCode(match.team2Code)})?`);
    if (!confirmed) return;

    try {
        await apiFetch(`/api/admin/esports/matches/${matchId}`, { method: 'DELETE' });
        if (Number(state.selectedMatchId) === Number(matchId)) {
            state.selectedMatchId = null;
            state.selectedGameDraftId = null;
            state.gameDrafts = [];
            resetDraftForm();
        }
        await refreshAllData({ preserveMatchId: true, preserveDraftId: true, preserveTournamentId: true });
        showToast(`Da xoa match #${matchId}.`, 'ok');
    } catch (error) {
        setPanelError('matches-error', error.message);
    }
}

async function submitFranchiseForm(event) {
    event.preventDefault();
    const franchiseId = toNullableNumber(byId('ff-id').value);
    const payload = {
        code: byId('ff-code').value.trim(),
        name: byId('ff-name').value.trim(),
        tierLevel: byId('ff-tier-level').value.trim(),
        region: byId('ff-region').value.trim(),
        displayOrder: Number(byId('ff-display-order').value || 0),
        active: byId('ff-active').checked,
        logoUrl: byId('ff-logo-url').value.trim(),
        description: byId('ff-description').value.trim()
    };

    if (!payload.code || !payload.name || !payload.tierLevel) {
        setPanelError('franchise-form-error', 'Code, name va tier level la bat buoc.');
        return;
    }

    setPanelError('franchise-form-error', '');
    setButtonLoading('btn-franchise-submit', true, 'Dang luu franchise...');
    try {
        await apiFetch(franchiseId ? `/api/admin/esports/franchises/${franchiseId}` : '/api/admin/esports/franchises', {
            method: franchiseId ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        resetFranchiseForm();
        await refreshAllData({ preserveMatchId: true, preserveDraftId: true, preserveTournamentId: true });
        showToast(franchiseId ? 'Da cap nhat franchise.' : 'Da tao franchise moi.', 'ok');
    } catch (error) {
        setPanelError('franchise-form-error', error.message);
    } finally {
        setButtonLoading('btn-franchise-submit', false);
    }
}

async function deleteFranchise(franchiseId) {
    const franchise = state.franchises.find(item => Number(item.id) === Number(franchiseId));
    if (!franchise) return;
    const confirmed = window.confirm(`Deactivate franchise ${franchise.code} - ${franchise.name}?`);
    if (!confirmed) return;

    try {
        await apiFetch(`/api/admin/esports/franchises/${franchiseId}`, { method: 'DELETE' });
        await refreshAllData({ preserveMatchId: true, preserveDraftId: true, preserveTournamentId: true });
        showToast(`Da deactivate franchise ${franchise.code}.`, 'ok');
    } catch (error) {
        setPanelError('franchise-form-error', error.message);
    }
}

async function submitTournamentForm(event) {
    event.preventDefault();
    const tournamentId = toNullableNumber(byId('tf-id').value);
    const payload = {
        franchiseId: toNullableNumber(byId('tf-franchise').value),
        name: byId('tf-name').value.trim(),
        slug: byId('tf-slug').value.trim(),
        seasonYear: toNullableNumber(byId('tf-season-year').value),
        splitName: byId('tf-split-name').value.trim(),
        tierLevel: byId('tf-tier-level').value.trim(),
        aerTier: toNullableNumber(byId('tf-aer-tier').value),
        status: byId('tf-status').value.trim(),
        startDate: byId('tf-start-date').value || null,
        endDate: byId('tf-end-date').value || null,
        logoUrl: byId('tf-logo-url').value.trim(),
        description: byId('tf-description').value.trim()
    };

    if (!payload.franchiseId || !payload.name || !payload.slug) {
        setPanelError('tournament-form-error', 'Franchise, name va slug la bat buoc.');
        return;
    }
    if (payload.aerTier == null || payload.aerTier <= 0) {
        setPanelError('tournament-form-error', 'AER Tier phai la so nguyen duong.');
        return;
    }

    setPanelError('tournament-form-error', '');
    setButtonLoading('btn-tournament-submit', true, 'Dang luu tournament...');
    try {
        const response = await apiFetch(tournamentId ? `/api/admin/esports/tournaments/${tournamentId}` : '/api/admin/esports/tournaments', {
            method: tournamentId ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        resetTournamentForm();
        await refreshAllData({ preserveMatchId: true, preserveDraftId: true, preserveTournamentId: true });
        if (response && response.id) {
            await selectTournament(response.id);
        }
        showToast(tournamentId ? 'Da cap nhat tournament.' : 'Da tao tournament moi.', 'ok');
    } catch (error) {
        setPanelError('tournament-form-error', error.message);
    } finally {
        setButtonLoading('btn-tournament-submit', false);
    }
}

async function deleteTournament(tournamentId) {
    const tournament = state.tournaments.find(item => Number(item.id) === Number(tournamentId));
    if (!tournament) return;
    const confirmed = window.confirm(`Xoa tournament ${tournament.name}?`);
    if (!confirmed) return;

    try {
        await apiFetch(`/api/admin/esports/tournaments/${tournamentId}`, { method: 'DELETE' });
        if (Number(state.selectedTournamentId) === Number(tournamentId)) {
            state.selectedTournamentId = null;
            state.tournamentTeams = [];
        }
        await refreshAllData({ preserveMatchId: true, preserveDraftId: true, preserveTournamentId: true });
        showToast(`Da xoa tournament ${tournament.name}.`, 'ok');
    } catch (error) {
        setPanelError('tournament-form-error', error.message);
    }
}

async function submitTournamentTeamForm(event) {
    event.preventDefault();
    if (!state.selectedTournamentId) {
        setPanelError('tournament-teams-error', 'Hay chon tournament truoc khi add team.');
        return;
    }

    const payload = {
        teamId: toNullableNumber(byId('ttf-team').value),
        groupName: byId('ttf-group-name').value.trim(),
        seedNumber: toNullableNumber(byId('ttf-seed-number').value),
        status: byId('ttf-status').value.trim(),
        note: byId('ttf-note').value.trim()
    };

    if (!payload.teamId) {
        setPanelError('tournament-teams-error', 'teamId la bat buoc.');
        return;
    }

    setPanelError('tournament-teams-error', '');
    setButtonLoading('btn-tournament-team-submit', true, 'Dang add team...');
    try {
        await apiFetch(`/api/admin/esports/tournaments/${state.selectedTournamentId}/teams`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        resetTournamentTeamForm();
        await selectTournament(state.selectedTournamentId);
        showToast('Da them team vao tournament.', 'ok');
    } catch (error) {
        setPanelError('tournament-teams-error', error.message);
    } finally {
        setButtonLoading('btn-tournament-team-submit', false);
    }
}

async function removeTournamentTeam(teamId) {
    const relation = state.tournamentTeams.find(item => Number(item.teamId) === Number(teamId));
    if (!relation || !state.selectedTournamentId) return;
    const confirmed = window.confirm(`Remove ${relation.teamName || relation.teamCode} khoi tournament hien tai?`);
    if (!confirmed) return;

    try {
        await apiFetch(`/api/admin/esports/tournaments/${state.selectedTournamentId}/teams/${teamId}`, { method: 'DELETE' });
        await selectTournament(state.selectedTournamentId);
        showToast('Da xoa team khoi tournament.', 'ok');
    } catch (error) {
        setPanelError('tournament-teams-error', error.message);
    }
}

function buildDraftPayload() {
    const durationResult = parseDurationInput(state.draftForm.duration);
    const validation = buildLocalValidation();
    return {
        errors: validation.issues,
        payload: {
            gameNumber: Number(state.draftForm.gameNumber),
            blueTeamId: toNullableNumber(state.draftForm.blueTeamId),
            redTeamId: toNullableNumber(state.draftForm.redTeamId),
            winnerTeamId: toNullableNumber(state.draftForm.winnerTeamId),
            durationSeconds: durationResult.value,
            draftFormatCode: String(state.draftForm.draftFormatCode || 'AOV_STANDARD_18').trim() || 'AOV_STANDARD_18',
            source: String(state.draftForm.source || 'manual').trim() || 'manual',
            blueBans: state.draftForm.blueBans.map(toNullableNumber),
            redBans: state.draftForm.redBans.map(toNullableNumber),
            blueLineup: {
                dsl: toNullableNumber(state.draftForm.blueLineup.DSL),
                jgl: toNullableNumber(state.draftForm.blueLineup.JGL),
                mid: toNullableNumber(state.draftForm.blueLineup.MID),
                adl: toNullableNumber(state.draftForm.blueLineup.ADL),
                sup: toNullableNumber(state.draftForm.blueLineup.SUP)
            },
            redLineup: {
                dsl: toNullableNumber(state.draftForm.redLineup.DSL),
                jgl: toNullableNumber(state.draftForm.redLineup.JGL),
                mid: toNullableNumber(state.draftForm.redLineup.MID),
                adl: toNullableNumber(state.draftForm.redLineup.ADL),
                sup: toNullableNumber(state.draftForm.redLineup.SUP)
            }
        }
    };
}

async function submitDraftForm(event) {
    event.preventDefault();

    if (!state.selectedMatchId) {
        setPanelError('draft-form-error', 'Hay chon match truoc khi tao game draft record.');
        return;
    }

    const { errors, payload } = buildDraftPayload();
    if (errors.length) {
        setPanelError('draft-form-error', errors.join(' '));
        return;
    }

    const draftId = toNullableNumber(state.draftForm.id);
    setPanelError('draft-form-error', '');
    setButtonLoading('btn-draft-submit', true, 'Dang luu game draft...');
    try {
        const response = await apiFetch(
            draftId
                ? `/api/admin/esports/game-drafts/${draftId}`
                : `/api/admin/esports/matches/${state.selectedMatchId}/game-drafts`,
            {
                method: draftId ? 'PUT' : 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            }
        );
        await selectMatch(state.selectedMatchId, response && response.id ? response.id : null);
        showToast(draftId ? 'Da cap nhat game draft record.' : 'Da tao game draft record moi.', 'ok');
    } catch (error) {
        setPanelError('draft-form-error', error.message);
    } finally {
        setButtonLoading('btn-draft-submit', false);
    }
}

async function deleteGameDraft(gameDraftId) {
    const draft = state.gameDrafts.find(item => Number(item.id) === Number(gameDraftId));
    if (!draft) return;
    const confirmed = window.confirm(`Xoa game draft #${draft.id} / Game ${draft.gameNumber}?`);
    if (!confirmed) return;

    try {
        await apiFetch(`/api/admin/esports/game-drafts/${gameDraftId}`, { method: 'DELETE' });
        if (Number(state.selectedGameDraftId) === Number(gameDraftId)) {
            resetDraftForm();
        }
        await selectMatch(state.selectedMatchId, null);
        showToast(`Da xoa game draft #${gameDraftId}.`, 'ok');
    } catch (error) {
        setPanelError('game-drafts-error', error.message);
    }
}

function handleDraftFieldSync() {
    state.draftForm.id = toNullableNumber(byId('df-id').value);
    state.draftForm.gameNumber = byId('df-game-number').value === '' ? '' : Number(byId('df-game-number').value);
    state.draftForm.blueTeamId = byId('df-blue-team').value;
    state.draftForm.redTeamId = byId('df-red-team').value;
    state.draftForm.winnerTeamId = byId('df-winner-team').value;
    state.draftForm.duration = byId('df-duration').value.trim();
    state.draftForm.draftFormatCode = byId('df-draft-format-code').value.trim();
    state.draftForm.source = byId('df-source').value.trim();
    for (let index = 0; index < 5; index += 1) {
        state.draftForm.blueBans[index] = toNullableNumber(byId(`blue-ban-${index + 1}`).value);
        state.draftForm.redBans[index] = toNullableNumber(byId(`red-ban-${index + 1}`).value);
    }
    LANE_ROLES.forEach(role => {
        state.draftForm.blueLineup[role] = toNullableNumber(byId(`blue-${role.toLowerCase()}`).value);
        state.draftForm.redLineup[role] = toNullableNumber(byId(`red-${role.toLowerCase()}`).value);
    });
    renderSelectedDraftCard();
    renderLocalValidation();
}

function bindEvents() {
    byId('btn-refresh-admin-esports')?.addEventListener('click', () => refreshAllData({ preserveMatchId: true, preserveDraftId: true, preserveTournamentId: true }));
    byId('btn-export-admin-esports')?.addEventListener('click', downloadGameDraftsCsv);
    byId('btn-add-draft')?.addEventListener('click', () => {
        if (!state.selectedMatchId) {
            showToast('Hay chon match truoc khi them van.', 'err');
            return;
        }
        resetDraftForm();
        byId('draft-form')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
    byId('import-file-input')?.addEventListener('change', handleImportInputsChanged);
    byId('import-overwrite-checkbox')?.addEventListener('change', handleImportInputsChanged);
    byId('btn-import-preview')?.addEventListener('click', previewImportFile);
    byId('btn-import-confirm')?.addEventListener('click', confirmImportPreview);
    byId('btn-import-reset')?.addEventListener('click', () => resetImportPreview());

    byId('btn-franchise-reset')?.addEventListener('click', resetFranchiseForm);
    byId('franchise-form')?.addEventListener('submit', submitFranchiseForm);
    byId('franchises-tbody')?.addEventListener('click', async event => {
        const button = event.target.closest('[data-action][data-id]');
        if (!button) return;
        const action = button.dataset.action;
        const id = Number(button.dataset.id);
        const franchise = state.franchises.find(item => Number(item.id) === id);
        if (!franchise) return;
        if (action === 'edit-franchise') {
            openFranchiseForm(franchise);
        } else if (action === 'delete-franchise') {
            await deleteFranchise(id);
        }
    });

    byId('btn-tournament-reset')?.addEventListener('click', resetTournamentForm);
    byId('btn-refresh-tournament-catalog')?.addEventListener('click', () => refreshAllData({ preserveMatchId: true, preserveDraftId: true, preserveTournamentId: true }));
    byId('tournament-form')?.addEventListener('submit', submitTournamentForm);
    byId('tournament-filter-franchise')?.addEventListener('change', () => {
        syncTournamentFilterFromDom();
        renderTournaments();
    });
    byId('tournaments-tbody')?.addEventListener('click', async event => {
        const button = event.target.closest('[data-action][data-id]');
        if (!button) return;
        const action = button.dataset.action;
        const id = Number(button.dataset.id);
        const tournament = state.tournaments.find(item => Number(item.id) === id);
        if (!tournament) return;
        if (action === 'select-tournament') {
            await selectTournament(id);
        } else if (action === 'edit-tournament') {
            openTournamentForm(tournament);
        } else if (action === 'delete-tournament') {
            await deleteTournament(id);
        }
    });

    byId('tournament-team-selector')?.addEventListener('change', async event => {
        await selectTournament(toNullableNumber(event.target.value));
    });
    byId('btn-tournament-team-reset')?.addEventListener('click', resetTournamentTeamForm);
    byId('tournament-team-form')?.addEventListener('submit', submitTournamentTeamForm);
    byId('tournament-teams-tbody')?.addEventListener('click', async event => {
        const button = event.target.closest('[data-action][data-id]');
        if (!button) return;
        if (button.dataset.action === 'remove-tournament-team') {
            await removeTournamentTeam(Number(button.dataset.id));
        }
    });

    ['match-filter-search', 'match-filter-tier', 'match-filter-team', 'match-filter-date-from', 'match-filter-date-to']
        .forEach(id => byId(id)?.addEventListener(id === 'match-filter-search' ? 'input' : 'change', () => {
            syncMatchFiltersFromDom();
            renderMatches();
        }));
    byId('btn-match-filter-reset')?.addEventListener('click', resetMatchFilters);
    byId('mf-tournament')?.addEventListener('change', event => {
        const tournament = findTournamentById(toNullableNumber(event.target.value));
        const aerTier = resolveTournamentAerTier(tournament);
        if (aerTier != null) {
            byId('mf-tier').value = String(aerTier);
            state.matchForm.tier = String(aerTier);
        }
    });

    byId('btn-match-reset')?.addEventListener('click', resetMatchForm);
    byId('match-form')?.addEventListener('submit', submitMatchForm);

    byId('matches-tbody')?.addEventListener('click', async event => {
        const button = event.target.closest('[data-action][data-id]');
        if (!button) return;
        const action = button.dataset.action;
        const id = Number(button.dataset.id);
        const match = state.matches.find(item => Number(item.id) === id);
        if (!match) return;

        if (action === 'select-match') {
            await selectMatch(id, null);
        } else if (action === 'edit-match') {
            openMatchForm(match);
        } else if (action === 'delete-match') {
            await deleteMatch(id);
        }
    });

    byId('game-draft-list')?.addEventListener('click', async event => {
        const button = event.target.closest('[data-action][data-id]');
        if (!button) return;
        const action = button.dataset.action;
        const id = Number(button.dataset.id);

        if (action === 'select-draft') {
            selectGameDraft(id);
        } else if (action === 'delete-draft') {
            await deleteGameDraft(id);
        }
    });

    byId('btn-draft-reset')?.addEventListener('click', resetDraftForm);
    byId('draft-form')?.addEventListener('submit', submitDraftForm);

    ['df-game-number', 'df-blue-team', 'df-red-team', 'df-winner-team', 'df-duration', 'df-draft-format-code', 'df-source']
        .concat(HERO_SELECT_IDS)
        .forEach(id => byId(id)?.addEventListener('change', handleDraftFieldSync));
    byId('df-duration')?.addEventListener('input', handleDraftFieldSync);
}

function initAdminEsportsDataPage() {
    if (!document.querySelector('[data-page="admin-esports-data"]')) return;

    updateClock();
    window.setInterval(updateClock, 1000);
    bindEvents();
    resetFranchiseForm();
    resetTournamentForm();
    resetTournamentTeamForm();
    resetMatchForm();
    resetDraftForm();
    resetImportPreview();
    renderAll();
    refreshAllData({ preserveMatchId: false, preserveDraftId: false, preserveTournamentId: false });
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initAdminEsportsDataPage);
} else {
    initAdminEsportsDataPage();
}
