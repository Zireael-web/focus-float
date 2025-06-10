# FocusFloat — Component Inventory & Screen Spec

Reference implementation lives in the mockup (`FocusFloat.html` + `ff-components.jsx`). Every component below maps to a Composable. Token names refer to `TOKENS.md`.

## Components

### AppTextRow
Text-first launcher row. Props: `name`, `state` (normal · paused · protected · hidden), `meta?`, `iconsEnabled?`.
- Height `rowH` (home favorites use `favRowH`). Horizontal padding `screenPad`. Press feedback = `press`.
- Optional leading icon = rounded-square monogram on `surface2` (off by default — icons are an opt-in setting, never the default).
- `paused`: name `text2`, hollow dot + caption "paused until 00:00".
- `protected`: name `text2`, caption "protected". `hidden`: name `text3`.

### PauseActionBlock
Home pause/unpause surface, pinned below favorites with a top hairline.
- **Ready:** single `appRow` action line — "Pause Dopamine until midnight".
- **Paused:** hollow dot + "Dopamine paused until 00:00" (`body`) + "Unpause now" (`button`).
- **Setup required / partial failure:** renders a `StatusBanner` instead.

### StatusBanner  *(persistent — the core error-visibility invariant)*
Props: `kind` (setup · failed · info), `title`, `sub?`, `action?`.
- `setup`/`info`: `surface` bg, 1px `line`, neutral dot.
- `failed`: `errDim` bg, solid `err` dot, `err` title. Radius `banner`.
- Always on-screen for unresolved Shizuku / pause errors. Toasts may accompany but never replace it.

### BottomSheet
Grabber (38×4 `text3`) + optional title/sub + action rows (`sheetRowH`). Top radius `sheet`, `surface` bg, upward shadow, scrim `rgba(0,0,0,.55)`.
- Rows: `label`, `kind` (normal · muted · destructive), `disabled`, `dot?`, `sub?`.
- Variants: App actions, App actions (protected — hides "Add to pause category", shows "Protected"), Add to category, Paused app, Pause result / error details.

### Dialog
Centered card, radius `dialog`, `surface`, 1px `line`. Title `sheetTitle`, body `body`/`text2`, optional underlined text input, right-aligned text buttons. Scrim `rgba(0,0,0,.6)`.

### SearchField
Underlined (`line`) row with trailing magnifier. Placeholder `text3`. Active = caret + value in `text`. Empty & no-results states show a centered `body`/`text2` line.

### Scrubber
Right-edge A–Z fast index. Active letter `text` + 600 weight; rest `scrubber`. Sits above the list, vertically centered.

### SettingsRow
`label` (+ optional `sub`) with trailing `value`, `chevron`, or expand caret. Min height 56, padding `screenPad`.

### CodeBlock
Quiet expandable technical block on `surface2`, radius 12. Rows of `section`-label + `mono` value; failing rows use `err`. Used in Pause-result details and Debug.

### Snackbar
Transient confirm on `surface2`, radius 14, with optional action. Always paired with persistent UI for errors.

### Small parts
StatusDot (hollow/solid/err) · Toggle (44×26 pill) · SelectRow (checkbox/radio) · TopBar (back + title) · SectionLabel (`section`) · Corner glyphs (phone / camera).

---

## Screen inventory

**Home** — Ready · Category paused · Shizuku setup required · Partial failure.
Layout: clock + battery arc (top) → flexible space → favorites → PauseActionBlock → "Search apps" entry → phone/camera corners. Long-press empty area → settings. First screen after setup is this working Home, never a landing page.

**All Apps & Search** — All apps (text-only, A–Z scrubber, gear bottom-right) · row-state showcase · icons-on (optional) · Search results · Search empty · Search no-results.

**Bottom Sheets** — App actions · App actions (protected) · Add to category · Paused app.

**Pause Category (Dopamine)** — Ready · Paused · Setup required · Failed to unpause · Pause result / error details. Each shows a bottom Status line (ready · paused · setup required · failed to unpause).

**Onboarding** — Welcome (local-only: no account/server/analytics) → Set as home screen → Pick favorites → Create pause category (default "Dopamine", editable) → Add apps (protected disabled) → Connect Shizuku (not installed · not running · permission required · ready) → Done. Progress dashes at top.

**Settings** — Index (Home screen · Favorites · Hidden apps · Pause categories · Appearance · Shizuku · Debug) · Appearance (theme + text size + clock/date toggles) · Hidden apps (+ restore) · Shizuku status · Debug (status + last command CodeBlock).

**Dialogs & Snackbar** — Rename app · Rename category · Create category · Confirm unpause · Permission explanation (exact alarm) · Snackbar.

**Themes & Accessibility** — every screen across AMOLED / Dark / Light + large-font (×1.3) variants.

---

## Hard constraints (from brief)
- No marketing hero, illustrations, mascots, charts, or dashboards.
- No cards-in-cards. No heavy gradients.
- App icons are an optional state, never the default (text-only default).
- Errors live in persistent UI, not toast-only.
- Vocabulary: Pause / Paused / Unpause / Setup required. Avoid block(ing)/blocker, backend, VPN, tracking, analytics.
