---
name: atg-ui-ux-review
description: Review and improve ATG Academy UI/UX for Spring Boot static HTML/CSS/JS surfaces. Use when tasks touch the shared header/navigation, dropdown grouping, Esports Data dashboards, Community Tier List creator, Ban/Pick pages, admin screens, responsive layout, accessibility, spacing, hierarchy, visual polish, or manual smoke-test planning. Do not use for database, API, or business logic changes unless the user explicitly asks.
---

# ATG Academy UI/UX Review

Use this skill for UI-only work in ATG Academy.

## Start Every Task

1. Read `docs/AGENTS.md`.
2. Read `docs/ui-style-guide.md`.
3. Inspect the exact HTML, CSS, and JS files touched by the request.
4. Read only the local reference file that matches the task:
   - `docs/agent-skills-references/vercel-web-design-guidelines.md` for the Vercel review skill wrapper, and `docs/agent-skills-references/vercel-web-interface-guidelines-command.md` for the current rule snapshot.
   - `docs/agent-skills-references/design-review.md` for visual polish and screenshot-style review.
   - `docs/agent-skills-references/frontend-design.md` only for larger redesign or stronger visual direction.
   - `docs/agent-skills-references/uxui-principles.md` for multi-step flows, audits, and admin danger-zone review.

## Scope

Allowed:

- Update static HTML, CSS, and vanilla JS for UI/UX improvements.
- Refine spacing, hierarchy, typography usage, component grouping, action placement, responsiveness, and clarity.
- Improve shared partials such as header, footer, and admin sidebar.
- Clean up microcopy when it helps clarity without changing meaning or workflow.
- Plan and report manual smoke tests.

Do not do this for UI-only tasks:

- Do not change the database.
- Do not create or edit SQL migrations.
- Do not change API contracts or endpoint behavior.
- Do not change backend business logic, auth rules, quotas, or content data format.
- Do not rewrite whole files or modules when a localized fix is enough.
- Do not commit unless the user explicitly asks.

Documentation rule:

- Leave `docs/so-do-tu-duy.md` unchanged for small UI polish.
- Update it only when the task materially changes UI workflow, structure, routing, or when the user explicitly wants workflow documentation.

## Project Surface Map

Use this file map first before broad searching:

- Header / navigation:
  - `demo/src/main/resources/static/html/header.html`
  - `demo/src/main/resources/static/js/header-loader.js`
  - `demo/src/main/resources/static/css/style.css`
- Esports public + data pages:
  - `demo/src/main/resources/static/html/esports.html`
  - `demo/src/main/resources/static/html/esports-data.html`
  - `demo/src/main/resources/static/css/esports.css`
  - `demo/src/main/resources/static/js/esports-data.js`
  - `demo/src/main/resources/static/js/admin-esports-data.js`
- Community Tier List + creator flows:
  - `demo/src/main/resources/static/html/tier-list.html`
  - `demo/src/main/resources/static/html/tier-list-community-shell.html`
  - `demo/src/main/resources/static/html/tier-list-all.html`
  - `demo/src/main/resources/static/html/tier-list-detail.html`
  - `demo/src/main/resources/static/html/tier-list-mine.html`
  - `demo/src/main/resources/static/html/tier-list-mine-shell.html`
  - `demo/src/main/resources/static/css/tier-list.css`
  - `demo/src/main/resources/static/js/tier-list-app.js`
  - `demo/src/main/resources/static/js/tier-list-community-page.js`
  - `demo/src/main/resources/static/js/tier-list-detail.js`
  - `demo/src/main/resources/static/js/tier-list-data.js`
- Ban/Pick pages:
  - `demo/src/main/resources/static/html/ban-pick-free.html`
  - `demo/src/main/resources/static/html/ban-pick-standard.html`
  - `demo/src/main/resources/static/html/ban-pick-solo.html`
  - `demo/src/main/resources/static/html/ban-pick-result.html`
  - `demo/src/main/resources/static/html/ban-pick-profile.html`
  - `demo/src/main/resources/static/css/ban-pick.css`
  - `demo/src/main/resources/static/js/ban-pick.js`
  - `demo/src/main/resources/static/js/ban-pick-page.js`
- Admin pages:
  - `demo/src/main/resources/static/html/admin.html`
  - `demo/src/main/resources/static/html/admin-sidebar.html`
  - `demo/src/main/resources/static/html/admin-esports-data.html`
  - `demo/src/main/resources/static/html/admin-heroes.html`
  - `demo/src/main/resources/static/html/admin-attributes.html`
  - `demo/src/main/resources/static/html/admin-wiki-data.html`
  - `demo/src/main/resources/static/css/admin.css`
  - `demo/src/main/resources/static/css/admin-heroes.css`
  - `demo/src/main/resources/static/js/admin-esports-data.js`
  - `demo/src/main/resources/static/js/admin-heroes.js`
  - `demo/src/main/resources/static/js/admin-attributes.js`
  - `demo/src/main/resources/static/js/admin-wiki-data.js`

