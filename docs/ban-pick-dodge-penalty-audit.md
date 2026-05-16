# Ban/Pick Dodge Penalty Audit

Ngày audit: 2026-05-16

Mục tiêu của file này là soi flow Solo Ban/Pick hiện tại trước khi code Task 6B Dodge Penalty, để tránh suy luận sai về state runtime, disconnect, timeout, reset room, và khả năng lưu penalty/cooldown.

## 1. Current flow summary

### Lobby

- Backend tạo room qua `BanPickRoomService.createRoom()`.
- Room mới có:
  - `status = WAITING`
  - `phaseType = DRAFT`
  - `hostUser` đã gắn
  - chưa có `guestUser`, `blueUser`, `redUser`
  - chưa có timer active
- Guest tham gia qua `joinRoom()`:
  - nếu room đang `IN_PROGRESS` hoặc `FINISHED` thì bị chặn
  - nếu room đã có guest thì bị chặn
  - nếu join thành công thì `guestUser` được gắn và `status` chuyển sang `READY`
- Không có API `leave room`.
- Không có API `cancel room`.
- `BanPickRoomStatus.CANCELLED` tồn tại trong enum nhưng hiện không có flow nào dùng.
- `ban_pick_room_participants` chỉ lưu `room`, `user`, `role`, `teamSide`, `joinedAt`.
- Nếu một người đóng tab ở lobby thì backend không remove họ khỏi room; slot host/guest vẫn còn trong DB.

### Ready / start

- `readyRoom()` chỉ set cờ:
  - host -> `hostReady = true`
  - guest -> `guestReady = true`
- `rollSide()` chỉ host gọi được, random Blue/Red một lần.
- `startRoom()` yêu cầu:
  - đủ 2 người
  - đã roll side
  - cả 2 cùng ready
  - room đang `READY`
- Khi start:
  - `status = IN_PROGRESS`
  - `phaseType = DRAFT`
  - `currentPhaseIndex = 0`
  - `currentPhaseSelectedCount = 0`
  - `timerStartedAt = now`
  - `phaseDeadlineAt = now + phaseDuration`

### Draft ban/pick

- Solo hiện chạy 15 phase trong `BanPickRoomService.DRAFT_PHASES`.
- Mỗi phase xác định:
  - `teamSide` đang được quyền thao tác
  - `actionType = BAN/PICK`
  - `count`
- `confirmAction()` chỉ cho phép:
  - room `IN_PROGRESS`
  - chưa vào `LINEUP_ADJUSTMENT`
  - đúng side
  - đúng action type
  - đúng active player theo `blueUser` / `redUser`
- Các action được lưu vào `ban_pick_actions` với:
  - `user`
  - `teamSide`
  - `actionType`
  - `heroId`
  - `phaseIndex`
  - `confirmedAt`

### Timer / timeout

- Backend có scheduler `BanPickRoomTimeoutScheduler` chạy mỗi 1 giây.
- Scheduler gọi:
  - `findExpiredRoomCodes(now)`
  - `resolveExpiredPhase(roomCode)`
- Logic timeout hiện tại trong `resolveExpiredPhaseIfNeeded()`:
  - Nếu đang `LINEUP_ADJUSTMENT` và hết giờ:
    - `finishDraft(room)`
  - Nếu đang phase draft và hết giờ:
    - phase BAN: skip thẳng sang phase kế tiếp
    - phase PICK: auto-pick random hero còn hợp lệ rồi sang phase kế tiếp
- Nghĩa là timeout hiện là soft timeout:
  - không có forfeit
  - không có rating penalty
  - không có record ai làm hết giờ

### Lineup adjustment

- Sau phase pick cuối, backend không finish ngay.
- `startLineupAdjustment()` sẽ:
  - `phaseType = LINEUP_ADJUSTMENT`
  - `lineupDeadlineAt = now + 30s`
  - set `bluePickOrder`, `redPickOrder`
  - reset `blueLineupConfirmed`, `redLineupConfirmed`
- Mỗi bên có thể:
  - reorder lineup của chính mình
  - confirm lineup của chính mình
- Khi cả 2 confirm:
  - `finishDraft(room)`
- Nếu hết giờ lineup:
  - backend finish tự động dù chưa biết ai cố tình không confirm

### Finished

