# Admin Solo Ban/Pick Control Preview

## 1. Current admin structure

### Current admin pages

| Route / file | Current purpose | Status |
|---|---|---|
| `demo/src/main/resources/static/html/admin.html` | Dashboard shell nhiều section trong cùng 1 page: `dashboard`, `guides`, `users`, `ban-pick-rating`, `aer-data` | Đang là entry chính cho dashboard admin |
| `demo/src/main/resources/static/html/admin-heroes.html` | Hero management | Đang chạy riêng bằng JS module riêng |
| `demo/src/main/resources/static/html/admin-attributes.html` | Attributes management | Đang chạy riêng bằng JS module riêng |
| `demo/src/main/resources/static/html/admin-wiki-data.html` | Spells & Enchantments | Đang chạy riêng bằng JS module riêng |
| `demo/src/main/resources/static/html/admin-esports-data.html` | Esports Data admin | Đang chạy riêng bằng JS module riêng |
| `demo/src/main/resources/static/html/admin-sidebar.html` | Shared admin navigation partial | Là source of truth cho sidebar runtime |

### Shared layout / sidebar

- `demo/src/main/resources/static/html/admin-sidebar.html` là shared sidebar thực tế.
- `demo/src/main/resources/static/js/header-loader.js` inject sidebar vào `#admin-sidebar-placeholder` và set active state theo `pathname` + `hash`.
- Các page admin standalone đều dùng pattern:
  - `#admin-sidebar-placeholder`
  - `.admin-shell`
  - `.topbar`
  - `body[data-page="..."]`
- `header-loader.js` đã map `#ban-pick-rating` thành active key `admin-ban-pick-rating`, nên hash route này đã khớp với shared nav hiện có.

### Legacy / duplicated admin sidebar markup

- `admin.html`, `admin-heroes.html`, `admin-attributes.html`, `admin-wiki-data.html`, `admin-esports-data.html` đều còn giữ một `<aside ... hidden>` cũ trong file.
- Các `<aside hidden>` này không nên được xem là source of truth nữa.
- Nếu thêm/chỉnh menu admin, nên ưu tiên `admin-sidebar.html`, không mở rộng các block hidden cũ.

### Current placement recommendation

- Nên đặt `Solo Ban/Pick Control` tại:
  - sidebar section `Ban/Pick` trong `admin-sidebar.html`
  - content section `admin.html#ban-pick-rating`
- Không nên tạo page mới ngay ở bước đầu, vì:
  - hash route `#ban-pick-rating` đã được wire ở shared layout
  - `admin.html` đã có sẵn section shell cho module này
  - pattern repo hiện tại đang để `Users` và `AER Data` chung trong `admin.html`

### Important current frontend findings

- `admin.html` đã có shell UI khá đầy đủ cho `Solo Rating Control`:
  - summary cards
  - tabs `Base`, `Macro`, `ELO Gap`, `Anti-trading`, `Dodge`, `Seasonal Reset`, `Diagnostics`
  - preview reset card
  - execute reset danger card
- Nhưng frontend này chưa hoàn thiện:
  - `admin.html` có load `/js/admin.js?v=20260518-ban-pick-rating-control`
  - file `demo/src/main/resources/static/js/admin.js` hiện không tồn tại trong repo
  - `admin.html` chỉ gọi `window.loadBanPickRatingControl()` nếu function này tồn tại
  - mình không thấy JS runtime nào khác đang wire `bp-*` controls
- CSS cũng đang dang dở:
  - `admin.html` dùng nhiều class custom như `bp-rating-summary-grid`, `bp-status`, `bp-diagnostics-grid`, `bp-danger-card`
  - các class `bp-*` này hiện không thấy định nghĩa tương ứng trong `demo/src/main/resources/static/css/admin.css`

### Documentation drift found during audit

- `docs/project-structure.md` vẫn mô tả Solo Ban/Pick reset là backend admin API, chưa có admin page lớn.
- `docs/so-do-tu-duy.md` vẫn liệt kê `demo/src/main/resources/static/js/admin.js`, nhưng file này hiện không tồn tại.

