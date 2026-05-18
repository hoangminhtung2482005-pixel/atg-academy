package com.example.demo.service;

import com.example.demo.dto.banpick.BanPickActionResponse;
import com.example.demo.dto.banpick.BanPickConfirmRequest;
import com.example.demo.dto.banpick.BanPickCreateRoomRequest;
import com.example.demo.dto.banpick.BanPickLineupConfirmRequest;
import com.example.demo.dto.banpick.BanPickLineupReorderRequest;
import com.example.demo.dto.banpick.BanPickParticipantResponse;
import com.example.demo.dto.banpick.BanPickPhaseResponse;
import com.example.demo.dto.banpick.BanPickRoomStateResponse;
import com.example.demo.dto.banpick.BanPickStrategyPoolRequest;
import com.example.demo.dto.banpick.BanPickUserSummary;
import com.example.demo.dto.wiki.HeroSummaryDto;
import com.example.demo.entity.BanPickAction;
import com.example.demo.entity.BanPickActionType;
import com.example.demo.entity.BanPickMatchMode;
import com.example.demo.entity.BanPickParticipantRole;
import com.example.demo.entity.BanPickPhaseType;
import com.example.demo.entity.BanPickRoom;
import com.example.demo.entity.BanPickRoomParticipant;
import com.example.demo.entity.BanPickRoomStatus;
import com.example.demo.entity.BanPickSeriesType;
import com.example.demo.entity.BanPickTeamSide;
import com.example.demo.entity.DraftHistoryEndReason;
import com.example.demo.entity.Hero;
import com.example.demo.entity.User;
import com.example.demo.repository.BanPickActionRepository;
import com.example.demo.repository.BanPickRoomParticipantRepository;
import com.example.demo.repository.BanPickRoomRepository;
import com.example.demo.repository.HeroRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.GoogleUserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BanPickRoomService {

    private static final int DEFAULT_PHASE_DURATION_SECONDS = 60;
    private static final int LINEUP_ADJUSTMENT_DURATION_SECONDS = 30;
    private static final DateTimeFormatter COOLDOWN_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
    private static final String ROOM_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int ROOM_CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREP_PHASE_MESSAGE = "Rank Mode đang ở giai đoạn chuẩn bị trước draft.";
    private static final String SERIES_REUSED_HERO_MESSAGE = "Tướng này đã được đội của bạn sử dụng ở ván trước.";
    private static final String DUPLICATE_HERO_MESSAGE = "Tướng này đã được chọn hoặc cấm.";

    private static final List<DraftPhase> DRAFT_PHASES = List.of(
            new DraftPhase(BanPickTeamSide.BLUE, BanPickActionType.BAN, 1, "Xanh cấm lượt 1"),
            new DraftPhase(BanPickTeamSide.RED, BanPickActionType.BAN, 1, "Đỏ cấm lượt 1"),
            new DraftPhase(BanPickTeamSide.BLUE, BanPickActionType.BAN, 1, "Xanh cấm lượt 2"),
            new DraftPhase(BanPickTeamSide.RED, BanPickActionType.BAN, 1, "Đỏ cấm lượt 2"),
            new DraftPhase(BanPickTeamSide.BLUE, BanPickActionType.PICK, 1, "Xanh chọn 1"),
            new DraftPhase(BanPickTeamSide.RED, BanPickActionType.PICK, 2, "Đỏ chọn 2"),
            new DraftPhase(BanPickTeamSide.BLUE, BanPickActionType.PICK, 2, "Xanh chọn 2"),
            new DraftPhase(BanPickTeamSide.RED, BanPickActionType.PICK, 1, "Đỏ chọn 1"),
            new DraftPhase(BanPickTeamSide.RED, BanPickActionType.BAN, 1, "Đỏ cấm lượt 3"),
            new DraftPhase(BanPickTeamSide.BLUE, BanPickActionType.BAN, 1, "Xanh cấm lượt 3"),
            new DraftPhase(BanPickTeamSide.RED, BanPickActionType.BAN, 1, "Đỏ cấm lượt 4"),
            new DraftPhase(BanPickTeamSide.BLUE, BanPickActionType.BAN, 1, "Xanh cấm lượt 4"),
            new DraftPhase(BanPickTeamSide.RED, BanPickActionType.PICK, 1, "Đỏ chọn 3"),
            new DraftPhase(BanPickTeamSide.BLUE, BanPickActionType.PICK, 2, "Xanh chọn 3-4"),
            new DraftPhase(BanPickTeamSide.RED, BanPickActionType.PICK, 1, "Đỏ chọn 4")
    );
    private static final List<DraftPhase> ULTIMATE_BATTLE_PHASES = List.of(
            new DraftPhase(BanPickTeamSide.BLUE, BanPickActionType.PICK, 1, "Ultimate Battle Xanh chon 1"),
            new DraftPhase(BanPickTeamSide.RED, BanPickActionType.PICK, 1, "Ultimate Battle Do chon 1"),
            new DraftPhase(BanPickTeamSide.BLUE, BanPickActionType.PICK, 1, "Ultimate Battle Xanh chon 2"),
            new DraftPhase(BanPickTeamSide.RED, BanPickActionType.PICK, 1, "Ultimate Battle Do chon 2"),
            new DraftPhase(BanPickTeamSide.BLUE, BanPickActionType.PICK, 1, "Ultimate Battle Xanh chon 3"),
            new DraftPhase(BanPickTeamSide.RED, BanPickActionType.PICK, 1, "Ultimate Battle Do chon 3"),
            new DraftPhase(BanPickTeamSide.BLUE, BanPickActionType.PICK, 1, "Ultimate Battle Xanh chon 4"),
            new DraftPhase(BanPickTeamSide.RED, BanPickActionType.PICK, 1, "Ultimate Battle Do chon 4"),
            new DraftPhase(BanPickTeamSide.BLUE, BanPickActionType.PICK, 1, "Ultimate Battle Xanh chon 5"),
            new DraftPhase(BanPickTeamSide.RED, BanPickActionType.PICK, 1, "Ultimate Battle Do chon 5")
    );

    private final BanPickRoomRepository roomRepository;
    private final BanPickRoomParticipantRepository participantRepository;
    private final BanPickActionRepository actionRepository;
    private final UserRepository userRepository;
    private final HeroRepository heroRepository;
    private final BanPickHistoryService banPickHistoryService;
    private final BanPickRankedRoomContextService rankedRoomContextService;
    private final BanPickRatingSettingsAccessor settingsAccessor;
    private final Map<String, PendingDisconnectState> pendingDisconnects = new ConcurrentHashMap<>();

    public BanPickRoomService(BanPickRoomRepository roomRepository,
                              BanPickRoomParticipantRepository participantRepository,
                              BanPickActionRepository actionRepository,
                              UserRepository userRepository,
                              HeroRepository heroRepository,
                              BanPickHistoryService banPickHistoryService) {
        this(
                roomRepository,
                participantRepository,
                actionRepository,
                userRepository,
                heroRepository,
                banPickHistoryService,
                new BanPickRankedRoomContextService(heroRepository),
                BanPickRatingSettingsSnapshot::defaults
        );
    }

    @Autowired
    BanPickRoomService(BanPickRoomRepository roomRepository,
                       BanPickRoomParticipantRepository participantRepository,
                       BanPickActionRepository actionRepository,
                       UserRepository userRepository,
                       HeroRepository heroRepository,
                       BanPickHistoryService banPickHistoryService,
                       BanPickRankedRoomContextService rankedRoomContextService,
                       BanPickRatingSettingsAccessor settingsAccessor) {
        this.roomRepository = roomRepository;
        this.participantRepository = participantRepository;
        this.actionRepository = actionRepository;
        this.userRepository = userRepository;
        this.heroRepository = heroRepository;
        this.banPickHistoryService = banPickHistoryService;
        this.rankedRoomContextService = rankedRoomContextService;
        this.settingsAccessor = settingsAccessor;
    }

    @Transactional
    public BanPickRoomStateResponse createRoom(GoogleUserPrincipal principal, BanPickCreateRoomRequest request) {
        User host = findOrCreateUser(principal);
        BanPickMatchMode requestedMode = BanPickMatchMode.defaultIfNull(request != null ? request.mode() : null);
        if (requestedMode == BanPickMatchMode.RANKED) {
            ensureSoloCooldownInactive(host, true);
        }

        BanPickRoom room = new BanPickRoom();
        room.setRoomCode(generateRoomCode());
        room.setMode(requestedMode);
        room.setStatus(BanPickRoomStatus.WAITING);
        room.setPhaseType(BanPickPhaseType.DRAFT);
        room.setSeriesType(BanPickSeriesType.defaultIfNull(request != null ? request.seriesType() : null));
        room.setCurrentGameNumber(1);
        room.setHostUser(host);
        room.setPhaseDurationSeconds(DEFAULT_PHASE_DURATION_SECONDS);
        rankedRoomContextService.prepareRankedRoom(room);
        resetLineupState(room);
        BanPickRoom savedRoom = roomRepository.save(room);

        BanPickRoomParticipant participant = new BanPickRoomParticipant();
        participant.setRoom(savedRoom);
        participant.setUser(host);
        participant.setRole(BanPickParticipantRole.HOST);
        participantRepository.save(participant);

        return buildState(savedRoom, host);
    }

    @Transactional
    public BanPickRoomStateResponse getRoomState(String roomCode, GoogleUserPrincipal principal) {
        User user = findUser(principal);
        BanPickRoom room = findRoomForUpdate(roomCode);
        ensureCanView(room, user);
        Set<Long> newActionIds = new HashSet<>();
        boolean changed = resolveExpiredPhaseIfNeeded(room, LocalDateTime.now(), newActionIds);
        BanPickRoom savedRoom = changed ? roomRepository.save(room) : room;
        if (changed) {
            recordHistoryIfFinished(savedRoom);
        }
        return buildState(savedRoom, user, newActionIds);
    }

    @Transactional
    public BanPickRoomStateResponse joinRoom(String roomCode, GoogleUserPrincipal principal) {
        User guest = findOrCreateUser(principal);
        BanPickRoom room = findRoomForUpdate(roomCode);

        // Only enforce cooldown for RANKED rooms
        if (BanPickMatchMode.defaultIfNull(room.getMode()) == BanPickMatchMode.RANKED) {
            ensureSoloCooldownInactive(guest, true);
        }

        if (isRoomParticipant(room, guest)) {
            return buildState(room, guest);
        }
        if (room.getStatus() == BanPickRoomStatus.IN_PROGRESS || room.getStatus() == BanPickRoomStatus.FINISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phòng không thể tham gia.");
        }
        if (room.getGuestUser() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phòng đã đủ người.");
        }

        room.setGuestUser(guest);
        room.setStatus(BanPickRoomStatus.READY);
        BanPickRoom savedRoom = roomRepository.save(room);

        BanPickRoomParticipant participant = new BanPickRoomParticipant();
        participant.setRoom(savedRoom);
        participant.setUser(guest);
        participant.setRole(BanPickParticipantRole.GUEST);
        participantRepository.save(participant);

        return buildState(savedRoom, guest);
    }

    @Transactional
    public BanPickRoomStateResponse readyRoom(String roomCode, GoogleUserPrincipal principal) {
        User user = findUser(principal);
        BanPickRoom room = findRoomForUpdate(roomCode);
        ensureParticipant(room, user);
        ensureStatus(room, BanPickRoomStatus.WAITING, BanPickRoomStatus.READY);

        if (isSameUser(room.getHostUser(), user)) {
            room.setHostReady(true);
        } else if (isSameUser(room.getGuestUser(), user)) {
            room.setGuestReady(true);
        }
        if (Boolean.TRUE.equals(room.getHostReady()) && Boolean.TRUE.equals(room.getGuestReady())) {
            rankedRoomContextService.initializeContextIfMissing(room);
        }

        return buildState(roomRepository.save(room), user);
    }

    @Transactional
    public BanPickRoomStateResponse rollSide(String roomCode, GoogleUserPrincipal principal) {
        User user = findUser(principal);
        BanPickRoom room = findRoomForUpdate(roomCode);
        ensureHost(room, user);
        ensureBothPlayersJoined(room);
        ensureStatus(room, BanPickRoomStatus.READY, BanPickRoomStatus.WAITING);
        if (room.getBlueUser() != null || room.getRedUser() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hai bên đã được phân định.");
        }

        boolean hostBlue = RANDOM.nextBoolean();
        room.setBlueUser(hostBlue ? room.getHostUser() : room.getGuestUser());
        room.setRedUser(hostBlue ? room.getGuestUser() : room.getHostUser());
        if (Boolean.TRUE.equals(room.getHostReady()) && Boolean.TRUE.equals(room.getGuestReady())) {
            rankedRoomContextService.initializeContextIfMissing(room);
        }
        room.setStatus(BanPickRoomStatus.READY);
        BanPickRoom savedRoom = roomRepository.save(room);
        syncParticipantSides(savedRoom);

        return buildState(savedRoom, user);
    }

    @Transactional
    public BanPickRoomStateResponse startRoom(String roomCode, GoogleUserPrincipal principal) {
        User user = findUser(principal);
        BanPickRoom room = findRoomForUpdate(roomCode);
        ensureHost(room, user);
        ensureBothPlayersJoined(room);
        ensureSidesAssigned(room);
        ensureBothPlayersReady(room);
        ensureStatus(room, BanPickRoomStatus.READY);
        if (BanPickMatchMode.defaultIfNull(room.getMode()) == BanPickMatchMode.RANKED) {
            ensureSoloCooldownInactive(room.getHostUser(), isSameUser(room.getHostUser(), user));
            ensureSoloCooldownInactive(room.getGuestUser(), isSameUser(room.getGuestUser(), user));
        }

        LocalDateTime now = LocalDateTime.now();
        rankedRoomContextService.initializeContextIfMissing(room);
        room.setStatus(BanPickRoomStatus.IN_PROGRESS);
        room.setCurrentPhaseIndex(0);
        room.setCurrentPhaseSelectedCount(0);
        room.setPhaseType(BanPickPhaseType.DRAFT);
        room.setTimerStartedAt(now);
        rankedRoomContextService.startPrepWindowIfNeeded(room, now);
        room.setPhaseDeadlineAt(room.getPrepPhaseEndAt() != null
                ? room.getPrepPhaseEndAt()
                : calculatePhaseDeadline(now, room));
        room.setLineupDeadlineAt(null);
        room.setBlueLineupConfirmed(false);
        room.setRedLineupConfirmed(false);
        room.setBluePickOrder(null);
        room.setRedPickOrder(null);
        clearPendingDisconnect(room.getRoomCode());
        clearFinishedDraftMetadata(room);
        return buildState(roomRepository.save(room), user);
    }

    @Transactional
    public BanPickRoomStateResponse confirmAction(String roomCode,
                                                  BanPickConfirmRequest request,
                                                  GoogleUserPrincipal principal) {
        User user = findUser(principal);
        BanPickRoom room = findRoomForUpdate(roomCode);
        ensureParticipant(room, user);
        ensureStatus(room, BanPickRoomStatus.IN_PROGRESS);
        if (room.getPhaseType() == BanPickPhaseType.LINEUP_ADJUSTMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đang ở giai đoạn sắp xếp đội hình.");
        }
        validateConfirmRequest(request);

        LocalDateTime now = LocalDateTime.now();
        Set<Long> newActionIds = new HashSet<>();
        if (resolveExpiredPhaseIfNeeded(room, now, newActionIds)) {
            BanPickRoom savedRoom = roomRepository.save(room);
            recordHistoryIfFinished(savedRoom);
            return buildState(savedRoom, user, newActionIds);
        }
        if (rankedRoomContextService.isPrepPhaseActive(room, now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, PREP_PHASE_MESSAGE);
        }
        DraftPhase phase = getDraftPhase(room, room.getCurrentPhaseIndex());
        if (phase == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Draft is already complete");
        }
        if (request.teamSide() != phase.teamSide()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team side does not match current phase");
        }
        if (request.actionType() != phase.actionType()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action type does not match current phase");
        }
        if (!isCurrentActivePlayer(room, user, phase.teamSide())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chưa đến lượt của bạn.");
        }
        if (room.getCurrentPhaseSelectedCount() >= phase.count()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Current phase selection count exceeded");
        }
        Long heroId = resolveHeroId(request);
        if (heroId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hero does not exist");
        }
        if (actionRepository.existsByRoomAndHeroId(room, heroId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, DUPLICATE_HERO_MESSAGE);
        }
        validateSeriesPickAvailability(room, phase.teamSide(), phase.actionType(), heroId);

        BanPickAction action = new BanPickAction();
        action.setRoom(room);
        action.setUser(user);
        action.setTeamSide(phase.teamSide());
        action.setActionType(phase.actionType());
        action.setHeroId(heroId);
        action.setPhaseIndex(room.getCurrentPhaseIndex());
        action.setConfirmedAt(now);
        try {
            BanPickAction savedAction = actionRepository.saveAndFlush(action);
            if (savedAction.getId() != null) {
                newActionIds.add(savedAction.getId());
            }
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, DUPLICATE_HERO_MESSAGE, ex);
        }

        int selectedCount = room.getCurrentPhaseSelectedCount() + 1;
        if (selectedCount >= phase.count()) {
            advanceToNextPhase(room, now);
        } else {
            room.setCurrentPhaseSelectedCount(selectedCount);
        }

        BanPickRoom savedRoom = roomRepository.save(room);
        recordHistoryIfFinished(savedRoom);
        return buildState(savedRoom, user, newActionIds);
    }

    @Transactional
    public BanPickRoomStateResponse reorderLineup(String roomCode,
                                                  BanPickLineupReorderRequest request,
                                                  GoogleUserPrincipal principal) {
        User user = findUser(principal);
        BanPickRoom room = findRoomForUpdate(roomCode);
        ensureParticipant(room, user);
        ensureStatus(room, BanPickRoomStatus.IN_PROGRESS);
        ensureLineupAdjustmentPhase(room);

        LocalDateTime now = LocalDateTime.now();
        Set<Long> newActionIds = new HashSet<>();
        if (resolveExpiredPhaseIfNeeded(room, now, newActionIds)) {
            BanPickRoom savedRoom = roomRepository.save(room);
            recordHistoryIfFinished(savedRoom);
            return buildState(savedRoom, user, newActionIds);
        }

        BanPickTeamSide actorSide = resolveUserSide(room, user);
        if (actorSide == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không xác định được bên của bạn.");
        }
        if (request == null || request.heroIds() == null || request.heroIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "heroIds is required");
        }
        if (request.teamSide() != null && request.teamSide() != actorSide) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn chỉ được sắp xếp đội hình của bên mình.");
        }
        if (isLineupConfirmed(room, actorSide)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đội hình của bạn đã được xác nhận.");
        }

        List<Long> currentOrder = currentPickOrderForSide(room, actorSide);
        List<Long> requestedOrder = sanitizeHeroIds(request.heroIds());
        validateLineupReorder(currentOrder, requestedOrder);
        setPickOrder(room, actorSide, requestedOrder);

        return buildState(roomRepository.save(room), user);
    }

    @Transactional
    public BanPickRoomStateResponse confirmLineup(String roomCode,
                                                  BanPickLineupConfirmRequest request,
                                                  GoogleUserPrincipal principal) {
        User user = findUser(principal);
        BanPickRoom room = findRoomForUpdate(roomCode);
        ensureParticipant(room, user);
        ensureStatus(room, BanPickRoomStatus.IN_PROGRESS);
        ensureLineupAdjustmentPhase(room);

        LocalDateTime now = LocalDateTime.now();
        Set<Long> newActionIds = new HashSet<>();
        if (resolveExpiredPhaseIfNeeded(room, now, newActionIds)) {
            BanPickRoom savedRoom = roomRepository.save(room);
            recordHistoryIfFinished(savedRoom);
            return buildState(savedRoom, user, newActionIds);
        }

        BanPickTeamSide actorSide = resolveUserSide(room, user);
        if (actorSide == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không xác định được bên của bạn.");
        }
        if (request != null && request.teamSide() != null && request.teamSide() != actorSide) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn chỉ được xác nhận đội hình của bên mình.");
        }

        if (actorSide == BanPickTeamSide.BLUE) {
            room.setBlueLineupConfirmed(true);
        } else {
            room.setRedLineupConfirmed(true);
        }
        if (Boolean.TRUE.equals(room.getBlueLineupConfirmed()) && Boolean.TRUE.equals(room.getRedLineupConfirmed())) {
            finishDraft(room);
        }

        BanPickRoom savedRoom = roomRepository.save(room);
        recordHistoryIfFinished(savedRoom);
        return buildState(savedRoom, user);
    }

    @Transactional
    public BanPickRoomStateResponse resetRoom(String roomCode, GoogleUserPrincipal principal) {
        User user = findUser(principal);
        BanPickRoom room = findRoomForUpdate(roomCode);
        ensureHost(room, user);
        BanPickRatingSettingsSnapshot settings = settingsAccessor.getCurrentSettings();
        // Only block reset during draft for RANKED mode (dodge protection)
        if (room.getStatus() == BanPickRoomStatus.IN_PROGRESS
                && settings.dodgeRejectResetDuringDraft()
                && BanPickMatchMode.defaultIfNull(room.getMode()) == BanPickMatchMode.RANKED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không thể reset khi phòng solo đang diễn ra. Hãy chờ trận kết thúc rồi reset."
            );
        }
        ensureStatus(room,
                BanPickRoomStatus.WAITING,
                BanPickRoomStatus.READY,
                BanPickRoomStatus.FINISHED);

        actionRepository.deleteByRoom(room);
        room.setCurrentPhaseIndex(0);
        room.setCurrentPhaseSelectedCount(0);
        room.setPhaseType(BanPickPhaseType.DRAFT);
        room.setTimerStartedAt(null);
        room.setPhaseDeadlineAt(null);
        room.setLineupDeadlineAt(null);
        room.setCurrentGameNumber(1);
        room.setBlueUsedPicksByGame(null);
        room.setRedUsedPicksByGame(null);
        room.setBlueSeriesUsedHeroIds(null);
        room.setRedSeriesUsedHeroIds(null);
        rankedRoomContextService.clearGeneratedContext(room);
        rankedRoomContextService.prepareRankedRoom(room);
        room.setBlueLineupConfirmed(false);
        room.setRedLineupConfirmed(false);
        room.setBluePickOrder(null);
        room.setRedPickOrder(null);
        room.setBlueUser(null);
        room.setRedUser(null);
        room.setHostReady(false);
        room.setGuestReady(false);
        room.setStatus(room.getGuestUser() != null
                ? BanPickRoomStatus.READY
                : BanPickRoomStatus.WAITING);
        clearPendingDisconnect(room.getRoomCode());
        clearFinishedDraftMetadata(room);

        BanPickRoom savedRoom = roomRepository.save(room);
        syncParticipantSides(savedRoom);
        return buildState(savedRoom, user);
    }

    @Transactional
    public BanPickRoomStateResponse nextGame(String roomCode, GoogleUserPrincipal principal) {
        User user = findUser(principal);
        BanPickRoom room = findRoomForUpdate(roomCode);
        ensureHost(room, user);
        ensureStatus(room, BanPickRoomStatus.FINISHED);

        if (isFinalGame(room)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Series đã ở ván cuối.");
        }

        List<BanPickAction> actions = actionRepository.findByRoomOrderByConfirmedAtAsc(room);
        captureFinishedGamePicks(room, actions);
        actionRepository.deleteByRoom(room);

        LocalDateTime now = LocalDateTime.now();
        int nextGameNumber = currentGameNumber(room) + 1;
        room.setCurrentGameNumber(nextGameNumber);
        if (isBo7ResetActive(room)) {
            clearSeriesUsedHeroes(room);
        }
        room.setCurrentPhaseIndex(0);
        room.setCurrentPhaseSelectedCount(0);
        room.setPhaseType(BanPickPhaseType.DRAFT);
        room.setStatus(BanPickRoomStatus.IN_PROGRESS);
        room.setTimerStartedAt(now);
        rankedRoomContextService.clearPrepWindow(room);
        room.setPhaseDeadlineAt(calculatePhaseDeadline(now, room));
        room.setLineupDeadlineAt(null);
        room.setBlueLineupConfirmed(false);
        room.setRedLineupConfirmed(false);
        room.setBluePickOrder(null);
        room.setRedPickOrder(null);
        clearPendingDisconnect(room.getRoomCode());
        clearFinishedDraftMetadata(room);

        return buildState(roomRepository.save(room), user);
    }

    @Transactional(readOnly = true)
    public List<String> findExpiredRoomCodes(LocalDateTime now) {
        return roomRepository.findExpiredRoomCodes(BanPickRoomStatus.IN_PROGRESS, now);
    }

    /**
     * Update the current user's strategy pool for the current room.
     * <p>
     * Rules:
     * - Pool is per-participant and scoped to the current room only.
     * - Player cannot add heroes that are in their own previous-used (locked) hero list.
     * - Player CAN add heroes that are in the opponent's previous-used hero list.
     * - Pool does not lock or ban heroes; it only affects display priority on the frontend.
     * - Can be updated during prep phase or at any time before/during draft.
     * - Reconnect restores the pool from the participant record.
     * </p>
     */
    @Transactional
    public BanPickRoomStateResponse updateStrategyPool(String roomCode,
                                                       BanPickStrategyPoolRequest request,
                                                       GoogleUserPrincipal principal) {
        User user = findUser(principal);
        BanPickRoom room = findRoomForUpdate(roomCode);
        ensureParticipant(room, user);
        ensureStatus(room, BanPickRoomStatus.WAITING, BanPickRoomStatus.READY, BanPickRoomStatus.IN_PROGRESS);

        List<Long> requestedHeroIds = request != null && request.heroIds() != null
                ? request.heroIds().stream().filter(Objects::nonNull).distinct().toList()
                : List.of();

        // Validate: player cannot add own locked heroes to pool
        Set<Long> ownLockedHeroIds = getOwnLockedHeroIds(room, user);
        List<Long> invalidHeroIds = requestedHeroIds.stream()
                .filter(ownLockedHeroIds::contains)
                .toList();
        if (!invalidHeroIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể thêm tướng đã bị khóa bởi đội của bạn vào strategy pool.");
        }

        // Find and update participant's strategy pool
        List<BanPickRoomParticipant> participants = participantRepository.findByRoomOrderByJoinedAtAsc(room);
        BanPickRoomParticipant participant = participants.stream()
                .filter(p -> isSameUser(p.getUser(), user))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a room participant"));

        participant.setStrategyPool(serializeHeroIds(requestedHeroIds));
        participantRepository.save(participant);

        return buildState(room, user);
    }

    /**
     * Returns the set of hero IDs that are locked for the given user's side (own previous-used locks).
     * These heroes cannot be added to the strategy pool or picked in RANKED mode.
     */
    private Set<Long> getOwnLockedHeroIds(BanPickRoom room, User user) {
        if (BanPickMatchMode.defaultIfNull(room.getMode()) != BanPickMatchMode.RANKED) {
            return Set.of();
        }
        BanPickTeamSide side = resolveUserSide(room, user);
        if (side == null) {
            return Set.of();
        }
        String encoded = side == BanPickTeamSide.BLUE
                ? room.getBluePreviousUsedHeroIds()
                : room.getRedPreviousUsedHeroIds();
        return new LinkedHashSet<>(parseHeroIdList(encoded));
    }

    @Transactional
    public Optional<BanPickRoomStateResponse> resolveExpiredPhase(String roomCode) {
        BanPickRoom room = findRoomForUpdate(roomCode);
        Set<Long> newActionIds = new HashSet<>();
        boolean changed = resolveExpiredPhaseIfNeeded(room, LocalDateTime.now(), newActionIds);
        if (!changed) {
            return Optional.empty();
        }
        BanPickRoom savedRoom = roomRepository.save(room);
        recordHistoryIfFinished(savedRoom);
        return Optional.of(buildState(savedRoom, newActionIds));
    }

    public void handlePresenceConnected(String roomCode, String email) {
        if (!StringUtils.hasText(roomCode) || !StringUtils.hasText(email)) {
            return;
        }
        String normalizedRoomCode = roomCode.trim().toUpperCase(Locale.ROOT);
        PendingDisconnectState pendingDisconnect = pendingDisconnects.get(normalizedRoomCode);
        if (pendingDisconnect != null && pendingDisconnect.hasEmail(email)) {
            pendingDisconnects.remove(normalizedRoomCode, pendingDisconnect);
        }
    }

    @Transactional
    public void handlePresenceDisconnected(String roomCode, String email, LocalDateTime now) {
        if (!StringUtils.hasText(roomCode) || !StringUtils.hasText(email) || now == null) {
            return;
        }
        BanPickRatingSettingsSnapshot settings = settingsAccessor.getCurrentSettings();
        if (!settings.dodgeEnabled()) {
            return;
        }

        BanPickRoom room;
        try {
            room = findRoomForUpdate(roomCode);
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return;
            }
            throw exception;
        }
        if (!isDraftDodgeWindow(room, settings)) {
            clearPendingDisconnect(room.getRoomCode());
            return;
        }

        User disconnectedUser = findParticipantByEmail(room, email);
        if (disconnectedUser == null) {
            return;
        }

        User activeUser = resolveCurrentDraftUser(room);
        if (!isSameUser(activeUser, disconnectedUser)) {
            return;
        }

        pendingDisconnects.put(
                room.getRoomCode(),
                new PendingDisconnectState(room.getRoomCode(), disconnectedUser.getEmail(), now)
        );
    }

    @Transactional
    public List<BanPickRoomStateResponse> resolveExpiredDisconnectGraceWindows(LocalDateTime now) {
        if (now == null || pendingDisconnects.isEmpty()) {
            return List.of();
        }
        BanPickRatingSettingsSnapshot settings = settingsAccessor.getCurrentSettings();
        if (!settings.dodgeEnabled()) {
            pendingDisconnects.clear();
            return List.of();
        }

        List<BanPickRoomStateResponse> updatedRooms = new ArrayList<>();
        for (PendingDisconnectState pendingDisconnect : List.copyOf(pendingDisconnects.values())) {
            if (pendingDisconnect == null || !pendingDisconnect.isExpiredAt(now, settings.dodgeDisconnectGraceSeconds())) {
                continue;
            }
            resolveExpiredDisconnectGraceWindow(pendingDisconnect, now, settings).ifPresent(updatedRooms::add);
        }
        return updatedRooms;
    }

    private String generateRoomCode() {
        String code;
        do {
            StringBuilder builder = new StringBuilder(ROOM_CODE_LENGTH);
            for (int i = 0; i < ROOM_CODE_LENGTH; i += 1) {
                builder.append(ROOM_CODE_ALPHABET.charAt(RANDOM.nextInt(ROOM_CODE_ALPHABET.length())));
            }
            code = builder.toString();
        } while (roomRepository.existsByRoomCode(code));
        return code;
    }

    private User findOrCreateUser(GoogleUserPrincipal principal) {
        return userRepository.findByEmail(principal.email()).map(existing -> {
            boolean changed = false;
            if (!Objects.equals(existing.getName(), principal.name())) {
                existing.setName(StringUtils.hasText(principal.name()) ? principal.name() : "User");
                changed = true;
            }
            if (!Objects.equals(existing.getAvatarUrl(), principal.picture())) {
                existing.setAvatarUrl(principal.picture());
                changed = true;
            }
            return changed ? userRepository.save(existing) : existing;
        }).orElseGet(() -> {
            User user = new User();
            user.setEmail(principal.email());
            user.setName(StringUtils.hasText(principal.name()) ? principal.name() : "User");
            user.setAvatarUrl(principal.picture());
            user.setRole(principal.isAdmin() ? "Admin" : principal.isStaff() ? "Staff" : "User");
            return userRepository.save(user);
        });
    }

    private User findUser(GoogleUserPrincipal principal) {
        return userRepository.findByEmail(principal.email())
                .orElseGet(() -> findOrCreateUser(principal));
    }

    private BanPickRoom findRoomForUpdate(String roomCode) {
        String normalizedRoomCode = normalizeRoomCode(roomCode);
        return roomRepository.findByRoomCodeForUpdate(normalizedRoomCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    }

    private String normalizeRoomCode(String roomCode) {
        if (!StringUtils.hasText(roomCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room code is required");
        }
        return roomCode.trim().toUpperCase(Locale.ROOT);
    }

    private void ensureCanView(BanPickRoom room, User user) {
        if (isRoomParticipant(room, user)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot view this room");
    }

    private void ensureParticipant(BanPickRoom room, User user) {
        if (!isRoomParticipant(room, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a room participant");
        }
    }

    private void ensureHost(BanPickRoom room, User user) {
        if (!isSameUser(room.getHostUser(), user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only host can perform this action");
        }
    }

    private void ensureBothPlayersJoined(BanPickRoom room) {
        if (room.getHostUser() == null || room.getGuestUser() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room requires both players");
        }
    }

    private void ensureSidesAssigned(BanPickRoom room) {
        if (room.getBlueUser() == null || room.getRedUser() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room sides are not assigned");
        }
    }

    private void ensureBothPlayersReady(BanPickRoom room) {
        if (!Boolean.TRUE.equals(room.getHostReady()) || !Boolean.TRUE.equals(room.getGuestReady())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cả hai người chơi phải sẵn sàng.");
        }
    }

    private void ensureStatus(BanPickRoom room, BanPickRoomStatus... allowedStatuses) {
        for (BanPickRoomStatus status : allowedStatuses) {
            if (room.getStatus() == status) return;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Room status does not allow this action");
    }

    private void ensureLineupAdjustmentPhase(BanPickRoom room) {
        if (room.getPhaseType() != BanPickPhaseType.LINEUP_ADJUSTMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể thực hiện thao tác này trong giai đoạn sắp xếp đội hình.");
        }
    }

    private void ensureSoloCooldownInactive(User user, boolean sameUserAsActor) {
        if (user == null) {
            return;
        }
        if (!settingsAccessor.getCurrentSettings().dodgeEnabled()) {
            return;
        }
        LocalDateTime cooldownUntil = user.getBanPickCooldownUntil();
        if (cooldownUntil == null || !cooldownUntil.isAfter(LocalDateTime.now())) {
            return;
        }
        String subject = sameUserAsActor ? "Bạn" : "Người chơi " + user.resolveDisplayName();
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                subject + " đang trong thời gian cooldown Solo Ban/Pick đến "
                        + cooldownUntil.format(COOLDOWN_TIME_FORMATTER) + "."
        );
    }

    private LocalDateTime calculatePhaseDeadline(LocalDateTime startedAt, BanPickRoom room) {
        int durationSeconds = room.getPhaseDurationSeconds() != null
                ? room.getPhaseDurationSeconds()
                : DEFAULT_PHASE_DURATION_SECONDS;
        return startedAt.plusSeconds(durationSeconds);
    }

    private boolean resolveExpiredPhaseIfNeeded(BanPickRoom room, LocalDateTime now, Set<Long> newActionIds) {
        if (room.getStatus() != BanPickRoomStatus.IN_PROGRESS) {
            return false;
        }

        if (room.getPrepPhaseEndAt() != null) {
            if (rankedRoomContextService.isPrepPhaseActive(room, now)) {
                return false;
            }
            rankedRoomContextService.clearPrepWindow(room);
            room.setTimerStartedAt(now);
            room.setPhaseDeadlineAt(calculatePhaseDeadline(now, room));
            return true;
        }

        if (room.getPhaseType() == BanPickPhaseType.LINEUP_ADJUSTMENT) {
            LocalDateTime lineupDeadline = room.getLineupDeadlineAt();
            if (lineupDeadline == null && room.getTimerStartedAt() != null) {
                lineupDeadline = room.getTimerStartedAt().plusSeconds(LINEUP_ADJUSTMENT_DURATION_SECONDS);
            }
            if (lineupDeadline == null || now.isBefore(lineupDeadline)) {
                return false;
            }
            finishDraft(room);
            return true;
        }

        LocalDateTime deadline = room.getPhaseDeadlineAt();
        if (deadline == null && room.getTimerStartedAt() != null) {
            deadline = calculatePhaseDeadline(room.getTimerStartedAt(), room);
        }
        if (deadline == null || now.isBefore(deadline)) {
            return false;
        }

        DraftPhase phase = getDraftPhase(room, room.getCurrentPhaseIndex());
        if (phase == null) {
            startLineupAdjustment(room, now);
            return true;
        }
        User dodgedUser = phase.teamSide() == BanPickTeamSide.BLUE ? room.getBlueUser() : room.getRedUser();
        if (dodgedUser == null) {
            return false;
        }
        // For RANKED: forfeit with dodge penalty
        // For SIMULATION: just finish the draft normally (no penalty)
        if (BanPickMatchMode.defaultIfNull(room.getMode()) == BanPickMatchMode.RANKED) {
            finishByForfeit(room, dodgedUser, DraftHistoryEndReason.DODGE_TIMEOUT, now);
        } else {
            finishDraft(room);
        }
        return true;
    }

    private void recordHistoryIfFinished(BanPickRoom room) {
        DraftHistoryEndReason endReason = room != null && room.getFinishedEndReason() != null
                ? room.getFinishedEndReason()
                : DraftHistoryEndReason.NORMAL;
        User dodgedUser = room != null ? findParticipantByEmail(room, room.getDodgedUserEmail()) : null;
        clearFinishedDraftMetadata(room);
        recordHistoryIfFinished(room, endReason, dodgedUser);
    }

    private void recordHistoryIfFinished(BanPickRoom room,
                                         DraftHistoryEndReason endReason,
                                         User dodgedUser) {
        if (room.getStatus() != BanPickRoomStatus.FINISHED) {
            return;
        }
        List<BanPickAction> actions = actionRepository.findByRoomOrderByConfirmedAtAsc(room);
        captureFinishedGamePicks(room, actions);
        if (endReason == DraftHistoryEndReason.NORMAL) {
            banPickHistoryService.recordFinishedDraft(room, actions);
            return;
        }
        User winner = resolveOpponent(room, dodgedUser);
        if (dodgedUser == null || winner == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không xác định được người dodge để lưu lịch sử.");
        }
        banPickHistoryService.recordFinishedDraft(
                room,
                actions,
                winner,
                endReason,
                dodgedUser
        );
    }

    private void validateSeriesPickAvailability(BanPickRoom room,
                                                BanPickTeamSide teamSide,
                                                BanPickActionType actionType,
                                                Long heroId) {
        if (actionType != BanPickActionType.PICK || heroId == null) {
            return;
        }
        if (getSeriesRestrictedHeroIds(room, teamSide).contains(heroId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, SERIES_REUSED_HERO_MESSAGE);
        }
    }

    private Set<Long> getSeriesRestrictedHeroIds(BanPickRoom room, BanPickTeamSide teamSide) {
        if (BanPickMatchMode.defaultIfNull(room.getMode()) == BanPickMatchMode.RANKED) {
            String encoded = teamSide == BanPickTeamSide.BLUE
                    ? room.getBluePreviousUsedHeroIds()
                    : room.getRedPreviousUsedHeroIds();
            return new LinkedHashSet<>(parseHeroIdList(encoded));
        }
        if (!isSeriesRestrictionActive(room)) {
            return Set.of();
        }
        String encoded = teamSide == BanPickTeamSide.BLUE
                ? room.getBlueSeriesUsedHeroIds()
                : room.getRedSeriesUsedHeroIds();
        return new LinkedHashSet<>(parseHeroIdList(encoded));
    }

    private boolean isSeriesRestrictionActive(BanPickRoom room) {
        BanPickSeriesType seriesType = seriesType(room);
        if (seriesType == BanPickSeriesType.BO1) {
            return false;
        }
        return !isBo7ResetActive(room);
    }

    private boolean isBo7ResetActive(BanPickRoom room) {
        return seriesType(room) == BanPickSeriesType.BO7 && currentGameNumber(room) == BanPickSeriesType.BO7.getMaxGames();
    }

    private void clearSeriesUsedHeroes(BanPickRoom room) {
        room.setBlueSeriesUsedHeroIds(null);
        room.setRedSeriesUsedHeroIds(null);
    }

    private boolean isFinalGame(BanPickRoom room) {
        return currentGameNumber(room) >= maxGames(room);
    }

    private int currentGameNumber(BanPickRoom room) {
        int currentGameNumber = room.getCurrentGameNumber() != null ? room.getCurrentGameNumber() : 1;
        return Math.max(1, currentGameNumber);
    }

    private int maxGames(BanPickRoom room) {
        return seriesType(room).getMaxGames();
    }

    private BanPickSeriesType seriesType(BanPickRoom room) {
        return BanPickSeriesType.defaultIfNull(room.getSeriesType());
    }

    private void captureFinishedGamePicks(BanPickRoom room, List<BanPickAction> actions) {
        int gameNumber = currentGameNumber(room);
        Map<Integer, List<Long>> blueByGame = parsePickHistory(room.getBlueUsedPicksByGame());
        Map<Integer, List<Long>> redByGame = parsePickHistory(room.getRedUsedPicksByGame());

        if (!blueByGame.containsKey(gameNumber)) {
            blueByGame.put(gameNumber, currentPickOrderForSide(room, BanPickTeamSide.BLUE, actions));
        }
        if (!redByGame.containsKey(gameNumber)) {
            redByGame.put(gameNumber, currentPickOrderForSide(room, BanPickTeamSide.RED, actions));
        }

        room.setBlueUsedPicksByGame(serializePickHistory(blueByGame));
        room.setRedUsedPicksByGame(serializePickHistory(redByGame));
        room.setBlueSeriesUsedHeroIds(serializeHeroIds(flattenHeroIds(blueByGame)));
        room.setRedSeriesUsedHeroIds(serializeHeroIds(flattenHeroIds(redByGame)));
    }

    private void startLineupAdjustment(BanPickRoom room, LocalDateTime now) {
        List<BanPickAction> actions = actionRepository.findByRoomOrderByConfirmedAtAsc(room);
        room.setCurrentPhaseIndex(draftPhasesForRoom(room).size());
        room.setCurrentPhaseSelectedCount(0);
        room.setPhaseType(BanPickPhaseType.LINEUP_ADJUSTMENT);
        room.setTimerStartedAt(now);
        room.setPhaseDeadlineAt(null);
        room.setLineupDeadlineAt(now.plusSeconds(LINEUP_ADJUSTMENT_DURATION_SECONDS));
        room.setBlueLineupConfirmed(false);
        room.setRedLineupConfirmed(false);
        room.setBluePickOrder(serializeHeroIds(heroIdsBySide(actions, BanPickTeamSide.BLUE)));
        room.setRedPickOrder(serializeHeroIds(heroIdsBySide(actions, BanPickTeamSide.RED)));
        clearPendingDisconnect(room.getRoomCode());
    }

    private void finishDraft(BanPickRoom room) {
        room.setStatus(BanPickRoomStatus.FINISHED);
        room.setTimerStartedAt(null);
        room.setPhaseDeadlineAt(null);
        room.setLineupDeadlineAt(null);
        clearPendingDisconnect(room.getRoomCode());
    }

    private void finishByForfeit(BanPickRoom room,
                                 User dodgedUser,
                                 DraftHistoryEndReason endReason,
                                 LocalDateTime now) {
        BanPickRatingSettingsSnapshot settings = settingsAccessor.getCurrentSettings();
        if (!isDraftDodgeWindow(room, settings) || dodgedUser == null) {
            return;
        }
        room.setFinishedEndReason(endReason);
        room.setDodgedUserEmail(dodgedUser.getEmail());
        applySoloDodgeCooldown(dodgedUser, now, settings);
        finishDraft(room);
    }

    private void applySoloDodgeCooldown(User user, LocalDateTime now, BanPickRatingSettingsSnapshot settings) {
        if (user == null || now == null || settings == null || !settings.dodgeEnabled()) {
            return;
        }
        LocalDateTime nextCooldownUntil = now.plusMinutes(settings.dodgeCooldownMinutes());
        LocalDateTime currentCooldownUntil = user.getBanPickCooldownUntil();
        if (currentCooldownUntil == null || currentCooldownUntil.isBefore(nextCooldownUntil)) {
            user.setBanPickCooldownUntil(nextCooldownUntil);
        }
    }

    private List<Long> heroIdsBySide(List<BanPickAction> actions, BanPickTeamSide teamSide) {
        if (actions == null || actions.isEmpty()) {
            return List.of();
        }
        return actions.stream()
                .filter(action -> action.getTeamSide() == teamSide)
                .filter(action -> action.getActionType() == BanPickActionType.PICK)
                .map(BanPickAction::getHeroId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Map<Integer, List<Long>> parsePickHistory(String encoded) {
        Map<Integer, List<Long>> history = new LinkedHashMap<>();
        if (!StringUtils.hasText(encoded)) {
            return history;
        }
        for (String entry : encoded.split(";")) {
            if (!StringUtils.hasText(entry)) {
                continue;
            }
            String[] parts = entry.split(":", 2);
            if (parts.length != 2) {
                continue;
            }
            try {
                int gameNumber = Integer.parseInt(parts[0].trim());
                history.put(gameNumber, parseHeroIdList(parts[1]));
            } catch (NumberFormatException ignored) {
                // Ignore malformed persisted entries and keep the room usable.
            }
        }
        return history;
    }

    private String serializePickHistory(Map<Integer, List<Long>> history) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        List<String> entries = new ArrayList<>();
        history.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> entries.add(entry.getKey() + ":" + serializeHeroIds(entry.getValue())));
        return String.join(";", entries);
    }

    private List<Long> parseHeroIdList(String encoded) {
        if (!StringUtils.hasText(encoded)) {
            return List.of();
        }
        List<Long> heroIds = new ArrayList<>();
        for (String token : encoded.split(",")) {
            if (!StringUtils.hasText(token)) {
                continue;
            }
            try {
                heroIds.add(Long.parseLong(token.trim()));
            } catch (NumberFormatException ignored) {
                // Ignore malformed hero ids and keep the rest of the list.
            }
        }
        return heroIds;
    }

    private String serializeHeroIds(List<Long> heroIds) {
        if (heroIds == null || heroIds.isEmpty()) {
            return "";
        }
        return heroIds.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private List<Long> flattenHeroIds(Map<Integer, List<Long>> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        Set<Long> heroIds = new LinkedHashSet<>();
        history.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> heroIds.addAll(entry.getValue()));
        return new ArrayList<>(heroIds);
    }

    private void advanceToNextPhase(BanPickRoom room, LocalDateTime now) {
        int nextPhaseIndex = (room.getCurrentPhaseIndex() != null ? room.getCurrentPhaseIndex() : 0) + 1;
        room.setCurrentPhaseIndex(nextPhaseIndex);
        room.setCurrentPhaseSelectedCount(0);
        if (nextPhaseIndex >= draftPhasesForRoom(room).size()) {
            startLineupAdjustment(room, now);
            return;
        }
        room.setPhaseType(BanPickPhaseType.DRAFT);
        room.setTimerStartedAt(now);
        room.setPhaseDeadlineAt(calculatePhaseDeadline(now, room));
        room.setLineupDeadlineAt(null);
        room.setBlueLineupConfirmed(false);
        room.setRedLineupConfirmed(false);
        room.setBluePickOrder(null);
        room.setRedPickOrder(null);
    }

    private boolean isRoomParticipant(BanPickRoom room, User user) {
        return isSameUser(room.getHostUser(), user) || isSameUser(room.getGuestUser(), user);
    }

    private boolean isSameUser(User first, User second) {
        return first != null && second != null && Objects.equals(first.getId(), second.getId());
    }

    private boolean isCurrentActivePlayer(BanPickRoom room, User user, BanPickTeamSide teamSide) {
        User activeUser = teamSide == BanPickTeamSide.BLUE ? room.getBlueUser() : room.getRedUser();
        return isSameUser(activeUser, user);
    }

    private BanPickTeamSide resolveUserSide(BanPickRoom room, User user) {
        if (isSameUser(room.getBlueUser(), user)) {
            return BanPickTeamSide.BLUE;
        }
        if (isSameUser(room.getRedUser(), user)) {
            return BanPickTeamSide.RED;
        }
        return null;
    }

    private User resolveCurrentDraftUser(BanPickRoom room) {
        if (room == null || room.getPrepPhaseEndAt() != null) {
            return null;
        }
        DraftPhase currentPhase = getDraftPhase(room, room.getCurrentPhaseIndex());
        if (currentPhase == null) {
            return null;
        }
        return currentPhase.teamSide() == BanPickTeamSide.BLUE ? room.getBlueUser() : room.getRedUser();
    }

    private User resolveOpponent(BanPickRoom room, User user) {
        if (room == null || user == null) {
            return null;
        }
        if (isSameUser(room.getBlueUser(), user)) {
            return room.getRedUser();
        }
        if (isSameUser(room.getRedUser(), user)) {
            return room.getBlueUser();
        }
        return null;
    }

    private User findParticipantByEmail(BanPickRoom room, String email) {
        if (room == null || !StringUtils.hasText(email)) {
            return null;
        }
        if (room.getHostUser() != null && email.equalsIgnoreCase(room.getHostUser().getEmail())) {
            return room.getHostUser();
        }
        if (room.getGuestUser() != null && email.equalsIgnoreCase(room.getGuestUser().getEmail())) {
            return room.getGuestUser();
        }
        return null;
    }

    private boolean isDraftDodgeWindow(BanPickRoom room, BanPickRatingSettingsSnapshot settings) {
        if (room == null || room.getStatus() != BanPickRoomStatus.IN_PROGRESS) {
            return false;
        }
        // Dodge penalty only applies to RANKED mode
        if (BanPickMatchMode.defaultIfNull(room.getMode()) != BanPickMatchMode.RANKED) {
            return false;
        }
        if (room.getPrepPhaseEndAt() != null) {
            return false;
        }
        if (settings == null || settings.dodgeApplyInDraftOnly()) {
            return room.getPhaseType() == BanPickPhaseType.DRAFT;
        }
        return room.getPhaseType() == BanPickPhaseType.DRAFT
                || room.getPhaseType() == BanPickPhaseType.LINEUP_ADJUSTMENT;
    }

    private void clearPendingDisconnect(String roomCode) {
        if (!StringUtils.hasText(roomCode)) {
            return;
        }
        pendingDisconnects.remove(roomCode.trim().toUpperCase(Locale.ROOT));
    }

    private Optional<BanPickRoomStateResponse> resolveExpiredDisconnectGraceWindow(PendingDisconnectState pendingDisconnect,
                                                                                   LocalDateTime now,
                                                                                   BanPickRatingSettingsSnapshot settings) {
        PendingDisconnectState currentPendingDisconnect = pendingDisconnects.get(pendingDisconnect.roomCode());
        if (currentPendingDisconnect == null || !currentPendingDisconnect.equals(pendingDisconnect)) {
            return Optional.empty();
        }

        BanPickRoom room = findRoomForUpdate(pendingDisconnect.roomCode());
        if (!isDraftDodgeWindow(room, settings)) {
            clearPendingDisconnect(room.getRoomCode());
            return Optional.empty();
        }

        User activeUser = resolveCurrentDraftUser(room);
        if (activeUser == null
                || !StringUtils.hasText(activeUser.getEmail())
                || !activeUser.getEmail().equalsIgnoreCase(pendingDisconnect.email())) {
            clearPendingDisconnect(room.getRoomCode());
            return Optional.empty();
        }
        if (!pendingDisconnect.isExpiredAt(now, settings.dodgeDisconnectGraceSeconds())) {
            return Optional.empty();
        }

        finishByForfeit(room, activeUser, DraftHistoryEndReason.DODGE_DISCONNECT, now);
        BanPickRoom savedRoom = roomRepository.save(room);
        recordHistoryIfFinished(savedRoom);
        return Optional.of(buildState(savedRoom, Set.of()));
    }

    private boolean isLineupConfirmed(BanPickRoom room, BanPickTeamSide teamSide) {
        return teamSide == BanPickTeamSide.BLUE
                ? Boolean.TRUE.equals(room.getBlueLineupConfirmed())
                : Boolean.TRUE.equals(room.getRedLineupConfirmed());
    }

    private List<Long> sanitizeHeroIds(List<Long> heroIds) {
        if (heroIds == null) {
            return List.of();
        }
        return heroIds.stream().filter(Objects::nonNull).toList();
    }

    private void validateLineupReorder(List<Long> currentOrder, List<Long> requestedOrder) {
        if (currentOrder.size() != requestedOrder.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh sách đội hình không hợp lệ.");
        }
        Set<Long> currentSet = new LinkedHashSet<>(currentOrder);
        Set<Long> requestedSet = new LinkedHashSet<>(requestedOrder);
        if (requestedSet.size() != requestedOrder.size()
                || currentSet.size() != currentOrder.size()
                || !currentSet.equals(requestedSet)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh sách đội hình không hợp lệ.");
        }
    }

    private List<Long> currentPickOrderForSide(BanPickRoom room, BanPickTeamSide teamSide) {
        return currentPickOrderForSide(room, teamSide, null);
    }

    private List<Long> currentPickOrderForSide(BanPickRoom room,
                                               BanPickTeamSide teamSide,
                                               List<BanPickAction> actions) {
        String encoded = teamSide == BanPickTeamSide.BLUE ? room.getBluePickOrder() : room.getRedPickOrder();
        List<Long> persistedOrder = parseHeroIdList(encoded);
        if (!persistedOrder.isEmpty()) {
            return persistedOrder;
        }
        List<BanPickAction> roomActions = actions != null ? actions : actionRepository.findByRoomOrderByConfirmedAtAsc(room);
        return heroIdsBySide(roomActions, teamSide);
    }

    private void setPickOrder(BanPickRoom room, BanPickTeamSide teamSide, List<Long> heroIds) {
        String encoded = serializeHeroIds(heroIds);
        if (teamSide == BanPickTeamSide.BLUE) {
            room.setBluePickOrder(encoded);
            return;
        }
        room.setRedPickOrder(encoded);
    }

    private void resetLineupState(BanPickRoom room) {
        room.setPhaseType(BanPickPhaseType.DRAFT);
        room.setTimerStartedAt(null);
        room.setPhaseDeadlineAt(null);
        room.setLineupDeadlineAt(null);
        room.setBlueLineupConfirmed(false);
        room.setRedLineupConfirmed(false);
        room.setBluePickOrder(null);
        room.setRedPickOrder(null);
        rankedRoomContextService.clearPrepWindow(room);
        clearFinishedDraftMetadata(room);
    }

    private void clearFinishedDraftMetadata(BanPickRoom room) {
        if (room == null) {
            return;
        }
        room.setFinishedEndReason(null);
        room.setDodgedUserEmail(null);
    }

    private void validateConfirmRequest(BanPickConfirmRequest request) {
        if (request == null || (request.heroId() == null && !StringUtils.hasText(request.heroName()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "heroId or heroName is required");
        }
        if (request.teamSide() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "teamSide is required");
        }
        if (request.actionType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "actionType is required");
        }
    }

    private List<DraftPhase> draftPhasesForRoom(BanPickRoom room) {
        return Boolean.TRUE.equals(room.getUltimateBattle()) ? ULTIMATE_BATTLE_PHASES : DRAFT_PHASES;
    }

    private DraftPhase getDraftPhase(BanPickRoom room, Integer phaseIndex) {
        int index = phaseIndex != null ? phaseIndex : 0;
        List<DraftPhase> phases = draftPhasesForRoom(room);
        return index >= 0 && index < phases.size() ? phases.get(index) : null;
    }

    private Long resolveHeroId(BanPickConfirmRequest request) {
        if (request.heroId() != null && heroRepository.existsById(request.heroId())) {
            return request.heroId();
        }
        if (!StringUtils.hasText(request.heroName())) {
            return null;
        }
        return heroRepository.findFirstByNameIgnoreCase(request.heroName().trim())
                .map(Hero::getId)
                .orElse(null);
    }

    private void syncParticipantSides(BanPickRoom room) {
        List<BanPickRoomParticipant> participants = participantRepository.findByRoomOrderByJoinedAtAsc(room);
        for (BanPickRoomParticipant participant : participants) {
            if (isSameUser(participant.getUser(), room.getBlueUser())) {
                participant.setTeamSide(BanPickTeamSide.BLUE);
            } else if (isSameUser(participant.getUser(), room.getRedUser())) {
                participant.setTeamSide(BanPickTeamSide.RED);
            } else {
                participant.setTeamSide(null);
            }
        }
        participantRepository.saveAll(participants);
    }

    private BanPickRoomStateResponse buildState(BanPickRoom room, Set<Long> newActionIds) {
        return buildState(room, null, newActionIds);
    }

    private BanPickRoomStateResponse buildState(BanPickRoom room, User currentUser) {
        return buildState(room, currentUser, Set.of());
    }    private BanPickRoomStateResponse buildState(BanPickRoom room, User currentUser, Set<Long> newActionIds) {
        List<BanPickRoomParticipant> participantEntities = participantRepository.findByRoomOrderByJoinedAtAsc(room);
        List<BanPickParticipantResponse> participants = participantEntities
                .stream()
                .map(p -> toParticipantResponse(p, currentUser))
                .toList();
        List<BanPickActionResponse> actions = actionRepository.findByRoomOrderByConfirmedAtAsc(room)
                .stream()
                .map(action -> toActionResponse(action, newActionIds.contains(action.getId())))
                .toList();
        DraftPhase currentPhase = room.getStatus() == BanPickRoomStatus.IN_PROGRESS
                && room.getPhaseType() == BanPickPhaseType.DRAFT
                ? getDraftPhase(room, room.getCurrentPhaseIndex())
                : null;
        Long draftHistoryId = room.getStatus() == BanPickRoomStatus.FINISHED
                ? banPickHistoryService.findLatestHistoryIdByRoomCode(room.getRoomCode()).orElse(null)
                : null;
        Map<Integer, List<Long>> blueUsedPicksByGame = parsePickHistory(room.getBlueUsedPicksByGame());
        Map<Integer, List<Long>> redUsedPicksByGame = parsePickHistory(room.getRedUsedPicksByGame());
        List<Long> bluePreviousUsedHeroIds = parseHeroIdList(room.getBluePreviousUsedHeroIds());
        List<Long> redPreviousUsedHeroIds = parseHeroIdList(room.getRedPreviousUsedHeroIds());
        List<Long> blueUsedPicks = activeUsedHeroIds(room, BanPickTeamSide.BLUE);
        List<Long> redUsedPicks = activeUsedHeroIds(room, BanPickTeamSide.RED);
        List<Long> bluePickOrder = currentPickOrderForSide(room, BanPickTeamSide.BLUE);
        List<Long> redPickOrder = currentPickOrderForSide(room, BanPickTeamSide.RED);
        BanPickTeamSide currentUserSide = currentUser != null ? resolveUserSide(room, currentUser) : null;

        // Resolve current user's strategy pool from their participant record
        List<Long> myStrategyPool = null;
        if (currentUser != null) {
            myStrategyPool = participantEntities.stream()
                    .filter(p -> isSameUser(p.getUser(), currentUser))
                    .findFirst()
                    .map(p -> parseHeroIdList(p.getStrategyPool()))
                    .orElse(null);
        }

        return new BanPickRoomStateResponse(
                room.getId(),
                room.getRoomCode(),
                BanPickMatchMode.defaultIfNull(room.getMode()),
                room.getVirtualSeriesFormat(),
                room.getVirtualGameIndex(),
                room.getUltimateBattle(),
                room.getPrepDurationSeconds(),
                room.getPrepPhaseStartAt(),
                room.getPrepPhaseEndAt(),
                bluePreviousUsedHeroIds,
                redPreviousUsedHeroIds,
                room.getStatus(),
                room.getPhaseType(),
                seriesType(room),
                currentGameNumber(room),
                maxGames(room),
                blueUsedPicksByGame,
                redUsedPicksByGame,
                blueUsedPicks,
                redUsedPicks,
                usedHeroesByTeam(blueUsedPicks, redUsedPicks),
                isFinalGame(room),
                isBo7ResetActive(room),
                toUserSummary(room.getHostUser()),
                toUserSummary(room.getGuestUser()),
                toUserSummary(room.getBlueUser()),
                toUserSummary(room.getRedUser()),
                room.getHostReady(),
                room.getGuestReady(),
                room.getCurrentPhaseIndex(),
                room.getCurrentPhaseSelectedCount(),
                room.getPhaseDurationSeconds(),
                room.getTimerStartedAt(),
                room.getPhaseDeadlineAt(),
                room.getLineupDeadlineAt(),
                room.getBlueLineupConfirmed(),
                room.getRedLineupConfirmed(),
                bluePickOrder,
                redPickOrder,
                currentUserSide,
                myStrategyPool,
                room.getCreatedAt(),
                room.getUpdatedAt(),
                draftHistoryId,
                currentPhase != null ? toPhaseResponse(room.getCurrentPhaseIndex(), currentPhase) : null,
                participants,
                actions
        );
    }

    private record PendingDisconnectState(
            String roomCode,
            String email,
            LocalDateTime disconnectedAt
    ) {
        private boolean hasEmail(String currentEmail) {
            return StringUtils.hasText(currentEmail) && email.equalsIgnoreCase(currentEmail.trim());
        }

        private boolean isExpiredAt(LocalDateTime now, int graceSeconds) {
            return disconnectedAt != null
                    && now != null
                    && !disconnectedAt.plusSeconds(Math.max(0, graceSeconds)).isAfter(now);
        }
    }


    private List<Long> activeUsedHeroIds(BanPickRoom room, BanPickTeamSide teamSide) {
        if (BanPickMatchMode.defaultIfNull(room.getMode()) == BanPickMatchMode.RANKED) {
            return parseHeroIdList(teamSide == BanPickTeamSide.BLUE
                    ? room.getBluePreviousUsedHeroIds()
                    : room.getRedPreviousUsedHeroIds());
        }
        return activeSimulationUsedHeroIds(room, teamSide);
    }

    private List<Long> activeSimulationUsedHeroIds(BanPickRoom room, BanPickTeamSide teamSide) {
        return new ArrayList<>(getSeriesRestrictedHeroIds(room, teamSide));
    }

    private Map<String, List<Long>> usedHeroesByTeam(List<Long> blueUsedPicks, List<Long> redUsedPicks) {
        Map<String, List<Long>> usedHeroesByTeam = new LinkedHashMap<>();
        usedHeroesByTeam.put("blue", blueUsedPicks != null ? blueUsedPicks : List.of());
        usedHeroesByTeam.put("red", redUsedPicks != null ? redUsedPicks : List.of());
        return usedHeroesByTeam;
    }

    private BanPickParticipantResponse toParticipantResponse(BanPickRoomParticipant participant, User currentUser) {
        // Only include strategy pool for the current user's own participant entry (keep opponent pool private)
        List<Long> strategyPool = null;
        if (currentUser != null && isSameUser(participant.getUser(), currentUser)) {
            strategyPool = parseHeroIdList(participant.getStrategyPool());
        }
        return new BanPickParticipantResponse(
                toUserSummary(participant.getUser()),
                participant.getRole(),
                participant.getTeamSide(),
                participant.getJoinedAt(),
                strategyPool
        );
    }

    private BanPickActionResponse toActionResponse(BanPickAction action, boolean isNew) {
        HeroSummaryDto hero = heroRepository.findById(action.getHeroId())
                .map(HeroSummaryDto::from)
                .orElse(null);
        return new BanPickActionResponse(
                action.getId(),
                toUserSummary(action.getUser()),
                action.getTeamSide(),
                action.getActionType(),
                action.getHeroId(),
                hero != null ? hero.name() : null,
                hero,
                action.getPhaseIndex(),
                action.getConfirmedAt(),
                isNew
        );
    }

    private BanPickPhaseResponse toPhaseResponse(Integer phaseIndex, DraftPhase phase) {
        return new BanPickPhaseResponse(
                phaseIndex,
                phase.teamSide(),
                phase.actionType(),
                phase.count(),
                phase.label()
        );
    }

    private BanPickUserSummary toUserSummary(User user) {
        if (user == null) return null;
        return new BanPickUserSummary(
                user.getId(),
                user.getEmail(),
                user.resolveDisplayName(),
                user.getAvatarUrl()
        );
    }

    private record DraftPhase(
            BanPickTeamSide teamSide,
            BanPickActionType actionType,
            int count,
            String label
    ) {
    }
}
