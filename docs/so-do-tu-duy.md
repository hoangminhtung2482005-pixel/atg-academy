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
│  ├─ Wiki tướng + hero pool data cho Ban/Pick; file: WikiController, WikiService, static/html/wiki.html
│  ├─ Tier List chính thức: `tier-list.html` là trang gốc của module Tier List, hiển thị Meta chính thức ở trên và tối đa 6 Community Tier List nổi bật ở dưới; file: TierListController, TierListCommunityService, static/html/tier-list.html
│  ├─ Tier List cộng đồng: giữ 2 static page `tier-list-all.html`, `tier-list-mine.html`; `tier-list.html` tiếp tục là trang gốc hiển thị Meta + community highlight, còn route cũ `/tier-list/recommended` chỉ redirect an toàn về trang gốc; detail dùng một rating panel chung, user lưu community rating còn Admin lưu admin rating; file: TierListController, AdminTierListController, static/html/tier-list-community-shell.html, static/js/tier-list-community-page.js, static/js/tier-list-app.js, tier-list-detail.js
│  ├─ Guides: list/filter/detail/tạo; file: GuideController, static/html/giao-an.html, tactics-guides.html, create-guide.html
│  ├─ Esports public: BXH đội và match feed; file: EsportsController, static/html/esports.html
│  ├─ Ban/Pick mode navigation: dropdown navbar vào thẳng `ban-pick-free.html`, `ban-pick-standard.html`, `ban-pick-solo.html`; `ban-pick.html` chỉ còn fallback redirect cho link cũ
│  └─ Ban/Pick 1v1 online: room/join/realtime/history/profile/leaderboard; file: BanPickRoomController, BanPickHistoryController, BanPickRoomWebSocketController
├─ Chức năng quản trị
│  ├─ Dashboard admin; file: static/html/admin.html, admin-heroes.html
│  ├─ Quản lý người dùng; file: AdminUserController, AdminUserService
│  ├─ Quản lý wiki hero + điểm Ban/Pick; file: AdminWikiHeroController, AdminWikiHeroService
│  ├─ Quản lý hero attributes; file: AdminWikiAttributeController, HeroAttributeController
│  ├─ Quản lý tier list chính thức và admin rating; file: TierListController, AdminTierListController
│  ├─ Quản lý esports teams/matches/import/reset Elo; file: EsportsAdminController, EsportsAdminService
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
│  │  ├─ DB: guides; có thêm bang_ngoc/phu_hieu/vat_pham và huong_dan_* nhưng chưa thấy dùng runtime rõ ràng
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
│  ├─ Public GET: wiki, guides, tier-lists, esports, một phần ban-pick result/leaderboard
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
│  └─ Static routes: StaticPageRedirectController redirect /, /guides, /tier-list, /tier-list/all, /tier-list/mine; route cũ `/tier-list/recommended`, `/tier-list-recommended.html`, `/html/tier-list-recommended.html` redirect an toàn về `tier-list.html`; `/ban-pick` và `/ban-pick.html` chuyển an toàn về mode phù hợp/default Free Mode, còn các route `/ban-pick/*` profile/result/leaderboard vẫn sang static/html như cũ
└─ Điểm cần lưu ý
   ├─ GuideController lọc guide bằng findAll().stream(); rủi ro scale
   ├─ Có 2 API admin attribute với semantics xóa khác nhau; cần xác minh thêm
   ├─ guides.contentData đang là nguồn dữ liệu chính; các bảng huong_dan_* có dấu hiệu chưa dùng runtime rõ ràng
   ├─ README nói có /api/meta nhưng code hiện tại không tìm thấy controller/service tương ứng
   ├─ wiki.html có tab Spells/Items/Arcana/Enchantments nhưng không tìm thấy API public tương ứng
   ├─ application.properties commit sẵn DB password/client id; nên externalize trước khi phát triển tiếp
   ├─ BanPickRoomStatus có CANCELLED nhưng chưa thấy flow sử dụng
   └─ Chưa thấy test bao phủ Guides, Tier Lists, Esports, Security/WebSocket end-to-end