## 2. Existing Solo Ban/Pick admin APIs

### Existing admin endpoints

| Feature | Method | Endpoint | Current status | Notes |
|---|---|---|---|---|
| Load rating control settings | `GET` | `/api/admin/ban-pick/rating-settings` | Đã có | Trả full settings + diagnostics trong 1 payload |
| Save rating control settings | `PUT` | `/api/admin/ban-pick/rating-settings` | Đã có | Validate range, lưu DB, cập nhật `updatedAt/updatedBy` |
| Reset settings về default | `POST` | `/api/admin/ban-pick/rating-settings/reset-defaults` | Đã có | Reset single-row config về defaults |
| Preview seasonal reset | `GET` | `/api/admin/ban-pick/rank-reset/preview?type=SOFT|HARD` | Đã có | Không mutate DB |
| Execute seasonal reset | `POST` | `/api/admin/ban-pick/rank-reset` | Đã có | Cần `confirmationText` chính xác |

### Settings already supported by backend

`GET /api/admin/ban-pick/rating-settings` đã gói sẵn các group này:

- `base`
  - `initialRating`
  - `baseWinDelta`
  - `baseLossDelta`
  - `minRating`
- `macro`
  - `enabled`
  - `activeWindowDays`
  - `balanceRating`
  - `activeTopPercent`
  - `ratingStep`
  - `winAdjustmentPerStep`
  - `minWinDelta`
  - `minimumActivePlayers`
- `gap`
  - `enabled`
  - `ratingDiffStep`
  - `modifierPerStep`
  - `maxModifier`
- `antiTrading`
  - `enabled`
  - `windowHours`
  - `allowedRecentMatches`
  - `blockedWinDelta = 0`
  - `blockedLossDelta = 0`
- `dodge`
  - `enabled`
  - `disconnectGraceSeconds`
  - `cooldownMinutes`
  - `applyInDraftOnly`
  - `rejectResetDuringDraft`
- `seasonalReset`
  - `schedulerEnabled`
  - `softResetMonths`
  - `hardResetMonths`
  - `hardPriorityOverSoft`
  - `confirmationText`
- `diagnostics`
  - `currentMacroWinDelta`
  - `currentActivePlayerCount`
  - `currentActivePoolSize`
  - `activePoolAverageRating`
  - `lastResetLog`
  - `nextScheduledReset`
  - `updatedAt`
  - `updatedBy`
  - `replayAnchorAdvanced`

### Seasonal reset behavior already supported

- Preview support:
  - `SOFT` và `HARD`
  - trả `affectedPlayerCount`
  - trả before/after min/max/avg
  - trả sample players
- Execute support:
  - manual reset có `type`, `confirmationText`, `note`
  - confirmation text hiện là `RESET SOLO RANK`
  - có audit log vào `ban_pick_rank_resets`
  - có idempotency theo `scheduled_date`
- Scheduler support:
  - service đã support determine next reset / run scheduled reset if due
  - source of truth runtime hiện là settings row `seasonSchedulerEnabled`

### Diagnostics status

- Diagnostics đã có backend.
- Nhưng diagnostics hiện đang là embedded data trong `GET /rating-settings`.
- Chưa có endpoint read-only riêng kiểu `/api/admin/ban-pick/diagnostics`.

## 3. Missing backend/API/config

### Missing or incomplete frontend/runtime wiring

| Item | Current state | Impact |
|---|---|---|
| `demo/src/main/resources/static/js/admin.js` | Không tồn tại | `admin.html` đang reference một asset thiếu |
| `window.loadBanPickRatingControl()` | Không thấy implementation | section `ban-pick-rating` chưa có loader thật |
| `bp-*` CSS blocks | Không thấy trong `admin.css` | shell UI có thể render thô hoặc thiếu layout |
| Button/action wiring cho `bp-*` controls | Không thấy JS handler | save / reset defaults / preview / execute chưa usable từ UI |

