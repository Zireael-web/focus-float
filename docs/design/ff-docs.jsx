/* FocusFloat — token & component spec cards for the canvas */
(function () {
  const { THEMES, TYPE, SPACE } = window.FF;
  const ty = window.FFty;
  const SANS = "'Roboto', system-ui, sans-serif";

  function DocCard({ children, pad = 28 }) {
    return (
      <div style={Object.assign({}, THEMES.amoled.vars, {
        width: '100%', height: '100%', background: 'var(--ff-bg)', color: 'var(--ff-text)',
        fontFamily: SANS, padding: pad, boxSizing: 'border-box', overflow: 'hidden',
      })}>{children}</div>
    );
  }
  const Head = ({ children, sub }) => (
    <div style={{ marginBottom: 22 }}>
      <div style={{ fontSize: 19, fontWeight: 500 }}>{children}</div>
      {sub && <div style={{ fontSize: 13, color: 'var(--ff-text2)', marginTop: 4 }}>{sub}</div>}
    </div>
  );

  // ── Color tokens (one card per theme) ────────────────────────
  const SWATCH_KEYS = [
    ['--ff-bg', 'bg'], ['--ff-surface', 'surface'], ['--ff-surface2', 'surface2'],
    ['--ff-text', 'text'], ['--ff-text2', 'text2 · secondary'], ['--ff-text3', 'text3 · disabled'],
    ['--ff-line', 'line · divider'], ['--ff-press', 'press'], ['--ff-dot', 'dot · paused'],
    ['--ff-err', 'err'], ['--ff-err-dim', 'err-dim'],
  ];
  function ColorCard({ themeKey }) {
    const t = THEMES[themeKey];
    return (
      <div style={Object.assign({}, t.vars, {
        width: '100%', height: '100%', background: 'var(--ff-bg)', color: 'var(--ff-text)',
        fontFamily: SANS, padding: 26, boxSizing: 'border-box',
      })}>
        <div style={{ fontSize: 18, fontWeight: 500 }}>{t.label}</div>
        <div style={{ fontSize: 12, color: 'var(--ff-text2)', marginTop: 3, marginBottom: 20 }}>Color tokens</div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 11 }}>
          {SWATCH_KEYS.map(([k, label]) => (
            <div key={k} style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <span style={{ width: 30, height: 30, borderRadius: 8, flex: '0 0 auto', background: t.vars[k], border: '1px solid var(--ff-line)' }} />
              <span style={{ flex: 1, minWidth: 0 }}>
                <span style={{ display: 'block', fontSize: 13, fontWeight: 500 }}>{label}</span>
                <span style={{ display: 'block', fontSize: 11, color: 'var(--ff-text2)', fontFamily: 'ui-monospace, monospace', marginTop: 1 }}>{t.vars[k]}</span>
              </span>
            </div>
          ))}
        </div>
      </div>
    );
  }
  window.FFColorCard = ColorCard;

  // ── Type scale ───────────────────────────────────────────────
  const TYPE_ROWS = [
    ['clock', 'Clock', '18:31'], ['date', 'Date', 'Tuesday, 9 June'],
    ['appRow', 'App row', 'YouTube'], ['sheetTitle', 'Sheet title', 'Add to category'],
    ['body', 'Body', 'Pausing is unavailable.'], ['button', 'Button', 'Unpause now'],
    ['section', 'Section label', 'STATUS'], ['caption', 'Caption · meta', 'paused until 00:00'],
    ['mono', 'Mono · debug', 'exit code 0'],
  ];
  window.FFTypeCard = () => (
    <DocCard>
      <Head sub="Roboto · sp">Typography</Head>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
        {TYPE_ROWS.map(([k, label, sample]) => {
          const t = TYPE[k];
          return (
            <div key={k}>
              <div style={{ fontSize: 11, color: 'var(--ff-text2)', marginBottom: 4, fontFamily: 'ui-monospace, monospace' }}>
                {label} · {t.size}/{t.weight}{t.upper ? ' · caps' : ''}
              </div>
              <div style={{ fontSize: Math.min(t.size, 30), fontWeight: t.weight, letterSpacing: t.ls, textTransform: t.upper ? 'uppercase' : 'none', fontFamily: k === 'mono' ? 'ui-monospace, monospace' : SANS }}>{sample}</div>
            </div>
          );
        })}
      </div>
    </DocCard>
  );

  // ── Spacing & radii ──────────────────────────────────────────
  window.FFSpacingCard = () => {
    const rows = [
      ['screenPad', SPACE.screenPad, 'screen gutter'], ['rowH', SPACE.rowH, 'app list row'],
      ['favRowH', SPACE.favRowH, 'home fav row'], ['sheetPad', SPACE.sheetPad, 'sheet padding'],
      ['sheetRowH', SPACE.sheetRowH, 'sheet row'], ['tap', SPACE.tap, 'min touch target'],
    ];
    const radii = [['radiusSheet', SPACE.radiusSheet], ['radiusDialog', SPACE.radiusDialog], ['radiusBanner', SPACE.radiusBanner]];
    return (
      <DocCard>
        <Head sub="dp">Spacing & radii</Head>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          {rows.map(([k, v, d]) => (
            <div key={k} style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
              <span style={{ height: 10, width: v, background: 'var(--ff-text)', borderRadius: 2, flex: '0 0 auto', maxWidth: 120 }} />
              <span style={{ fontSize: 13, fontWeight: 500, width: 78 }}>{v}dp</span>
              <span style={{ fontSize: 12, color: 'var(--ff-text2)' }}>{k} · {d}</span>
            </div>
          ))}
        </div>
        <div style={{ marginTop: 26, marginBottom: 12, fontSize: 12, color: 'var(--ff-text2)', textTransform: 'uppercase', letterSpacing: '0.6px' }}>Corner radius</div>
        <div style={{ display: 'flex', gap: 16 }}>
          {radii.map(([k, v]) => (
            <div key={k} style={{ textAlign: 'center' }}>
              <div style={{ width: 56, height: 56, background: 'var(--ff-surface2)', border: '1px solid var(--ff-line)', borderRadius: v, borderTopLeftRadius: v, borderTopRightRadius: v }} />
              <div style={{ fontSize: 11, marginTop: 6 }}>{v}</div>
            </div>
          ))}
        </div>
      </DocCard>
    );
  };

  // ── States reference ─────────────────────────────────────────
  window.FFStatesCard = () => {
    const { FFAppRow: AppRow, FFDot: Dot } = window;
    return (
      <div style={Object.assign({}, THEMES.amoled.vars, { width: '100%', height: '100%', background: 'var(--ff-bg)', color: 'var(--ff-text)', fontFamily: SANS, boxSizing: 'border-box' })}>
        <div style={{ padding: '26px 26px 14px' }}>
          <div style={{ fontSize: 19, fontWeight: 500 }}>Row states</div>
          <div style={{ fontSize: 13, color: 'var(--ff-text2)', marginTop: 4 }}>AppTextRow — every state</div>
        </div>
        <AppRow name="YouTube" />
        <AppRow name="YouTube" pressed />
        <AppRow name="Instagram" state="paused" />
        <AppRow name="Phone" state="protected" />
        <AppRow name="System UI" state="hidden" />
        <div style={{ padding: '12px 26px', display: 'flex', gap: 24, alignItems: 'center', borderTop: '1px solid var(--ff-line2)', marginTop: 8 }}>
          {[['ready', 'ready'], ['paused', 'paused'], ['failed', 'failed']].map(([k, l]) => (
            <span key={k} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12, color: 'var(--ff-text2)' }}><Dot kind={k} size={9} />{l}</span>
          ))}
        </div>
      </div>
    );
  };

  // ── Component inventory ──────────────────────────────────────
  window.FFInventoryCard = () => {
    const items = [
      ['AppTextRow', 'Text-first app row · normal / paused / protected / hidden'],
      ['PauseActionBlock', 'Home pause / unpause action with status line'],
      ['StatusBanner', 'Persistent setup / failure surface — never toast-only'],
      ['BottomSheet', 'Grabber + title + action rows; app actions, add-to-category'],
      ['Dialog', 'Centered confirm / rename / permission'],
      ['SearchField', 'Underlined search with magnifier + A–Z scrubber'],
      ['SettingsRow', 'Label + value / chevron list row'],
      ['StatusDot', 'Hollow (paused) · solid (ready) · error (failed)'],
      ['CodeBlock', 'Quiet expandable stdout / stderr / exit code'],
      ['Snackbar', 'Transient confirm — paired with persistent UI'],
    ];
    return (
      <DocCard>
        <Head sub="Build these in Compose">Component inventory</Head>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 15 }}>
          {items.map(([n, d]) => (
            <div key={n} style={{ borderLeft: '2px solid var(--ff-line)', paddingLeft: 13 }}>
              <div style={{ fontSize: 14, fontWeight: 500 }}>{n}</div>
              <div style={{ fontSize: 12, color: 'var(--ff-text2)', marginTop: 2, lineHeight: 1.4 }}>{d}</div>
            </div>
          ))}
        </div>
      </DocCard>
    );
  };

})();
