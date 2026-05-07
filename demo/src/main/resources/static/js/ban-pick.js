/* ===================================================================
         *  Hero data - fetched from /api/wiki/heroes (real DB IDs).
         *  Fallback hardcoded list dùng khi API không khả dụng.
         * =================================================================== */
        let heroes = [];
        let heroIdMap = {};   // { heroName -> heroId }  tra cứu nhanh
        let heroByIdMap = {};
        let heroesLoaded = false;

        function getApiHeroPrimaryRoleCode(hero) {
            if (hero && hero.primaryRole && hero.primaryRole.code) return hero.primaryRole.code;
            if (Array.isArray(hero && hero.roles) && hero.roles.length) return hero.roles[0];
            return '';
        }

        function getApiHeroLaneRoleLabels(hero) {
            const labels = [];
            if (hero && hero.primaryRole) labels.push(hero.primaryRole.name || hero.primaryRole.code);
            if (Array.isArray(hero && hero.subRoles)) {
                hero.subRoles.forEach(role => labels.push(role.name || role.code));
            }
            if (!labels.length && Array.isArray(hero && hero.laneRoles)) return hero.laneRoles;
            return labels.filter(Boolean);
        }

        async function loadHeroesFromApi() {
            try {
                const response = await fetch('/api/wiki/heroes', { headers: { 'Accept': 'application/json' } });
                if (!response.ok) throw new Error('HTTP ' + response.status);
                const data = await response.json();
                if (!Array.isArray(data) || data.length === 0) throw new Error('Empty hero list');
                heroes = data.map(h => ({
                    id: h.id,
                    name: h.name,
                    role: getApiHeroPrimaryRoleCode(h),
                    laneRoles: getApiHeroLaneRoleLabels(h),
                    attributes: Array.isArray(h.attributes) ? h.attributes : [],
                    avatarUrl: h.avatarUrl || ''
                }));
                heroIdMap = {};
                heroByIdMap = {};
                heroes.forEach(h => {
                    heroIdMap[h.name] = h.id;
                    heroByIdMap[String(h.id)] = h;
                });
                heroesLoaded = true;
                console.log('[BanPick] Loaded', heroes.length, 'heroes from API');
            } catch (error) {
                console.warn('[BanPick] API fetch failed, hero list is empty:', error.message);
                heroesLoaded = false;
                heroes = [];
                heroIdMap = {};
                heroByIdMap = {};
            }
        }

        function getHeroIdByName(heroName) {
            return heroIdMap[heroName] || null;
        }

        let currentRoleFilter = 'Tất cả';
        let currentMode = null;
        let playerBlue = "Bên Xanh";
        let playerRed = "Bên Đỏ";
        let playerOneName = "Minh Tùng";
        let playerTwoName = "Opponent";
        let sideAssigned = false;
        let diceRolling = false;
        let currentPhaseIndex = 0;
        let selectedPreviewHero = null;
        let currentPhaseSelectedCount = 0;
        let phaseDuration = 60;
        let timerRemaining = phaseDuration;
        let timerInterval = null;
        let blueBans = [];
        let redBans = [];
        let bluePicks = [];
        let redPicks = [];
        let localLineupAdjustmentActive = false;
        let localLineupConfirmed = false;
        let selectedOnlineSeriesType = "BO1";
        let onlineRoom = null;
        let onlineRoomCode = null;
        let onlineShareableUrl = "";
        let onlineFallbackPollInterval = null;
        let onlineTimerInterval = null;
        let onlineStompClient = null;
        let onlineSocketRoomCode = null;
        let onlineSocketConnected = false;
        let onlineSocketClosing = false;
        let onlineInfoMessage = "";
        let onlineErrorMessage = "";
        let onlineNewActionSlots = new Set();
        let onlinePresence = null;
        let suppressOnlineStatusMessage = false;
        let toastTimer = null;
        let lineupDragState = null;
        let selectedLineupSwap = null;

        const draftPhases = [
            { team: "blue", action: "ban", count: 1, label: "Xanh cấm lượt 1" },
            { team: "red", action: "ban", count: 1, label: "Đỏ cấm lượt 1" },
            { team: "blue", action: "ban", count: 1, label: "Xanh cấm lượt 2" },
            { team: "red", action: "ban", count: 1, label: "Đỏ cấm lượt 2" },
            { team: "blue", action: "pick", count: 1, label: "Xanh chọn 1" },
            { team: "red", action: "pick", count: 2, label: "Đỏ chọn 2" },
            { team: "blue", action: "pick", count: 2, label: "Xanh chọn 2" },
            { team: "red", action: "pick", count: 1, label: "Đỏ chọn 1" },
            { team: "red", action: "ban", count: 1, label: "Đỏ cấm lượt 3" },
            { team: "blue", action: "ban", count: 1, label: "Xanh cấm lượt 3" },
            { team: "red", action: "ban", count: 1, label: "Đỏ cấm lượt 4" },
            { team: "blue", action: "ban", count: 1, label: "Xanh cấm lượt 4" },
            { team: "red", action: "pick", count: 1, label: "Đỏ chọn 3" },
            { team: "blue", action: "pick", count: 2, label: "Xanh chọn 3-4" },
            { team: "red", action: "pick", count: 1, label: "Đỏ chọn 4" }
        ];

        const heroImageMap = {
            "Aoi": "Aoi.jpeg",
            "Iggy": "Iggy.jpeg",
            "Arthur": "Athur.jpg",
            "Grakk": "Grakk.png",
            "Alice": "Alice.png",
            "Kahli": "Kahlii.png",
            "Kriknak": "Kriknak.png",
            "Krixi": "Krixi.png",
            "Krizzix": "Krizzix.png",
            "Mina": "Mina.png",
            "Slimz": "Slimz.png",
            "Thane": "Thane.png",
            "Kil'Groth": "Kil'Groth.gif",
            "Lu Bu": "L\u1eef B\u1ed1.jpg",
            "Wukong": "Ng\u1ed9 Kh\u00f4ng.jpg",
            "Zanis": "Tri\u1ec7u V\u00e2n.jpg",
            "Diaochan": "\u0110i\u00eau Thuy\u1ec1n.jpg",
            "Ormarr": "Omarr.jpg",
            "Rourka": "Rourke.jpg",
            "Thorne": "Thorn.jpg",
            "Wonder Woman": "Wonder Women.jpg",
            "Zephys": "Zephis.jpg",
            "Riktor": "Richter.jpg",
            "Flowborn (Marksman)": "Flowborn (ADL).jpg",
            "D'Arcy": "D'Arcy.jpg",
            "Flowborn": "Flowborn (ADL).jpg",
            "Flowborn ADL": "Flowborn (ADL).jpg",
            "Flowborn MID": "Flowborn (MID).jpg",
            "Ngo Khong": "Ng\u1ed9 Kh\u00f4ng.jpg",
            "Trieu Van": "Tri\u1ec7u V\u00e2n.jpg",
            "Dieu Thuyen": "\u0110i\u00eau Thuy\u1ec1n.jpg",
            "Lu Bo": "L\u1eef B\u1ed1.jpg",
            "Roule": "Rouie.jpg",
            "Governa": "Goverra.jpg",
            "Tochi": "Tachi.jpg",
            "Richter": "Richter.jpg",
            "KilGroth": "Kil'Groth.gif"
        };

        function normalizeHeroName(name) {
            const aliases = { "Flowborn": "Flowborn (Marksman)", "Flowborn ADL": "Flowborn (Marksman)", "Flowborn MID": "Flowborn (Marksman)", "Ngo Khong": "Wukong", "Trieu Van": "Zanis", "Dieu Thuyen": "Diaochan", "Lu Bo": "Lu Bu", "Roule": "Rouie", "Governa": "Goverra", "Tochi": "Tachi", "Richter": "Riktor", "KilGroth": "Kil'Groth" };
            const trimmed = String(name || '').trim();
            return aliases[trimmed] || trimmed;
        }

        const teamLabels = {
            blue: "Đội Xanh",
            red: "Đội Đỏ"
        };

        const actionLabels = {
            ban: "Cấm",
            pick: "Chọn"
        };

        const modeLabels = {
            standard: "Cấm chọn tiêu chuẩn",
            free: "Cấm chọn tự do",
            "solo-1v1": "Solo Ban/Pick 1v1 Online"
        };

        function normalizeConfiguredMode(mode) {
            if (!mode) return "";
            const normalized = String(mode).trim().toLowerCase();
            if (normalized === "solo") return "solo-1v1";
            if (normalized === "solo-1v1" || normalized === "standard" || normalized === "free") return normalized;
            return "";
        }

        function getPageMode() {
            return normalizeConfiguredMode(document.body?.dataset?.banPickMode || "");
        }

        function isModeLockedPage() {
            return Boolean(getPageMode());
        }

        function isStandardDraftMode() {
            return currentMode === "standard" || currentMode === "solo-1v1";
        }

        function isSoloOneVOneMode() {
            return currentMode === "solo-1v1";
        }

        function isOnlineLineupAdjustment() {
            return Boolean(isOnlineRoomMode() && onlineRoom?.status === "IN_PROGRESS" && onlineRoom?.phaseType === "LINEUP_ADJUSTMENT");
        }

        function isLocalLineupAdjustment() {
            return Boolean(currentMode === "standard" && localLineupAdjustmentActive);
        }

        function isAnyLineupAdjustment() {
            return isOnlineLineupAdjustment() || isLocalLineupAdjustment();
        }

        async function initApp() {
            // 1. Fetch hero list from API (real DB IDs)
            await loadHeroesFromApi();

            // 2. Render hero grid
            renderHeroGrid();

            // 3. Slot drag-drop setup
            document.querySelectorAll('.slot').forEach(slot => {
                slot.ondragover = allowDrop;
                slot.ondragenter = dragEnter;
                slot.ondragleave = dragLeave;
                slot.ondrop = dropOnSlot;
                slot.ondragstart = dragStartFromSlot;
                slot.ondragend = dragEnd;
            });

            document.body.ondragover = allowDrop;
            document.body.ondrop = dropOutsideSlots;

            renderModeOptions();
            startConfiguredMode();
            document.addEventListener('authChanged', () => {
                if (currentMode !== "solo-1v1") return;
                renderOnlineSetup();
                if (getCurrentAuthUser() && onlineRoomCode) {
                    if (!onlineRoom) {
                        loadOrJoinOnlineRoomFromUrl(onlineRoomCode);
                    } else {
                        startOnlineRealtime(onlineRoomCode);
                    }
                } else {
                    disconnectOnlineSocket();
                    stopOnlineFallbackPolling();
                    stopOnlineTimer();
                }
            });
        }

        function startConfiguredMode() {
            const configuredMode = getPageMode();
            if (!configuredMode) {
                renderDraft();
                initializeOnlineRoomFromUrl();
                return;
            }

            document.querySelectorAll('input[name="draftMode"]').forEach(input => {
                input.checked = input.value === configuredMode;
            });
            renderModeOptions();

            const selector = document.getElementById('mode-selector-panel');
            if (selector) selector.classList.add('is-hidden');

            currentMode = configuredMode;
            resetDraftState();

            if (configuredMode === "free") {
                showDraftBoard();
                renderDraft();
                setStatus("Kéo tướng vào bất kỳ ô cấm/chọn nào để thử nghiệm tự do.", "");
                return;
            }

            if (configuredMode === "solo-1v1") {
                showPvpSetup();
                renderOnlineSetup();
                initializeOnlineRoomFromUrl();
                return;
            }

            showDraftBoard();
            renderDraft();
            setStatus("Đến lượt Xanh cấm 1. Chọn tướng để xem trước rồi bấm Xác nhận.", "");
        }

        function renderHeroGrid() {
            const grid = document.getElementById('hero-grid');
            grid.innerHTML = '';

            if (heroes.length === 0) {
                grid.innerHTML = '<div class="draft-warning" style="grid-column:1/-1">Chưa có dữ liệu tướng từ database. Hãy chạy sql/seed_heroes.sql rồi tải lại trang.</div>';
                return;
            }

            heroes.forEach(hero => {
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'hero-btn';
                btn.id = `hero-${hero.name}`;
                btn.title = `${hero.name} - ${[hero.role, ...(hero.laneRoles || [])].filter(Boolean).join(' / ')}`;
                btn.setAttribute('data-hero-name', hero.name);
                btn.setAttribute('data-role', hero.role);
                btn.setAttribute('data-lane-roles', (hero.laneRoles || []).join(','));
                if (hero.id) btn.setAttribute('data-hero-id', hero.id);

                // Build avatar URL
                const heroNameClean = hero.name.trim();
                const heroNameNormalized = normalizeHeroName(heroNameClean);
                const apiAvatar = hero.avatarUrl;
                const imgFilename = heroImageMap[heroNameNormalized] || `${heroNameNormalized}.jpg`;
                const localImgUrl = `/images/heroes/${encodeURI(imgFilename).replace(/'/g, "%27")}?v=3`;
                const imgUrl = apiAvatar || localImgUrl;

                // Create <img> element inside button
                const imgEl = document.createElement('img');
                imgEl.src = imgUrl;
                imgEl.alt = hero.name;
                imgEl.loading = 'lazy';
                imgEl.draggable = false;
                imgEl.onerror = function () {
                    if (apiAvatar && this.src !== localImgUrl) {
                        this.src = localImgUrl;
                    } else {
                        this.src = '/images/ui/default.png';
                    }
                };
                btn.appendChild(imgEl);

                // Store avatar URL for slot copying
                btn.setAttribute('data-avatar', imgUrl);

                // Hero name label (hidden by default, visible when image fails)
                const nameSpan = document.createElement('span');
                nameSpan.className = 'hero-btn-name';
                nameSpan.textContent = hero.name;
                btn.appendChild(nameSpan);

                btn.onclick = () => selectHero(hero.name);
                btn.ondragstart = (ev) => dragStartFromList(ev, hero.name);
                btn.ondragend = dragEnd;
                grid.appendChild(btn);
            });
        }

        function renderModeOptions() {
            document.querySelectorAll('.mode-option').forEach(option => {
                const input = option.querySelector('input[type="radio"]');
                option.classList.toggle('is-selected', input.checked);
            });
        }

        function setOnlineSeriesType(seriesType) {
            selectedOnlineSeriesType = ["BO1", "BO3", "BO5", "BO7"].includes(seriesType) ? seriesType : "BO1";
            document.querySelectorAll('.series-type-btn').forEach(btn => {
                btn.classList.toggle('is-active', btn.getAttribute('data-series-type') === selectedOnlineSeriesType);
            });
        }

        function beginDraftFromMode() {
            const selectedMode = document.querySelector('input[name="draftMode"]:checked')?.value || "standard";
            currentMode = selectedMode;
            document.getElementById('mode-selector-panel').classList.add('is-hidden');

            if (currentMode === "free") {
                resetDraftState();
                showDraftBoard();
                renderDraft();
                setStatus("Kéo tướng vào bất kỳ ô cấm/chọn nào để thử nghiệm tự do.", "");
                return;
            }

            resetDraftState();

            if (currentMode === "solo-1v1") {
                showPvpSetup();
                renderOnlineSetup();
                return;
            }

            showDraftBoard();
            renderDraft();
            setStatus("Đến lượt Xanh cấm 1. Chọn tướng để xem trước rồi bấm Xác nhận.", "");
        }

        function changeMode() {
            pauseTimer(false);
            disconnectOnlineSocket();
            stopOnlineFallbackPolling();
            stopOnlineTimer();
            currentMode = null;
            onlineRoom = null;
            onlineRoomCode = null;
            onlineShareableUrl = "";
            onlineInfoMessage = "";
            onlineErrorMessage = "";
            resetDraftState();
            document.body.classList.remove('app-layout-active');
            document.getElementById('draft-status-panel').classList.add('is-hidden');
            document.getElementById('draft-board').classList.add('is-hidden');
            document.getElementById('pvp-setup-panel').classList.add('is-hidden');
            document.getElementById('pvp-match-panel').classList.add('is-hidden');
            document.getElementById('online-series-panel').classList.add('is-hidden');
            document.getElementById('draft-summary-panel').classList.add('is-hidden');
            if (isModeLockedPage()) {
                window.location.assign('/ban-pick');
                return;
            }
            document.getElementById('mode-selector-panel').classList.remove('is-hidden');
            if (new URLSearchParams(window.location.search).has('room')) {
                window.history.replaceState({}, "", window.location.pathname);
            }
            resetPvpLobby();
            renderModeOptions();
            renderDraft();
        }

        function showDraftBoard() {
            document.body.classList.add('app-layout-active');
            document.getElementById('pvp-setup-panel').classList.add('is-hidden');
            document.getElementById('draft-summary-panel').classList.add('is-hidden');
            document.getElementById('draft-status-panel').classList.remove('is-hidden');
            document.getElementById('draft-board').classList.remove('is-hidden');
            document.getElementById('pvp-match-panel').classList.toggle('is-hidden', !isSoloOneVOneMode());
            document.getElementById('online-series-panel').classList.toggle('is-hidden', !isOnlineRoomMode());
            renderOnlineSeriesPanel();
        }

        function showPvpSetup() {
            document.body.classList.remove('app-layout-active');
            document.getElementById('draft-status-panel').classList.add('is-hidden');
            document.getElementById('draft-board').classList.add('is-hidden');
            document.getElementById('pvp-match-panel').classList.add('is-hidden');
            document.getElementById('online-series-panel').classList.add('is-hidden');
            document.getElementById('draft-summary-panel').classList.add('is-hidden');
            document.getElementById('pvp-setup-panel').classList.remove('is-hidden');
            renderOnlineSetup();
        }

        function resetPvpLobby() {
            playerBlue = "Bên Xanh";
            playerRed = "Bên Đỏ";
            playerOneName = "Minh Tùng";
            playerTwoName = "Opponent";
            sideAssigned = false;
            diceRolling = false;
            setOnlineSeriesType("BO1");
            renderOnlineSetup();
        }

        function getCurrentAuthUser() {
            return typeof window.getAuthUser === "function" ? window.getAuthUser() : null;
        }

        function isOnlineRoomMode() {
            return currentMode === "solo-1v1";
        }

        function isOnlineParticipant() {
            const user = getCurrentAuthUser();
            if (!onlineRoom || !user?.email) return false;
            return [onlineRoom.hostUser?.email, onlineRoom.guestUser?.email].includes(user.email);
        }

        function isOnlineHost() {
            const user = getCurrentAuthUser();
            return Boolean(onlineRoom?.hostUser?.email && user?.email === onlineRoom.hostUser.email);
        }

        function isCurrentOnlineUserReady() {
            const user = getCurrentAuthUser();
            if (!onlineRoom || !user?.email) return false;
            if (onlineRoom.hostUser?.email === user.email) return Boolean(onlineRoom.hostReady);
            if (onlineRoom.guestUser?.email === user.email) return Boolean(onlineRoom.guestReady);
            return false;
        }

        function getOnlineUserSide() {
            const sideFromRoom = String(onlineRoom?.currentUserSide || "").toLowerCase();
            if (["blue", "red"].includes(sideFromRoom)) return sideFromRoom;
            const user = getCurrentAuthUser();
            if (!onlineRoom || !user?.email) return null;
            if (onlineRoom.blueUser?.email === user.email) return "blue";
            if (onlineRoom.redUser?.email === user.email) return "red";
            return null;
        }

        function isOnlineActivePlayer() {
            if (isOnlineLineupAdjustment()) return false;
            if (isAnyLineupAdjustment()) {
                setStatus("Đang ở giai đoạn sắp xếp đội hình. Không thể cấm hoặc chọn thêm tướng.", "warning");
                return;
            }

            const phase = getCurrentPhase();
            return Boolean(isOnlineRoomMode() && phase && getOnlineUserSide() === phase.team);
        }

        function getOnlineRoomCodeFromUrl() {
            return new URLSearchParams(window.location.search).get('room')?.trim().toUpperCase() || "";
        }

        function getOnlineSocketHeaders() {
            const token = typeof window.getAuthToken === "function" ? window.getAuthToken() : null;
            return token ? { Authorization: `Bearer ${token}` } : {};
        }

        function buildOnlineRoomLink(roomCode) {
            return `${window.location.origin}${window.location.pathname}?room=${encodeURIComponent(roomCode)}`;
        }

        function setOnlineFeedback(message = "", type = "") {
            onlineInfoMessage = type === "error" ? "" : message;
            onlineErrorMessage = type === "error" ? message : "";
            const feedback = document.getElementById('online-room-feedback');
            if (!feedback) return;
            feedback.textContent = message;
            feedback.className = `online-room-feedback ${type}`.trim();
        }

        function setOnlineRealtimeIndicator(message, type = "") {
            const indicator = document.getElementById('online-realtime-indicator');
            if (!indicator) return;
            indicator.textContent = message;
            indicator.className = `online-realtime-indicator ${type}`.trim();
        }

        function applyOnlinePresence(payload) {
            onlinePresence = payload;
            if (!onlineRoom || !payload?.connectedEmails) return;
            const user = getCurrentAuthUser();
            const opponentEmail = user?.email === onlineRoom.hostUser?.email
                ? onlineRoom.guestUser?.email
                : onlineRoom.hostUser?.email;
            const connectedEmails = payload.connectedEmails || [];
            if (opponentEmail && onlineRoom.status === "IN_PROGRESS" && !connectedEmails.includes(opponentEmail)) {
                setOnlineRealtimeIndicator("Đối thủ đã mất kết nối", "error");
                return;
            }
            if (onlineSocketConnected) {
                setOnlineRealtimeIndicator("Realtime đã kết nối", "success");
            }
        }

        function showBanPickToast(message) {
            const toast = document.getElementById('banpick-toast');
            if (!toast) return;
            toast.textContent = message;
            toast.classList.add('is-visible');
            if (toastTimer) clearTimeout(toastTimer);
            toastTimer = setTimeout(() => toast.classList.remove('is-visible'), 1800);
        }

        async function readApiResponse(response) {
            const text = await response.text();
            let data = null;
            try {
                data = text ? JSON.parse(text) : null;
            } catch (error) {
                data = null;
            }
            if (!response.ok) {
                throw new Error(data?.message || data?.error || text || "Không thể xử lý yêu cầu phòng solo.");
            }
            return data;
        }

        async function onlineRoomRequest(path, options = {}) {
            const response = await fetch(path, {
                ...options,
                headers: {
                    'Content-Type': 'application/json',
                    ...(options.headers || {})
                }
            });
            return readApiResponse(response);
        }

        function parseSocketPayload(message) {
            try {
                return message?.body ? JSON.parse(message.body) : null;
            } catch (error) {
                return null;
            }
        }

        function startOnlineRealtime(roomCode) {
            const normalizedRoomCode = String(roomCode || onlineRoomCode || "").trim().toUpperCase();
            if (!normalizedRoomCode || !getCurrentAuthUser()) return;

            if (onlineStompClient && onlineSocketConnected && onlineSocketRoomCode === normalizedRoomCode) {
                return;
            }

            disconnectOnlineSocket();
            onlineSocketRoomCode = normalizedRoomCode;

            if (!window.SockJS || !window.StompJs?.Client) {
                setOnlineRealtimeIndicator("Realtime lỗi, đang dùng polling dự phòng", "warning");
                startOnlineFallbackPolling();
                return;
            }

            onlineSocketClosing = false;
            setOnlineRealtimeIndicator("Đang kết nối realtime...", "warning");
            onlineStompClient = new StompJs.Client({
                webSocketFactory: () => new SockJS('/ws'),
                connectHeaders: getOnlineSocketHeaders(),
                reconnectDelay: 3000,
                debug: () => { },
                onConnect: () => {
                    onlineSocketConnected = true;
                    setOnlineRealtimeIndicator("Realtime đã kết nối", "success");
                    stopOnlineFallbackPolling();
                    onlineStompClient.subscribe(`/topic/ban-pick/${normalizedRoomCode}`, message => {
                        const room = parseSocketPayload(message);
                        if (room) {
                            selectedPreviewHero = null;
                            applyOnlineRoomState(room);
                            if (room.status === "IN_PROGRESS") setStatusForOnlineTurn();
                        }
                    });
                    onlineStompClient.subscribe(`/topic/ban-pick/${normalizedRoomCode}/presence`, message => {
                        const payload = parseSocketPayload(message);
                        if (payload) applyOnlinePresence(payload);
                    });
                    onlineStompClient.subscribe('/user/queue/ban-pick/errors', message => {
                        const payload = parseSocketPayload(message);
                        const errorMessage = payload?.message || "Không thể xử lý thao tác realtime.";
                        selectedPreviewHero = null;
                        renderDraft();
                        setStatus(errorMessage, "error");
                        setOnlineFeedback(errorMessage, "error");
                        fetchOnlineRoomState(false);
                    });
                    fetchOnlineRoomState(false);
                },
                onStompError: frame => {
                    onlineSocketConnected = false;
                    const errorMessage = frame.headers?.message || "Kết nối realtime gặp lỗi.";
                    setOnlineRealtimeIndicator("Realtime lỗi, đang kết nối lại...", "error");
                    setOnlineFeedback(errorMessage, "error");
                    startOnlineFallbackPolling();
                },
                onWebSocketClose: () => {
                    onlineSocketConnected = false;
                    if (!onlineSocketClosing) {
                        setOnlineRealtimeIndicator("Mất kết nối realtime, đang thử kết nối lại...", "warning");
                    }
                    if (!onlineSocketClosing && onlineRoomCode) {
                        startOnlineFallbackPolling();
                    }
                }
            });
            onlineStompClient.activate();
        }

        function disconnectOnlineSocket() {
            onlineSocketClosing = true;
            onlineSocketConnected = false;
            onlineSocketRoomCode = null;
            if (onlineStompClient) {
                onlineStompClient.deactivate();
                onlineStompClient = null;
            }
            onlinePresence = null;
            setOnlineRealtimeIndicator("Realtime chưa kết nối");
            onlineSocketClosing = false;
        }

        function sendOnlineRoomCommand(command, body = {}) {
            if (!onlineRoomCode) return false;
            if (!onlineStompClient || !onlineSocketConnected) {
                startOnlineRealtime(onlineRoomCode);
                setOnlineFeedback("Đang kết nối realtime, vui lòng thử lại sau vài giây.", "error");
                startOnlineFallbackPolling();
                return false;
            }
            onlineStompClient.publish({
                destination: `/app/ban-pick/${encodeURIComponent(onlineRoomCode)}/${command}`,
                body: JSON.stringify(body)
            });
            return true;
        }

        function initializeOnlineRoomFromUrl() {
            const roomCode = getOnlineRoomCodeFromUrl();
            if (!roomCode) return;

            const soloRadio = document.getElementById('mode-solo-1v1');
            if (soloRadio) soloRadio.checked = true;
            currentMode = "solo-1v1";
            onlineRoomCode = roomCode;
            document.getElementById('mode-selector-panel').classList.add('is-hidden');
            showPvpSetup();
            const joinInput = document.getElementById('join-room-code');
            if (joinInput) joinInput.value = roomCode;

            setTimeout(() => {
                renderOnlineSetup();
                if (getCurrentAuthUser()) loadOrJoinOnlineRoomFromUrl(roomCode);
            }, 250);
        }

        async function loadOrJoinOnlineRoomFromUrl(roomCode) {
            try {
                const room = await onlineRoomRequest(`/api/ban-pick/rooms/${encodeURIComponent(roomCode)}`);
                applyOnlineRoomState(room);
                if (!isOnlineParticipant() && room.status === "WAITING") {
                    await joinOnlineRoom(roomCode);
                    return;
                }
                startOnlineRealtime(roomCode);
            } catch (error) {
                await joinOnlineRoom(roomCode);
            }
        }

        function renderOnlineSetup() {
            const loggedIn = Boolean(getCurrentAuthUser());
            document.getElementById('online-login-gate').classList.toggle('is-hidden', loggedIn);
            document.getElementById('online-room-shell').classList.toggle('is-hidden', !loggedIn);

            const roomActions = document.getElementById('online-room-actions');
            const roomCard = document.getElementById('online-room-card');
            roomActions.classList.toggle('is-hidden', Boolean(onlineRoom));
            roomCard.classList.toggle('is-hidden', !onlineRoom);
            renderOnlineSeriesPanel();

            if (!loggedIn) {
                setOnlineFeedback("Bạn cần đăng nhập để tạo hoặc tham gia phòng solo.", "error");
                return;
            }

            if (!onlineRoom) {
                const roomCode = getOnlineRoomCodeFromUrl();
                if (roomCode) document.getElementById('join-room-code').value = roomCode;
                setOnlineFeedback(onlineErrorMessage || onlineInfoMessage || "Tạo phòng mới hoặc nhập mã phòng để tham gia.", onlineErrorMessage ? "error" : "");
                return;
            }

            const roomCode = onlineRoom.roomCode || onlineRoomCode;
            const shareUrl = onlineShareableUrl || buildOnlineRoomLink(roomCode);
            document.getElementById('online-room-code').textContent = roomCode;
            document.getElementById('online-room-link').textContent = shareUrl;
            if (!onlineSocketConnected && onlineRoom.status !== "FINISHED") {
                setOnlineRealtimeIndicator("Đang kết nối realtime...", "warning");
            }
            document.getElementById('online-host-player').textContent = onlineRoom.hostUser?.name || "Đang chờ";
            document.getElementById('online-guest-player').textContent = onlineRoom.guestUser?.name || "Đang chờ đối thủ";
            document.getElementById('online-host-ready').textContent = onlineRoom.hostReady ? "Chủ phòng: Sẵn sàng" : "Chủ phòng: Chưa sẵn sàng";
            document.getElementById('online-guest-ready').textContent = onlineRoom.guestReady ? "Người tham gia: Sẵn sàng" : "Người tham gia: Chưa sẵn sàng";
            document.getElementById('online-blue-player').textContent = `Bên Xanh: ${onlineRoom.blueUser?.name || "Chưa xác định"}`;
            document.getElementById('online-red-player').textContent = `Bên Đỏ: ${onlineRoom.redUser?.name || "Chưa xác định"}`;
            document.getElementById('online-room-status').textContent = onlineRoom.status || "WAITING";
            renderOnlineSeriesPanel();

            const waiting = onlineRoom.status === "WAITING";
            const ready = onlineRoom.status === "READY";
            const bothPlayers = Boolean(onlineRoom.hostUser && onlineRoom.guestUser);
            const sidesAssigned = Boolean(onlineRoom.blueUser && onlineRoom.redUser);
            const bothReady = Boolean(onlineRoom.hostReady && onlineRoom.guestReady);
            const lobbyMessage = !bothPlayers
                ? "Đang chờ đối thủ..."
                : !sidesAssigned
                    ? "Hai người chơi đã vào phòng. Tung xúc xắc chọn bên."
                    : !bothReady
                        ? "Hai người chơi cần bấm Sẵn sàng trước khi bắt đầu."
                        : ready
                            ? "Đã chọn bên và cả hai đã sẵn sàng. Chủ phòng có thể bắt đầu cấm chọn."
                            : onlineRoom.status === "IN_PROGRESS"
                                ? "Draft đang diễn ra."
                                : onlineRoom.status === "FINISHED"
                                    ? "Draft đã hoàn tất."
                                    : "Phòng đã cập nhật.";
            document.getElementById('online-room-message').textContent = lobbyMessage;
            document.getElementById('online-roll-side-btn').disabled = !isOnlineHost() || !bothPlayers || sidesAssigned || onlineRoom.status === "IN_PROGRESS" || onlineRoom.status === "FINISHED";
            document.getElementById('online-ready-room-btn').disabled = !isOnlineParticipant() || isCurrentOnlineUserReady() || onlineRoom.status === "IN_PROGRESS" || onlineRoom.status === "FINISHED";
            document.getElementById('online-start-room-btn').disabled = !isOnlineHost() || !ready || !sidesAssigned || !bothReady;
            setOnlineFeedback(onlineErrorMessage || onlineInfoMessage || lobbyMessage, onlineErrorMessage ? "error" : (onlineInfoMessage ? "success" : ""));

            if (onlineRoom.status === "IN_PROGRESS") {
                syncOnlineRoomToDraft(onlineRoom);
                showDraftBoard();
                renderDraft();
            } else if (onlineRoom.status === "FINISHED") {
                stopOnlineTimer();
                syncOnlineRoomToDraft(onlineRoom);
                showDraftSummary();
                stopOnlineFallbackPolling();
            } else {
                stopOnlineTimer();
            }
        }

        function applyOnlineRoomState(room) {
            onlineRoom = room;
            onlineRoomCode = room?.roomCode || onlineRoomCode;
            if (onlineRoomCode) onlineShareableUrl = buildOnlineRoomLink(onlineRoomCode);
            if (room?.seriesType) setOnlineSeriesType(room.seriesType);
            renderOnlineSetup();
        }

        async function createOnlineRoom() {
            if (!getCurrentAuthUser()) {
                renderOnlineSetup();
                return;
            }
            setOnlineFeedback("Đang tạo phòng...");
            try {
                const payload = await onlineRoomRequest('/api/ban-pick/rooms', {
                    method: 'POST',
                    body: JSON.stringify({ seriesType: selectedOnlineSeriesType })
                });
                onlineShareableUrl = payload.shareableUrl || buildOnlineRoomLink(payload.roomCode);
                applyOnlineRoomState(payload.room);
                window.history.replaceState({}, "", `?room=${encodeURIComponent(payload.roomCode)}`);
                startOnlineRealtime(payload.roomCode);
                showBanPickToast("Đã tạo phòng solo.");
            } catch (error) {
                setOnlineFeedback(error.message, "error");
            }
        }

        async function joinOnlineRoom(roomCodeOverride) {
            if (!getCurrentAuthUser()) {
                renderOnlineSetup();
                return;
            }
            const input = document.getElementById('join-room-code');
            const roomCode = String(roomCodeOverride || input?.value || getOnlineRoomCodeFromUrl()).trim().toUpperCase();
            if (!roomCode) {
                setOnlineFeedback("Nhập mã phòng để tham gia.", "error");
                return;
            }

            setOnlineFeedback("Đang tham gia phòng...");
            try {
                const room = await onlineRoomRequest(`/api/ban-pick/rooms/${encodeURIComponent(roomCode)}/join`, { method: 'POST' });
                applyOnlineRoomState(room);
                window.history.replaceState({}, "", `?room=${encodeURIComponent(room.roomCode)}`);
                startOnlineRealtime(room.roomCode);
                showBanPickToast("Đã tham gia phòng.");
            } catch (error) {
                setOnlineFeedback(error.message, "error");
            }
        }

        async function fetchOnlineRoomState(showErrors = false) {
            if (!onlineRoomCode) return;
            try {
                const room = await onlineRoomRequest(`/api/ban-pick/rooms/${encodeURIComponent(onlineRoomCode)}`);
                applyOnlineRoomState(room);
            } catch (error) {
                if (showErrors) setOnlineFeedback(error.message, "error");
            }
        }

        function startOnlineFallbackPolling() {
            if (!onlineRoomCode || onlineSocketConnected) return;
            stopOnlineFallbackPolling();
            onlineFallbackPollInterval = setInterval(() => fetchOnlineRoomState(false), 2000);
        }

        function stopOnlineFallbackPolling() {
            if (onlineFallbackPollInterval) {
                clearInterval(onlineFallbackPollInterval);
                onlineFallbackPollInterval = null;
            }
        }

        function stopOnlineTimer() {
            if (onlineTimerInterval) {
                clearInterval(onlineTimerInterval);
                onlineTimerInterval = null;
            }
        }

        function startOnlineTimerFromDeadline(room) {
            stopOnlineTimer();
            if (!room || room.status !== "IN_PROGRESS") return;

            const rawDeadline = room.phaseType === "LINEUP_ADJUSTMENT"
                ? room.lineupDeadlineAt
                : room.phaseDeadlineAt;
            if (!rawDeadline) return;

            const deadlineAt = new Date(rawDeadline).getTime();
            const update = () => {
                timerRemaining = Math.max(0, Math.ceil((deadlineAt - Date.now()) / 1000));
                renderTimer();
                if (timerRemaining <= 0) {
                    stopOnlineTimer();
                    setStatus("Hết thời gian. Máy chủ đang tự xử lý lượt này...", "warning");
                }
            };
            update();
            onlineTimerInterval = setInterval(update, 1000);
        }

        async function rollOnlineSide() {
            if (!onlineRoomCode) return;
            setOnlineFeedback("Đang tung xúc xắc chọn bên...");
            if (sendOnlineRoomCommand('roll-side')) {
                showBanPickToast("Đã chọn bên.");
            }
        }

        async function readyOnlineRoom() {
            if (!onlineRoomCode) return;
            setOnlineFeedback("Đang xác nhận sẵn sàng...");
            if (sendOnlineRoomCommand('ready')) {
                showBanPickToast("Đã sẵn sàng.");
            }
        }

        async function startOnlineRoom() {
            if (!onlineRoomCode) return;
            setOnlineFeedback("Đang bắt đầu draft...");
            if (sendOnlineRoomCommand('start')) {
                showDraftBoard();
                setStatusForOnlineTurn();
            }
        }

        async function resetOnlineRoom() {
            if (!onlineRoomCode) return;
            try {
                const room = await onlineRoomRequest(`/api/ban-pick/rooms/${encodeURIComponent(onlineRoomCode)}/reset`, { method: 'POST' });
                selectedPreviewHero = null;
                applyOnlineRoomState(room);
                showPvpSetup();
                startOnlineRealtime(room.roomCode);
                showBanPickToast("Đã reset draft.");
            } catch (error) {
                setStatus(error.message, "error");
            }
        }

        async function nextOnlineGame() {
            if (!onlineRoomCode) return;
            try {
                const room = await onlineRoomRequest(`/api/ban-pick/rooms/${encodeURIComponent(onlineRoomCode)}/next-game`, { method: 'POST' });
                selectedPreviewHero = null;
                applyOnlineRoomState(room);
                showDraftBoard();
                startOnlineRealtime(room.roomCode);
                setStatusForOnlineTurn();
                showBanPickToast("Đã chuyển sang ván tiếp theo.");
            } catch (error) {
                setOnlineFeedback(error.message, "error");
                setStatus(error.message, "error");
            }
        }

        function handleOnlineSeriesSummaryAction() {
            if (!isOnlineRoomMode() || !onlineRoom) return;
            if (onlineRoom.isFinalGame) {
                stopOnlineFallbackPolling();
                stopOnlineTimer();
                showBanPickToast("Series đã kết thúc.");
                return;
            }
            nextOnlineGame();
        }

        async function confirmOnlineSelection() {
            const phase = getCurrentPhase();
            if (!phase || !selectedPreviewHero || !onlineRoomCode) return;
            if (!isOnlineActivePlayer()) {
                setStatus("Đang chờ đối thủ chọn...", "warning");
                return;
            }

            const heroId = getHeroIdByName(selectedPreviewHero);
            const payload = {
                teamSide: phase.team.toUpperCase(),
                actionType: phase.action.toUpperCase(),
                heroName: selectedPreviewHero
            };
            // Gửi heroId nếu có (ưu tiên cao hơn heroName ở backend)
            if (heroId) payload.heroId = heroId;

            const sent = sendOnlineRoomCommand('confirm', payload);
            if (sent) {
                selectedPreviewHero = null;
                renderDraft();
            }
        }

        function copyOnlineRoomLink() {
            const link = onlineShareableUrl || (onlineRoomCode ? buildOnlineRoomLink(onlineRoomCode) : "");
            if (!link) return;
            navigator.clipboard?.writeText(link)
                .then(() => showBanPickToast("Đã copy link phòng."))
                .catch(() => {
                    const temp = document.createElement('textarea');
                    temp.value = link;
                    document.body.appendChild(temp);
                    temp.select();
                    document.execCommand('copy');
                    temp.remove();
                    showBanPickToast("Đã copy link phòng.");
                });
        }

        function getDraftResultLink() {
            const historyId = onlineRoom?.draftHistoryId;
            return historyId ? `${window.location.origin}/ban-pick/result/${historyId}` : "";
        }

        function shareDraftResult() {
            const link = getDraftResultLink();
            if (!link) {
                showBanPickToast("Chưa có kết quả draft để chia sẻ.");
                return;
            }
            navigator.clipboard?.writeText(link)
                .then(() => showBanPickToast("Đã copy link kết quả draft."))
                .catch(() => {
                    const temp = document.createElement('textarea');
                    temp.value = link;
                    document.body.appendChild(temp);
                    temp.select();
                    document.execCommand('copy');
                    temp.remove();
                    showBanPickToast("Đã copy link kết quả draft.");
                });
        }

        function getHeroNameById(heroId) {
            const hero = heroByIdMap[String(heroId)] || heroes.find(h => Number(h.id) === Number(heroId));
            return hero?.name || `Hero #${heroId}`;
        }

        function getHeroById(heroId) {
            return heroByIdMap[String(heroId)] || heroes.find(h => Number(h.id) === Number(heroId)) || null;
        }

        function getActionHeroName(action) {
            return action.hero?.name || action.heroName || getHeroNameById(action.heroId);
        }

        function getHeroByName(heroName) {
            return heroes.find(hero => hero.name === heroName) || null;
        }

        function getHeroFromValue(value) {
            if (value && typeof value === 'object') {
                const id = value.id ?? value.heroId;
                return (id ? getHeroById(id) : null) || getHeroByName(value.name || value.heroName) || value;
            }
            if (typeof value === 'number' || /^\d+$/.test(String(value || ''))) {
                return getHeroById(value);
            }
            return getHeroByName(value);
        }

        function getHeroNameFromValue(value) {
            if (value && typeof value === 'object') {
                return value.name || value.heroName || getHeroNameById(value.heroId ?? value.id);
            }
            if (typeof value === 'number' || /^\d+$/.test(String(value || ''))) {
                return getHeroNameById(value);
            }
            return String(value || '').trim();
        }

        function getHeroAvatarUrl(heroName) {
            const hero = getHeroByName(heroName);
            const normalizedName = normalizeHeroName(String(heroName || '').trim());
            const imgFilename = heroImageMap[normalizedName] || `${normalizedName}.jpg`;
            const localImgUrl = `/images/heroes/${encodeURI(imgFilename).replace(/'/g, "%27")}?v=3`;
            return hero?.avatarUrl || localImgUrl;
        }

        function getHeroIdFromDisplayName(heroName) {
            const directId = getHeroIdByName(heroName) || getHeroByName(heroName)?.id;
            if (directId) return directId;
            const fallbackMatch = String(heroName || "").match(/^Hero #(\d+)$/);
            return fallbackMatch ? Number(fallbackMatch[1]) : null;
        }

        function getHeroIdSet(heroIds) {
            return new Set((Array.isArray(heroIds) ? heroIds : []).map(id => String(id)));
        }

        function getOnlineUsedHeroIdsByTeam(team) {
            if (!onlineRoom || !team) return [];
            const normalizedTeam = String(team).toLowerCase();
            const usedHeroesByTeam = onlineRoom.usedHeroesByTeam || {};
            const direct = usedHeroesByTeam[normalizedTeam];
            const enumKey = usedHeroesByTeam[normalizedTeam.toUpperCase()];
            const legacy = normalizedTeam === "blue" ? onlineRoom.blueUsedPicks : onlineRoom.redUsedPicks;
            const heroIds = Array.isArray(direct) ? direct : (Array.isArray(enumKey) ? enumKey : legacy);
            return Array.isArray(heroIds) ? heroIds : [];
        }

        function getActiveSideRestrictedHeroIds() {
            if (!isOnlineRoomMode() || !onlineRoom || onlineRoom.status !== "IN_PROGRESS" || onlineRoom.bo7ResetActive) {
                return new Set();
            }
            const phase = getCurrentPhase();
            if (!phase || phase.action !== "pick") {
                return new Set();
            }
            return getHeroIdSet(getOnlineUsedHeroIdsByTeam(phase.team));
        }

        function isHeroRestrictedForCurrentOnlinePick(heroName) {
            const heroId = getHeroIdByName(heroName) || getHeroByName(heroName)?.id;
            return Boolean(heroId && getActiveSideRestrictedHeroIds().has(String(heroId)));
        }

        function escapeBanPickHtml(value) {
            return String(value ?? '').replace(/[&<>"']/g, char => ({
                '&': '&amp;',
                '<': '&lt;',
                '>': '&gt;',
                '"': '&quot;',
                "'": '&#39;'
            }[char]));
        }

        function getHeroIconData(value) {
            const hero = getHeroFromValue(value);
            const heroName = getHeroNameFromValue(hero || value);
            return {
                name: heroName || 'Hero',
                avatarUrl: hero?.avatarUrl || getHeroAvatarUrl(heroName) || '/images/ui/default.png'
            };
        }

        function renderHeroIcon(value, small = false) {
            const hero = getHeroIconData(value);
            return `<img class="hero-icon${small ? ' small' : ''}" src="${escapeBanPickHtml(hero.avatarUrl)}" alt="${escapeBanPickHtml(hero.name)}" title="${escapeBanPickHtml(hero.name)}" loading="lazy" onerror="this.onerror=null;this.src='/images/ui/default.png'">`;
        }

        function formatHeroIconList(values, options = {}) {
            const heroes = (Array.isArray(values) ? values : []).filter(value => value !== undefined && value !== null && value !== '');
            if (!heroes.length) return '<span class="hero-icon-empty">Chưa có</span>';
            const className = `hero-icon-list${options.used ? ' used-heroes-grid' : ''}`;
            return `<span class="${className}">${heroes.map(value => renderHeroIcon(value, Boolean(options.small))).join('')}</span>`;
        }

        function formatUsedHeroIcons(heroIds) {
            return formatHeroIconList(heroIds, { small: true, used: true });
        }

        function formatUsedPickChips(heroIds) {
            const ids = Array.isArray(heroIds) ? heroIds : [];
            if (!ids.length) return 'Chưa có';
            return ids.map(heroId => '<span class="used-pick-chip">' + escapeBanPickHtml(getHeroNameById(heroId)) + '</span>').join('');
        }

        function renderOnlineSeriesPanel() {
            const overviewPanel = document.getElementById('online-series-panel');
            const blueUsedPanel = document.getElementById('online-blue-used-panel');
            const redUsedPanel = document.getElementById('online-red-used-panel');
            const panels = [overviewPanel, blueUsedPanel, redUsedPanel].filter(Boolean);
            if (!panels.length) return;
            const draftVisible = !document.getElementById('draft-status-panel')?.classList.contains('is-hidden');
            const summaryVisible = !document.getElementById('draft-summary-panel')?.classList.contains('is-hidden');
            const shouldShow = isOnlineRoomMode() && Boolean(onlineRoom) && (draftVisible || summaryVisible);
            panels.forEach(panel => panel.classList.toggle('is-hidden', !shouldShow));
            if (!shouldShow) return;

            const seriesType = onlineRoom.seriesType || "BO1";
            const currentGame = onlineRoom.currentGameNumber || 1;
            const maxGames = onlineRoom.maxGames || 1;
            document.getElementById('online-series-type-label').textContent = seriesType;
            document.getElementById('online-series-game-label').textContent = `Ván ${currentGame} / ${maxGames}`;
            const resetLabel = document.getElementById('online-series-reset-label');
            resetLabel.hidden = !onlineRoom.bo7ResetActive;
            document.getElementById('online-blue-used-picks').innerHTML = formatUsedHeroIcons(getOnlineUsedHeroIdsByTeam("blue"));
            document.getElementById('online-red-used-picks').innerHTML = formatUsedHeroIcons(getOnlineUsedHeroIdsByTeam("red"));
        }

        function syncOnlineRoomToDraft(room) {
            currentMode = "solo-1v1";
            playerBlue = room.blueUser?.name || "Bên Xanh";
            playerRed = room.redUser?.name || "Bên Đỏ";
            phaseDuration = room.phaseDurationSeconds || 60;
            currentPhaseIndex = room.currentPhaseIndex || 0;
            currentPhaseSelectedCount = room.currentPhaseSelectedCount || 0;
            blueBans = [];
            redBans = [];
            bluePicks = [];
            redPicks = [];
            onlineNewActionSlots = new Set();

            (room.actions || []).forEach(action => {
                const team = String(action.teamSide || "").toLowerCase();
                const type = String(action.actionType || "").toLowerCase();
                if (!["blue", "red"].includes(team) || !["ban", "pick"].includes(type)) return;
                const heroName = getActionHeroName(action);
                const collection = getCollection(team, type);
                const index = collection.length;
                collection.push(heroName);
                if (action.isNew) {
                    onlineNewActionSlots.add(getSlotId(team, type, index));
                }
            });

            if (Array.isArray(room.bluePickOrder) && room.bluePickOrder.length) {
                bluePicks = room.bluePickOrder.map(getHeroNameById).filter(Boolean);
            }
            if (Array.isArray(room.redPickOrder) && room.redPickOrder.length) {
                redPicks = room.redPickOrder.map(getHeroNameById).filter(Boolean);
            }

            const rawDeadline = room.phaseType === "LINEUP_ADJUSTMENT" ? room.lineupDeadlineAt : room.phaseDeadlineAt;
            const deadlineAt = rawDeadline ? new Date(rawDeadline).getTime() : null;
            const startedAt = room.timerStartedAt ? new Date(room.timerStartedAt).getTime() : null;
            if (room.status === "IN_PROGRESS" && deadlineAt) {
                timerRemaining = Math.max(0, Math.ceil((deadlineAt - Date.now()) / 1000));
            } else if (room.status === "IN_PROGRESS" && startedAt) {
                const elapsed = Math.floor((Date.now() - startedAt) / 1000);
                const countdownDuration = room.phaseType === "LINEUP_ADJUSTMENT" ? 30 : phaseDuration;
                timerRemaining = Math.max(0, countdownDuration - elapsed);
            } else {
                timerRemaining = room.phaseType === "LINEUP_ADJUSTMENT" ? 30 : phaseDuration;
            }
            startOnlineTimerFromDeadline(room);
            renderOnlineSeriesPanel();
        }

        function setStatusForOnlineTurn() {
            if (isOnlineLineupAdjustment()) {
                setStatus("Sắp xếp đội hình. Hai đội có 30 giây để đưa tướng về đúng vị trí.", "");
                return;
            }
            const phase = getCurrentPhase();
            if (!phase) return;
            if (isOnlineActivePlayer()) {
                setStatus(`Lượt của ${getActivePlayerName()} - ${getActiveSideLabel()}. Chọn tướng rồi bấm Xác nhận.`, "");
            } else {
                setStatus("Đang chờ đối thủ chọn...", "warning");
            }
        }

        function resetDraftState() {
            pauseTimer(false);
            currentPhaseIndex = 0;
            selectedPreviewHero = null;
            currentPhaseSelectedCount = 0;
            timerRemaining = phaseDuration;
            blueBans = [];
            redBans = [];
            bluePicks = [];
            redPicks = [];
            localLineupAdjustmentActive = false;
            localLineupConfirmed = false;
            lineupDragState = null;
            selectedLineupSwap = null;
            document.getElementById('searchInput').value = '';
            currentRoleFilter = 'Tất cả';
            document.querySelectorAll('.role-btn').forEach(btn => {
                const value = btn.getAttribute('data-role-filter') || btn.textContent.trim();
                btn.classList.toggle('active', value === currentRoleFilter);
            });
        }

        function getCurrentPhase() {
            if (!isStandardDraftMode() || isAnyLineupAdjustment()) return null;
            return draftPhases[currentPhaseIndex] || null;
        }

        function getActivePlayerName() {
            const phase = getCurrentPhase();
            if (!phase || !isSoloOneVOneMode()) return "";
            return phase.team === "blue" ? playerBlue : playerRed;
        }

        function getActiveSideLabel() {
            const phase = getCurrentPhase();
            if (!phase) return "";
            return phase.team === "blue" ? "Bên Xanh" : "Bên Đỏ";
        }

        function isTimerRunning() {
            return timerInterval !== null;
        }

        function getCollection(team, action) {
            if (team === "blue" && action === "ban") return blueBans;
            if (team === "blue" && action === "pick") return bluePicks;
            if (team === "red" && action === "ban") return redBans;
            return redPicks;
        }

        function getAllDraftHeroes() {
            return [...blueBans, ...redBans, ...bluePicks, ...redPicks].filter(Boolean);
        }

        function getUsedHeroes() {
            return new Set(getAllDraftHeroes());
        }

        function getHeroUsageState(heroName) {
            if (blueBans.includes(heroName) || redBans.includes(heroName)) return 'ban';
            if (bluePicks.includes(heroName) || redPicks.includes(heroName)) return 'pick';
            return '';
        }

        function hasAnySelections() {
            return getAllDraftHeroes().length > 0 || Boolean(selectedPreviewHero);
        }

        function isHeroUsed(heroName) {
            return getUsedHeroes().has(heroName);
        }

        function setStatus(message, type = "") {
            const status = document.getElementById('status');
            status.textContent = message;
            status.className = `draft-warning ${type}`.trim();
        }

        function getSlotId(team, action, index) {
            return `${team}-${action}-${index}`;
        }

        function parseSlotId(slotId) {
            const match = String(slotId || '').match(/^(blue|red)-(ban|pick)-(\d+)$/);
            if (!match) return null;
            return { team: match[1], action: match[2], index: Number(match[3]) };
        }

        function trimEmptyTail(collection) {
            while (collection.length && !collection[collection.length - 1]) {
                collection.pop();
            }
        }

        function removeHeroFromSlot(slotId) {
            const slotInfo = parseSlotId(slotId);
            if (!slotInfo) return;
            const collection = getCollection(slotInfo.team, slotInfo.action);
            collection[slotInfo.index] = undefined;
            trimEmptyTail(collection);
        }

        function clearSlot(slot) {
            slot.textContent = '';
            slot.style.backgroundImage = '';
            slot.style.backgroundSize = '';
            slot.style.color = '';
            slot.style.textShadow = '';
            slot.removeAttribute('data-hero-name');
            slot.removeAttribute('aria-label');
            slot.removeAttribute('title');
            slot.classList.remove('pick-blue', 'pick-red', 'banned', 'active', 'drag-over', 'lock-new', 'lock-new-ban', 'lock-new-pick');
        }

        function fillSlot(slot, heroName, team, action, isNew = false) {
            slot.textContent = '';
            slot.setAttribute('data-hero-name', heroName);
            slot.setAttribute('aria-label', heroName);
            slot.setAttribute('title', heroName);

            const heroBtn = document.getElementById(`hero-${heroName}`);
            if (heroBtn) {
                const avatarUrl = heroBtn.getAttribute('data-avatar');
                if (avatarUrl) {
                    slot.style.backgroundImage = `url("${avatarUrl}")`;
                    slot.style.backgroundSize = 'cover';
                    slot.style.color = 'transparent';
                    slot.style.textShadow = 'none';
                }
            }

            if (action === 'ban') {
                slot.classList.add('banned');
            } else {
                slot.classList.add(team === 'blue' ? 'pick-blue' : 'pick-red');
            }
            if (isNew) {
                slot.classList.add('lock-new', action === 'ban' ? 'lock-new-ban' : 'lock-new-pick');
            }
        }

        function renderSlots() {
            document.querySelectorAll('.slot').forEach(slot => {
                clearSlot(slot);
                slot.draggable = currentMode === "free";
            });

            [
                { team: "blue", action: "ban", collection: blueBans },
                { team: "red", action: "ban", collection: redBans },
                { team: "blue", action: "pick", collection: bluePicks },
                { team: "red", action: "pick", collection: redPicks }
            ].forEach(group => {
                group.collection.forEach((heroName, index) => {
                    if (!heroName) return;
                    const slot = document.getElementById(getSlotId(group.team, group.action, index));
                    if (slot) fillSlot(slot, heroName, group.team, group.action, onlineNewActionSlots.has(slot.id));
                });
            });

            if (onlineNewActionSlots.size) {
                onlineNewActionSlots = new Set();
            }

            const phase = getCurrentPhase();
            if (!phase) return;

            const collection = getCollection(phase.team, phase.action);
            const startIndex = collection.length;
            const remaining = phase.count - currentPhaseSelectedCount;
            for (let offset = 0; offset < remaining; offset += 1) {
                const activeSlot = document.getElementById(getSlotId(phase.team, phase.action, startIndex + offset));
                if (activeSlot) activeSlot.classList.add('active');
            }
        }

        function getCurrentLineupSide() {
            if (isOnlineLineupAdjustment()) return getOnlineUserSide();
            if (isLocalLineupAdjustment()) return "blue";
            return null;
        }

        function getLineupPicks(team) {
            return team === "blue" ? bluePicks : redPicks;
        }

        function isLineupSideConfirmed(team) {
            if (isOnlineLineupAdjustment()) {
                return team === "blue"
                    ? Boolean(onlineRoom?.blueLineupConfirmed)
                    : Boolean(onlineRoom?.redLineupConfirmed);
            }
            return Boolean(localLineupConfirmed);
        }

        function getOpponentLineupStatus(team) {
            const opponent = team === "blue" ? "red" : "blue";
            return isLineupSideConfirmed(opponent) ? "Đối thủ: Đã xác nhận" : "Đối thủ: Chưa xác nhận";
        }

        function canEditLineupSide(team) {
            if (!isAnyLineupAdjustment() || !team) return false;
            if (isOnlineLineupAdjustment()) {
                if (onlineRoom?.status !== "IN_PROGRESS") return false;
                if (isLineupSideConfirmed(team)) return false;
                return getOnlineUserSide() === team;
            }
            return currentMode === "standard" && team === getCurrentLineupSide() && !localLineupConfirmed;
        }

        function applyLineupOrder(team, nextCollection, shouldSubmit = false) {
            if (team === "blue") {
                bluePicks = nextCollection;
            } else {
                redPicks = nextCollection;
            }
            selectedLineupSwap = null;
            renderDraft();
            if (shouldSubmit && isOnlineLineupAdjustment()) {
                submitOnlineLineupOrder(team);
            }
        }

        function moveLineupHero(team, index, direction, event) {
            if (event) event.stopPropagation();
            if (!canEditLineupSide(team)) return;
            const collection = getLineupPicks(team);
            const nextIndex = index + direction;
            if (!Array.isArray(collection) || nextIndex < 0 || nextIndex >= collection.length) return;
            const nextCollection = [...collection];
            const currentHero = nextCollection[index];
            nextCollection[index] = nextCollection[nextIndex];
            nextCollection[nextIndex] = currentHero;
            applyLineupOrder(team, nextCollection, true);
        }

        function swapLineupHeroes(team, firstIndex, secondIndex) {
            if (!canEditLineupSide(team) || firstIndex === secondIndex) return;
            const collection = getLineupPicks(team);
            if (!Array.isArray(collection) || !collection[firstIndex] || !collection[secondIndex]) return;
            const nextCollection = [...collection];
            const firstHero = nextCollection[firstIndex];
            nextCollection[firstIndex] = nextCollection[secondIndex];
            nextCollection[secondIndex] = firstHero;
            applyLineupOrder(team, nextCollection, true);
        }

        function selectLineupSwap(team, index) {
            if (!canEditLineupSide(team)) return;
            if (selectedLineupSwap?.team === team && selectedLineupSwap.index !== index) {
                swapLineupHeroes(team, selectedLineupSwap.index, index);
                return;
            }
            selectedLineupSwap = selectedLineupSwap?.team === team && selectedLineupSwap.index === index
                ? null
                : { team, index };
            renderDraft();
        }

        function handleLineupCardKeydown(event, team, index) {
            if (event.key !== "Enter" && event.key !== " ") return;
            event.preventDefault();
            selectLineupSwap(team, index);
        }

        function dragStartLineupHero(event, team, index) {
            if (!canEditLineupSide(team)) {
                event.preventDefault();
                return;
            }
            lineupDragState = { team, index };
            event.dataTransfer.effectAllowed = "move";
            event.dataTransfer.setData("text/plain", `${team}:${index}`);
            setTimeout(() => event.currentTarget?.classList.add('is-dragging'), 0);
        }

        function dragOverLineupHero(event) {
            event.preventDefault();
            event.currentTarget?.classList.add('is-drop-target');
            if (event.dataTransfer) event.dataTransfer.dropEffect = "move";
        }

        function dragLeaveLineupHero(event) {
            event.currentTarget?.classList.remove('is-drop-target');
        }

        function dragEndLineupHero(event) {
            event.currentTarget?.classList.remove('is-dragging');
            document.querySelectorAll('.lineup-order-item.is-drop-target').forEach(item => item.classList.remove('is-drop-target'));
            lineupDragState = null;
        }

        function dropLineupHero(event, team, targetIndex) {
            event.preventDefault();
            event.stopPropagation();
            event.currentTarget?.classList.remove('is-drop-target');
            const data = event.dataTransfer?.getData("text/plain") || "";
            const [dragTeam, dragIndexRaw] = data.split(":");
            const sourceTeam = lineupDragState?.team || dragTeam;
            const sourceIndex = lineupDragState?.index ?? Number.parseInt(dragIndexRaw, 10);
            if (sourceTeam !== team || !Number.isInteger(sourceIndex) || sourceIndex === targetIndex || !canEditLineupSide(team)) return;

            const collection = getLineupPicks(team);
            if (!Array.isArray(collection) || sourceIndex < 0 || sourceIndex >= collection.length || targetIndex < 0 || targetIndex >= collection.length) return;
            const nextCollection = [...collection];
            const [movedHero] = nextCollection.splice(sourceIndex, 1);
            nextCollection.splice(targetIndex, 0, movedHero);
            applyLineupOrder(team, nextCollection, true);
        }

        function getCurrentLineupIds(team) {
            const picks = getLineupPicks(team);
            return picks.map(getHeroIdFromDisplayName).filter(Boolean);
        }

        async function submitOnlineLineupOrder(team, options = {}) {
            if (!onlineRoomCode || !isOnlineLineupAdjustment() || getOnlineUserSide() !== team) return false;
            const heroIds = getCurrentLineupIds(team);
            const payload = {
                teamSide: team.toUpperCase(),
                heroIds
            };
            if (onlineSocketConnected && onlineStompClient) {
                return sendOnlineRoomCommand('lineup/reorder', payload);
            }
            try {
                const room = await onlineRoomRequest(`/api/ban-pick/rooms/${encodeURIComponent(onlineRoomCode)}/lineup/reorder`, {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });
                applyOnlineRoomState(room);
                return true;
            } catch (error) {
                if (!options.silent) setOnlineFeedback(error.message, "error");
                return false;
            }
        }

        function renderLineupList(team) {
            const container = document.getElementById('lineup-order-list');
            if (!container) return;
            const picks = getLineupPicks(team);
            const canEdit = canEditLineupSide(team);
            const confirmed = isLineupSideConfirmed(team);

            container.innerHTML = picks.map((heroName, index) => {
                const selected = selectedLineupSwap?.team === team && selectedLineupSwap.index === index;
                const avatarUrl = getHeroAvatarUrl(heroName);
                return `
                    <div class="lineup-order-item ${canEdit ? 'is-editable' : 'is-locked'} ${selected ? 'is-selected' : ''}"
                         draggable="${canEdit}"
                         tabindex="${canEdit ? '0' : '-1'}"
                         role="button"
                         aria-label="Slot ${index + 1}: ${escapeBanPickHtml(heroName)}"
                         ondragstart="dragStartLineupHero(event, '${team}', ${index})"
                         ondragover="dragOverLineupHero(event)"
                         ondragleave="dragLeaveLineupHero(event)"
                         ondrop="dropLineupHero(event, '${team}', ${index})"
                         ondragend="dragEndLineupHero(event)"
                         onkeydown="handleLineupCardKeydown(event, '${team}', ${index})"
                         onclick="selectLineupSwap('${team}', ${index})">
                        <span class="lineup-order-position">${index + 1}</span>
                        <img class="lineup-order-avatar" src="${escapeBanPickHtml(avatarUrl)}" alt="${escapeBanPickHtml(heroName)}">
                        <div class="lineup-order-text">
                            <strong>${escapeBanPickHtml(heroName)}</strong>
                            <small>Slot ${index + 1}</small>
                        </div>
                        <div class="lineup-order-actions" onclick="event.stopPropagation()">
                            <button type="button" class="lineup-move-btn" aria-label="Đưa lên" onclick="moveLineupHero('${team}', ${index}, -1, event)" ${!canEdit || index === 0 ? 'disabled' : ''}>&uarr;</button>
                            <button type="button" class="lineup-move-btn" aria-label="Đưa xuống" onclick="moveLineupHero('${team}', ${index}, 1, event)" ${!canEdit || index === picks.length - 1 ? 'disabled' : ''}>&darr;</button>
                        </div>
                    </div>
                `;
            }).join('');

            if (!picks.length) {
                container.innerHTML = '<div class="draft-warning">Chưa có đủ tướng để xác nhận đội hình.</div>';
            }

            const stateLabel = document.getElementById('lineup-confirmed-state');
            if (stateLabel) {
                stateLabel.textContent = confirmed ? "Đã xác nhận" : (canEdit ? "Bạn có thể sắp xếp" : "Đang chờ");
            }
        }

        function renderLineupPanel() {
            const panel = document.getElementById('lineup-adjustment-panel');
            const modal = document.getElementById('lineup-modal');
            const actionBar = document.getElementById('lineup-action-bar');
            const visible = isAnyLineupAdjustment();
            panel.classList.toggle('is-hidden', !visible);
            actionBar?.classList.toggle('is-hidden', !visible);
            document.getElementById('lineup-adjustment-notice').classList.toggle('is-hidden', !visible);
            if (!visible) {
                selectedLineupSwap = null;
                return;
            }

            const currentSide = getCurrentLineupSide();
            const blueConfirmed = isOnlineLineupAdjustment() ? Boolean(onlineRoom?.blueLineupConfirmed) : Boolean(localLineupConfirmed);
            const redConfirmed = isOnlineLineupAdjustment() ? Boolean(onlineRoom?.redLineupConfirmed) : Boolean(localLineupConfirmed);
            document.getElementById('blue-lineup-status').textContent = `Đội Xanh: ${blueConfirmed ? 'Đã xác nhận' : 'Chưa xác nhận'}`;
            document.getElementById('red-lineup-status').textContent = `Đội Đỏ: ${redConfirmed ? 'Đã xác nhận' : 'Chưa xác nhận'}`;

            if (!currentSide) {
                document.getElementById('lineup-team-label').textContent = "Đội của bạn";
                document.getElementById('lineup-order-list').innerHTML = '<div class="draft-warning">Không xác định được đội của bạn trong phòng này.</div>';
                document.getElementById('lineup-confirm-btn').disabled = true;
                return;
            }

            modal.classList.toggle('blue', currentSide === "blue");
            modal.classList.toggle('red', currentSide === "red");
            actionBar.classList.toggle('blue', currentSide === "blue");
            actionBar.classList.toggle('red', currentSide === "red");
            document.getElementById('lineup-team-label').textContent = currentSide === "blue" ? "Đội Xanh" : "Đội Đỏ";

            const currentConfirmed = isLineupSideConfirmed(currentSide);
            document.getElementById('lineup-confirmed-message').classList.toggle('is-hidden', !currentConfirmed);
            renderLineupList(currentSide);

            const countdown = Math.max(0, Number.parseInt(timerRemaining, 10) || 0);
            document.getElementById('lineup-bar-countdown').textContent = `Còn ${countdown}s`;
            document.getElementById('lineup-opponent-status').textContent = getOpponentLineupStatus(currentSide);
            const confirmBtn = document.getElementById('lineup-confirm-btn');
            confirmBtn.disabled = currentConfirmed || !canEditLineupSide(currentSide);
            confirmBtn.textContent = currentConfirmed ? "Đã xác nhận" : "Xác nhận đội hình";
        }

        function renderHeroButtons() {
            const usedHeroes = getUsedHeroes();
            const draftNotStarted = currentMode === null;
            const standardComplete = isStandardDraftMode() && !getCurrentPhase();
            const onlineBlocked = isOnlineRoomMode() && onlineRoom?.status === "IN_PROGRESS" && !isOnlineActivePlayer();
            const lineupBlocked = isAnyLineupAdjustment();

            document.querySelectorAll('.hero-btn').forEach(btn => {
                const heroName = btn.getAttribute('data-hero-name') || btn.innerText.trim();
                const isUsed = usedHeroes.has(heroName);
                const isSeriesRestricted = !isUsed && isHeroRestrictedForCurrentOnlinePick(heroName);
                const isPreview = selectedPreviewHero === heroName;
                const usageState = getHeroUsageState(heroName);
                btn.classList.toggle('selected', isUsed);
                btn.classList.toggle('is-banned', usageState === 'ban');
                btn.classList.toggle('is-picked', usageState === 'pick');
                btn.classList.toggle('series-restricted', isSeriesRestricted);
                btn.classList.toggle('preview-selected', isPreview);
                btn.setAttribute('data-usage-label', usageState === 'ban' ? 'Đã cấm' : (usageState === 'pick' ? 'Đã chọn' : ''));
                btn.disabled = isUsed || isSeriesRestricted || draftNotStarted || standardComplete || onlineBlocked || lineupBlocked;
                btn.draggable = !btn.disabled;
            });
            filterHeroes();
        }

        function renderPreview() {
            const dock = document.getElementById('confirm-dock');
            const previewLabel = document.getElementById('preview-label');
            const previewActionLabel = document.getElementById('preview-action-label');
            const confirmBtn = document.getElementById('confirm-preview-btn');
            const cancelBtn = document.getElementById('cancel-preview-btn');
            const hasPreview = Boolean(selectedPreviewHero);
            const onlineBlocked = isOnlineRoomMode() && onlineRoom?.status === "IN_PROGRESS" && !isOnlineActivePlayer();
            const lineupActive = isAnyLineupAdjustment();

            if (previewActionLabel) {
                if (lineupActive) {
                    previewActionLabel.textContent = "Đội hình";
                } else if (currentMode === "free") {
                    previewActionLabel.textContent = "Tự do";
                } else {
                    previewActionLabel.textContent = getCurrentPhase()?.action === "ban" ? "Cấm" : "Chọn";
                }
            }

            if (lineupActive) {
                dock.classList.add('is-hidden');
                return;
            }

            dock.classList.toggle('is-hidden', currentMode === null || currentMode === "free" || !getCurrentPhase());
            previewLabel.textContent = hasPreview ? `Đang chọn: ${selectedPreviewHero}` : "Đang chọn: Chưa chọn tướng";
            confirmBtn.disabled = !hasPreview || !isStandardDraftMode() || onlineBlocked;
            cancelBtn.disabled = !hasPreview || !isStandardDraftMode() || onlineBlocked;
        }

        function syncConfirmDockState() {
            const confirmBtn = document.getElementById('confirm-preview-btn');
            const cancelBtn = document.getElementById('cancel-preview-btn');
            if (!confirmBtn || !cancelBtn) return;

            if (isAnyLineupAdjustment()) {
                confirmBtn.textContent = "Xác nhận đội hình";
                cancelBtn.hidden = true;
                return;
            }

            confirmBtn.textContent = getCurrentPhase()?.action === "ban" ? "Xác nhận cấm" : "Xác nhận chọn";
            cancelBtn.hidden = false;
        }

        function renderPhaseStatus() {
            const phase = getCurrentPhase();
            const statusPanel = document.getElementById('draft-status-panel');
            const modeLabel = document.getElementById('current-mode-label');
            const progress = document.getElementById('phase-progress');
            const label = document.getElementById('phase-label');
            const team = document.getElementById('phase-team');
            const action = document.getElementById('phase-action');
            const remaining = document.getElementById('phase-remaining');

            statusPanel.classList.toggle('free-mode', currentMode === "free");
            statusPanel.classList.toggle('online-mode', isOnlineRoomMode());
            statusPanel.classList.toggle('online-waiting', isOnlineRoomMode() && onlineRoom?.status === "IN_PROGRESS" && !isOnlineActivePlayer());
            statusPanel.classList.toggle('red-active', phase?.team === 'red');
            document.querySelector('.team-panel.blue').classList.toggle('is-active', phase?.team === 'blue');
            document.querySelector('.team-panel.red').classList.toggle('is-active', phase?.team === 'red');
            modeLabel.textContent = currentMode
                ? `Chế độ: ${modeLabels[currentMode]}${isOnlineRoomMode() && onlineRoom ? `   ${onlineRoom.seriesType || 'BO1'}   Ván ${onlineRoom.currentGameNumber || 1}/${onlineRoom.maxGames || 1}` : ''}`
                : "Chưa chọn chế độ";

            if (currentMode === "free") {
                progress.textContent = "Tự do";
                label.textContent = "Cấm chọn tự do";
                team.textContent = "Hai đội";
                team.className = "phase-chip neutral";
                action.textContent = "Tự do";
                remaining.textContent = "Không giới hạn";
                return;
            }

            if (isAnyLineupAdjustment()) {
                const lineupProgress = isOnlineLineupAdjustment()
                    ? `Sắp xếp đội hình   ${onlineRoom?.currentGameNumber || 1}/${onlineRoom?.maxGames || 1}`
                    : "Sắp xếp đội hình";
                progress.textContent = lineupProgress;
                label.textContent = "Sắp xếp đội hình";
                team.textContent = "Hai đội";
                team.className = "phase-chip neutral";
                action.textContent = "Chốt đội hình";
                remaining.textContent = "30 giây điều chỉnh vị trí";
                return;
            }

            if (!phase) {
                progress.textContent = `Lượt ${draftPhases.length} / ${draftPhases.length}`;
                label.textContent = isStandardDraftMode() ? "Hoàn tất draft" : "Chọn chế độ";
                team.textContent = isStandardDraftMode() ? "Hai đội" : "Chưa bắt đầu";
                team.className = "phase-chip neutral";
                action.textContent = isStandardDraftMode() ? "Hoàn tất" : "Đang chờ";
                remaining.textContent = "Còn 0 lựa chọn";
                return;
            }

            const remainingCount = phase.count - currentPhaseSelectedCount;
            progress.textContent = `Lượt ${currentPhaseIndex + 1} / ${draftPhases.length}`;
            label.textContent = phase.label;
            team.textContent = isSoloOneVOneMode()
                ? `${getActivePlayerName()} - ${getActiveSideLabel()}`
                : teamLabels[phase.team];
            team.className = `phase-chip ${phase.team}`;
            action.textContent = actionLabels[phase.action];
            remaining.textContent = `Còn ${remainingCount} lựa chọn`;
        }

        function renderTimer() {
            const displayTime = `${Math.max(0, Number.parseInt(timerRemaining, 10) || 0)}s`;
            document.getElementById('timer-display').textContent = displayTime;
            document.getElementById('pvp-timer-display').textContent = displayTime;
            const actionBarTimer = document.getElementById('draft-action-bar-timer');
            if (actionBarTimer) {
                actionBarTimer.textContent = displayTime;
                actionBarTimer.classList.toggle('is-low', (Number.parseInt(timerRemaining, 10) || 0) <= 10);
            }
            document.getElementById('phaseDurationInput').value = phaseDuration;
            const lineupCountdown = document.getElementById('lineup-bar-countdown');
            if (lineupCountdown && isAnyLineupAdjustment()) {
                lineupCountdown.textContent = `Còn ${Math.max(0, Number.parseInt(timerRemaining, 10) || 0)}s`;
            }
        }

        function renderPvpHeader() {
            const phase = getCurrentPhase();
            const gamePrefix = isOnlineRoomMode() && onlineRoom
                ? `Ván ${onlineRoom.currentGameNumber || 1}/${onlineRoom.maxGames || 1}   `
                : "";
            document.getElementById('pvp-blue-player').textContent = playerBlue;
            document.getElementById('pvp-red-player').textContent = playerRed;
            document.getElementById('pvp-blue-card').classList.toggle('is-active', phase?.team === "blue");
            document.getElementById('pvp-red-card').classList.toggle('is-active', phase?.team === "red");

            if (isAnyLineupAdjustment()) {
                document.getElementById('pvp-phase-progress').textContent = `${gamePrefix}Sắp xếp đội hình`;
                document.getElementById('pvp-phase-label').textContent = "Sắp xếp đội hình";
                document.getElementById('pvp-turn-owner').textContent = "Hai đội có 30 giây để đưa tướng về đúng vị trí";
                return;
            }

            if (!phase) {
                document.getElementById('pvp-phase-progress').textContent = `${gamePrefix}Lượt ${draftPhases.length} / ${draftPhases.length}`;
                document.getElementById('pvp-phase-label').textContent = "Hoàn tất draft";
                document.getElementById('pvp-turn-owner').textContent = "Draft đã hoàn tất";
                return;
            }

            document.getElementById('pvp-phase-progress').textContent = `${gamePrefix}Lượt ${currentPhaseIndex + 1} / ${draftPhases.length}`;
            document.getElementById('pvp-phase-label').textContent = phase.label;
            document.getElementById('pvp-turn-owner').textContent = `Lượt của ${getActivePlayerName()} - ${getActiveSideLabel()}`;
        }

        function renderDraft() {
            renderOnlineSeriesPanel();
            renderSlots();
            renderLineupPanel();
            renderPhaseStatus();
            renderTimer();
            renderPvpHeader();
            renderPreview();
            syncConfirmDockState();
            renderHeroButtons();
        }

        function selectHero(heroName) {
            if (!currentMode) return;

            if (isHeroUsed(heroName)) {
                setStatus("Tướng này đã bị cấm hoặc đã được chọn.", "warning");
                return;
            }

            if (isHeroRestrictedForCurrentOnlinePick(heroName)) {
                setStatus("Tướng này đã được đội của bạn sử dụng ở ván trước.", "warning");
                return;
            }

            if (currentMode === "free") {
                setStatus("Chế độ tự do dùng kéo-thả tướng vào ô cấm/chọn.", "warning");
                return;
            }

            const phase = getCurrentPhase();
            if (!phase) {
                setStatus("Draft đã hoàn tất. Bấm Reset draft để bắt đầu lại.", "success");
                return;
            }

            if (isOnlineRoomMode() && onlineRoom?.status === "IN_PROGRESS" && !isOnlineActivePlayer()) {
                setStatus("Đang chờ đối thủ chọn...", "warning");
                return;
            }

            selectedPreviewHero = heroName;
            renderDraft();
            setStatus(
                isSoloOneVOneMode()
                    ? `Đang chọn: ${heroName}. ${getActivePlayerName()} cần bấm Xác nhận để khóa lượt.`
                    : `Đang chọn: ${heroName}. Bấm Xác nhận để khóa lượt ${phase.label}.`,
                ""
            );
        }

        function cancelPreviewSelection() {
            selectedPreviewHero = null;
            renderDraft();
            setStatus("Đã hủy chọn tướng xem trước.", "warning");
        }

        function confirmPreviewSelection() {
            if (isAnyLineupAdjustment()) {
                confirmLineupAdjustment();
                return;
            }
            const phase = getCurrentPhase();
            if (!phase) {
                setStatus("Không có lượt tiêu chuẩn đang hoạt động.", "warning");
                return;
            }

            if (!selectedPreviewHero) {
                setStatus("Chưa chọn tướng để xác nhận.", "warning");
                return;
            }

            if (isHeroUsed(selectedPreviewHero)) {
                selectedPreviewHero = null;
                renderDraft();
                setStatus("Tướng này đã không còn khả dụng.", "warning");
                return;
            }

            if (isHeroRestrictedForCurrentOnlinePick(selectedPreviewHero)) {
                selectedPreviewHero = null;
                renderDraft();
                setStatus("Tướng này đã được đội của bạn sử dụng ở ván trước.", "warning");
                return;
            }

            if (isOnlineRoomMode() && onlineRoom?.status === "IN_PROGRESS") {
                confirmOnlineSelection();
                return;
            }

            getCollection(phase.team, phase.action).push(selectedPreviewHero);
            currentPhaseSelectedCount += 1;
            const lockedHero = selectedPreviewHero;
            selectedPreviewHero = null;
            renderDraft();

            if (currentPhaseSelectedCount >= phase.count) {
                advancePhase(false);
                return;
            }

            const remaining = phase.count - currentPhaseSelectedCount;
            setStatus(
                isSoloOneVOneMode()
                    ? `Đã khóa ${lockedHero}. ${getActivePlayerName()} còn ${remaining} lựa chọn trong lượt này.`
                    : `Đã khóa ${lockedHero}. Còn ${remaining} lựa chọn trong lượt ${phase.label}.`,
                "success"
            );
        }

        async function confirmLineupAdjustment() {
            if (isOnlineLineupAdjustment()) {
                const currentSide = getCurrentLineupSide();
                if (!onlineRoomCode || !currentSide || isLineupSideConfirmed(currentSide)) return;
                await submitOnlineLineupOrder(currentSide, { silent: true });
                const payload = { teamSide: currentSide.toUpperCase() };
                if (onlineSocketConnected && onlineStompClient) {
                    if (sendOnlineRoomCommand('lineup/confirm', payload)) {
                        if (currentSide === "blue") {
                            onlineRoom = { ...onlineRoom, blueLineupConfirmed: true };
                        } else {
                            onlineRoom = { ...onlineRoom, redLineupConfirmed: true };
                        }
                        setStatus("Bạn đã xác nhận đội hình.", "success");
                        renderDraft();
                    }
                } else {
                    try {
                        const room = await onlineRoomRequest(`/api/ban-pick/rooms/${encodeURIComponent(onlineRoomCode)}/lineup/confirm`, {
                            method: 'POST',
                            body: JSON.stringify(payload)
                        });
                        applyOnlineRoomState(room);
                        setStatus("Bạn đã xác nhận đội hình.", "success");
                    } catch (error) {
                        setOnlineFeedback(error.message, "error");
                    }
                }
                return;
            }

            if (!isLocalLineupAdjustment()) return;
            localLineupConfirmed = true;
            finishLocalLineupAdjustment();
        }

        function startLocalLineupAdjustment() {
            if (!currentMode || currentMode !== "standard") {
                completeStandardDraft();
                return;
            }
            pauseTimer(false);
            localLineupAdjustmentActive = true;
            localLineupConfirmed = false;
            selectedPreviewHero = null;
            timerRemaining = 30;
            renderDraft();
            startTimer(false);
            setStatus("Sắp xếp đội hình. Hai đội có 30 giây để đưa tướng về đúng vị trí.", "");
        }

        function finishLocalLineupAdjustment() {
            localLineupAdjustmentActive = false;
            pauseTimer(false);
            renderDraft();
            completeStandardDraft();
        }

        function advancePhase(isManual) {
            if (!isStandardDraftMode()) return;

            const keepTimerRunning = isTimerRunning();
            pauseTimer(false);
            currentPhaseIndex += 1;
            selectedPreviewHero = null;
            currentPhaseSelectedCount = 0;
            timerRemaining = phaseDuration;

            if (!getCurrentPhase() && !isLocalLineupAdjustment()) {
                startLocalLineupAdjustment();
                return;
            }

            renderDraft();
            const nextPhase = getCurrentPhase();
            const manualText = isManual ? "Đã chuyển lượt thủ công. " : "";
            setStatus(
                isSoloOneVOneMode()
                    ? `${manualText}Lượt của ${getActivePlayerName()} - ${getActiveSideLabel()}. Chọn tướng rồi bấm Xác nhận.`
                    : `${manualText}Đến lượt ${nextPhase.label}. Chọn tướng rồi bấm Xác nhận.`,
                ""
            );

            if (keepTimerRunning) startTimer(false);
        }

        function completeStandardDraft() {
            pauseTimer(false);
            renderDraft();

            if (isSoloOneVOneMode()) {
                showDraftSummary();
                return;
            }

            setStatus("Hoàn tất draft. Tất cả lượt cấm/chọn đã kết thúc.", "success");
        }

        function formatHeroList(list) {
            const heroes = list.filter(Boolean);
            return heroes.length ? heroes.join(", ") : "Chưa có";
        }

        function showDraftSummary() {
            document.body.classList.remove('app-layout-active');
            document.getElementById('lineup-adjustment-panel').classList.add('is-hidden');
            document.getElementById('lineup-action-bar').classList.add('is-hidden');
            document.getElementById('draft-status-panel').classList.add('is-hidden');
            document.getElementById('draft-board').classList.add('is-hidden');
            document.getElementById('pvp-match-panel').classList.add('is-hidden');
            document.getElementById('draft-summary-panel').classList.remove('is-hidden');
            document.getElementById('online-series-panel').classList.toggle('is-hidden', !isOnlineRoomMode());
            renderOnlineSeriesPanel();
            const seriesProgress = document.getElementById('summary-series-progress');
            if (seriesProgress) {
                seriesProgress.textContent = isOnlineRoomMode() && onlineRoom
                    ? `${onlineRoom.seriesType || 'BO1'}   Ván ${onlineRoom.currentGameNumber || 1} / ${onlineRoom.maxGames || 1}`
                    : "Solo Ban/Pick 1v1 Online";
            }
            document.getElementById('summary-blue-player').textContent = `${playerBlue} - Bên Xanh`;
            document.getElementById('summary-red-player').textContent = `${playerRed} - Bên Đỏ`;
            document.getElementById('summary-blue-bans').innerHTML = formatHeroIconList(blueBans);
            document.getElementById('summary-red-bans').innerHTML = formatHeroIconList(redBans);
            document.getElementById('summary-blue-picks').innerHTML = formatHeroIconList(bluePicks);
            document.getElementById('summary-red-picks').innerHTML = formatHeroIconList(redPicks);
            const shareButton = document.getElementById('share-draft-btn');
            if (shareButton) shareButton.disabled = isOnlineRoomMode() && !onlineRoom?.draftHistoryId;
            const seriesActionButton = document.getElementById('online-series-action-btn');
            if (seriesActionButton) {
                const showSeriesButton = isOnlineRoomMode() && onlineRoom && (onlineRoom.maxGames || 1) > 1;
                seriesActionButton.hidden = !showSeriesButton;
                seriesActionButton.textContent = onlineRoom?.isFinalGame ? "Kết thúc series" : "Ván tiếp theo";
                seriesActionButton.disabled = Boolean(showSeriesButton && !onlineRoom?.isFinalGame && !isOnlineHost());
            }
        }

        function restartPvpDraft() {
            if (isOnlineRoomMode() && onlineRoomCode) {
                resetOnlineRoom();
                return;
            }
            currentMode = "solo-1v1";
            resetDraftState();
            showDraftBoard();
            renderDraft();
            setStatus(`Draft lại. Lượt của ${getActivePlayerName()} - ${getActiveSideLabel()}.`, "");
        }

        function manualNextPhase() {
            if (!isStandardDraftMode()) return;
            if (!getCurrentPhase() && !isLocalLineupAdjustment()) {
                setStatus("Draft đã hoàn tất. Không còn lượt tiếp theo.", "success");
                return;
            }
            advancePhase(true);
        }

        function applyPhaseDuration() {
            if (!isStandardDraftMode()) return;
            const input = document.getElementById('phaseDurationInput');
            const nextDuration = Math.max(10, Math.min(180, Number.parseInt(input.value, 10) || 60));
            phaseDuration = nextDuration;
            timerRemaining = phaseDuration;
            renderTimer();
            setStatus(`Thời gian mỗi lượt: ${phaseDuration}s`, "success");
        }

        function startTimer(showMessage = true) {
            if (!isStandardDraftMode()) return;
            if (isLocalLineupAdjustment() && timerRemaining <= 0) timerRemaining = 30;

            if (!getCurrentPhase() && !isLocalLineupAdjustment()) {
                setStatus("Draft đã hoàn tất. Bấm Reset draft để bắt đầu lại.", "success");
                return;
            }

            if (timerRemaining <= 0 && !isLocalLineupAdjustment()) timerRemaining = phaseDuration;
            if (timerInterval) clearInterval(timerInterval);

            timerInterval = setInterval(() => {
                timerRemaining -= 1;
                if (timerRemaining <= 0) {
                    timerRemaining = 0;
                    renderTimer();
                    pauseTimer(false);
                    if (isLocalLineupAdjustment()) {
                        finishLocalLineupAdjustment();
                        return;
                    }
                    setStatus(
                        isSoloOneVOneMode()
                            ? "Hết thời gian! Người chơi cần chọn hoặc chuyển lượt."
                            : "Hết thời gian! Vui lòng chọn tướng hoặc chuyển lượt.",
                        "error"
                    );
                    return;
                }
                renderTimer();
            }, 1000);

            if (showMessage) {
                if (isLocalLineupAdjustment()) {
                    setStatus("Sắp xếp đội hình. Hai đội có 30 giây để đưa tướng về đúng vị trí.", "");
                    return;
                }
                const phase = getCurrentPhase();
                setStatus(`Đang chạy đồng hồ cho ${phase.label}.`, "");
            }
        }

        function pauseTimer(showMessage = true) {
            if (timerInterval) {
                clearInterval(timerInterval);
                timerInterval = null;
            }

            if (showMessage && isStandardDraftMode()) setStatus("Đã tạm dừng đồng hồ.", "warning");
        }

        function resetDraft() {
            if (isOnlineRoomMode() && onlineRoomCode) {
                resetOnlineRoom();
                return;
            }
            const modeBeforeReset = currentMode;
            resetDraftState();
            currentMode = modeBeforeReset;
            renderDraft();

            if (currentMode === "solo-1v1") {
                setStatus(`Draft 1v1 đã reset. Lượt của ${getActivePlayerName()} - ${getActiveSideLabel()}.`, "");
            } else if (currentMode === "standard") {
                setStatus("Draft đã reset. Đến lượt Xanh cấm 1.", "");
            } else if (currentMode === "free") {
                setStatus("Draft tự do đã reset. Kéo tướng vào ô cấm/chọn để tiếp tục.", "");
            }
        }

        function placeHeroInFreeSlot(heroName, targetSlot, sourceSlotId) {
            const targetInfo = parseSlotId(targetSlot.id);
            if (!targetInfo) return;

            if (sourceSlotId === targetSlot.id) return;
            if (isHeroUsed(heroName) && !sourceSlotId) {
                setStatus("Tướng này đã bị cấm hoặc đã được chọn.", "warning");
                return;
            }

            removeHeroFromSlot(targetSlot.id);
            if (sourceSlotId) removeHeroFromSlot(sourceSlotId);

            const targetCollection = getCollection(targetInfo.team, targetInfo.action);
            targetCollection[targetInfo.index] = heroName;
            trimEmptyTail(targetCollection);
            renderDraft();
            setStatus(`${teamLabels[targetInfo.team]} ${actionLabels[targetInfo.action].toLowerCase()} tự do: ${heroName}.`, "success");
        }

        function dragStartFromList(ev, heroName) {
            if (!currentMode || isHeroUsed(heroName) || isHeroRestrictedForCurrentOnlinePick(heroName) || (isStandardDraftMode() && !getCurrentPhase())
                || (isOnlineRoomMode() && onlineRoom?.status === "IN_PROGRESS" && !isOnlineActivePlayer())) {
                ev.preventDefault();
                return;
            }

            const draggedElement = ev.currentTarget;
            ev.dataTransfer.setData("source", "list");
            ev.dataTransfer.setData("heroName", heroName);
            ev.dataTransfer.effectAllowed = "move";
            setTimeout(() => draggedElement.classList.add('dragging'), 0);
        }

        function dragStartFromSlot(ev) {
            const heroName = ev.currentTarget.getAttribute('data-hero-name') || '';
            if (currentMode !== "free" || !heroName) {
                ev.preventDefault();
                return;
            }

            const draggedElement = ev.currentTarget;
            ev.dataTransfer.setData("source", "slot");
            ev.dataTransfer.setData("sourceSlotId", ev.currentTarget.id);
            ev.dataTransfer.setData("heroName", heroName);
            ev.dataTransfer.effectAllowed = "move";
            setTimeout(() => draggedElement.classList.add('dragging'), 0);
        }

        function dragEnd(ev) {
            ev.currentTarget?.classList.remove('dragging');
        }

        function allowDrop(ev) {
            ev.preventDefault();
            if (ev.dataTransfer) ev.dataTransfer.dropEffect = "move";
        }

        function dragEnter(ev) {
            ev.preventDefault();
            const slot = ev.currentTarget;
            if (currentMode === "free" || slot.classList.contains('active')) {
                slot.classList.add('drag-over');
            }
        }

        function dragLeave(ev) {
            ev.currentTarget.classList.remove('drag-over');
        }

        function dropOnSlot(ev) {
            ev.preventDefault();
            ev.stopPropagation();
            const targetSlot = ev.currentTarget;
            targetSlot.classList.remove('drag-over');

            const heroName = ev.dataTransfer.getData("heroName");
            const sourceSlotId = ev.dataTransfer.getData("sourceSlotId");
            if (!currentMode || !heroName) return;

            if (currentMode === "free") {
                placeHeroInFreeSlot(heroName, targetSlot, sourceSlotId);
                return;
            }

            const phase = getCurrentPhase();
            if (!phase) return;

            if (isOnlineRoomMode() && onlineRoom?.status === "IN_PROGRESS" && !isOnlineActivePlayer()) {
                setStatus("Đang chờ đối thủ chọn...", "warning");
                return;
            }

            const isActiveTeamSlot = targetSlot.id.startsWith(`${phase.team}-`);
            const isActiveActionSlot = targetSlot.id.includes(`-${phase.action}-`);

            if (!targetSlot.classList.contains('active') || !isActiveTeamSlot || !isActiveActionSlot) {
                setStatus(`Chỉ được ${actionLabels[phase.action].toLowerCase()} cho ${teamLabels[phase.team]} trong lượt này.`, "warning");
                return;
            }

            selectHero(heroName);
        }

        function dropOutsideSlots(ev) {
            if (currentMode !== "free" || ev.target.closest('.slot')) return;

            const sourceSlotId = ev.dataTransfer?.getData("sourceSlotId");
            if (!sourceSlotId) return;

            ev.preventDefault();
            removeHeroFromSlot(sourceSlotId);
            renderDraft();
            setStatus("Đã bỏ tướng khỏi ô cấm/chọn tự do.", "warning");
        }

        function setRoleFilter(role) {
            currentRoleFilter = role;
            document.querySelectorAll('.role-btn').forEach(btn => {
                const value = btn.getAttribute('data-role-filter') || btn.textContent.trim();
                btn.classList.toggle('active', value === role);
            });
            filterHeroes();
        }

        function filterHeroes() {
            const query = document.getElementById('searchInput').value.toLowerCase();
            document.querySelectorAll('.hero-btn').forEach(btn => {
                const heroRole = btn.getAttribute('data-role');
                const heroName = (btn.getAttribute('data-hero-name') || btn.innerText).toLowerCase();
                const matchesRole = (currentRoleFilter === 'Tất cả' || heroRole === currentRoleFilter);
                const matchesSearch = heroName.includes(query);
                btn.style.display = matchesRole && matchesSearch ? 'flex' : 'none';
            });
        }

        initApp();

