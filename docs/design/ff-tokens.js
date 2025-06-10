/* FocusFloat — Design Tokens
 * Pixel 7 launcher. Near-monochrome. Color reserved for errors only.
 * Logical units = dp/sp (Pixel 7: 1080×2400 px @ 2.625 → 411×914 dp).
 *
 * Themes expose the same CSS custom-property contract so any component can be
 * re-skinned by swapping the variable block on a <Screen> root. */
(function () {
  // ── Palettes ──────────────────────────────────────────────────────────
  // text  = primary content   text2 = secondary/meta   text3 = disabled/hint
  // line  = hairline divider   press = touch-feedback overlay
  // err   = the ONLY hue in the system (muted, desaturated — calm not alarming)
  const THEMES = {
    amoled: {
      label: 'AMOLED Black',
      vars: {
        '--ff-bg': '#000000',
        '--ff-surface': '#0c0c0d',     // sheets / dialogs sit barely above black
        '--ff-surface2': '#161618',    // pressed sheet rows, inset blocks
        '--ff-text': '#f4f4f5',
        '--ff-text2': 'rgba(244,244,245,0.56)',
        '--ff-text3': 'rgba(244,244,245,0.30)',
        '--ff-line': 'rgba(244,244,245,0.10)',
        '--ff-line2': 'rgba(244,244,245,0.06)',
        '--ff-press': 'rgba(244,244,245,0.07)',
        '--ff-dot': 'rgba(244,244,245,0.40)',  // neutral status dot (paused)
        '--ff-err': '#e26a5c',
        '--ff-err-dim': 'rgba(226,106,92,0.16)',
        '--ff-scrubber': 'rgba(244,244,245,0.40)',
      },
    },
    dark: {
      label: 'Dark',
      vars: {
        '--ff-bg': '#111113',
        '--ff-surface': '#1c1c1f',
        '--ff-surface2': '#26262a',
        '--ff-text': '#eaeaec',
        '--ff-text2': 'rgba(234,234,236,0.58)',
        '--ff-text3': 'rgba(234,234,236,0.32)',
        '--ff-line': 'rgba(234,234,236,0.12)',
        '--ff-line2': 'rgba(234,234,236,0.07)',
        '--ff-press': 'rgba(234,234,236,0.08)',
        '--ff-dot': 'rgba(234,234,236,0.42)',
        '--ff-err': '#e57367',
        '--ff-err-dim': 'rgba(229,115,103,0.18)',
        '--ff-scrubber': 'rgba(234,234,236,0.42)',
      },
    },
    light: {
      label: 'Light',
      vars: {
        '--ff-bg': '#ffffff',
        '--ff-surface': '#ffffff',
        '--ff-surface2': '#f1f1f2',
        '--ff-text': '#161618',
        '--ff-text2': 'rgba(22,22,24,0.56)',
        '--ff-text3': 'rgba(22,22,24,0.32)',
        '--ff-line': 'rgba(22,22,24,0.12)',
        '--ff-line2': 'rgba(22,22,24,0.07)',
        '--ff-press': 'rgba(22,22,24,0.05)',
        '--ff-dot': 'rgba(22,22,24,0.45)',
        '--ff-err': '#c5392e',
        '--ff-err-dim': 'rgba(197,57,46,0.10)',
        '--ff-scrubber': 'rgba(22,22,24,0.45)',
      },
    },
  };

  // ── Type scale (sp) ───────────────────────────────────────────────────
  // Roboto. weight: clock is Light(300); everything else Regular(400) with
  // Medium(500) reserved for buttons/active labels.
  const TYPE = {
    clock:    { size: 52, weight: 300, ls: '-0.5px', lh: 1.0 },
    date:     { size: 15, weight: 400, ls: '0px',    lh: 1.2 },
    appRow:   { size: 22, weight: 400, ls: '0px',    lh: 1.2 },  // home + all-apps rows
    sheetTitle:{ size: 19, weight: 500, ls: '0px',   lh: 1.25 },
    section:  { size: 12, weight: 500, ls: '0.6px',  lh: 1.2, upper: true }, // group headers
    body:     { size: 16, weight: 400, ls: '0px',    lh: 1.45 },
    button:   { size: 16, weight: 500, ls: '0px',    lh: 1.2 },
    caption:  { size: 13, weight: 400, ls: '0px',    lh: 1.3 },  // meta: "paused until 00:00"
    mono:     { size: 12, weight: 400, ls: '0px',    lh: 1.5 },  // debug stdout/stderr
  };

  // ── Spacing (dp) ──────────────────────────────────────────────────────
  const SPACE = {
    screenPad: 24,   // left/right screen gutter
    rowH: 56,        // all-apps list row
    favRowH: 52,     // home favorite row
    sheetPad: 24,    // bottom-sheet horizontal padding
    sheetRowH: 56,   // bottom-sheet action row
    radiusSheet: 26,
    radiusDialog: 28,
    radiusBanner: 16,
    tap: 48,         // minimum touch target
    statusH: 36,     // reserved system status strip
    gestureH: 24,    // bottom gesture bar
  };

  // Device
  const DEVICE = { w: 412, h: 916, name: 'Pixel 7', px: '1080 × 2400' };

  window.FF = { THEMES, TYPE, SPACE, DEVICE };
})();
