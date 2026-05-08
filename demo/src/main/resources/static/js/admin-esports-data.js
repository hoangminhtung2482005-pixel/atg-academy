const DEFAULT_DRAFT_FORMAT_NAME = 'AOV Standard 18 Phase';
const TOURNAMENT_OPTIONS = [
    { value: '0', label: 'AER International' },
    { value: '1', label: 'AER Pro League' },
    { value: '2', label: 'AER Challenger' }
];
const STAGE_OPTIONS = [
    { value: 'bang', label: 'Vòng bảng' },
    { value: 'playoff', label: 'Playoff' },
    { value: 'ck', label: 'Chung kết' },
    { value: 'vongloai', label: 'Vòng loại' }
];
const LANE_ROLES = [
    { role: 'DSL', position: 1, label: 'DSL' },
    { role: 'JGL', position: 2, label: 'JGL' },
    { role: 'MID', position: 3, label: 'MID' },
    { role: 'ADL', position: 4, label: 'ADL' },
    { role: 'SUP', position: 5, label: 'SUP' }
];
const HARD_PHASE_RULES = [
    { stepNumber: 1, teamSide: 'BLUE', actionType: 'BAN' },
    { stepNumber: 2, teamSide: 'RED', actionType: 'BAN' },
    { stepNumber: 3, teamSide: 'BLUE', actionType: 'BAN' },
    { stepNumber: 4, teamSide: 'RED', actionType: 'BAN' },
    { stepNumber: 5, teamSide: 'BLUE', actionType: 'PICK' },
    { stepNumber: 6, teamSide: 'RED', actionType: 'PICK' },
    { stepNumber: 7, teamSide: 'RED', actionType: 'PICK' },
    { stepNumber: 8, teamSide: 'BLUE', actionType: 'PICK' },
    { stepNumber: 9, teamSide: 'BLUE', actionType: 'PICK' },
    { stepNumber: 10, teamSide: 'RED', actionType: 'PICK' },
    { stepNumber: 11, teamSide: 'RED', actionType: 'BAN' },
    { stepNumber: 12, teamSide: 'BLUE', actionType: 'BAN' },
    { stepNumber: 13, teamSide: 'RED', actionType: 'BAN' },
    { stepNumber: 14, teamSide: 'BLUE', actionType: 'BAN' },
    { stepNumber: 15, teamSide: 'RED', actionType: 'PICK' },
    { stepNumber: 16, teamSide: 'BLUE', actionType: 'PICK' },
    { stepNumber: 17, teamSide: 'BLUE', actionType: 'PICK' },
    { stepNumber: 18, teamSide: 'RED', actionType: 'PICK' }
];

