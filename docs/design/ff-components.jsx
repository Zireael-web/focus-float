/* FocusFloat — shared launcher primitives.
 * All components read theme via CSS custom props set by <Screen>.
 * Type sizes scale with --ff-fs (accessibility font). */
(function () {
  const { TYPE, SPACE, DEVICE } = window.FF;

  // type token -> style object (size scales with --ff-fs)
  function ty(name, extra) {
    const t = TYPE[name];
    return Object.assign({
      fontSize: `calc(${t.size}px * var(--ff-fs, 1))`,
      fontWeight: t.weight,
      letterSpacing: t.ls,
      lineHeight: t.lh,
      textTransform: t.upper ? 'uppercase' : 'none',
    }, extra || {});
  }
  window.FFty = ty;

  const SANS = "'Roboto', system-ui, sans-serif";

  // ─────────────────────────────────────────────────────────────
  // Icons — single weight line set, currentColor
  // ─────────────────────────────────────────────────────────────
  function Ic({ d, size = 22, fill, stroke = 1.7, vb = 24, children, style }) {
    return (
      <svg width={size} height={size} viewBox={`0 0 ${vb} ${vb}`} fill={fill || 'none'}
        stroke={fill ? 'none' : 'currentColor'} strokeWidth={stroke} strokeLinecap="round"
        strokeLinejoin="round" style={style}>
        {d ? <path d={d} /> : children}
      </svg>
    );
  }
  const Icon = {
    search: (p) => <Ic {...p} d="M11 4a7 7 0 105 11.9M20 21l-4.3-4.3" />,
    gear:   (p) => <Ic {...p} vb={24}><circle cx="12" cy="12" r="3.2"/><path d="M12 2.5v3M12 18.5v3M21.5 12h-3M5.5 12h-3M18.7 5.3l-2.1 2.1M7.4 16.6l-2.1 2.1M18.7 18.7l-2.1-2.1M7.4 7.4 5.3 5.3"/></Ic>,
    phone:  (p) => <Ic {...p} fill="currentColor" stroke="none" d="M6.6 3.5c.5 0 .9.3 1 .8l.9 3.2c.1.5 0 1-.4 1.3l-1.6 1.3a13 13 0 005.7 5.7l1.3-1.6c.3-.4.8-.5 1.3-.4l3.2.9c.5.1.8.5.8 1v3.3c0 .8-.7 1.5-1.5 1.4C9.8 21 3 14.2 3 5.4 3 4.6 3.7 4 4.5 4z" />,
    camera: (p) => <Ic {...p}><path d="M3.5 8.5A1.5 1.5 0 015 7h1.6l1-1.6a1 1 0 01.85-.4h5.1a1 1 0 01.85.4l1 1.6H19a1.5 1.5 0 011.5 1.5v9A1.5 1.5 0 0119 18H5a1.5 1.5 0 01-1.5-1.5z"/><circle cx="12" cy="12.5" r="3.1"/></Ic>,
    chevDown:(p) => <Ic {...p} d="M6 9.5l6 6 6-6" />,
    chevRight:(p)=> <Ic {...p} d="M9 6l6 6-6 6" />,
    back:   (p) => <Ic {...p} d="M15 5l-7 7 7 7M8 12h12" />,
    check:  (p) => <Ic {...p} d="M5 12.5l4.5 4.5L19 6.5" />,
    plus:   (p) => <Ic {...p} d="M12 5v14M5 12h14" />,
    x:      (p) => <Ic {...p} d="M6 6l12 12M18 6L6 18" />,
    calendar:(p)=> <Ic {...p}><rect x="4" y="5.5" width="16" height="15" rx="2"/><path d="M4 9.5h16M8 3.5v3M16 3.5v3"/></Ic>,
    lock:   (p) => <Ic {...p}><rect x="5" y="11" width="14" height="9" rx="2"/><path d="M8 11V8a4 4 0 018 0v3"/></Ic>,
    eyeOff: (p) => <Ic {...p}><path d="M4 4l16 16M9.5 9.6a3 3 0 004.2 4.2M6.5 6.7C4.6 8 3.2 9.9 2.5 12c1.5 4 5.2 6.5 9.5 6.5 1.7 0 3.3-.4 4.7-1.1M10 5.6A9.7 9.7 0 0112 5.5c4.3 0 8 2.5 9.5 6.5-.5 1.3-1.3 2.5-2.3 3.5"/></Ic>,
  };
  window.FFIcon = Icon;

  // ─────────────────────────────────────────────────────────────
  // Screen — device-sized root, applies theme + font scale
  // ─────────────────────────────────────────────────────────────
  function Screen({ theme = 'amoled', fontScale = 1, children, statusTime = '18:31', showStatus = false, gesture = true, style }) {
    const vars = window.FF.THEMES[theme].vars;
    return (
      <div style={Object.assign({}, vars, {
        '--ff-fs': fontScale,
        width: DEVICE.w, height: DEVICE.h,
        background: 'var(--ff-bg)', color: 'var(--ff-text)',
        fontFamily: SANS, position: 'relative', overflow: 'hidden',
        WebkitFontSmoothing: 'antialiased',
      }, style)}>
        {showStatus && <StatusStrip time={statusTime} />}
        {children}
        {gesture && (
          <div style={{ position: 'absolute', left: 0, right: 0, bottom: 8, display: 'flex', justifyContent: 'center', pointerEvents: 'none' }}>
            <div style={{ width: 120, height: 4, borderRadius: 2, background: 'var(--ff-text)', opacity: 0.4 }} />
          </div>
        )}
      </div>
    );
  }
  window.FFScreen = Screen;

  // minimal system status strip (kept very quiet)
  function StatusStrip({ time }) {
    return (
      <div style={{ height: SPACE.statusH, padding: '0 22px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: 13, color: 'var(--ff-text2)', fontWeight: 500 }}>
        <span style={{ fontVariantNumeric: 'tabular-nums' }}>{time}</span>
        <span style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
          <span style={{ fontSize: 11 }}>5G</span>
          <span style={{ width: 22, height: 11, border: '1.4px solid var(--ff-text2)', borderRadius: 3, position: 'relative', display: 'inline-block' }}>
            <span style={{ position: 'absolute', left: 1, top: 1, bottom: 1, width: '70%', background: 'var(--ff-text2)', borderRadius: 1 }} />
          </span>
        </span>
      </div>
    );
  }

  // ─────────────────────────────────────────────────────────────
  // Clock + battery arc (home)
  // ─────────────────────────────────────────────────────────────
  function BatteryArc({ pct = 0.62, charging = true }) {
    // faint full track + bright level arc over the top, ~230° sweep
    const R = 54, C = 60, sweep = 230, start = -90 - sweep / 2;
    const pt = (a) => [C + R * Math.cos((a * Math.PI) / 180), C + R * Math.sin((a * Math.PI) / 180)];
    const arc = (a0, a1) => {
      const [x0, y0] = pt(a0), [x1, y1] = pt(a1);
      const large = a1 - a0 > 180 ? 1 : 0;
      return `M ${x0.toFixed(1)} ${y0.toFixed(1)} A ${R} ${R} 0 ${large} 1 ${x1.toFixed(1)} ${y1.toFixed(1)}`;
    };
    return (
      <svg width="120" height="64" viewBox="0 8 120 56" style={{ display: 'block' }}>
        <path d={arc(start, start + sweep)} fill="none" stroke="var(--ff-line)" strokeWidth="2.4" strokeLinecap="round" />
        <path d={arc(start, start + sweep * pct)} fill="none" stroke="var(--ff-text)" strokeWidth="2.4" strokeLinecap="round" />
      </svg>
    );
  }
  window.FFBatteryArc = BatteryArc;

  function Clock({ time = '18:31', date = 'Tuesday, 9 June', pct = 0.62 }) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <div style={{ marginBottom: 4 }}><BatteryArc pct={pct} /></div>
        <div style={ty('clock', { fontVariantNumeric: 'tabular-nums', color: 'var(--ff-text)' })}>{time}</div>
        <div style={ty('date', { color: 'var(--ff-text2)', marginTop: 8, whiteSpace: 'nowrap' })}>{date}</div>
      </div>
    );
  }
  window.FFClock = Clock;

  // ─────────────────────────────────────────────────────────────
  // Status dot — neutral hollow (paused), filled error (failed)
  // ─────────────────────────────────────────────────────────────
  function Dot({ kind = 'paused', size = 8 }) {
    const s = { width: size, height: size, borderRadius: size, flex: '0 0 auto', boxSizing: 'border-box' };
    if (kind === 'failed') return <span style={{ ...s, background: 'var(--ff-err)' }} />;
    if (kind === 'ready') return <span style={{ ...s, background: 'var(--ff-text)', opacity: 0.85 }} />;
    return <span style={{ ...s, border: '1.5px solid var(--ff-dot)' }} />; // paused (hollow)
  }
  window.FFDot = Dot;

  // App icon placeholder (optional state) — quiet rounded square monogram
  function AppMark({ name, on }) {
    if (!on) return null;
    return (
      <span style={{ width: 30, height: 30, borderRadius: 8, flex: '0 0 auto', background: 'var(--ff-surface2)',
        display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 14, fontWeight: 500, color: 'var(--ff-text2)' }}>
        {name[0]}
      </span>
    );
  }

  // ─────────────────────────────────────────────────────────────
  // AppRow — text row. state: normal | paused | protected | hidden
  // ─────────────────────────────────────────────────────────────
  function AppRow({ name, state = 'normal', meta, icons = false, height = SPACE.rowH, trailing, pressed = false }) {
    const dim = state === 'protected' || state === 'hidden';
    const nameColor = state === 'hidden' ? 'var(--ff-text3)' : dim ? 'var(--ff-text2)' : 'var(--ff-text)';
    let metaText = meta;
    if (!metaText) {
      if (state === 'paused') metaText = 'paused until 00:00';
      else if (state === 'protected') metaText = 'protected';
      else if (state === 'hidden') metaText = 'hidden';
    }
    return (
      <div style={{ minHeight: `calc(${height}px * var(--ff-fs,1))`, display: 'flex', alignItems: 'center', gap: 14,
        padding: `0 ${SPACE.screenPad}px`, background: pressed ? 'var(--ff-press)' : 'transparent' }}>
        <AppMark name={name} on={icons} />
        <span style={{ display: 'flex', flexDirection: 'column', minWidth: 0, flex: 1 }}>
          <span style={ty('appRow', { color: nameColor, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' })}>{name}</span>
          {metaText && (
            <span style={ty('caption', { color: state === 'paused' ? 'var(--ff-text3)' : 'var(--ff-text3)', marginTop: 3, display: 'flex', alignItems: 'center', gap: 7 })}>
              {state === 'paused' && <Dot kind="paused" size={7} />}
              {metaText}
            </span>
          )}
        </span>
        {trailing}
      </div>
    );
  }
  window.FFAppRow = AppRow;

  // ─────────────────────────────────────────────────────────────
  // PauseBlock — home pause action / paused / disabled-by-setup
  // ─────────────────────────────────────────────────────────────
  function ActionLine({ children, color = 'var(--ff-text)', sub, onTop = true }) {
    return (
      <div style={{ borderTop: onTop ? '1px solid var(--ff-line)' : 'none', padding: `18px ${SPACE.screenPad}px` }}>
        <div style={ty('appRow', { color })}>{children}</div>
        {sub && <div style={ty('caption', { color: 'var(--ff-text2)', marginTop: 5 })}>{sub}</div>}
      </div>
    );
  }
  window.FFActionLine = ActionLine;

  // ─────────────────────────────────────────────────────────────
  // StatusBanner — PERSISTENT. setup | failed | info
  // ─────────────────────────────────────────────────────────────
  function StatusBanner({ kind = 'setup', title, sub, action, secondary }) {
    const isErr = kind === 'failed';
    return (
      <div style={{
        margin: `0 ${SPACE.screenPad}px`, padding: '14px 16px', borderRadius: SPACE.radiusBanner,
        background: isErr ? 'var(--ff-err-dim)' : 'var(--ff-surface)',
        border: `1px solid ${isErr ? 'transparent' : 'var(--ff-line)'}`,
        display: 'flex', alignItems: 'center', gap: 13,
      }}>
        <Dot kind={isErr ? 'failed' : 'paused'} size={9} />
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={ty('body', { color: isErr ? 'var(--ff-err)' : 'var(--ff-text)', fontWeight: 500 })}>{title}</div>
          {sub && <div style={ty('caption', { color: 'var(--ff-text2)', marginTop: 3 })}>{sub}</div>}
        </div>
        {action && (
          <span style={ty('button', { color: isErr ? 'var(--ff-err)' : 'var(--ff-text)', whiteSpace: 'nowrap' })}>{action}</span>
        )}
      </div>
    );
  }
  window.FFStatusBanner = StatusBanner;

  // ─────────────────────────────────────────────────────────────
  // SearchField — underlined, magnifier
  // ─────────────────────────────────────────────────────────────
  function SearchField({ value, placeholder = 'Search apps', icon = true, autofocus = false }) {
    const empty = !value;
    return (
      <div style={{ padding: `0 ${SPACE.screenPad}px` }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, borderBottom: '1px solid var(--ff-line)', paddingBottom: 10 }}>
          <span style={ty('appRow', { color: empty ? 'var(--ff-text3)' : 'var(--ff-text)', flex: 1 })}>
            {empty ? placeholder : value}
            {autofocus && <span style={{ display: 'inline-block', width: 1.5, height: '0.95em', background: 'var(--ff-text)', marginLeft: 2, transform: 'translateY(2px)' }} />}
          </span>
          {icon && <Icon.search size={22} style={{ color: 'var(--ff-text2)' }} />}
        </div>
      </div>
    );
  }
  window.FFSearchField = SearchField;

  // ─────────────────────────────────────────────────────────────
  // Scrubber — A-Z fast index (right rail)
  // ─────────────────────────────────────────────────────────────
  function Scrubber({ items, active }) {
    const list = items || ['2', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'K', 'M', 'N', 'O', 'P', 'R', 'S', 'T', 'V', 'W', 'Y', 'Z'];
    return (
      <div style={{ position: 'absolute', right: 7, top: '50%', transform: 'translateY(-50%)', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 1 }}>
        {list.map((c, i) => (
          <span key={i} style={{ fontSize: `calc(11px * var(--ff-fs,1))`, lineHeight: 1.45, fontWeight: c === active ? 600 : 400,
            color: c === active ? 'var(--ff-text)' : 'var(--ff-scrubber)' }}>{c}</span>
        ))}
      </div>
    );
  }
  window.FFScrubber = Scrubber;

  // gear, bottom-right
  function GearButton(props) {
    return (
      <div style={{ position: 'absolute', right: 18, bottom: 34, color: 'var(--ff-text2)', ...props.style }}>
        <Icon.gear size={24} />
      </div>
    );
  }
  window.FFGearButton = GearButton;

  // ─────────────────────────────────────────────────────────────
  // Buttons / text actions
  // ─────────────────────────────────────────────────────────────
  function Btn({ children, kind = 'primary', full = false, onTop = false, disabled = false }) {
    const color = disabled ? 'var(--ff-text3)' : kind === 'destructive' ? 'var(--ff-err)' : kind === 'secondary' ? 'var(--ff-text2)' : 'var(--ff-text)';
    if (kind === 'filled') {
      return (
        <div style={{ width: full ? '100%' : 'auto', padding: '15px 22px', borderRadius: 14, textAlign: 'center',
          background: disabled ? 'var(--ff-surface2)' : 'var(--ff-text)',
          color: disabled ? 'var(--ff-text3)' : 'var(--ff-bg)', ...ty('button'), boxSizing: 'border-box' }}>{children}</div>
      );
    }
    return (
      <div style={{ width: full ? '100%' : 'auto', padding: '16px 0', textAlign: full ? 'center' : 'left',
        borderTop: onTop ? '1px solid var(--ff-line)' : 'none', color, ...ty('button') }}>{children}</div>
    );
  }
  window.FFBtn = Btn;

  // ─────────────────────────────────────────────────────────────
  // Backdrop + Sheet (bottom sheet)
  // ─────────────────────────────────────────────────────────────
  function Backdrop({ children, dim = 0.55 }) {
    return (
      <div style={{ position: 'absolute', inset: 0, background: `rgba(0,0,0,${dim})`, display: 'flex', flexDirection: 'column', justifyContent: 'flex-end' }}>
        {children}
      </div>
    );
  }
  window.FFBackdrop = Backdrop;

  function Sheet({ title, sub, children, footer }) {
    return (
      <div style={{ background: 'var(--ff-surface)', borderTopLeftRadius: SPACE.radiusSheet, borderTopRightRadius: SPACE.radiusSheet,
        borderTop: '1px solid var(--ff-line)', paddingBottom: 26, boxShadow: '0 -8px 40px rgba(0,0,0,0.5)' }}>
        <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 10, paddingBottom: 6 }}>
          <div style={{ width: 38, height: 4, borderRadius: 2, background: 'var(--ff-text3)' }} />
        </div>
        {title && (
          <div style={{ padding: `8px ${SPACE.sheetPad}px 6px` }}>
            <div style={ty('sheetTitle', { color: 'var(--ff-text)' })}>{title}</div>
            {sub && <div style={ty('caption', { color: 'var(--ff-text2)', marginTop: 4 })}>{sub}</div>}
          </div>
        )}
        <div>{children}</div>
        {footer}
      </div>
    );
  }
  window.FFSheet = Sheet;

  function SheetRow({ label, kind = 'normal', disabled = false, dot, sub, pressed = false }) {
    const color = disabled ? 'var(--ff-text3)' : kind === 'destructive' ? 'var(--ff-err)' : kind === 'muted' ? 'var(--ff-text2)' : 'var(--ff-text)';
    return (
      <div style={{ minHeight: `calc(${SPACE.sheetRowH}px * var(--ff-fs,1))`, display: 'flex', alignItems: 'center', gap: 12,
        padding: `0 ${SPACE.sheetPad}px`, background: pressed ? 'var(--ff-press)' : 'transparent' }}>
        {dot && <Dot kind={dot} size={8} />}
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={ty('appRow', { fontSize: `calc(17px * var(--ff-fs,1))`, color })}>{label}</div>
          {sub && <div style={ty('caption', { color: 'var(--ff-text2)', marginTop: 2 })}>{sub}</div>}
        </div>
      </div>
    );
  }
  window.FFSheetRow = SheetRow;

  // ─────────────────────────────────────────────────────────────
  // Dialog (centered)
  // ─────────────────────────────────────────────────────────────
  function Dialog({ title, body, input, children, actions }) {
    return (
      <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 28 }}>
        <div style={{ width: '100%', background: 'var(--ff-surface)', borderRadius: SPACE.radiusDialog, border: '1px solid var(--ff-line)',
          padding: '26px 24px 16px', boxShadow: '0 20px 60px rgba(0,0,0,0.6)' }}>
          {title && <div style={ty('sheetTitle', { color: 'var(--ff-text)', fontSize: `calc(20px * var(--ff-fs,1))` })}>{title}</div>}
          {body && <div style={ty('body', { color: 'var(--ff-text2)', marginTop: 10 })}>{body}</div>}
          {input != null && (
            <div style={{ marginTop: 18, borderBottom: '1.5px solid var(--ff-text)', paddingBottom: 8 }}>
              <span style={ty('appRow', { color: 'var(--ff-text)', fontSize: `calc(19px * var(--ff-fs,1))` })}>{input}</span>
              <span style={{ display: 'inline-block', width: 1.5, height: '0.95em', background: 'var(--ff-text)', marginLeft: 2, transform: 'translateY(2px)' }} />
            </div>
          )}
          {children}
          {actions && (
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 22 }}>{actions}</div>
          )}
        </div>
      </div>
    );
  }
  window.FFDialog = Dialog;

  function DialogBtn({ children, kind = 'normal' }) {
    const color = kind === 'destructive' ? 'var(--ff-err)' : kind === 'muted' ? 'var(--ff-text2)' : 'var(--ff-text)';
    return <span style={ty('button', { color, padding: '10px 14px' })}>{children}</span>;
  }
  window.FFDialogBtn = DialogBtn;

  // ─────────────────────────────────────────────────────────────
  // Settings / list rows + TopBar + section headers
  // ─────────────────────────────────────────────────────────────
  function TopBar({ title, back = true, trailing }) {
    return (
      <div style={{ height: 56, display: 'flex', alignItems: 'center', padding: '0 18px', gap: 14 }}>
        {back && <span style={{ color: 'var(--ff-text)' }}><Icon.back size={24} /></span>}
        <div style={ty('body', { flex: 1, fontWeight: 500, color: 'var(--ff-text)', fontSize: `calc(18px * var(--ff-fs,1))` })}>{title}</div>
        {trailing}
      </div>
    );
  }
  window.FFTopBar = TopBar;

  function SettingsRow({ label, value, expand = false, chevron = false, sub }) {
    return (
      <div style={{ minHeight: `calc(56px * var(--ff-fs,1))`, display: 'flex', alignItems: 'center', gap: 12, padding: `0 ${SPACE.screenPad}px` }}>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={ty('body', { color: 'var(--ff-text)', fontSize: `calc(17px * var(--ff-fs,1))` })}>{label}</div>
          {sub && <div style={ty('caption', { color: 'var(--ff-text2)', marginTop: 2 })}>{sub}</div>}
        </div>
        {value && <span style={ty('caption', { color: 'var(--ff-text2)', fontSize: `calc(15px * var(--ff-fs,1))` })}>{value}</span>}
        {expand && <span style={{ color: 'var(--ff-text2)' }}><Icon.chevDown size={20} /></span>}
        {chevron && <span style={{ color: 'var(--ff-text3)' }}><Icon.chevRight size={20} /></span>}
      </div>
    );
  }
  window.FFSettingsRow = SettingsRow;

  function SectionLabel({ children }) {
    return <div style={ty('section', { color: 'var(--ff-text2)', padding: `0 ${SPACE.screenPad}px`, marginBottom: 6 })}>{children}</div>;
  }
  window.FFSectionLabel = SectionLabel;

  // selectable row (onboarding) — checkbox or radio
  function SelectRow({ name, selected = false, disabled = false, meta, shape = 'check' }) {
    const color = disabled ? 'var(--ff-text3)' : 'var(--ff-text)';
    return (
      <div style={{ minHeight: `calc(${SPACE.rowH}px * var(--ff-fs,1))`, display: 'flex', alignItems: 'center', gap: 16, padding: `0 ${SPACE.screenPad}px` }}>
        <span style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column' }}>
          <span style={ty('appRow', { color })}>{name}</span>
          {meta && <span style={ty('caption', { color: 'var(--ff-text3)', marginTop: 2 })}>{meta}</span>}
        </span>
        <Box selected={selected} disabled={disabled} shape={shape} />
      </div>
    );
  }
  window.FFSelectRow = SelectRow;

  function Box({ selected, disabled, shape = 'check' }) {
    const r = shape === 'radio' ? 11 : 6;
    if (selected) {
      return (
        <span style={{ width: 22, height: 22, borderRadius: r, background: 'var(--ff-text)', color: 'var(--ff-bg)', display: 'flex', alignItems: 'center', justifyContent: 'center', flex: '0 0 auto' }}>
          <Icon.check size={15} stroke={2.4} />
        </span>
      );
    }
    return <span style={{ width: 22, height: 22, borderRadius: r, border: `1.6px solid ${disabled ? 'var(--ff-text3)' : 'var(--ff-text2)'}`, flex: '0 0 auto' }} />;
  }

  // Snackbar / toast
  function Snackbar({ children, action }) {
    return (
      <div style={{ position: 'absolute', left: 16, right: 16, bottom: 26, background: 'var(--ff-surface2)', borderRadius: 14,
        border: '1px solid var(--ff-line)', padding: '14px 16px', display: 'flex', alignItems: 'center', gap: 14, boxShadow: '0 8px 30px rgba(0,0,0,0.5)' }}>
        <span style={ty('body', { flex: 1, color: 'var(--ff-text)', fontSize: `calc(15px * var(--ff-fs,1))` })}>{children}</span>
        {action && <span style={ty('button', { color: 'var(--ff-text)', fontSize: `calc(15px * var(--ff-fs,1))` })}>{action}</span>}
      </div>
    );
  }
  window.FFSnackbar = Snackbar;

  // Expandable technical block (quiet) — used in error details + debug
  function CodeBlock({ rows }) {
    return (
      <div style={{ background: 'var(--ff-surface2)', borderRadius: 12, padding: '12px 14px', display: 'flex', flexDirection: 'column', gap: 8 }}>
        {rows.map((r, i) => (
          <div key={i}>
            <div style={ty('section', { color: 'var(--ff-text3)', marginBottom: 3 })}>{r.k}</div>
            <div style={ty('mono', { color: r.err ? 'var(--ff-err)' : 'var(--ff-text2)', fontFamily: "'Roboto Mono', ui-monospace, monospace", whiteSpace: 'pre-wrap', wordBreak: 'break-word' })}>{r.v}</div>
          </div>
        ))}
      </div>
    );
  }
  window.FFCodeBlock = CodeBlock;

  // small chevron-row used as expandable disclosure ("View details")
  function Disclosure({ label, open = false }) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--ff-text2)' }}>
        <span style={{ transform: open ? 'rotate(90deg)' : 'none', display: 'flex' }}><Icon.chevRight size={16} /></span>
        <span style={ty('caption', { color: 'var(--ff-text2)' })}>{label}</span>
      </div>
    );
  }
  window.FFDisclosure = Disclosure;

})();