### Missing or partial API surface

- Không có endpoint riêng để lấy:
  - danh sách reset log lịch sử
  - danh sách user đang cooldown Solo Ban/Pick
  - danh sách match/pair bị anti-trading block gần đây
  - thống kê dodge gần đây theo `end_reason`
- Đây không phải blocker cho v1 control panel.
- Nhưng nếu muốn tab `Diagnostics` có giá trị vận hành thật, rất có thể sẽ cần ít nhất 1 hoặc 2 read-only endpoint bổ sung.

### Partial or misleading backend controls

- `dodgeRejectResetDuringDraft` hiện được persist và expose qua API.
- Tuy nhiên hành vi thực tế đang chưa thật sự toggle được:
  - `resetRoom()` có check riêng `if (IN_PROGRESS && dodgeRejectResetDuringDraft())`
  - nhưng ngay sau đó `ensureStatus(...)` vẫn không cho `IN_PROGRESS`
  - kết quả là dù flag này là `false`, reset room đang draft vẫn không đi qua được
- Kết luận:
  - field này hiện là `persisted config`, nhưng behavior thực tế đang gần như fixed reject
  - cần cleanup backend hoặc ẩn field khỏi UI cho tới khi semantics rõ ràng

### Config drift

- `demo/src/main/resources/application.properties` vẫn có:
  - `banpick.season-reset.scheduler-enabled=false`
- Nhưng runtime scheduler gate trong code hiện tại không đọc property này để quyết định bật/tắt logic reset.
- Gate thực tế đang nằm ở:
  - `ban_pick_rating_config.season_scheduler_enabled`
  - đọc qua `BanPickRatingSettingsService` -> `BanPickSeasonResetService`
- Kết luận:
  - property này hiện có tính legacy/documentation hơn là source of truth runtime
  - UI/admin docs nên coi DB settings row là source of truth hiện tại

### Environment risk to note

- DB support cho settings đã có file SQL:
  - `demo/sql/add_ban_pick_rating_settings.sql`
- DB support cho dodge/reset cũng đã có file SQL:
  - `demo/sql/add_ban_pick_dodge_penalty_fields.sql`
  - `demo/sql/add_ban_pick_season_reset_support.sql`
- Local app đang để `spring.jpa.hibernate.ddl-auto=update`, nên local dev có thể tự đỡ phần schema.
- Nhưng nếu môi trường staging/prod không rely vào `ddl-auto=update`, cần xác nhận `ban_pick_rating_config` đã tồn tại trước khi rollout admin panel này.

## 4. Recommended Admin UI layout

### Recommended route and structure

- Giữ route:
  - `/html/admin.html#ban-pick-rating`
- Không tách page mới ở bước đầu.
- Dùng chính section `page-ban-pick-rating` đã scaffold sẵn trong `admin.html`.

### Recommended layout by zone

#### Header summary

- 4 summary cards ở đầu section là hợp lý và nên giữ:
  - `Macro win delta`
  - `Active pool`
  - `Next reset`
  - `Updated by / updated at`
- Đây là zone đọc nhanh trước khi admin mở từng tab.

#### Tabs

- Giữ đúng 7 tab hiện scaffold:
  - `Base`
  - `Macro`
  - `ELO Gap`
  - `Anti-trading`
  - `Dodge`
  - `Seasonal Reset`
  - `Diagnostics`

#### Base

- Input group:
  - `Initial Rating`
  - `Min Rating`
  - `Base Win Delta`
  - `Base Loss Delta`
- Thêm helper note rõ:
  - đổi các field này là replay-sensitive
  - có thể làm `replayAnchorAdvanced = true`

#### Macro

- 1 toggle enable + grid numeric fields.
- Nên có note read-only:
  - snapshot tính theo ngày
  - active pool lấy từ completed matches window hiện tại

#### ELO Gap

