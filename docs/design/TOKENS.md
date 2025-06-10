# FocusFloat — Design Tokens

Minimalist Android launcher for Google Pixel 7. Near-monochrome; **color is reserved for errors only**. All UI is English.

Device: **Pixel 7 — 1080 × 2400 px @ 2.625 density → 411 × 414 dp logical** (mockups drawn at 412 × 916 dp). Portrait-first.

These tokens are mirrored 1:1 in `ff-tokens.js` (the source the mockups render from). Use them as the Compose theme contract.

---

## Color tokens

Three themes. Same semantic names; swap the value block. Primary theme = **AMOLED Black**.

| Token | Role | AMOLED Black | Dark | Light |
|---|---|---|---|---|
| `bg` | screen background | `#000000` | `#111113` | `#FFFFFF` |
| `surface` | sheets / dialogs / banners | `#0C0C0D` | `#1C1C1F` | `#FFFFFF` |
| `surface2` | pressed rows, inset / code blocks | `#161618` | `#26262A` | `#F1F1F2` |
| `text` | primary content | `#F4F4F5` | `#EAEAEC` | `#161618` |
| `text2` | secondary / meta | `rgba(244,244,245,.56)` | `rgba(234,234,236,.58)` | `rgba(22,22,24,.56)` |
| `text3` | disabled / hint | `rgba(244,244,245,.30)` | `rgba(234,234,236,.32)` | `rgba(22,22,24,.32)` |
| `line` | hairline divider | `rgba(244,244,245,.10)` | `rgba(234,234,236,.12)` | `rgba(22,22,24,.12)` |
| `line2` | faint divider | `rgba(244,244,245,.06)` | `rgba(234,234,236,.07)` | `rgba(22,22,24,.07)` |
| `press` | touch-feedback overlay | `rgba(244,244,245,.07)` | `rgba(234,234,236,.08)` | `rgba(22,22,24,.05)` |
| `dot` | neutral status dot (paused) | `rgba(244,244,245,.40)` | `rgba(234,234,236,.42)` | `rgba(22,22,24,.45)` |
| `err` | **only hue in the system** | `#E26A5C` | `#E57367` | `#C5392E` |
| `errDim` | error surface tint | `rgba(226,106,92,.16)` | `rgba(229,115,103,.18)` | `rgba(197,57,46,.10)` |
| `scrubber` | A–Z rail | `rgba(244,244,245,.40)` | `rgba(234,234,236,.42)` | `rgba(22,22,24,.45)` |

`err` is muted/desaturated on purpose — calm, not alarming. Never use it for anything other than failure states.

---

## Typography — Roboto

`clock` is Roboto **Light (300)**. Everything else **Regular (400)**, with **Medium (500)** reserved for buttons and active labels. Sizes in **sp** and scale with the user's text-size setting (see below).

| Style | Size | Weight | Tracking | Use |
|---|---|---|---|---|
| `clock` | 52 | 300 | -0.5 | home clock |
| `date` | 15 | 400 | 0 | home date |
| `appRow` | 22 | 400 | 0 | home + all-apps rows |
| `sheetTitle` | 19 | 500 | 0 | bottom-sheet / dialog title |
| `body` | 16 | 400 | 0 | banner / dialog body |
| `button` | 16 | 500 | 0 | text actions |
| `section` | 12 | 500 | 0.6, UPPERCASE | group headers |
| `caption` | 13 | 400 | 0 | meta — "paused until 00:00" |
| `mono` | 12 | 400 (Roboto Mono) | 0 | debug stdout / stderr |

**Text-size setting** multiplies every size: Small ×0.85 · Medium ×1.0 · Large ×1.15 · Extra large ×1.3. The mockups model this as `--ff-fs`; in Compose, use `fontScale` / scaled sp.

---

## Spacing & shape (dp)

| Token | Value | Use |
|---|---|---|
| `screenPad` | 24 | left/right screen gutter |
| `rowH` | 56 | all-apps list row |
| `favRowH` | 52 | home favorite row |
| `sheetPad` | 24 | bottom-sheet horizontal padding |
| `sheetRowH` | 56 | bottom-sheet action row |
| `tap` | 48 | **minimum** touch target |
| `statusH` | 36 | reserved system status strip |
| `gestureH` | 24 | bottom gesture bar |

Corner radius: `sheet` 26 · `dialog` 28 · `banner` 16 · app-icon placeholder 8.

---

## States

| State | Treatment |
|---|---|
| normal | `text` |
| pressed | row background = `press` |
| disabled | `text3`, no ripple |
| paused | name dimmed to `text2`, `caption` meta "paused until 00:00", **hollow** dot |
| protected | name `text2`, meta "protected", no pause/hide actions |
| hidden | name `text3`, excluded from list & search |
| setup required | **persistent** banner on `surface`, neutral dot, action e.g. "Open Shizuku" |
| error / failed | `err` text, **solid** `err` dot, `errDim` surface — **persistent**, never toast-only |

**Status dot:** hollow ring = paused · solid `text` = ready · solid `err` = failed.

---

## Buttons

- **Primary (text action):** `text`, `button` style, optional top hairline, full-width centered or left-aligned.
- **Primary (filled):** background `text`, label `bg` — onboarding CTAs only.
- **Secondary:** `text2`.
- **Destructive / critical:** `err`.
- Disabled: `text3` (filled → `surface2` bg).

---

## Copy rules

Use **Pause / Paused / Unpause / Setup required**. Never "block / blocking / blocker", "backend", "VPN", "tracking", "analytics".
