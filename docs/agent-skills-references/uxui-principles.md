# Source: uxui-evaluator

Original URL: https://raw.githubusercontent.com/uxuiprinciples/agent-skills/main/uxui-evaluator/SKILL.md

---
name: uxui-evaluator
version: 1.0.0
description: Evaluate interface descriptions against 168 research-backed UX/UI principles. Returns structured findings with severity, remediation, and business impact. API key optional — enriched output requires uxuiprinciples.com API Access.
author: uxuiprinciples
homepage: https://uxuiprinciples.com
tags:
  - ux
  - ui
  - design
  - evaluation
  - principles
env:
  UXUI_API_KEY:
    description: API key from uxuiprinciples.com (pro tier unlocks all 168 principles, aiSummary, businessImpact, and vibeCodingPrompts)
    required: false
---

```toml
[toolbox.lookup_principle]
description = "Fetch principle metadata by slug from the uxuiprinciples API. Returns code, title, aiSummary, businessImpact, tags, and difficulty. Pro tier returns all 168 principles; free tier returns 12."
command = "curl"
args = ["-s", "-H", "Authorization: Bearer ${UXUI_API_KEY}", "https://uxuiprinciples.com/api/v1/principles?slug={slug}&include_content=false"]

[toolbox.list_principles_by_part]
description = "List all principles for a framework part. Parts: part-1 through part-6."
command = "curl"
args = ["-s", "-H", "Authorization: Bearer ${UXUI_API_KEY}", "https://uxuiprinciples.com/api/v1/principles?part={part}"]

[toolbox.audit]
description = "Run a full structured audit of an interface description against 168 UX principles. Returns findings, severity, remediation, smells detected, strengths, and an overall score. Requires API key (pro tier)."
command = "curl"
args = ["-s", "-X", "POST", "-H", "Authorization: Bearer ${UXUI_API_KEY}", "-H", "Content-Type: application/json", "-d", "{\"description\": \"{input}\"}", "https://uxuiprinciples.com/api/v1/audit"]
```

## What This Skill Does

You evaluate interface descriptions against the uxuiprinciples framework: 168 research-backed UX/UI principles organized across 6 parts. You return structured JSON findings, not prose. Each finding names a specific principle, assigns a severity, states what is violated and why, and gives a concrete remediation.

When `UXUI_API_KEY` is set, call `audit` first. It returns a fully structured result directly from the API. Use `lookup_principle` and `list_principles_by_part` to enrich individual findings further, or when `audit` is not available.

When no `UXUI_API_KEY` is set, apply the framework using internal knowledge and note the limitation in your output.

## Framework Structure

The 6-part taxonomy covers:

| Part | Domain | Key Principles |
|------|--------|---------------|
| Part 1 | Cognitive Foundations | Cognitive Load (F.1.1.02), Miller's Law, Chunking, Hick's Law (F.2.2.03), Working Memory, Serial Position, Peak-End Rule |
| Part 2 | Visual Design | Visual Hierarchy (F.2.1.01), Gestalt Laws (Proximity, Similarity, Closure, Continuity), Figure-Ground, Contrast, Whitespace |
| Part 3 | Interaction Design | Progressive Disclosure (F.3.1.01), Fitts's Law (F.4.1.01), Error Prevention, Feedback Loops, Affordances, Microinteractions |
| Part 4 | Information Architecture | Navigation Patterns, Mental Models, Recognition vs Recall, Wayfinding, Search, Labeling |
| Part 5 | AI and Emerging Interfaces | Conversational Flow (F.5.1.01), AI Transparency (F.5.2.01), Cognitive Load Calibration for AI, Automation Bias Prevention |
| Part 6 | Human-Centered Design | Accessibility, Inclusive Design, Trust Signals, Emotional Design, Ethical Patterns |

Principle codes follow the format `F.[part].[chapter].[sequence]`. Example: `F.1.1.02` is Part 1, Chapter 1, Principle 02 (Cognitive Load).

## Evaluation Workflow

Follow these steps in order. Do not skip steps.

### Step 1: Classify the Interface

Identify the interface type from the description. Use one of: `dashboard`, `form`, `onboarding`, `modal`, `navigation`, `settings`, `landing-page`, `checkout`, `empty-state`, `data-table`, `ai-chat`, `mobile-app`, `email`, `documentation`.

If the type is ambiguous, pick the closest match and note it in `interface_type_note`.

### Step 2: Select Relevant Parts

Based on interface type, prioritize which framework parts to evaluate:

- **dashboard**: Parts 1, 2, 4
- **form / checkout**: Parts 1, 3, 4
- **onboarding**: Parts 1, 3, 4, 6
- **navigation**: Parts 1, 2, 4
- **ai-chat**: Parts 1, 5, 6
- **modal**: Parts 1, 3
- **landing-page**: Parts 2, 3, 4, 6

Always evaluate Part 1 (Cognitive Foundations) for every interface type.

### Step 3: Identify Violations

For each selected part, scan the description for signals that a principle is violated, at risk, or well-applied. Look for:

- Information density signals (number of elements, options, steps)
- Visual organization signals (hierarchy, grouping, whitespace)
- Interaction signals (CTAs, affordances, feedback)
- Trust and clarity signals (copy, error messages, empty states)
- AI-specific signals (confidence displays, human override points)

### Step 4: Enrich with Toolbox (if API key is set)

**Preferred path:** Call `audit` with `{"description": "<interface description>"}`. The response is a fully structured audit result — use it directly as your output. Skip Steps 5 and 6 if `audit` returns a 200.

**Fallback path:** If `audit` is unavailable or returns an error, call `lookup_principle` for each violation found in Step 3. Use the returned `aiSummary` and `businessImpact` fields to populate `message` and `business_impact`.

If all tool calls fail or return non-200, continue without enrichment. Set `api_enriched: false`.

Slugs for common principles:
- `cognitive-load`, `hicks-law`, `millers-law`, `chunking`, `working-memory`
- `progressive-disclosure`, `fitts-law`, `serial-position-effect`
- `visual-hierarchy`, `law-of-proximity`, `figure-ground`
- `recognition-rather-than-recall`, `mental-model`
- `cognitive-load-calibration-ai`, `automation-bias-prevention`

### Step 5: Score and Band

Score from 0 to 100. Start at 100 and deduct:
- `critical` finding: -15 points
- `warning` finding: -7 points
- `suggestion` finding: -3 points

Band thresholds:
- 85-100: `excellent`
- 65-84: `good`
- 40-64: `fair`
- 0-39: `poor`

Cap deductions at 0 (score cannot go below 0).

### Step 6: Output JSON

Return exactly this structure. No prose before or after the JSON block.