## ATG UI Direction

Use the ATG style guide as the baseline, then adapt with restraint:

- Use one clear primary accent color.
- Keep primary buttons pill-shaped.
- Keep search inputs pill-shaped when the screen already uses that pattern.
- Prefer lightly bordered cards over heavy shadows.
- Keep hover, focus, and active states visible but subtle.
- Keep layouts responsive without horizontal overflow.
- Keep dashboards data-dense enough for fast reading.
- Use microcopy sparingly on public data-heavy pages.

Avoid:

- Decorative redesign for its own sake.
- Large empty vertical gaps in dashboard screens.
- Hidden primary actions.
- Dropdowns without clear grouping or active state.
- Public UI text that leaks technical database terms.
- Admin screens that make destructive actions look equal to safe actions.

## Surface-Specific Rules

### Header / Navigation

- Group related pages into dropdowns only when it reduces clutter.
- Keep labels short and scannable.
- Ensure active state matches the current route.
- Ensure desktop and mobile expose the same important destinations.
- Do not break login, search, or user-menu behavior.
- Do not break shared header loading or route highlighting.
- Keep mobile navigation stable and free of overflow.

### Public Esports Pages

- Prioritize fast reading of numbers and comparisons.
- Keep KPI cards focused on label plus value.
- Remove unnecessary microcopy unless it helps empty or error states.
- Keep filters, tables, and reset actions easy to find.
- Keep charts and tables readable on mobile.
- Avoid exposing internal table names or raw backend terminology.

### Community Tier List Creator

- Make drag-and-drop easy to understand and easy to perform.
- Keep the main action button or action bar easy to see at all times.
- On desktop, prefer a clear split between tier board and hero/source list when layout changes are needed.
- On mobile, stack vertically and preserve action visibility.
- Keep search and role filters accessible after layout changes.
- Do not change content data format, quota logic, or guest/login behavior unless explicitly requested.

### Ban/Pick

- Prioritize operation speed over decorative chrome.
- Keep both teams visually balanced.
- Do not cover current phase, timer, confirm/cancel controls, or selected hero context.
- Keep score, win-rate, and supporting data compact and readable.

### Admin Pages

- Keep the layout compact, clean, and efficient.
- Make dashboard sections easy to scan.
- Keep bulk actions and filters obvious.
- Separate destructive actions into a clear danger zone.
- Preserve confirmation text and consequence clarity for risky actions.
- Avoid decorative UI that slows routine admin work.

## Editing Strategy

1. Narrow the change to the exact page, component, and supporting assets.
2. Prefer localized HTML/CSS/JS edits over broad rewrites.
3. Reuse existing ATG patterns before introducing new UI patterns.
4. Preserve current runtime behavior unless the user explicitly asks for a UX behavior change.
5. If the requested UI change would require DB, API, or backend logic changes, stop and call out the dependency clearly.

## Verification

When JS changes:

```powershell
node --check .\demo\src\main\resources\static\js\<changed-file>.js
```

Always perform manual smoke checks for the affected screen:

- Check desktop layout.
- Check a mobile-width layout.
- Check that no new horizontal overflow appears.
- Check that affected actions are still discoverable and clickable.

When editing navigation, smoke test at least:

- `/html/index.html`
- `/html/esports.html`
- `/html/esports-data.html`
- `/html/tier-list.html`
- `/html/wiki.html`
- one Ban/Pick page

For navigation changes, verify:

- Active state
- Dropdown open and click behavior
- Mobile menu
- Shared route availability

When editing Community Tier List, verify where applicable:

- Create flow entry point
- Search
- Role filter
- Drag hero to a tier
- Move hero between tiers
- Return hero to the source list if that interaction exists
- Footer or sticky actions
- Guest flow and logged-in flow when credentials are available

When editing Esports Data, verify where applicable:

- Data load state
- Filters
- Reset action
- KPI values presentation
- Top Banned / Top Picked / Blue Side sections
- Mobile readability

## Final Report

Use this format unless the user asks for another one:

```text
Summary
- ...

Files changed
- ...

Docs updated
- Yes/No.
- If No, explain why.
- If Yes, list the updated docs.

Database changed
- No, unless the task explicitly required DB work.

Verification
- Commands run and results.
- Manual smoke tests performed.

Notes
- Blockers, deferred checks, browser-only checks, or intentionally preserved behavior.
```
