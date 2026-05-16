# Agent Instructions

## Project Context

- Backend uses Spring Boot with Controller, Service, and Repository layers.
- Frontend uses HTML, Tailwind CSS, and vanilla JavaScript.
- Main product areas are Wiki, Tier List, Guides, Esports, and Ban/Pick.
- `docs/so-do-tu-duy.md` is the project mind map and must reflect real system behavior.

## Priority Rules

1. Follow the user's explicit request.
2. Preserve existing functionality and change behavior only when the task explicitly requires it.
3. Treat actual code and runtime behavior as the source of truth when documentation is outdated.
4. Prefer existing project structure, naming, patterns, and dependencies over new patterns.
5. Keep the implementation scoped to the requested change.

## Core Workflow

- Inspect the relevant files before editing.
- Reuse existing Spring Boot controller, service, repository, entity, and DTO patterns for backend work.
- Reuse existing HTML, Tailwind, CSS, and vanilla JavaScript patterns for frontend work.
- Keep unrelated code unchanged.
- Modify only the files required to complete the task.
- Add dependencies only when the task explicitly requires them and existing dependencies cannot satisfy the requirement.
- Keep database schema, entities, migrations, and seed data unchanged for UI-only tasks.
- Commit changes only when the user explicitly asks for a commit.

## Database Work

When the user explicitly requests a database change, or when the requested implementation requires a database, entity, schema, migration, seed, or cleanup change, the agent must handle the database work end-to-end.

Required database workflow:

1. Inspect the current code, entity, repository, service, controller, DTO, SQL, and documentation related to the requested database change.
2. Read the active database configuration from `application.properties` or the relevant runtime config.
3. Identify the local database target before running any SQL.
4. Create or update the required migration SQL in the project SQL folder.
5. Review existing migrations to avoid duplicate columns, duplicate tables, conflicting seeds, or repeated destructive actions.
6. Backup the local database before destructive actions such as `DROP TABLE`, `DROP COLUMN`, mass `DELETE`, or destructive data migration, when database access is available.
7. Apply the migration directly to the local database when credentials, tools, and permissions are available.
8. Verify the real database after migration using SQL checks such as `SHOW TABLES`, `INFORMATION_SCHEMA`, row counts, FK checks, and targeted `SELECT` queries.
9. Update code so JPA entities, repositories, services, controllers, DTOs, frontend calls, and tests match the new schema.
10. Run compile/tests after applying database changes.
11. Update `docs/so-do-tu-duy.md` when the database, entity, schema, migration, seed data, API, or workflow changes.
12. Report the exact database target, SQL files executed, SQL checks run, schema/data result, and any remaining manual steps.

The agent must not stop after only writing a SQL file when it has working access to the local database. If local database access is available, the agent must apply and verify the migration.

If the agent cannot apply the database change directly, it must stop and report a blocker instead of pretending the change was applied. The blocker report must include:

- Why the database change could not be applied.
- What is missing, such as database credentials, `mysql` CLI, database server access, permission, database name, or network access.
- Which SQL file was prepared.
- The exact command or MySQL Workbench steps the user must run manually.
- The SQL verification queries the user should run afterward.

Database safety rules:

- Never run destructive SQL against an unknown or production database.
- Never drop core tables unless the user explicitly asks and the task scope confirms it.
- Never claim a migration was applied unless it was actually executed against the target database.
- Never rely only on `ddl-auto=update` as a substitute for explicit migration SQL when the task changes schema intentionally.
- Do not create empty placeholder classes/entities just to hide compile errors after removing a database module. Fix the real runtime dependency instead.

## Anti-Overengineering Rules

- Keep refactors limited to code directly affected by the task.
- Rewrite a full file, full module, or full page only when the task explicitly requires that scope.
- Keep existing frameworks and libraries unless the task explicitly requires a new one.
- Keep abstractions aligned with existing project patterns.
- Use a localized fix for single-bug, single-component, and single-page requests.

## UI/UX Work

Before editing frontend UI for ATG Academy:

- Read `docs/ui-style-guide.md`.
- Use the style guide as the baseline for visual decisions.
- Apply matching color, spacing, radius, and typography tokens from the guide to edited UI elements.
- Adapt the visual direction to ATG Academy.

Use these UI defaults for edited screens:

