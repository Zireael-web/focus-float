# FocusFloat — Design Reference

A minimalist Android **launcher** for Google Pixel 7. Near-monochrome, AMOLED-black first.
The original concept pauses a category of apps (e.g. "Dopamine") until midnight via **Shizuku**.

## Read in this order
1. **TOKENS.md** — colors (AMOLED / Dark / Light), Roboto type scale, spacing, radii, states.
2. **COMPONENTS.md** — every component → Composable mapping, full screen inventory, hard constraints.
3. **FocusFloat.html** — the interactive mockup of all ~45 screens & states. Open it through a local
   static server (pan/zoom canvas; click any screen to view fullscreen).

## Sources
- `ff-tokens.js` — the exact token object the mockups render from. Mirrored in the Compose theme.
- `ff-components.jsx` — reference markup for AppTextRow, StatusBanner, BottomSheet, Dialog,
  SearchField, Scrubber, SettingsRow, CodeBlock, etc.
- `ff-screens-a.jsx` / `ff-screens-b.jsx` — composition of each screen.
- `ff-docs.jsx`, `design-canvas.jsx` — render the spec/canvas (not needed for the app itself).

## Hard constraints
- First screen after setup is the working Home, never a landing page.
- App icons are an optional state; text-only is the default.
- All Shizuku/pause errors live in persistent UI, never toast-only.
- No marketing hero, illustrations, mascots, charts, dashboards, cards-in-cards, heavy gradients.
- Color is reserved for errors only.
- Vocabulary: Pause / Paused / Unpause / Setup required. Never block(ing), backend, VPN, tracking, analytics.