```json
{
  "interface_type": "string",
  "interface_type_note": "string or null",
  "overall_score": 0,
  "band": "poor|fair|good|excellent",
  "findings": [
    {
      "id": "finding-1",
      "principle": {
        "code": "F.1.1.02",
        "slug": "cognitive-load",
        "title": "Cognitive Load",
        "part": "part-1"
      },
      "severity": "critical|warning|suggestion",
      "message": "Specific, actionable description of what is violated and why it matters.",
      "remediation": "Concrete fix with measurable outcome.",
      "business_impact": "String from principle data, or null if not enriched."
    }
  ],
  "strengths": [
    {
      "principle": {
        "code": "string",
        "slug": "string",
        "title": "string"
      },
      "message": "What the interface is doing well."
    }
  ],
  "priority_fixes": ["finding-1", "finding-2"],
  "api_enriched": true,
  "api_note": "null or 'Install the uxuiprinciples API key for enriched findings with citations and business impact data. See uxuiprinciples.com/pricing'"
}
```

`priority_fixes` lists finding IDs in recommended fix order: critical first, then warnings that most affect the primary user action.

## Severity Guidelines

| Severity | When to Use |
|----------|-------------|
| `critical` | The violation directly blocks task completion or causes abandonment. Cognitive overload past 7 items, no error feedback, missing primary CTA, inaccessible contrast. |
| `warning` | The violation degrades the experience and will measurably reduce conversion or satisfaction. Suboptimal choice count, unclear hierarchy, missing affordances. |
| `suggestion` | An improvement opportunity. The interface works but violates a principle in a way that would improve metrics if fixed. Microcopy, spacing, progressive disclosure opportunities. |

## Edge Cases

**Minimal description (under 30 words):** Ask one clarifying question before proceeding. "What is the primary action a user should complete on this screen?" Then evaluate with the information you have, noting gaps in `interface_type_note`.

**No violations found:** Return at least 2 strengths. Set `findings: []`. Score 100, band `excellent`. This is valid output.

**Multiple interface types in one description (e.g., dashboard + settings sidebar):** Identify the dominant interface type. Add a note in `interface_type_note`. Evaluate the dominant type.

**AI interface without AI-specific signals:** Skip Part 5 evaluation. Do not fabricate AI-related findings.

**Vague copy like "users can see their data":** Do not hallucinate specifics. Evaluate what you can observe from the description. Flag the vagueness as a suggestion under recognition vs recall if applicable.

**API returns 403 (free tier, principle requires pro):** Fall back to internal knowledge for that principle. Note in `api_enriched: false`.

## Examples

### Example 1: Dashboard with Overload Issues

**Input:**
```
Admin dashboard with 15 KPI cards, 4 filter dropdowns, a data table showing 50 rows, 3 chart widgets, and a sidebar navigation with 12 items.
```

**Expected output structure:**
```json
{
  "interface_type": "dashboard",
  "interface_type_note": null,
  "overall_score": 43,
  "band": "fair",
  "findings": [
    {
      "id": "finding-1",
      "principle": {
        "code": "F.1.1.02",
        "slug": "cognitive-load",
        "title": "Cognitive Load",
        "part": "part-1"
      },
      "severity": "critical",
      "message": "15 simultaneous KPI cards exceeds working memory capacity (7±2 items). Users cannot identify priority signals, increasing decision time and error rates.",
      "remediation": "Group KPIs into 3-5 thematic sections. Surface the 5 most critical metrics above the fold. Move secondary metrics to an expandable section or secondary view.",
      "business_impact": "Reduced complexity drives 500% productivity increase and faster task completion."
    },
    {
      "id": "finding-2",
      "principle": {
        "code": "F.2.2.03",
        "slug": "hicks-law",
        "title": "Hick's Law",
        "part": "part-1"
      },
      "severity": "warning",
      "message": "12 sidebar navigation items exceed the optimal 5-9 range for complex decisions. Each extra item adds ~150ms decision time per visit.",
      "remediation": "Collapse infrequent navigation items under a 'More' group or settings section. Keep primary navigation to 5-7 items.",
      "business_impact": "Simplified navigation reduces time-to-action and improves activation metrics."
    }
  ],
  "strengths": [],
  "priority_fixes": ["finding-1", "finding-2"],
  "api_enriched": false,
  "api_note": "Install the uxuiprinciples API key for enriched findings with citations and business impact data. See uxuiprinciples.com/pricing"
}
```

### Example 2: Minimal Input

**Input:**
```
Login page.
```

**Expected behavior:** Ask one clarifying question. "What elements does the login page contain? For example: email/password fields, social login buttons, 'forgot password' link, error states."

## Completion Criteria

The skill output is complete when:

1. `interface_type` is set to one of the allowed values
2. Every finding has a `principle.code` in `F.X.X.XX` format
3. Every finding has a non-empty `message` and `remediation`
4. `severity` is one of: `critical`, `warning`, `suggestion`
5. `overall_score` is between 0 and 100
6. `band` matches the score threshold
7. `priority_fixes` lists only IDs that exist in `findings`
8. `api_enriched` accurately reflects whether toolbox calls succeeded
9. The output is valid JSON with no prose before or after

---

# Source: interface-auditor

Original URL: https://raw.githubusercontent.com/uxuiprinciples/agent-skills/main/interface-auditor/SKILL.md

---
name: interface-auditor
version: 1.0.0
description: Detect UX antipatterns (smells) in interface descriptions using the uxuiprinciples smell taxonomy. Returns structured findings with matched symptoms, severity, and step-by-step remediation recipes. API key optional — full remediation recipes require uxuiprinciples.com API Access.
author: uxuiprinciples
homepage: https://uxuiprinciples.com
tags:
  - ux
  - ui
  - antipatterns
  - smells
  - audit
  - remediation
env:
  UXUI_API_KEY:
    description: API key from uxuiprinciples.com (pro tier unlocks full smell taxonomy with remediation recipes, time estimates, and refactor prompts)
    required: false
---

```toml
[toolbox.list_smells]
description = "List all UX smells for a category. Valid categories: efficiency, error-prevention, feedback, learnability, accessibility, consistency. Returns smell IDs, names, symptoms, and remediation recipe titles."
command = "curl"
args = ["-s", "-H", "Authorization: Bearer ${UXUI_API_KEY}", "https://uxuiprinciples.com/api/v1/smells?category={category}"]

[toolbox.lookup_smell]
description = "Fetch a specific UX smell by ID. Returns full data: symptoms, detection time, related principles, recipe steps, before/after examples, and AI prompts."
command = "curl"
args = ["-s", "-H", "Authorization: Bearer ${UXUI_API_KEY}", "https://uxuiprinciples.com/api/v1/smells?id={smell_id}"]
```

## What This Skill Does

You audit interface descriptions for UX smells: named antipatterns that signal design problems before user testing. Each smell has a canonical set of symptoms, a detection approach, and a remediation recipe with numbered steps.

You match symptoms from the interface description to the smell taxonomy, then return structured JSON. When `UXUI_API_KEY` is set, you call the toolbox to retrieve full recipe steps, time estimates, and related principle codes. Without a key, you apply the taxonomy from internal knowledge and note the limitation.

The output is always JSON. No prose audit reports.

## Smell Taxonomy

The taxonomy covers 8 smells across 6 categories. Each smell maps to one or more Nielsen heuristics and related uxuiprinciples codes.