- 1 toggle enable + 3 numeric fields.
- Dùng wording nhất quán với backend:
  - `ratingDiffStep`
  - `modifierPerStep`
  - `maxModifier`

#### Anti-trading

- 1 toggle enable + 2 numeric fields.
- Giữ `Blocked delta: 0 / 0` là read-only note, không phải editable input.

#### Dodge

- 1 toggle enable + grace/cooldown + scope toggles.
- Khuyến nghị:
  - `applyInDraftOnly` giữ visible
  - `rejectResetDuringDraft` chỉ nên visible nếu team quyết định fix semantics backend
  - nếu chưa fix, tốt hơn nên disable/hide field này khỏi UI đầu tiên để tránh “fake control”

#### Seasonal Reset

- Chia rõ 3 zone:
  - scheduler/month settings
  - preview reset
  - execute reset danger zone
- `Preview` và `Execute` phải tách visual rất rõ.
- `Execute Reset` chỉ nên nằm trong card danger riêng.

#### Diagnostics

- V1 có thể chỉ render data đã có trong `GET /rating-settings`.
- Gợi ý hiển thị:
  - macro snapshot
  - active player count
  - active pool size
  - average rating
  - next scheduled reset
  - last reset log
  - updated by / updated at
  - replay barrier status

### Action placement recommendation

- Giữ 2 nút ở header:
  - `Khôi phục mặc định`
  - `Lưu cấu hình`
- Giữ thêm footer action row là hợp lý vì section dài.
- Không auto-save khi blur/change input.

### Canonical nav recommendation

- Sidebar label nên dùng:
  - `Solo Ban/Pick Control`
  - hoặc giữ `Solo Rating Control` nếu team muốn ngắn
- Nhưng vì phạm vi hiện đã rộng hơn rating thuần, tên chuẩn hơn là:
  - `Solo Ban/Pick Control`

## 5. Suggested DB/settings model nếu cần

### Current DB/settings model is already sufficient for v1

- `ban_pick_rating_config`
  - single-row config table
  - row chuẩn là `id = 1`
  - lưu runtime settings hiện hành
  - lưu `updated_at`, `updated_by`
- `ban_pick_rank_resets`
  - audit log cho reset
  - idempotency theo `scheduled_date`
- `player_stats`
  - `rating_anchor`
  - `rating_anchor_at`
  - `last_reset_type`
- `users`
  - `ban_pick_cooldown_until`
- `draft_histories`
  - `dodged_user_id`
  - `end_reason`
  - `win_rating_delta`
  - `loss_rating_delta`

### Recommendation for first implementation

- Không cần thêm DB table mới cho task implement admin panel đầu tiên.
- Không cần migration mới chỉ để mở UI control panel này.
- Chỉ cần tận dụng model hiện có.

### Optional future-only model

- Chỉ cân nhắc thêm `ban_pick_rating_config_audit` nếu sản phẩm thực sự cần:
  - lưu full history mọi lần đổi settings
  - diff before/after
  - rollback forensic
- Hiện tại chưa bắt buộc, vì:
  - latest settings đã có `updatedAt/updatedBy`
  - reset history đã có `ban_pick_rank_resets`

## 6. Safety rules

- Không auto-save settings.
- Save phải là hành động explicit bằng button.
- `Preview reset` không được mutate DB.
- `Execute reset` phải luôn tách khỏi save settings.
- Confirmation text cho execute reset phải giữ nguyên `RESET SOLO RANK`.
- Danger action phải nằm trong danger zone riêng, không ngang visual priority với save thường.
- Sau khi lưu settings replay-sensitive, UI phải báo rõ nếu `diagnostics.replayAnchorAdvanced = true`.
- Không bật scheduler mặc định chỉ vì UI có toggle.
- Không expose/giữ visible những toggle backend chưa có semantics thực sự rõ.
- Không chạy reset thật trong smoke test thường.
- Không đổi DB/migration trong task implement UI đầu tiên nếu không phát sinh blocker mới.