- Khi room `FINISHED`, `BanPickRoomService.recordHistoryIfFinished()` gọi `BanPickHistoryService.recordFinishedDraft(room, actions)`.
- `BanPickHistoryService` hiện:
  - lấy picks/bans từ action và pick order cuối
  - tính winner tự động theo Ban/Pick Score
  - tie thì `winnerUser = null`, `win/loss delta = 0/0`
  - rating logic sau đó đi qua base + macro + ELO gap + anti-win-trading
- History chỉ được lưu khi room đã `FINISHED`.
- Partial draft chưa finish thì hiện không có row audit nào trong `draft_histories`.

### Reset / next game

- `nextGame()`:
  - chỉ host gọi được
  - chỉ dùng khi room `FINISHED`
  - capture picks ván trước
  - xoá action hiện tại
  - tăng `currentGameNumber`
  - quay lại `IN_PROGRESS`
- `resetRoom()`:
  - chỉ host gọi được
  - hiện cho phép ở cả `WAITING`, `READY`, `IN_PROGRESS`, `FINISHED`
  - xoá toàn bộ action
  - clear side assignment, ready flags, series state, lineup state
  - giữ nguyên `hostUser`, `guestUser`, participant rows
- Đây là lỗ hổng quan trọng cho Task 6B:
  - host có thể reset giữa trận để xoá flow draft mà không sinh history và không mất điểm

### Disconnect / presence

- Presence hiện nằm ở `BanPickPresenceEventListener`.
- Nó chỉ theo dõi WebSocket/STOMP:
  - `SessionSubscribeEvent`
  - `SessionDisconnectEvent`
- Payload presence broadcast hiện gồm:
  - `roomCode`
  - `connectedEmails`
  - `updatedAt`
- Presence chỉ là state in-memory:
  - không lưu DB
  - không gọi sang `BanPickRoomService`
  - không có grace timer
  - không có `disconnectedAt`
- Frontend `ban-pick.js`:
  - subscribe room topic và presence topic
  - nếu đối thủ mất presence khi room đang `IN_PROGRESS` thì chỉ hiện message "Đối thủ đã mất kết nối"
  - có reconnect tự động STOMP `reconnectDelay = 3000`
  - có fallback polling mỗi 2 giây khi realtime lỗi
- Không có `beforeunload` / `pagehide` / API leave riêng.
- Vì vậy:
  - đóng tab/browser
  - mất mạng
  - refresh tab
  - WebSocket drop nhưng HTTP polling vẫn còn
  đều đang quy về cùng một lớp tín hiệu rất mơ hồ là "presence biến mất".

## 2. Dodge definition proposal

Khuyến nghị cho Task 6B v1: chỉ coi là dodge khi room đã thực sự vào trận, và chỉ phạt chắc tay ở phase draft.

### Nên tính là dodge

- Player đang ở room `IN_PROGRESS`, `phaseType = DRAFT`, rồi disconnect/đóng tab/mất socket và không quay lại trong grace window.
- Player để hết giờ ở đúng turn draft của mình, nếu product quyết định timeout turn = thua trắng thay vì auto-skip/auto-pick như hiện tại.
- Host cố reset room giữa lúc `IN_PROGRESS`, nếu vẫn giữ endpoint `reset` cho mid-draft.

### Không nên tính là dodge

- Rời/mất kết nối ở lobby `WAITING` trước khi đủ người.
- Rời/mất kết nối ở trạng thái `READY` nhưng room chưa `start`.
- WebSocket disconnect ngắn rồi reconnect lại trong grace window.
- Refresh tab rồi load lại room bình thường.
- Rời sau khi room đã `FINISHED`.
- `next-game` sau khi room đã `FINISHED`.
- Reset room trước khi trận bắt đầu.

### Nên trì hoãn quyết định ở v1

- Thoát ở `LINEUP_ADJUSTMENT`.

Lý do:

- Về mặt state hiện tại, ta có thể biết `blueLineupConfirmed` / `redLineupConfirmed`.
- Nhưng lineup timeout đang auto-finish và không có metadata riêng để phân biệt:
  - ai cố tình bỏ
  - ai lag
  - cả 2 bên đều chưa confirm
- Nếu phạt ở lineup ngay từ v1 thì dễ false positive hơn draft turn timeout/disconnect.

Khuyến nghị thực thi v1:

- Dodge chỉ áp cho `status = IN_PROGRESS` và `phaseType = DRAFT`.
- `LINEUP_ADJUSTMENT` giữ nguyên soft-finish như hiện tại, chưa phạt.

## 3. Current code support

### State đã có và dùng được ngay