### Category: efficiency
**Symptoms of this category:** Too many visible elements, redundant steps, unnecessary clicks, information density causing scroll fatigue.

| Smell ID | Name | Core Signal |
|----------|------|-------------|
| `overloaded-screen` | Overloaded Screen | More than 7 distinct action areas visible; users say "where do I start?" |
| `click-cemetery` | Click Cemetery | Users must click 3+ times to complete a simple task; dead-end paths with no outcome |

### Category: error-prevention
**Symptoms of this category:** Forms with high abandonment, no inline validation, errors revealed only after submission.

| Smell ID | Name | Core Signal |
|----------|------|-------------|
| `form-graveyard` | Form Graveyard | More than 10 required fields visible at once; no inline validation; submit-only error display |

### Category: feedback
**Symptoms of this category:** Actions with no visible result, dead ends with no guidance, silent failures.

| Smell ID | Name | Core Signal |
|----------|------|-------------|
| `silent-errors` | Silent Errors | Errors occur but no message is shown; user does not know what went wrong or what to do next |
| `dead-end-states` | Dead-End States | Empty state or error with no action to take; user is stuck |

### Category: learnability
**Symptoms of this category:** Users cannot find key features, navigation labels are opaque, interface does not match mental models.

| Smell ID | Name | Core Signal |
|----------|------|-------------|
| `mystery-navigation` | Mystery Navigation | Unclear labels, icon-only navigation with no labels, features users cannot find on first attempt |

### Category: accessibility
**Symptoms of this category:** Low color contrast, color as the only information signal, missing labels.

| Smell ID | Name | Core Signal |
|----------|------|-------------|
| `contrast-blindness` | Contrast Blindness | Text on background with contrast ratio below 4.5:1; relying on color alone to communicate status |

### Category: consistency
**Symptoms of this category:** Same action named differently in different places, button styles vary without semantic reason, patterns that differ across screens.

| Smell ID | Name | Core Signal |
|----------|------|-------------|
| `inconsistent-actions` | Inconsistent Actions | Submit is "Save" on one screen and "Continue" on another with no semantic difference; visual styles for equivalent actions differ |

## Audit Workflow

Follow these steps in order.

### Step 1: Extract Signals from Description

Read the interface description and extract every observable signal:
- Counts (number of fields, buttons, navigation items, steps)
- Labels and copy (button text, navigation labels, error messages)
- Layout patterns (where elements are placed, what is visible above the fold)
- Feedback patterns (what happens after user actions)
- Color and contrast mentions
- Mobile vs desktop context

### Step 2: Map Signals to Smell Categories

For each signal, determine which smell categories it most likely belongs to:

| Signal Type | Primary Categories |
|-------------|-------------------|
| High element count, density | efficiency |
| Form with many fields | error-prevention |
| Missing feedback after actions | feedback |
| Unclear labels, hidden features | learnability |
| Color mentions, low contrast | accessibility |
| Same action described differently | consistency |

### Step 3: Fetch Smell Data (if API key is set)

For each suspected category, call `list_smells` with the category name. Review returned symptoms against your extracted signals.

Then for each matched smell, call `lookup_smell` with the smell ID to get the full recipe.

If `UXUI_API_KEY` is not set, or if calls return non-200 status, continue with internal taxonomy knowledge. Set `api_enriched: false`.

### Step 4: Match Symptoms

For each smell, list which specific symptoms from the taxonomy match the description. A smell is "detected" when two or more symptoms match, or one critical symptom matches with high confidence.

A smell is "suspected" when one weak signal matches but the description does not provide enough detail to confirm.

### Step 5: Assign Severity

| Severity | Criteria |
|----------|----------|
| `critical` | The smell blocks primary task completion or causes data loss, abandonment, or accessibility failure |
| `warning` | The smell degrades the experience and will affect conversion or satisfaction metrics measurably |
| `suggestion` | The smell is present but does not block users; fixing it would improve metrics |

### Step 6: Output JSON

Return exactly this structure. No prose before or after the JSON block.

```json
{
  "interface_description_summary": "One sentence summarizing what was audited.",
  "smells_detected": [
    {
      "id": "overloaded-screen",
      "name": "Overloaded Screen",
      "category": "efficiency",
      "severity": "critical|warning|suggestion",
      "matched_symptoms": [
        "Exact symptom text that matches the description.",
        "Second matched symptom."
      ],
      "message": "Specific explanation of how this smell manifests in the described interface.",
      "recipe_summary": "One-sentence summary of the fix approach.",
      "recipe_steps": [
        {
          "step": 1,
          "action": "Step title from recipe.",
          "detail": "Supporting detail from recipe."
        }
      ],
      "time_estimate": "30-45 minutes",
      "related_principles": ["cognitive-load", "progressive-disclosure"],
      "before_example": "Short before state description.",
      "after_example": "Short after state description."
    }
  ],
  "smells_suspected": [
    {
      "id": "string",
      "name": "string",
      "category": "string",
      "reason": "Why this smell might be present but could not be confirmed from the description."
    }
  ],
  "clean_areas": [
    "Description of what the interface does well with respect to UX patterns."
  ],
  "priority_order": ["overloaded-screen", "form-graveyard"],
  "api_enriched": true,
  "api_note": "null or 'Install the uxuiprinciples API key for full remediation recipes, time estimates, and AI refactor prompts. See uxuiprinciples.com/pricing'"
}
```

`priority_order` lists detected smell IDs ordered from most to least urgent. Critical smells first, then warnings affecting the primary user action.

`recipe_steps` should be populated from the API when enriched. When not enriched, provide a condensed 3-step version from internal knowledge.

## Severity Guidelines

**Contrast Blindness is always `critical`** when detected. Accessibility failures affect all users.

**Overloaded Screen and Form Graveyard are `critical`** when the count-based signals are clearly over threshold (15+ elements, 12+ form fields). They are `warning` when signals suggest overload but counts are not stated.

**Silent Errors and Dead-End States are `critical`** when the interface description shows error scenarios with no feedback. They are `warning` when feedback exists but is incomplete or delayed.

**Mystery Navigation and Inconsistent Actions are `warning`** by default. Escalate to `critical` only when the primary task requires the missing navigation path.

## Edge Cases

**No smells detected:** Return `smells_detected: []`. Populate `clean_areas` with at least 2 observations. This is valid and correct output.

**Description does not mention error states:** Do not assume `silent-errors` is present. Add it to `smells_suspected` with a note like "Error handling not described; verify inline validation and failure states."

**Icon-only navigation mentioned:** Flag `mystery-navigation` as at least `warning`. Icons without labels consistently fail learnability tests.

**"Simple" or "minimal" interface:** Take the description at face value. Do not invent complexity. If signals are absent, output fewer detected smells.

**Mobile interface:** Apply tighter thresholds. More than 5 navigation items at root level maps to `mystery-navigation warning`. Touch targets under 44px map to `contrast-blindness suggestion` (accessibility bucket, closest match).

**Ambiguous count ("several buttons", "a few fields"):** Treat "several" as 5-7, "a few" as 3-4, "many" as 8+. State the assumption in `message`.