- Use one primary accent color.
- Use pill-shaped primary buttons.
- Use pill-shaped search inputs.
- Use lightly bordered cards.
- Use minimal shadow.
- Use subtle hover, focus, and active states.
- Keep responsive behavior clear and predictable.

Apply these page priorities:

- Tier List and Esports pages: keep data dense enough for fast comparison and reading.
- Ban/Pick screens: prioritize fast operation and balanced visibility between the two teams.
- Admin screens: use a clean modern dashboard layout with restrained color and efficient controls.
- Homepage: maintain a premium game and esports academy feel.

## Mind Map Documentation Triggers

Update `docs/so-do-tu-duy.md` in the same task when a change affects any of these areas:

- Project structure
- Major website functionality
- UI workflow
- API endpoint or API behavior
- Database, entity, schema, migration, or seed data
- Admin workflow
- Public user workflow
- Routing or static pages
- Frontend JavaScript module ownership or behavior
- CSS module ownership or behavior
- New feature creation
- Existing feature behavior change
- Module removal, split, or replacement
- Separate HTML page creation

Create `docs/so-do-tu-duy.md` when a documentation-triggering change occurs and the file does not exist.

Leave `docs/so-do-tu-duy.md` unchanged only when the task is a small CSS-only change that does not affect workflow, structure, API, routing, or module ownership. In the final report, state: "Khong cap nhat docs vi thay doi chi la style nho, khong anh huong chuc nang/cau truc."

When documentation impact is unclear, update `docs/so-do-tu-duy.md`.

## Mind Map Documentation Content

Keep `docs/so-do-tu-duy.md` as a mind map of current website functionality.

Track these main functional areas:

- Homepage
- Header / Navigation
- Authentication / User Profile
- Wiki Hero
- Tier List Official
- Community Tier List
- Tier List Import / Export
- Hero Role System: Primary Role / Sub Roles
- Admin Hero
- Admin Attributes
- Esports Ranking
- AOV Esports Slider
- Ban/Pick Free Mode
- Ban/Pick Standard Mode
- Ban/Pick Solo Online
- Guides / Tactics
- Database / Entity / Migration
- API
- UI/UX Design System

For every affected feature branch, include these fields:

- Feature name
- Purpose
- Related users
- Main HTML, JavaScript, and CSS files
- Related controller, service, entity, and repository files
- API endpoints
- Database table or entity
- Main workflow
- Access permissions
- Risk notes or manual testing areas

Write `N/A` for any field that has no matching code, endpoint, database object, permission rule, or testing note.

## Documentation Update Rules

1. Add a new branch when adding a new feature. Include related files, APIs, database details, and user scope: users, admins, or both.
2. Update the existing branch when changing an existing feature. Replace outdated behavior with current behavior.
3. Update the mind map structure when removing or splitting a module. State what replaced the old module.
4. Update the routing/static pages section when adding a separate HTML page, including pages such as `ban-pick-free.html`, `ban-pick-standard.html`, and `ban-pick-solo.html`.
5. Update the API branch when adding or changing an API. Include method, endpoint, behavior, and access permissions.
6. Update the Database branch when changing database, entity, schema, migration, or seed behavior. Include related migration, schema, entity, and seed details.

## Definition of Done

- The requested change is implemented.
- Existing functionality still works unless the task explicitly changed it.
- Changed backend, frontend, API, routing, and database behavior matches the user's request.
- Run the available verification command or manual check for the changed behavior.
- State why verification was not run when the repository provides no usable verification path.
- For database-impacting tasks, migration SQL is created or updated, applied to the local database when access is available, and verified with real schema/data checks.
- For database-impacting tasks that cannot be applied directly, a clear blocker report is provided with the prepared SQL and manual commands.
- `docs/so-do-tu-duy.md` is updated when a documentation trigger applies.
- Only necessary files are modified.
- The final response follows the user's requested output format.

## Final Response Rules

- Provide only the information requested by the user.
- Include explanations only when the user requests explanations.
- When no custom output format is requested, include:
  - Docs updated: yes/no
  - File updated
  - Summary of documentation changes

For database-impacting tasks, also include:

- Database changed: yes/no
- Database target
- Backup created: yes/no
- SQL files executed
- Schema/data verification result
- Blocker, if the database change could not be applied directly
