(function () {
    'use strict';

    var MONTH_OPTIONS = [
        { value: 1, label: 'T01' },
        { value: 2, label: 'T02' },
        { value: 3, label: 'T03' },
        { value: 4, label: 'T04' },
        { value: 5, label: 'T05' },
        { value: 6, label: 'T06' },
        { value: 7, label: 'T07' },
        { value: 8, label: 'T08' },
        { value: 9, label: 'T09' },
        { value: 10, label: 'T10' },
        { value: 11, label: 'T11' },
        { value: 12, label: 'T12' }
    ];

    var READ_ONLY_DODGE_FIELD_ID = 'bp-dodge-reject-reset-during-draft';
    var SAVE_BUTTON_IDS = ['bp-save-settings-btn', 'bp-save-settings-btn-bottom'];
    var RESET_DEFAULT_BUTTON_IDS = ['bp-reset-defaults-btn', 'bp-reset-defaults-btn-bottom'];
    var PREVIEW_BUTTON_IDS = {
        SOFT: 'bp-preview-soft-btn',
        HARD: 'bp-preview-hard-btn'
    };

    var state = {
        initialized: false,
        isHydrating: false,
        isLoading: false,
        isSaving: false,
        isResettingDefaults: false,
        isPreviewLoading: false,
        isExecuting: false,
        hasLoaded: false,
        settings: null,
        preview: null,
        loadPromise: null
    };

    function byId(id) {
        return document.getElementById(id);
    }

    function queryAll(selector, root) {
        return Array.prototype.slice.call((root || document).querySelectorAll(selector));
    }

    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function parseApiErrorMessage(rawText, response) {
        if (!rawText) {
            return response.status + ' ' + response.statusText;
        }
        try {
            var payload = JSON.parse(rawText);
            return payload.error || payload.message || payload.detail || rawText;
        } catch (error) {
            return rawText;
        }
    }

    async function apiFetch(url, options) {
        var response = await fetch(url, options || {});
        var rawText = await response.text();
        if (!response.ok) {
            throw new Error(parseApiErrorMessage(rawText, response));
        }
        return rawText ? JSON.parse(rawText) : null;
    }

    function emitToast(message, type) {
        if (typeof window.showToast === 'function') {
            window.showToast(message, type === 'success' ? 'ok' : 'err');
        }
    }

    function formatNumber(value, maximumFractionDigits) {
        if (value == null || value === '') {
            return '--';
        }
        var numeric = Number(value);
        if (!Number.isFinite(numeric)) {
            return '--';
        }
        return numeric.toLocaleString('vi-VN', {
            minimumFractionDigits: 0,
            maximumFractionDigits: maximumFractionDigits == null ? 0 : maximumFractionDigits
        });
    }

    function formatDate(value) {
        if (!value) {
            return '--';
        }
        var parsed = new Date(value + 'T00:00:00');
        if (Number.isNaN(parsed.getTime())) {
            parsed = new Date(value);
        }
        if (Number.isNaN(parsed.getTime())) {
            return String(value);
        }
        return parsed.toLocaleDateString('vi-VN');
    }

    function formatDateTime(value) {
        if (!value) {
            return '--';
        }
        var parsed = new Date(value);
        if (Number.isNaN(parsed.getTime())) {
            return String(value);
        }
        return parsed.toLocaleString('vi-VN', {
            dateStyle: 'short',
            timeStyle: 'short'
        });
    }

    function monthLabel(month) {
        var numericMonth = Number(month);
        if (!Number.isFinite(numericMonth) || numericMonth < 1 || numericMonth > 12) {
            return String(month == null ? '--' : month);
        }
        return 'T' + String(numericMonth).padStart(2, '0');
    }

    function monthListLabel(months) {
        if (!Array.isArray(months) || !months.length) {
            return 'None';
        }
        return months.map(monthLabel).join(', ');
    }

    function setStatus(type, message, sticky) {
        var banner = byId('bp-rating-status');
        if (!banner) {
            return;
        }
        if (!message) {
            banner.className = 'bp-status bp-status-info hidden';
            banner.textContent = '';
            return;
        }
        banner.className = 'bp-status bp-status-' + type + (sticky ? '' : '');
        banner.textContent = message;
    }

    function clearStatus() {
        setStatus('info', '');
    }

    function setElementValue(id, value) {
        var element = byId(id);
        if (!element) {
            return;
        }
        element.value = value == null ? '' : String(value);
    }

    function setCheckboxValue(id, value) {
        var element = byId(id);
        if (!element) {
            return;
        }
        element.checked = Boolean(value);
    }

    function updateSummaryCard(id, value) {
        var element = byId(id);
        if (element) {
            element.textContent = value == null ? '--' : String(value);
        }
    }

    function renderEmptyDiagnostics() {
        var grid = byId('bp-diagnostics-grid');
        var lastReset = byId('bp-last-reset-log');
        if (grid) {
            grid.innerHTML = '<article class="bp-diagnostic-card"><span class="bp-diagnostic-label">Diagnostics</span><strong class="bp-diagnostic-value">--</strong><p class="bp-diagnostic-note">Load settings de xem runtime diagnostics.</p></article>';
        }
        if (lastReset) {
            lastReset.innerHTML = '<h4>Last reset log</h4><p class="text-sm text-slate-500">Chua co reset log nao.</p>';
        }
    }

    function renderPreviewPlaceholder(message) {
        var container = byId('bp-reset-preview-content');
        if (!container) {
            return;
        }
        state.preview = null;
        container.innerHTML = '<p class="bp-preview-empty">' + escapeHtml(message) + '</p>';
    }

    function confirmationText() {
        var value = state.settings
            && state.settings.seasonalReset
            && state.settings.seasonalReset.confirmationText;
        return value || 'RESET SOLO RANK';
    }

    function syncExecuteButtonState() {
        var button = byId('bp-reset-execute-btn');
        var input = byId('bp-reset-execute-confirmation');
        var help = byId('bp-reset-execute-confirmation-help');
        if (!button || !input) {
            return;
        }
        var expected = confirmationText();
        var matches = input.value.trim() === expected;
        var disabled = state.isExecuting || state.isLoading || !state.hasLoaded || !matches;
        button.disabled = disabled;
        if (!help) {
            return;
        }
        if (matches) {
            help.textContent = 'Confirmation hop le. API execute chi duoc goi khi bam nut Execute Reset.';
            help.className = 'field-help bp-confirmation-help bp-confirmation-help-ok';
        } else {
            help.textContent = 'Nhap chinh xac "' + expected + '" de mo khoa Execute Reset.';
            help.className = 'field-help bp-confirmation-help';
        }
    }

    function setButtonLoading(buttonId, loading, loadingText) {
        var button = byId(buttonId);
        if (!button) {
            return;
        }
        if (loading) {
            if (!button.dataset.originalText) {
                button.dataset.originalText = button.textContent;
            }
            button.textContent = loadingText;
            button.disabled = true;
            button.classList.add('bp-is-loading');
            return;
        }
        button.textContent = button.dataset.originalText || button.textContent;
        button.disabled = false;
        button.classList.remove('bp-is-loading');
    }

    function setButtonsLoading(ids, loading, loadingText) {
        ids.forEach(function (buttonId) {
            setButtonLoading(buttonId, loading, loadingText);
        });
    }

    function updateActionAvailability() {
        var saveBusy = state.isLoading || state.isSaving || state.isResettingDefaults || state.isPreviewLoading || state.isExecuting;
        SAVE_BUTTON_IDS.forEach(function (buttonId) {
            var button = byId(buttonId);
            if (!button) {
                return;
            }
            if (!state.isSaving) {
                button.disabled = saveBusy || !state.hasLoaded;
            }
        });

        RESET_DEFAULT_BUTTON_IDS.forEach(function (buttonId) {
            var button = byId(buttonId);
            if (!button) {
                return;
            }
            if (!state.isResettingDefaults) {
                button.disabled = saveBusy || !state.hasLoaded;
            }
        });

        Object.keys(PREVIEW_BUTTON_IDS).forEach(function (key) {
            var button = byId(PREVIEW_BUTTON_IDS[key]);
            if (!button) {
                return;
            }
            if (!state.isPreviewLoading) {
                button.disabled = state.isLoading || state.isSaving || state.isResettingDefaults || state.isExecuting || !state.hasLoaded;
            }
        });

        var executeType = byId('bp-reset-execute-type');
        var executeNote = byId('bp-reset-execute-note');
        var executeInput = byId('bp-reset-execute-confirmation');
        if (executeType) {
            executeType.disabled = state.isLoading || state.isSaving || state.isResettingDefaults || state.isExecuting || !state.hasLoaded;
        }
        if (executeNote) {
            executeNote.disabled = state.isLoading || state.isSaving || state.isResettingDefaults || state.isExecuting || !state.hasLoaded;
        }
        if (executeInput) {
            executeInput.disabled = state.isLoading || state.isSaving || state.isResettingDefaults || state.isExecuting || !state.hasLoaded;
        }
        syncExecuteButtonState();
    }

    function activateTab(tabName) {
        queryAll('[data-bp-tab]').forEach(function (button) {
            button.classList.toggle('active', button.dataset.bpTab === tabName);
        });
        queryAll('[data-bp-panel]').forEach(function (panel) {
            panel.classList.toggle('active', panel.dataset.bpPanel === tabName);
        });
    }

    function buildMonthGrid(containerId) {
        var container = byId(containerId);
        if (!container || container.dataset.ready === 'true') {
            return;
        }
        container.innerHTML = MONTH_OPTIONS.map(function (option) {
            return [
                '<label class="checkbox-label bp-month-option">',
                '  <input type="checkbox" value="' + option.value + '">',
                '  <span>' + option.label + '</span>',
                '</label>'
            ].join('');
        }).join('');
        container.dataset.ready = 'true';
    }

    function selectedMonths(containerId) {
        var container = byId(containerId);
        if (!container) {
            return [];
        }
        return queryAll('input[type="checkbox"]', container)
            .filter(function (input) { return input.checked; })
            .map(function (input) { return Number(input.value); })
            .filter(function (month) { return Number.isInteger(month) && month >= 1 && month <= 12; })
            .sort(function (left, right) { return left - right; });
    }

    function setSelectedMonths(containerId, months) {
        var monthSet = new Set((Array.isArray(months) ? months : []).map(function (value) { return Number(value); }));
        var container = byId(containerId);
        if (!container) {
            return;
        }
        queryAll('input[type="checkbox"]', container).forEach(function (input) {
            input.checked = monthSet.has(Number(input.value));
        });
    }

    function resetExecuteForm() {
        setElementValue('bp-reset-execute-confirmation', '');
        setElementValue('bp-reset-execute-note', '');
        var typeSelect = byId('bp-reset-execute-type');
        if (typeSelect) {
            typeSelect.value = 'SOFT';
        }
        syncExecuteButtonState();
    }

    function renderAntiTradingSummary(antiTrading) {
        var pill = byId('bp-anti-blocked-delta');
        if (!pill) {
            return;
        }
        var safeAntiTrading = antiTrading || {};
        var blockedWin = Number.isFinite(Number(safeAntiTrading.blockedWinDelta))
            ? Number(safeAntiTrading.blockedWinDelta)
            : 0;
        var blockedLoss = Number.isFinite(Number(safeAntiTrading.blockedLossDelta))
            ? Number(safeAntiTrading.blockedLossDelta)
            : 0;
        pill.textContent = 'Blocked delta co dinh: ' + blockedWin + ' / ' + blockedLoss;
    }

    function renderDodgeReadonlyState(dodge) {
        var field = byId('bp-dodge-reject-reset-during-draft-field');
        var note = byId('bp-dodge-readonly-note');
        var value = Boolean(dodge && dodge.rejectResetDuringDraft);
        setCheckboxValue(READ_ONLY_DODGE_FIELD_ID, value);
        if (field) {
            field.classList.add('disabled');
            field.setAttribute('title', 'Read-only until backend supports this toggle clearly.');
        }
        if (note) {
            note.textContent = [
                'Read-only: backend van luu flag nay, nhung reset room hien da bi status flow chan san.',
                ' Stored value: ',
                value ? 'ON' : 'OFF',
                '.'
            ].join('');
        }
    }

    function renderSummary(settings) {
        var diagnostics = settings && settings.diagnostics ? settings.diagnostics : {};
        var seasonalReset = settings && settings.seasonalReset ? settings.seasonalReset : {};
        var nextReset = diagnostics.nextScheduledReset;

        updateSummaryCard('bp-summary-macro-delta', diagnostics.currentMacroWinDelta == null ? '--' : formatNumber(diagnostics.currentMacroWinDelta, 0));
        updateSummaryCard(
            'bp-summary-active-pool',
            diagnostics.currentActivePoolSize == null ? '--' : formatNumber(diagnostics.currentActivePoolSize, 0) + ' players'
        );
        updateSummaryCard(
            'bp-summary-next-reset',
            nextReset ? nextReset.resetType + ' - ' + formatDate(nextReset.scheduledDate) : (seasonalReset.schedulerEnabled ? 'Pending' : 'Disabled')
        );
        updateSummaryCard('bp-summary-updated-by', diagnostics.updatedBy || 'SYSTEM_DEFAULT');

        var activePoolNote = byId('bp-summary-active-pool-note');
        if (activePoolNote) {
            activePoolNote.textContent = diagnostics.currentActivePlayerCount == null
                ? 'Diagnostics chua co du lieu.'
                : formatNumber(diagnostics.currentActivePlayerCount, 0) + ' active players in current window.';
        }

        var nextResetNote = byId('bp-summary-next-reset-note');
        if (nextResetNote) {
            nextResetNote.textContent = nextReset
                ? 'Computed from current month config. Scheduler ' + (seasonalReset.schedulerEnabled ? 'is ON.' : 'is OFF.')
                : (seasonalReset.schedulerEnabled ? 'No reset month is due next.' : 'Scheduler is currently disabled.');
        }

        var updatedAt = byId('bp-summary-updated-at');
        if (updatedAt) {
            updatedAt.textContent = diagnostics.updatedAt
                ? 'Updated at ' + formatDateTime(diagnostics.updatedAt)
                : 'No runtime save has been recorded yet.';
        }
    }

    function renderDiagnostics(settings) {
        var diagnostics = settings && settings.diagnostics ? settings.diagnostics : {};
        var macro = settings && settings.macro ? settings.macro : {};
        var seasonalReset = settings && settings.seasonalReset ? settings.seasonalReset : {};
        var nextReset = diagnostics.nextScheduledReset;
        var grid = byId('bp-diagnostics-grid');
        if (grid) {
            var cards = [
                {
                    label: 'Macro win delta',
                    value: diagnostics.currentMacroWinDelta == null ? '--' : formatNumber(diagnostics.currentMacroWinDelta, 0),
                    note: 'Current snapshot for newly completed matches.'
                },
                {
                    label: 'Active players',
                    value: diagnostics.currentActivePlayerCount == null ? '--' : formatNumber(diagnostics.currentActivePlayerCount, 0),
                    note: 'Window days: ' + (macro.activeWindowDays == null ? '--' : formatNumber(macro.activeWindowDays, 0))
                },
                {
                    label: 'Active pool size',
                    value: diagnostics.currentActivePoolSize == null ? '--' : formatNumber(diagnostics.currentActivePoolSize, 0),
                    note: 'Top percent: ' + (macro.activeTopPercent == null ? '--' : formatNumber(macro.activeTopPercent, 0)) + '%'
                },
                {
                    label: 'Pool avg rating',
                    value: diagnostics.activePoolAverageRating == null ? '--' : formatNumber(diagnostics.activePoolAverageRating, 2),
                    note: 'Balance target: ' + (macro.balanceRating == null ? '--' : formatNumber(macro.balanceRating, 0))
                },
                {
                    label: 'Scheduler',
                    value: seasonalReset.schedulerEnabled ? 'Enabled' : 'Disabled',
                    note: 'Soft: ' + monthListLabel(seasonalReset.softResetMonths) + ' | Hard: ' + monthListLabel(seasonalReset.hardResetMonths)
                },
                {
                    label: 'Next scheduled reset',
                    value: nextReset ? nextReset.resetType + ' - ' + formatDate(nextReset.scheduledDate) : 'None',
                    note: nextReset ? 'Derived from current reset calendar.' : 'No upcoming reset from current config.'
                },
                {
                    label: 'Updated by',
                    value: diagnostics.updatedBy || 'SYSTEM_DEFAULT',
                    note: diagnostics.updatedAt ? formatDateTime(diagnostics.updatedAt) : 'No runtime save yet.'
                },
                {
                    label: 'Replay anchor',
                    value: diagnostics.replayAnchorAdvanced ? 'Advanced' : 'Unchanged',
                    note: 'Moves only when replay-sensitive rules change.'
                }
            ];

            grid.innerHTML = cards.map(function (card) {
                return [
                    '<article class="bp-diagnostic-card">',
                    '  <span class="bp-diagnostic-label">' + escapeHtml(card.label) + '</span>',
                    '  <strong class="bp-diagnostic-value">' + escapeHtml(card.value) + '</strong>',
                    '  <p class="bp-diagnostic-note">' + escapeHtml(card.note) + '</p>',
                    '</article>'
                ].join('');
            }).join('');
        }

        var lastReset = byId('bp-last-reset-log');
        if (!lastReset) {
            return;
        }
        if (!diagnostics.lastResetLog) {
            lastReset.innerHTML = '<h4>Last reset log</h4><p class="text-sm text-slate-500">Chua co reset log nao.</p>';
            return;
        }
        var log = diagnostics.lastResetLog;
        lastReset.innerHTML = [
            '<h4>Last reset log</h4>',
            '<div class="bp-key-value-grid">',
            '  <div><span class="bp-key-value-label">Reset</span><strong class="bp-key-value-value">' + escapeHtml(log.resetType || '--') + '</strong></div>',
            '  <div><span class="bp-key-value-label">Scheduled date</span><strong class="bp-key-value-value">' + escapeHtml(formatDate(log.scheduledDate)) + '</strong></div>',
            '  <div><span class="bp-key-value-label">Executed at</span><strong class="bp-key-value-value">' + escapeHtml(formatDateTime(log.executedAt)) + '</strong></div>',
            '  <div><span class="bp-key-value-label">Affected players</span><strong class="bp-key-value-value">' + escapeHtml(formatNumber(log.affectedPlayers, 0)) + '</strong></div>',
            '  <div><span class="bp-key-value-label">Base rating</span><strong class="bp-key-value-value">' + escapeHtml(formatNumber(log.baseRating, 0)) + '</strong></div>',
            '  <div><span class="bp-key-value-label">Executed by</span><strong class="bp-key-value-value">' + escapeHtml(log.executedBy || '--') + '</strong></div>',
            '</div>',
            '<p class="bp-log-formula"><strong>Formula:</strong> ' + escapeHtml(log.formula || '--') + '</p>',
            '<p class="bp-log-note"><strong>Note:</strong> ' + escapeHtml(log.note || 'No note') + '</p>'
        ].join('');
    }

    function renderPreview(preview) {
        var container = byId('bp-reset-preview-content');
        if (!container) {
            return;
        }
        if (!preview) {
            renderPreviewPlaceholder('Preview response is empty.');
            return;
        }
        state.preview = preview;
        var samples = Array.isArray(preview.samples) ? preview.samples : [];
        var beforeSummary = preview.before || {};
        var afterSummary = preview.after || {};
        container.innerHTML = [
            '<div class="bp-preview-meta">',
            '  <span class="bp-preview-badge">' + escapeHtml(preview.resetType || '--') + '</span>',
            '  <span class="bp-preview-formula">' + escapeHtml(preview.formula || '--') + '</span>',
            '</div>',
            '<div class="bp-preview-metrics">',
            '  <article class="bp-preview-metric"><span class="bp-preview-label">Affected players</span><strong class="bp-preview-value">' + escapeHtml(formatNumber(preview.affectedPlayerCount, 0)) + '</strong></article>',
            '  <article class="bp-preview-metric"><span class="bp-preview-label">Base rating</span><strong class="bp-preview-value">' + escapeHtml(formatNumber(preview.baseRating, 0)) + '</strong></article>',
            '  <article class="bp-preview-metric"><span class="bp-preview-label">Before avg</span><strong class="bp-preview-value">' + escapeHtml(formatNumber(beforeSummary.averageRating, 2)) + '</strong></article>',
            '  <article class="bp-preview-metric"><span class="bp-preview-label">After avg</span><strong class="bp-preview-value">' + escapeHtml(formatNumber(afterSummary.averageRating, 2)) + '</strong></article>',
            '</div>',
            '<div class="bp-preview-summary-grid">',
            '  <article class="bp-preview-summary-card">',
            '    <span class="bp-preview-label">Before</span>',
            '    <p>Min: <strong>' + escapeHtml(formatNumber(beforeSummary.minRating, 0)) + '</strong></p>',
            '    <p>Max: <strong>' + escapeHtml(formatNumber(beforeSummary.maxRating, 0)) + '</strong></p>',
            '    <p>Avg: <strong>' + escapeHtml(formatNumber(beforeSummary.averageRating, 2)) + '</strong></p>',
            '  </article>',
            '  <article class="bp-preview-summary-card">',
            '    <span class="bp-preview-label">After</span>',
            '    <p>Min: <strong>' + escapeHtml(formatNumber(afterSummary.minRating, 0)) + '</strong></p>',
            '    <p>Max: <strong>' + escapeHtml(formatNumber(afterSummary.maxRating, 0)) + '</strong></p>',
            '    <p>Avg: <strong>' + escapeHtml(formatNumber(afterSummary.averageRating, 2)) + '</strong></p>',
            '  </article>',
            '</div>',
            samples.length
                ? [
                    '<div class="bp-preview-samples">',
                    '  <div class="bp-preview-table-wrap">',
                    '    <table class="bp-preview-table">',
                    '      <thead><tr><th>User</th><th>Email</th><th>Before</th><th>After</th></tr></thead>',
                    '      <tbody>',
                    samples.map(function (sample) {
                        return [
                            '<tr>',
                            '  <td><strong>' + escapeHtml(sample.displayName || ('User #' + sample.userId)) + '</strong><div class="bp-preview-user-id">ID ' + escapeHtml(sample.userId) + '</div></td>',
                            '  <td>' + escapeHtml(sample.email || '--') + '</td>',
                            '  <td>' + escapeHtml(formatNumber(sample.beforeRating, 0)) + '</td>',
                            '  <td>' + escapeHtml(formatNumber(sample.afterRating, 0)) + '</td>',
                            '</tr>'
                        ].join('');
                    }).join(''),
                    '      </tbody>',
                    '    </table>',
                    '  </div>',
                    '</div>'
                ].join('')
                : '<p class="bp-preview-empty">Preview khong co sample player nao.</p>'
        ].join('');
    }

    function renderSettings(settings) {
        state.settings = settings;
        state.hasLoaded = true;
        state.isHydrating = true;
        try {
            setElementValue('bp-initial-rating', settings.base && settings.base.initialRating);
            setElementValue('bp-min-rating', settings.base && settings.base.minRating);
            setElementValue('bp-base-win-delta', settings.base && settings.base.baseWinDelta);
            setElementValue('bp-base-loss-delta', settings.base && settings.base.baseLossDelta);

            setCheckboxValue('bp-macro-enabled', settings.macro && settings.macro.enabled);
            setElementValue('bp-macro-window-days', settings.macro && settings.macro.activeWindowDays);
            setElementValue('bp-macro-balance-rating', settings.macro && settings.macro.balanceRating);
            setElementValue('bp-macro-active-top-percent', settings.macro && settings.macro.activeTopPercent);
            setElementValue('bp-macro-rating-step', settings.macro && settings.macro.ratingStep);
            setElementValue('bp-macro-win-adjustment-step', settings.macro && settings.macro.winAdjustmentPerStep);
            setElementValue('bp-macro-min-win-delta', settings.macro && settings.macro.minWinDelta);
            setElementValue('bp-macro-min-active-players', settings.macro && settings.macro.minimumActivePlayers);

            setCheckboxValue('bp-gap-enabled', settings.gap && settings.gap.enabled);
            setElementValue('bp-gap-rating-diff-step', settings.gap && settings.gap.ratingDiffStep);
            setElementValue('bp-gap-modifier-per-step', settings.gap && settings.gap.modifierPerStep);
            setElementValue('bp-gap-max-modifier', settings.gap && settings.gap.maxModifier);

            setCheckboxValue('bp-anti-enabled', settings.antiTrading && settings.antiTrading.enabled);
            setElementValue('bp-anti-window-hours', settings.antiTrading && settings.antiTrading.windowHours);
            setElementValue('bp-anti-allowed-recent-matches', settings.antiTrading && settings.antiTrading.allowedRecentMatches);

            setCheckboxValue('bp-dodge-enabled', settings.dodge && settings.dodge.enabled);
            setCheckboxValue('bp-dodge-apply-in-draft-only', settings.dodge && settings.dodge.applyInDraftOnly);
            setElementValue('bp-dodge-disconnect-grace-seconds', settings.dodge && settings.dodge.disconnectGraceSeconds);
            setElementValue('bp-dodge-cooldown-minutes', settings.dodge && settings.dodge.cooldownMinutes);

            setCheckboxValue('bp-season-scheduler-enabled', settings.seasonalReset && settings.seasonalReset.schedulerEnabled);
            setCheckboxValue('bp-season-hard-priority-over-soft', settings.seasonalReset && settings.seasonalReset.hardPriorityOverSoft);
            setSelectedMonths('bp-soft-month-grid', settings.seasonalReset && settings.seasonalReset.softResetMonths);
            setSelectedMonths('bp-hard-month-grid', settings.seasonalReset && settings.seasonalReset.hardResetMonths);

            var confirmation = byId('bp-season-confirmation-text');
            if (confirmation) {
                confirmation.textContent = confirmationText();
            }

            renderAntiTradingSummary(settings.antiTrading);
            renderDodgeReadonlyState(settings.dodge);
            renderSummary(settings);
            renderDiagnostics(settings);
            resetExecuteForm();
        } finally {
            state.isHydrating = false;
            updateActionAvailability();
        }
    }

    function readNumericField(id, label, errors, options) {
        var input = byId(id);
        if (!input) {
            errors.push(label + ' field is missing in DOM.');
            return null;
        }
        var rawValue = String(input.value == null ? '' : input.value).trim();
        if (!rawValue) {
            errors.push(label + ' is required.');
            return null;
        }
        var normalizedValue = rawValue.replace(',', '.');
        var value = Number(normalizedValue);
        if (!Number.isFinite(value)) {
            errors.push(label + ' must be a valid number.');
            return null;
        }
        if (options && options.integer && !Number.isInteger(value)) {
            errors.push(label + ' must be an integer.');
            return null;
        }
        if (options && options.min != null && value < options.min) {
            errors.push(label + ' must be >= ' + options.min + '.');
            return null;
        }
        if (options && options.max != null && value > options.max) {
            errors.push(label + ' must be <= ' + options.max + '.');
            return null;
        }
        return value;
    }

    function checkboxValue(id) {
        var input = byId(id);
        return Boolean(input && input.checked);
    }

    function collectPayload() {
        var errors = [];
        var payload = {
            base: {
                initialRating: readNumericField('bp-initial-rating', 'Initial Rating', errors, { integer: true, min: 0 }),
                baseWinDelta: readNumericField('bp-base-win-delta', 'Base Win Delta', errors, { integer: true, min: 0 }),
                baseLossDelta: readNumericField('bp-base-loss-delta', 'Base Loss Delta', errors, { integer: true, max: 0 }),
                minRating: readNumericField('bp-min-rating', 'Min Rating', errors, { integer: true, min: 0 })
            },
            macro: {
                enabled: checkboxValue('bp-macro-enabled'),
                activeWindowDays: readNumericField('bp-macro-window-days', 'Macro Active Window Days', errors, { integer: true, min: 1, max: 365 }),
                balanceRating: readNumericField('bp-macro-balance-rating', 'Macro Balance Rating', errors, { integer: true, min: 0 }),
                activeTopPercent: readNumericField('bp-macro-active-top-percent', 'Macro Active Top Percent', errors, { integer: true, min: 1, max: 100 }),
                ratingStep: readNumericField('bp-macro-rating-step', 'Macro Rating Step', errors, { integer: true, min: 1 }),
                winAdjustmentPerStep: readNumericField('bp-macro-win-adjustment-step', 'Macro Win Adjustment Per Step', errors, { min: 0, max: 1 }),
                minWinDelta: readNumericField('bp-macro-min-win-delta', 'Macro Min Win Delta', errors, { integer: true, min: 0 }),
                minimumActivePlayers: readNumericField('bp-macro-min-active-players', 'Macro Minimum Active Players', errors, { integer: true, min: 1 })
            },
            gap: {
                enabled: checkboxValue('bp-gap-enabled'),
                ratingDiffStep: readNumericField('bp-gap-rating-diff-step', 'Gap Rating Diff Step', errors, { integer: true, min: 1 }),
                modifierPerStep: readNumericField('bp-gap-modifier-per-step', 'Gap Modifier Per Step', errors, { min: 0, max: 1 }),
                maxModifier: readNumericField('bp-gap-max-modifier', 'Gap Max Modifier', errors, { min: 0, max: 1 })
            },
            antiTrading: {
                enabled: checkboxValue('bp-anti-enabled'),
                windowHours: readNumericField('bp-anti-window-hours', 'Anti-trading Window Hours', errors, { integer: true, min: 1, max: 720 }),
                allowedRecentMatches: readNumericField('bp-anti-allowed-recent-matches', 'Anti-trading Allowed Recent Matches', errors, { integer: true, min: 0 })
            },
            dodge: {
                enabled: checkboxValue('bp-dodge-enabled'),
                disconnectGraceSeconds: readNumericField('bp-dodge-disconnect-grace-seconds', 'Dodge Disconnect Grace Seconds', errors, { integer: true, min: 0, max: 300 }),
                cooldownMinutes: readNumericField('bp-dodge-cooldown-minutes', 'Dodge Cooldown Minutes', errors, { integer: true, min: 0, max: 1440 }),
                applyInDraftOnly: checkboxValue('bp-dodge-apply-in-draft-only'),
                rejectResetDuringDraft: checkboxValue(READ_ONLY_DODGE_FIELD_ID)
            },
            seasonalReset: {
                schedulerEnabled: checkboxValue('bp-season-scheduler-enabled'),
                softResetMonths: selectedMonths('bp-soft-month-grid'),
                hardResetMonths: selectedMonths('bp-hard-month-grid'),
                hardPriorityOverSoft: checkboxValue('bp-season-hard-priority-over-soft')
            }
        };

        if (payload.base.initialRating != null && payload.base.minRating != null && payload.base.minRating > payload.base.initialRating) {
            errors.push('Min Rating must not be greater than Initial Rating.');
        }

        return {
            payload: payload,
            errors: errors
        };
    }

    async function fetchSettings(force) {
        if (state.loadPromise && !force) {
            return state.loadPromise;
        }
        state.isLoading = true;
        updateActionAvailability();
        setStatus('info', 'Dang tai Solo Ban/Pick rating settings...', true);

        state.loadPromise = apiFetch('/api/admin/ban-pick/rating-settings', { cache: 'no-store' })
            .then(function (response) {
                renderSettings(response);
                if (!state.preview) {
                    renderPreviewPlaceholder('Chon Preview Soft hoac Preview Hard de xem truoc reset. Preview khong mutate DB.');
                }
                clearStatus();
                return response;
            })
            .catch(function (error) {
                renderEmptyDiagnostics();
                renderPreviewPlaceholder('Khong the tai settings. Kiem tra lai API va thu refresh page.');
                setStatus('error', 'Khong the tai rating settings: ' + error.message, true);
                throw error;
            })
            .finally(function () {
                state.isLoading = false;
                state.loadPromise = null;
                updateActionAvailability();
            });

        return state.loadPromise;
    }

    async function handleSaveSettings() {
        var result = collectPayload();
        if (result.errors.length) {
            setStatus('error', result.errors.join('\n'), true);
            emitToast('Validation error. Kiem tra lai form.', 'error');
            return;
        }

        state.isSaving = true;
        setButtonsLoading(SAVE_BUTTON_IDS, true, 'Dang luu...');
        updateActionAvailability();
        setStatus('info', 'Dang luu rating settings...', true);

        try {
            var response = await apiFetch('/api/admin/ban-pick/rating-settings', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(result.payload)
            });
            renderSettings(response);
            renderPreviewPlaceholder('Settings da thay doi. Chay preview lai neu can kiem tra reset.');
            var message = 'Da luu rating settings thanh cong.';
            if (response && response.diagnostics && response.diagnostics.replayAnchorAdvanced) {
                message += ' Replay anchor da duoc advance.';
            }
            setStatus('success', message, true);
            emitToast('Da luu Solo Ban/Pick rating settings.', 'success');
        } catch (error) {
            setStatus('error', 'Khong the luu rating settings: ' + error.message, true);
            emitToast('Khong the luu rating settings.', 'error');
        } finally {
            state.isSaving = false;
            setButtonsLoading(SAVE_BUTTON_IDS, false, 'Dang luu...');
            updateActionAvailability();
        }
    }

    async function handleResetDefaults() {
        if (!window.confirm('Khoi phuc toan bo Solo Ban/Pick rating settings ve mac dinh?')) {
            return;
        }

        state.isResettingDefaults = true;
        setButtonsLoading(RESET_DEFAULT_BUTTON_IDS, true, 'Dang khoi phuc...');
        updateActionAvailability();
        setStatus('info', 'Dang khoi phuc rating settings ve mac dinh...', true);

        try {
            var response = await apiFetch('/api/admin/ban-pick/rating-settings/reset-defaults', {
                method: 'POST'
            });
            renderSettings(response);
            renderPreviewPlaceholder('Da khoi phuc mac dinh. Chay preview lai neu can kiem tra reset.');
            setStatus('success', 'Da khoi phuc rating settings ve mac dinh.', true);
            emitToast('Da khoi phuc settings mac dinh.', 'success');
        } catch (error) {
            setStatus('error', 'Khong the khoi phuc mac dinh: ' + error.message, true);
            emitToast('Khong the khoi phuc settings mac dinh.', 'error');
        } finally {
            state.isResettingDefaults = false;
            setButtonsLoading(RESET_DEFAULT_BUTTON_IDS, false, 'Dang khoi phuc...');
            updateActionAvailability();
        }
    }

    async function handlePreview(type) {
        state.isPreviewLoading = true;
        updateActionAvailability();
        activateTab('seasonal');
        setButtonLoading(PREVIEW_BUTTON_IDS[type], true, type === 'SOFT' ? 'Dang preview SOFT...' : 'Dang preview HARD...');
        setStatus('info', 'Dang preview ' + type + ' seasonal reset...', true);

        try {
            var response = await apiFetch('/api/admin/ban-pick/rank-reset/preview?type=' + encodeURIComponent(type), {
                cache: 'no-store'
            });
            renderPreview(response);
            setStatus('success', 'Preview ' + type + ' da tai xong. DB khong bi mutate.', true);
        } catch (error) {
            renderPreviewPlaceholder('Khong the tai preview ' + type + '.');
            setStatus('error', 'Khong the preview reset: ' + error.message, true);
            emitToast('Preview reset that bai.', 'error');
        } finally {
            state.isPreviewLoading = false;
            setButtonLoading(PREVIEW_BUTTON_IDS[type], false, '');
            updateActionAvailability();
        }
    }

    async function handleExecuteReset() {
        var expected = confirmationText();
        var provided = String((byId('bp-reset-execute-confirmation') || {}).value || '').trim();
        if (provided !== expected) {
            setStatus('error', 'Confirmation text phai chinh xac la "' + expected + '". API execute chua duoc goi.', true);
            emitToast('Confirmation text chua dung.', 'error');
            syncExecuteButtonState();
            return;
        }

        var executeType = byId('bp-reset-execute-type');
        var noteField = byId('bp-reset-execute-note');
        var type = executeType ? executeType.value : 'SOFT';
        var note = noteField ? noteField.value.trim() : '';
        var confirmMessage = 'Ban sap execute ' + type + ' seasonal reset cho Solo Ban/Pick. Tiep tuc?';
        if (!window.confirm(confirmMessage)) {
            return;
        }

        state.isExecuting = true;
        setButtonLoading('bp-reset-execute-btn', true, 'Dang execute...');
        updateActionAvailability();
        setStatus('warning', 'Dang execute ' + type + ' seasonal reset...', true);

        try {
            var response = await apiFetch('/api/admin/ban-pick/rank-reset', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    type: type,
                    confirmationText: provided,
                    note: note || null
                })
            });
            if (response && response.preview) {
                renderPreview(response.preview);
            }
            await fetchSettings(true);
            setStatus('success', 'Execute reset thanh cong. Reset log ID: ' + (response && response.resetLogId != null ? response.resetLogId : '--') + '.', true);
            emitToast('Execute seasonal reset thanh cong.', 'success');
        } catch (error) {
            setStatus('error', 'Khong the execute reset: ' + error.message, true);
            emitToast('Execute reset that bai.', 'error');
        } finally {
            state.isExecuting = false;
            setButtonLoading('bp-reset-execute-btn', false, 'Dang execute...');
            resetExecuteForm();
            updateActionAvailability();
        }
    }

    function bindEvents() {
        queryAll('[data-bp-tab]').forEach(function (button) {
            button.addEventListener('click', function () {
                activateTab(button.dataset.bpTab);
            });
        });

        SAVE_BUTTON_IDS.forEach(function (buttonId) {
            var button = byId(buttonId);
            if (button) {
                button.addEventListener('click', handleSaveSettings);
            }
        });

        RESET_DEFAULT_BUTTON_IDS.forEach(function (buttonId) {
            var button = byId(buttonId);
            if (button) {
                button.addEventListener('click', handleResetDefaults);
            }
        });

        var previewSoft = byId(PREVIEW_BUTTON_IDS.SOFT);
        if (previewSoft) {
            previewSoft.addEventListener('click', function () {
                handlePreview('SOFT');
            });
        }

        var previewHard = byId(PREVIEW_BUTTON_IDS.HARD);
        if (previewHard) {
            previewHard.addEventListener('click', function () {
                handlePreview('HARD');
            });
        }

        var executeButton = byId('bp-reset-execute-btn');
        if (executeButton) {
            executeButton.addEventListener('click', handleExecuteReset);
        }

        var confirmationInput = byId('bp-reset-execute-confirmation');
        if (confirmationInput) {
            confirmationInput.addEventListener('input', syncExecuteButtonState);
        }

        var ratingForm = byId('bp-rating-form');
        if (ratingForm) {
            ratingForm.addEventListener('input', function () {
                if (state.isHydrating) {
                    return;
                }
                if (!state.isLoading && !state.isSaving && !state.isResettingDefaults && !state.isExecuting) {
                    clearStatus();
                }
                syncExecuteButtonState();
            });
            ratingForm.addEventListener('change', function () {
                if (state.isHydrating) {
                    return;
                }
                if (!state.isLoading && !state.isSaving && !state.isResettingDefaults && !state.isExecuting) {
                    clearStatus();
                }
                syncExecuteButtonState();
            });
        }
    }

    function initialize() {
        if (state.initialized) {
            return;
        }
        if (!byId('page-ban-pick-rating')) {
            return;
        }

        buildMonthGrid('bp-soft-month-grid');
        buildMonthGrid('bp-hard-month-grid');
        bindEvents();
        activateTab('base');
        renderEmptyDiagnostics();
        renderPreviewPlaceholder('Chon Preview Soft hoac Preview Hard de xem truoc reset. Preview khong mutate DB.');
        updateSummaryCard('bp-summary-macro-delta', '--');
        updateSummaryCard('bp-summary-active-pool', '--');
        updateSummaryCard('bp-summary-next-reset', '--');
        updateSummaryCard('bp-summary-updated-by', '--');
        syncExecuteButtonState();
        updateActionAvailability();
        state.initialized = true;
    }

    window.loadBanPickRatingControl = function (forceReload) {
        initialize();
        if (!state.initialized) {
            return Promise.resolve(null);
        }
        if (state.hasLoaded && !forceReload) {
            return Promise.resolve(state.settings);
        }
        return fetchSettings(Boolean(forceReload));
    };
}());