const state = {
    teams: [],
    heroes: [],
    matches: [],
    games: [],
    draftActions: [],
    lineups: [],
    selectedMatchId: null,
    selectedGameId: null,
    matchFilters: {
        search: '',
        tournamentTier: '',
        teamCode: '',
        dateFrom: '',
        dateTo: ''
    },
    matchForm: createDefaultMatchForm(),
    gameForm: createEmptyGameForm(),
    draftSelections: createEmptyDraftSelections(),
    draftSearchTerms: {},
    lineupSelections: createEmptyLineupSelections()
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

function normalizeAssetUrl(url) {
    if (!url) return '';
    if (/^(https?:)?\/\//i.test(url) || url.startsWith('data:') || url.startsWith('/')) {
        return url;
    }
    return '/' + url.replace(/^\.?\//, '');
}

function normalizeTeamCode(code) {
    return String(code == null ? '' : code).trim().toUpperCase();
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

function createDefaultMatchForm() {
    return {
        id: null,
        matchDate: toDateTimeLocalInputValue(new Date()),
        team1Code: '',
        team2Code: '',
        score1: 3,
        score2: 1,
        tier: '1',
        stage: 'bang'
    };
}

function createEmptyGameForm() {
    return {
        id: null,
        gameNumber: '',
        blueTeamId: '',
        redTeamId: '',
        winnerTeamId: '',
        durationSeconds: '',
        draftFormatId: ''
    };
}

function createEmptyDraftSelections() {
    const selections = {};
    HARD_PHASE_RULES.forEach(rule => {
        selections[rule.stepNumber] = null;
    });
    return selections;
}

function createEmptyLineupSelections() {
    return {
        BLUE: LANE_ROLES.reduce((result, item) => {
            result[item.role] = null;
            return result;
        }, {}),
        RED: LANE_ROLES.reduce((result, item) => {
            result[item.role] = null;
            return result;
        }, {})
    };
}

function cloneLineupSelections(selections) {
    return {
        BLUE: { ...selections.BLUE },
        RED: { ...selections.RED }
    };
}

function resetPageError() {
    setPanelError('page-error', '');
}

function setPanelError(targetId, message) {
    const element = byId(targetId);
    if (!element) return;
    if (message) {
        element.textContent = message;
        element.classList.remove('hidden');
    } else {
        element.textContent = '';
        element.classList.add('hidden');
    }
}

function setButtonLoading(buttonId, loading, loadingText) {
    const button = byId(buttonId);
    if (!button) return;
    if (loading) {
        button.dataset.originalText = button.textContent;
        button.textContent = loadingText || 'Đang xử lý...';
        button.disabled = true;
    } else {
        button.textContent = button.dataset.originalText || button.textContent;
        button.disabled = false;
    }
}

function showToast(message, type) {
    const toast = byId('toast');
    if (!toast) return;
    toast.textContent = `${type === 'ok' ? 'OK' : 'ERR'}: ${message}`;
    toast.className = `toast-box ${type === 'ok' ? 'toast-ok' : 'toast-err'}`;
    window.setTimeout(() => toast.classList.add('show'), 20);
    window.setTimeout(() => toast.classList.remove('show'), 3600);
}

function updateClock() {
    const clock = byId('header-clock');
    if (clock) {
        clock.textContent = new Date().toLocaleString('vi-VN');
    }
}

function tournamentLabel(tier) {
    const matched = TOURNAMENT_OPTIONS.find(option => option.value === String(tier == null ? '1' : tier));
    return matched ? matched.label : 'AER Pro League';
}

function stageLabel(stage) {
    const matched = STAGE_OPTIONS.find(option => option.value === String(stage || 'bang'));
    return matched ? matched.label : String(stage || 'bang');
}

function formatDateTime(value) {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return escapeHtml(value);
    return date.toLocaleString('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function formatDuration(seconds) {
    const totalSeconds = Number(seconds);
    if (!Number.isFinite(totalSeconds) || totalSeconds < 0) return '—';
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const remainSeconds = totalSeconds % 60;
    if (hours > 0) {
        return `${hours}h ${String(minutes).padStart(2, '0')}m ${String(remainSeconds).padStart(2, '0')}s`;
    }
    return `${minutes}m ${String(remainSeconds).padStart(2, '0')}s`;
}

function toDateTimeLocalInputValue(value) {
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) {
        return '';
    }
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

function parseDateBoundary(value, boundary) {
    if (!value) return null;
    const suffix = boundary === 'end' ? 'T23:59:59.999' : 'T00:00:00.000';
    const date = new Date(`${value}${suffix}`);
    return Number.isNaN(date.getTime()) ? null : date.getTime();
}

function findTeamById(id) {
    return state.teams.find(team => Number(team.id) === Number(id)) || null;
}

function findTeamByCode(code) {
    const normalizedCode = normalizeTeamCode(code);
    return state.teams.find(team => normalizeTeamCode(team.teamCode) === normalizedCode) || null;
}

function displayTeamName(team) {
    if (!team) return '—';
    return team.teamName && String(team.teamName).trim() ? team.teamName : team.teamCode;
}

function displayTeamNameByCode(code) {
    const team = findTeamByCode(code);
    return team ? displayTeamName(team) : normalizeTeamCode(code);
}

function heroById(id) {
    return state.heroes.find(hero => Number(hero.id) === Number(id)) || null;
}

function getSelectedMatch() {
    return state.matches.find(match => Number(match.id) === Number(state.selectedMatchId)) || null;
}

function getSelectedGame() {
    return state.games.find(game => Number(game.id) === Number(state.selectedGameId)) || null;
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
    return String(left.name || '').localeCompare(String(right.name || ''), 'vi', { sensitivity: 'base' });
}

function renderHeroInline(hero) {
    if (!hero) {
        return '<span class="empty-muted">Chưa chọn hero</span>';
    }

    const avatar = normalizeAssetUrl(hero.avatarUrl);
    const avatarMarkup = avatar
        ? `<img src="${escapeHtml(avatar)}" alt="${escapeHtml(hero.name || hero.slug || 'Hero')}" class="mini-avatar">`
        : '<span class="mini-avatar mini-avatar-fallback">H</span>';

    return `<span class="hero-inline">${avatarMarkup}<span>${escapeHtml(hero.name || hero.slug || 'Hero')}</span></span>`;
}

function renderTeamInline(team) {
    if (!team) {
        return '<span class="empty-muted">Thiếu team</span>';
    }

    const logo = normalizeAssetUrl(team.logoUrl);
    const logoMarkup = logo
        ? `<img src="${escapeHtml(logo)}" alt="${escapeHtml(displayTeamName(team))}" class="mini-avatar">`
        : '<span class="mini-avatar mini-avatar-fallback">T</span>';

    return `<span class="hero-inline">${logoMarkup}<span>${escapeHtml(displayTeamName(team))}</span></span>`;
}

function buildTournamentOptions(includeAll) {
    const options = includeAll ? [{ value: '', label: 'Tất cả giải / tier' }] : [];
    return options.concat(TOURNAMENT_OPTIONS);
}

function renderSelectOptions(targetId, options, selectedValue) {
    const select = byId(targetId);
    if (!select) return;
    select.innerHTML = options.map(option => {
        const value = String(option.value == null ? '' : option.value);
        const selected = String(selectedValue == null ? '' : selectedValue) === value ? ' selected' : '';
        return `<option value="${escapeHtml(value)}"${selected}>${escapeHtml(option.label)}</option>`;
    }).join('');
}

function populateStaticSelects() {
    renderSelectOptions('match-filter-tier', buildTournamentOptions(true), state.matchFilters.tournamentTier);
    renderSelectOptions('mf-tier', buildTournamentOptions(false), state.matchForm.tier);
    renderSelectOptions('mf-stage', STAGE_OPTIONS, state.matchForm.stage);
}

function populateTeamSelects() {
    const sortedTeams = state.teams.slice().sort(compareTeams);
    const filterOptions = [{ value: '', label: 'Tất cả team' }].concat(sortedTeams.map(team => ({
        value: normalizeTeamCode(team.teamCode),
        label: `${displayTeamName(team)} (${normalizeTeamCode(team.teamCode)})`
    })));
    renderSelectOptions('match-filter-team', filterOptions, state.matchFilters.teamCode);

    const formOptions = [{ value: '', label: 'Chọn team' }].concat(sortedTeams.map(team => ({
        value: normalizeTeamCode(team.teamCode),
        label: `${displayTeamName(team)} (${normalizeTeamCode(team.teamCode)})`
    })));
    renderSelectOptions('mf-team1', formOptions, state.matchForm.team1Code);
    renderSelectOptions('mf-team2', formOptions, state.matchForm.team2Code);
}

function populateMatchForm() {
    byId('mf-id').value = state.matchForm.id || '';
    byId('mf-date').value = state.matchForm.matchDate || '';
    byId('mf-score1').value = state.matchForm.score1;
    byId('mf-score2').value = state.matchForm.score2;
    populateStaticSelects();
    populateTeamSelects();
}

function syncMatchFormFromDom() {
    state.matchForm.id = toNullableNumber(byId('mf-id').value);
    state.matchForm.matchDate = byId('mf-date').value;
    state.matchForm.team1Code = normalizeTeamCode(byId('mf-team1').value);
    state.matchForm.team2Code = normalizeTeamCode(byId('mf-team2').value);
    state.matchForm.score1 = byId('mf-score1').value === '' ? '' : Number(byId('mf-score1').value);
    state.matchForm.score2 = byId('mf-score2').value === '' ? '' : Number(byId('mf-score2').value);
    state.matchForm.tier = byId('mf-tier').value;
    state.matchForm.stage = byId('mf-stage').value;
}

function resetMatchForm() {
    state.matchForm = createDefaultMatchForm();
    populateMatchForm();
    setPanelError('match-form-error', '');
    byId('match-form-title').textContent = 'Tạo match mới';
    byId('match-form-subtitle').textContent = 'Dùng API admin hiện có để tạo hoặc sửa một series.';
    byId('btn-match-submit').textContent = 'Lưu match';
}

function openMatchForm(match) {
    state.matchForm = {
        id: match.id,
        matchDate: toDateTimeLocalInputValue(match.matchDate),
        team1Code: normalizeTeamCode(match.team1Code),
        team2Code: normalizeTeamCode(match.team2Code),
        score1: Number(match.score1 == null ? 0 : match.score1),
        score2: Number(match.score2 == null ? 0 : match.score2),
        tier: String(match.tier == null ? '1' : match.tier),
        stage: String(match.stage || 'bang')
    };
    populateMatchForm();
    setPanelError('match-form-error', '');
    byId('match-form-title').textContent = `Sửa match #${match.id}`;
    byId('match-form-subtitle').textContent = 'Cập nhật lại series hiện có mà không ảnh hưởng flow draft/game riêng.';
    byId('btn-match-submit').textContent = 'Lưu chỉnh sửa';
}

function getNextGameNumber() {
    if (!state.games.length) return 1;
    return Math.max(...state.games.map(game => Number(game.gameNumber) || 0)) + 1;
}

function resetGameForm() {
    const selectedMatch = getSelectedMatch();
    const selectedGame = getSelectedGame();
    const matchTeams = getSelectedMatchTeams();
    const defaultBlueTeamId = matchTeams[0] ? Number(matchTeams[0].id) : '';
    const defaultRedTeamId = matchTeams[1] ? Number(matchTeams[1].id) : '';
    state.gameForm = {
        id: null,
        gameNumber: selectedGame ? selectedGame.gameNumber : getNextGameNumber(),
        blueTeamId: defaultBlueTeamId,
        redTeamId: defaultRedTeamId,
        winnerTeamId: '',
        durationSeconds: '',
        draftFormatId: ''
    };
    populateGameForm();
    setPanelError('game-form-error', '');
    byId('game-form-title').textContent = 'Thêm game mới';
    byId('game-form-subtitle').textContent = selectedMatch
        ? 'Game mới sẽ mặc định dùng AOV Standard 18 Phase.'
        : 'Chọn match trước khi thêm game.';
    byId('btn-game-submit').textContent = 'Lưu game';
}

function populateGameTeamOptions() {
    const selectedMatch = getSelectedMatch();
    const matchTeams = getSelectedMatchTeams().sort(compareTeams);
    const missingTeams = selectedMatch && matchTeams.length < 2;
    const baseOptions = [{ value: '', label: missingTeams ? 'Thiếu team record trong DB' : 'Chọn team' }];
    const teamOptions = baseOptions.concat(matchTeams.map(team => ({
        value: String(team.id),
        label: `${displayTeamName(team)} (${normalizeTeamCode(team.teamCode)})`
    })));
    const winnerOptions = [{ value: '', label: 'Chưa xác định winner' }].concat(matchTeams.map(team => ({
        value: String(team.id),
        label: `${displayTeamName(team)} (${normalizeTeamCode(team.teamCode)})`
    })));

    renderSelectOptions('gf-blue-team', teamOptions, state.gameForm.blueTeamId);
    renderSelectOptions('gf-red-team', teamOptions, state.gameForm.redTeamId);
    renderSelectOptions('gf-winner-team', winnerOptions, state.gameForm.winnerTeamId);

    byId('gf-blue-team').disabled = !selectedMatch || missingTeams;
    byId('gf-red-team').disabled = !selectedMatch || missingTeams;
    byId('gf-winner-team').disabled = !selectedMatch || missingTeams;
    byId('gf-game-number').disabled = !selectedMatch || missingTeams;
    byId('gf-duration').disabled = !selectedMatch || missingTeams;
    byId('btn-game-submit').disabled = !selectedMatch || missingTeams;
}

function populateGameForm() {
    byId('gf-id').value = state.gameForm.id || '';
    byId('gf-draft-format-id').value = state.gameForm.draftFormatId || '';
    byId('gf-game-number').value = state.gameForm.gameNumber === '' ? '' : state.gameForm.gameNumber;
    byId('gf-duration').value = state.gameForm.durationSeconds === '' ? '' : state.gameForm.durationSeconds;
    byId('gf-draft-format-name').value = DEFAULT_DRAFT_FORMAT_NAME;
    populateGameTeamOptions();
}

function openGameForm(game) {
    state.gameForm = {
        id: game.id,
        gameNumber: Number(game.gameNumber || ''),
        blueTeamId: Number(game.blueTeamId || ''),
        redTeamId: Number(game.redTeamId || ''),
        winnerTeamId: game.winnerTeamId == null ? '' : Number(game.winnerTeamId),
        durationSeconds: game.durationSeconds == null ? '' : Number(game.durationSeconds),
        draftFormatId: game.draftFormatId == null ? '' : Number(game.draftFormatId)
    };
    populateGameForm();
    setPanelError('game-form-error', '');
    byId('game-form-title').textContent = `Sửa game #${game.gameNumber}`;
    byId('game-form-subtitle').textContent = 'Giữ nguyên hard rule 18 phase; chỉ cập nhật metadata của ván đấu.';
    byId('btn-game-submit').textContent = 'Lưu chỉnh sửa';
}

function getMatchFromFilterSearchText(match) {
    return normalizeText([
        displayTeamNameByCode(match.team1Code),
        displayTeamNameByCode(match.team2Code),
        normalizeTeamCode(match.team1Code),
        normalizeTeamCode(match.team2Code),
        tournamentLabel(match.tier),
        stageLabel(match.stage)
    ].join(' '));
}

function getFilteredMatches() {
    const search = normalizeText(state.matchFilters.search);
    const dateFrom = parseDateBoundary(state.matchFilters.dateFrom, 'start');
    const dateTo = parseDateBoundary(state.matchFilters.dateTo, 'end');

    return state.matches.filter(match => {
        if (state.matchFilters.tournamentTier && String(match.tier) !== String(state.matchFilters.tournamentTier)) {
            return false;
        }

        if (state.matchFilters.teamCode) {
            const normalizedCode = normalizeTeamCode(state.matchFilters.teamCode);
            if (normalizeTeamCode(match.team1Code) !== normalizedCode
                && normalizeTeamCode(match.team2Code) !== normalizedCode) {
                return false;
            }
        }

        if (search && !getMatchFromFilterSearchText(match).includes(search)) {
            return false;
        }

        if (dateFrom != null || dateTo != null) {
            const matchDate = new Date(match.matchDate);
            if (Number.isNaN(matchDate.getTime())) {
                return false;
            }
            const timestamp = matchDate.getTime();
            if (dateFrom != null && timestamp < dateFrom) return false;
            if (dateTo != null && timestamp > dateTo) return false;
        }

        return true;
    });
}

function renderSelectionSummary() {
    const match = getSelectedMatch();
    const game = getSelectedGame();
    const value = byId('selection-summary-value');
    const note = byId('selection-summary-note');
    if (!value || !note) return;

    if (!match) {
        value.textContent = 'Chưa chọn match';
        note.textContent = 'Chọn một series để mở game, draft và lineup.';
        return;
    }

    if (!game) {
        value.textContent = `Match #${match.id}`;
        note.textContent = `${displayTeamNameByCode(match.team1Code)} vs ${displayTeamNameByCode(match.team2Code)} • chọn game để nhập draft.`;
        return;
    }

    value.textContent = `Match #${match.id} • Game #${game.gameNumber}`;
    note.textContent = `${displayTeamNameByCode(match.team1Code)} vs ${displayTeamNameByCode(match.team2Code)} • ${tournamentLabel(match.tier)}.`;
}

function renderMatches() {
    const tbody = byId('matches-tbody');
    const matches = getFilteredMatches();
    byId('match-count-pill').textContent = `${matches.length} match`;

    if (!matches.length) {
        tbody.innerHTML = '<tr><td colspan="6" class="table-state">Không tìm thấy match nào theo bộ lọc hiện tại.</td></tr>';
        return;
    }

    tbody.innerHTML = matches.map(match => {
        const selected = Number(match.id) === Number(state.selectedMatchId);
        const rowClass = selected ? 'row-selected' : '';
        return `<tr class="${rowClass}">
            <td>${escapeHtml(formatDateTime(match.matchDate))}</td>
            <td>
                <div class="match-teams-block">
                    <strong>${escapeHtml(displayTeamNameByCode(match.team1Code))}</strong>
                    <span class="match-vs">vs</span>
                    <strong>${escapeHtml(displayTeamNameByCode(match.team2Code))}</strong>
                </div>
                <div class="table-subtext">${escapeHtml(normalizeTeamCode(match.team1Code))} vs ${escapeHtml(normalizeTeamCode(match.team2Code))}</div>
            </td>
            <td><span class="chip">${escapeHtml(tournamentLabel(match.tier))}</span></td>
            <td><span class="chip">${escapeHtml(stageLabel(match.stage))}</span></td>
            <td><span class="score-chip">${escapeHtml(`${match.score1} - ${match.score2}`)}</span></td>
            <td>
                <div class="row-actions wrap">
                    <button type="button" class="btn btn-light btn-small" data-action="select-match" data-id="${match.id}">${selected ? 'Đang chọn' : 'Chọn'}</button>
                    <button type="button" class="btn btn-light btn-small" data-action="edit-match" data-id="${match.id}">Sửa</button>
                    <button type="button" class="btn btn-danger btn-small" data-action="delete-match" data-id="${match.id}">Xóa</button>
                </div>
            </td>
        </tr>`;
    }).join('');
}

function renderSelectedMatchCard() {
    const selectedMatch = getSelectedMatch();
    const card = byId('selected-match-card');
    if (!card) return;

    if (!selectedMatch) {
        card.className = 'selected-summary empty';
        card.textContent = 'Chưa chọn match để quản lý game.';
        return;
    }

    const teams = getSelectedMatchTeams();
    const missingTeams = teams.length < 2;
    card.className = 'selected-summary';
    card.innerHTML = `
        <div class="selected-summary-grid">
            <div>
                <span class="summary-label">Series đang chọn</span>
                <strong class="summary-value">Match #${selectedMatch.id} • ${escapeHtml(displayTeamNameByCode(selectedMatch.team1Code))} vs ${escapeHtml(displayTeamNameByCode(selectedMatch.team2Code))}</strong>
                <p class="summary-note">${escapeHtml(formatDateTime(selectedMatch.matchDate))} • ${escapeHtml(tournamentLabel(selectedMatch.tier))} • ${escapeHtml(stageLabel(selectedMatch.stage))}</p>
            </div>
            <div>
                <span class="summary-label">Tỷ số series</span>
                <strong class="summary-value">${escapeHtml(`${selectedMatch.score1} - ${selectedMatch.score2}`)}</strong>
                <p class="summary-note">${missingTeams ? 'Thiếu record team trong DB, cần bổ sung để tạo game.' : 'Blue/Red chỉ được chọn trong đúng 2 team của series.'}</p>
            </div>
        </div>
    `;
}

function renderGames() {
    const tbody = byId('games-tbody');
    const selectedMatch = getSelectedMatch();
    byId('game-count-pill').textContent = `${state.games.length} game`;

    if (!selectedMatch) {
        tbody.innerHTML = '<tr><td colspan="7" class="table-state">Chọn match để tải danh sách game.</td></tr>';
        return;
    }

    if (!state.games.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="table-state">Match này chưa có game nào.</td></tr>';
        return;
    }

    tbody.innerHTML = state.games.map(game => {
        const selected = Number(game.id) === Number(state.selectedGameId);
        const rowClass = selected ? 'row-selected' : '';
        return `<tr class="${rowClass}">
            <td>
                <strong>Game ${escapeHtml(game.gameNumber)}</strong>
                <div class="table-subtext">ID ${escapeHtml(game.id)}</div>
            </td>
            <td>${renderTeamInline(findTeamById(game.blueTeamId))}</td>
            <td>${renderTeamInline(findTeamById(game.redTeamId))}</td>
            <td>${game.winnerTeamId ? renderTeamInline(findTeamById(game.winnerTeamId)) : '<span class="empty-muted">Chưa có winner</span>'}</td>
            <td>${escapeHtml(formatDuration(game.durationSeconds))}</td>
            <td><span class="chip">${escapeHtml(game.draftFormatName || DEFAULT_DRAFT_FORMAT_NAME)}</span></td>
            <td>
                <div class="row-actions wrap">
                    <button type="button" class="btn btn-light btn-small" data-action="select-game" data-id="${game.id}">${selected ? 'Đang quản lý' : 'Quản lý'}</button>
                    <button type="button" class="btn btn-light btn-small" data-action="edit-game" data-id="${game.id}">Sửa</button>
                    <button type="button" class="btn btn-danger btn-small" data-action="delete-game" data-id="${game.id}">Xóa</button>
                </div>
            </td>
        </tr>`;
    }).join('');
}

function restoreDraftSelectionsFromServer() {
    state.draftSelections = createEmptyDraftSelections();
    state.draftActions.forEach(action => {
        state.draftSelections[action.stepNumber] = action.heroId;
    });
}

function restoreLineupSelectionsFromServer() {
    state.lineupSelections = createEmptyLineupSelections();
    state.lineups.forEach(lineup => {
        const side = String(lineup.teamSide || '');
        const role = String(lineup.laneRole || '');
        if (state.lineupSelections[side] && Object.prototype.hasOwnProperty.call(state.lineupSelections[side], role)) {
            state.lineupSelections[side][role] = lineup.heroId;
        }
    });
}

function getSavedDraftActionMap() {
    const map = new Map();
    state.draftActions.forEach(action => {
        map.set(Number(action.stepNumber), action);
    });
    return map;
}

function isDraftDirty() {
    const savedMap = getSavedDraftActionMap();
    return HARD_PHASE_RULES.some(rule => {
        const savedHeroId = savedMap.get(rule.stepNumber)?.heroId || null;
        return Number(savedHeroId || 0) !== Number(state.draftSelections[rule.stepNumber] || 0);
    });
}

function isLineupDirty() {
    const savedMap = new Map();
    state.lineups.forEach(lineup => {
        savedMap.set(`${lineup.teamSide}:${lineup.laneRole}`, lineup.heroId);
    });
    return ['BLUE', 'RED'].some(side => LANE_ROLES.some(item => {
        const savedHeroId = savedMap.get(`${side}:${item.role}`) || null;
        return Number(savedHeroId || 0) !== Number(state.lineupSelections[side][item.role] || 0);
    }));
}

function countDraftSelections() {
    let total = 0;
    let bans = 0;
    let picks = 0;
    HARD_PHASE_RULES.forEach(rule => {
        if (state.draftSelections[rule.stepNumber]) {
            total++;
            if (rule.actionType === 'BAN') bans++;
            if (rule.actionType === 'PICK') picks++;
        }
    });
    return { total, bans, picks };
}

function countLineupSelections() {
    return ['BLUE', 'RED'].reduce((count, side) => {
        return count + LANE_ROLES.filter(item => state.lineupSelections[side][item.role]).length;
    }, 0);
}

function renderSelectedGameCard() {
    const selectedGame = getSelectedGame();
    const card = byId('selected-game-card');
    const draftChip = byId('draft-status-chip');
    const lineupChip = byId('lineup-status-chip');
    if (!card || !draftChip || !lineupChip) return;

    if (!selectedGame) {
        card.className = 'selected-summary empty';
        card.textContent = 'Chưa chọn game để nhập 18 phase draft.';
        draftChip.textContent = 'Chưa chọn game';
        lineupChip.textContent = 'Lineup 0 / 10';
        return;
    }

    const draftCounts = countDraftSelections();
    const lineupCount = countLineupSelections();
    card.className = 'selected-summary';
    card.innerHTML = `
        <div class="selected-summary-grid">
            <div>
                <span class="summary-label">Game đang chọn</span>
                <strong class="summary-value">Game ${escapeHtml(selectedGame.gameNumber)} • ${escapeHtml(displayTeamNameByCode(selectedGame.blueTeamCode))} vs ${escapeHtml(displayTeamNameByCode(selectedGame.redTeamCode))}</strong>
                <p class="summary-note">Blue side: ${escapeHtml(displayTeamNameByCode(selectedGame.blueTeamCode))} • Red side: ${escapeHtml(displayTeamNameByCode(selectedGame.redTeamCode))}</p>
            </div>
            <div>
                <span class="summary-label">Metadata</span>
                <strong class="summary-value">${escapeHtml(selectedGame.draftFormatName || DEFAULT_DRAFT_FORMAT_NAME)}</strong>
                <p class="summary-note">Winner: ${escapeHtml(selectedGame.winnerTeamName || 'Chưa có')} • Duration: ${escapeHtml(formatDuration(selectedGame.durationSeconds))}</p>
            </div>
        </div>
    `;

    draftChip.textContent = `Draft ${draftCounts.total} / ${HARD_PHASE_RULES.length}${isDraftDirty() ? ' • Chưa lưu' : ''}`;
    lineupChip.textContent = `Lineup ${lineupCount} / 10${isLineupDirty() ? ' • Chưa lưu' : ''}`;
}

function buildDraftHeroOptions(stepNumber) {
    const search = normalizeText(state.draftSearchTerms[stepNumber] || '');
    const selectedHeroId = Number(state.draftSelections[stepNumber] || 0);
    const usedHeroIds = new Set(HARD_PHASE_RULES
        .filter(rule => rule.stepNumber !== stepNumber)
        .map(rule => Number(state.draftSelections[rule.stepNumber] || 0))
        .filter(Boolean));

    const heroes = state.heroes
        .filter(hero => {
            if (!search) return true;
            return normalizeText(hero.name).includes(search) || normalizeText(hero.slug).includes(search);
        })
        .sort(compareHeroes)
        .slice();

    if (selectedHeroId && !heroes.some(hero => Number(hero.id) === selectedHeroId)) {
        const selectedHero = heroById(selectedHeroId);
        if (selectedHero) {
            heroes.unshift(selectedHero);
        }
    }

    return heroes.map(hero => {
        const heroId = Number(hero.id);
        const disabled = usedHeroIds.has(heroId) ? ' disabled' : '';
        const selected = heroId === selectedHeroId ? ' selected' : '';
        return `<option value="${heroId}"${selected}${disabled}>${escapeHtml(hero.name)} (${escapeHtml(hero.slug || '')})</option>`;
    }).join('');
}

function renderDraftPhases() {
    const tbody = byId('draft-phases-tbody');
    const selectedGame = getSelectedGame();
    if (!selectedGame) {
        tbody.innerHTML = '<tr><td colspan="6" class="table-state">Chọn game để mở 18 phase cố định.</td></tr>';
        return;
    }

    tbody.innerHTML = HARD_PHASE_RULES.map(rule => {
        const selectedHeroId = state.draftSelections[rule.stepNumber];
        const hero = heroById(selectedHeroId);
        const sideTeam = rule.teamSide === 'BLUE'
            ? findTeamById(selectedGame.blueTeamId)
            : findTeamById(selectedGame.redTeamId);
        const searchValue = state.draftSearchTerms[rule.stepNumber] || '';
        return `<tr>
            <td>
                <div class="phase-meta">
                    <strong>Phase ${rule.stepNumber}</strong>
                    <span class="table-subtext">${escapeHtml(displayTeamName(sideTeam))}</span>
                </div>
            </td>
            <td><span class="chip phase-side-chip ${rule.teamSide === 'BLUE' ? 'blue' : 'red'}">${escapeHtml(rule.teamSide)}</span></td>
            <td><span class="chip phase-action-chip ${rule.actionType === 'BAN' ? 'ban' : 'pick'}">${escapeHtml(rule.actionType)}</span></td>
            <td>${renderHeroInline(hero)}</td>
            <td>
                <input
                    type="search"
                    class="draft-search-input"
                    data-draft-search="${rule.stepNumber}"
                    value="${escapeHtml(searchValue)}"
                    placeholder="Tìm theo tên hoặc slug">
            </td>
            <td>
                <select class="draft-hero-select" data-draft-select="${rule.stepNumber}">
                    <option value="">Chưa chọn hero</option>
                    ${buildDraftHeroOptions(rule.stepNumber)}
                </select>
            </td>
        </tr>`;
    }).join('');
}

function pickedHeroesBySide() {
    return state.draftActions.reduce((result, action) => {
        if (String(action.actionType) !== 'PICK') {
            return result;
        }
        if (result[action.teamSide]) {
            result[action.teamSide].push(heroById(action.heroId));
        }
        return result;
    }, { BLUE: [], RED: [] });
}

function buildLineupHeroOptions(side, role) {
    const selections = state.lineupSelections;
    const currentHeroId = Number(selections[side][role] || 0);
    const usedHeroIds = new Set(
        ['BLUE', 'RED'].flatMap(teamSide => LANE_ROLES.map(item => {
            if (teamSide === side && item.role === role) {
                return null;
            }
            return Number(selections[teamSide][item.role] || 0);
        })).filter(Boolean)
    );

    return pickedHeroesBySide()[side]
        .filter(Boolean)
        .sort(compareHeroes)
        .map(hero => {
            const heroId = Number(hero.id);
            const selected = heroId === currentHeroId ? ' selected' : '';
            const disabled = usedHeroIds.has(heroId) ? ' disabled' : '';
            return `<option value="${heroId}"${selected}${disabled}>${escapeHtml(hero.name)}</option>`;
        })
        .join('');
}

function renderLineupGrid() {
    const grid = byId('lineup-grid');
    const selectedGame = getSelectedGame();
    if (!selectedGame) {
        grid.innerHTML = '<div class="selected-summary empty">Chọn game để nhập final lineup.</div>';
        return;
    }

    const picks = pickedHeroesBySide();
    grid.innerHTML = ['BLUE', 'RED'].map(side => {
        const team = side === 'BLUE'
            ? findTeamById(selectedGame.blueTeamId)
            : findTeamById(selectedGame.redTeamId);
        return `<section class="lineup-card ${side === 'BLUE' ? 'blue' : 'red'}">
            <div class="lineup-card-header">
                <div>
                    <span class="summary-label">${side} SIDE</span>
                    <strong class="summary-value">${escapeHtml(displayTeamName(team))}</strong>
                </div>
                <span class="chip">${picks[side].filter(Boolean).length} PICK khả dụng</span>
            </div>
            <div class="lineup-rows">
                ${LANE_ROLES.map(item => {
                    const selectedHero = heroById(state.lineupSelections[side][item.role]);
                    return `<label class="lineup-row">
                        <span class="lineup-role">${escapeHtml(item.label)}</span>
                        <select data-lineup-side="${side}" data-lineup-role="${item.role}">
                            <option value="">Chọn hero PICK</option>
                            ${buildLineupHeroOptions(side, item.role)}
                        </select>
                        <span class="lineup-preview">${renderHeroInline(selectedHero)}</span>
                    </label>`;
                }).join('')}
            </div>
        </section>`;
    }).join('');
}

function renderVerifySection() {
    const summaryGrid = byId('verify-summary-grid');
    const detailsGrid = byId('verify-details-grid');
    const selectedGame = getSelectedGame();
    if (!selectedGame) {
        summaryGrid.innerHTML = `
            <article class="summary-card">
                <span class="summary-label">Draft</span>
                <strong class="summary-value">0 / 18</strong>
                <p class="summary-note">Chọn game để xem tóm tắt.</p>
            </article>
        `;
        detailsGrid.innerHTML = `
            <article class="verify-card">
                <h4>Blue BAN</h4>
                <div class="verify-items"><div class="empty-muted">Chưa có dữ liệu.</div></div>
            </article>
        `;
        return;
    }

    const draftCounts = countDraftSelections();
    const lineupCount = countLineupSelections();
    const banComplete = draftCounts.bans === HARD_PHASE_RULES.filter(rule => rule.actionType === 'BAN').length;
    const pickComplete = draftCounts.picks === HARD_PHASE_RULES.filter(rule => rule.actionType === 'PICK').length;
    const lineupComplete = lineupCount === 10;

    summaryGrid.innerHTML = `
        <article class="summary-card">
            <span class="summary-label">Draft progress</span>
            <strong class="summary-value">${draftCounts.total} / 18</strong>
            <p class="summary-note">${isDraftDirty() ? 'Có thay đổi draft chưa lưu.' : 'Draft local đã đồng bộ với server.'}</p>
        </article>
        <article class="summary-card">
            <span class="summary-label">Bans</span>
            <strong class="summary-value">${draftCounts.bans} / 8</strong>
            <p class="summary-note">${banComplete ? 'Đã đủ 8 BAN.' : 'Chưa đủ 8 BAN.'}</p>
        </article>
        <article class="summary-card">
            <span class="summary-label">Picks</span>
            <strong class="summary-value">${draftCounts.picks} / 10</strong>
            <p class="summary-note">${pickComplete ? 'Đã đủ 10 PICK.' : 'Chưa đủ 10 PICK.'}</p>
        </article>
        <article class="summary-card">
            <span class="summary-label">Lineup</span>
            <strong class="summary-value">${lineupCount} / 10</strong>
            <p class="summary-note">${lineupComplete ? 'Đã đủ final lineup 10/10.' : 'Lineup chưa đủ 10/10.'}</p>
        </article>
    `;

    const groupItems = [
        { title: 'Blue BAN', rules: HARD_PHASE_RULES.filter(rule => rule.teamSide === 'BLUE' && rule.actionType === 'BAN') },
        { title: 'Red BAN', rules: HARD_PHASE_RULES.filter(rule => rule.teamSide === 'RED' && rule.actionType === 'BAN') },
        { title: 'Blue PICK', rules: HARD_PHASE_RULES.filter(rule => rule.teamSide === 'BLUE' && rule.actionType === 'PICK') },
        { title: 'Red PICK', rules: HARD_PHASE_RULES.filter(rule => rule.teamSide === 'RED' && rule.actionType === 'PICK') }
    ];

    const details = groupItems.map(group => {
        const items = group.rules.map(rule => {
            const hero = heroById(state.draftSelections[rule.stepNumber]);
            return `<div class="verify-item">
                <span class="verify-item-label">Phase ${rule.stepNumber}</span>
                <span class="verify-item-value">${hero ? renderHeroInline(hero) : '<span class="empty-muted">Chưa chọn</span>'}</span>
            </div>`;
        }).join('');
        return `<article class="verify-card">
            <h4>${escapeHtml(group.title)}</h4>
            <div class="verify-items">${items}</div>
        </article>`;
    });

    const lineupCards = ['BLUE', 'RED'].map(side => {
        const items = LANE_ROLES.map(item => {
            const hero = heroById(state.lineupSelections[side][item.role]);
            return `<div class="verify-item">
                <span class="verify-item-label">${escapeHtml(item.role)}</span>
                <span class="verify-item-value">${hero ? renderHeroInline(hero) : '<span class="empty-muted">Chưa chọn</span>'}</span>
            </div>`;
        }).join('');
        return `<article class="verify-card">
            <h4>${escapeHtml(side)} Lineup</h4>
            <div class="verify-items">${items}</div>
        </article>`;
    });

    detailsGrid.innerHTML = details.concat(lineupCards).join('');
}

function renderAll() {
    populateMatchForm();
    renderSelectionSummary();
    renderMatches();
    renderSelectedMatchCard();
    populateGameForm();
    renderGames();
    renderSelectedGameCard();
    renderDraftPhases();
    renderLineupGrid();
    renderVerifySection();
}

async function loadTeams() {
    state.teams = await apiFetch('/api/admin/esports/teams');
    state.teams.sort(compareTeams);
}

async function loadHeroes() {
    state.heroes = await apiFetch('/api/wiki/heroes');
    state.heroes.sort(compareHeroes);
}

async function loadMatches() {
    state.matches = await apiFetch('/api/admin/esports/matches');
}

async function loadGames(matchId) {
    state.games = await apiFetch(`/api/admin/esports/matches/${matchId}/games`);
}

async function loadDraftAndLineups(gameId) {
    const [draftActions, lineups] = await Promise.all([
        apiFetch(`/api/admin/esports/games/${gameId}/draft-actions`),
        apiFetch(`/api/admin/esports/games/${gameId}/lineups`)
    ]);
    state.draftActions = Array.isArray(draftActions) ? draftActions : [];
    state.lineups = Array.isArray(lineups) ? lineups : [];
    restoreDraftSelectionsFromServer();
    restoreLineupSelectionsFromServer();
}

function clearGameSelection() {
    state.selectedGameId = null;
    state.draftActions = [];
    state.lineups = [];
    state.draftSelections = createEmptyDraftSelections();
    state.lineupSelections = createEmptyLineupSelections();
    state.draftSearchTerms = {};
    resetGameForm();
}

async function selectMatch(matchId, preserveGameId) {
    state.selectedMatchId = Number(matchId);
    clearGameSelection();
    renderAll();

    setPanelError('games-error', '');
    try {
        await loadGames(state.selectedMatchId);
        const shouldRestoreGame = preserveGameId && state.games.some(game => Number(game.id) === Number(preserveGameId));
        resetGameForm();
        if (shouldRestoreGame) {
            await selectGame(preserveGameId);
        } else {
            renderAll();
        }
    } catch (error) {
        state.games = [];
        renderAll();
        setPanelError('games-error', error.message);
        showToast(error.message, 'err');
    }
}

async function selectGame(gameId) {
    state.selectedGameId = Number(gameId);
    const selectedGame = getSelectedGame();
    if (selectedGame) {
        openGameForm(selectedGame);
    }
    renderAll();
    setPanelError('draft-error', '');
    setPanelError('lineup-error', '');

    try {
        await loadDraftAndLineups(state.selectedGameId);
        renderAll();
    } catch (error) {
        state.draftActions = [];
        state.lineups = [];
        state.draftSelections = createEmptyDraftSelections();
        state.lineupSelections = createEmptyLineupSelections();
        renderAll();
        setPanelError('draft-error', error.message);
        showToast(error.message, 'err');
    }
}

async function refreshAllData(options) {
    const settings = options || {};
    const previousMatchId = state.selectedMatchId;
    const previousGameId = state.selectedGameId;
    resetPageError();
    setPanelError('matches-error', '');

    try {
        await Promise.all([loadTeams(), loadHeroes()]);
        await loadMatches();
        if (previousMatchId && state.matches.some(match => Number(match.id) === Number(previousMatchId))) {
            await selectMatch(previousMatchId, previousGameId);
        } else {
            state.selectedMatchId = null;
            state.games = [];
            clearGameSelection();
            resetMatchForm();
            renderAll();
        }
        if (!settings.silent) {
            showToast('Đã làm mới dữ liệu admin esports.', 'ok');
        }
    } catch (error) {
        renderAll();
        setPanelError('page-error', error.message);
        if (!settings.silent) {
            showToast(error.message, 'err');
        }
    }
}

function handleFilterInputChange() {
    state.matchFilters.search = byId('match-filter-search').value.trim();
    state.matchFilters.tournamentTier = byId('match-filter-tier').value;
    state.matchFilters.teamCode = byId('match-filter-team').value;
    state.matchFilters.dateFrom = byId('match-filter-date-from').value;
    state.matchFilters.dateTo = byId('match-filter-date-to').value;
    renderMatches();
}

function resetMatchFilters() {
    state.matchFilters = {
        search: '',
        tournamentTier: '',
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

async function submitMatchForm(event) {
    event.preventDefault();
    const payload = {
        matchDate: byId('mf-date').value,
        team1Code: normalizeTeamCode(byId('mf-team1').value),
        team2Code: normalizeTeamCode(byId('mf-team2').value),
        score1: Number(byId('mf-score1').value),
        score2: Number(byId('mf-score2').value),
        tier: byId('mf-tier').value,
        stage: byId('mf-stage').value
    };
    const matchId = toNullableNumber(byId('mf-id').value);

    if (!payload.matchDate || !payload.team1Code || !payload.team2Code) {
        setPanelError('match-form-error', 'Vui lòng nhập đủ ngày thi đấu và 2 team.');
        return;
    }
    if (payload.team1Code === payload.team2Code) {
        setPanelError('match-form-error', 'Team 1 và Team 2 không được trùng nhau.');
        return;
    }
    if (!Number.isFinite(payload.score1) || payload.score1 < 0 || !Number.isFinite(payload.score2) || payload.score2 < 0) {
        setPanelError('match-form-error', 'Tỷ số series phải là số nguyên không âm.');
        return;
    }

    setPanelError('match-form-error', '');
    setButtonLoading('btn-match-submit', true, 'Đang lưu match...');
    try {
        const response = await apiFetch(matchId ? `/api/admin/esports/matches/${matchId}` : '/api/admin/esports/matches', {
            method: matchId ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        resetMatchForm();
        await refreshAllData({ silent: true });
        if (response && response.id) {
            await selectMatch(response.id);
        }
        showToast(matchId ? 'Đã cập nhật match thành công.' : 'Đã tạo match mới thành công.', 'ok');
    } catch (error) {
        setPanelError('match-form-error', error.message);
        showToast(error.message, 'err');
    } finally {
        setButtonLoading('btn-match-submit', false);
    }
}

async function deleteMatch(matchId) {
    const match = state.matches.find(item => Number(item.id) === Number(matchId));
    if (!match) return;
    const confirmed = window.confirm(`Xóa match #${match.id} (${displayTeamNameByCode(match.team1Code)} vs ${displayTeamNameByCode(match.team2Code)})?\nFlow ranking/matches cũ vẫn sẽ dùng lại danh sách còn lại sau khi xóa.`);
    if (!confirmed) return;

    try {
        await apiFetch(`/api/admin/esports/matches/${matchId}`, { method: 'DELETE' });
        if (Number(state.selectedMatchId) === Number(matchId)) {
            state.selectedMatchId = null;
            state.games = [];
            clearGameSelection();
        }
        await refreshAllData({ silent: true });
        showToast(`Đã xóa match #${matchId}.`, 'ok');
    } catch (error) {
        showToast(error.message, 'err');
    }
}

function syncGameFormFromDom() {
    state.gameForm.id = toNullableNumber(byId('gf-id').value);
    state.gameForm.gameNumber = byId('gf-game-number').value;
    state.gameForm.blueTeamId = byId('gf-blue-team').value;
    state.gameForm.redTeamId = byId('gf-red-team').value;
    state.gameForm.winnerTeamId = byId('gf-winner-team').value;
    state.gameForm.durationSeconds = byId('gf-duration').value;
    state.gameForm.draftFormatId = byId('gf-draft-format-id').value;
}

async function submitGameForm(event) {
    event.preventDefault();
    const selectedMatch = getSelectedMatch();
    if (!selectedMatch) {
        setPanelError('game-form-error', 'Hãy chọn match trước khi thêm hoặc sửa game.');
        return;
    }

    syncGameFormFromDom();
    const payload = {
        gameNumber: Number(state.gameForm.gameNumber),
        blueTeamId: toNullableNumber(state.gameForm.blueTeamId),
        redTeamId: toNullableNumber(state.gameForm.redTeamId),
        winnerTeamId: toNullableNumber(state.gameForm.winnerTeamId),
        durationSeconds: state.gameForm.durationSeconds === '' ? null : Number(state.gameForm.durationSeconds),
        draftFormatId: toNullableNumber(state.gameForm.draftFormatId)
    };
    const gameId = toNullableNumber(state.gameForm.id);

    if (!Number.isFinite(payload.gameNumber) || payload.gameNumber <= 0) {
        setPanelError('game-form-error', 'Game number phải lớn hơn 0.');
        return;
    }
    if (!payload.blueTeamId || !payload.redTeamId) {
        setPanelError('game-form-error', 'Blue Team và Red Team là bắt buộc.');
        return;
    }
    if (payload.blueTeamId === payload.redTeamId) {
        setPanelError('game-form-error', 'Blue Team và Red Team không được trùng nhau.');
        return;
    }
    if (payload.durationSeconds != null && (!Number.isFinite(payload.durationSeconds) || payload.durationSeconds < 0)) {
        setPanelError('game-form-error', 'Duration phải là số không âm.');
        return;
    }

    setPanelError('game-form-error', '');
    setButtonLoading('btn-game-submit', true, 'Đang lưu game...');
    try {
        const response = await apiFetch(gameId ? `/api/admin/esports/games/${gameId}` : `/api/admin/esports/matches/${selectedMatch.id}/games`, {
            method: gameId ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        await selectMatch(selectedMatch.id, response?.id || state.selectedGameId);
        if (response?.id) {
            await selectGame(response.id);
        }
        showToast(gameId ? 'Đã cập nhật game thành công.' : 'Đã tạo game mới thành công.', 'ok');
    } catch (error) {
        setPanelError('game-form-error', error.message);
        showToast(error.message, 'err');
    } finally {
        setButtonLoading('btn-game-submit', false);
    }
}

async function deleteGame(gameId) {
    const game = state.games.find(item => Number(item.id) === Number(gameId));
    if (!game) return;
    const confirmed = window.confirm(`Xóa game #${game.gameNumber}?\nDraft actions và final lineup sẽ cascade theo backend.`);
    if (!confirmed) return;

    try {
        await apiFetch(`/api/admin/esports/games/${gameId}`, { method: 'DELETE' });
        if (Number(state.selectedGameId) === Number(gameId)) {
            clearGameSelection();
        }
        await selectMatch(state.selectedMatchId);
        showToast(`Đã xóa game #${game.gameNumber}.`, 'ok');
    } catch (error) {
        showToast(error.message, 'err');
    }
}

function collectDraftPayload() {
    const selectedGame = getSelectedGame();
    const duplicates = new Set();
    const usedHeroIds = new Set();
    const payload = [];

    HARD_PHASE_RULES.forEach(rule => {
        const heroId = toNullableNumber(state.draftSelections[rule.stepNumber]);
        if (!heroId) {
            return;
        }

        if (usedHeroIds.has(heroId)) {
            duplicates.add(heroId);
            return;
        }
        usedHeroIds.add(heroId);

        payload.push({
            teamId: rule.teamSide === 'BLUE' ? selectedGame.blueTeamId : selectedGame.redTeamId,
            heroId,
            actionType: rule.actionType,
            stepNumber: rule.stepNumber,
            teamSide: rule.teamSide
        });
    });

    return { payload, duplicateHeroIds: Array.from(duplicates) };
}

async function saveDraftActions() {
    const selectedGame = getSelectedGame();
    if (!selectedGame) {
        setPanelError('draft-error', 'Hãy chọn game trước khi lưu draft.');
        return;
    }

    const { payload, duplicateHeroIds } = collectDraftPayload();
    if (duplicateHeroIds.length) {
        const heroNames = duplicateHeroIds.map(heroId => heroById(heroId)?.name || `Hero #${heroId}`);
        setPanelError('draft-error', `Không cho chọn trùng hero trong cùng game: ${heroNames.join(', ')}.`);
        return;
    }

    setPanelError('draft-error', '');
    setButtonLoading('btn-draft-save', true, 'Đang lưu draft...');
    try {
        await apiFetch(`/api/admin/esports/games/${selectedGame.id}/draft-actions`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ actions: payload })
        });
        await loadDraftAndLineups(selectedGame.id);
        renderAll();
        showToast('Đã lưu draft 18 phase thành công.', 'ok');
    } catch (error) {
        setPanelError('draft-error', error.message);
        showToast(error.message, 'err');
    } finally {
        setButtonLoading('btn-draft-save', false);
    }
}

function restoreDraftSelections() {
    restoreDraftSelectionsFromServer();
    setPanelError('draft-error', '');
    renderAll();
    showToast('Đã khôi phục draft đã lưu.', 'ok');
}

async function clearDraftActions() {
    const selectedGame = getSelectedGame();
    if (!selectedGame) {
        setPanelError('draft-error', 'Hãy chọn game trước khi xóa draft.');
        return;
    }

    const confirmed = window.confirm('Xóa toàn bộ draft actions của game này? Final lineup không hợp lệ sẽ bị backend purge tự động.');
    if (!confirmed) return;

    setPanelError('draft-error', '');
    setButtonLoading('btn-draft-clear', true, 'Đang xóa draft...');
    try {
        await apiFetch(`/api/admin/esports/games/${selectedGame.id}/draft-actions`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ actions: [] })
        });
        await loadDraftAndLineups(selectedGame.id);
        renderAll();
        showToast('Đã xóa draft của game hiện tại.', 'ok');
    } catch (error) {
        setPanelError('draft-error', error.message);
        showToast(error.message, 'err');
    } finally {
        setButtonLoading('btn-draft-clear', false);
    }
}

function buildLineupPayload() {
    const selectedGame = getSelectedGame();
    const payload = [];
    const duplicates = new Set();
    const usedHeroIds = new Set();

    ['BLUE', 'RED'].forEach(side => {
        LANE_ROLES.forEach(item => {
            const heroId = toNullableNumber(state.lineupSelections[side][item.role]);
            if (!heroId) {
                return;
            }
            if (usedHeroIds.has(heroId)) {
                duplicates.add(heroId);
                return;
            }
            usedHeroIds.add(heroId);
            payload.push({
                teamId: side === 'BLUE' ? selectedGame.blueTeamId : selectedGame.redTeamId,
                teamSide: side,
                positionNumber: item.position,
                laneRole: item.role,
                heroId
            });
        });
    });

    return { payload, duplicateHeroIds: Array.from(duplicates) };
}

async function saveLineups() {
    const selectedGame = getSelectedGame();
    if (!selectedGame) {
        setPanelError('lineup-error', 'Hãy chọn game trước khi lưu final lineup.');
        return;
    }

    const picks = pickedHeroesBySide();
    if (picks.BLUE.filter(Boolean).length < 5 || picks.RED.filter(Boolean).length < 5) {
        setPanelError('lineup-error', 'Cần lưu draft đủ 10 PICK trước khi nhập final lineup.');
        return;
    }

    const { payload, duplicateHeroIds } = buildLineupPayload();
    if (payload.length !== 10) {
        setPanelError('lineup-error', 'Final lineup phải đủ 10 hero: BLUE/RED DSL/JGL/MID/ADL/SUP.');
        return;
    }
    if (duplicateHeroIds.length) {
        const heroNames = duplicateHeroIds.map(heroId => heroById(heroId)?.name || `Hero #${heroId}`);
        setPanelError('lineup-error', `Không cho trùng hero trong final lineup: ${heroNames.join(', ')}.`);
        return;
    }

    setPanelError('lineup-error', '');
    setButtonLoading('btn-lineup-save', true, 'Đang lưu lineup...');
    try {
        const response = await apiFetch(`/api/admin/esports/games/${selectedGame.id}/lineups`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ lineups: payload })
        });
        state.lineups = Array.isArray(response) ? response : [];
        restoreLineupSelectionsFromServer();
        renderAll();
        showToast('Đã upsert final lineup thành công.', 'ok');
    } catch (error) {
        setPanelError('lineup-error', error.message);
        showToast(error.message, 'err');
    } finally {
        setButtonLoading('btn-lineup-save', false);
    }
}

function restoreLineups() {
    restoreLineupSelectionsFromServer();
    setPanelError('lineup-error', '');
    renderAll();
    showToast('Đã khôi phục final lineup đã lưu.', 'ok');
}

async function clearLineups() {
    const selectedGame = getSelectedGame();
    if (!selectedGame) {
        setPanelError('lineup-error', 'Hãy chọn game trước khi xóa lineup.');
        return;
    }

    const confirmed = window.confirm('Xóa toàn bộ final lineup của game này?');
    if (!confirmed) return;

    setPanelError('lineup-error', '');
    setButtonLoading('btn-lineup-clear', true, 'Đang xóa lineup...');
    try {
        const response = await apiFetch(`/api/admin/esports/games/${selectedGame.id}/lineups`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ lineups: [] })
        });
        state.lineups = Array.isArray(response) ? response : [];
        restoreLineupSelectionsFromServer();
        renderAll();
        showToast('Đã xóa final lineup của game hiện tại.', 'ok');
    } catch (error) {
        setPanelError('lineup-error', error.message);
        showToast(error.message, 'err');
    } finally {
        setButtonLoading('btn-lineup-clear', false);
    }
}