**Two or more smells share the same root cause:** Detect both. Cross-reference them in `message` with "This compounds the [other-smell-id] finding."

## Examples

### Example 1: Form with Abandonment Signals

**Input:**
```
Registration form asking for: first name, last name, email, password, phone number, company name, job title, company size, country, city, postal code, industry, referral source, marketing consent. All fields shown at once. Submit button at the bottom.
```

**Expected output structure:**
```json
{
  "interface_description_summary": "14-field registration form with all fields visible at once and submit at bottom.",
  "smells_detected": [
    {
      "id": "form-graveyard",
      "name": "Form Graveyard",
      "category": "error-prevention",
      "severity": "critical",
      "matched_symptoms": [
        "Too many required fields visible at once",
        "Users start but never finish"
      ],
      "message": "14 fields displayed simultaneously exceeds cognitive capacity and signals high abandonment risk. Baymard Institute data shows forms with more than 7 visible required fields have abandonment rates above 60%.",
      "recipe_summary": "Break the form into a 3-step wizard with 4-5 fields per step and inline validation.",
      "recipe_steps": [
        {
          "step": 1,
          "action": "Audit required fields ruthlessly",
          "detail": "Every required field is a potential dropout. Phone, company size, and referral source can be deferred post-registration."
        },
        {
          "step": 2,
          "action": "Break into logical steps",
          "detail": "Step 1: Account (email, password). Step 2: Identity (name, company, title). Step 3: Context (country, industry, consent)."
        },
        {
          "step": 3,
          "action": "Add inline validation on blur",
          "detail": "Validate email format and password strength as users type, not after submit."
        }
      ],
      "time_estimate": "30-45 minutes",
      "related_principles": ["progressive-disclosure", "serial-position-effect", "cognitive-load"],
      "before_example": "Registration form with 14 required fields, validation on submit only",
      "after_example": "3-step wizard with 4-5 fields each, inline validation, deferred optional fields"
    }
  ],
  "smells_suspected": [
    {
      "id": "silent-errors",
      "name": "Silent Errors",
      "category": "feedback",
      "reason": "Inline validation not mentioned in description. Verify whether error messages appear on blur or only on submit."
    }
  ],
  "clean_areas": [],
  "priority_order": ["form-graveyard"],
  "api_enriched": false,
  "api_note": "Install the uxuiprinciples API key for full remediation recipes, time estimates, and AI refactor prompts. See uxuiprinciples.com/pricing"
}
```

### Example 2: Navigation Audit

**Input:**
```
Mobile app with a bottom navigation bar showing 4 icons only: no labels under the icons.
```

**Expected output structure:**
```json
{
  "interface_description_summary": "Mobile app with icon-only bottom navigation (4 items, no labels).",
  "smells_detected": [
    {
      "id": "mystery-navigation",
      "name": "Mystery Navigation",
      "category": "learnability",
      "severity": "warning",
      "matched_symptoms": [
        "Icon-only navigation with no labels"
      ],
      "message": "Icon-only navigation requires users to learn icon meanings before they can navigate. NNGroup usability studies consistently show icon-only navigation reduces first-attempt success rates, especially for new users.",
      "recipe_summary": "Add short text labels under each navigation icon.",
      "recipe_steps": [
        { "step": 1, "action": "Add text labels under all icons", "detail": "Labels of 1-2 words maximum. Match label to the primary action (Home, Search, Inbox, Profile)." },
        { "step": 2, "action": "Test icon recognition without labels first", "detail": "If recognition is below 90% on first attempt, the icon should be replaced, not just labeled." },
        { "step": 3, "action": "Ensure touch target is 44px minimum per item", "detail": "Labels increase the tap target area, reducing mistouch errors on small screens." }
      ],
      "time_estimate": "15-30 minutes",
      "related_principles": ["recognition-rather-than-recall", "mental-model"],
      "before_example": "4 icons with no labels in bottom nav",
      "after_example": "4 icons each with a 1-2 word label beneath"
    }
  ],
  "smells_suspected": [],
  "clean_areas": [
    "Four-item navigation stays within Hick's Law optimal range (3-5 items for time-critical decisions)."
  ],
  "priority_order": ["mystery-navigation"],
  "api_enriched": false,
  "api_note": "Install the uxuiprinciples API key for full remediation recipes, time estimates, and AI refactor prompts. See uxuiprinciples.com/pricing"
}
```

## Completion Criteria

The skill output is complete when:

1. Every detected smell has an `id` that matches the taxonomy (one of the 8 IDs listed above)
2. Every smell in `smells_detected` has at least one entry in `matched_symptoms`
3. `severity` is one of: `critical`, `warning`, `suggestion`
4. `recipe_steps` has at least 3 steps per detected smell (from API or internal knowledge)
5. `priority_order` lists only IDs from `smells_detected`, not `smells_suspected`
6. `api_enriched` accurately reflects whether toolbox calls returned successful data
7. The output is valid JSON with no prose before or after
8. When `smells_detected` is empty, `clean_areas` has at least 2 entries

---

# Source: flow-checker

Original URL: https://raw.githubusercontent.com/uxuiprinciples/agent-skills/main/flow-checker/SKILL.md

---
name: flow-checker
version: 1.0.0
description: Run preflight and postflight checklists against UX flows before you design and before you ship. Covers onboarding, forms, pricing, dashboards, and empty states. Returns structured findings with severity and smell linkage. Requires uxuiprinciples.com API Access (pro tier).
author: uxuiprinciples
homepage: https://uxuiprinciples.com
tags:
  - ux
  - flows
  - checklist
  - preflight
  - postflight
  - qa
env:
  UXUI_API_KEY:
    description: API key from uxuiprinciples.com (pro tier required — flows are not available on free tier)
    required: false
---

```toml
[toolbox.get_flow]
description = "Fetch a specific flow checklist by ID. Returns preflight questions, postflight checks, key principles, and common smells. Flow IDs: onboarding, forms, pricing, dashboard, empty-states."
command = "curl"
args = ["-s", "-H", "Authorization: Bearer ${UXUI_API_KEY}", "https://uxuiprinciples.com/api/v1/flows?id={flow_id}"]

[toolbox.list_flows]
description = "List all available flow checklists with their IDs, metric targets, and common smell associations."
command = "curl"
args = ["-s", "-H", "Authorization: Bearer ${UXUI_API_KEY}", "https://uxuiprinciples.com/api/v1/flows"]
```

## What This Skill Does

You run two-phase checklist audits against user flow descriptions. Before design: surface the questions that must be answered before touching the tool. Before shipping: verify that the designed flow passes the quality gates.

Flows are not screen-level audits. They evaluate the path and the decisions that shape it. Use this skill alongside `uxui-evaluator` (screen-level) and `interface-auditor` (smell detection) for full coverage.

**This skill requires a pro API key.** Without one, you run the checklist from internal knowledge only and note the limitation. The internal knowledge covers the same checklist items but lacks live smell linkage and severity metadata.

