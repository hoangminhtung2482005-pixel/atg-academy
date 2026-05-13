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
- `demo/src/main/resources/static/js`: client logic; `auth.js`, `tier-list-*`, `tactics-guides.js`, `admin.js`, `admin-heroes.js`.
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
│  ├─ Quản lý tài khoản: hồ sơ + panel `Nội dung của bạn` đếm/list content chính chủ; file: UserProfileController, UserProfileService, static/html/account.html
│  ├─ Trang chủ feed; file: HomeFeedController, HomeFeedService, static/html/index.html
│  ├─ Wiki tướng + hero pool data cho Ban/Pick; dropdown `Wiki` trỏ đến `wiki.html`, deep-link `?tab=spells`, `?tab=enchantments`, và `Esports Data`; file: WikiController, SpellController, EnchantmentController, static/html/wiki.html, header.html, header-loader.js
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
│  ├─ AER Data admin: teams/matches/import/reset Elo trên `admin.html#aer-data`; file: EsportsAdminController, EsportsAdminService, static/html/admin.html
│  ├─ Esports Data admin: quan ly `upload file -> preview import -> confirm import` va `match -> game draft records -> validate` tren `admin-esports-data.html`; file: EsportsAdminController, EsportsAdminService, EsportsDraftService, static/html/admin-esports-data.html, static/js/admin-esports-data.js
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
│     └─ Luồng: create room -> join -> roll side -> ready -> 15 phase draft -> frontend dùng hero pool `/api/wiki/heroes` để cộng `banPickScore` cho tướng đã pick -> hiển thị tổng điểm/tỷ lệ thắng dự đoán -> lineup adjustment -> finish -> history/stats
├─ Authentication & Authorization
│  ├─ Backend: stateless bearer auth, validate issuer/audience/email_verified của Google token
│  ├─ Public GET: wiki heroes, spells, enchantments, guides, tier-lists, esports, một phần ban-pick result/leaderboard
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
│  └─ Static routes: StaticPageRedirectController redirect /, /guides, /tier-list, /tier-list/all, /tier-list/mine; route cũ `/tier-list/recommended`, `/tier-list-recommended.html`, `/html/tier-list-recommended.html` redirect an toàn về `tier-list.html`; `/tactics-guides.html` và `/html/tactics-guides.html` redirect an toàn về `giao-an.html`; `/ban-pick`, `/ban-pick.html`, `/html/ban-pick.html` chuyển an toàn về mode phù hợp/default Free Mode, còn các route `/ban-pick/*` profile/result/leaderboard vẫn sang static/html như cũ; admin shell `/html/admin.html` giữ module `AER Data` tại hash `#aer-data` và vẫn alias `#teams`/`#esports`, còn `/html/admin-esports-data.html` phục vụ module `Esports Data` nhập draft chi tiết; placeholder `content.html` đã bị xóa khỏi codebase và không giữ redirect legacy
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
| Tài khoản | Xem/sửa hồ sơ cá nhân + nội dung của bạn | Đổi `displayName`, `level`; sidebar dashboard hiển thị số guide đã đăng và community tier list đã đăng của user hiện tại | `UserProfileController`, `UserProfileService`, `GuideRepository`, `TierListRepository`, `static/html/account.html` | `/api/users/me/profile`, `/api/users/me/content-summary` | `users`, `guides`, `tier_lists` | Content summary dùng security principal, chỉ tính guide published và tier list không official của chính user; frontend account dashboard render count Community Tier List theo dang `n/5` |
| Trang chủ | Community feed | Section homepage dùng 3 Tier List cộng đồng nổi bật; feed hỗn hợp cũ vẫn giữ cho backend | `HomeFeedController`, `HomeFeedService`, `TierListRatingRepository`, `TierListAdminRatingRepository`, `static/html/index.html` | `/api/home/feed`, `/api/home/community-tier-highlights` | `tier_lists`, `guides`, `tier_list_ratings`, `tier_list_admin_ratings` | `/api/home/community-tier-highlights` bỏ official, tránh trùng và có thể trả ít hơn 3 item |
| Wiki | Danh mục tướng + gameplay data | Trả catalog tướng, class/role/attribute, skill, matchup, guide liên quan; hero summary kèm `banPickScore`; cùng page `wiki.html` hiện runtime chỉ còn tab `Tướng`, `Bổ trợ`, `Phù hiệu` đọc từ API/JSON thật | `WikiController`, `SpellController`, `EnchantmentController`, `SpellService`, `EnchantmentService`, `WikiJsonStorageService`, `static/html/wiki.html`, `static/html/header.html`, `static/js/header-loader.js`, `static/css/wiki.css` | `/api/wiki/heroes*`, `/api/spells*`, `/api/enchantments*`, `wiki.html?tab=spells`, `wiki.html?tab=enchantments` | `heroes`, `hero_skills`, `hero_matchups`, `static/data/spells.json`, `static/data/enchantments.json` | Dropdown `Wiki` deep-link vào `wiki.html`, `wiki.html?tab=spells`, `wiki.html?tab=enchantments`; runtime không còn tab/frontend reference `items/arcana`; legacy DB `phu_hieu` có thể còn tồn tại trong SQL thủ công nhưng không còn là runtime dependency |
| Tier list | Meta chính thức | Official meta duoc generate tu `heroes.ban_pick_score`; public doc tren `tier-list.html`; Admin save/regenerate de persist vao `tier_lists.contentData`; trang goc van hien board official o tren va 6 community noi bat o duoi | `TierListController`, `AdminTierListController`, `TierListCommunityService`, `HeroContentDataService`, `static/html/tier-list.html`, `static/js/tier-list-app.js` | `/api/tier-lists/official`, `POST /api/admin/tier-lists/official/regenerate-from-hero-scores` | `tier_lists`, `heroes` | Rule tier: `>9 S`, `>7.5 A`, `>5 B`, `>2.5 C`, else `D`; role columns giu DSL/JGL/MID/ADL/SUP; section community tren trang goc van dung `GET /api/tier-lists/community` |
| Tier list | Community tier list | User tạo tier list; danh sách cộng đồng giữ 2 page `tier-list-all.html`, `tier-list-mine.html`; dropdown `Tier List` trên header giữ `Tier List Meta`, `Tất cả Tier List`, `Tier List của bạn`; page `tier-list-recommended.html` đã bị xóa, còn route cũ chỉ redirect về `tier-list.html`; xem detail, rate, comment; Admin dùng cùng rating panel trên detail page để lưu admin rating | `TierListController`, `AdminTierListController`, `TierListCommunityService`, `TierListRatingRepository`, `TierListAdminRatingRepository`, `static/html/tier-list-community-shell.html`, `static/html/tier-list-all.html`, `static/html/tier-list-mine.html`, `static/js/tier-list-community-page.js`, `static/js/tier-list-app.js`, `static/html/tier-list-detail.html`, `tier-list-detail.js` | `/api/tier-lists/*`, `/api/admin/tier-lists/{id}/admin-rating` | `tier_lists`, `tier_list_ratings`, `tier_list_comments`, `tier_list_admin_ratings` | Guest chi co 1 draft tam trong `sessionStorage` key `atg_guest_tier_list_draft`; logged-in user duoc tao/luu toi da 5 Community Tier List non-official; update tier list da ton tai khong tinh them quota; official/admin tier list khong bi ap limit nay |
| Guide | Danh sách/chi tiết guide | Search/filter theo `status`, `heroId`, `lane`, `category`, `search`, `sort` | `GuideController`, `GuideRepository`, `static/js/tactics-guides.js` | `GET /api/guides*` | `guides` | Filter đang chạy in-memory |
| Guide | Tạo guide | Tạo giáo án từ form frontend; lưu metadata + `contentData` JSON | `GuideController`, `static/html/create-guide.html` | `POST /api/guides` | `guides`, `users`, `heroes` | Không thấy update/delete/moderation |
| Esports | BXH public | Trả danh sách đội xếp theo Elo và recent matches | `EsportsController`, `static/html/esports.html` | `/api/esports/teams`, `/matches/recent` | `esports_teams`, `esports_matches` | `esports.html` hiện dùng team list; recent feed cần xác minh thêm |
| Esports | Esports Data | Dashboard analytics public cho draft esports theo model game-level moi | `EsportsController`, `EsportsDataService`, `EsportsGameDraftRepository`, `static/html/esports-data.html`, `static/js/esports-data.js`, `static/css/esports.css` | `/esports/data`, `/api/esports/data/*` | `heroes`, `esports_matches`, `esports_game_drafts` | Public page doc tu `esports_game_drafts`; KPI/top bans/top picks/side WR/hero stats khong con aggregate runtime tu cac bang draft cu |
| Ban/Pick | Room draft online | Create room, join, ready, roll side, start, confirm pick/ban, reorder lineup, next game, reset; UI có panel compact hiển thị tổng điểm đội hình và tỷ lệ thắng dự đoán từ các tướng đã pick | `BanPickRoomController`, `BanPickRoomService`, `BanPickRoomWebSocketController`, `ban-pick-free.html`, `ban-pick-standard.html`, `ban-pick-solo.html`, `ban-pick-shell.html`, `ban-pick.js` | `/api/ban-pick/rooms/*`, `/ws`, `/api/wiki/heroes` | `ban_pick_rooms`, `ban_pick_actions`, `ban_pick_room_participants`, `heroes` | Navbar dropdown là luồng chuyển mode chính; chỉ còn legacy entry route `/ban-pick*` redirect/fallback; không cộng điểm tướng bị ban |
| Ban/Pick | L?ch s?/BXH c? nh?n | Luu draft finished, ghi ngu?i th?ng, profile, leaderboard | `BanPickHistoryController`, `BanPickHistoryService`, `ban-pick-profile.html`, `ban-pick-result.html` | `/api/ban-pick/history*`, `/leaderboard`, `/profile` | `draft_histories`, `player_stats` | Rating ngu?i choi tang/gi?m ?15 |
| Admin | Quản lý user | List/search/filter user, chỉnh `name/avatar/role/status/note` | `AdminUserController`, `AdminUserService`, `static/html/admin.html` | `/api/admin/users*` | `users` | Admin không thể tự hạ role/khoá chính mình |
| Admin | Quản lý hero wiki | Sửa basic info, class, role, attribute, difficulty, `banPickScore`; admin panel validate min/max/2 chữ số thập phân trước khi lưu DB | `AdminWikiHeroController`, `AdminWikiHeroService`, `static/html/admin-heroes.html`, `static/js/admin-heroes.js` | `/api/admin/wiki/heroes*` | `heroes`, join tables hero_* | Có gợi ý role theo class; user thường không có quyền sửa |
| Admin | Quản lý hero attributes | Có 2 API admin attribute khác nhau | `AdminWikiAttributeController`, `HeroAttributeController`, `AdminWikiHeroService`, `HeroAttributeService` | `/api/admin/wiki/attributes*`, `/api/admin/attributes*` | `hero_attributes`, `hero_attribute_mapping` | Hành vi delete không nhất quán |
| Admin | AER Data | CRUD team/match, bulk import, recalculation Elo cho workflow ranking/AER tổng quát | `EsportsAdminController`, `EsportsAdminService`, `static/html/admin.html`, `static/css/admin.css` | `/api/admin/esports/teams*`, `/api/admin/esports/matches*`, `/api/admin/esports/teams/matches/bulk-import` | `esports_teams`, `esports_matches` | Sidebar admin có entry `AER Data`; canonical route là `admin.html#aer-data`, vẫn alias `#teams` và `#esports` để không gãy flow cũ |
| Admin | Esports Data | Quan ly tung van dau theo `game draft record`, preview import Excel/CSV truoc khi commit DB, export CSV cho thong ke public `/esports/data`, va co `Tournament Management` de admin chinh `aerTier`/roster | `EsportsAdminController`, `EsportsAdminService`, `EsportsDraftService`, `static/html/admin-esports-data.html`, `static/js/admin-esports-data.js`, `static/css/admin.css` | `/api/admin/esports/franchises*`, `/api/admin/esports/tournaments*`, `/api/admin/esports/tournaments/{id}/teams*`, `/api/admin/esports/matches*`, `/api/admin/esports/matches/{matchId}/game-drafts`, `/api/admin/esports/game-drafts/{id}`, `/api/admin/esports/game-drafts/export`, `/api/admin/esports/game-drafts/import/preview`, `/api/admin/esports/game-drafts/import/confirm` | `esports_franchises`, `esports_tournaments`, `esports_tournament_teams`, `esports_matches`, `esports_game_drafts` | Sidebar admin co entry `Esports Data`; page admin them workflow tournament management + upload file -> preview ket qua -> danh sach loi/warning -> confirm import; task hien tai chi lam tournament management, khong doi CSV/JSON/TXT flow |

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
| GET | `/api/esports/tournaments`, `/api/esports/tournaments/{id}`, `/api/esports/tournaments/{id}/teams` | `EsportsController` | `EsportsTournamentService` | list: `franchiseId, franchiseCode`; detail/team: `id` | tournament list/detail/team payload co `aerTier` | Public tournament catalog va roster team tham gia; `esports_tournaments.aer_tier` la nguon tier cho workflow AER JSON sau nay |
| GET | `/api/esports/data/tournaments` | `EsportsController` | `EsportsDataService` | none | `EsportsTournamentOptionResponse[]` | Danh sach tournament scope co du lieu draft cho filter public page |
| GET | `/api/esports/data/top-banned-heroes` | `EsportsController` | `EsportsDataService` | `tournamentId, tournamentName, limit` | `EsportsHeroBanStatResponse[]` | Top hero bi ban nhieu nhat, aggregate tu 10 cot ban cua `esports_game_drafts`; van fallback tier legacy neu `tournament_id` chua co |
| GET | `/api/esports/data/top-blue-banned-heroes` | `EsportsController` | `EsportsDataService` | `tournamentId, tournamentName, limit` | `EsportsHeroBanStatResponse[]` | Top hero bi blue side ban nhieu nhat |
| GET | `/api/esports/data/hero-stats` | `EsportsController` | `EsportsDataService` | `tournamentId, tournamentName` | `EsportsHeroStatResponse[]` | Picks, bans, presence, wins, WR tu lineup + bans cua `esports_game_drafts` |
| GET | `/api/esports/data/dashboard` | `EsportsController` | `EsportsDataService` | `tournamentId, tournamentName, teamCode, dateFrom, dateTo` | `EsportsDashboardResponse` | KPI/tables/charts data cho page public; runtime doc tu `esports_game_drafts` |
| POST | `/api/ban-pick/rooms` | `BanPickRoomController` | `BanPickRoomService` | `seriesType` | `BanPickCreateRoomResponse` | Tạo phòng online |
| GET | `/api/ban-pick/rooms/{roomCode}` | `BanPickRoomController` | `BanPickRoomService` | `roomCode` | `BanPickRoomStateResponse` | State phòng |
| POST | `/api/ban-pick/rooms/{roomCode}/join`, `/roll-side`, `/ready`, `/start` | `BanPickRoomController` | `BanPickRoomService` | none | `BanPickRoomStateResponse` | Lobby + bắt đầu draft |
| POST | `/api/ban-pick/rooms/{roomCode}/confirm` | `BanPickRoomController` | `BanPickRoomService` | `teamSide, actionType, heroId/heroName` | `BanPickRoomStateResponse` | Xác nhận ban/pick |
| POST | `/api/ban-pick/rooms/{roomCode}/lineup/reorder`, `/lineup/confirm` | `BanPickRoomController` | `BanPickRoomService` | reorder: `heroIds`; confirm: `teamSide` | `BanPickRoomStateResponse` | Sắp xếp/xác nhận lineup |
| POST | `/api/ban-pick/rooms/{roomCode}/next-game`, `/reset` | `BanPickRoomController` | `BanPickRoomService` | none | `BanPickRoomStateResponse` | Sang ván mới / reset |
| GET | `/api/ban-pick/history`, `/api/ban-pick/history/{id}` | `BanPickHistoryController` | `BanPickHistoryService` | `id` optional | `DraftHistoryResponse[]` / `DraftHistoryResponse` | Lịch sử draft |
| POST | `/api/ban-pick/history/{id}/winner` | `BanPickHistoryController` | `BanPickHistoryService` | `winnerSide` | `DraftHistoryResponse` | Ghi nhận người thắng |
| GET | `/api/ban-pick/leaderboard`, `/api/ban-pick/profile` | `BanPickHistoryController` | `BanPickHistoryService` | none | `PlayerStatsResponse[]`, `BanPickProfileResponse` | BXH và hồ sơ ban/pick |
| GET, PUT | `/api/admin/users`, `/api/admin/users/{id}` | `AdminUserController` | `AdminUserService` | list filters; update `name/avatarUrl/role/status/note` | page/detail DTO | Quản lý user |
| GET, PUT | `/api/admin/wiki/heroes`, `/api/admin/wiki/heroes/{id}` | `AdminWikiHeroController` | `AdminWikiHeroService` | update basic info + `banPickScore` | hero list/detail DTO kèm `banPickScore` | Quản lý hero |
| PUT | `/api/admin/wiki/heroes/{id}/roles`, `/api/admin/wiki/heroes/{id}/attributes` | `AdminWikiHeroController` | `AdminWikiHeroService` | `roles[]`, `attributes[]` | hero detail DTO | Gán role/attribute |
| GET, POST, PUT, DELETE | `/api/admin/wiki/attributes*` | `AdminWikiAttributeController` | `AdminWikiHeroService` | attribute upsert | list/detail/204 | Attribute admin chuẩn wiki |
| GET, POST, PATCH, DELETE | `/api/admin/attributes*` | `HeroAttributeController` | `HeroAttributeService` | attribute upsert | list/detail/204 | API legacy cho attribute |
| GET | `/api/admin/esports/game-drafts/export` | `EsportsAdminController` | `EsportsDraftService` | `tournamentId, tournamentName, matchId, dateFrom, dateTo` | CSV attachment UTF-8 BOM | Export game draft records theo format Excel mau; moi dong = 1 row trong `esports_game_drafts` |
| POST | `/api/admin/esports/game-drafts/import/preview` | `EsportsAdminController` | `EsportsDraftService` | multipart `file`, `overwriteExisting` | `EsportsGameDraftImportPreviewResponse` | Parse CSV/Excel, map tournament/team/hero, resolve parent match, validate duplicate `(match_id, game_number)`, va tra preview token + summary truoc khi import that |
| POST | `/api/admin/esports/game-drafts/import/confirm` | `EsportsAdminController` | `EsportsDraftService` | `previewToken` | `EsportsGameDraftImportConfirmResponse` | Confirm preview hop le de tao/cap nhat `esports_matches` va `esports_game_drafts`; recalculate Elo neu match cha bi tao moi hoac doi ty so |
| GET, POST, PUT, DELETE | `/api/admin/esports/franchises*` | `EsportsAdminController` | `EsportsFranchiseService` | franchise body | list/detail/message | Admin CRUD/deactivate franchise catalog |
| GET, POST, PUT, DELETE | `/api/admin/esports/tournaments*` | `EsportsAdminController` | `EsportsTournamentService` | list: `franchiseId, franchiseCode`; request body tournament co `aerTier` | list/detail/message | Admin CRUD tournament catalog, sua duoc `aerTier`; xoa bi chan neu da link vao `esports_matches` |
| GET, POST, DELETE | `/api/admin/esports/tournaments/{id}/teams*` | `EsportsAdminController` | `EsportsTournamentService` | team body / `teamId` | roster/message | Admin quan ly team tham gia tung tournament |
| GET, POST, PUT, DELETE | `/api/admin/esports/teams*`, `/api/admin/esports/teams/matches/bulk-import` | `EsportsAdminController` | `EsportsAdminService` | team body / raw text import | team/list/message | Quản lý/import team |
| GET, POST, PUT, DELETE | `/api/admin/esports/matches*`, `DELETE /api/admin/esports/matches` | `EsportsAdminController` | `EsportsAdminService` | match body | match/list/message | Quản lý/reset match history; co the set `tournamentId` nhung flow cu van chay khi de null |
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
| `esports_tournaments` | `id,franchise_id,name,slug,season_year,split_name,tier_level,aer_tier,start_date,end_date,status` | N-1 `esports_franchises`; 1-N `esports_tournament_teams`; optional 1-N `esports_matches` qua `tournament_id` | `EsportsTournamentRepository` | Tung mua/giai cu the nhu `AOG Spring 2026`, `RPL Summer 2026`; `aer_tier` la tier so cho workflow CSV -> AER JSON sau nay |
| `esports_tournament_teams` | `id,tournament_id,team_id,group_name,seed_number,status,note` | N-1 `esports_tournaments`, N-1 `esports_teams` | `EsportsTournamentTeamRepository` | Roster team tham gia tung tournament |
| `esports_teams` | `id,teamCode,teamName,logoUrl,region,score,gameWins,gameLosses,matchWins,matchLosses` | Khong FK truc tiep toi `esports_matches`; lien he bang `teamCode`; co the duoc map vao `esports_tournament_teams` | `EsportsTeamRepository` | BXH esports va nguon roster cho tournament |
| `esports_matches` | `id,matchDate,team1Code,team2Code,score1,score2,tier,stage,tournament_id(optional)` | Quan he logic qua `teamCode`; optional N-1 `esports_tournaments` qua `tournament_id` | `EsportsMatchRepository` | Lich su tran va input tinh Elo; flow cu van fallback `tier` neu match chua link tournament, con match co tournament chinh thuc se dong bo fallback tier theo `esports_tournaments.aer_tier` |
| `esports_game_drafts` | `id,match_id,game_number,blue_team_id,red_team_id,winner_team_id,duration_seconds,draft_format_code,source,10 ban hero ids,10 lineup hero ids,raw_draft_json` | N-1 `esports_matches`; N-1 `esports_teams` cho blue/red/winner; hero refs luu dang flat columns | `EsportsGameDraftRepository` | Nguon game-level moi cho public Esports Data va admin draft workflow; 1 row = 1 van dau; van giu 5 ban moi ben cho export/admin |
| `ban_pick_rooms` | `id,roomCode,status,phaseType,seriesType,currentGameNumber,host/guest/blue/red user,ready flags,deadline fields,pick history` | N-1 nhiều lần về `users`; 1-N `ban_pick_actions`, `ban_pick_room_participants` | `BanPickRoomRepository` | Trạng thái draft room |
| `ban_pick_room_participants` | `id,room_id,user_id,role,teamSide,joinedAt` | N-1 `ban_pick_rooms`, N-1 `users` | `BanPickRoomParticipantRepository` | Thành viên phòng |
| `ban_pick_actions` | `id,room_id,user_id,teamSide,actionType,heroId,phaseIndex,confirmedAt` | N-1 `ban_pick_rooms`, N-1 `users` | `BanPickActionRepository` | Log ban/pick từng phase |
| `draft_histories` | `id,roomCode,blue_user_id,red_user_id,winner_user_id,bluePicks,redPicks,blueBans,redBans,resultRecordedAt` | N-1 `users` | `DraftHistoryRepository` | Lịch sử draft đã hoàn tất |
| `player_stats` | `id,user_id,totalMatches,wins,losses,rating,pickedHeroCounts` | 1-1 `users` | `PlayerStatsRepository` | Profile/BXH ban-pick |

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
|  |- Shared navbar dropdown pattern: `header.html` + `header-loader.js` dung cho `Ban/Pick` va `Tier List`
|  |- Shared compact draft evaluation component: `draft-balance-panel`, `draft-balance-item` cho score + win-rate cua hai doi
|  `- Base classes: `.atg-page`, `.atg-section`, `.atg-section-header`, `.atg-card`, `.atg-button-*`, `.atg-search`, `.atg-table`, `.atg-modal`, `.atg-empty-state`, `.atg-error-state`
|- CSS ownership sau refactor
|  |- `style.css` chi giu token, reset/base, shared typography, layout utilities, buttons, cards, search/input, modal/toast/empty/error, header/footer shared
|  |- `tier-list.css` own `tier-list.html`, `tier-list-all.html`, `tier-list-mine.html`, `tier-list-detail.html`
|  |- `ban-pick.css` own `ban-pick-free.html`, `ban-pick-standard.html`, `ban-pick-solo.html`, `ban-pick-profile.html`, `ban-pick-result.html`, `ban-pick-leaderboard.html`
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
|  |- `GET /api/esports/data/hero-stats` -> picks, bans, presence, wins, WR tu `esports_game_drafts`
|  |- `GET /api/esports/data/top-banned-heroes?tournamentName=...&limit=5`
|  `- `GET /api/esports/data/top-blue-banned-heroes?tournamentName=...&limit=5`
|- Database table or entity
|  |- `heroes`
|  |- `esports_matches`
|  |- `esports_game_drafts`
|  `- `esports_match_games`, `esports_match_draft_actions`, `esports_match_game_lineups` da duoc retire khoi runtime; source of truth la `esports_game_drafts`
|- Header / Navigation
|  |- Shared header bo menu top-level rieng `Esports Data`; top-level chi con `Wiki` dang dropdown
|  |- Dropdown `Wiki` gom item `Wiki`, deep-link `Bo tro` (`/html/wiki.html?tab=spells`), `Phu hieu` (`/html/wiki.html?tab=enchantments`), va `Esports Data`
|  `- Active state duoc nhan dien tren trigger `Wiki` khi dang o `wiki.html`, `wiki.html?tab=spells`, `wiki.html?tab=enchantments`, hoac `/esports/data`; item `Esports Data` van active dung tren `esports-data.html`
|- Routing / Static Pages
|  |- Them page `esports-data.html`
|  |- Them route dep `/esports/data` -> redirect `/html/esports-data.html`
|  `- Legacy top-level `/esports-data.html` duoc redirect vao static page moi
|- Main workflow
|  |- User mo `/esports/data` hoac `esports-data.html`
|  |- Frontend load danh sach giai dau tu `GET /api/esports/data/tournaments`
|  |- Frontend hien tournament filter, KPI cards, bang top bans, bang top picks, side win rate, va hero statistics; UI giu tinh than Apple-like toi gian theo `docs/ui-style-guide.md`
|  |- `GET /api/esports/data/dashboard` la nguon chinh cho summary, side advantage, hero stats, team options, top banned hero, va top blue banned hero
|  |- `GET /api/esports/data/hero-stats` va dashboard deu aggregate tu `esports_game_drafts`: 10 cot ban cho bans, 10 cot lineup cho picks, `winner_team_id` so voi `blue_team_id`/`red_team_id` cho side WR va hero wins
|  |- Filter `tournamentName` tiep tuc di theo convention `tier`; backend van chap nhan `teamCode/dateFrom/dateTo` cho reuse sau nay
|  |- Top picked heroes duoc suy ra tu lineup theo lane `DSL/JGL/MID/ADL/SUP`, khong can draft-phase data
|  |- Bang Hero Statistics tren `esports-data.html` co loading state, empty state, sort client-side theo `Picks Σ`, `Picks WR`, `Bans Σ`, `Picks & Bans Σ`, va header tach ro `Hero` / `Picks` / `Blue side Picks` / `Red side Picks` / `Bans` / `Picks & Bans`
|  |- `GET /api/esports/data/top-banned-heroes` va `GET /api/esports/data/top-blue-banned-heroes` duoc giu de page co the render bang rieng va empty-state ro rang
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
   |- Can test top bans, top blue bans, top picks, side WR, hero stats tren data that sau migration
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
|  `- `esports_game_drafts` chi bo sung game-level data cho Esports Data, khong chen vao tinh Elo/ranking
|- Admin Esports
|  |- Admin tiep tuc CRUD team/match/import/reset Elo nhu truoc qua `EsportsAdminService`
|  |- Admin giu `Bulk Import` + team roster + match history/ranking o module `AER Data` (`/html/admin.html#aer-data`)
|  |- Admin dung page `/html/admin-esports-data.html` cho workflow `upload file -> preview import -> confirm import` va `match -> game draft records` qua `EsportsDraftService`
|  |- Sidebar/admin dashboard tach ro `AER Data` va `Esports Data`; hash cu `#teams`/`#esports` van mo lai AER Data de giu flow cu
|  |- Form series tren page admin nhap `match_date`, `team 1`, `team 2`, `score1`, `score2`, `giai/tier`, va `stage`; `stage` cho phep nhap custom, khong bi khoa vao 4 preset
|  |- Chon 1 match se load list game draft records cua series, selected match card, editor form, va validation summary
|  |- Nut `Xuat CSV` goi `GET /api/admin/esports/game-drafts/export`; neu dang chon match thi uu tien `matchId`, neu khong thi co the dung `tournamentName/dateFrom/dateTo`
|  |- Import UI moi co file input `.csv/.xlsx/.xls`, checkbox overwrite ro rang, status card preview, summary cards, bang preview tung dong, danh sach error/warning, va nut `Confirm Import`
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
|  |- Admin co the `Bulk Import` match history ngay tren `/html/admin.html#aer-data`; API va format raw text giu nguyen workflow cu
|  |- Admin tao/sua series bang form match: `match_date` + `team1/team2` + `score1/score2` + `tier` + `stage`
|  |- Admin co the upload file Excel/CSV tren `/html/admin-esports-data.html` -> backend preview map tournament/team/hero -> hien summary + error/warning list -> chi cho Confirm khi preview sach loi
|  |- Confirm import co the:
|  |- Dung `esports_matches` hien co neu khop ngay + tournament + cap doi
|  |- Gan `tournament_id` vao match cu dang null neu file match entity tournament
|  |- Tao match cha moi neu chua co, score series suy ra tu cot `Winner`, stage mac dinh `bang`, va gio match mac dinh `12:00`
|  |- Tao draft moi hoac overwrite draft trung `game_number` neu admin da bat overwrite
|  |- Sau import, page refresh data va admin co the mo `/esports/data` de verify public analytics doc du lieu moi
|  |- Frontend gui payload draft editor thu cong: `blueBans[]`, `redBans[]`, `blueLineup{DSL..SUP}`, `redLineup{DSL..SUP}`, `winnerTeamId`, `durationSeconds`
|  |- `Length` co the nhap dang `mm:ss` hoac seconds; frontend normalize truoc khi goi API editor tay va backend convert khi import file
|  |- Save xong, page reload list draft cua match, render status chip, va cap nhat validation summary
|  |- Export CSV doc truc tiep tu `esports_game_drafts`; Team_1 = blue side, Team_2 = red side, moi dong = 1 van, `Length` = `mm:ss`
|  |- Public/frontend runtime Esports Data chi con doc `/api/esports/data/*`; admin runtime chi con doc `/api/admin/esports/*/game-drafts`
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
   `- Can smoke-test `admin.html#aer-data` cho bulk import/match history va `admin-esports-data.html` cho upload preview/confirm import, chon match, create/update/delete game draft record, duplicate validation, va link verify sang `/esports/data`