- `BanPickRoom.status`
  - `WAITING`, `READY`, `IN_PROGRESS`, `FINISHED`
- `BanPickRoom.phaseType`
  - `DRAFT`, `LINEUP_ADJUSTMENT`
- `hostUser`, `guestUser`, `blueUser`, `redUser`
- `hostReady`, `guestReady`
- `currentPhaseIndex`, `currentPhaseSelectedCount`
- `currentPhase.teamSide` và `currentPhase.actionType` trong response
- `timerStartedAt`, `phaseDeadlineAt`, `lineupDeadlineAt`
- `BanPickAction.confirmedAt`
- `BanPickRoomParticipant.role`, `teamSide`, `joinedAt`
- Presence payload theo `roomCode + connectedEmails`
- Scheduler timeout backend chạy đều mỗi giây
- Reconnect flow của participant hiện đã có:
  - participant cũ vẫn đọc được room mid-game qua `GET /api/ban-pick/rooms/{roomCode}`
  - frontend tự reconnect socket / polling

### State có thể suy ra ở runtime, nhưng chưa được lưu bền

- Người đang tới lượt:
  - suy ra từ `currentPhaseIndex -> DraftPhase.teamSide -> blueUser/redUser`
- Người vừa disconnect:
  - suy ra từ `SessionDisconnectEvent` + email + roomCode
- Người chưa confirm lineup:
  - suy ra từ `blueLineupConfirmed` / `redLineupConfirmed`

### State còn thiếu

- Không có `leave room` event rõ nghĩa từ client.
- Không có `disconnectedAt` lưu trong DB.
- Không có `reconnectedAt`.
- Không có `graceDeadlineAt`.
- Không có `dodgedUserId`.
- Không có `endReason` cho room hoặc history.
- Không có `cooldownUntil`.
- Không có penalty counter riêng cho Solo Ban/Pick.
- Không có field để phân biệt:
  - normal finish
  - tie
  - forfeit by dodge
  - host reset mid-draft

### Kết luận phần code support

- Code hiện tại đủ runtime state để implement một dodge detector v1 cho `IN_PROGRESS + DRAFT`.
- Code hiện tại chưa đủ persisted state để làm audit sạch và cooldown sạch.

## 4. DB impact assessment

### `ban_pick_rooms`

Hiện có:

- status
- phaseType
- ready flags
- timer/deadline fields
- user assignments

Đánh giá:

- Đủ để biết room có đang ở draft/live hay không.
- Không đủ để lưu dodge owner hoặc grace state.
- Không phù hợp để lưu cooldown dài hạn vì room là runtime object.

### `ban_pick_room_participants`

Hiện có:

- role
- teamSide
- joinedAt

Đánh giá:

- Chỉ đủ để biết ai từng ở trong room và side hiện tại.
- Không có:
  - `leftAt`
  - `disconnectedAt`
  - `lastSeenAt`
  - `readyAt`

### `draft_histories`

Hiện có:

- `blue_user_id`
- `red_user_id`
- `winner_user_id`
- bans/picks
- `resultRecordedAt`
- `win_rating_delta`
- `loss_rating_delta`

Đánh giá:

- Nếu chỉ cần áp rating forfeit và rolling-50 replay, về mặt kỹ thuật có thể tái dùng bảng này.
- Có thể infer loser là người dodge nếu winner được set bằng opponent.
- Nhưng hiện không thể phân biệt rõ:
  - thua bình thường
  - thua vì dodge
  - thua vì timeout turn
  - match bị reset/abort

Khuyến nghị:

- Nên thêm ít nhất:
  - `end_reason`
  - `dodged_user_id`

### `player_stats`

Hiện có:

- wins
- losses
- totalMatches
- rating

Đánh giá:

- Đủ để replay rating và W/L nếu forfeit match đã được lưu thành history hợp lệ.
- Không phù hợp để lưu cooldown hoặc audit chi tiết dodge.

### `users`

Hiện có:

- `status = ACTIVE/LOCKED`
- `note`

Đánh giá:

- `status = LOCKED` là global account state, không phù hợp cho cooldown Solo Ban/Pick theo phút/giờ.
- `note` là text tự do, không nên dùng làm machine state.
- Không có field cooldown riêng cho Ban/Pick.

### Đề xuất DB tối thiểu cho Task 6B

Khuyến nghị tối thiểu, không overengineer:

- `users.ban_pick_cooldown_until DATETIME NULL`
- `draft_histories.end_reason VARCHAR(32) NULL`
- `draft_histories.dodged_user_id BIGINT NULL`