## 7. Implementation task breakdown

### Task 1. Implement frontend wiring first

Mình khuyến nghị đây là next task nên làm trước.

Scope:

- Tạo `demo/src/main/resources/static/js/admin.js`
- Implement `window.loadBanPickRatingControl()`
- Wire:
  - `GET /api/admin/ban-pick/rating-settings`
  - `PUT /api/admin/ban-pick/rating-settings`
  - `POST /api/admin/ban-pick/rating-settings/reset-defaults`
  - `GET /api/admin/ban-pick/rank-reset/preview`
  - `POST /api/admin/ban-pick/rank-reset`
- Render:
  - all form fields
  - month checkboxes
  - diagnostics cards
  - preview shell
  - danger-zone execute flow

Reason to do first:

- backend đã có phần lớn logic
- current blocker lớn nhất là frontend shell chưa usable
- task này đem lại admin value nhanh nhất với rủi ro thấp nhất

### Task 2. Add missing Ban/Pick admin CSS

Scope:

- Bổ sung các `bp-*` classes đang được `admin.html` dùng mà `admin.css` chưa có
- Hoàn thiện responsive layout cho:
  - summary grid
  - tab body
  - month checklist
  - diagnostics grid
  - danger card

### Task 3. Backend consistency cleanup for misleading settings

Scope:

- Quyết định rõ số phận của `dodgeRejectResetDuringDraft`
- Chọn 1 trong 2 hướng:
  - làm cho `false` thực sự có effect runtime
  - hoặc bỏ/ẩn field khỏi admin UI và API contract hiển thị
- Clarify scheduler source of truth:
  - DB row
  - hay property

### Task 4. Optional diagnostics expansion

Chỉ làm nếu product muốn panel này dùng cho vận hành thật, không chỉ config.

Gợi ý endpoint bổ sung:

- reset log history list
- cooldown user list
- recent dodge matches
- recent anti-trading blocked pairs

### Task 5. Docs alignment after implementation

- Update `docs/so-do-tu-duy.md`
- Update `docs/project-structure.md`
- Loại bỏ các mô tả cũ như:
  - “chưa có page admin lớn”
  - reference tới `admin.js` nếu file path thay đổi hoặc module structure khác

## 8. Verification plan

### For this audit task

- Chỉ update doc preview.
- Không cần Maven.
- Không cần test runtime.
- Không chạy reset thật.

### For the next implementation task

1. Mở `/html/admin.html#ban-pick-rating`.
2. Verify sidebar active đúng `Ban/Pick`.
3. Verify không còn 404 cho `/js/admin.js`.
4. Verify `GET /api/admin/ban-pick/rating-settings` load đầy đủ form + diagnostics.
5. Verify `Save settings` với payload hợp lệ cập nhật:
   - summary cards
   - `updatedBy`
   - `updatedAt`
   - status/toast
6. Verify payload invalid hiển thị error rõ ràng.
7. Verify `Reset defaults` repopulate đúng defaults backend.
8. Verify `Preview Soft` và `Preview Hard` render before/after/sample mà không mutate DB.
9. Không execute reset thật trong smoke test thường.
10. Verify các section khác trong `admin.html` không bị vỡ:
    - `dashboard`
    - `users`
    - `aer-data`
11. Nếu có thay đổi JS:
    - `node --check .\\demo\\src\\main\\resources\\static\\js\\admin.js`
12. Nếu có thay đổi backend runtime:
    - `.\mvnw.cmd compile`
    - `.\mvnw.cmd test`

## Recommended next task

- Implement `demo/src/main/resources/static/js/admin.js` và CSS `bp-*` còn thiếu để biến `admin.html#ban-pick-rating` thành panel usable trên các API backend đã có.
- Trong cùng task đó, hoặc ẩn tạm `rejectResetDuringDraft`, hoặc fix semantics backend của field này để tránh admin nhìn thấy một toggle nhưng không thật sự điều khiển được hành vi.
