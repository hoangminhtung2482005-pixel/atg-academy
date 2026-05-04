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
│  ├─ Quản lý tài khoản; file: UserProfileController, UserProfileService, static/html/account.html
│  ├─ Trang chủ feed; file: HomeFeedController, HomeFeedService, static/html/index.html
│  ├─ Wiki tướng; file: WikiController, WikiService, static/html/wiki.html
│  ├─ Tier List chính thức; file: TierListController, TierListCommunityService, static/html/tier-list.html
│  ├─ Tier List cộng đồng: tạo/xem/đánh giá/bình luận; file: TierListController, static/js/tier-list-app.js, tier-list-detail.js
│  ├─ Guides: list/filter/detail/tạo; file: GuideController, static/html/giao-an.html, tactics-guides.html, create-guide.html
│  ├─ Esports public: BXH đội và match feed; file: EsportsController, static/html/esports.html
│  ├─ Ban/Pick Lab offline UI; file: static/html/ban-pick.html
│  └─ Ban/Pick 1v1 online: room/join/realtime/history/profile/leaderboard; file: BanPickRoomController, BanPickHistoryController, BanPickRoomWebSocketController
├─ Chức năng quản trị
│  ├─ Dashboard admin; file: static/html/admin.html, admin-heroes.html
│  ├─ Quản lý người dùng; file: AdminUserController, AdminUserService
│  ├─ Quản lý wiki hero; file: AdminWikiHeroController, AdminWikiHeroService
│  ├─ Quản lý hero attributes; file: AdminWikiAttributeController, HeroAttributeController
│  ├─ Quản lý tier list chính thức và admin rating; file: TierListController, AdminTierListController
│  ├─ Quản lý esports teams/matches/import/reset Elo; file: EsportsAdminController, EsportsAdminService
│  └─ Không tìm thấy trong codebase: guide moderation/admin guide hoàn chỉnh
├─ Chức năng nghiệp vụ chính
│  ├─ Wiki Hero
│  │  ├─ Mục đích: catalog hero, role/class/attribute, skill, matchup, guide liên quan
│  │  ├─ File: WikiService, HeroRepository, HeroSkillRepository, HeroMatchupRepository
│  │  ├─ API: GET /api/wiki/heroes, GET /api/wiki/heroes/{slug}
│  │  ├─ DB: heroes, hero_roles, hero_classes, hero_attributes, hero_skills, hero_matchups
│  │  └─ Luồng: slug -> hero + relations -> skills + matchups + related guides -> HeroDetailDto
│  ├─ Tier List
│  │  ├─ Mục đích: meta chính thức + community tier list
│  │  ├─ File: TierListController, TierListCommunityService, HeroContentDataService
│  │  ├─ API: /api/tier-lists/*
│  │  ├─ DB: tier_lists, tier_list_ratings, tier_list_comments, tier_list_admin_ratings
│  │  └─ Luồng: contentData JSON -> normalize/enrich hero refs -> response + rating/comment summary
│  ├─ Guide
│  │  ├─ Mục đích: tạo và đọc giáo án chiến thuật
│  │  ├─ File: GuideController, GuideRepository, static/html/create-guide.html
│  │  ├─ API: GET/POST /api/guides, GET /api/guides/{id}
│  │  ├─ DB: guides; có thêm bang_ngoc/phu_hieu/vat_pham và huong_dan_* nhưng chưa thấy dùng runtime rõ ràng
│  │  └─ Luồng: frontend gửi contentData JSON -> backend lưu guide + author + hero + metadata
│  ├─ Home Feed
│  │  ├─ Mục đích: ghép tier list cộng đồng mới + guide mới publish lên trang chủ
│  │  ├─ File: HomeFeedController, HomeFeedService
│  │  ├─ API: GET /api/home/feed
│  │  └─ Luồng: lấy 6 tier list + 6 guides -> merge -> sort theo createdAt
│  ├─ Esports Ranking
│  │  ├─ Mục đích: xếp hạng đội theo Elo, quản trị trận đấu
│  │  ├─ File: EsportsController, EsportsAdminService, EloCalculationService, EsportsDataSeeder
│  │  ├─ API: /api/esports/*, /api/admin/esports/*
│  │  ├─ DB: esports_teams, esports_matches
│  │  └─ Luồng: seed nếu DB trống -> thêm/sửa/xóa match -> tính lại Elo toàn bộ
│  └─ Ban/Pick Online
│     ├─ Mục đích: solo draft online BO1/3/5/7 có realtime
│     ├─ File: BanPickRoomService, BanPickHistoryService, BanPickRoomBroadcaster, BanPickPresenceEventListener
│     ├─ API: /api/ban-pick/rooms/*, /api/ban-pick/history*, /api/ban-pick/profile, /api/ban-pick/leaderboard
│     ├─ WebSocket: /ws, /app/ban-pick/{roomCode}/*, /topic/ban-pick/{roomCode}
│     ├─ DB: ban_pick_rooms, ban_pick_room_participants, ban_pick_actions, draft_histories, player_stats
│     └─ Luồng: create room -> join -> roll side -> ready -> 15 phase draft -> lineup adjustment -> finish -> history/stats
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
│  └─ Static routes: StaticPageRedirectController redirect /, /guides, /tier-list, /ban-pick/* sang static/html
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
| Tài khoản | Xem/sửa hồ sơ cá nhân | Đổi `displayName`, `level` | `UserProfileController`, `UserProfileService`, `static/html/account.html` | `/api/users/me/profile` | `users` | `level` chỉ cho `Normal/Vip` |
| Trang chủ | Community feed | Ghép tier list cộng đồng mới nhất và guide đã publish | `HomeFeedController`, `HomeFeedService`, `static/html/index.html` | `/api/home/feed` | `tier_lists`, `guides` | Không thấy endpoint `/api/meta` |
| Wiki | Danh mục + chi tiết tướng | Trả catalog tướng, class/role/attribute, skill, matchup, guide liên quan | `WikiController`, `WikiService`, `static/html/wiki.html` | `/api/wiki/heroes*` | `heroes`, `hero_skills`, `hero_matchups` | Tab khác trong `wiki.html` đang placeholder |
| Tier list | Meta chính thức | Admin tạo/sửa tier list official; public đọc | `TierListController`, `HeroContentDataService`, `static/js/tier-list-app.js` | `/api/tier-lists/official`, `POST /api/tier-lists` | `tier_lists` | `contentData` lưu JSON |
| Tier list | Community tier list | User tạo tier list, xem detail, rate, comment | `TierListController`, `TierListCommunityService`, `tier-list-detail.js` | `/api/tier-lists/*` | `tier_lists`, `tier_list_ratings`, `tier_list_comments`, `tier_list_admin_ratings` | Có endpoint legacy `/rate`, `/admin-rate` |
| Guide | Danh sách/chi tiết guide | Search/filter theo `status`, `heroId`, `lane`, `category`, `search`, `sort` | `GuideController`, `GuideRepository`, `static/js/tactics-guides.js` | `GET /api/guides*` | `guides` | Filter đang chạy in-memory |
| Guide | Tạo guide | Tạo giáo án từ form frontend; lưu metadata + `contentData` JSON | `GuideController`, `static/html/create-guide.html` | `POST /api/guides` | `guides`, `users`, `heroes` | Không thấy update/delete/moderation |
| Esports | BXH public | Trả danh sách đội xếp theo Elo và recent matches | `EsportsController`, `static/html/esports.html` | `/api/esports/teams`, `/matches/recent` | `esports_teams`, `esports_matches` | `esports.html` hiện dùng team list; recent feed cần xác minh thêm |
| Ban/Pick | Room draft online | Create room, join, ready, roll side, start, confirm pick/ban, reorder lineup, next game, reset | `BanPickRoomController`, `BanPickRoomService`, `BanPickRoomWebSocketController`, `ban-pick.html` | `/api/ban-pick/rooms/*`, `/ws` | `ban_pick_rooms`, `ban_pick_actions`, `ban_pick_room_participants` | Module phức tạp nhất |
| Ban/Pick | Lịch sử/BXH cá nhân | Lưu draft finished, ghi người thắng, profile, leaderboard | `BanPickHistoryController`, `BanPickHistoryService`, `ban-pick-profile.html`, `ban-pick-result.html` | `/api/ban-pick/history*`, `/leaderboard`, `/profile` | `draft_histories`, `player_stats` | Rating người chơi tăng/giảm ±15 |
| Admin | Quản lý user | List/search/filter user, chỉnh `name/avatar/role/status/note` | `AdminUserController`, `AdminUserService`, `static/html/admin.html` | `/api/admin/users*` | `users` | Admin không thể tự hạ role/khoá chính mình |
| Admin | Quản lý hero wiki | Sửa basic info, class, role, attribute, difficulty | `AdminWikiHeroController`, `AdminWikiHeroService`, `static/html/admin-heroes.html` | `/api/admin/wiki/heroes*` | `heroes`, join tables hero_* | Có gợi ý role theo class |
| Admin | Quản lý hero attributes | Có 2 API admin attribute khác nhau | `AdminWikiAttributeController`, `HeroAttributeController`, `AdminWikiHeroService`, `HeroAttributeService` | `/api/admin/wiki/attributes*`, `/api/admin/attributes*` | `hero_attributes`, `hero_attribute_mapping` | Hành vi delete không nhất quán |
| Admin | Quản lý esports | CRUD team/match, bulk import, reset history, recalculation Elo | `EsportsAdminController`, `EsportsAdminService`, `EloCalculationService` | `/api/admin/esports/*` | `esports_teams`, `esports_matches` | Tự tính lại Elo sau mỗi thay đổi |

**5. Bảng API**

| Method | Endpoint | Controller/Handler | Service | Request input suy luận được | Response output suy luận được | Mục đích |
|---|---|---|---|---|---|---|
| GET | `/api/home/feed` | `HomeFeedController` | `HomeFeedService` | none | `HomeFeedItemResponse[]` | Feed trang chủ |
| GET | `/api/wiki/heroes` | `WikiController` | `WikiService` | none | `HeroSummaryDto[]` | Catalog tướng |
| GET | `/api/wiki/heroes/{slug}` | `WikiController` | `WikiService` | `slug` | `HeroDetailDto` | Chi tiết tướng |
| GET | `/api/guides` | `GuideController` | controller trực tiếp | `status, heroId, category, lane, search, sort` | list guide summary/detail map | List/filter guide |
| GET | `/api/guides/{id}` | `GuideController` | controller trực tiếp | `id` | guide map | Chi tiết guide; tăng view |
| POST | `/api/guides` | `GuideController` | controller trực tiếp | `title, heroId/heroName, lane, category, excerpt, coverImageUrl, status, contentData` | created guide map | Tạo guide |
| GET | `/api/tier-lists/official` | `TierListController` | `TierListCommunityService` | none | tier list map hoặc `{exists:false}` | Tier list chính thức |
| GET | `/api/tier-lists/community` | `TierListController` | `TierListCommunityService` | none | `List<Map>` | Tier list cộng đồng |
| GET | `/api/tier-lists/{id}` | `TierListController` | `TierListCommunityService` | `id` | tier list map | Detail tier list |
| GET, POST | `/api/tier-lists/{id}/comments` | `TierListController` | `TierListCommunityService` | POST: `content/comment` | GET: list comment; POST: comment map | Đọc/gửi bình luận |
| GET | `/api/tier-lists/{id}/ratings`, `/api/tier-lists/{id}/ratings/summary` | `TierListController` | `TierListCommunityService` | `id` | summary map | Lấy thống kê rating |
| POST | `/api/tier-lists/{id}/ratings`, `/api/tier-lists/{id}/rate` | `TierListController` | `TierListCommunityService` | `ratingValue/stars` | summary map | User chấm điểm |
| POST | `/api/tier-lists` | `TierListController` | `TierListCommunityService` + `HeroContentDataService` | `title, description, isOfficial, contentData` | tier list map | Tạo tier list; admin có thể tạo official |
| PUT | `/api/tier-lists/{id}` | `TierListController` | `TierListCommunityService` + `HeroContentDataService` | `title, description, contentData` | tier list map | Sửa tier list |
| PUT | `/api/tier-lists/{id}/admin-rate`, `/api/admin/tier-lists/{id}/admin-rating` | `TierListController`, `AdminTierListController` | `TierListCommunityService` | `ratingValue/stars/adminRating, note` | admin rating summary | Admin endorsement |
| GET, PUT | `/api/users/me/profile` | `UserProfileController` | `UserProfileService` | PUT: `displayName, level` | `UserProfileResponse` | Hồ sơ user |
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
| GET, PUT | `/api/admin/wiki/heroes`, `/api/admin/wiki/heroes/{id}` | `AdminWikiHeroController` | `AdminWikiHeroService` | update basic info | hero list/detail DTO | Quản lý hero |
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
| `heroes` | `id,name,slug,title,avatarUrl,portraitUrl,bannerUrl,heroClass,difficulty,description,lore` | N-N `hero_roles`, `hero_classes`, `hero_attributes`; 1-N `hero_skills`, `hero_matchups`; N-1 từ `guides` | `HeroRepository` | Wiki hero, tier list hero refs, guide link, ban/pick hero pool |
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