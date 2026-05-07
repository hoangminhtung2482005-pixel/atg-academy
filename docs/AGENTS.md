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