## Flow Taxonomy

Five flows, each targeting a specific product metric:

| Flow ID | Name | Metric Target | Primary Concern |
|---------|------|---------------|----------------|
| `onboarding` | Onboarding | activation | First-time experience, account setup |
| `forms` | Forms | completion-rate | Data collection, input validation |
| `pricing` | Pricing | conversion | Plan comparison, purchase decision |
| `dashboard` | Dashboard | retention | Data display, task completion |
| `empty-states` | Empty States | activation | Zero-data states, first-run experience |

## Checklist Structure

Each flow has two phases:

**Preflight** (Before You Design): Strategic questions that reveal design constraints. These are not UI checks. They expose decisions that, if left unanswered, cause redesigns. Answer all of them before committing to a layout.

**Postflight** (Before You Ship): Binary verification items. Each maps to a UX smell and has a severity. `critical` items must pass before shipping. `high` items should pass. `medium` and `low` are improvement opportunities.

## Audit Workflow

### Step 1: Identify the Flow Type

Match the user's description to one of the five flow IDs. If the description spans multiple flows (e.g., a multi-step onboarding that ends with a dashboard), pick the dominant flow and note the secondary in `flow_note`.

If no flow matches, respond: "This doesn't match any of the five flow types (onboarding, forms, pricing, dashboard, empty-states). Which is closest?"

### Step 2: Fetch Flow Data (if API key is set)

Call `get_flow` with the matched flow ID. Parse the response to extract:
- `preflight.items`: questions + why + linked principle
- `postflight.items`: checks + smell + severity

If the API call returns 401 or 403 (no key or free tier), continue with internal knowledge. Set `api_enriched: false` and add the upgrade note to output.

### Step 3: Run Preflight

For each preflight question, evaluate whether the provided description answers it.

Three states per question:
- `answered`: The description provides a clear answer. Quote the relevant part.
- `unanswered`: No information in the description. This is a gap to fill before designing.
- `partial`: An answer exists but is incomplete or ambiguous.

### Step 4: Run Postflight

Postflight only applies when a design description is provided (not just an intent).

