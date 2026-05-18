**Báo cáo phân tích repository**

Báo cáo này dựa trên đọc mã nguồn và cấu hình hiện có; tôi không sửa code, không chạy build/test, và không thay đổi file nào. Tên sản phẩm trong codebase là `ATG Academy` theo `README.md`, còn module Maven hiện tại là `demo`.

**1. Khảo sát tổng quan dự án**

- Loại dự án: `fullstack web monolith` dùng một ứng dụng Spring Boot để phục vụ cả API backend lẫn frontend tĩnh.
- Kiểu triển khai: `backend API + static frontend + WebSocket realtime`; không thấy dấu hiệu microservice.
- Công nghệ chính:
  - Ngôn ngữ: `Java 17`
  - Backend: `Spring Boot 4.0.6`, `Spring Web MVC`, `Spring Data JPA`, `Spring Security`, `Spring WebSocket`
  - Auth: `Google ID token/JWT validation`, không thấy flow OAuth redirect server-side
  - Database: `MySQL`, `Hibernate/JPA`
  - Build tool: `Maven` qua `pom.xml` và `mvnw.cmd`
  - Frontend: `HTML`, `Tailwind CSS` qua CDN, `Vanilla JavaScript`, `SockJS`, `STOMP`
- File đã đọc để xác định kiến trúc: `README.md`, `demo/pom.xml`, `demo/src/main/resources/application.properties`, `controller/*`, `service/*`, `security/*`, `entity/*`, `repository/*`, `static/html/*`, `static/js/*`.
- Không tìm thấy trong codebase: `package.json`, `build.gradle`, `docker-compose.yml`, `Flyway`, `Liquibase`, profile `application-dev/application-prod`.

**2. Phân tích cấu trúc thư mục**

- `demo/src/main/java/com/example/demo/controller`: REST API, static page redirect, WebSocket message handlers.
- `demo/src/main/java/com/example/demo/service`: business logic chính; module `Guide` và `EsportsController` có phần logic nằm ngay trong controller.
- `demo/src/main/java/com/example/demo/entity`: entity JPA cho user, hero, guide, tier list, esports, ban/pick.
- `demo/src/main/java/com/example/demo/repository`: DAO/JPA repository.
- `demo/src/main/java/com/example/demo/security`: xác thực Google JWT, phân quyền, handler 401/403.
- `demo/src/main/java/com/example/demo/config`: CORS, WebSocket, scheduling.
- `demo/src/main/java/com/example/demo/component`: startup seeder/logger.
- `demo/src/main/java/com/example/demo/dto`: DTO cho `wiki`, `user`, `admin`, `banpick`, `home`.
- `demo/src/main/resources/static/html`: UI/views tĩnh; chứa `index`, `wiki`, `tier-list`, `giao-an`, `create-guide`, `guide-detail`, `esports`, `ban-pick`, `account`, `admin`.
- `demo/src/main/resources/static/js`: client logic; `auth.js`, `tier-list-*`, `tactics-guides.js`, `admin.js` (wire `admin.html#ban-pick-rating` cho Solo Ban/Pick Rating Control), `admin-heroes.js`.
- `demo/src/main/resources/static/css`: CSS tĩnh.
- `style.css` la shared/base CSS; cac page/module load them `tier-list.css`, `ban-pick.css`, `account.css`, `wiki.css`, `esports.css`, `guides.css`, `admin.css` sau `style.css`.
- `demo/src/main/resources/static/images`: static assets.
- `demo/src/main/resources/static/error`: trang lỗi `404`.
- `demo/src/main/resources/db` và `demo/sql`: schema/seed/migration thủ công.
- `demo/src/test`: test service/controller/util.

**Vai trò theo lớp**
- UI/views/templates: `demo/src/main/resources/static/html`
- API/controllers: `demo/src/main/java/com/example/demo/controller`
- Business logic/services: `demo/src/main/java/com/example/demo/service`
- Database models/entities: `demo/src/main/java/com/example/demo/entity`
- Repositories/DAO: `demo/src/main/java/com/example/demo/repository`
- Config: `demo/src/main/java/com/example/demo/config`, `application.properties`
- Authentication/authorization: `demo/src/main/java/com/example/demo/security`, `static/js/auth.js`
- Static assets: `demo/src/main/resources/static/images`, `css`, `js`
- Tests: `demo/src/test/java`

**3. Sơ đồ tư duy chức năng dự án**