function bindEvents() {
    byId('btn-refresh-admin-esports')?.addEventListener('click', () => refreshAllData({ silent: false }));

    ['match-filter-search', 'match-filter-tier', 'match-filter-team', 'match-filter-date-from', 'match-filter-date-to']
        .forEach(id => byId(id)?.addEventListener(id === 'match-filter-search' ? 'input' : 'change', handleFilterInputChange));
    byId('btn-match-filter-reset')?.addEventListener('click', resetMatchFilters);

    byId('btn-match-reset')?.addEventListener('click', resetMatchForm);
    byId('match-form')?.addEventListener('submit', submitMatchForm);
    ['mf-date', 'mf-team1', 'mf-team2', 'mf-score1', 'mf-score2', 'mf-tier', 'mf-stage']
        .forEach(id => byId(id)?.addEventListener(id === 'mf-score1' || id === 'mf-score2' ? 'input' : 'change', syncMatchFormFromDom));

    byId('matches-tbody')?.addEventListener('click', async event => {
        const button = event.target.closest('button[data-action]');
        if (!button) return;
        const action = button.dataset.action;
        const id = Number(button.dataset.id);
        const match = state.matches.find(item => Number(item.id) === id);
        if (!match) return;
        if (action === 'select-match') {
            await selectMatch(id);
        } else if (action === 'edit-match') {
            openMatchForm(match);
        } else if (action === 'delete-match') {
            await deleteMatch(id);
        }
    });

    byId('btn-game-reset')?.addEventListener('click', resetGameForm);
    byId('game-form')?.addEventListener('submit', submitGameForm);
    ['gf-game-number', 'gf-blue-team', 'gf-red-team', 'gf-winner-team', 'gf-duration']
        .forEach(id => byId(id)?.addEventListener(id === 'gf-game-number' || id === 'gf-duration' ? 'input' : 'change', syncGameFormFromDom));

    byId('games-tbody')?.addEventListener('click', async event => {
        const button = event.target.closest('button[data-action]');
        if (!button) return;
        const action = button.dataset.action;
        const id = Number(button.dataset.id);
        const game = state.games.find(item => Number(item.id) === id);
        if (!game) return;
        if (action === 'select-game') {
            await selectGame(id);
        } else if (action === 'edit-game') {
            openGameForm(game);
        } else if (action === 'delete-game') {
            await deleteGame(id);
        }
    });

    byId('draft-phases-tbody')?.addEventListener('input', event => {
        const input = event.target.closest('[data-draft-search]');
        if (!input) return;
        state.draftSearchTerms[input.dataset.draftSearch] = input.value;
        renderDraftPhases();
    });

    byId('draft-phases-tbody')?.addEventListener('change', event => {
        const select = event.target.closest('[data-draft-select]');
        if (!select) return;
        state.draftSelections[Number(select.dataset.draftSelect)] = toNullableNumber(select.value);
        setPanelError('draft-error', '');
        renderAll();
    });

    byId('btn-draft-save')?.addEventListener('click', saveDraftActions);
    byId('btn-draft-restore')?.addEventListener('click', restoreDraftSelections);
    byId('btn-draft-clear')?.addEventListener('click', clearDraftActions);

    byId('lineup-grid')?.addEventListener('change', event => {
        const select = event.target.closest('[data-lineup-side][data-lineup-role]');
        if (!select) return;
        const side = select.dataset.lineupSide;
        const role = select.dataset.lineupRole;
        state.lineupSelections[side][role] = toNullableNumber(select.value);
        setPanelError('lineup-error', '');
        renderAll();
    });

    byId('btn-lineup-save')?.addEventListener('click', saveLineups);
    byId('btn-lineup-restore')?.addEventListener('click', restoreLineups);
    byId('btn-lineup-clear')?.addEventListener('click', clearLineups);

    document.addEventListener('authExpired', () => {
        window.location.replace('/html/index.html');
    });
}

function initAdminEsportsDataPage() {
    if (!document.querySelector('[data-page="admin-esports-data"]')) return;
    if (typeof requireRoleAccess === 'function' && !requireRoleAccess('Admin', '/html/index.html')) {
        return;
    }

    bindEvents();
    populateStaticSelects();
    resetMatchForm();
    resetGameForm();
    renderAll();
    updateClock();
    window.setInterval(updateClock, 1000);
    refreshAllData({ silent: true });
}

window.AdminEsportsDataPage = {
    state,
    refreshAllData,
    selectMatch,
    selectGame
};

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initAdminEsportsDataPage);
} else {
    initAdminEsportsDataPage();
}