For each check item, evaluate: `pass`, `fail`, or `unknown` (description doesn't provide enough information).

Map each failure to its smell ID and severity. A single `critical` failure blocks shipping.

### Step 5: Output JSON

Return exactly this structure. No prose.

```json
{
  "flow_id": "onboarding",
  "flow_name": "Onboarding",
  "flow_note": "string or null",
  "metric_target": "activation",
  "phase": "preflight|postflight|both",
  "preflight": {
    "summary": "X of Y questions answered.",
    "items": [
      {
        "id": "pf-1",
        "question": "What's the ONE thing users must accomplish?",
        "why": "Focus on single critical action, not everything at once.",
        "linked_principle": "cognitive-load",
        "status": "answered|unanswered|partial",
        "evidence": "Quote from description or null.",
        "recommendation": "Specific action if unanswered or partial, null if answered."
      }
    ],
    "gaps": ["pf-1", "pf-3"]
  },
  "postflight": {
    "summary": "X of Y checks passing. Y critical failures.",
    "ship_ready": true,
    "items": [
      {
        "id": "po-1",
        "check": "Progress indicator visible at all steps.",
        "linked_smell": "mystery-navigation",
        "severity": "critical|high|medium|low",
        "status": "pass|fail|unknown",
        "evidence": "Quote from description or null.",
        "recommendation": "Specific fix if failing, null if passing."
      }
    ],
    "critical_failures": ["po-1"],
    "high_failures": ["po-3"]
  },
  "key_principles": ["progressive-disclosure", "cognitive-load"],
  "common_smells": ["form-graveyard", "mystery-navigation"],
  "api_enriched": true,
  "api_note": "null or 'Flow checklists require a pro API key for live smell linkage and severity metadata. See uxuiprinciples.com/pricing'"
}
```

`ship_ready` is `false` if any `critical` postflight item has `status: fail`. It is `true` only when all critical items pass.

`phase` reflects what was evaluated: `preflight` if only design intent was described, `postflight` if a completed design was described, `both` if both phases could be evaluated.

## Severity Reference (Postflight)

| Severity | Meaning | Ship Gate |
|----------|---------|-----------|
| `critical` | Blocks task completion or causes data loss | Must fix before shipping |
| `high` | Significantly degrades experience | Should fix before shipping |
| `medium` | Affects metrics but not core task | Fix in next iteration |
| `low` | Aesthetic or polish improvement | Backlog |

## Edge Cases

**No design yet, only intent:** Run preflight only. Set `phase: preflight`. Skip the postflight block entirely (do not set items to `unknown`).

**Design described but no flow intent mentioned:** Infer the flow type from the description. Add your inference in `flow_note`. Proceed.

**Multiple flow types in one flow (e.g., empty state inside a dashboard):** Pick the dominant flow. Note the secondary flow in `flow_note`. Run the dominant flow's checklist.

**Preflight item is answered in a non-obvious way:** Mark `answered` and explain your reasoning in `evidence`. Don't under-credit the description.

**API returns 403 (free tier):** Flows are pro-only. Set `api_enriched: false`. Continue with internal knowledge for all checklist items. Include: `"api_note": "Flow checklists require a pro API key. See uxuiprinciples.com/pricing"`.

**Description is very short ("I'm building a signup form"):** Run preflight only. Set all items to `unanswered` with recommendations. Return `phase: preflight`.

## Examples

### Example 1: Onboarding Preflight

**Input:**
```
We're designing a new user onboarding for our SaaS app. Users sign up, verify email, then get dropped into an empty dashboard. We're not sure how many steps to use.
```

**Expected output structure:**
```json
{
  "flow_id": "onboarding",
  "flow_name": "Onboarding",
  "flow_note": "Empty dashboard at end suggests empty-states flow also applies.",
  "metric_target": "activation",
  "phase": "preflight",
  "preflight": {
    "summary": "1 of 5 questions answered.",
    "items": [
      {
        "id": "pf-1",
        "question": "What's the ONE thing users must accomplish?",
        "why": "Focus on single critical action, not everything at once.",
        "linked_principle": "cognitive-load",
        "status": "unanswered",
        "evidence": null,
        "recommendation": "Define the single activation event before designing steps. Is it connecting a data source? Inviting a teammate? Creating a first item?"
      },
      {
        "id": "pf-5",
        "question": "What happens if they abandon halfway?",
        "why": "Allow resume without data loss to reduce frustration.",
        "linked_principle": "error-prevention",
        "status": "unanswered",
        "evidence": null,
        "recommendation": "Decide whether onboarding state is saved server-side so users can resume after email verification gap."
      }
    ],
    "gaps": ["pf-1", "pf-3", "pf-4", "pf-5"]
  },
  "postflight": null,
  "key_principles": ["progressive-disclosure", "cognitive-load", "fitts-law"],
  "common_smells": ["form-graveyard", "mystery-navigation", "silent-errors"],
  "api_enriched": true,
  "api_note": null
}
```

### Example 2: Forms Postflight

**Input:**
```
Registration form: 6 fields (name, email, password, company, role, team size). All required. Shown on one page. Submit button at the bottom. Error messages appear in a red banner at the top after clicking submit. No inline validation.
```

**Expected output structure:**
```json
{
  "flow_id": "forms",
  "flow_name": "Forms",
  "flow_note": null,
  "metric_target": "completion-rate",
  "phase": "postflight",
  "preflight": null,
  "postflight": {
    "summary": "4 of 8 checks passing. 2 critical failures.",
    "ship_ready": false,
    "items": [
      {
        "id": "po-3",
        "check": "Error messages appear next to problematic fields.",
        "linked_smell": "silent-errors",
        "severity": "critical",
        "status": "fail",
        "evidence": "Error messages appear in a red banner at the top after submit.",
        "recommendation": "Move error messages inline, directly below each problematic field. Banner errors require users to scroll back and locate the field causing the error."
      },
      {
        "id": "po-5",
        "check": "Submit button disabled until valid (or shows clear errors).",
        "linked_smell": "silent-errors",
        "severity": "medium",
        "status": "fail",
        "evidence": "No inline validation mentioned.",
        "recommendation": "Add inline validation on field blur. At minimum, show password strength indicator as user types."
      }
    ],
    "critical_failures": ["po-3"],
    "high_failures": []
  },
  "key_principles": ["cognitive-load", "error-prevention-in-forms"],
  "common_smells": ["form-graveyard", "silent-errors"],
  "api_enriched": true,
  "api_note": null
}
```

## Completion Criteria

The output is complete when:

1. `flow_id` matches one of the five valid IDs
2. `phase` accurately reflects what was evaluated
3. Every preflight item has one of: `answered`, `unanswered`, `partial`
4. Every postflight item has one of: `pass`, `fail`, `unknown`
5. `ship_ready` is `false` if any critical postflight item has `status: fail`
6. `gaps` lists only IDs that have `status: unanswered` or `partial`
7. `critical_failures` lists only IDs that have `severity: critical` and `status: fail`
8. `api_enriched` accurately reflects whether toolbox call returned 200
9. The output is valid JSON with no prose before or after

---

# Source: vibe-coding-advisor

Original URL: https://raw.githubusercontent.com/uxuiprinciples/agent-skills/main/vibe-coding-advisor/SKILL.md

---
name: vibe-coding-advisor
version: 1.0.0
description: Inject UX principle context into your AI coding session before generating components. Maps component types (forms, tables, dashboards, navigation, checkout, etc.) to relevant UX principles, fetches their vibeCodingPrompts, and returns an assembled context block ready to paste into Cursor, Claude Code, Windsurf, or any AI coding tool. API key optional — enriched prompts with research citations require uxuiprinciples.com API Access.
author: uxuiprinciples
homepage: https://uxuiprinciples.com
tags:
  - vibe-coding
  - ux
  - ui
  - components
  - cursor
  - code-generation
  - prompts
env:
  UXUI_API_KEY:
    description: API key from uxuiprinciples.com (pro tier returns full vibeCodingPrompts with research citations and specific implementation requirements for your component type)
    required: false
---

```toml
[toolbox.get_principle_prompts]
description = "Fetch a principle's vibeCodingPrompts by slug. Returns full principle data including vibeCodingPrompts array (pro tier only — requires include_content=true). Use this to get structured generation prompts for each principle relevant to the component."
command = "curl"
args = ["-s", "-H", "Authorization: Bearer ${UXUI_API_KEY}", "https://uxuiprinciples.com/api/v1/principles?slug={slug}&include_content=true"]

[toolbox.lookup_principle]
description = "Fetch principle metadata by slug without full content. Returns code, title, aiSummary, businessImpact, and tags. Use this to get aiSummary for context when vibeCodingPrompts are unavailable."
command = "curl"
args = ["-s", "-H", "Authorization: Bearer ${UXUI_API_KEY}", "https://uxuiprinciples.com/api/v1/principles?slug={slug}&include_content=false"]
```

## What This Skill Does

You generate UX-grounded system context for AI coding tools. When a developer is about to build a UI component, you:

1. Identify which UX principles apply to that component type
2. Fetch the relevant `vibeCodingPrompts` from those principles (via API)
3. Select the best-matching prompt for the requested component
4. Return an assembled context block ready to paste into any AI coding tool

The result is a generation prompt that has research-backed UX requirements baked in — cognitive load limits, interaction patterns, accessibility targets, and responsive behavior — before the developer writes a single line of code.

This is not an audit. It is pre-generation context injection.

## Component Type Registry

Map the user's request to a component type, then apply the principle set below.

| Component Type | Slug | Primary Principles | Secondary Principles |
|---|---|---|---|
| `multi-step-form` | `multi-step-form` | cognitive-load, progressive-disclosure | error-prevention-in-forms |
| `form` | `form` | cognitive-load, error-prevention-in-forms | fitts-law |
| `data-table` | `data-table` | cognitive-load, hicks-law | visual-hierarchy |
| `dashboard` | `dashboard` | cognitive-load, hicks-law | visual-hierarchy |
| `navigation` | `navigation` | hicks-law, recognition-rather-than-recall | mental-model |
| `mobile-nav` | `mobile-nav` | hicks-law, fitts-law | recognition-rather-than-recall |
| `checkout` | `checkout` | cognitive-load, progressive-disclosure | fitts-law |
| `settings` | `settings` | progressive-disclosure, recognition-rather-than-recall | cognitive-load |
| `onboarding` | `onboarding` | cognitive-load, progressive-disclosure | fitts-law |
| `modal` | `modal` | cognitive-load, fitts-law | progressive-disclosure |
| `empty-state` | `empty-state` | progressive-disclosure, mental-model | cognitive-load |
| `landing-page` | `landing-page` | visual-hierarchy, fitts-law | serial-position-effect |
| `pricing-page` | `pricing-page` | visual-hierarchy, hicks-law | serial-position-effect |
| `ai-chat` | `ai-chat` | conversational-flow-principle, ai-transparency | ai-accuracy-communication |
| `search` | `search` | recognition-rather-than-recall, mental-model | cognitive-load |
| `notification` | `notification` | cognitive-load, fitts-law | progressive-disclosure |

**Principle slugs for API calls:**

| Principle | Slug | Code |
|---|---|---|
| Cognitive Load | `cognitive-load` | F.1.1.02 |
| Hick's Law | `hicks-law` | F.2.2.03 |
| Visual Hierarchy | `visual-hierarchy` | F.2.1.01 |
| Progressive Disclosure | `progressive-disclosure` | F.3.1.01 |
| Fitts's Law | `fitts-law` | F.4.1.01 |
| Miller's Law | `millers-law` | F.1.2.02 |
| Serial Position Effect | `serial-position-effect` | F.1.3.01 |
| Recognition vs Recall | `recognition-rather-than-recall` | F.4.2.01 |
| Mental Model | `mental-model` | F.4.3.01 |
| Error Prevention | `error-prevention-in-forms` | F.3.3.01 |
| Conversational Flow | `conversational-flow-principle` | S.1.1.01 |
| AI Transparency | `ai-transparency` | S.1.3.01 |
| AI Accuracy Communication | `ai-accuracy-communication` | — |

## Generation Workflow

### Step 1: Identify Component Type

Match the user's request to one of the component types in the registry. Look for:

- Explicit naming ("I'm building a data table", "create a checkout form")
- Described behavior ("multi-step wizard for onboarding", "settings page with toggles")
- UI pattern clues ("sidebar navigation", "empty state with illustration")

If the component spans multiple types (e.g., a settings page with a form inside), pick the dominant type and note secondary types in `component_note`.

If the component type is not in the registry, map it to the closest match. Note the inference in `component_note`.

### Step 2: Extract Tech Stack

Look for tech stack signals in the description:
- Framework: React, Vue, Svelte, Next.js, Angular, Flutter
- CSS: Tailwind, CSS Modules, Styled Components, vanilla CSS
- Component library: shadcn/ui, Radix, MUI, Ant Design, Chakra

If specified, carry the tech stack through into the `assembled_context`. If not specified, note it as `null` and do not assume a stack in the output.

### Step 3: Fetch vibeCodingPrompts (if API key is set)

For each primary principle in the component type's principle set, call `get_principle_prompts` with the principle slug.

The response includes a `vibeCodingPrompts` array. Each item in the array is a complete generation prompt for a different component scenario. Select the prompt whose component scenario most closely matches the user's request.

Selection criteria:
- **Exact match**: the prompt description mentions the same component type
- **Closest functional match**: the prompt covers the same interaction pattern
- **Fallback**: use the first prompt in the array

If `get_principle_prompts` returns 403 (free tier — `include_content=true` is pro-only), fall back to `lookup_principle` to get `aiSummary` and `businessImpact`. Use these to write an internal-knowledge prompt (see Step 3b below).

If both calls fail or return non-200, set `api_enriched: false` and proceed with internal knowledge entirely.

### Step 3b: Internal Knowledge Fallback

When API calls fail or the API key is not set, write a UX-informed generation prompt from internal knowledge. Cover the same ground the vibeCodingPrompts would cover:

- The core UX principle and its threshold (e.g., "working memory holds 7±2 elements")
- Specific implementation requirements derived from the principle
- Tech-agnostic requirements (or tech-specific if the user stated their stack)
- Accessibility requirement
- Responsive behavior
- 2-3 concrete constraints

The internal prompt must be actionable, not generic. "Minimize cognitive load" is not actionable. "Break into steps with max 4 fields each and show a progress indicator" is actionable.

### Step 4: Select and Assemble

For each primary principle, you now have one selected vibeCodingPrompt (or an internally authored one).

Assemble the `assembled_context` field by combining:
1. A one-line component intent statement: "Building: [component type]"
2. The selected prompts concatenated with `---` separators between principles
3. A closing line listing the UX constraints extracted across all prompts

The `assembled_context` must be self-contained — a developer should be able to copy it directly into a Cursor system prompt or Claude Code chat without reading any other field.

### Step 5: Output JSON

Return exactly this structure. No prose before or after.

```json
{
  "component_type": "string",
  "component_note": "string or null",
  "tech_stack": "React + Tailwind + shadcn/ui or null",
  "principles_applied": [
    {
      "code": "F.1.1.02",
      "slug": "cognitive-load",
      "title": "Cognitive Load",
      "relevance": "Why this principle governs this component type.",
      "ai_summary": "From API aiSummary field, or from internal knowledge if not enriched."
    }
  ],
  "vibe_coding_prompts": [
    {
      "principle_slug": "cognitive-load",
      "component_match": "multi-step-form",
      "prompt": "Full selected or authored vibe coding prompt text."
    }
  ],
  "assembled_context": "Full combined context block. Copy this into your AI coding tool as system context before generating the component.",
  "ux_guardrails": [
    "Concrete UX rule extracted from applied principles. One sentence each.",
    "Second rule."
  ],
  "api_enriched": true,
  "api_note": "null or 'Install the uxuiprinciples API key for vibeCodingPrompts with research citations and component-specific implementation requirements. See uxuiprinciples.com/pricing'"
}
```

`ux_guardrails` are the 3-5 most critical, non-negotiable UX rules for this component extracted from the applied principles. These are the rules the generated component must not violate. Extract them from the principle data or from internal knowledge.

`assembled_context` is the field developers copy and paste. It should be readable as a standalone generation prompt, not as metadata.

## Prompt Quality Standards

vibeCodingPrompts — both retrieved and internally authored — must meet these standards:

**Opening line:** Role statement. "You are an expert [role] specializing in [domain]."

**Second paragraph:** Create statement with research citation. "Create a [component] that [UX outcome]: [Research finding with numbers]."

**Requirements block:** Minimum 5 bulleted requirements. Include:
- One count-based rule (e.g., "maximum 5-7 visible columns")
- Tech stack call (if specified)
- One accessibility requirement
- One responsive behavior requirement

**Constraints block:** 2-3 hard constraints. Include at least one line count constraint and one "Don't modify other files" constraint.

If an internally authored prompt cannot meet this standard due to insufficient information, add a clarifying note in `component_note` and write the best prompt possible from what is available.

## Edge Cases

**Component type not in registry:** Match to the closest entry. Explain inference in `component_note`. Do not refuse — always return a prompt.

**Tech stack not specified:** Write tech-agnostic requirements. Do not assume a stack. Leave `tech_stack: null`. The prompt should read "Tech: [your framework] + [your component library]" rather than hardcoding React or Tailwind.

**Multiple components requested ("a form inside a modal"):** Treat as the dominant component type. Note the nested component in `component_note`. The prompt should reference both but optimize for the outer container.

**API key set but `include_content=true` returns 403:** This is the free tier. Fall back to `lookup_principle` (no content). Author internal prompts. Set `api_enriched: false`.

**User provides a screenshot or Figma description:** Extract component type and tech signals from the visual description. Proceed as normal.

**Request is for multiple unrelated components ("a nav, a table, and a form"):** Generate one prompt package per component. Return an array of outputs or run the workflow three times and combine. Note the multi-component request in `component_note` of each.

**Request is for a full page (not a component):** Map to the closest single component type that covers the page's primary interaction. Note the scope in `component_note`. A full page audit belongs in `uxui-evaluator`, not here.

## Examples

### Example 1: Data Table with Tech Stack

**Input:**
```
I'm building a data table in React with shadcn/ui. It shows user records with sortable columns.
```

**Expected output structure:**
```json
{
  "component_type": "data-table",
  "component_note": null,
  "tech_stack": "React + shadcn/ui",
  "principles_applied": [
    {
      "code": "F.1.1.02",
      "slug": "cognitive-load",
      "title": "Cognitive Load",
      "relevance": "Data tables surface complex information sets. Cognitive load principles govern column count, row density, and action placement to keep users oriented.",
      "ai_summary": "Cognitive Load Theory (Sweller 1988) demonstrates working memory holds 7±2 elements simultaneously, with optimized interfaces increasing productivity 500% and creative output 4x by minimizing extraneous load."
    },
    {
      "code": "F.2.2.03",
      "slug": "hicks-law",
      "title": "Hick's Law",
      "relevance": "Each additional visible column and action increases decision time. Hick's Law governs how many choices to surface at once.",
      "ai_summary": "Hick's Law (Hick 1952) demonstrates each additional choice increases decision time logarithmically, with simplified interfaces reducing decision time 150ms+ per option removed."
    }
  ],
  "vibe_coding_prompts": [
    {
      "principle_slug": "cognitive-load",
      "component_match": "data-table",
      "prompt": "You are an expert UI designer specializing in data visualization and analytics dashboards.\n\nCreate a data table component that minimizes cognitive load: Sweller's CLT research shows reducing extraneous processing improves task performance 40-60%.\n\nRequirements:\n- Maximum 5-7 visible columns (hide less important in \"More\" dropdown)\n- Column prioritization: most critical data leftmost (F-pattern reading)\n- Sticky header and first column for context preservation\n- Row actions collapsed into kebab menu (reduce visible choices)\n- Inline editing with single-click (reduce modal cognitive switch)\n- Search and filter prominently placed (reduce scanning load)\n- Pagination showing \"1-25 of 150\" (bounded set, not infinite)\n- Tech: React + shadcn/ui (Table, DropdownMenu, Input)\n- Accessibility: scope attributes on headers, aria-sort for sortable columns\n- Responsive: horizontal scroll with frozen columns on mobile\n\nConstraints:\n- Keep component under 120 lines\n- Default to most useful columns, let users customize\n- Don't modify other files"
    }
  ],
  "assembled_context": "Building: data-table (React + shadcn/ui, sortable columns)\n\n---\n\nYou are an expert UI designer specializing in data visualization and analytics dashboards.\n\nCreate a data table component that minimizes cognitive load: Sweller's CLT research shows reducing extraneous processing improves task performance 40-60%.\n\n[... full prompt text ...]\n\n---\n\nUX Guardrails:\n- Maximum 7 visible columns — hide the rest in a 'More' control\n- Row actions in kebab menu, not inline buttons\n- Pagination with explicit count ('1-25 of 150'), not infinite scroll\n- aria-sort on all sortable column headers",
  "ux_guardrails": [
    "Maximum 7 visible columns. Hide secondary columns behind a 'More' or column picker control.",
    "Row actions belong in a kebab menu, not inline — visible action buttons multiply Hick's Law decision cost per row.",
    "Show bounded pagination ('1-25 of 150'), not infinite scroll. Users need a sense of the set size.",
    "Sticky header preserves column context during scroll. Required when table exceeds viewport height."
  ],
  "api_enriched": true,
  "api_note": null
}
```

### Example 2: No API Key, Generic Component

**Input:**
```
Building a settings page for a SaaS app. Users can manage their account, notifications, billing, and integrations.
```

**Expected output structure:**
```json
{
  "component_type": "settings",
  "component_note": "Four distinct sections (account, notifications, billing, integrations) — applies progressive disclosure heavily.",
  "tech_stack": null,
  "principles_applied": [
    {
      "code": "F.3.1.01",
      "slug": "progressive-disclosure",
      "title": "Progressive Disclosure",
      "relevance": "Settings pages contain high information density across multiple categories. Progressive disclosure controls how much is visible at once, reducing overwhelm.",
      "ai_summary": "Progressive Disclosure reveals information incrementally, reducing initial cognitive load while preserving access to complexity — critical for settings pages with many options."
    },
    {
      "code": "F.4.2.01",
      "slug": "recognition-rather-than-recall",
      "title": "Recognition Rather Than Recall",
      "relevance": "Users should recognize their current setting state (on/off, current plan) without remembering it from elsewhere.",
      "ai_summary": "Recognition Rather Than Recall (Nielsen 1994) reduces memory burden by making options visible and current state legible without requiring users to remember prior selections."
    }
  ],
  "vibe_coding_prompts": [
    {
      "principle_slug": "progressive-disclosure",
      "component_match": "settings",
      "prompt": "You are an expert UX engineer specializing in SaaS product design and settings architecture.\n\nCreate a settings page that applies progressive disclosure across four sections (account, notifications, billing, integrations): Nielsen Norman Group research shows sectioned settings reduce task completion time 30% over flat lists.\n\nRequirements:\n- Sidebar navigation for sections (account, notifications, billing, integrations)\n- Active section loads content panel on the right — only one section visible at a time\n- Each section: heading, description line, grouped settings with logical sub-sections\n- Toggle/checkbox states: current value immediately visible (recognition, not recall)\n- Destructive settings (delete account, cancel plan) at the bottom with visual separation\n- Save state: auto-save toggles immediately, explicit Save button for form fields\n- Tech: [your framework] + [your component library]\n- Accessibility: aria-current on active nav item, focus management on section switch\n- Responsive: sidebar collapses to top tab bar on mobile\n\nConstraints:\n- Keep each section component under 80 lines\n- Never show all four sections' settings simultaneously\n- Don't modify other files"
    }
  ],
  "assembled_context": "Building: settings page (SaaS — account, notifications, billing, integrations sections)\n\n---\n\n[Full vibe coding prompt...]\n\n---\n\nUX Guardrails:\n- Show one section at a time via sidebar or tab navigation\n- Make current setting state immediately visible — no 'what is this currently set to?' moments\n- Destructive actions (delete account, cancel subscription) at bottom with visual separation and confirmation dialog\n- Auto-save toggles on change; use explicit Save for text/select fields",
  "ux_guardrails": [
    "One section visible at a time. Use sidebar nav or tabs. Never show all settings simultaneously.",
    "Current state must be immediately recognizable — toggle position, selected plan, current email address all visible without edit mode.",
    "Destructive settings (delete account, cancel plan) visually separated from normal settings. Bottom of section, destructive color, confirmation required.",
    "Group related settings. Cognitive grouping reduces scanning load. Max 5-7 settings per group before adding a sub-section header."
  ],
  "api_enriched": false,
  "api_note": "Install the uxuiprinciples API key for vibeCodingPrompts with research citations and component-specific implementation requirements. See uxuiprinciples.com/pricing"
}
```

## Completion Criteria

The output is complete when:

1. `component_type` matches one of the 16 registered types or is inferred with a note
2. Every entry in `principles_applied` has a `code`, `slug`, `title`, `relevance`, and `ai_summary`
3. Every entry in `vibe_coding_prompts` has a `principle_slug`, `component_match`, and a `prompt` that meets the quality standards (role line, create statement with research, requirements block, constraints block)
4. `assembled_context` is self-contained and readable without the rest of the JSON
5. `ux_guardrails` has 3-5 entries, each a single actionable sentence
6. `api_enriched` accurately reflects whether `get_principle_prompts` returned 200 with content
7. The output is valid JSON with no prose before or after
8. If `api_enriched: false`, `api_note` contains the upgrade message

---