```text
ATG Academy (module Maven: demo)
├─ Tổng quan
│  ├─ Mục tiêu: cổng thông tin Liên Quân/Arena of Valor cho meta, tier list, guide, esports, ban/pick
│  ├─ Loại dự án: fullstack monolith; Spring Boot phục vụ API + static frontend
│  ├─ Người dùng: khách, user đăng nhập Google, admin; role STAFF có model nhưng chưa thấy quyền riêng
│  └─ Luồng tổng quát: HTML/JS -> /api/* -> controller -> service/repository -> MySQL; riêng ban/pick online thêm /ws
├─ Chức năng người dùng
│  ├─ Đăng nhập Google; file: static/js/auth.js, security/GoogleJwt*
│  ├─ Quản lý tài khoản: hồ sơ + Player Card preview dùng chung với Solo Ban/Pick + panel `Nội dung của bạn` đếm/list content chính chủ; file: UserProfileController, UserProfileService, static/html/account.html, static/js/components/player-card.js
│  ├─ Trang chủ feed; file: HomeFeedController, HomeFeedService, static/html/index.html
│  ├─ Wiki tướng + hero pool data cho Ban/Pick; dropdown `Wiki` trỏ đến `wiki.html`, deep-link `?tab=spells`, `?tab=enchantments`; dropdown `Esports` tách riêng để chứa `esports.html` và `Esports Data`; file: WikiController, SpellController, EnchantmentController, static/html/wiki.html, header.html, header-loader.js
│  ├─ Tier List chính thức: `tier-list.html` là trang gốc của module Tier List, hiển thị Meta chính thức ở trên và tối đa 6 Community Tier List nổi bật ở dưới; file: TierListController, TierListCommunityService, static/html/tier-list.html
│  ├─ Tier List cộng đồng: giữ 2 static page `tier-list-all.html`, `tier-list-mine.html`; `tier-list.html` tiếp tục là trang gốc hiển thị Meta + community highlight, còn route cũ `/tier-list/recommended` chỉ redirect an toàn về trang gốc; detail dùng một rating panel chung, user lưu community rating còn Admin lưu admin rating; file: TierListController, AdminTierListController, static/html/tier-list-community-shell.html, static/js/tier-list-community-page.js, static/js/tier-list-app.js, tier-list-detail.js
│  ├─ Guides: list/filter/detail/tạo; file: GuideController, static/html/giao-an.html, create-guide.html
│  ├─ Esports public: BXH đội và match feed; file: EsportsController, static/html/esports.html
│  ├─ Ban/Pick mode navigation: dropdown navbar vào thẳng `ban-pick-free.html`, `ban-pick-standard.html`, `ban-pick-solo.html`; các legacy entry route `/ban-pick`, `/ban-pick.html`, `/html/ban-pick.html` chỉ còn fallback redirect cho link cũ
│  └─ Ban/Pick 1v1 online: room/join/realtime/history/profile/leaderboard; file: BanPickRoomController, BanPickHistoryController, BanPickRoomWebSocketController
├─ Chức năng quản trị
│  ├─ Dashboard admin; file: static/html/admin.html, admin-heroes.html
│  ├─ Quản lý người dùng; file: AdminUserController, AdminUserService
│  ├─ Quản lý wiki hero + điểm Ban/Pick; file: AdminWikiHeroController, AdminWikiHeroService
│  ├─ Quản lý hero attributes; file: AdminWikiAttributeController, HeroAttributeController
│  ├─ Quản lý tier list chính thức và admin rating; file: TierListController, AdminTierListController
│  ├─ AER Data admin: teams/matches/import trên `admin.html#aer-data`; match history cũ vẫn xem/sửa/xóa từng trận tại đây, còn reset dữ liệu đã chuyển sang `admin-esports-data.html`; file: EsportsAdminController, EsportsAdminService, static/html/admin.html
│  ├─ Esports Data admin: quan ly `upload file -> preview import -> ap dung vao DB`, recalculate AER/Elo truc tiep tu `esports_matches` + `esports_game_drafts` theo `affectedMatchIds` ngay sau khi confirm import, va `match -> game draft records -> validate` tren `admin-esports-data.html`; file: EsportsAdminController, EsportsAdminService, EsportsDraftService, static/html/admin-esports-data.html, static/js/admin-esports-data.js
│  └─ Không tìm thấy trong codebase: guide moderation/admin guide hoàn chỉnh
├─ Chức năng nghiệp vụ chính
│  ├─ Wiki Hero
│  │  ├─ Mục đích: catalog hero, role/class/attribute, skill, matchup, guide liên quan
│  │  ├─ File: WikiService, HeroRepository, HeroSkillRepository, HeroMatchupRepository
│  │  ├─ API: GET /api/wiki/heroes, GET /api/wiki/heroes/{slug}; hero summary hiện trả thêm `banPickScore` để frontend Ban/Pick dùng cùng nguồn dữ liệu backend
│  │  ├─ DB: heroes (`ban_pick_score`), hero_roles, hero_classes, hero_attributes, hero_skills, hero_matchups
│  │  └─ Luồng: slug -> hero + relations -> skills + matchups + related guides -> HeroDetailDto; riêng hero catalog -> HeroSummaryDto kèm `banPickScore`
│  ├─ Tier List
│  │  ├─ Mục đích: meta chính thức + community tier list
│  │  ├─ File: TierListController, TierListCommunityService, HeroContentDataService
│  │  ├─ API: /api/tier-lists/*; `/community` giữ vai trò highlight tối đa 6 item, `/community/all` trả toàn bộ community tier list, `/me` trả community tier list của user hiện tại; official/community payload có `creatorName` + `creator_name`
│  │  ├─ DB: tier_lists, tier_list_ratings, tier_list_comments, tier_list_admin_ratings
│  │  └─ Luồng: GET /official -> `tier-list.html` render title `Tier List Meta` và ngay bên dưới fetch `GET /community` để hiển thị tối đa 6 Community Tier List nổi bật (bỏ official, tránh trùng, có thể ít hơn 6 nếu DB không đủ); dropdown `Tier List` trên header giữ `tier-list.html` (Meta), `tier-list-all.html` -> GET /community/all, `tier-list-mine.html` -> GET /me; route cũ `/tier-list/recommended` và các URL HTML cũ của page đề xuất đều redirect về `tier-list.html`; page `Của tôi` yêu cầu đăng nhập và hiển thị message nếu là guest
│  ├─ Guide
│  │  ├─ Mục đích: tạo và đọc giáo án chiến thuật
│  │  ├─ File: GuideController, GuideRepository, static/html/create-guide.html
│  │  ├─ API: GET/POST /api/guides, GET /api/guides/{id}
│  │  ├─ DB: guides; `contentData` là nguồn runtime chính cho create-guide hiện tại. `spells`/`phu_hieu` không còn là nguồn runtime của Wiki/Admin sau khi chuyển sang JSON
│  │  └─ Luồng: frontend gửi contentData JSON -> backend lưu guide + author + hero + metadata
│  ├─ Home Feed
│  │  ├─ Mục đích: giữ feed hỗn hợp cho nội dung trang chủ và cấp riêng 3 slot Tier List cộng đồng nổi bật cho section homepage
│  │  ├─ File: HomeFeedController, HomeFeedService, TierListRatingRepository, TierListAdminRatingRepository, static/html/index.html
│  │  ├─ API: GET /api/home/feed, GET /api/home/community-tier-highlights
│  │  └─ Luồng: /api/home/feed vẫn lấy 6 tier list + 6 guides -> merge -> sort theo createdAt; /api/home/community-tier-highlights chọn tối đa 3 tier list cộng đồng theo newest -> top average user rating 30 ngày -> top admin rating 30 ngày, bỏ official và tránh trùng
│  ├─ Esports Ranking
│  │  ├─ Mục đích: xếp hạng đội theo Elo, quản trị trận đấu
│  │  ├─ File: EsportsController, EsportsAdminService, EloCalculationService, EsportsDataSeeder
│  │  ├─ API: /api/esports/*, /api/admin/esports/*
│  │  ├─ DB: esports_teams, esports_matches
│  │  └─ Luồng: seed nếu DB trống -> thêm/sửa/xóa match -> tính lại Elo toàn bộ
│  └─ Ban/Pick Online
│     ├─ Mục đích: solo draft online BO1/3/5/7 có realtime + đánh giá đội hình theo điểm hero đã pick
│     ├─ File: BanPickRoomService, BanPickHistoryService, BanPickRoomBroadcaster, BanPickPresenceEventListener
│     ├─ API: /api/ban-pick/rooms/*, /api/ban-pick/history*, /api/ban-pick/profile, /api/ban-pick/leaderboard
│     ├─ WebSocket: /ws, /app/ban-pick/{roomCode}/*, /topic/ban-pick/{roomCode}
│     ├─ DB: ban_pick_rooms, ban_pick_room_participants, ban_pick_actions, draft_histories, player_stats
│     └─ Luồng: create room -> join -> roll side -> ready -> lobby Solo render sidebar/tab nội bộ với `Tìm trận` mặc định, `Thông tin cá nhân`, và `Bảng xếp hạng`; tab `Tìm trận` giữ create/join room + chọn BO1/BO3/BO5/BO7 + cooldown/error room, tab `Thông tin cá nhân` render Player Card nhỏ riêng và Stats Panel 50 trận riêng khi user đã đăng nhập, tab `Bảng xếp hạng` render leaderboard trực tiếp, guest ở tab profile chỉ thấy message yêu cầu login -> 15 phase draft -> frontend dùng hero pool `/api/wiki/heroes` để cộng `banPickScore` cho tướng đã pick -> lineup adjustment -> cả 2 đội confirm lineup cuối -> mới hiển thị tổng điểm/tỷ lệ thắng dự đoán -> finish -> hệ thống tự xác định winner theo tổng điểm lineup cuối, nếu hòa thì không chọn bừa winner và không cập nhật W/L/rating -> trước khi save history backend chốt Macro Economy snapshot của ngày hiện tại (window 30 ngày tính tới mốc 00:00, active pool là top 50% người chơi có nhiều completed matches nhất, so average rating với mốc 1500, mỗi lệch 10 điểm điều chỉnh 2% win delta, floor +20) -> sau đó áp thêm ELO gap modifier theo rating trước trận của winner/loser (10 điểm lệch = 2%, cap 50%; cửa dưới thắng được thưởng thêm và cửa trên thua bị phạt nặng hơn, cửa trên thắng bị giảm điểm nhận và cửa dưới thua bị phạt nhẹ hơn) -> trước khi chốt snapshot rating backend kiểm tra anti-win-trading theo unordered pair `(minUserId, maxUserId)`: trong cửa sổ 48 giờ, 2 lần gặp đầu vẫn tính điểm, từ lần gặp thứ 3 trở đi override snapshot thành `0/0`, history vẫn lưu nhưng ELO không đổi; quá 48 giờ thì reset pair window -> lưu `win_rating_delta/loss_rating_delta` cuối cùng vào `draft_histories` -> `BanPickHistoryService` rebuild `player_stats` theo 50 draft gần nhất của từng player với base rating khởi tạo 1000, replay winner/loser theo delta snapshot đã lưu, tie không đổi W/L/rating và match bị anti-win-trading cũng không cộng trừ rating nhờ snapshot `0/0` -> backend đồng thời compute percentile rank `S/A/B/C/D` từ `player_stats.rating`, Player Card/leaderboard chỉ render `rankCode`/`rankLabel` trả về -> retention cleanup chỉ xóa history khi row đó đã out top 50 của cả 2 participant
├─ Authentication & Authorization
│  ├─ Backend: stateless bearer auth, validate issuer/audience/email_verified của Google token
│  ├─ Public GET: wiki heroes, spells, enchantments, guides, tier-lists, esports, một phần ban-pick result read-only legacy route/leaderboard
│  ├─ Auth required: create guide, create/rate/comment tier list, profile, ban-pick room APIs
│  ├─ Admin required: /api/admin/**
│  └─ Frontend guest role "Custom" chỉ là quy ước UI trong auth.js; backend role thực tế là Admin/Staff/User
├─ Cấu hình & môi trường
│  ├─ application.properties: MySQL localhost, ddl-auto=update, show-sql=true, Google client id, admin emails, CORS
│  ├─ test application.properties: tắt scheduler
│  ├─ Không thấy profile dev/prod riêng
│  └─ SQL seed/migration: demo/sql, resources/db
├─ Luồng chạy ứng dụng
│  ├─ Entry point: DemoApplication
│  ├─ Start: mvnw.cmd spring-boot:run
│  ├─ Startup hook: EsportsDataSeeder seed teams/matches nếu DB esports trống
│  ├─ Startup hook: HeroDataStartupLogger cảnh báo nếu chưa seed hero
│  └─ Static routes: StaticPageRedirectController redirect /, /guides, /tier-list, /tier-list/all, /tier-list/mine; route cũ `/tier-list/recommended`, `/tier-list-recommended.html`, `/html/tier-list-recommended.html` redirect an toàn về `tier-list.html`; `/tactics-guides.html` và `/html/tactics-guides.html` redirect an toàn về `giao-an.html`; `/ban-pick`, `/ban-pick.html`, `/html/ban-pick.html` chuyển an toàn về mode phù hợp/default Free Mode; `/ban-pick/profile`, `/ban-pick-profile.html`, và `/html/ban-pick-profile.html` đều là legacy redirect về `/html/ban-pick-solo.html`; `/ban-pick/leaderboard` vẫn sang static/html; còn `/ban-pick/result/{id}`, `/ban-pick-result.html`, `/html/ban-pick-result.html` chỉ giữ để tương thích link cũ và đều redirect an toàn về `/html/ban-pick-solo.html`, không còn hiển thị result page riêng; admin shell `/html/admin.html` giữ module `AER Data` tại hash `#aer-data` và vẫn alias `#teams`/`#esports`, còn `/html/admin-esports-data.html` phục vụ module `Esports Data` nhập draft chi tiết; placeholder `content.html` đã bị xóa khỏi codebase và không giữ redirect legacy
└─ Điểm cần lưu ý
   ├─ GuideController lọc guide bằng findAll().stream(); rủi ro scale
   ├─ Có 2 API admin attribute với semantics xóa khác nhau; cần xác minh thêm
   ├─ guides.contentData đang là nguồn dữ liệu chính; các bảng huong_dan_* có dấu hiệu chưa dùng runtime rõ ràng
   ├─ README nói có /api/meta nhưng code hiện tại không tìm thấy controller/service tương ứng
   ├─ wiki.html runtime chỉ còn 3 tab `heroes`, `spells`, `enchantments`; placeholder `items/arcana` đã bị loại khỏi runtime cùng cleanup legacy Arcana/Item
   ├─ application.properties commit sẵn DB password/client id; nên externalize trước khi phát triển tiếp
   ├─ BanPickRoomStatus có CANCELLED nhưng chưa thấy flow sử dụng
   └─ Chưa thấy test bao phủ Guides, Tier Lists, Esports, Security/WebSocket end-to-end
```

**4. Bảng tổng hợp chức năng**

| Nhóm chức năng | Chức năng cụ thể | Mô tả | File/thư mục liên quan | API/route | Entity/database | Ghi chú |
|---|---|---|---|---|---|---|
| Xác thực | Đăng nhập Google | Frontend nhận Google credential, lưu localStorage, tự gắn Bearer token vào `/api/**` | `static/js/auth.js`, `security/*` | toàn bộ `/api/**` private | `users` | Guest UI dùng role `Custom` |
| Tài khoản | Xem/sửa hồ sơ cá nhân + nội dung của bạn | Đổi `displayName`, `level`; sidebar dashboard hiển thị Player Card preview dùng chung component với Solo Ban/Pick, có form chọn preset `badge` + sửa `player title`, kèm số guide đã đăng và community tier list đã đăng của user hiện tại | `UserProfileController`, `UserProfileService`, `GuideRepository`, `TierListRepository`, `static/html/account.html`, `static/js/components/player-card.js`, `static/css/player-card.css` | `/api/users/me/profile`, `/api/users/me/content-summary`, `/api/ban-pick/profile` | `users`, `guides`, `tier_lists`, `player_stats`, `draft_histories` | Content summary dùng security principal, chỉ tính guide published và tier list không official của chính user; preview Player Card đọc avatar/display name từ `users`, ELO/rank từ snapshot solo 50 trận; Player Card config persist ở `users.player_badge_code`, `users.player_badge_name`, `users.player_badge_icon_url`, `users.player_title`; frontend account dashboard render count Community Tier List theo dạng `n/5` |
| Trang chủ | Community feed | Section homepage dùng 3 Tier List cộng đồng nổi bật; feed hỗn hợp cũ vẫn giữ cho backend | `HomeFeedController`, `HomeFeedService`, `TierListRatingRepository`, `TierListAdminRatingRepository`, `static/html/index.html` | `/api/home/feed`, `/api/home/community-tier-highlights` | `tier_lists`, `guides`, `tier_list_ratings`, `tier_list_admin_ratings` | `/api/home/community-tier-highlights` bỏ official, tránh trùng và có thể trả ít hơn 3 item |
| Wiki | Danh mục tướng + gameplay data | Trả catalog tướng, class/role/attribute, skill, matchup, guide liên quan; hero summary kèm `banPickScore`; cùng page `wiki.html` hiện runtime chỉ còn tab `Tướng`, `Bổ trợ`, `Phù hiệu` đọc từ API/JSON thật | `WikiController`, `SpellController`, `EnchantmentController`, `SpellService`, `EnchantmentService`, `WikiJsonStorageService`, `static/html/wiki.html`, `static/html/header.html`, `static/js/header-loader.js`, `static/css/wiki.css` | `/api/wiki/heroes*`, `/api/spells*`, `/api/enchantments*`, `wiki.html?tab=spells`, `wiki.html?tab=enchantments` | `heroes`, `hero_skills`, `hero_matchups`, `static/data/spells.json`, `static/data/enchantments.json` | Dropdown `Wiki` deep-link vào `wiki.html`, `wiki.html?tab=spells`, `wiki.html?tab=enchantments`; runtime không còn tab/frontend reference `items/arcana`; legacy DB `phu_hieu` có thể còn tồn tại trong SQL thủ công nhưng không còn là runtime dependency |
| Tier list | Meta chính thức | Official meta duoc generate tu `heroes.ban_pick_score`; public doc tren `tier-list.html`; Admin save/regenerate de persist vao `tier_lists.contentData`; trang goc van hien board official o tren va 6 community noi bat o duoi | `TierListController`, `AdminTierListController`, `TierListCommunityService`, `HeroContentDataService`, `static/html/tier-list.html`, `static/js/tier-list-app.js` | `/api/tier-lists/official`, `POST /api/admin/tier-lists/official/regenerate-from-hero-scores` | `tier_lists`, `heroes` | Rule tier: `>9 S`, `>7.5 A`, `>5 B`, `>2.5 C`, else `D`; role columns giu DSL/JGL/MID/ADL/SUP; section community tren trang goc van dung `GET /api/tier-lists/community` |
| Tier list | Community tier list | User tạo tier list; danh sách cộng đồng giữ 2 page `tier-list-all.html`, `tier-list-mine.html`; dropdown `Tier List` trên header giữ `Tier List Meta`, `Tất cả Tier List`, `Tier List của bạn`; page `tier-list-recommended.html` đã bị xóa, còn route cũ chỉ redirect về `tier-list.html`; xem detail, rate, comment; Admin dùng cùng rating panel trên detail page để lưu admin rating | `TierListController`, `AdminTierListController`, `TierListCommunityService`, `TierListRatingRepository`, `TierListAdminRatingRepository`, `static/html/tier-list-community-shell.html`, `static/html/tier-list-all.html`, `static/html/tier-list-mine.html`, `static/js/tier-list-community-page.js`, `static/js/tier-list-app.js`, `static/html/tier-list-detail.html`, `tier-list-detail.js` | `/api/tier-lists/*`, `/api/admin/tier-lists/{id}/admin-rating` | `tier_lists`, `tier_list_ratings`, `tier_list_comments`, `tier_list_admin_ratings` | Guest chi co 1 draft tam trong `sessionStorage` key `atg_guest_tier_list_draft`; logged-in user duoc tao/luu toi da 5 Community Tier List non-official; update tier list da ton tai khong tinh them quota; official/admin tier list khong bi ap limit nay |
| Guide | Danh sách/chi tiết guide | Search/filter theo `status`, `heroId`, `lane`, `category`, `search`, `sort` | `GuideController`, `GuideRepository`, `static/js/tactics-guides.js` | `GET /api/guides*` | `guides` | Filter đang chạy in-memory |
| Guide | Tạo guide | Tạo giáo án từ form frontend; lưu metadata + `contentData` JSON | `GuideController`, `static/html/create-guide.html` | `POST /api/guides` | `guides`, `users`, `heroes` | Không thấy update/delete/moderation |
| Esports | BXH public | Trả danh sách đội xếp theo Elo và recent matches | `EsportsController`, `static/html/esports.html` | `/api/esports/teams`, `/matches/recent` | `esports_teams`, `esports_matches` | `esports.html` hiện dùng team list; cột leaderboard `Series W/L` đọc từ `matchWins/matchLosses`, không phải `gameWins/gameLosses`; recent feed cần xác minh thêm |
| Esports | Esports Data | Dashboard analytics public cho draft esports theo model `1 series = 1 esports_matches`, `1 game = 1 esports_game_drafts` | `EsportsController`, `EsportsDataService`, `EsportsGameDraftRepository`, `static/html/esports-data.html`, `static/js/esports-data.js`, `static/css/esports.css` | `/esports/data`, `/api/esports/data/*` | `heroes`, `esports_matches`, `esports_game_drafts` | Public page đọc từ `esports_game_drafts`; `Total Games` = số row game draft, `Total Series` = distinct `match_id`; KPI/top bans/top picks/side WR/hero stats không còn aggregate runtime từ các bảng draft cũ |
| Ban/Pick | Room draft online | Create room, join, ready, roll side, start, confirm pick/ban, reorder lineup, next game, reset; Free/Standard giữ panel compact hiển thị tổng điểm đội hình và tỷ lệ thắng dự đoán trong lúc draft, còn Solo Online chỉ hiện sau khi room finish và cả 2 đội confirm lineup cuối; màn tổng kết không còn nút chia sẻ draft | `BanPickRoomController`, `BanPickRoomService`, `BanPickRoomWebSocketController`, `ban-pick-free.html`, `ban-pick-standard.html`, `ban-pick-solo.html`, `ban-pick-shell.html`, `ban-pick.js` | `/api/ban-pick/rooms/*`, `/ws`, `/api/wiki/heroes` | `ban_pick_rooms`, `ban_pick_actions`, `ban_pick_room_participants`, `heroes` | Navbar dropdown là luồng chuyển mode chính; chỉ còn legacy entry route `/ban-pick*` redirect/fallback; không cộng điểm tướng bị ban |
| Ban/Pick | Lịch sử/BXH/solo stats | Lưu draft finished, auto-resolve winner theo Ban/Pick Score, leaderboard public, panel thống kê cá nhân gắn thẳng vào Solo page qua sidebar/tab nội bộ `Tìm trận` / `Thông tin cá nhân` / `Bảng xếp hạng`, và seasonal reset toàn server cho rating/rank Solo Ban/Pick | `BanPickHistoryController`, `BanPickHistoryService`, `BanPickMacroEconomyService`, `BanPickSeasonResetService`, `ban-pick-solo.html`, `ban-pick-shell.html`, `ban-pick.js` | `/api/ban-pick/history*`, `/leaderboard`, `/profile`, `/api/admin/ban-pick/rank-reset*`, `/api/admin/ban-pick/rating-settings*` | `draft_histories`, `player_stats`, `ban_pick_rank_resets`, `ban_pick_rating_config` | Stats current user vẫn chỉ lấy 50 draft gần nhất; `player_stats` được rebuild theo 50 draft gần nhất của từng player sau mỗi Solo draft finished; tab `Tìm trận` giữ create/join + BO + cooldown, tab `Thông tin cá nhân` giữ Player Card tách riêng Stats Panel, tab `Bảng xếp hạng` render leaderboard trực tiếp bằng API hiện có; rating khởi tạo `1000`, floor `0`; winner trước hết dùng macro-adjusted `currentWinDelta` từ snapshot ngày 00:00 dựa trên average rating của top 50% active players trong 30 ngày gần nhất, mỗi lệch `10` điểm so với mốc `1500` điều chỉnh `2%`, floor `+20`, rồi áp ELO gap modifier theo rating trước trận của 2 người chơi với rule `10 điểm = 2%`, cap `50%`; cửa dưới thắng được tăng win delta và cửa trên thua bị phạt nặng hơn, cửa trên thắng bị giảm win delta và cửa dưới thua bị phạt nhẹ hơn; sau đó anti-win-trading kiểm tra unordered pair trong cửa sổ `48 giờ`, cho phép 2 lần gặp đầu giữ nguyên delta và từ lần gặp thứ 3 override snapshot về `0/0`; `draft_histories` lưu snapshot `win_rating_delta/loss_rating_delta` cuối cùng để replay chính xác; seasonal reset dùng `rating_anchor` + `rating_anchor_at` để replay rating chỉ cộng các trận sau reset, còn history panel 50 trận gần nhất vẫn giữ nguyên; lịch reset cố định `01/02 SOFT`, `01/04 SOFT`, `01/06 HARD`, `01/08 SOFT`, `01/10 SOFT`, `01/12 HARD`; score hòa thì không cập nhật W/L/rating |
| Admin | Quản lý user | List/search/filter user, chỉnh `name/avatar/role/status/note` | `AdminUserController`, `AdminUserService`, `static/html/admin.html` | `/api/admin/users*` | `users` | Admin không thể tự hạ role/khoá chính mình |
| Admin | Quản lý hero wiki | Sửa basic info, class, role, attribute, difficulty, `banPickScore`; admin panel validate min/max/2 chữ số thập phân trước khi lưu DB | `AdminWikiHeroController`, `AdminWikiHeroService`, `static/html/admin-heroes.html`, `static/js/admin-heroes.js` | `/api/admin/wiki/heroes*` | `heroes`, join tables hero_* | Có gợi ý role theo class; user thường không có quyền sửa |
| Admin | Quản lý hero attributes | Có 2 API admin attribute khác nhau | `AdminWikiAttributeController`, `HeroAttributeController`, `AdminWikiHeroService`, `HeroAttributeService` | `/api/admin/wiki/attributes*`, `/api/admin/attributes*` | `hero_attributes`, `hero_attribute_mapping` | Hành vi delete không nhất quán |
| Admin | Solo Ban/Pick Rating Control | Admin panel `admin.html#ban-pick-rating` cho Base Rating, Macro Economy, ELO Gap, Anti-win-trading, Dodge Penalty, Seasonal Reset preview/execute, diagnostics; wiring bằng `static/js/admin.js` và style trong `static/css/admin.css` | `AdminBanPickController`, `BanPickRatingSettingsService`, `BanPickSeasonResetService`, `BanPickSeasonResetScheduler`, `static/html/admin.html`, `static/js/admin.js`, `static/css/admin.css` | `GET/PUT /api/admin/ban-pick/rating-settings`, `POST /api/admin/ban-pick/rating-settings/reset-defaults`, `GET /api/admin/ban-pick/rank-reset/preview?type=SOFT|HARD`, `POST /api/admin/ban-pick/rank-reset` | `ban_pick_rating_config`, `player_stats`, `ban_pick_rank_resets` | Reset không xóa `draft_histories`; `SOFT = round((currentRating + 1000) / 2)`, `HARD = 1000`; scheduler mặc định disabled; `dodgeRejectResetDuringDraft` hiện để read-only trong UI vì backend chưa support toggle độc lập rõ ràng; log `ban_pick_rank_resets` chặn chạy trùng cùng `scheduled_date` |
| Admin | AER Data | CRUD team/match, bulk import, recalculation Elo cho workflow ranking/AER tổng quát; không còn nút reset dữ liệu legacy trên trang này | `EsportsAdminController`, `EsportsAdminService`, `static/html/admin.html`, `static/css/admin.css` | `/api/admin/esports/teams*`, `/api/admin/esports/matches*`, `/api/admin/esports/teams/matches/bulk-import` | `esports_teams`, `esports_matches` | Sidebar admin có entry `AER Data`; canonical route là `admin.html#aer-data`, vẫn alias `#teams` và `#esports` để không gãy flow cũ; reset full esports data phải dùng `admin-esports-data.html` |
| Admin | Esports Data | Quản lý từng ván đấu theo `game draft record`, preview import Excel/CSV trước khi commit DB, group các dòng game thành series cha trước khi ghi DB, export CSV cho thống kê public `/esports/data`, và cập nhật ranking AER/Elo trực tiếp từ DB ngay sau khi confirm import | `EsportsAdminController`, `EsportsAdminService`, `EsportsDraftService`, `EsportsTournamentService`, `static/html/admin-esports-data.html`, `static/js/admin-esports-data.js`, `static/css/admin.css` | `/api/admin/esports/franchises*`, `/api/admin/esports/tournaments*`, `/api/admin/esports/tournaments/{id}/teams*`, `/api/admin/esports/matches*`, `/api/admin/esports/matches/{matchId}/game-drafts`, `/api/admin/esports/game-drafts/{id}`, `/api/admin/esports/game-drafts/export`, `/api/admin/esports/game-drafts/import/preview`, `/api/admin/esports/game-drafts/import/confirm` | `esports_franchises`, `esports_tournaments`, `esports_tournament_teams`, `esports_matches`, `esports_game_drafts` | Sidebar admin có entry `Esports Data`; import file hiểu `Match = game_number`, group series theo `Date + Tournament + Stage + team pair`, sau đó resolve parent theo `date-only + tournament_id + stage + unordered team pair + score`; nếu có nhiều parent exact thì ưu tiên parent đã có draft rồi mới tới `id` nhỏ hơn; `esports_tournaments.aer_tier` là source of truth, `esports_matches.tier` chỉ là fallback/legacy snapshot, tier hợp lệ gồm `0/1/2`; stage canonical lưu trong DB chỉ được là `ck/playoff/bang/vongloai`, admin create/update match sẽ normalize alias như `final/group/qualifier/play-off` về canonical, và unknown stage bị reject để tránh Elo fallback sai; update `aer_tier` sẽ sync snapshot match link và recalculate ranking, UI chính chỉ hiện summary import/ranking, không có AER JSON trung gian, và không có bước preview/download file riêng |

**5. Bảng API**

| Method | Endpoint | Controller/Handler | Service | Request input suy luận được | Response output suy luận được | Mục đích |
|---|---|---|---|---|---|---|
| GET | `/api/home/feed` | `HomeFeedController` | `HomeFeedService` | none | `HomeFeedItemResponse[]` | Feed trang chủ hỗn hợp tier list + guide |
| GET | `/api/home/community-tier-highlights` | `HomeFeedController` | `HomeFeedService` | none | `HomeFeedItemResponse[]` | Tối đa 3 Tier List cộng đồng nổi bật cho homepage |
| GET | `/api/wiki/heroes` | `WikiController` | `WikiService` | none | `HeroSummaryDto[]` kèm `banPickScore` | Catalog tướng; là nguồn dữ liệu score cho Ban/Pick frontend |
| GET | `/api/wiki/heroes/{slug}` | `WikiController` | `WikiService` | `slug` | `HeroDetailDto` | Chi tiết tướng |
| GET | `/api/guides` | `GuideController` | controller trực tiếp | `status, heroId, category, lane, search, sort` | list guide summary/detail map | List/filter guide |
| GET | `/api/guides/{id}` | `GuideController` | controller trực tiếp | `id` | guide map | Chi tiết guide; tăng view |
| POST | `/api/guides` | `GuideController` | controller trực tiếp | `title, heroId/heroName, lane, category, excerpt, coverImageUrl, status, contentData` | created guide map | Tạo guide |
| GET | `/api/tier-lists/official` | `TierListController` | `TierListCommunityService` | none | tier list map; neu chua co official row thi tra preview generate tu hero score | Tier list chính thức |
| GET | `/api/tier-lists/community` | `TierListController` | `TierListCommunityService` | none | `List<Map>` | Tối đa 6 Tier List cộng đồng nổi bật: top điểm TB 30 ngày, nhiều rating 30 ngày, nhiều 5 sao 30 ngày, top admin rating 30 ngày, 2 newest; bỏ official và tránh trùng |
| GET | `/api/tier-lists/community/all` | `TierListController` | `TierListCommunityService` | none | `List<Map>` | Trả toàn bộ Tier List cộng đồng theo thứ tự mới nhất, bỏ official, giữ cùng response shape của card hiện tại |
| GET | `/api/tier-lists/me` | `TierListController` | `TierListCommunityService` | bearer auth từ user hiện tại | `List<Map>` | Trả Tier List cộng đồng do chính user đang đăng nhập tạo, bỏ official; guest/unauthorized nhận 401 |
| GET | `/api/tier-lists/{id}` | `TierListController` | `TierListCommunityService` | `id` | tier list map | Detail tier list |
| GET, POST | `/api/tier-lists/{id}/comments` | `TierListController` | `TierListCommunityService` | POST: `content/comment` | GET: list comment; POST: comment map | Đọc/gửi bình luận |
| GET | `/api/tier-lists/{id}/ratings`, `/api/tier-lists/{id}/ratings/summary` | `TierListController` | `TierListCommunityService` | `id` | summary map | Lấy thống kê rating |
| POST | `/api/tier-lists/{id}/ratings`, `/api/tier-lists/{id}/rate` | `TierListController` | `TierListCommunityService` | `ratingValue/stars` | summary map | User chấm điểm |
| POST | `/api/tier-lists` | `TierListController` | `TierListCommunityService` + `HeroContentDataService` | `title, description, isOfficial, contentData` | tier list map | Tạo tier list community; guest flow frontend khong goi endpoint nay; backend reject create Community Tier List thu 6 cua cung user voi message `Bạn chỉ có thể lưu tối đa 5 tier list.`; official save UI khong con dung import textarea |
| PUT | `/api/tier-lists/{id}` | `TierListController` | `TierListCommunityService` + `HeroContentDataService` | `title, description, contentData` | tier list map | Sửa tier list |
| PUT | `/api/tier-lists/{id}/admin-rate`, `/api/admin/tier-lists/{id}/admin-rating` | `TierListController`, `AdminTierListController` | `TierListCommunityService` | `ratingValue/stars/adminRating, note(optional)` | admin rating summary | Admin endorsement; detail page Admin star click gửi vào endpoint này |
| POST | `/api/admin/tier-lists/official/regenerate-from-hero-scores` | `AdminTierListController` | `TierListCommunityService` + `HeroContentDataService` | none | official tier list map | Admin generate/save official meta tu `heroes.ban_pick_score` vao `tier_lists.contentData` |
| GET, PUT | `/api/users/me/profile` | `UserProfileController` | `UserProfileService` | PUT: `displayName, level` | `UserProfileResponse` | Hồ sơ user |
| GET | `/api/users/me/content-summary` | `UserProfileController` | `UserProfileService` | none | `UserContentSummaryResponse` | Account dashboard panel `Nội dung của bạn`; đếm/list guide published và tier list cộng đồng thuộc user đang đăng nhập; frontend tier list/account page dung `tierListCount` hien `n/5` |
| GET | `/api/esports/teams` | `EsportsController` | repo trực tiếp | none | `EsportsTeam[]` | BXH public |
| GET | `/api/esports/matches/recent` | `EsportsController` | repo tr?c ti?p | `limit` | `RecentMatchDto[]` | Feed tr?n g?n d?y |
| GET | `/api/esports/franchises`, `/api/esports/franchises/{code}` | `EsportsController` | `EsportsFranchiseService` | detail: `code` | `EsportsFranchiseResponse[]` / detail | Public franchise catalog; seed mac dinh RPL, AOG, GCS, APL, AIC |
| GET | `/api/esports/tournaments`, `/api/esports/tournaments/{id}`, `/api/esports/tournaments/{id}/teams` | `EsportsController` | `EsportsTournamentService` | list: `franchiseId, franchiseCode`; detail/team: `id` | tournament list/detail/team payload co `aerTier` | Public tournament catalog va roster team tham gia; `esports_tournaments.aer_tier` la nguon tier chuan cho ranking AER doc truc tiep tu DB sau import |
| GET | `/api/esports/data/tournaments` | `EsportsController` | `EsportsDataService` | none | `EsportsTournamentOptionResponse[]` | Danh sách tournament scope có dữ liệu draft cho filter public page |
| GET | `/api/esports/data/top-banned-heroes` | `EsportsController` | `EsportsDataService` | `tournamentId, tournamentName, limit` | `EsportsHeroBanStatResponse[]` | Top hero bị ban nhiều nhất, aggregate từ 10 cột ban của `esports_game_drafts`; vẫn fallback tier legacy nếu `tournament_id` chưa có |
| GET | `/api/esports/data/top-blue-banned-heroes` | `EsportsController` | `EsportsDataService` | `tournamentId, tournamentName, limit` | `EsportsHeroBanStatResponse[]` | Top hero bi blue side ban nhieu nhat |
| GET | `/api/esports/data/hero-stats` | `EsportsController` | `EsportsDataService` | `tournamentId, tournamentName, teamCode, dateFrom, dateTo` | `EsportsHeroStatResponse[]` | Payload chi tiet cho Hero Statistics: pick/ban/presence rates, Blue Side, Red Side, va `heroIconUrl`/`heroAvatarUrl` tu `esports_game_drafts` |
| GET | `/api/esports/data/dashboard` | `EsportsController` | `EsportsDataService` | `tournamentId, tournamentName, teamCode, dateFrom, dateTo` | `EsportsDashboardResponse` | KPI/tables/charts data cho page public; runtime doc tu `esports_game_drafts` |
| POST | `/api/ban-pick/rooms` | `BanPickRoomController` | `BanPickRoomService` | `seriesType` | `BanPickCreateRoomResponse` | Tạo phòng online |
| GET | `/api/ban-pick/rooms/{roomCode}` | `BanPickRoomController` | `BanPickRoomService` | `roomCode` | `BanPickRoomStateResponse` | State phòng |
| POST | `/api/ban-pick/rooms/{roomCode}/join`, `/roll-side`, `/ready`, `/start` | `BanPickRoomController` | `BanPickRoomService` | none | `BanPickRoomStateResponse` | Lobby + bắt đầu draft |
| POST | `/api/ban-pick/rooms/{roomCode}/confirm` | `BanPickRoomController` | `BanPickRoomService` | `teamSide, actionType, heroId/heroName` | `BanPickRoomStateResponse` | Xác nhận ban/pick |
| POST | `/api/ban-pick/rooms/{roomCode}/lineup/reorder`, `/lineup/confirm` | `BanPickRoomController` | `BanPickRoomService` | reorder: `heroIds`; confirm: `teamSide` | `BanPickRoomStateResponse` | Sắp xếp/xác nhận lineup |
| POST | `/api/ban-pick/rooms/{roomCode}/next-game`, `/reset` | `BanPickRoomController` | `BanPickRoomService` | none | `BanPickRoomStateResponse` | Sang ván mới / reset |
| GET | `/api/ban-pick/history`, `/api/ban-pick/history/{id}` | `BanPickHistoryController` | `BanPickHistoryService` | `id` optional | `DraftHistoryResponse[]` / `DraftHistoryResponse` | Lịch sử draft |
| POST | `/api/ban-pick/history/{id}/winner` | `BanPickHistoryController` | `BanPickHistoryService` | legacy request | `DraftHistoryResponse` | Endpoint legacy bị reject với message winner được tính tự động theo Ban/Pick score |
| GET | `/api/ban-pick/leaderboard`, `/api/ban-pick/profile` | `BanPickHistoryController` | `BanPickHistoryService` | none | `PlayerStatsResponse[]`, `BanPickProfileResponse` | BXH public và stats cá nhân trên Solo page; `BanPickProfileResponse` gồm `user`, `stats`, `playerCard`, `history` để Solo page và Account preview dùng chung |
| GET | `/api/admin/ban-pick/rank-reset/preview` | `AdminBanPickController` | `BanPickSeasonResetService` | `type=SOFT|HARD` | `BanPickSeasonResetPreviewResponse` | Preview seasonal reset toàn server, trả count + min/max/avg rating trước/sau + sample player, không mutate DB |
| POST | `/api/admin/ban-pick/rank-reset` | `AdminBanPickController` | `BanPickSeasonResetService` | `type, confirmationText, note` | `BanPickSeasonResetExecuteResponse` | Execute seasonal reset thủ công toàn server; bắt buộc `confirmationText = RESET SOLO RANK`; ghi audit log và chặn chạy trùng cùng ngày |
| GET, PUT | `/api/admin/users`, `/api/admin/users/{id}` | `AdminUserController` | `AdminUserService` | list filters; update `name/avatarUrl/role/status/note` | page/detail DTO | Quản lý user |
| GET, PUT | `/api/admin/wiki/heroes`, `/api/admin/wiki/heroes/{id}` | `AdminWikiHeroController` | `AdminWikiHeroService` | update basic info + `banPickScore` | hero list/detail DTO kèm `banPickScore` | Quản lý hero |
| PUT | `/api/admin/wiki/heroes/{id}/roles`, `/api/admin/wiki/heroes/{id}/attributes` | `AdminWikiHeroController` | `AdminWikiHeroService` | `roles[]`, `attributes[]` | hero detail DTO | Gán role/attribute |
| GET, POST, PUT, DELETE | `/api/admin/wiki/attributes*` | `AdminWikiAttributeController` | `AdminWikiHeroService` | attribute upsert | list/detail/204 | Attribute admin chuẩn wiki |
| GET, POST, PATCH, DELETE | `/api/admin/attributes*` | `HeroAttributeController` | `HeroAttributeService` | attribute upsert | list/detail/204 | API legacy cho attribute |
| GET | `/api/admin/esports/game-drafts/export` | `EsportsAdminController` | `EsportsDraftService` | `tournamentId, tournamentName, matchId, dateFrom, dateTo` | CSV attachment UTF-8 BOM | Export game draft records theo format Excel mau; moi dong = 1 row trong `esports_game_drafts` |
| POST | `/api/admin/esports/game-drafts/import/preview` | `EsportsAdminController` | `EsportsDraftService` | multipart `file`, `overwriteExisting` | `EsportsGameDraftImportPreviewResponse` | Parse CSV/Excel, map tournament/team/hero, resolve parent match theo `date-only + tournament + stage + unordered team pair + score`, validate duplicate `(match_id, game_number)`, va tra preview token + summary truoc khi import that |
| POST | `/api/admin/esports/game-drafts/import/confirm` | `EsportsAdminController` | `EsportsDraftService` | `previewToken` | `EsportsGameDraftImportConfirmResponse` | Confirm preview hop le de tao/cap nhat `esports_matches` va `esports_game_drafts`; neu parent exact da ton tai thi reuse parent cu thay vi tao match moi, overwrite draft theo `(match_id, game_number)` khi admin bat overwrite, recalculate Elo neu match cha bi tao moi hoac doi ty so, va tra summary DB/ranking gom `affectedMatchIds`, `affectedSeriesCount`, `rankingsRecalculated` |
| GET, POST, PUT, DELETE | `/api/admin/esports/franchises*` | `EsportsAdminController` | `EsportsFranchiseService` | franchise body | list/detail/message | Admin CRUD/deactivate franchise catalog |
| GET, POST, PUT, DELETE | `/api/admin/esports/tournaments*` | `EsportsAdminController` | `EsportsTournamentService` | list: `franchiseId, franchiseCode`; request body tournament co `aerTier` | list/detail/message | Admin CRUD tournament catalog, sua duoc `aerTier`; xoa bi chan neu da link vao `esports_matches` |
| GET, POST, DELETE | `/api/admin/esports/tournaments/{id}/teams*` | `EsportsAdminController` | `EsportsTournamentService` | team body / `teamId` | roster/message | Admin quan ly team tham gia tung tournament |
| GET, POST, PUT, DELETE | `/api/admin/esports/teams*`, `/api/admin/esports/teams/matches/bulk-import` | `EsportsAdminController` | `EsportsAdminService` | team body / raw text import | team/list/message | Quản lý/import team |
| GET, POST, PUT, DELETE | `/api/admin/esports/matches*` | `EsportsAdminController` | `EsportsAdminService` | `EsportsMatchRequest` | match/list/message | Quản lý match history; `stage` ghi DB chi hop le `ck/playoff/bang/vongloai`, alias hop le se duoc normalize truoc khi save, unknown stage bi reject; `DELETE /api/admin/esports/matches/{id}` vẫn dùng để xóa từng trận, còn reset toàn bộ đã chuyển sang `POST /api/admin/esports/reset-data`; co the set `tournamentId` nhung flow cu van chay khi de null |
| GET | `/api/admin/esports/matches/{matchId}/game-drafts` | `EsportsAdminController` | `EsportsDraftService` | none | `EsportsGameDraftResponse[]` | List game draft records thuoc mot match |
| POST | `/api/admin/esports/matches/{matchId}/game-drafts` | `EsportsAdminController` | `EsportsDraftService` | `EsportsGameDraftRequest` | `EsportsGameDraftResponse` | Tao 1 game draft record moi |
| GET | `/api/admin/esports/game-drafts/{id}` | `EsportsAdminController` | `EsportsDraftService` | none | `EsportsGameDraftResponse` | Lay chi tiet 1 game draft record |
| PUT | `/api/admin/esports/game-drafts/{id}` | `EsportsAdminController` | `EsportsDraftService` | `EsportsGameDraftRequest` | `EsportsGameDraftResponse` | Sua 1 game draft record |
| DELETE | `/api/admin/esports/game-drafts/{id}` | `EsportsAdminController` | `EsportsDraftService` | none | `message` | Xoa 1 game draft record |

Ghi chú realtime:
- WebSocket endpoint: `/ws`
- App destinations: `/app/ban-pick/{roomCode}/confirm|lineup/reorder|lineup/confirm|start|roll-side|ready`
- Broadcast topics: `/topic/ban-pick/{roomCode}`, `/topic/ban-pick/{roomCode}/presence`
- Error queue: `/user/queue/ban-pick/errors`

**6. Bảng database**

| Entity/Table | Field chính | Quan hệ | Repository/DAO | Chức năng sử dụng |
|---|---|---|---|---|
| `users` | `id,email,name,displayName,avatarUrl,role,level,status,note` | 1-1 `player_stats`; 1-N `guides`, `tier_lists`, `tier_list_comments`, `tier_list_admin_ratings`, `draft_histories`; nhiều FK trong ban-pick | `UserRepository` | Auth, profile, admin user, author/content owner |
| `heroes` | `id,name,slug,title,avatarUrl,portraitUrl,bannerUrl,heroClass,difficulty,description,lore,ban_pick_score` | N-N `hero_roles`, `hero_classes`, `hero_attributes`; 1-N `hero_skills`, `hero_matchups`; N-1 từ `guides` | `HeroRepository` | Wiki hero, tier list hero refs, guide link, ban/pick hero pool, score nguồn cho tính tổng điểm/tỷ lệ thắng |
| `hero_roles` + `hero_role_mapping` | `id,code,name` | N-N với `heroes` | `HeroRoleRepository` | Lane role của tướng |
| `hero_classes` + `hero_class_mapping` | `id,name,displayName` | N-N với `heroes` | `HeroClassRepository` | Class tướng |
| `hero_attributes` + `hero_attribute_mapping` | `id,name,description,iconUrl,sortOrder` | N-N với `heroes` | `HeroAttributeRepository` | Attribute/dặc điểm hero |
| `hero_skills` | `id,hero_id,name,skillType,description,cooldown,manaCost,iconUrl,sortOrder` | N-1 về `heroes` | `HeroSkillRepository` | Skill chi tiết trong wiki |
| `hero_matchups` | `id,hero_id,target_hero_id,matchupType,difficulty,notes` | N-1 tới `heroes` và `targetHero` | `HeroMatchupRepository` | Matchup/counter/synergy |
| `guides` | `id,title,coverImageUrl,status,category,lane,excerpt,viewCount,readingTimeMinutes,author_id,hero_id,contentData,publishedAt` | N-1 `users`, N-1 `heroes`; runtime hiện chủ yếu đọc/ghi `contentData` | `GuideRepository` | Giáo án chiến thuật; spell hiện được lưu dạng string/slug/iconUrl trong `contentData` |
| `arcanas`, `bang_ngoc`, `huong_dan_ngoc`, `items`, `vat_pham`, `huong_dan_vat_pham` | Legacy Arcana/Item schema cũ | Đã retired khỏi runtime code/entity/repository/frontend; không còn được map bởi JPA/Hibernate | Không còn repository runtime; cleanup script `demo/sql/cleanup_legacy_unused_tables.sql` | Đã backup, review FK và drop khỏi DB local trong DB consistency repair; `guides.contentData` vẫn là nguồn Guide runtime |
| `phu_hieu`, `huong_dan_phu_hieu` | legacy bảng SQL cũ cho enchantment/guide relation | Có thể còn tồn tại ở môi trường DB cũ | không còn repository/entity runtime cho enchantments | Không còn là nguồn dữ liệu runtime của Wiki/Admin; cleanup cần migration riêng nếu muốn drop hẳn |
| `tier_lists` | `id,title,author_id,contentData,description,isOfficial,adminRating,createdAt,updatedAt` | N-1 `users`; 1-N `tier_list_ratings/comments/admin_ratings` | `TierListRepository` | Meta chính thức và community tier list |
| `meta_tier_lists` | Legacy/core table theo yêu cầu product owner | Không thuộc phạm vi cleanup legacy Arcana/Item/Esports old model | Giữ nguyên | Bảng này phải tiếp tục tồn tại; task này không drop, không gỡ runtime liên quan |
| `tier_list_ratings` | `id,tier_list_id,user_id(stored email),stars,createdAt,updatedAt` | N-1 `tier_lists` | `TierListRatingRepository` | Rating user |
| `tier_list_comments` | `id,tier_list_id,user_id,content,createdAt,updatedAt` | N-1 `tier_lists`, N-1 `users` | `TierListCommentRepository` | Bình luận community |
| `tier_list_admin_ratings` | `id,tier_list_id,admin_user_id,ratingValue,note` | N-1 `tier_lists`, N-1 `users` | `TierListAdminRatingRepository` | Đánh giá admin |
| `esports_franchises` | `id,code,name,tier_level,region,display_order,active` | 1-N `esports_tournaments` | `EsportsFranchiseRepository` | He thong giai dau me/franchise; seed mac dinh RPL, AOG, GCS, APL, AIC |
| `esports_tournaments` | `id,franchise_id,name,slug,season_year,split_name,tier_level,aer_tier,start_date,end_date,status` | N-1 `esports_franchises`; 1-N `esports_tournament_teams`; optional 1-N `esports_matches` qua `tournament_id` | `EsportsTournamentRepository` | Tung mua/giai cu the nhu `AOG Spring 2026`, `RPL Summer 2026`; `aer_tier` la source of truth cho ranking AER/Elo, hop le `0/1/2`, va `0` dung cho T0/global tournament nhu `APL`/`AIC` |
| `esports_tournament_teams` | `id,tournament_id,team_id,group_name,seed_number,status,note` | N-1 `esports_tournaments`, N-1 `esports_teams` | `EsportsTournamentTeamRepository` | Full roster team tham gia tung tournament, duoc sync theo cac doi thuc te xuat hien trong `esports_matches` cua tournament do |
| `esports_teams` | `id,teamCode,teamName,logoUrl,region,score,gameWins,gameLosses,matchWins,matchLosses` | Khong FK truc tiep toi `esports_matches`; lien he bang `teamCode`; co the duoc map vao `esports_tournament_teams` | `EsportsTeamRepository` | BXH esports va nguon roster cho tournament; leaderboard public dung `matchWins/matchLosses` cho `Series W/L`, khong dung `gameWins/gameLosses` |
| `esports_matches` | `id,matchDate,team1Code,team2Code,score1,score2,tier,stage,tournament_id(optional)` | Quan he logic qua `teamCode`; optional N-1 `esports_tournaments` qua `tournament_id` | `EsportsMatchRepository` | 1 row = 1 series/tran cha; dung cho Elo/AER ranking, match-level dashboard, va scope `affectedMatchIds` sau import; `score1/score2` la series score, khong phai score cua tung game; `tier` khong co FK tier-to-tier ma chi la fallback/legacy snapshot, chi duoc dung khi match khong co `tournament_id`; `stage` canonical bat buoc la `ck`, `playoff`, `bang`, hoac `vongloai` |
| `esports_game_drafts` | `id,match_id,game_number,blue_team_id,red_team_id,winner_team_id,duration_seconds,draft_format_code,source,10 ban hero ids,10 lineup hero ids,raw_draft_json` | N-1 `esports_matches`; N-1 `esports_teams` cho blue/red/winner; hero refs luu dang flat columns | `EsportsGameDraftRepository` | 1 row = 1 game/van con thuoc series `esports_matches`; dung cho hero stats, pick/ban, blue-red side stats, va export/admin draft workflow |
| `ban_pick_rooms` | `id,roomCode,status,phaseType,seriesType,currentGameNumber,host/guest/blue/red user,ready flags,deadline fields,pick history` | N-1 nhiều lần về `users`; 1-N `ban_pick_actions`, `ban_pick_room_participants` | `BanPickRoomRepository` | Trạng thái draft room |
| `ban_pick_room_participants` | `id,room_id,user_id,role,teamSide,joinedAt` | N-1 `ban_pick_rooms`, N-1 `users` | `BanPickRoomParticipantRepository` | Thành viên phòng |
| `ban_pick_actions` | `id,room_id,user_id,teamSide,actionType,heroId,phaseIndex,confirmedAt` | N-1 `ban_pick_rooms`, N-1 `users` | `BanPickActionRepository` | Log ban/pick từng phase |
| `draft_histories` | `id,roomCode,blue_user_id,red_user_id,winner_user_id,bluePicks,redPicks,blueBans,redBans,resultRecordedAt,win_rating_delta,loss_rating_delta` | N-1 `users` | `DraftHistoryRepository` | Lịch sử draft đã hoàn tất; mỗi row lưu snapshot delta rating cuối cùng của trận sau macro + ELO gap, và nếu bị anti-win-trading thì snapshot bị override thành `0/0`; rolling-50 replay vì vậy giữ đúng lịch sử rating kể cả match bị block; seasonal reset không xóa history mà chỉ đổi mốc replay bằng anchor; retention cleanup chỉ xóa row khi history đó không còn nằm trong top 50 gần nhất của cả 2 participant |
| `player_stats` | `id,user_id,totalMatches,wins,losses,rating,rating_anchor,rating_anchor_at,last_reset_type,pickedHeroCounts` | 1-1 `users` | `PlayerStatsRepository` | Aggregate Ban/Pick synced theo 50 draft gần nhất của từng user để phục vụ profile/leaderboard; rating base khởi tạo `1000`, floor `0`, tie không đổi W/L/rating; winner/loss penalty đều đọc lại snapshot `win_rating_delta/loss_rating_delta` đã lưu sau Macro Economy 30 ngày + ELO gap modifier (`10 điểm = 2%`, cap `50%`), rồi bị giữ nguyên `0/0` nếu match đã bị anti-win-trading block; seasonal reset toàn server sẽ cập nhật `rating`, `rating_anchor`, `rating_anchor_at`, `last_reset_type` để replay sau reset không cộng lại history cũ trước mùa; rank `S/A/B/C/D` được backend tính động theo percentile từ pool rating hiện tại |
| `ban_pick_rank_resets` | `id,reset_type,scheduled_date,executed_at,affected_players,base_rating,formula,executed_by,note` | N/A | `BanPickRankResetLogRepository` | Audit/idempotency cho seasonal reset toàn server; unique `scheduled_date` để scheduler/manual không chạy trùng; lưu loại reset, công thức, số player bị ảnh hưởng, người chạy và ghi chú |

**7. Kết luận**

- Dự án này là một cổng thông tin Liên Quân/Arena of Valor gồm 5 trục chính: `Wiki tướng`, `Tier List`, `Guide cộng đồng`, `Esports ranking`, và `Ban/Pick lab`; toàn bộ chạy trong một ứng dụng Spring Boot fullstack.
- Nhóm chức năng chính hiện thấy rõ trong code là:
  - Public content: trang chủ, wiki hero, guides, tier lists, esports
  - Authenticated community: profile, tạo guide, tạo/rate/comment tier list, tham gia ban/pick online
  - Admin: user management, wiki hero/attribute management, official tier list, esports data management
- Luồng người dùng chính:
  - Guest vào trang chủ -> xem wiki/tier list/guide/esports
  - User đăng nhập Google -> cập nhật profile -> tạo nội dung community hoặc tham gia ban/pick online
  - Admin -> quản lý hero/wiki/users/esports -> publish/meta official tier list
- Phần quan trọng nhất trong codebase là `BanPickRoomService` vì đây là state machine lớn nhất và có realtime, locking, timeout, series rules, history/statistics.
- Những điểm cần kiểm tra thêm trước khi phát triển tiếp:
  - Xác minh hướng chuẩn cho `hero attribute admin API` vì hiện có 2 route khác semantics.
  - Xác minh module `Guide` sẽ dùng `contentData JSON` hay sẽ chuyển sang các bảng chuẩn hóa `huong_dan_*`.
  - Xác minh README/documentation vì hiện không thấy `/api/meta`, trong khi README vẫn mô tả.
  - Theo dõi tiếp các module ngoài phạm vi cleanup này như guide admin/moderation; placeholder `items/arcana` trên `wiki.html` đã được loại khỏi runtime.
  - Đưa secret/config ra biến môi trường; hiện `application.properties` đang commit trực tiếp DB password và Google client id.
  - Bổ sung test cho `Guides`, `Tier Lists`, `Esports`, `Security`, và `WebSocket/BanPick` end-to-end.

**8. UI/UX Design System**

```text
UI/UX Design System
|- Muc dich
|  |- Tao baseline giao dien chung cho ATG Academy theo `docs/ui-style-guide.md`
|  |- Chuyen hoa Apple-like style thanh premium game/esports academy, khong copy Apple 100%
|  `- Giam tinh trang moi trang mot kieu button/card/search rieng
|- Nguoi dung lien quan
|  |- Public users: trang chu, wiki, tier list, esports, guides, ban/pick
|  |- Authenticated users: profile, community tier list, guide creation, ban/pick online
|  `- Admin users: dashboard, hero admin, attribute admin, esports data admin
|- File chinh
|  |- Style guide: `docs/ui-style-guide.md`
|  |- Agent rule: `docs/AGENTS.md`
|  |- CSS shared/base: `demo/src/main/resources/static/css/style.css`
|  |- CSS modules: `demo/src/main/resources/static/css/tier-list.css`, `ban-pick.css`, `account.css`, `wiki.css`, `esports.css`, `guides.css`, `admin.css`
|  `- Homepage va shared shell hien tai van load `style.css` lam lop nen chung
|- Component/token base hien tai
|  |- CSS variables: `--atg-primary`, `--atg-primary-focus`, `--atg-canvas`, `--atg-canvas-parchment`, `--atg-ink`, `--atg-hairline`
|  |- Spacing/radius/shadow/font tokens: `--atg-space-*`, `--atg-radius-*`, `--atg-shadow-*`, `--atg-font-*`
|  |- Legacy aliases: `--blue-color`, `--red-color`, `--bg-color`, `--panel-bg`, `--text-color`, `--gold-color`
|  |- Shared navbar dropdown pattern: `header.html` + `header-loader.js` dung cho `Tier List`, `Ban/Pick`, `Esports`, va `Wiki`
|  |- Shared compact draft evaluation component: `draft-balance-panel`, `draft-balance-item` cho score + win-rate cua hai doi
|  `- Base classes: `.atg-page`, `.atg-section`, `.atg-section-header`, `.atg-card`, `.atg-button-*`, `.atg-search`, `.atg-table`, `.atg-modal`, `.atg-empty-state`, `.atg-error-state`
|- CSS ownership sau refactor
|  |- `style.css` chi giu token, reset/base, shared typography, layout utilities, buttons, cards, search/input, modal/toast/empty/error, header/footer shared
|  |- `tier-list.css` own `tier-list.html`, `tier-list-all.html`, `tier-list-mine.html`, `tier-list-detail.html`
|  |- `ban-pick.css` own layout/page chrome của `ban-pick-free.html`, `ban-pick-standard.html`, `ban-pick-solo.html`, `ban-pick-leaderboard.html`
|  |- `player-card.css` own shared Player Card component cho `ban-pick-solo.html` và `account.html`
|  |- `account.css` own `account.html`; `wiki.css` own `wiki.html`; `esports.css` own `esports.html`, `esports-data.html`
|  |- `guides.css` own `giao-an.html`, `create-guide.html`, `guide-detail.html`
|  |- `admin.css` own `admin.html`, `admin-heroes.html`, `admin-attributes.html`, `admin-esports-data.html`
|  |- Shared/risky hero-pool selectors (`.tier-pool`, `#searchInput`, `.role-filters`, `.role-btn`, `.hero-temp-*`, `.hero-pool-sticky-header`) tam giu trong `style.css` vi con duoc tai su dung
|  `- Thu tu load CSS: `style.css` truoc, module CSS sau, de giu base token va page override on dinh
|- Luong hoat dong chinh
|  |- Giai doan 1: them token va class base, chua thay doi HTML/JS workflow
|  |- Giai doan 2: header va homepage se ap dung token/component base
|  |- Giai doan 3: tier list, wiki, esports ap dung component base nhung giu mat do du lieu
|  |- Giai doan 4: ban/pick ap dung visual system nhung uu tien thao tac nhanh va can bang hai doi
|  `- Giai doan 5: admin ap dung dashboard style sach, it mau, de thao tac
|- API/database
|  |- Khong co API endpoint moi
|  |- Khong co controller/service/entity/repository moi
|  `- Khong doi database/table/entity/schema/seed
|- Quyen truy cap
|  `- Khong doi auth/authorization; chi la CSS/design system baseline
`- Rui ro/can test thu cong
   |- Tach ownership CSS giam diff lon, nhung can smoke-test homepage, tier list, ban/pick, account, wiki, esports, guides, admin sau moi lan di chuyen selector
   |- Cac selector shared/risky duoc giu lai trong `style.css` cho den khi ownership ro hon; can tranh move tiep neu chua audit call-site
   `- Can kiem tra responsive va focus-visible tren desktop/tablet/mobile, dac biet cac page co hero pool, draft board, table va dashboard
```

**15. Esports Data**

```text
Esports Data
|- Feature name
|  `- Esports Data
|- Purpose
|  |- Tong hop draft esports da luu trong DB thanh public analytics page `/esports/data`
|  |- Runtime aggregate doc tu model game-level moi `esports_game_drafts`; 1 row = 1 van dau
|  |- Giu duoc cac chi so chinh: total games, total matches, blue side WR, top bans, top picks, hero stats
|  `- Khong con phu thuoc runtime vao `esports_match_games`, `esports_match_draft_actions`, `esports_match_game_lineups`
|- Related users
|  |- Public users xem du lieu thong ke Esports Data
|  `- Admin users giu workflow team/bulk import/match history o `admin.html#aer-data`, sau do quan ly game draft records qua `admin-esports-data.html` va mo nhanh trang public de check lai thong ke
|- Main HTML, JavaScript, and CSS files
|  |- `demo/src/main/resources/static/html/esports-data.html`
|  |- Main JavaScript: `demo/src/main/resources/static/js/esports-data.js`
|  |- Main CSS: `demo/src/main/resources/static/css/esports.css`
|  `- Header / Navigation: `demo/src/main/resources/static/html/header.html`, `demo/src/main/resources/static/js/header-loader.js`
|- Related controller, service, entity, and repository files
|  |- Controller: `demo/src/main/java/com/example/demo/controller/EsportsController.java`
|  |- Controller: `demo/src/main/java/com/example/demo/controller/StaticPageRedirectController.java`
|  |- Service: `demo/src/main/java/com/example/demo/service/EsportsDataService.java`
|  |- Repository: `demo/src/main/java/com/example/demo/repository/EsportsGameDraftRepository.java`
|  |- Util: `demo/src/main/java/com/example/demo/util/EsportsTournamentCatalog.java`
|  |- DTO: `demo/src/main/java/com/example/demo/dto/esports/EsportsHeroBanStatResponse.java`
|  |- DTO: `demo/src/main/java/com/example/demo/dto/esports/EsportsHeroStatResponse.java`
|  |- DTO: `demo/src/main/java/com/example/demo/dto/esports/EsportsTournamentOptionResponse.java`
|  |- Entity reused: `demo/src/main/java/com/example/demo/entity/Hero.java`
|  |- Entity reused: `demo/src/main/java/com/example/demo/entity/EsportsMatch.java`
|  `- Entity: `demo/src/main/java/com/example/demo/entity/EsportsGameDraft.java`
|- API endpoints
|  |- `GET /api/esports/data/tournaments`
|  |- `GET /api/esports/data/dashboard?tournamentName=&teamCode=&dateFrom=&dateTo=` -> tra summary, sideAdvantage, heroStats, topBannedHeroes, topBlueBannedHero, teamOptions
|  |- `GET /api/esports/data/hero-stats?tournamentId=&tournamentName=&teamCode=&dateFrom=&dateTo=` -> tra payload Hero Statistics day du cho `Picks`, `Blue Side`, `Red Side`, `Bans`, `Picks & Bans`
|  |- `GET /api/esports/data/top-banned-heroes?tournamentName=...&limit=5`
|  |- `GET /api/esports/data/top-blue-banned-heroes?tournamentName=...&limit=5`
|  `- `GET /api/esports/data/top-red-banned-heroes?tournamentName=...&limit=5`
|- Database table or entity
|  |- `heroes`
|  |- `esports_matches`
|  |- `esports_game_drafts`
|  `- `esports_match_games`, `esports_match_draft_actions`, `esports_match_game_lineups` da duoc retire khoi runtime; source of truth la `esports_game_drafts`
|- Header / Navigation
|  |- Shared header bo menu top-level rieng `Esports Data`; trigger top-level `Esports` da doi thanh dropdown
|  |- Dropdown `Esports` gom item `Esports Ranking` (`/html/esports.html`) va `Esports Data` (`/esports/data`); dropdown `Wiki` giu nguyen `Tướng`, `Bổ trợ`, `Phù hiệu`
|  `- Active state duoc nhan dien tren trigger `Esports` khi dang o `esports.html`, `/esports`, `/esports/data`, hoac `esports-data.html`; item con tuong ung active theo page hien tai
|- Routing / Static Pages
|  |- Them page `esports-data.html`
|  |- Them route dep `/esports/data` -> redirect `/html/esports-data.html`
|  `- Legacy top-level `/esports-data.html` duoc redirect vao static page moi
|- Main workflow
|  |- User mo `/esports/data` hoac `esports-data.html`
|  |- Frontend load danh sach giai dau tu `GET /api/esports/data/tournaments`
|  |- Frontend hien tournament filter, KPI cards, bang top bans, bang top blue/red side bans, bang top picks, side win rate, va hero statistics; UI giu tinh than Apple-like toi gian theo `docs/ui-style-guide.md`
|  |- `GET /api/esports/data/dashboard` la nguon chinh cho summary, side advantage, hero stats, team options, top banned hero, va top blue banned hero
|  |- `GET /api/esports/data/hero-stats` va dashboard deu aggregate tu `esports_game_drafts`: 10 cot ban cho bans, 10 cot lineup cho picks, `winner_team_id` so voi `blue_team_id`/`red_team_id` cho side WR va hero wins; response hien gom `pickCount/pickWins/pickLosses/pickWinRate/pickRate`, `bluePick*`, `redPick*`, `banCount/banRate`, `presenceCount/presenceRate`, va `heroIconUrl` (giu `heroAvatarUrl` de tuong thich nguoc)
|  |- Filter `tournamentName` tiep tuc di theo convention `tier`; backend van chap nhan `teamCode/dateFrom/dateTo` cho reuse sau nay
|  |- Top picked heroes duoc suy ra tu lineup theo lane `DSL/JGL/MID/ADL/SUP`, khong can draft-phase data
|  |- Bang Hero Statistics tren `esports-data.html` co loading state, empty state, grouped headers `Picks` / `Blue Side` / `Red Side` / `Bans` / `Picks & Bans`, cot `Details` voi nut `Show` an toan, va sort mac dinh tu backend theo `pickCount desc -> presenceCount desc -> heroName asc`
|  |- `GET /api/esports/data/top-banned-heroes`, `GET /api/esports/data/top-blue-banned-heroes`, va `GET /api/esports/data/top-red-banned-heroes` duoc giu de page co the render bang rieng va empty-state ro rang
|  |- UI public hien tai uu tien tournament filter; team/date filters giu o backend cho reuse sau nay
|  |- Backend van dung `EsportsTournamentCatalog`, nhung neu UI gui raw `tournamentTier` dang ton tai trong data thi van chap nhan de tranh empty sai
|  `- Empty state hien khi chua co `esports_game_drafts` hoac scope filter khong tra ve data
|- Access permissions
|  `- Tất cả `GET /api/esports/**` và page public đều cho guest xem theo `SecurityConfig`
`- Risk notes or manual testing areas
   |- Can verify count group-by dung tren du lieu DB that su, nhat la case nhieu game trong cung mot match va khong bi double-count khi merge flat columns
   |- Can verify cong thuc WR khong bao gio ra `NaN/Infinity` khi mau so bang 0
   |- Can test default scope khi bo trong `tournamentName`: hien dung toan bo draft data, khong tu dong khoa vao giai moi nhat
   |- Can test `tournamentName` invalid tra 400 va UI khong crash
   |- Can test empty state khi chua co `esports_game_drafts`
   |- Can test top bans, top blue bans, top red bans, top picks, side WR, hero stats tren data that sau migration
   |- Can test responsive/table density tren desktop-mobile, nhat la card border mong + shadow nhe theo style guide
   `- Can test trang `esports.html`, Elo ranking, match feed, va AER workflow khong bi anh huong
```

**14. Esports Match Game Draft History**

```text
Esports Match Game Draft History
|- Feature name
|  `- Esports Match Game Draft History
|- Purpose
|  |- Luu du lieu draft esports theo tung van thuoc mot `esports_matches` series da co san
|  |- Don gian hoa model thanh 1 record game-level duy nhat `esports_game_drafts`
|  |- Moi record chua side, winner, duration, bans, final lineup theo lane, va `raw_draft_json`
|  `- Giu `esports_matches`, `esports_teams`, `heroes`; khong xoa du lieu that neu chua co migration/backup ro rang
|- Related users
|  |- Admin users quan ly du lieu tran va tung game draft record
|  `- Public users tiep tuc doc duoc danh sach van / draft history qua public compatibility endpoints neu can
|- Main HTML, JavaScript, and CSS files
|  |- `demo/src/main/resources/static/html/admin-esports-data.html`
|  |- Main JavaScript: `demo/src/main/resources/static/js/admin-esports-data.js`
|  `- Main CSS: `demo/src/main/resources/static/css/admin.css`
|- Related controller, service, entity, repository, dto, and bootstrap files
|  |- Controller: `demo/src/main/java/com/example/demo/controller/EsportsAdminController.java`
|  |- Controller: `demo/src/main/java/com/example/demo/controller/EsportsController.java`
|  |- Service: `demo/src/main/java/com/example/demo/service/EsportsDraftService.java`
|  |- Entity: `demo/src/main/java/com/example/demo/entity/EsportsGameDraft.java`
|  |- Entity reused: `demo/src/main/java/com/example/demo/entity/EsportsMatch.java`
|  |- Entity reused: `demo/src/main/java/com/example/demo/entity/EsportsTeam.java`
|  |- Entity reused: `demo/src/main/java/com/example/demo/entity/Hero.java`
|  |- Repository: `demo/src/main/java/com/example/demo/repository/EsportsGameDraftRepository.java`
|  |- DTO: `demo/src/main/java/com/example/demo/dto/esports/EsportsGameDraftRequest.java`
|  |- DTO: `demo/src/main/java/com/example/demo/dto/esports/EsportsGameDraftResponse.java`
|  |- DTO: `demo/src/main/java/com/example/demo/dto/esports/EsportsGameDraftImportPreviewResponse.java`
|  |- DTO: `demo/src/main/java/com/example/demo/dto/esports/EsportsGameDraftImportConfirmRequest.java`
|  `- DTO: `demo/src/main/java/com/example/demo/dto/esports/EsportsGameDraftImportConfirmResponse.java`
|- API endpoints
|  |- `GET /api/admin/esports/matches/{matchId}/game-drafts`
|  |- `POST /api/admin/esports/matches/{matchId}/game-drafts`
|  |- `GET /api/admin/esports/game-drafts/export`
|  |- `POST /api/admin/esports/game-drafts/import/preview`
|  |- `POST /api/admin/esports/game-drafts/import/confirm`
|  |- `GET /api/admin/esports/game-drafts/{id}`
|  |- `PUT /api/admin/esports/game-drafts/{id}`
|  |- `DELETE /api/admin/esports/game-drafts/{id}`
|  `- Public compatibility endpoints cu `/api/esports/matches/{matchId}/games`, `/api/esports/games/{gameId}/draft-actions`, `/api/esports/games/{gameId}/lineups` da bi xoa khoi runtime
|- Database / Entity / Migration
|  |- Table/entity moi: `esports_game_drafts`
|  |- 1 row = 1 game: `match_id`, `game_number`, `blue_team_id`, `red_team_id`, `winner_team_id`, `duration_seconds`, `draft_format_code`, `source`, 10 cot ban, 10 cot lineup, `raw_draft_json`
|  |- Reused table/entity: `esports_matches`
|  |- Reused table/entity: `esports_teams`
|  |- Reused table/entity: `heroes`
|  |- SQL migration: `demo/sql/esports_game_drafts_direction_b_migration.sql`
|  |- Migration script tao bang moi, backfill best-effort tu `esports_match_games`, `esports_match_draft_actions`, `esports_match_game_lineups`, va them SQL verify count/FK
|  |- Cac bang `esports_match_games`, `esports_match_draft_actions`, `esports_match_game_lineups` da duoc retire khoi runtime code/JPA
|  |- Unique moi: `(match_id, game_number)`
|  |- FK moi: `match_id -> esports_matches.id`, `blue_team_id/red_team_id/winner_team_id -> esports_teams.id`
|  |- Khong doi schema trong task export CSV; 5 blue bans + 5 red bans van giu nguyen
|  |- Vi project dang `ddl-auto=update`, task nay da xoa old entities/repositories/deprecated DTO de tranh Hibernate tao lai bang cu
|  `- `demo/sql/cleanup_legacy_unused_tables.sql` da duoc chay sau backup/FK review; 3 bang old model da drop khoi DB local
|- Esports Ranking
|  |- Elo/ranking workflow cu van tiep tuc doc `esports_matches` va `teamCode`, khong refactor model ranking
|  |- Refactor nay khong doi business logic `EloCalculationService` va khong xoa `esports_matches`
|  |- Regression test `demo/src/test/java/com/example/demo/service/EloCalculationServiceTest.java` mo phong `docs/tinhtoan.py` de khoa tier 0/1/2, protected min, tier 2 safe mode, shockwave, RDP, Champion Point, game/match stats, va ordering `match_date ASC, id ASC`
|  `- `esports_game_drafts` chi bo sung game-level data cho Esports Data, khong chen vao tinh Elo/ranking
|- Admin Esports
|  |- Admin tiep tuc CRUD team/match/import qua `EsportsAdminService`; nut reset data legacy da bo khoi `admin.html`
|  |- Admin giu `Bulk Import` + team roster + match history/ranking o module `AER Data` (`/html/admin.html#aer-data`)
|  |- Admin dung page `/html/admin-esports-data.html` cho workflow `upload file -> preview import -> ap dung vao DB` va `match -> game draft records` qua `EsportsDraftService`
|  |- Sidebar/admin dashboard tach ro `AER Data` va `Esports Data`; hash cu `#teams`/`#esports` van mo lai AER Data de giu flow cu
|  |- Form series tren page admin nhap `match_date`, `team 1`, `team 2`, `score1`, `score2`, `giai/tier`, va `stage`; UI goi y 4 stage canonical `bang/playoff/ck/vongloai`, frontend se doi alias quen tay ve canonical truoc khi goi API, va backend van reject unknown stage
|  |- Chon 1 match se load list game draft records cua series, selected match card, editor form, va validation summary
|  |- Nut `Xuat CSV` goi `GET /api/admin/esports/game-drafts/export`; neu dang chon match thi uu tien `matchId`, neu khong thi co the dung `tournamentName/dateFrom/dateTo`
|  |- Import UI moi co file input `.csv/.xlsx/.xls`, checkbox overwrite ro rang, status card preview, summary cards, bang preview tung dong, danh sach error/warning, va nut `Ap dung vao DB`
|  |- UI moi theo `docs/ui-style-guide.md`: card vien mong, shadow rat nhe, content-first, button pill, khong dua framework moi vao frontend
|  |- Form game draft cho phep nhap `gameNumber`, `blueTeam`, `redTeam`, `winnerTeam`, `duration`, `draftFormatCode`, `source`, 5 blue bans, 5 red bans, lineup `DSL/JGL/MID/ADL/SUP`
|  |- Validate service layer:
|  |- `match` phai ton tai
|  |- `blueTeamId`/`redTeamId` phai hop le va thuoc series
|  |- `blueTeamId != redTeamId`
|  |- `winnerTeamId` neu co phai la mot trong hai side
|  |- `gameNumber` khong duoc trung trong cung match
|  |- `durationSeconds >= 0` neu co
|  |- Hero khong duoc trung trong cung game khi xet bans + lineup
|  |- Import row bat buoc co `Date`, `Tournament`, `Match/game_number`, `Team_1`, `T1_Side`, `Team_2`, `T2_Side`; `Length` dang `mm:ss` duoc convert sang seconds
|  |- Tournament phai match entity `esports_tournaments` theo `name/slug`; khong tu tao tournament moi khi import
|  |- Team map theo `teamCode/teamName`; hero map theo `name/slug`; khong map duoc thi row vao danh sach loi
|  |- T1_Side=Blue thi Team_1 map vao blue side; T1_Side=Red thi Team_1 map vao red side
|  |- Duplicate `(match_id, game_number)` chi duoc overwrite khi admin bat overwrite truoc luc preview
|  `- Completeness summary tren admin page tong hop so game complete/incomplete, tong bans/picks, missing winner, missing lineup, missing bans
|- Main workflow
|  |- Admin có thể `Bulk Import` match history ngay trên `/html/admin.html#aer-data`; API và format raw text giữ nguyên workflow cũ
|  |- Admin tao/sua series bang form match: `match_date` + `team1/team2` + `score1/score2` + `tier` + `stage`; stage alias hop le nhu `final`, `group`, `qualifier`, `play-off`, `vòng bảng`, `chung kết` se duoc normalize ve `ck/playoff/bang/vongloai`
|  |- Admin co the upload file Excel/CSV tren `/html/admin-esports-data.html` -> backend preview map tournament/team/hero -> hien summary + error/warning list -> chi cho Confirm khi preview sach loi
|  |- Confirm import co the:
|  |- Dung `esports_matches` hien co neu khop `DATE(match_date) + Tournament + Stage + unordered team pair + series score`; khac `match_time` hoac dao team order van phai reuse parent cu
|  |- Gan `tournament_id` vao match cu dang null neu file match entity tournament
|  |- Tao 1 match cha moi neu chua co, score series suy ra tu cot `Winner`, stage lay tu file neu co hoac fallback `bang`, va gio match mac dinh `12:00`
|  |- Cot `Match` trong file import duoc hieu la `game_number`, khong phai `match_id`
|  |- Nhieu dong cung `Date + Tournament + Stage + team pair` se duoc group thanh 1 series cha va nhieu row `esports_game_drafts`
|  |- Neu co nhieu parent exact thi preview chon canonical parent: uu tien match da co draft, neu van hoa thi lay `id` nho hon
|  |- Tao draft moi hoac overwrite draft trung `game_number` theo khoa `(match_id, game_number)` neu admin da bat overwrite
|  |- Sau import, page refresh data va admin co the mo `/esports/data` de verify public analytics doc du lieu moi
|  |- Frontend gui payload draft editor thu cong: `blueBans[]`, `redBans[]`, `blueLineup{DSL..SUP}`, `redLineup{DSL..SUP}`, `winnerTeamId`, `durationSeconds`
|  |- `Length` co the nhap dang `mm:ss` hoac seconds; frontend normalize truoc khi goi API editor tay va backend convert khi import file
|  |- Save xong, page reload list draft cua match, render status chip, va cap nhat validation summary
|  |- Export CSV doc truc tiep tu `esports_game_drafts`; Team_1 = blue side, Team_2 = red side, moi dong = 1 van, `Length` = `mm:ss`
|  |- Public/frontend runtime Esports Data chi con doc `/api/esports/data/*`; admin runtime chi con doc `/api/admin/esports/*/game-drafts`
|  |- Dashboard public hien `Total Games` = so row `esports_game_drafts` va `Total Series` = distinct `match_id`
|  `- Xoa game draft record khong anh huong `esports_matches`; series parent van duoc giu nguyen cho AER/Ranking
|- Access permissions
|  |- `GET /api/esports/**` lien quan public read la guest-friendly theo `SecurityConfig`
|  `- `GET /api/admin/esports/game-drafts/export` va cac `POST/PUT/DELETE /api/admin/esports/**` deu yeu cau role `ADMIN` theo `SecurityConfig`
`- Risk notes or manual testing areas
   |- Migration tu model 18-phase sang flat table la best-effort; can backup DB that truoc khi drop bat ky bang cu nao
   |- Neu DB that chua co `esports_game_drafts`, public page se hien empty state du compile/test van pass
   |- Manual CRUD admin can co admin token/session hop le; verify HTTP anonymous khong du de ket luan full workflow
   |- DB consistency repair da backup DB local, chay cleanup script va drop `esports_match_games`, `esports_match_draft_actions`, `esports_match_game_lineups`; `esports_game_drafts` tiep tuc la source of truth game-level
   |- Can smoke-test import `.csv` va `.xlsx`: preview header, map team/hero/tournament, convert `Length`, warning match moi, va button Confirm chi mo khi khong con error
   |- Can test duplicate `(match_id, game_number)` voi 2 case: overwrite tat -> vao danh sach loi; overwrite bat -> preview thanh overwrite action
   |- Can test row khong map duoc tournament/team/hero thi bi chan truoc confirm va khong ghi DB
   `- Can smoke-test `admin.html#aer-data` cho bulk import/match history va `admin-esports-data.html` cho upload preview/ap dung vao DB, chon match, create/update/delete game draft record, duplicate validation, va link verify sang `/esports/data`
```

## 2026-05-16 - Solo Ban/Pick Dodge Penalty v1

- Scope:
  - Chi ap dung cho room `IN_PROGRESS` + `phaseType = DRAFT`.
  - Khong phat lobby `WAITING/READY`.
  - Khong phat room da `FINISHED`.
  - Khong phat `LINEUP_ADJUSTMENT` trong v1.

- Runtime:
  - Current-turn player timeout trong draft -> room finish theo forfeit.
  - Current-turn player disconnect trong draft -> grace window `10s`; reconnect trong grace thi khong phat.
  - `resetRoom()` khi room dang `IN_PROGRESS` bi reject de dong escape hatch.

- Rating / stats:
  - Dodge van di qua pipeline da co: macro economy, ELO gap modifier, anti-win-trading, rating delta snapshot, rolling-50 replay.
  - History dodge van duoc luu de audit; neu anti-win-trading block rating thi delta co the la `0/0`.

- Persistence / DB:
  - `users.ban_pick_cooldown_until`
  - `draft_histories.end_reason`
  - `draft_histories.dodged_user_id`
  - Cooldown v1 = `5 minutes`.

**20. Elo Replay Ordering**

```text
Elo Replay Ordering
|- Feature name
|  `- Elo Replay Ordering
|- Purpose
|  |- Giu Elo replay/recalculate deterministic khi nhieu series co cung `match_date`
|  `- Tranh sai lech ranking do Elo phu thuoc thu tu xu ly tran
|- Related controller, service, entity, and repository files
|  |- Service: `demo/src/main/java/com/example/demo/service/EloCalculationService.java`
|  |- Service: `demo/src/main/java/com/example/demo/service/EsportsDraftService.java`
|  |- Repository: `demo/src/main/java/com/example/demo/repository/EsportsMatchRepository.java`
|  `- Entity: `demo/src/main/java/com/example/demo/entity/EsportsMatch.java`
|- Main workflow
|  |- Moi lan replay/recalculate ranking, backend doc `esports_matches` theo `match_date ASC, id ASC`
|  |- Neu 2 match cung timestamp thi match co `id` nho hon duoc tinh truoc
|  `- `id ASC` dong vai tro tie-breaker deterministic vi Elo phu thuoc thu tu tran, trong khi formula/tier/stage logic giu nguyen
|- Database table or entity
|  `- `esports_matches` la nguon replay cho Elo; khong doi schema va khong can migration
`- Risk notes or manual testing areas
   |- Can test 2 match cung `matchDate` nhung `id` khac nhau de verify service tinh match `id` nho truoc
   `- Can smoke-test ranking workflow sau create/update/import match de dam bao moi luong replay deu dung ordering nay
```

**16. Esports Franchise / Tournament Catalog**

```text
Esports Franchise / Tournament Catalog
|- Feature name
|  `- Esports Franchise / Tournament Catalog
|- Purpose
|  |- Tach "franchise" (giai me) khoi "tournament" (mua giai cu the)
|  |- Quan ly roster team tham gia tung tournament ma khong pha `esports_teams`, `esports_matches`, `esports_game_drafts`
|  |- `esports_tournaments.aer_tier` la nguon tier so chuan cho ranking AER doc truc tiep tu DB
|  `- Cho phep filter analytics/export theo `tournamentId` trong khi van fallback `tier` cho data legacy
|- Related users
|  |- Public users xem danh sach franchise, tournament, va team tham gia
|  `- Admins CRUD franchise/tournament, sua `aerTier`, va mapping team
|- Main HTML, JavaScript, and CSS files
|  |- Public page `demo/src/main/resources/static/html/esports-data.html`
|  |- Public JS `demo/src/main/resources/static/js/esports-data.js`
|  |- Admin page `demo/src/main/resources/static/html/admin-esports-data.html`
|  `- Admin JS `demo/src/main/resources/static/js/admin-esports-data.js`
|- Related controller, service, entity, and repository files
|  |- Controllers: `demo/src/main/java/com/example/demo/controller/EsportsController.java`, `demo/src/main/java/com/example/demo/controller/EsportsAdminController.java`
|  |- Services: `demo/src/main/java/com/example/demo/service/EsportsFranchiseService.java`, `demo/src/main/java/com/example/demo/service/EsportsTournamentService.java`
|  |- Entities: `demo/src/main/java/com/example/demo/entity/EsportsFranchise.java`, `demo/src/main/java/com/example/demo/entity/EsportsTournament.java`, `demo/src/main/java/com/example/demo/entity/EsportsTournamentTeam.java`
|  |- Repositories: `demo/src/main/java/com/example/demo/repository/EsportsFranchiseRepository.java`, `demo/src/main/java/com/example/demo/repository/EsportsTournamentRepository.java`, `demo/src/main/java/com/example/demo/repository/EsportsTournamentTeamRepository.java`
|  `- Match link reused: `demo/src/main/java/com/example/demo/entity/EsportsMatch.java`
|- API endpoints
|  |- `GET /api/esports/franchises`
|  |- `GET /api/esports/franchises/{code}`
|  |- `GET /api/esports/tournaments`
|  |- `GET /api/esports/tournaments/{id}`
|  |- `GET /api/esports/tournaments/{id}/teams`
|  |- `GET /api/admin/esports/franchises`
|  |- `POST /api/admin/esports/franchises`
|  |- `PUT /api/admin/esports/franchises/{id}`
|  |- `DELETE /api/admin/esports/franchises/{id}`
|  |- `GET /api/admin/esports/tournaments`
|  |- `POST /api/admin/esports/tournaments`
|  |- `PUT /api/admin/esports/tournaments/{id}`
|  |- `DELETE /api/admin/esports/tournaments/{id}`
|  |- `GET /api/admin/esports/tournaments/{id}/teams`
|  |- `POST /api/admin/esports/tournaments/{id}/teams`
|  `- `DELETE /api/admin/esports/tournaments/{id}/teams/{teamId}`
|- Database / Entity / Migration
|  |- Tables/entities: `esports_franchises`, `esports_tournaments`, `esports_tournament_teams`
|  |- Optional match link: `esports_matches.tournament_id -> esports_tournaments.id`
|  |- Khong co FK tier-to-tier giua `esports_matches.tier` va `esports_tournaments.aer_tier`
|  |- SQL migration: `demo/sql/esports_franchises_tournaments_migration.sql`, `demo/sql/add_aer_tier_to_esports_tournaments.sql`
|  |- Data backfill script da duoc them cho case import draft GCS ngay `2026-03-27`: `demo/sql/backfill_gcs_spring_2026_match_tournament_id_20260327.sql`
|  |- Seed franchise bat buoc: `RPL`, `AOG`, `GCS`, `APL`, `AIC`
|  |- Seed tournament toi thieu: `AOG Spring 2026`, `RPL Summer 2026`, `GCS Spring 2026`
|  |- `esports_tournaments.aer_tier` mac dinh `1`, hop le `0/1/2`; admin co the sua truc tiep tren Tournament Management ma khong duoc ep `0` thanh `1`
|  `- `esports_tournament_teams` la full roster seed/runtime sync; chi insert khi `esports_teams.team_code` da ton tai
|- Main workflow
|  |- Public page load franchise/tournament catalog -> user chon scope -> analytics API goi `tournamentId`
|  |- Admin vao `Tournament Management` -> filter theo franchise -> tao/sua tournament -> set `aerTier` + `tierLevel` -> gan team tham gia; neu sua `aerTier` thi linked `esports_matches.tier` duoc sync lai snapshot va ranking duoc recalculate
|  |- Sau khi `ap dung vao DB`, backend lookup `esports_tournaments.aer_tier`, recalculate ranking AER/Elo truc tiep tu DB theo `affectedMatchIds`, va UI chi hien summary import/ranking
|  `- Match cu van hop le neu `tournament_id` de null; service/export fallback ve `tier`, con match co tournament se uu tien `aer_tier`; chi can khai bao tournament `APL/AIC` voi `aer_tier = 0` la moi match link vao tournament do se tinh theo Tier 0
|- Access permissions
|  |- `GET /api/esports/**` la public read
|  `- `POST/PUT/DELETE /api/admin/esports/**` can role `ADMIN`
`- Risk notes or manual testing areas
   |- Production DB can chay SQL migration hoac de Hibernate `ddl-auto=update` tao schema; local MySQL hien da co `mysql`/`mysqldump` CLI va du lieu backfill `tournament_id` can duoc backup + verify bang SQL truc tiep truoc khi import XLSX that
   |- `tournament_id` tren `esports_matches` la optional; can smoke-test admin create/update match voi ca 2 truong hop null va co gia tri
   |- `EsportsDataSeeder` dang gan `matchDate` theo `now().minusDays(30) + i minutes`, nen du lieu seed co the trung ngay that cua file XLSX; neu cung cap doi xuat hien nhieu lan va `tournament_id` con null thi preview import game draft co the bao ambiguous parent cho den khi duoc backfill
   |- Roster seed phu thuoc data team hien co; neu DB that thieu `team_code` thi row mapping se bi skip co chu y
   `- Export CSV game draft hien co khong doi; task nay khong import CSV vao `esports_matches`/`esports_game_drafts` va khong tao JSON/TXT
```

**9. Tier List Import / Export**

```text
Tier List Import / Export
|- Feature name
|  `- Auto Tier List Meta from Hero Score + Tier List export
|- Purpose
|  |- Bo workflow import tay `Ten tuong + Tier`; official meta gio duoc sinh truc tiep tu `heroes.ban_pick_score`
|  |- Xep tier theo rule `score > 9 => S`, `> 7.5 => A`, `> 5 => B`, `> 2.5 => C`, con lai `D`
|  `- Public Tier List page va file export anh van dung title `Tier List Meta`, creator metadata official, va aggregate average/count da gom user + admin rating
|- Related users
|  |- Public users xem meta chinh thuc
|  `- Admin generate/save meta tu hero score
|- Main HTML, JavaScript, CSS files
|  |- `demo/src/main/resources/static/html/tier-list.html`
|  |- `demo/src/main/resources/static/html/tier-list-detail.html`
|  |- `demo/src/main/resources/static/js/tier-list-app.js`
|  |- `demo/src/main/resources/static/js/tier-list-export.js`
|  |- `demo/src/main/resources/static/css/style.css` (shared/base)
|  `- `demo/src/main/resources/static/css/tier-list.css` (official/community/detail/export UI)
|- Related controller, service, entity, repository files
|  |- Controller: `demo/src/main/java/com/example/demo/controller/TierListController.java`
|  |- Controller: `demo/src/main/java/com/example/demo/controller/AdminTierListController.java`
|  |- Service: `demo/src/main/java/com/example/demo/service/TierListCommunityService.java`
|  |- Service: `demo/src/main/java/com/example/demo/service/HeroContentDataService.java`
|  |- Entity: `demo/src/main/java/com/example/demo/entity/Hero.java`, `demo/src/main/java/com/example/demo/entity/HeroRole.java`, `demo/src/main/java/com/example/demo/entity/TierList.java`
|  `- Repository: `demo/src/main/java/com/example/demo/repository/HeroRepository.java`, `demo/src/main/java/com/example/demo/repository/TierListRepository.java`
|- API endpoints
|  |- `GET /api/tier-lists/official`
|  |- `POST /api/admin/tier-lists/official/regenerate-from-hero-scores`
|  |- `POST /api/tier-lists`
|  `- `GET /api/tier-lists/{id}`; official/community response van giu `creatorName` + `creator_name`
|- Database table or entity
|  |- `tier_lists`
|  |- `users`
|  |- `heroes`
|  `- `hero_roles`
|- Main workflow
|  |- Public mo trang Tier List -> frontend goi `GET /api/tier-lists/official`; neu DB chua co official row thi backend tra preview generate tu hero score de board khong bi trong
|  |- Admin sua `banPickScore` trong `admin-heroes.html`
|  |- Admin quay lai `tier-list.html` va bam `Luu Meta Chinh`
|  |- Backend `HeroContentDataService.generateOfficialTierListFromHeroScores()` lay hero tu DB, uu tien `Primary Role`, fallback sub role dau tien hop le, xep hero vao DSL -> JGL -> MID -> ADL -> SUP, roi serialize vao `tier_lists.contentData`
|  `- Export anh van dung renderer hien tai; community detail/rating/download khong doi
|- Access permissions
|  |- Public xem official page va export detail/community tier list cua minh
|  `- Chi Admin duoc regenerate/save official tier list
`- Risk notes or manual testing areas
   |- Hero thieu score/null phai fallback `0` -> `D`, khong hien `NaN/Infinity`
   |- Hero thieu `Primary Role` phai fallback sub role dau tien neu co, khong crash
   |- Can test boundary `9`, `7.5`, `5`, `2.5` vi rule dung `>`
   `- Can test save/reload de dam bao `tier_lists.contentData` va UI khong lech nhau, cung voi creator metadata + export image
```

**10. Ban/Pick Navigation & Routing**

```text
Ban/Pick Navigation & Routing
|- Header / Navigation
|  |- Navbar dropdown `Ban/Pick` la cach vao va chuyen mode chinh cho Ban/Pick
|  |- Dropdown di thang toi `ban-pick-free.html`, `ban-pick-standard.html`, `ban-pick-solo.html`
|  `- Active nav state van dua tren filename `ban-pick-*`, khong can landing page trung gian
|- Ban/Pick Free Mode
|  |- Wrapper page: `demo/src/main/resources/static/html/ban-pick-free.html`
|  |- Hero pool van lay tu `GET /api/wiki/heroes`; moi hero co `banPickScore` tu backend/database
|  `- Khong con nut `Đổi chế độ` / `Thoát` chi de quay ve route legacy `/ban-pick`; user doi mode qua navbar dropdown, panel `Đánh giá đội hình` cap nhat realtime khi pick/huy/reset
|- Ban/Pick Standard Mode
|  |- Wrapper page: `demo/src/main/resources/static/html/ban-pick-standard.html`
|  |- Hero pool van lay tu `GET /api/wiki/heroes`; moi hero co `banPickScore` tu backend/database
|  `- Khong con nut `Đổi chế độ` / `Thoát` chi de quay ve route legacy `/ban-pick`; luong pick/ban, timer, reset giu nguyen, panel `Đánh giá đội hình` cap nhat realtime trong draft va lineup adjustment local
|- Ban/Pick Solo Online
|  |- Wrapper page: `demo/src/main/resources/static/html/ban-pick-solo.html`
|  |- Khong con nut quay ve landing page trong lobby/summary/status shell
|  |- Shareable room link di thang toi `/html/ban-pick-solo.html?room={roomCode}`
|  |- Lobby co sidebar/tab noi bo gom `Tìm trận`, `Thông tin cá nhân`, `Bảng xếp hạng`; tab mac dinh la `Tìm trận`
|  |- Tab `Tìm trận` giu nguyen create room, join room, chon BO1/BO3/BO5/BO7, cooldown message, va room feedback/error hien co
|  |- Player stats khong con page rieng; tab `Thông tin cá nhân` render `Player Card` nho tach rieng module stats 50 tran
|  |- `Player Card` chi hien avatar Google, IGN/display name, rank, ELO, badge, title; layout/logic dung chung `static/js/components/player-card.js` + `static/css/player-card.css`
|  |- Badge/title cua `Player Card` doc tu DB (`users.player_badge_*`, `users.player_title`) qua `GET /api/ban-pick/profile`; Solo page chi render, khong co UI chinh truc tiep
|  |- `rankCode`/`rankLabel` cua Player Card do backend tinh theo percentile rank tren pool `player_stats.rating`; frontend khong tu map rank theo nguong ELO
|  |- Stats Panel rieng hien W/L, win rate, tong tran, top hero pick, va 3 recent draft; khong tron vao Player Card
|  |- Tab `Bảng xếp hạng` render leaderboard truc tiep bang `GET /api/ban-pick/leaderboard`; page leaderboard rieng van duoc giu lai
|  |- Guest tren Solo page chi thay message gon yeu cau dang nhap de xem thong tin ca nhan; khong co menu/link noi bo tro toi `ban-pick-profile.html`
|  |- Hero pool van lay tu `GET /api/wiki/heroes`; moi hero co `banPickScore` tu backend/database
|  `- Cac nut nghiep vu room nhu join, ready, start, reset, next game, lineup confirm van giu nguyen; panel `Đánh giá đội hình` bi an trong suot draft/lineup adjustment va chi mo lai o man tong ket sau khi ca 2 doi confirm lineup cuoi
|- CSS ownership
|  |- `ban-pick-free.html`, `ban-pick-standard.html`, `ban-pick-solo.html`, `ban-pick-leaderboard.html` deu load `style.css` + `ban-pick.css`
|  |- `ban-pick-solo.html` va `account.html` load them `player-card.css` khi can render component Player Card dung chung
|  |- `style.css` giu token/base va mot so hero-pool primitive shared; `ban-pick.css` own draft board, pick/ban slots, team panel, status, solo-player-stats, solo-tab layout, va leaderboard layout
|  |- Cac page Ban/Pick dat body flag de `header-loader.js` bo qua shared footer, giu them khong gian thao tac draft; shared header van duoc inject nhu cu
|  `- `ban-pick-shell.html` tiep tuc la shell DOM chung; khong doi JS state machine, route API, hay DB room flow; `GET /api/ban-pick/profile` giu payload `playerCard` va nay lay badge/title tu DB thay vi fallback hardcode
|- Routing / Static Pages
|  |- `/ban-pick` redirect ve `/html/ban-pick-free.html` khi khong co query
|  |- `/ban-pick?mode=standard|solo|free` redirect den mode tuong ung
|  |- `/ban-pick?room={roomCode}`, `/ban-pick.html?room={roomCode}` va `/html/ban-pick.html?room={roomCode}` redirect vao thang Solo room
|  |- Khong con file static `ban-pick.html`; controller redirect legacy theo `room` / `mode` hoac ve Free Mode
|  `- Khong them API moi, khong doi DB, khong doi draft/room state machine
`- UI/UX Design notes
   |- Loai bo cac action chi dung de quay ve landing page cu, giu layout draft gon hon
   |- Them panel `draft-balance-panel` nho gon ngay duoi status, tranh tao them banner lon hay lam day top controls
   `- Chuyen mode thong qua navbar dropdown de dong nhat dieu huong desktop/mobile
```

**11. Community Tier List Pages**

```text
Community Tier List Pages
|- Feature name
|  `- Community Tier List static pages + header dropdown navigation
|- Purpose
|  |- Dua navigation Community Tier List len dropdown `Tier List` trong navbar chinh de dong nhat workflow
|  `- Giu `tier-list.html` lam trang goc: official meta o tren + toi da 6 community tier list noi bat o duoi
|- Related users
|  |- Public users xem `Tier List Meta`, `T?t c? Tier List`, `tier-list-detail`; guest co the mo modal tao tier list tam va luu draft trong `sessionStorage` cung tab
|  `- Authenticated users xem them `Tier list ban than`, dang community tier list len DB, luu bookmark/reference va tai anh theo logic cu
|- Main HTML, JavaScript, and CSS files
|  |- `demo/src/main/resources/static/html/header.html`
|  |- `demo/src/main/resources/static/html/tier-list.html`
|  |- `demo/src/main/resources/static/html/tier-list-community-shell.html`
|  |- `demo/src/main/resources/static/html/tier-list-all.html`
|  |- `demo/src/main/resources/static/html/tier-list-mine.html`
|  |- `demo/src/main/resources/static/html/tier-list-detail.html`
|  |- `demo/src/main/resources/static/js/tier-list-app.js`
|  |- `demo/src/main/resources/static/js/tier-list-community-page.js`
|  |- `demo/src/main/resources/static/js/tier-list-detail.js`
|  |- `demo/src/main/resources/static/js/tier-list-export.js`
|  |- `demo/src/main/resources/static/js/header-loader.js`
|  |- `demo/src/main/resources/static/css/style.css` (shared/base)
|  `- `demo/src/main/resources/static/css/tier-list.css` (official/community/detail/card UI)
|- CSS ownership
|  |- `style.css` giu token, shared card/button/modal/header va hero-pool selectors duoc tai su dung boi modal tao tier list
|  `- `tier-list.css` own official meta board, community grid/card, detail panel, rating/comment/download UI va guest draft notice trong modal
|  `- Community creator modal chi doi layout cho flow Tier List cong dong: desktop/tablet lon dung 2 cot board trai + hero list phai, mobile stack doc va giu scroll noi bo
|- Related controller, service, entity, and repository files
|  |- Controller: `demo/src/main/java/com/example/demo/controller/StaticPageRedirectController.java`
|  |- Controller: `demo/src/main/java/com/example/demo/controller/TierListController.java`
|  |- Service: `demo/src/main/java/com/example/demo/service/TierListCommunityService.java`
|  |- Entity: `demo/src/main/java/com/example/demo/entity/TierList.java`
|  `- Repository: `demo/src/main/java/com/example/demo/repository/TierListRepository.java`
|- Header / Navigation
|  |- Navbar `Tier List` doi tu link don thanh dropdown dung chung pattern voi `Ban/Pick`
|  |- Dropdown gom 3 diem vao: `/tier-list`, `/tier-list/all`, `/tier-list/mine`
|  |- Khong con item hay static page `tier-list-recommended.html` trong dropdown `Tier List`
|  |- Route cu `/tier-list/recommended` va cac URL HTML cu chi redirect an toan ve `tier-list.html`
|  |- Active nav `Tier List` dung tren `tier-list.html`, `tier-list-all.html`, `tier-list-mine.html`, `tier-list-detail.html`
|  `- Khong doi dropdown `Ban/Pick`, login/account menu, hay shared header auth flow
|- Tier List Official
|  |- `tier-list.html` van render full official meta board, download anh, title/subtitle va role columns DSL/JGL/MID/ADL/SUP
|  |- Section import tay `Import tu bang du lieu` da bi bo hoan toan; Admin chi con action `Luu Meta Chinh` de regenerate/save tu hero score backend
|  `- Ngay duoi official board van la section `Tier List Cong Dong`; neu official/detail/export co hien rating thi average/count tong van dung aggregate gom user + admin rating, con admin badge/detail van rieng
|- Community Tier List
|  |- Trang goc `tier-list.html` dung `GET /api/tier-lists/community` de render toi da 6 community tier list noi bat
|  |- Section tren trang goc chi hien community cards, khong hien official tier list trong danh sach nay
|  |- `tier-list.html` tiep tuc dung `GET /api/tier-lists/community` de render toi da 6 community highlight
|  |- `tier-list-all.html` van dung `GET /api/tier-lists/community/all`
|  |- Guest draft key `atg_guest_tier_list_draft` chi song trong `sessionStorage`; frontend chi luu title, note, contentData va `updatedAt`, reload cung tab se khoi phuc, dong tab/session moi thi mat; guest chi co 1 draft va bam `Tao Tier List` khi draft da ton tai thi frontend mo lai draft hien co thay vi tao draft thu 2
|  `- `tier-list-mine.html` vẫn dùng `GET /api/tier-lists/me`; guest thấy message đăng nhập, không crash; card/detail/rating summary đều hiện average/count tổng đã gồm admin rating, còn admin badge vẫn tách riêng; section `Tier List bạn tạo` hiện count dạng `n/5`
|- Routing / Static Pages
|  |- `/tier-list` redirect sang `/html/tier-list.html`
|  |- `/tier-list/recommended`, `/tier-list-recommended.html`, `/html/tier-list-recommended.html` deu redirect sang `/html/tier-list.html`
|  |- `/tier-list/all` redirect sang `/html/tier-list-all.html`
|  |- `/tier-list/mine` redirect sang `/html/tier-list-mine.html`
|  `- `tier-list-community-shell.html` chi con shell render title/subtitle + create button + grid, khong con body dropdown
|- API
|  |- Them `POST /api/admin/tier-lists/official/regenerate-from-hero-scores` cho Admin generate/save official meta tu hero score
|  |- `GET /api/tier-lists/official` van giu route cu, nhung neu DB chua co official row thi tra preview generate de trang khong bi trong
|  |- `GET /api/tier-lists/community/all` va `GET /api/tier-lists/me` khong doi
|  `- `POST /api/tier-lists` van la endpoint tao community tier list, nhung guest flow frontend khong goi endpoint nay; backend dem so tier list non-official hien co cua current user va reject create moi khi da dat 5/5; `PUT /api/tier-lists/{id}` de update item cu khong tinh la tao moi; Community/rating/comment/download APIs khong doi route hay permission, `adminRating/adminRatingDetail` van tach rieng
|- Database table or entity
|  |- `tier_lists`
|  |- `tier_list_ratings`
|  |- `tier_list_comments`
|  `- `tier_list_admin_ratings`; khong doi schema/entity/migration, `tier_lists.adminRating` van giu lam legacy fallback va chi duoc dung neu chua co row admin rating de tranh double-count
|- Main workflow
|  |- User mo `/tier-list` hoac `tier-list.html` -> frontend goi `GET /api/tier-lists/official` -> render official meta duoc luu trong DB hoac preview generate neu chua co official row -> sau do goi `GET /api/tier-lists/community` de render toi da 6 community card
|  |- Mo dropdown `Tier List` tren header -> chon `Tier List Meta` -> route `/tier-list` -> redirect sang `tier-list.html` -> frontend goi `GET /api/tier-lists/community` de render toi da 6 community card
|  |- Chọn `Tất cả Tier List` -> route `/tier-list/all` -> redirect sang `tier-list-all.html` -> frontend fetch `/api/tier-lists/community/all`
|  |- Chon `Tier List c?a b?n` -> route `/tier-list/mine` -> redirect sang `tier-list-mine.html` -> frontend fetch `/api/tier-lists/me`
|  |- Truy cap route cu `/tier-list/recommended` hoac URL HTML cu -> redirect sang `tier-list.html` -> frontend fetch `/api/tier-lists/community`
|  |- Guest bam `Tao Tier List` -> mo modal tao tier list nhu user thuong -> keo/tha/chinh title-note -> frontend autosave vao `sessionStorage` va reload cung tab se auto khoi phuc draft; neu draft guest da ton tai thi frontend tiep tuc mo lai draft do
|  |- Guest bấm `Đăng Tier List`, `Lưu` hoặc `Tải ảnh` -> frontend hiện toast yêu cầu đăng nhập và dừng lại trước khi gọi API hay export file
|  |- User đăng nhập bấm `Tạo Tier List` hoặc `Đăng Tier List` -> frontend gọi `/api/users/me/content-summary` để đọc `tierListCount`; nếu đã đạt 5/5 thì cảnh báo ngay và không đi tiếp flow tạo mới
|  |- User đăng nhập mở lại cùng modal -> có thể dùng tiếp guest draft đã restore nếu chưa vượt quota; nếu `POST /api/tier-lists` thành công thì frontend clear session draft để tránh trùng
|  |- Admin sua `banPickScore` trong `admin-heroes.html` -> quay lai `tier-list.html` -> bam `Luu Meta Chinh` -> backend generate official contentData tu `heroes.ban_pick_score` va save vao `tier_lists.contentData`
|  |- `tier-list-community-page.js` load shell chung, doc `body[data-community-view]`, set title/subtitle, roi nap `tier-list-app.js`
|  `- `tier-list-app.js` tai su dung renderer card/rating/delete/modal tao tier list; official page va 2 page community dung chung renderer card, con `tier-list-detail.js` dong bo lai average/count tong ngay sau khi Admin update rating
|- Access permissions
|  |- Public xem official, all, detail, rating summary; guest duoc mo modal tao tier list tam, nhung khong duoc luu/publish/export va se nhan login prompt khi bam cac nut persistent
|  |- `mine` yêu cầu user đã đăng nhập; guest sẽ thấy message `Vui lòng đăng nhập để xem Tier List của bạn.`
|  `- Logged-in user duoc tao/lưu toi da 5 Community Tier List non-official; update tier list cu va official/admin tier list khong bi ap quota nay; quyen xoa tier list van theo owner/Admin nhu truoc
`- Risk notes or manual testing areas
   |- Can test official board boundary `9`, `7.5`, `5`, `2.5`, score null, va case hero thieu `Primary Role`
   |- Can test `tier-list.html` khong con import textarea/nut `Xem truoc`/nut `Ap dung vao Tier List`, nhung van render official meta + toi da 6 community card
   |- Can test case `user rating 4 + admin rating 3` de card/detail/summary hien average `3.5` va count `2`
   |- Can test modal `Tao Tier List` tren official page va 2 page community de chac rang hero pool community van load dung, guest notice hien dung va `sessionStorage` loi/quota khong lam crash UI
   `- Cần test guest reload cùng tab khôi phục draft, đóng tab/session mới mất draft, không xuất hiện row mới trong `tier_lists`, không vào Community list/Account dashboard và không phát sinh API save/publish/export khi chưa đăng nhập

**11B. Community Creator Layout**

Community Creator Layout
|- Feature name
|  `- Community Tier List Creator Modal Layout
|- Scope
|  `- Chi ap dung cho modal tao/sua Community Tier List tren `tier-list.html`, `tier-list-all.html`, va `tier-list-mine.html`; khong doi official meta board
|- Main HTML, JavaScript, and CSS files
|  |- `demo/src/main/resources/static/html/tier-list.html`
|  |- `demo/src/main/resources/static/html/tier-list-community-shell.html`
|  |- `demo/src/main/resources/static/html/tier-list-mine-shell.html`
|  `- `demo/src/main/resources/static/css/tier-list.css`
|- Main workflow
|  |- Desktop/tablet lon: modal creator dung layout ngang 2 cot voi board trai va panel `Danh sach tuong` phai
|  |- Hero panel phai giu search, role filter, hero grid scroll rieng, va van la source drag/drop cho board
|  |- Footer `Huy` / `Dang Tier List` nam ngoai vung body scroll de luon de thay khi thao tac
|  `- Mobile: modal creator stack doc board tren / hero list duoi, khong tran ngang viewport
`- Risk notes or manual testing areas
   |- Can test search, role filter, drag tu hero list sang tier, drag giua cac tier, va drag tra ve list van hoat dong sau khi doi layout
   `- Can test mobile width de chac rang hero grid co scroll rieng va footer action van thay duoc

**12. Tier List Saved Reference**

Tier List Saved Reference
|- Community Tier List
|  |- User đăng nhập có thể `Lưu` Community Tier List theo kiểu bookmark/reference, không clone và không snapshot
|  |- Saved item luon render du lieu live tu `tier_list_id` goc: title, `contentData`, author, `createdAt`, aggregate average/count tong da gom user + admin rating, admin badge/detail rieng, comment count
|  |- Neu tier list goc thay doi noi dung, review, rating hoac admin rating thi danh sach da luu tu dong phan anh thay doi moi
|  |- `tier-list.html`, `tier-list-all.html`, `tier-list-mine.html` và `tier-list-detail.html` đều có thể hiện state `Lưu` / `Bỏ lưu`
|  `- `tier-list-mine.html` được tách 2 section rõ nghĩa: `Tier List bạn tạo` và `Tier List đã lưu`
|- Authentication / User Profile
|  |- `POST /api/tier-lists/{id}/save`, `DELETE /api/tier-lists/{id}/save`, `GET /api/tier-lists/saved` đều yêu cầu đăng nhập
|  |- Guest bấm `Lưu`, `Đăng Tier List` hoặc `Tải ảnh` thì frontend hiện message đăng nhập, không crash; guest draft chỉ nằm trong `sessionStorage` key `atg_guest_tier_list_draft`
|  |- `/api/tier-lists/me` tiep tuc chi co nghia la tier list user tu tao, khong tron voi danh sach da luu
|  `- Guest vào `tier-list-mine.html` vẫn không crash; shell hiện cảnh báo đăng nhập thay vì render lỗi
|- API
|  |- Them `GET /api/tier-lists/saved` -> tra danh sach tier list da luu cua current user theo response shape card hien co
|  |- Them `POST /api/tier-lists/{id}/save` -> tao saved relation neu chua co, tra `saved=true` va item render tu tier list goc
|  |- Them `DELETE /api/tier-lists/{id}/save` -> xoa saved relation cua current user, khong xoa tier list goc
|  |- Payload tier list community/detail bo sung `saved`, `isSavedByCurrentUser`, `savedAt`
|  `- Route id-based trong `TierListController` duoc rang buoc dang so de `/saved` khong va cham voi route detail `/{id}`
|- Database / Entity / Migration
|  |- Them entity + table `user_saved_tier_lists`
|  |- Cot: `id`, `user_id`, `tier_list_id`, `saved_at`
|  |- Unique constraint: `(user_id, tier_list_id)`
|  |- Khong doi schema cua `tier_lists`; khong tao ban sao moi trong bang nay khi user bam `Luu`
|  `- Neu du an dang chay JPA `ddl-auto=update` thi khong can migration thu cong cho thay doi nho nay
|- Routing / Static Pages
|  |- `tier-list-mine.html` nay load shell rieng de render 2 section `toi tao` + `da luu`
|  |- `tier-list-community-page.js` chon shell theo `body[data-community-view]`: all dung shell chung, mine dung shell rieng
|  |- `tier-list-detail.html` bổ sung action `Lưu` / `Bỏ lưu` bên cạnh `Tải ảnh`
|  `- Khong doi route dep `/tier-list`, `/tier-list/all`, `/tier-list/mine`; route cu `/tier-list/recommended` chi con vai tro redirect
|- Bảng tổng hợp chức năng Tier List
|  |- Meta chinh thuc: khong doi logic import/export/admin save
|  |- Community tier list: bo sung bookmark/reference theo user, card van giu metadata goc cua owner va hien aggregate average/count tong da gom admin rating
|  `- Tier List của bạn: gồm 2 nguồn dữ liệu tách biệt `GET /me` (tôi tạo) và `GET /saved` (tôi đã lưu)
|- Bảng API
|  |- `GET /api/tier-lists/me` -> van la danh sach tier list current user tu tao; average/count display tong da gom admin rating
|  |- `GET /api/tier-lists/saved` -> danh sach tier list current user da luu; average/count display tong da gom admin rating
|  |- `POST /api/tier-lists/{id}/save` -> save relation, khong clone tier list
|  `- `DELETE /api/tier-lists/{id}/save` -> unsave relation, khong xoa tier list goc
|- Bảng Database
|  |- `tier_lists`: khong doi owner, `createdAt`, `contentData`, rating khi user khac bam `Luu`
|  |- `user_saved_tier_lists`: bang relation moi giua user va tier list goc
|  `- `tier_list_ratings` / `tier_list_admin_ratings` van la du lieu danh gia cua tier list goc, saved list doc lai live data tu day
`- Risk notes or manual testing areas
   |- Can test user A luu tier list cua user B, sau do user B sua noi dung/rating/admin rating de xac nhan saved list cua A tu dong thay doi
   |- Can test save duplicate khong tao them dong moi trong `tier_lists` va cung khong tao duplicate trong `user_saved_tier_lists`
   |- Cần test guest `POST /api/tier-lists/{id}/save` nhận 401 và guest `GET /api/tier-lists/saved` nhận 401; frontend không chủ động gọi các API này khi chưa đăng nhập
   `- Cần test section `Tier List đã lưu` trên `tier-list-mine.html` xóa item ngay sau thao tác `Bỏ lưu` mà không ảnh hưởng Tier List gốc
```

**13. Ban/Pick Hero Score & Win Rate**

```text
Ban/Pick Hero Score & Win Rate
|- Feature name
|  `- Ban/Pick Hero Score & Win Rate
|- Purpose
|  |- Luu diem Ban/Pick cho tung hero trong database thay vi hard-code frontend
|  |- Cho Admin xem/sua diem nay trong Admin Hero panel
|  `- Dung cung nguon du lieu backend de tinh tong diem doi hinh va ty le thang du doan trong Ban/Pick
|- Related users
|  |- Public users dung Ban/Pick Free Mode
|  |- Public users dung Ban/Pick Standard Mode
|  |- Authenticated users dung Ban/Pick Solo Online
|  `- Admin users quan ly hero wiki va cap nhat diem Ban/Pick
|- Main HTML, JavaScript, and CSS files
|  |- `demo/src/main/resources/static/html/admin-heroes.html`
|  |- `demo/src/main/resources/static/html/ban-pick-shell.html`
|  |- `demo/src/main/resources/static/html/ban-pick-standard.html`
|  |- `demo/src/main/resources/static/html/ban-pick-free.html`
|  |- `demo/src/main/resources/static/html/ban-pick-solo.html`
|  |- `demo/src/main/resources/static/js/admin-heroes.js`
|  |- `demo/src/main/resources/static/js/ban-pick-page.js`
|  |- `demo/src/main/resources/static/js/ban-pick.js`
|  |- `demo/src/main/resources/static/css/style.css` (shared/base)
|  |- `demo/src/main/resources/static/css/ban-pick.css`
|  `- `demo/src/main/resources/static/css/admin.css`
|- Related controller, service, entity, repository, and seed files
|  |- Controller: `demo/src/main/java/com/example/demo/controller/AdminWikiHeroController.java`
|  |- Controller: `demo/src/main/java/com/example/demo/controller/WikiController.java`
|  |- Service: `demo/src/main/java/com/example/demo/service/AdminWikiHeroService.java`
|  |- Service: `demo/src/main/java/com/example/demo/service/WikiService.java`
|  |- Entity: `demo/src/main/java/com/example/demo/entity/Hero.java`
|  |- Repository: `demo/src/main/java/com/example/demo/repository/HeroRepository.java`
|  |- Seeder: `demo/src/main/java/com/example/demo/component/HeroBanPickScoreSeeder.java`
|  |- Seed data: `demo/src/main/resources/seed/hero-ban-pick-scores.txt`
|  `- Manual SQL: `demo/sql/add_ban_pick_score_to_heroes.sql`, `demo/sql/wiki_hero_schema.sql`
|- API endpoints
|  |- `GET /api/wiki/heroes` -> `HeroSummaryDto` bo sung `banPickScore` cho hero pool frontend
|  |- `GET /api/admin/wiki/heroes`
|  |- `PUT /api/admin/wiki/heroes/{id}` -> request/response DTO bo sung `banPickScore`
|  `- Khong them endpoint Ban/Pick moi; frontend tai score qua hero catalog backend co san
|- Database / Entity / Migration
|  |- Them field `banPickScore` trong entity `Hero`
|  |- Cot DB: `heroes.ban_pick_score DECIMAL(5,2) NULL`
|  |- Fallback an toan: neu score null/khong hop le thi frontend/backend deu quy ve `0`
|  |- Startup seeder chi backfill hero co score null; khong de ghi de diem Admin da sua
|  `- Co SQL script de add cot + backfill score ban dau theo slug/alias ma khong lam mat du lieu hero hien co
|- Admin Hero
|  |- Bang hero admin hien them cot `Ban/Pick`
|  |- Modal sua hero co input `banPickScore` voi helper text va inline error
|  |- Validate: bat buoc la number, min `0`, max `10`, toi da `2` chu so thap phan
|  |- Backend `AdminWikiHeroService` xac thuc lai validation truoc khi persist vao DB
|  `- Chi Admin moi goi duoc API `/api/admin/**`; user thuong khong sua duoc score
|- Ban/Pick Free Mode
|  |- `ban-pick.js` tai hero pool tu `/api/wiki/heroes` va gan `banPickScore` vao moi hero
|  |- Helper `getHeroBanPickScore(hero)` uu tien doc `hero.banPickScore`, fallback `0`
|  |- Helper `calculateTeamBanPickScore(picks)` chi cong cac hero da pick, bo qua slot rong/null va object co `actionType=BAN`
|  `- UI `Đánh giá đội hình` cap nhat realtime khi pick, huy chon, reset
|- Ban/Pick Standard Mode
|  |- Dung cung helper `getHeroBanPickScore`, `calculateTeamBanPickScore`, `calculateBanPickWinRate`
|  |- Ty le thang tinh theo cong thuc `teamScore / (blueScore + redScore) * 100`
|  |- Neu tong diem bang `0` thi fallback `50/50`, khong hien `NaN/Infinity`
|  `- Khong doi phase order, timer, confirm, search, filter role hay logic ban/pick hien co
|- Ban/Pick Solo Online
|  |- Frontend nhan room state nhu cu, nhung phan score/win-rate duoc tinh local tu danh sach pick hien tai va hero pool backend
|  |- Khi room state thay doi, render draft chay lai va cap nhat panel `Đánh giá đội hình`
|  |- Reset room, huy preview, next game hoac load room tu link deu dung chung fallback score `0`
|  `- Khong doi room API, websocket event, hay state machine `BanPickRoomService`
|- UI/UX Design System
|  |- Them component chia se `draft-balance-panel` nho gon ngay duoi status
|  |- Hien 2 dong tong ket: `Xanh: [diem] · [%]` va `Đỏ: [diem] · [%]`
|  |- Style uu tien compact, khong tao banner lon, khong lam day top status/control
|  |- `ban-pick.css` own panel/board/page-specific layout; `admin.css` own admin hero table/modal; `style.css` chi giu base/shared token
|  `- Responsive cho desktop/mobile, cho phep wrap nhe khi khong du ngang
|- Main workflow
|  |- App boot -> `HeroBanPickScoreSeeder` doc `hero-ban-pick-scores.txt` -> map hero theo name/slug/alias -> backfill `heroes.ban_pick_score` neu dang null
|  |- Admin mo `admin-heroes.html` -> xem cot Ban/Pick -> sua trong modal -> client validate -> `PUT /api/admin/wiki/heroes/{id}` -> backend validate -> save DB
|  |- Ban/Pick page load shell -> `ban-pick.js` fetch `/api/wiki/heroes` -> hero pool co `banPickScore`
|  |- User pick hero -> `renderDraft()` goi `renderBanPickEvaluation()` -> cong diem tung doi -> tinh win-rate -> cap nhat panel compact
|  `- Huy chon, reset, room sync, next game deu di qua cung renderer nen score/win-rate tu dong cap nhat lai
|- Access permissions
|  |- Public/guest duoc xem score/win-rate trong Ban/Pick page
|  |- Solo room APIs van theo auth/quyen hien co
|  `- Chi Admin duoc sua score hero qua `Admin Hero`
`- Risk notes or manual testing areas
   |- Cần verify alias seed cho hero tên Việt hóa/accent như `Triệu Vân`, `Ngộ Không`, `Điêu Thuyền` và tên variant như `Riktor`
   |- Can test hero khong co score hoac score null van hien `0`, khong crash frontend
   |- Can test Admin sua `10`, `9.84`, `0`, input sai dinh dang, va case reload sau save
   |- Can test Free/Standard/Solo deu cap nhat dung khi pick, huy preview, reset, next game, load room
   `- Can test layout mobile/desktop de panel moi khong che bottom action bar va khong lam day top status
```

**14. Wiki Gameplay Data - Spells**

```text
Wiki Gameplay Data - Spells
|- Feature name
|  `- Wiki Gameplay Data - Spells
|- Purpose
|  |- Chuẩn hóa module `Bổ trợ` theo internal naming `spells`, nhưng dữ liệu runtime dùng JSON thay vì DB
|  |- Cấp 1 nguồn dữ liệu chung cho Wiki tab `Bổ trợ`, admin Wiki Data, và form `Tạo giáo án`
|  `- Giữ UI tiếng Việt là `Bổ trợ`, không dùng tên `talents`
|- Related users
|  |- Public users xem Wiki `Bổ trợ`
|  |- Public users dùng `create-guide.html` để chọn bổ trợ
|  `- Admins quản lý dữ liệu qua JSON/API naming `spells`
|- Main HTML, JavaScript, and CSS files
|  |- `demo/src/main/resources/static/html/wiki.html`
|  |- `demo/src/main/resources/static/html/create-guide.html`
|  |- `demo/src/main/resources/static/html/admin-wiki-data.html`
|  |- `demo/src/main/resources/static/js/admin-wiki-data.js`
|  `- `demo/src/main/resources/static/css/wiki.css`
|- Related controller, service, dto, and storage files
|  |- Controller: `demo/src/main/java/com/example/demo/controller/SpellController.java`
|  |- Controller: `demo/src/main/java/com/example/demo/controller/AdminSpellController.java`
|  |- Service: `demo/src/main/java/com/example/demo/service/SpellService.java`
|  |- Storage: `demo/src/main/java/com/example/demo/service/WikiJsonStorageService.java`
|  `- DTO: `demo/src/main/java/com/example/demo/dto/wiki/SpellDto.java`
|- API
|  |- `GET /api/spells` -> trả danh sách 12 bổ trợ từ JSON với `slug`, `name`, `iconUrl`, `description`
|  |- `GET /api/spells/{slug}` -> trả chi tiết 1 bổ trợ theo slug chuẩn hóa
|  |- `GET /api/admin/spells`
|  |- `POST /api/admin/spells`
|  |- `PUT /api/admin/spells/{slug}`
|  `- `DELETE /api/admin/spells/{slug}`; `GET /api/spells/**` vẫn là public trong `SecurityConfig`
|- Data / Migration
|  |- Runtime source: `demo/src/main/resources/static/data/spells.json`
|  |- `WikiJsonStorageService` ưu tiên external directory qua `atg.data.dir`; nếu không có thì fallback bundled JSON/classpath và ghi local source tree khi chạy dev
|  |- Không còn entity/repository DB cho `spells`; `Spell.java` và `SpellRepository.java` đã bị loại khỏi runtime
|  |- `demo/src/main/resources/schema.sql` và `data.sql` cũ cho spells đã bị vô hiệu hóa để tránh seed/truy cập DB runtime
|  `- Có script cleanup riêng `demo/sql/drop_spells_table.sql` nếu cần drop bảng legacy sau migration
|- Static assets
|  |- Asset runtime dùng convention sẵn có của project: `demo/src/main/resources/static/images/spells/`
|  |- Icon path public: `/images/spells/{slug}.png`
|  `- JSON hiện map đủ 12 icon thật: `boc-pha.png`, `cap-cuu.png`, `gam-thet.png`, `la-chan-sinh-menh.png`, `ngat-ngu.png`, `suy-nhuoc.png`, `thanh-tay.png`, `toc-bien.png`, `toc-hanh.png`, `trung-tri.png`, `tu-bao-bon.png`, `vien-binh-lien-hiep.png`
|- Main workflow
|  |- User mở `wiki.html?tab=spells` -> frontend fetch `GET /api/spells` -> render grid card gồm icon + tên bổ trợ
|  |- User mở `create-guide.html` -> spell pool fetch `GET /api/spells` -> render option icon + name -> lưu `spell`, `spellSlug`, `spellIconUrl` vào `contentData`
|  |- Admin mở `admin-wiki-data.html` -> CRUD qua `/api/admin/spells` -> backend validate `slug/name/iconUrl` -> ghi JSON an toàn
|  `- Nếu file JSON external thiếu hoặc lỗi, service fallback bundled JSON hoặc trả danh sách rỗng thay vì crash app
|- Access permissions
|  |- Public/guest được xem `GET /api/spells` và `GET /api/spells/{slug}`
|  |- `/api/admin/spells/**` yêu cầu role `ADMIN`
|  `- Luồng `POST /api/guides` vẫn yêu cầu đăng nhập như trước
`- Risk notes or manual testing areas
   |- Cần test `spells.json` tồn tại và đủ 12 item
   |- Cần test `GET /api/spells` và `GET /api/admin/spells` đều đọc cùng nguồn JSON
   |- Cần test `wiki.html?tab=spells` render đủ 12 card, icon không 404, mobile/desktop layout ổn
   `- Cần test `create-guide.html` vẫn chọn/lưu được bổ trợ và delete admin bị chặn nếu `spellSlug` còn đang được guide tham chiếu
```

**15. Wiki Gameplay Data - Enchantments**

```text
Wiki Gameplay Data - Enchantments
|- Feature name
|  `- Wiki Gameplay Data - Enchantments
|- Purpose
|  |- Tách `Phù hiệu` thành module riêng với internal naming `enchantments`, nhưng runtime dùng JSON thay vì DB
|  |- Cấp nguồn dữ liệu chung cho Wiki tab `Phù hiệu` và admin Wiki Data
|  `- Giữ asset path ổn định theo `/images/enchantments/{category-folder}/{file-name}`
|- Related users
|  |- Public users xem Wiki `Phù hiệu`
|  |- Admins quản lý dữ liệu qua JSON/API naming `enchantments`
|  `- Guide authors chưa bị đổi flow `create-guide.html`; picker Phù hiệu vẫn là placeholder visual trong task này
|- Main HTML, JavaScript, and CSS files
|  |- `demo/src/main/resources/static/html/wiki.html`
|  |- `demo/src/main/resources/static/html/header.html`
|  |- `demo/src/main/resources/static/html/admin-wiki-data.html`
|  |- `demo/src/main/resources/static/js/admin-wiki-data.js`
|  `- `demo/src/main/resources/static/css/wiki.css`
|- Related controller, service, dto, and storage files
|  |- Controller: `demo/src/main/java/com/example/demo/controller/EnchantmentController.java`
|  |- Controller: `demo/src/main/java/com/example/demo/controller/AdminEnchantmentController.java`
|  |- Service: `demo/src/main/java/com/example/demo/service/EnchantmentService.java`
|  |- Storage: `demo/src/main/java/com/example/demo/service/WikiJsonStorageService.java`
|  `- DTO: `demo/src/main/java/com/example/demo/dto/wiki/EnchantmentDto.java`
|- API
|  |- `GET /api/enchantments` -> trả danh sách Phù hiệu với `slug`, `name`, `branch`, `branchName`, `level`, `iconUrl`, `description`, `branchIconUrl`
|  |- `GET /api/enchantments/{slug}` -> trả chi tiết 1 Phù hiệu theo slug chuẩn hóa
|  |- `GET /api/admin/enchantments`
|  |- `POST /api/admin/enchantments`
|  |- `PUT /api/admin/enchantments/{slug}`
|  `- `DELETE /api/admin/enchantments/{slug}`; `GET /api/enchantments/**` vẫn là public trong `SecurityConfig`
|- Data / Migration
|  |- Runtime source: `demo/src/main/resources/static/data/enchantments.json`
|  |- Không còn entity/repository/catalog/seeder DB cho enchantments; `Enchantment.java`, `EnchantmentRepository.java`, `EnchantmentCatalog.java`, `EnchantmentDataSeeder.java`, `seed/enchantments.txt` đã bị loại khỏi runtime
|  |- JSON ghi an toàn qua `WikiJsonStorageService`, cùng cơ chế `atg.data.dir` như spells
|  |- Legacy `phu_hieu` và `huong_dan_phu_hieu` có thể vẫn còn trong DB cũ; runtime hiện không đọc các bảng này nữa
|  `- Có script cleanup riêng `demo/sql/drop_phu_hieu_tables_after_guide_migration.sql` để drop sau khi xác nhận không còn cần migration guide legacy
|- Static assets
|  |- Asset root: `demo/src/main/resources/static/images/enchantments/`
|  |- 4 folder con: `thanh-khoi-nguyen`, `thap-quang-minh`, `vuc-hon-mang`, `rung-nguyen-sinh`
|  |- JSON hiện đọc 31 icon thật từ 4 folder con; 4 file cùng tên folder được dùng làm `branch icon` cho section header của Wiki
|  `- Không đổi tên file; slug metadata được normalize nhưng `iconUrl` vẫn map đúng file thật
|- Main workflow
|  |- User mở `wiki.html?tab=enchantments` -> frontend fetch `GET /api/enchantments` -> render group theo 4 nhánh với branch icon + card grid
|  |- Header dropdown `Wiki` có shortcut `Tướng`, `Bổ trợ`, `Phù hiệu` để deep-link đúng tab
|  |- Admin mở `admin-wiki-data.html` -> CRUD qua `/api/admin/enchantments` -> backend validate `slug/name/branch/iconUrl` -> ghi JSON an toàn
|  `- `create-guide.html` giữ nguyên flow hiện tại; chưa chuyển khung Phù hiệu placeholder sang API picker trong task này
|- Access permissions
|  |- Public/guest được xem `GET /api/enchantments` và `GET /api/enchantments/{slug}`
|  |- `/api/admin/enchantments/**` yêu cầu role `ADMIN`
|  `- Nếu không kiểm tra được legacy reference DB thì admin UI hiển thị cảnh báo trước khi xóa
`- Risk notes or manual testing areas
   |- Cần verify `enchantments.json` đọc đủ dữ liệu từ 4 folder con và icon không 404
   |- Cần test `wiki.html?tab=enchantments` không làm hỏng tab `heroes` và `spells`
   |- Cần test `/api/admin/enchantments` thêm/sửa/xóa cập nhật JSON đúng và giữ `branchName/level/iconUrl`
   `- Cần test dropdown `Wiki` active đúng khi mở `wiki.html?tab=enchantments`
```

**17. Admin Esports Import DB-First AER Ranking**

```text
Admin Esports Import DB-First AER Ranking
|- Feature name
|  `- Admin Esports Import DB-First AER Ranking
|- Purpose
|  |- Sau khi Admin confirm import Excel/CSV vào DB, hệ thống cập nhật ranking AER/Elo trực tiếp từ DB trong cùng workflow
|  |- Read source chinh tu `esports_matches`, `esports_game_drafts`, va lay tier chuan tu `esports_tournaments.aer_tier`
|  |- UI chi hien status summary cho import, series bi anh huong, va trang thai recalculate ranking
|  `- Khong tao AER JSON/file trung gian rieng, khong preview/download file rieng, va REST API van tra payload JSON binh thuong cho web app
|- Related users
|  `- Admin users tren `admin-esports-data.html`
|- Main HTML, JavaScript, and CSS files
|  |- `demo/src/main/resources/static/html/admin-esports-data.html`
|  |- `demo/src/main/resources/static/js/admin-esports-data.js`
|  `- `demo/src/main/resources/static/css/admin.css`
|- Related controller, service, dto, entity, and repository files
|  |- Controller: `demo/src/main/java/com/example/demo/controller/EsportsAdminController.java`
|  |- Service: `demo/src/main/java/com/example/demo/service/EsportsDraftService.java`
|  |- Service reused: `demo/src/main/java/com/example/demo/service/EloCalculationService.java`
|  |- DTO: `demo/src/main/java/com/example/demo/dto/esports/EsportsGameDraftImportConfirmResponse.java`
|  |- Repository: `demo/src/main/java/com/example/demo/repository/EsportsMatchRepository.java`
|  |- Repository: `demo/src/main/java/com/example/demo/repository/EsportsGameDraftRepository.java`
|  |- Entity reused: `demo/src/main/java/com/example/demo/entity/EsportsMatch.java`
|  `- Entity reused: `demo/src/main/java/com/example/demo/entity/EsportsGameDraft.java`
|- API endpoints
|  `- `POST /api/admin/esports/game-drafts/import/confirm`
|- Request / response behavior
|  |- Confirm import success: tra `EsportsGameDraftImportConfirmResponse` gom `importedRows`, `createdMatches`, `updatedMatches`, `createdDrafts`, `overwrittenDrafts`, `affectedMatchIds`, `affectedSeriesCount`, `rankingsRecalculated`
|  `- Khong con endpoint utility rieng cho export du lieu trung gian; API khac cua he thong van tra JSON nhu binh thuong
|- Database table or entity
|  |- Write source chinh: `esports_matches`
|  |- Write source chinh: `esports_game_drafts`
|  |- Read-only field tier chuan: `esports_tournaments.aer_tier`
|  |- Public/Admin ranking tiep tuc doc du lieu tu DB/API
|  |- Khong tao bang moi
|  `- Khong doi schema DB trong task nay
|- Main workflow
|  |- Import Excel/CSV/XLSX vao DB van di theo flow rieng `upload file -> preview import -> ap dung vao DB`
|  |- Import confirm se group game theo series cha, create/update `esports_matches`, va create/overwrite `esports_game_drafts`
|  |- Neu series cha bi tao moi hoac doi score/tournament link thi `EloCalculationService.calculateAllRankings()` chay lai tu DB
|  |- Frontend hien imported rows, created matches/drafts, overwritten drafts, `affectedSeriesCount`, `affectedMatchIds`, va `rankingsRecalculated`
|  |- Public dashboard/ranking tiep tuc doc du lieu tu DB/API hien co
|  `- Khong co buoc tao file trung gian, khong co preview/download file rieng, va khong phuc hoi luong file -> projection rieng
|- Access permissions
|  `- `/api/admin/esports/game-drafts/import/confirm` can role `ADMIN`
`- Risk notes or manual testing areas
   |- Can test import moi tao series cha + draft moi va verify ranking duoc recalculate khi score series thay doi
   |- Can test import vao parent da co san nhung khong doi score de dam bao `rankingsRecalculated=false`
   |- Can test public `/esports/data` va export CSV van doc du lieu DB dung sau import
   `- Can test reset xong import lai de verify workflow DB-first van hoat dong on dinh
```

**18. Exact Duplicate Esports Series Cleanup**

```text
Exact Duplicate Esports Series Cleanup
|- Feature name
|  `- Exact Duplicate Esports Series Cleanup
|- Purpose
|  |- Remove 8 exact duplicate `esports_matches` parent rows left by a previous import run after exact parent reuse was fixed
|  `- Keep canonical old parents and leave the 12 stage-conflict rows for a separate task
|- Related files
|  |- `demo/sql/cleanup_8_exact_duplicate_esports_series.sql`
|  `- `demo/sql/backups/aov_tactics_before_cleanup_8_exact_duplicate_series_*.sql`
|- Database / Entity / Migration
|  |- Data-only cleanup in `esports_matches` and `esports_game_drafts`
|  |- Canonical mapping: `1682->1618`, `1683->1623`, `1684->1627`, `1685->1631`, `1686->1635`, `1687->1639`, `1688->1644`, `1689->1648`
|  |- Safe rule: delete duplicate child drafts only when the canonical parent already has the same `game_number` and the draft payload matches exactly; then delete the orphan duplicate parent
|  |- No schema change, no entity change, no code change to Import Excel/CSV/XLSX, ranking recalculation logic, or Export CSV
|  `- Explicitly excludes stage-conflict ids `1674..1681` and `1690..1693`
|- Main workflow
|  |- Backup local DB with `mysqldump` before any mutation
|  |- Run pre-cleanup audit for the 16 scoped ids and build temp tables to mark `safe_to_cleanup` pairs only
|  |- Delete duplicate draft rows and duplicate parent rows inside a transaction
|  `- Verify `total_games`, `total_series`, duplicate `(match_id, game_number)`, score mismatch via `winner_team_id`, and that ids `1682..1689` are gone
`- Risk notes or manual testing areas
   |- If any duplicate parent is missing a canonical `game_number` or has draft payload mismatch, leave that pair blocked instead of partially deleting it
   `- The 12 stage-conflict ids still need their own cleanup workflow outside Task 6B
```

**19. Admin Esports Reset Data**

```text
Admin Esports Reset Data
|- Feature name
|  `- Admin Esports Reset Data
|- Purpose
|  |- Thêm Danger Zone riêng cho admin để xóa sạch dataset import esports và cho phép import lại XLSX/CSV từ đầu
|  |- Chi xoa du lieu import trong `esports_matches` va `esports_game_drafts`, khong xoa toan bo database
|  `- Bat buoc backup DB + confirmation text truoc khi reset, va chan production profile neu khong co flag ro rang
|- Related users
|  `- Admin users tren `admin-esports-data.html`
|- Main HTML, JavaScript, and CSS files
|  |- `demo/src/main/resources/static/html/admin-esports-data.html`
|  |- `demo/src/main/resources/static/js/admin-esports-data.js`
|  |- `demo/src/main/resources/static/css/admin.css`
|  `- `demo/src/main/resources/static/html/admin.html` (legacy endpoint reset cu da bi khoa o backend va nut reset legacy da duoc go khoi UI; admin phai dung trang `admin-esports-data.html` cho flow reset moi)
|- Related controller, service, dto, entity, and repository files
|  |- Controller: `demo/src/main/java/com/example/demo/controller/EsportsAdminController.java`
|  |- Service: `demo/src/main/java/com/example/demo/service/EsportsAdminMaintenanceService.java`
|  |- Service: `demo/src/main/java/com/example/demo/service/EsportsDatabaseBackupService.java`
|  |- Service: `demo/src/main/java/com/example/demo/service/MySqlEsportsDatabaseBackupService.java`
|  |- DTO: `demo/src/main/java/com/example/demo/dto/esports/EsportsResetDataRequest.java`
|  |- DTO: `demo/src/main/java/com/example/demo/dto/esports/EsportsResetDataResponse.java`
|  |- Repository: `demo/src/main/java/com/example/demo/repository/EsportsGameDraftRepository.java`
|  |- Repository: `demo/src/main/java/com/example/demo/repository/EsportsMatchRepository.java`
|  `- Repository: `demo/src/main/java/com/example/demo/repository/EsportsTeamRepository.java`
|- API endpoints
|  |- `POST /api/admin/esports/reset-data`
|  `- Legacy `DELETE /api/admin/esports/matches` khong con cho reset truc tiep; endpoint nay tra loi huong admin sang flow moi co confirmation
|- Request / response behavior
|  |- Request body: `{"confirmationText":"RESET ESPORTS DATA","backupBeforeReset":true}`
|  |- Sai `confirmationText` hoặc `backupBeforeReset=false` -> `400`, không xóa gì
|  |- Backup fail -> `500`, reset bi huy va khong xoa gi
|  |- Production profile khong co `app.admin.esports-reset.allow-production=true` -> `403`
|  `- Success response tra `reset`, `backupFile`, `deletedGameDrafts`, `deletedMatches`, `deletedPlayerStats`, `remainingGameDrafts`, `remainingMatches`, `playerStatsCleared`, `playerStatsRetainedReason`
|- Database table or entity
|  |- Delete runtime data: `esports_game_drafts`
|  |- Delete runtime data: `esports_matches`
|  |- Preserve rows: `esports_teams`, `esports_tournaments`, `esports_franchises`, `esports_tournament_teams`, `heroes`, `users`, `guides`, `tier_lists`, `meta_tier_lists`, `ban_pick_rooms`, `ban_pick_actions`, `draft_histories`
|  |- `player_stats` duoc audit la Ban/Pick leaderboard theo user, khong derived tu `esports_matches`, nen khong bi xoa trong reset nay
|  |- `esports_teams` khong bi delete, nhung cac field ranking/stat derived (`score`, `game_wins`, `game_losses`, `match_wins`, `match_losses`) duoc reset ve baseline `1200/0/0/0/0`
|  `- Backup file runtime: `demo/sql/backups/aov_tactics_before_reset_esports_data_YYYYMMDD_HHMMSS.sql`
|- Main workflow
|  |- Admin mo `admin-esports-data.html` -> xuong section `Danger Zone`
|  |- Doc canh bao rang reset chi xoa `esports_matches` + `esports_game_drafts`, khong xoa teams/heroes/tournaments, va sau do phai import lai XLSX/CSV
|  |- Bam `Reset Esports Data` -> frontend prompt admin nhap chinh xac `RESET ESPORTS DATA`
|  |- Backend count dataset hien tai -> backup DB bang `mysqldump` -> delete `esports_game_drafts` truoc -> delete `esports_matches` sau -> reset ranking fields tren `esports_teams`
|  |- Backend verify `remainingGameDrafts = 0` va `remainingMatches = 0` roi tra response kem duong dan backup
|  |- Frontend hien rows deleted, backup file, remaining counts, va note `player_stats` duoc giu nguyen
|  `- Sau reset, admin co the upload preview/apply lai file XLSX/CSV; workflow cap nhat ranking AER truc tiep tu DB sau import van giu nguyen
|- Access permissions
|  `- `/api/admin/esports/reset-data` can role `ADMIN`
`- Risk notes or manual testing areas
   |- Can test backup tool `mysqldump` co san va tao file SQL truoc khi delete
   |- Can test sai confirmation text de dam bao khong mutate DB
   |- Can test remaining counts ve 0 sau reset va row counts cua `esports_teams`, `esports_tournaments`, `heroes` khong bi giam
   |- Can test import lai XLSX/CSV sau reset va verify summary import/ranking van cap nhat tu DB
   `- Can test production profile guard truoc khi cho phep dung tren moi truong nhay cam
```
## 2026-05 Ranked Mode Note

- Solo Ban/Pick is now split into `SIMULATION` and `RANKED`.
- `SIMULATION` keeps the old solo draft behavior and does not affect leaderboard/rank.
- `RANKED` is a real BO1 room with a persisted virtual context `BO7` stored on `ban_pick_rooms`.
- Persisted ranked context fields: `virtual_series_format`, `virtual_game_index`, `ultimate_battle`, `prep_duration_seconds`, `blue_previous_used_hero_ids`, `red_previous_used_hero_ids`, `prep_phase_start_at`, `prep_phase_end_at`.
- Previous-used hero generation uses `heroes.primary_role_id` as the role source, not global bans and not sub-role mapping.


## 2026-05-18 Frontend Split: Giả lập Solo vs Rank Mode

```text
Frontend Split: Giả lập Solo vs Rank Mode
|- Feature name
|  `- Frontend Split: Giả lập Solo vs Rank Mode
|- Purpose
|  |- Tách frontend Solo Ban/Pick thành 2 page riêng biệt
|  |- Giả lập Solo (SIMULATION): luyện tập, không tính rank
|  |- Rank Mode (RANKED): xếp hạng thật với virtual BO7 context
|  `- Không trộn 2 mode vào cùng 1 page
|- Related users
|  |- Authenticated users chơi Giả lập Solo hoặc Rank Mode
|  `- Public users xem leaderboard
|- Main HTML, JavaScript, and CSS files
|  |- `demo/src/main/resources/static/html/ban-pick-solo.html` (Giả lập Solo)
|  |- `demo/src/main/resources/static/html/ban-pick-ranked.html` (Rank Mode - MỚI)
|  |- `demo/src/main/resources/static/html/ban-pick-shell.html` (shared shell)
|  |- `demo/src/main/resources/static/html/header.html` (navigation)
|  |- `demo/src/main/resources/static/js/ban-pick.js`
|  |- `demo/src/main/resources/static/js/ban-pick-page.js`
|  `- `demo/src/main/resources/static/css/ban-pick.css`
|- Navigation
|  |- Header dropdown `Ban/Pick` có 4 entry:
|  |  |- Cấm chọn tự do
|  |  |- Cấm chọn tiêu chuẩn
|  |  |- Giả lập Solo
|  |  `- Rank Mode
|  `- Mobile nav cũng có 4 entry tương ứng
|- Mode detection
|  |- `ban-pick-solo.html`: `body[data-ban-pick-mode="solo"]` → mode `solo-1v1` → gửi `mode: SIMULATION`
|  |- `ban-pick-ranked.html`: `body[data-ban-pick-mode="ranked"]` → mode `ranked` → gửi `mode: RANKED`
|  `- `normalizeConfiguredMode()` nhận "ranked" trả "ranked"
|- Giả lập Solo
|  |- Không hiển thị virtual context, previous-used, prep phase, strategy pool
|  |- Không tính rating/rank/leaderboard
|  |- Lobby header: "Giả lập Solo 1v1 Online"
|  |- Sidebar kicker: "Giả lập Solo"
|  `- Vẫn có series selector BO1/BO3/BO5/BO7
|- Rank Mode
|  |- Lobby header: "Solo Ban/Pick Xếp Hạng"
|  |- Sidebar kicker: "Rank Mode"
|  |- Ẩn series selector (backend luôn dùng BO1 thật, virtual BO7)
|  |- Hiển thị virtual context: "Game X / 7" hoặc "Ultimate Battle"
|  |- Previous-used heroes panel:
|  |  |- Blue panel: label "Tướng bên bạn đã dùng" (nếu user là Blue) hoặc "Tướng đối thủ đã dùng"
|  |  |- Red panel: tương tự
|  |  `- Ẩn panel nếu danh sách rỗng (Game 1)
|  |- Prep phase (Game 2-6):
|  |  |- Countdown hiển thị trong `ranked-prep-panel`
|  |  |- Strategy Pool UI cho add/remove hero
|  |  `- Hết giờ → chuyển draft (backend scheduler xử lý)
|  |- Strategy Pool:
|  |  |- Hero trong pool hiển thị trước trong hero grid
|  |  |- `sortHeroGridByStrategyPool()` reorder DOM
|  |  |- Gửi qua WebSocket hoặc REST
|  |  |- Frontend block add own locked hero (+ backend cũng reject)
|  |  `- Cho phép add opponent locked hero
|  |- Own locked heroes:
|  |  |- Disabled trong hero grid (class `series-restricted`)
|  |  `- Không thể pick (backend reject 409)
|  |- Opponent locked heroes:
|  |  `- Vẫn chọn được bình thường
|  |- Ultimate Battle (Game 7):
|  |  |- Ẩn ban slots (`.bans-col.is-hidden`)
|  |  |- Label "Ultimate Battle: Không ban, blind-pick only."
|  |  `- Chỉ pick phases
|  `- Summary: winner = player có Ban/Pick Score cao hơn; tie = hòa
|- API
|  |- `POST /api/ban-pick/rooms` body gửi `mode: "RANKED"` hoặc `mode: "SIMULATION"` explicit
|  |- `POST /api/ban-pick/rooms/{roomCode}/strategy-pool` hoặc WebSocket `/app/ban-pick/{roomCode}/strategy-pool`
|  `- Room state response chứa `myStrategyPool`, `bluePreviousUsedHeroIds`, `redPreviousUsedHeroIds`, `ultimateBattle`, `virtualGameIndex`, `prepPhaseEndAt`
|- Database
|  `- Không đổi DB trong task frontend này
`- Risk notes
   |- Manual smoke test cần cover Game 1, Game 2-6, Game 7 (random từ backend)
   |- Strategy pool sort chỉ reorder DOM, không ảnh hưởng validation
   `- Mobile responsive cần test previous-used panels và prep phase UI
```