Giá trị `end_reason` gợi ý:

- `NORMAL`
- `TIE`
- `DODGE_FORFEIT`

Field optional nếu muốn audit sâu hơn:

- `draft_histories.dodge_phase_type`
- `draft_histories.dodge_phase_index`
- `draft_histories.dodge_trigger`

Không khuyến nghị cho v1:

- tạo table penalty riêng ngay từ đầu nếu product chưa cần lịch sử nhiều lớp.

## 5. Implementation plan for Task 6B

### Backend changes

1. Thêm guard cooldown

- Check cooldown ở:
  - `createRoom()`
  - `joinRoom()`
- Có thể check thêm ở `startRoom()` như safety net.

2. Thêm khái niệm grace window cho disconnect

- `BanPickPresenceEventListener` không chỉ broadcast presence nữa, mà cần gọi một service/domain method kiểu:
  - `markDisconnected(roomCode, email, now)`
  - `markReconnected(roomCode, email, now)`
- Không nên phạt ngay khi vừa disconnect.
- Nên có grace window khoảng 10-15 giây vì frontend có:
  - STOMP reconnect delay 3 giây
  - fallback polling 2 giây

3. Chặn escape hatch `reset` mid-draft

Khuyến nghị đơn giản nhất:

- Không cho `resetRoom()` khi `status = IN_PROGRESS`.

Nếu product muốn phạt host reset:

- `resetRoom()` khi `IN_PROGRESS` phải đi qua flow forfeit, không được xoá trắng room state.

4. Thay đổi timeout logic

Hiện tại timeout draft đang:

- BAN -> skip
- PICK -> auto-pick

Nếu Task 6B muốn "timeout = thua trắng", cần thay logic này thành:

- xác định active side ở phase hết giờ
- map side -> user
- gọi `finishByForfeit(room, dodgedUser, trigger)`

5. Tạo flow finish by forfeit

Cần thêm nhánh tách riêng trong `BanPickRoomService`:

- `finishByForfeit(room, dodgedUser, reason)`

Flow này nên:

- set room `FINISHED`
- clear timer/deadline
- ghi metadata dodge nếu có schema
- gọi history service bằng winner cưỡng bức, không dùng auto-evaluate theo score

6. Mở rộng `BanPickHistoryService`

Hiện `recordFinishedDraft()` đang luôn tính winner từ Ban/Pick Score.

Task 6B sẽ cần thêm một đường riêng, ví dụ:

- `recordFinishedDraft(room, actions, forcedWinner, endReason, dodgedUser)`

để:

- match dodge không phụ thuộc đủ 10 picks
- winner là opponent của người dodge
- rating delta và rolling-50 snapshot vẫn lưu được

7. Rating / penalty policy gợi ý cho 6B

Khuyến nghị v1 đơn giản:

- Dodge trong draft:
  - dodger = thua trắng
  - opponent = thắng
  - vẫn đi qua macro/gap hay không là quyết định sản phẩm

Khuyến nghị an toàn hơn cho v1:

- Forfeit match dùng snapshot delta cố định riêng:
  - winner lấy win delta cơ sở hoặc macro-only
  - dodger nhận loss penalty cơ sở hoặc penalty loss riêng

Lý do:

- Dodge không phải một draft hoàn tất bình thường.
- Áp full score-based path và gap/macro history có thể khó giải thích hơn cho người chơi.

8. Cooldown policy gợi ý

Vì app không có "queue" thật, "cấm tìm trận" trong hệ hiện tại nên hiểu là:

- không được `create room`
- không được `join room`

Cooldown gợi ý cho v1:

- dodge lần đầu: 5 phút
- nếu cần escalation sau này thì làm sau

### Frontend changes

- Khi backend trả cooldown active:
  - create/join room hiển thị thông báo rõ còn bao lâu
- Khi đối thủ disconnect trong draft:
  - UI có thể hiện "Đối thủ mất kết nối, chờ X giây trước khi xử thua"
- Nếu backend block `reset` mid-draft:
  - disable hoặc ẩn nút reset trong online draft khi `status = IN_PROGRESS`
- Không cần UI lớn hơn mức này ở v1

### DB changes

Khuyến nghị cho Task 6B:

- Có migration.
- Không nên cố nhồi cooldown vào field hiện có.

### Tests cần thêm