```

**16. Esports Franchise / Tournament Catalog**

```text
Esports Franchise / Tournament Catalog
|- Feature name
|  `- Esports Franchise / Tournament Catalog
|- Purpose
|  |- Tach "franchise" (giai me) khoi "tournament" (mua giai cu the)
|  |- Quan ly roster team tham gia tung tournament ma khong pha `esports_teams`, `esports_matches`, `esports_game_drafts`
|  |- `esports_tournaments.aer_tier` la nguon tier so chuan cho workflow CSV -> AER JSON o task sau
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
|  |- SQL migration: `demo/sql/esports_franchises_tournaments_migration.sql`, `demo/sql/add_aer_tier_to_esports_tournaments.sql`
|  |- Seed franchise bat buoc: `RPL`, `AOG`, `GCS`, `APL`, `AIC`
|  |- Seed tournament toi thieu: `AOG Spring 2026`, `AOG Winter 2026`, `RPL Summer 2026`, `GCS Spring 2026`
|  |- `esports_tournaments.aer_tier` mac dinh `1`; admin co the sua truc tiep tren Tournament Management
|  `- Starter roster seed chi insert khi `esports_teams.team_code` da ton tai
|- Main workflow
|  |- Public page load franchise/tournament catalog -> user chon scope -> analytics API goi `tournamentId`
|  |- Admin vao `Tournament Management` -> filter theo franchise -> tao/sua tournament -> set `aerTier` + `tierLevel` -> gan team tham gia
|  |- Task CSV -> AER JSON chua lam trong feature nay; task sau se lookup tier tu `esports_tournaments.aer_tier` va bao loi neu tournament trong CSV chua ton tai
|  `- Match cu van hop le neu `tournament_id` de null; service/export fallback ve `tier`, con match co tournament se uu tien `aer_tier`
|- Access permissions
|  |- `GET /api/esports/**` la public read
|  `- `POST/PUT/DELETE /api/admin/esports/**` can role `ADMIN`
`- Risk notes or manual testing areas
   |- Production DB can chay SQL migration hoac de Hibernate `ddl-auto=update` tao schema; task nay khong chay SQL that va chua verify du lieu tren MySQL local vi may hien tai khong co `mysql` CLI
   |- `tournament_id` tren `esports_matches` la optional; can smoke-test admin create/update match voi ca 2 truong hop null va co gia tri
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
|  `- Khong con nut `Đổi chế độ` / `Thoát` chi de quay ve route legacy `/ban-pick`; luong pick/ban, timer, reset giu nguyen, panel `Đánh giá đội hình` cap nhat realtime khi pick/huy/reset
|- Ban/Pick Solo Online
|  |- Wrapper page: `demo/src/main/resources/static/html/ban-pick-solo.html`
|  |- Khong con nut quay ve landing page trong lobby/summary/status shell
|  |- Shareable room link di thang toi `/html/ban-pick-solo.html?room={roomCode}`
|  |- Hero pool van lay tu `GET /api/wiki/heroes`; moi hero co `banPickScore` tu backend/database
|  `- Cac nut nghiep vu room nhu join, ready, start, reset, next game, lineup confirm van giu nguyen; panel `Đánh giá đội hình` cap nhat khi room state doi, pick/huy/reset/sang van moi
|- CSS ownership
|  |- `ban-pick-free.html`, `ban-pick-standard.html`, `ban-pick-solo.html`, `ban-pick-profile.html`, `ban-pick-result.html`, `ban-pick-leaderboard.html` deu load `style.css` + `ban-pick.css`
|  |- `style.css` giu token/base va mot so hero-pool primitive shared; `ban-pick.css` own draft board, pick/ban slots, team panel, status, profile/result/leaderboard layout
|  `- `ban-pick-shell.html` tiep tuc la shell DOM chung; khong doi JS state machine, API, hay DB
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