```

**4. Bảng tổng hợp chức năng**

| Nhóm chức năng | Chức năng cụ thể | Mô tả | File/thư mục liên quan | API/route | Entity/database | Ghi chú |
|---|---|---|---|---|---|---|
| Xác thực | Đăng nhập Google | Frontend nhận Google credential, lưu localStorage, tự gắn Bearer token vào `/api/**` | `static/js/auth.js`, `security/*` | toàn bộ `/api/**` private | `users` | Guest UI dùng role `Custom` |
| Tài khoản | Xem/sửa hồ sơ cá nhân + nội dung của bạn | Đổi `displayName`, `level`; sidebar dashboard hiển thị số guide đã đăng và community tier list đã đăng của user hiện tại | `UserProfileController`, `UserProfileService`, `GuideRepository`, `TierListRepository`, `static/html/account.html` | `/api/users/me/profile`, `/api/users/me/content-summary` | `users`, `guides`, `tier_lists` | Content summary dùng security principal, chỉ tính guide published và tier list không official của chính user |
| Trang chủ | Community feed | Section homepage dùng 3 Tier List cộng đồng nổi bật; feed hỗn hợp cũ vẫn giữ cho backend | `HomeFeedController`, `HomeFeedService`, `TierListRatingRepository`, `TierListAdminRatingRepository`, `static/html/index.html` | `/api/home/feed`, `/api/home/community-tier-highlights` | `tier_lists`, `guides`, `tier_list_ratings`, `tier_list_admin_ratings` | `/api/home/community-tier-highlights` bỏ official, tránh trùng và có thể trả ít hơn 3 item |
| Wiki | Danh mục + chi tiết tướng | Trả catalog tướng, class/role/attribute, skill, matchup, guide liên quan; hero summary kèm `banPickScore` để Ban/Pick và admin dùng chung dữ liệu backend | `WikiController`, `WikiService`, `static/html/wiki.html` | `/api/wiki/heroes*` | `heroes`, `hero_skills`, `hero_matchups` | Tab khác trong `wiki.html` đang placeholder |
| Tier list | Meta chính thức | Official meta duoc generate tu `heroes.ban_pick_score`; public doc tren `tier-list.html`; Admin save/regenerate de persist vao `tier_lists.contentData`; trang goc van hien board official o tren va 6 community noi bat o duoi | `TierListController`, `AdminTierListController`, `TierListCommunityService`, `HeroContentDataService`, `static/html/tier-list.html`, `static/js/tier-list-app.js` | `/api/tier-lists/official`, `POST /api/admin/tier-lists/official/regenerate-from-hero-scores` | `tier_lists`, `heroes` | Rule tier: `>9 S`, `>7.5 A`, `>5 B`, `>2.5 C`, else `D`; role columns giu DSL/JGL/MID/ADL/SUP; section community tren trang goc van dung `GET /api/tier-lists/community` |
| Tier list | Community tier list | User tạo tier list; danh sách cộng đồng giữ 2 page `tier-list-all.html`, `tier-list-mine.html`; dropdown `Tier List` trên header giữ `Tier List Meta`, `Tất cả tier list`, `Tier list bản thân`; page `tier-list-recommended.html` đã bị xóa, còn route cũ chỉ redirect về `tier-list.html`; xem detail, rate, comment; Admin dùng cùng rating panel trên detail page để lưu admin rating | `TierListController`, `AdminTierListController`, `TierListCommunityService`, `TierListRatingRepository`, `TierListAdminRatingRepository`, `static/html/tier-list-community-shell.html`, `static/html/tier-list-all.html`, `static/html/tier-list-mine.html`, `static/js/tier-list-community-page.js`, `static/js/tier-list-app.js`, `static/html/tier-list-detail.html`, `tier-list-detail.js` | `/api/tier-lists/*`, `/api/admin/tier-lists/{id}/admin-rating` | `tier_lists`, `tier_list_ratings`, `tier_list_comments`, `tier_list_admin_ratings` | `/community` giữ logic highlight tối đa 6 item; `/community/all` trả full community list; `/me` chỉ trả tier list không official của user đang đăng nhập và guest sẽ nhận yêu cầu đăng nhập; API/DB không đổi |
| Guide | Danh sách/chi tiết guide | Search/filter theo `status`, `heroId`, `lane`, `category`, `search`, `sort` | `GuideController`, `GuideRepository`, `static/js/tactics-guides.js` | `GET /api/guides*` | `guides` | Filter đang chạy in-memory |
| Guide | Tạo guide | Tạo giáo án từ form frontend; lưu metadata + `contentData` JSON | `GuideController`, `static/html/create-guide.html` | `POST /api/guides` | `guides`, `users`, `heroes` | Không thấy update/delete/moderation |
| Esports | BXH public | Trả danh sách đội xếp theo Elo và recent matches | `EsportsController`, `static/html/esports.html` | `/api/esports/teams`, `/matches/recent` | `esports_teams`, `esports_matches` | `esports.html` hiện dùng team list; recent feed cần xác minh thêm |
| Ban/Pick | Room draft online | Create room, join, ready, roll side, start, confirm pick/ban, reorder lineup, next game, reset; UI có panel compact hiển thị tổng điểm đội hình và tỷ lệ thắng dự đoán từ các tướng đã pick | `BanPickRoomController`, `BanPickRoomService`, `BanPickRoomWebSocketController`, `ban-pick-free.html`, `ban-pick-standard.html`, `ban-pick-solo.html`, `ban-pick-shell.html`, `ban-pick.js` | `/api/ban-pick/rooms/*`, `/ws`, `/api/wiki/heroes` | `ban_pick_rooms`, `ban_pick_actions`, `ban_pick_room_participants`, `heroes` | Navbar dropdown là luồng chuyển mode chính; `ban-pick.html` chỉ còn redirect/fallback; không cộng điểm tướng bị ban |
| Ban/Pick | Lịch sử/BXH cá nhân | Lưu draft finished, ghi người thắng, profile, leaderboard | `BanPickHistoryController`, `BanPickHistoryService`, `ban-pick-profile.html`, `ban-pick-result.html` | `/api/ban-pick/history*`, `/leaderboard`, `/profile` | `draft_histories`, `player_stats` | Rating người chơi tăng/giảm ±15 |
| Admin | Quản lý user | List/search/filter user, chỉnh `name/avatar/role/status/note` | `AdminUserController`, `AdminUserService`, `static/html/admin.html` | `/api/admin/users*` | `users` | Admin không thể tự hạ role/khoá chính mình |
| Admin | Quản lý hero wiki | Sửa basic info, class, role, attribute, difficulty, `banPickScore`; admin panel validate min/max/2 chữ số thập phân trước khi lưu DB | `AdminWikiHeroController`, `AdminWikiHeroService`, `static/html/admin-heroes.html`, `static/js/admin-heroes.js` | `/api/admin/wiki/heroes*` | `heroes`, join tables hero_* | Có gợi ý role theo class; user thường không có quyền sửa |
| Admin | Quản lý hero attributes | Có 2 API admin attribute khác nhau | `AdminWikiAttributeController`, `HeroAttributeController`, `AdminWikiHeroService`, `HeroAttributeService` | `/api/admin/wiki/attributes*`, `/api/admin/attributes*` | `hero_attributes`, `hero_attribute_mapping` | Hành vi delete không nhất quán |
| Admin | Quản lý esports | CRUD team/match, bulk import, reset history, recalculation Elo | `EsportsAdminController`, `EsportsAdminService`, `EloCalculationService` | `/api/admin/esports/*` | `esports_teams`, `esports_matches` | Tự tính lại Elo sau mỗi thay đổi |

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
| POST | `/api/tier-lists` | `TierListController` | `TierListCommunityService` + `HeroContentDataService` | `title, description, isOfficial, contentData` | tier list map | Tạo tier list community; official save UI khong con dung import textarea |
| PUT | `/api/tier-lists/{id}` | `TierListController` | `TierListCommunityService` + `HeroContentDataService` | `title, description, contentData` | tier list map | Sửa tier list |
| PUT | `/api/tier-lists/{id}/admin-rate`, `/api/admin/tier-lists/{id}/admin-rating` | `TierListController`, `AdminTierListController` | `TierListCommunityService` | `ratingValue/stars/adminRating, note(optional)` | admin rating summary | Admin endorsement; detail page Admin star click gửi vào endpoint này |
| POST | `/api/admin/tier-lists/official/regenerate-from-hero-scores` | `AdminTierListController` | `TierListCommunityService` + `HeroContentDataService` | none | official tier list map | Admin generate/save official meta tu `heroes.ban_pick_score` vao `tier_lists.contentData` |
| GET, PUT | `/api/users/me/profile` | `UserProfileController` | `UserProfileService` | PUT: `displayName, level` | `UserProfileResponse` | Hồ sơ user |
| GET | `/api/users/me/content-summary` | `UserProfileController` | `UserProfileService` | none | `UserContentSummaryResponse` | Account dashboard panel `Nội dung của bạn`; đếm/list guide published và tier list cộng đồng thuộc user đang đăng nhập |
| GET | `/api/esports/teams` | `EsportsController` | repo trực tiếp | none | `EsportsTeam[]` | BXH public |
| GET | `/api/esports/matches/recent` | `EsportsController` | repo trực tiếp | `limit` | `RecentMatchDto[]` | Feed trận gần đây |
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
| GET, POST, PUT, DELETE | `/api/admin/esports/teams*`, `/api/admin/esports/teams/matches/bulk-import` | `EsportsAdminController` | `EsportsAdminService` | team body / raw text import | team/list/message | Quản lý/import team |
| GET, POST, PUT, DELETE | `/api/admin/esports/matches*`, `DELETE /api/admin/esports/matches` | `EsportsAdminController` | `EsportsAdminService` | match body | match/list/message | Quản lý/reset match history |

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
| `guides` | `id,title,coverImageUrl,status,category,lane,excerpt,viewCount,readingTimeMinutes,author_id,hero_id,contentData,publishedAt` | N-1 `users`, N-1 `heroes`; có 1-N tới `GuideItem/GuideArcana/GuideEnchantment` | `GuideRepository` | Giáo án chiến thuật |
| `bang_ngoc`, `phu_hieu`, `vat_pham` | slug, tên, mô tả, stats JSON, ảnh | Được link qua `huong_dan_*` | `ArcanaRepository`, `EnchantmentRepository`, `ItemRepository` | Dữ liệu support cho guide; mức sử dụng runtime cần xác minh thêm |
| `huong_dan_ngoc`, `huong_dan_phu_hieu`, `huong_dan_vat_pham` | FK guide + item/arcana/enchantment + order/count | N-1 về `guides` và bảng support | `GuideArcanaRepository`, `GuideEnchantmentRepository`, `GuideItemRepository` | Schema guide chuẩn hóa; hiện chưa thấy controller dùng rõ ràng |
| `tier_lists` | `id,title,author_id,contentData,description,isOfficial,adminRating,createdAt,updatedAt` | N-1 `users`; 1-N `tier_list_ratings/comments/admin_ratings` | `TierListRepository` | Meta chính thức và community tier list |
| `tier_list_ratings` | `id,tier_list_id,user_id(stored email),stars,createdAt,updatedAt` | N-1 `tier_lists` | `TierListRatingRepository` | Rating user |
| `tier_list_comments` | `id,tier_list_id,user_id,content,createdAt,updatedAt` | N-1 `tier_lists`, N-1 `users` | `TierListCommentRepository` | Bình luận community |
| `tier_list_admin_ratings` | `id,tier_list_id,admin_user_id,ratingValue,note` | N-1 `tier_lists`, N-1 `users` | `TierListAdminRatingRepository` | Đánh giá admin |
| `esports_teams` | `id,teamCode,teamName,logoUrl,region,score,gameWins,gameLosses,matchWins,matchLosses` | Không FK trực tiếp tới `esports_matches`; liên hệ bằng `teamCode` | `EsportsTeamRepository` | BXH esports |
| `esports_matches` | `id,matchDate,team1Code,team2Code,score1,score2,tier,stage` | Quan hệ logic qua `teamCode` | `EsportsMatchRepository` | Lịch sử trận và input tính Elo |
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
  - Hoàn thiện hoặc loại bỏ các placeholder chưa có backend thật: tab `spells/items/arcana/enchantments` trong `wiki.html`, `guide admin/moderation`.
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
|  |- `account.css` own `account.html`; `wiki.css` own `wiki.html`; `esports.css` own `esports.html`
|  |- `guides.css` own `giao-an.html`, `tactics-guides.html`, `create-guide.html`, `guide-detail.html`
|  |- `admin.css` own `admin.html`, `admin-heroes.html`, `admin-attributes.html`
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
|  |- Tong hop draft esports da luu trong DB de thong ke hero PICK/BAN theo giai dau
|  |- Hien thi bang Hero Statistics gom Picks / Blue side / Red side / Bans / Picks & Bans
|  |- Giu top hero bi cam nhieu nhat va top hero bi BLUE side cam nhieu nhat de doc nhanh
|  `- Reuse `heroes`, `esports_matches`, `esports_match_games`, `esports_match_draft_actions`; khong tao bang generic moi
|- Related users
|  `- Public users xem du lieu thong ke Esports Data
|- Main HTML, JavaScript, and CSS files
|  |- `demo/src/main/resources/static/html/esports-data.html`
|  |- Main JavaScript: inline script trong `esports-data.html`
|  |- Main CSS: `demo/src/main/resources/static/css/esports.css`
|  `- Header / Navigation: `demo/src/main/resources/static/html/header.html`, `demo/src/main/resources/static/js/header-loader.js`
|- Related controller, service, entity, and repository files
|  |- Controller: `demo/src/main/java/com/example/demo/controller/EsportsController.java`
|  |- Controller: `demo/src/main/java/com/example/demo/controller/StaticPageRedirectController.java`
|  |- Service: `demo/src/main/java/com/example/demo/service/EsportsDataService.java`
|  |- Util: `demo/src/main/java/com/example/demo/util/EsportsTournamentCatalog.java`
|  |- Repository: `demo/src/main/java/com/example/demo/repository/EsportsMatchDraftActionRepository.java`
|  |- DTO: `demo/src/main/java/com/example/demo/dto/esports/EsportsHeroPickStatAggregate.java`
|  |- DTO: `demo/src/main/java/com/example/demo/dto/esports/EsportsHeroBanStatResponse.java`
|  |- DTO: `demo/src/main/java/com/example/demo/dto/esports/EsportsHeroStatResponse.java`
|  |- DTO: `demo/src/main/java/com/example/demo/dto/esports/EsportsTournamentOptionResponse.java`
|  |- Entity reused: `demo/src/main/java/com/example/demo/entity/Hero.java`
|  |- Entity reused: `demo/src/main/java/com/example/demo/entity/EsportsMatch.java`
|  |- Entity reused: `demo/src/main/java/com/example/demo/entity/EsportsMatchGame.java`
|  `- Entity reused: `demo/src/main/java/com/example/demo/entity/EsportsMatchDraftAction.java`
|- API endpoints
|  |- `GET /api/esports/data/tournaments`
|  |- `GET /api/esports/data/hero-stats?tournamentName=...`
|  |- `GET /api/esports/data/top-banned-heroes?tournamentName=...&limit=5`
|  `- `GET /api/esports/data/top-blue-banned-heroes?tournamentName=...&limit=5`
|- Database table or entity
|  |- `heroes`
|  |- `esports_matches`
|  |- `esports_match_games`
|  `- `esports_match_draft_actions`; khong doi schema, migration, hay seed
|- Header / Navigation
|  |- Shared header them link `Esports Data` gan muc `Esports`
|  `- Active state duoc nhan dien cho route `/esports/data` va file `esports-data.html`
|- Routing / Static Pages
|  |- Them page `esports-data.html`
|  |- Them route dep `/esports/data` -> redirect `/html/esports-data.html`
|  `- Legacy top-level `/esports-data.html` duoc redirect vao static page moi
|- Main workflow
|  |- User mo `/esports/data` hoac `esports-data.html`
|  |- Frontend load danh sach giai dau tu `GET /api/esports/data/tournaments`
|  |- Neu chua chon giai dau thi frontend goi stats voi toan bo draft data hien co
|  |- `GET /api/esports/data/hero-stats` aggregate PICK va BAN rieng trong repository roi merge theo `hero_id` trong service de tranh nhan doi count
|  |- Hero Statistics tinh `pickWins` bang cach so `draft_actions.team_id` voi `esports_match_games.winner_team_id`; `pickLosses`, `blueLosses`, `redLosses` duoc suy ra tu tong PICK tru di so tran thang
|  |- Bang Hero Statistics tren `esports-data.html` co loading state, empty state, filter `tournamentName`, va sort client-side theo `Picks Σ`, `Picks WR`, `Bans Σ`, `Picks & Bans Σ`
|  |- `GET /api/esports/data/top-banned-heroes` chi tinh `action_type = BAN`
|  |- `GET /api/esports/data/top-blue-banned-heroes` chi tinh `action_type = BAN` va `team_side = BLUE`
|  |- Filter `tournamentName` map qua `tier -> tournamentName` bang `EsportsTournamentCatalog` vi schema hien tai chua co cot `tournament_name`
|  `- Empty state hien khi chua co draft data hoac khong co PICK/BAN phu hop
|- Access permissions
|  `- Tat ca `GET /api/esports/**` va page public deu cho guest xem theo `SecurityConfig`
`- Risk notes or manual testing areas
   |- Can verify count group-by dung tren du lieu DB that su, nhat la case nhieu game trong cung mot match va khong bi double-count khi merge PICK/BAN
   |- Can verify cong thuc WR khong bao gio ra `NaN/Infinity` khi mau so bang 0
   |- Can test default scope khi bo trong `tournamentName`: hien dung toan bo draft data, khong tu dong khoa vao giai moi nhat
   |- Can test `tournamentName` invalid tra 400 va UI khong crash
   |- Can test empty state khi chua co `esports_match_draft_actions`
   |- Can test sort Hero Statistics va layout bang data-dense tren desktop/mobile
   `- Can test trang `esports.html`, Elo ranking, match feed, va draft admin APIs cu khong bi anh huong
```

**14. Esports Match Game Draft History**

```text
Esports Match Game Draft History
|- Feature name
|  `- Esports Match Game Draft History
|- Purpose
|  |- Luu lich su draft esports theo tung van thuoc mot `esports_matches` series da co san
|  |- Tach ro cap `match -> game -> draft action` de quan ly du lieu ban/pick theo step
|  `- Reuse `heroes`, `esports_teams`, `esports_matches` thay vi tao bang generic moi
|- Related users
|  |- Admin users quan ly du lieu tran, van, draft action
|  `- Public users co the doc danh sach van va draft history neu frontend/public API can hien thi
|- Main HTML, JavaScript, and CSS files
|  |- `demo/src/main/resources/static/html/esports.html`
|  |- Main JavaScript: `N/A`
|  `- Main CSS: `N/A`
|- Related controller, service, entity, and repository files
|  |- Controller: `demo/src/main/java/com/example/demo/controller/EsportsAdminController.java`
|  |- Controller: `demo/src/main/java/com/example/demo/controller/EsportsController.java`
|  |- Service: `demo/src/main/java/com/example/demo/service/EsportsDraftService.java`
|  |- Entity: `demo/src/main/java/com/example/demo/entity/EsportsMatchGame.java`
|  |- Entity: `demo/src/main/java/com/example/demo/entity/EsportsMatchDraftAction.java`
|  |- Entity reused: `demo/src/main/java/com/example/demo/entity/EsportsMatch.java`
|  |- Entity reused: `demo/src/main/java/com/example/demo/entity/EsportsTeam.java`
|  |- Entity reused: `demo/src/main/java/com/example/demo/entity/Hero.java`
|  |- Repository: `demo/src/main/java/com/example/demo/repository/EsportsMatchGameRepository.java`
|  `- Repository: `demo/src/main/java/com/example/demo/repository/EsportsMatchDraftActionRepository.java`
|- API endpoints
|  |- `GET /api/esports/matches/{matchId}/games`
|  |- `GET /api/esports/games/{gameId}/draft-actions`
|  |- `GET /api/admin/esports/matches/{matchId}/games`
|  |- `POST /api/admin/esports/matches/{matchId}/games`
|  |- `GET /api/admin/esports/games/{gameId}`
|  |- `PUT /api/admin/esports/games/{gameId}`
|  |- `DELETE /api/admin/esports/games/{gameId}`
|  |- `GET /api/admin/esports/games/{gameId}/draft-actions`
|  |- `POST /api/admin/esports/games/{gameId}/draft-actions`
|  |- `PUT /api/admin/esports/draft-actions/{actionId}`
|  `- `DELETE /api/admin/esports/draft-actions/{actionId}`
|- Database / Entity / Migration
|  |- New table/entity: `esports_match_games`
|  |- New table/entity: `esports_match_draft_actions`
|  |- Reused table/entity: `esports_matches`
|  |- Reused table/entity: `esports_teams`
|  |- Reused table/entity: `heroes`
|  |- SQL script: `demo/sql/esports_match_game_draft_history.sql`
|  |- `esports_match_games` dung FK `match_id -> esports_matches.id`
|  |- `esports_match_games` dung FK `blue_team_id/red_team_id/winner_team_id -> esports_teams.id`
|  |- `esports_match_draft_actions` dung FK `game_id -> esports_match_games.id` voi cascade delete
|  |- `esports_match_draft_actions` dung FK `team_id -> esports_teams.id`
|  |- `esports_match_draft_actions` dung FK `hero_id -> heroes.id`
|  |- Unique game: `(match_id, game_number)`
|  |- Unique draft action: `(game_id, step_number)` va `(game_id, hero_id)`
|  `- Project van dung database Spring Boot hien tai; khong them `CREATE DATABASE`
|- Esports Ranking
|  |- Elo/ranking workflow cu van tiep tuc doc `esports_matches` va `teamCode`, khong refactor model ranking
|  |- Draft history moi bo sung ben canh ranking, khong thay doi `EloCalculationService`
|  `- Neu xoa `esports_matches` thi `esports_match_games` va `esports_match_draft_actions` se bi xoa theo schema moi
|- Admin Esports
|  |- Admin tiep tuc CRUD team/match/import/reset Elo nhu truoc qua `EsportsAdminService`
|  |- Admin co them CRUD van dau va draft action qua `EsportsDraftService`
|  |- Validate service layer:
|  |- `gameNumber` khong duoc trung trong cung match
|  |- `stepNumber` khong duoc trung trong cung game
|  |- `heroId` khong duoc ban/pick trung trong cung game
|  |- `teamId` phai la blue team hoac red team cua game
|  `- `teamSide` phai khop voi team duoc gui len
|- Main workflow
|  |- Admin tao mot `esports_match_game` cho `esports_matches` da co san
|  |- Admin chon `blueTeamId` va `redTeamId` trong 2 doi cua match hien co
|  |- Admin them tung `esports_match_draft_action` theo `stepNumber`
|  |- Public/Admin GET draft actions -> backend tra ve danh sach theo thu tu `stepNumber`
|  `- Xoa game -> draft actions cua game do bi xoa theo cascade
|- Access permissions
|  |- `GET /api/esports/**` lien quan draft history la public read-only
|  `- `POST/PUT/DELETE /api/admin/esports/**` yeu cau role `ADMIN` theo `SecurityConfig`
`- Risk notes or manual testing areas
   |- Can test match cu van import/reset Elo binh thuong sau khi them game/draft history
   |- Can test delete match/game de xac nhan cascade khong de lai orphan draft actions
   |- Can test hero/team khong hop le, trung step, trung hero deu bi reject o service/API
   `- Hien tai chua them UI admin/public rieng cho draft history; backend foundation/API da san sang cho buoc sau
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
|  `- Khong con nut `Đổi chế độ` / `Thoát` chi de quay ve `ban-pick.html`; user doi mode qua navbar dropdown, panel `Đánh giá đội hình` cap nhat realtime khi pick/huy/reset
|- Ban/Pick Standard Mode
|  |- Wrapper page: `demo/src/main/resources/static/html/ban-pick-standard.html`
|  |- Hero pool van lay tu `GET /api/wiki/heroes`; moi hero co `banPickScore` tu backend/database
|  `- Khong con nut `Đổi chế độ` / `Thoát` chi de quay ve `ban-pick.html`; luong pick/ban, timer, reset giu nguyen, panel `Đánh giá đội hình` cap nhat realtime khi pick/huy/reset
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
|  |- `/ban-pick?room={roomCode}` va `/ban-pick.html?room={roomCode}` redirect vao thang Solo room
|  |- `/html/ban-pick.html` khong con render chooser page; chi redirect legacy theo `room` / `mode` hoac ve Free Mode
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
|  |- Public users xem `Tier List Meta`, `Tất cả tier list`, `tier-list-detail`
|  `- Authenticated users xem them `Tier list bản thân` va tao/xoa tier list cua minh
|- Main HTML, JavaScript, and CSS files
|  |- `demo/src/main/resources/static/html/header.html`
|  |- `demo/src/main/resources/static/html/tier-list.html`
|  |- `demo/src/main/resources/static/html/tier-list-community-shell.html`
|  |- `demo/src/main/resources/static/html/tier-list-all.html`
|  |- `demo/src/main/resources/static/html/tier-list-mine.html`
|  |- `demo/src/main/resources/static/html/tier-list-detail.html`
|  |- `demo/src/main/resources/static/js/tier-list-app.js`
|  |- `demo/src/main/resources/static/js/tier-list-community-page.js`
|  |- `demo/src/main/resources/static/js/header-loader.js`
|  |- `demo/src/main/resources/static/css/style.css` (shared/base)
|  `- `demo/src/main/resources/static/css/tier-list.css` (official/community/detail/card UI)
|- CSS ownership
|  |- `style.css` giu token, shared card/button/modal/header va hero-pool selectors duoc tai su dung boi modal tao tier list
|  `- `tier-list.css` own official meta board, community grid/card, detail panel, rating/comment/download UI
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
|  `- `tier-list-mine.html` van dung `GET /api/tier-lists/me`; guest thay message dang nhap, khong crash; card/detail/rating summary deu hien average/count tong da gom admin rating, con admin badge van tach rieng
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
|  `- Community/rating/comment/download APIs khong doi route hay permission; field average/count hien tai van la tong gom user + admin rating, `adminRating/adminRatingDetail` van tach rieng
|- Database table or entity
|  |- `tier_lists`
|  |- `tier_list_ratings`
|  |- `tier_list_comments`
|  `- `tier_list_admin_ratings`; khong doi schema/entity/migration, `tier_lists.adminRating` van giu lam legacy fallback va chi duoc dung neu chua co row admin rating de tranh double-count
|- Main workflow
|  |- User mo `/tier-list` hoac `tier-list.html` -> frontend goi `GET /api/tier-lists/official` -> render official meta duoc luu trong DB hoac preview generate neu chua co official row -> sau do goi `GET /api/tier-lists/community` de render toi da 6 community card
|  |- Mo dropdown `Tier List` tren header -> chon `Tier List Meta` -> route `/tier-list` -> redirect sang `tier-list.html` -> frontend goi `GET /api/tier-lists/community` de render toi da 6 community card
|  |- Chon `Tất cả tier list` -> route `/tier-list/all` -> redirect sang `tier-list-all.html` -> frontend fetch `/api/tier-lists/community/all`
|  |- Chon `Tier list bản thân` -> route `/tier-list/mine` -> redirect sang `tier-list-mine.html` -> frontend fetch `/api/tier-lists/me`
|  |- Truy cap route cu `/tier-list/recommended` hoac URL HTML cu -> redirect sang `tier-list.html` -> frontend fetch `/api/tier-lists/community`
|  |- Admin sua `banPickScore` trong `admin-heroes.html` -> quay lai `tier-list.html` -> bam `Luu Meta Chinh` -> backend generate official contentData tu `heroes.ban_pick_score` va save vao `tier_lists.contentData`
|  |- `tier-list-community-page.js` load shell chung, doc `body[data-community-view]`, set title/subtitle, roi nap `tier-list-app.js`
|  `- `tier-list-app.js` tai su dung renderer card/rating/delete/modal tao tier list; official page va 2 page community dung chung renderer card, con `tier-list-detail.js` dong bo lai average/count tong ngay sau khi Admin update rating
|- Access permissions
|  |- Public xem official, all, detail, rating summary, export
|  |- `mine` yeu cau user da dang nhap; guest se thay message `Vui lòng đăng nhập để xem tier list của bạn.`
|  `- Quyen xoa tier list van theo owner/Admin nhu truoc
`- Risk notes or manual testing areas
   |- Can test official board boundary `9`, `7.5`, `5`, `2.5`, score null, va case hero thieu `Primary Role`
   |- Can test `tier-list.html` khong con import textarea/nut `Xem truoc`/nut `Ap dung vao Tier List`, nhung van render official meta + toi da 6 community card
   |- Can test case `user rating 4 + admin rating 3` de card/detail/summary hien average `3.5` va count `2`
   |- Can test modal `Tạo Tier List` tren official page va 2 page community de chac rang hero pool community van load dung
   `- Can test save/reload official meta va verify homepage community highlights, account dashboard, Ban/Pick score panel khong bi anh huong sau khi tach `tier-list.css`

**12. Tier List Saved Reference**

Tier List Saved Reference
|- Community Tier List
|  |- User dang nhap co the `Luu` community tier list theo kieu bookmark/reference, khong clone va khong snapshot
|  |- Saved item luon render du lieu live tu `tier_list_id` goc: title, `contentData`, author, `createdAt`, aggregate average/count tong da gom user + admin rating, admin badge/detail rieng, comment count
|  |- Neu tier list goc thay doi noi dung, review, rating hoac admin rating thi danh sach da luu tu dong phan anh thay doi moi
|  |- `tier-list.html`, `tier-list-all.html`, `tier-list-mine.html` va `tier-list-detail.html` deu co the hien state `Luu` / `Bo luu`
|  `- `tier-list-mine.html` duoc tach 2 section ro nghia: `Tier list toi tao` va `Tier list da luu`
|- Authentication / User Profile
|  |- `POST /api/tier-lists/{id}/save`, `DELETE /api/tier-lists/{id}/save`, `GET /api/tier-lists/saved` deu yeu cau dang nhap
|  |- Guest bam `Luu` thi frontend hien message dang nhap, khong crash
|  |- `/api/tier-lists/me` tiep tuc chi co nghia la tier list user tu tao, khong tron voi danh sach da luu
|  `- Guest vao `tier-list-mine.html` van khong crash; shell hien canh bao dang nhap thay vi render loi
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
|  |- `tier-list-detail.html` bo sung action `Luu` / `Bo luu` ben canh `Tai anh`
|  `- Khong doi route dep `/tier-list`, `/tier-list/all`, `/tier-list/mine`; route cu `/tier-list/recommended` chi con vai tro redirect
|- Bảng tổng hợp chức năng Tier List
|  |- Meta chinh thuc: khong doi logic import/export/admin save
|  |- Community tier list: bo sung bookmark/reference theo user, card van giu metadata goc cua owner va hien aggregate average/count tong da gom admin rating
|  `- Tier list ban than: gom 2 nguon du lieu tach biet `GET /me` (toi tao) va `GET /saved` (toi da luu)
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
   |- Can test guest `POST /api/tier-lists/{id}/save` nhan 401 va guest `GET /api/tier-lists/saved` nhan 401
   `- Can test section `Tier list da luu` tren `tier-list-mine.html` xoa item ngay sau thao tac `Bo luu` ma khong anh huong tier list goc
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
   |- Can verify alias seed cho hero ten Viet hoa/accent nhu `Triệu Vân`, `Ngộ Không`, `Điêu Thuyền` va ten variant nhu `Riktor`
   |- Can test hero khong co score hoac score null van hien `0`, khong crash frontend
   |- Can test Admin sua `10`, `9.84`, `0`, input sai dinh dang, va case reload sau save
   |- Can test Free/Standard/Solo deu cap nhat dung khi pick, huy preview, reset, next game, load room
   `- Can test layout mobile/desktop de panel moi khong che bottom action bar va khong lam day top status
```