- Disconnect ở lobby `WAITING` không bị phạt.
- Disconnect ở `READY` chưa start không bị phạt.
- Disconnect giữa draft nhưng reconnect trước grace window không bị phạt.
- Disconnect giữa draft, quá grace window, room bị xử thua trắng đúng user.
- Hết timer ở turn draft -> không còn auto-pick/skip, mà bị forfeit nếu sản phẩm chốt rule đó.
- `resetRoom()` khi `IN_PROGRESS` bị chặn hoặc bị convert thành dodge theo rule đã chọn.
- Disconnect sau `FINISHED` không bị phạt.
- Rolling-50 replay đọc đúng snapshot của forfeit match.
- User đang cooldown không thể create/join room.

## 6. Risks

### False positive vì mạng lag

- Presence hiện dựa trên WebSocket session, không phải health state toàn diện.
- Có thể bị drop socket ngắn trong khi user vẫn còn tab và sắp reconnect.

### Reconnect hợp lệ

- Frontend đã có reconnect delay 3 giây và polling 2 giây.
- Nếu backend phạt quá nhanh sẽ giết nhầm người refresh tab hoặc đổi mạng.

### User đóng tab tạm thời

- Browser close/tab refresh hiện đều đi qua cùng lớp disconnect signal.
- Không có explicit "tôi rời trận" vs "tab vừa reload".

### Room đã finished / summary page

- User vẫn có thể ngắt socket sau `FINISHED`.
- Không được dùng disconnect thuần để phạt khi room đã xong.

### Reset room giữa trận

- Đây là risk lớn nhất hiện tại.
- Host có thể xoá flow mid-draft mà không tạo history.

### Lobby slot bị kẹt

- Không có leave room, không có kick guest, không có auto-cancel room.
- Một guest biến mất ở lobby vẫn chiếm slot; đây là vấn đề UX riêng, nhưng không nên gộp vội vào dodge rating.

## Kết luận

Kết luận ngắn gọn:

- Runtime state hiện tại đủ để implement một Dodge Penalty v1 cho `IN_PROGRESS + DRAFT`.
- Runtime state hiện tại chưa đủ sạch để phạt dựa trên presence alone; bắt buộc nên có grace window.
- Persistence hiện tại chưa đủ cho cooldown và audit reason.
- Nếu làm Task 6B đúng và an toàn, nhiều khả năng sẽ cần DB change tối thiểu cho:
  - `users.ban_pick_cooldown_until`
  - `draft_histories.end_reason`
  - `draft_histories.dodged_user_id`

## Implemented v1 (2026-05-16)

- Scope:
  - Dodge Penalty v1 chi ap dung khi room dang `IN_PROGRESS` va `phaseType = DRAFT`.
  - Khong phat lobby `WAITING/READY`.
  - Khong phat room da `FINISHED`.
  - Khong phat `LINEUP_ADJUSTMENT` trong v1.

- Trigger da implement:
  - Current-turn player timeout trong draft -> room finish theo forfeit.
  - Current-turn player disconnect trong draft va khong reconnect trong grace window `10s` -> room finish theo forfeit.
  - `resetRoom()` khi room dang `IN_PROGRESS` bi reject de dong escape hatch.

- Rating va history:
  - Dodge van di qua pipeline rating hien co: macro economy, ELO gap modifier, anti-win-trading, snapshot delta, rolling-50 replay.
  - `draft_histories` luu them `end_reason` va `dodged_user_id`.
  - `end_reason` v1 dung: `NORMAL`, `DODGE_TIMEOUT`, `DODGE_DISCONNECT`, `DODGE_RESET`.
  - Neu anti-win-trading block pair rating thi history dodge van luu, nhung delta co the la `0/0`.

- Cooldown:
  - Them `users.ban_pick_cooldown_until`.
  - Cooldown v1 = `5 minutes`.
  - User dang cooldown khong the create, join, hoac start room Solo Ban/Pick.

- Persisted state:
  - DB da bo sung:
    - `users.ban_pick_cooldown_until`
    - `draft_histories.end_reason`
    - `draft_histories.dodged_user_id`
  - `disconnectedAt` van chi xu ly runtime qua pending disconnect grace, chua persist vao DB o v1.
- Điểm cần xử lý đầu tiên ở implementation không phải disconnect UI, mà là:
  - chốt định nghĩa dodge chỉ trong `IN_PROGRESS + DRAFT`
  - sửa timeout path
  - đóng hoặc xử lý `resetRoom()` giữa trận
