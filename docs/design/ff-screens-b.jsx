/* FocusFloat screens — Onboarding, Settings, Dialogs, Snackbar */
(function () {
  const { SPACE } = window.FF;
  const ty = window.FFty;
  const { FFScreen: Screen, FFIcon: Icon, FFBtn: Btn, FFDot: Dot, FFTopBar: TopBar,
    FFSettingsRow: SettingsRow, FFSectionLabel: SectionLabel, FFSelectRow: SelectRow,
    FFStatusBanner: StatusBanner, FFDialog: Dialog, FFDialogBtn: DialogBtn,
    FFSnackbar: Snackbar, FFCodeBlock: CodeBlock, FFAppRow: AppRow } = window;

  // ── Onboarding shell ─────────────────────────────────────────
  function OnbShell({ theme, fontScale, step, total = 7, title, body, children, primary, secondary, scroll = false }) {
    return (
      <Screen theme={theme} fontScale={fontScale}>
        <div style={{ height: '100%', display: 'flex', flexDirection: 'column', padding: `64px ${SPACE.screenPad}px 30px` }}>
          {step != null && (
            <div style={{ display: 'flex', gap: 6, marginBottom: 40 }}>
              {Array.from({ length: total }).map((_, i) => (
                <span key={i} style={{ height: 3, flex: 1, borderRadius: 2, background: i <= step ? 'var(--ff-text)' : 'var(--ff-line)' }} />
              ))}
            </div>
          )}
          <div style={ty('clock', { fontSize: `calc(30px * var(--ff-fs,1))`, fontWeight: 400, color: 'var(--ff-text)', lineHeight: 1.15 })}>{title}</div>
          {body && <div style={ty('body', { color: 'var(--ff-text2)', marginTop: 16, maxWidth: 320 })}>{body}</div>}
          <div style={{ flex: 1, overflow: 'hidden', marginTop: children ? 28 : 0, marginLeft: -SPACE.screenPad, marginRight: -SPACE.screenPad }}>{children}</div>
          <div style={{ marginTop: 20 }}>
            {primary}
            {secondary && <div style={{ textAlign: 'center', marginTop: 6 }}><Btn kind="secondary">{secondary}</Btn></div>}
          </div>
        </div>
      </Screen>
    );
  }

  // 12 · Welcome
  window.OnbWelcome = ({ theme, fontScale }) => (
    <OnbShell theme={theme} fontScale={fontScale} step={0}
      title="FocusFloat"
      body="A quiet launcher that lets you pause distracting apps until midnight. Everything stays on your phone — no account, no server, no analytics."
      primary={<Btn kind="filled" full>Continue</Btn>} />
  );

  // 13 · Set as home screen
  window.OnbHome = ({ theme, fontScale }) => (
    <OnbShell theme={theme} fontScale={fontScale} step={1}
      title="Set as home screen"
      body="Make FocusFloat your default launcher so it opens every time you press home."
      primary={<Btn kind="filled" full>Set as home screen</Btn>} secondary="Skip for now" />
  );

  // 14 · Pick favorites
  window.OnbFavorites = ({ theme, fontScale }) => (
    <OnbShell theme={theme} fontScale={fontScale} step={2}
      title="Pick favorites"
      body="Choose the few apps you want on your home screen."
      primary={<Btn kind="filled" full>Continue</Btn>}>
      {['Phone', 'Messages', 'Calendar', 'Maps', 'Notes', 'Camera', 'Clock', 'Chrome'].map((n, i) => (
        <SelectRow key={n} name={n} selected={i < 5} />
      ))}
    </OnbShell>
  );

  // 15 · Create pause category
  window.OnbCategory = ({ theme, fontScale }) => (
    <OnbShell theme={theme} fontScale={fontScale} step={3}
      title="Create a pause category"
      body="Group the apps you’d like to pause together. You can rename it anytime."
      primary={<Btn kind="filled" full>Create category</Btn>}>
      <div style={{ padding: `0 ${SPACE.screenPad}px` }}>
        <SectionLabel>Category name</SectionLabel>
        <div style={{ borderBottom: '1.5px solid var(--ff-text)', paddingBottom: 8, display: 'flex', alignItems: 'center' }}>
          <span style={ty('appRow', { color: 'var(--ff-text)', fontSize: `calc(24px * var(--ff-fs,1))` })}>Dopamine</span>
          <span style={{ display: 'inline-block', width: 1.5, height: '1em', background: 'var(--ff-text)', marginLeft: 2 }} />
        </div>
      </div>
    </OnbShell>
  );

  // 16 · Add apps to Dopamine
  window.OnbAddApps = ({ theme, fontScale }) => (
    <OnbShell theme={theme} fontScale={fontScale} step={4}
      title="Add apps to Dopamine"
      body="Protected system apps can’t be paused."
      primary={<Btn kind="filled" full>Continue</Btn>}>
      {[
        { n: 'YouTube', s: true }, { n: 'Instagram', s: true }, { n: 'Reddit', s: true },
        { n: 'TikTok', s: true }, { n: 'X' }, { n: 'Chrome' },
        { n: 'Phone', disabled: true, meta: 'Protected' }, { n: 'Messages', disabled: true, meta: 'Protected' },
      ].map((a) => <SelectRow key={a.n} name={a.n} selected={a.s} disabled={a.disabled} meta={a.meta} />)}
    </OnbShell>
  );

  // 17 · Shizuku setup (status-driven)
  const SHIZUKU = {
    notInstalled: { kind: 'failed', label: 'Shizuku not installed', body: 'FocusFloat uses Shizuku to pause apps without root. Install it from the Play Store to continue.', primary: 'Get Shizuku', secondary: null },
    notRunning: { kind: 'paused', label: 'Shizuku installed, not running', body: 'Start Shizuku via Wireless Debugging. After every reboot, open Shizuku and start it again.', primary: 'Open Shizuku', secondary: null },
    permission: { kind: 'paused', label: 'Permission required', body: 'Shizuku is running. Grant FocusFloat permission to pause apps.', primary: 'Request permission', secondary: null },
    ready: { kind: 'ready', label: 'Shizuku ready', body: 'You’re all set. FocusFloat can pause apps on demand.', primary: 'Continue', secondary: null },
  };
  window.OnbShizuku = ({ theme, fontScale, status = 'notRunning' }) => {
    const s = SHIZUKU[status];
    return (
      <OnbShell theme={theme} fontScale={fontScale} step={5}
        title="Connect Shizuku"
        body={s.body}
        primary={<Btn kind="filled" full>{s.primary}</Btn>}
        secondary={status === 'ready' ? null : 'How does this work?'}>
        <div style={{ padding: `0 ${SPACE.screenPad}px` }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 13, padding: '16px 16px', borderRadius: SPACE.radiusBanner,
            background: s.kind === 'failed' ? 'var(--ff-err-dim)' : 'var(--ff-surface)', border: s.kind === 'failed' ? 'none' : '1px solid var(--ff-line)' }}>
            <Dot kind={s.kind} size={9} />
            <span style={ty('body', { fontWeight: 500, color: s.kind === 'failed' ? 'var(--ff-err)' : 'var(--ff-text)' })}>{s.label}</span>
          </div>
        </div>
      </OnbShell>
    );
  };

  // 18 · Done
  window.OnbDone = ({ theme, fontScale }) => (
    <Screen theme={theme} fontScale={fontScale}>
      <div style={{ height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: `0 ${SPACE.screenPad}px` }}>
        <div style={{ width: 56, height: 56, borderRadius: 28, border: '1.6px solid var(--ff-text)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--ff-text)' }}>
          <Icon.check size={28} stroke={1.8} />
        </div>
        <div style={ty('clock', { fontSize: `calc(28px * var(--ff-fs,1))`, fontWeight: 400, marginTop: 26 })}>You’re set</div>
        <div style={ty('body', { color: 'var(--ff-text2)', marginTop: 12, textAlign: 'center', maxWidth: 280 })}>Dopamine is ready to pause whenever you need a break.</div>
      </div>
      <div style={{ position: 'absolute', left: SPACE.screenPad, right: SPACE.screenPad, bottom: 30 }}>
        <Btn kind="filled" full>Go to FocusFloat</Btn>
      </div>
    </Screen>
  );

  // ── Settings ─────────────────────────────────────────────────
  // 19 · Settings index
  window.SettingsIndex = ({ theme, fontScale }) => (
    <Screen theme={theme} fontScale={fontScale}>
      <TopBar title="Settings" />
      <div style={{ paddingTop: 6 }}>
        <SettingsRow label="Home screen" chevron />
        <SettingsRow label="Favorites" chevron sub="5 apps" />
        <SettingsRow label="Hidden apps" chevron sub="3 apps" />
        <SettingsRow label="Pause categories" chevron sub="Dopamine · Work Distractions" />
        <SettingsRow label="Appearance" chevron sub="AMOLED · Medium" />
        <SettingsRow label="Shizuku" chevron sub="Ready" />
        <SettingsRow label="Debug" chevron />
      </div>
    </Screen>
  );

  // 20 · Appearance
  function Toggle({ on }) {
    return (
      <span style={{ width: 44, height: 26, borderRadius: 13, background: on ? 'var(--ff-text)' : 'var(--ff-surface2)', position: 'relative', flex: '0 0 auto' }}>
        <span style={{ position: 'absolute', top: 3, left: on ? 21 : 3, width: 20, height: 20, borderRadius: 10, background: on ? 'var(--ff-bg)' : 'var(--ff-text2)', transition: 'left .15s' }} />
      </span>
    );
  }
  window.SettingsAppearance = ({ theme, fontScale }) => (
    <Screen theme={theme} fontScale={fontScale}>
      <TopBar title="Appearance" />
      <div style={{ paddingTop: 10 }}>
        <SectionLabel>Theme</SectionLabel>
        {['System', 'Light', 'Dark', 'AMOLED Black'].map((t) => <SelectRow key={t} name={t} selected={t === 'AMOLED Black'} shape="radio" />)}
        <div style={{ height: 14 }} />
        <SectionLabel>Text size</SectionLabel>
        {['Small', 'Medium', 'Large', 'Extra large'].map((t) => <SelectRow key={t} name={t} selected={t === 'Medium'} shape="radio" />)}
        <div style={{ height: 14 }} />
        <SectionLabel>Home screen</SectionLabel>
        <div style={{ minHeight: 56, display: 'flex', alignItems: 'center', padding: `0 ${SPACE.screenPad}px` }}>
          <span style={ty('body', { flex: 1, color: 'var(--ff-text)', fontSize: `calc(17px * var(--ff-fs,1))` })}>Show clock</span>
          <Toggle on />
        </div>
        <div style={{ minHeight: 56, display: 'flex', alignItems: 'center', padding: `0 ${SPACE.screenPad}px` }}>
          <span style={ty('body', { flex: 1, color: 'var(--ff-text)', fontSize: `calc(17px * var(--ff-fs,1))` })}>Show date</span>
          <Toggle on />
        </div>
      </div>
    </Screen>
  );

  // 21 · Hidden apps
  window.SettingsHidden = ({ theme, fontScale }) => (
    <Screen theme={theme} fontScale={fontScale}>
      <TopBar title="Hidden apps" />
      <div style={{ padding: `4px ${SPACE.screenPad}px 14px` }}>
        <span style={ty('caption', { color: 'var(--ff-text2)' })}>Hidden apps don’t appear in the app list or search.</span>
      </div>
      {['System UI', 'Files', 'Carrier Services'].map((n) => (
        <div key={n} style={{ minHeight: 56, display: 'flex', alignItems: 'center', padding: `0 ${SPACE.screenPad}px` }}>
          <span style={{ flex: 1, ...ty('appRow', { color: 'var(--ff-text2)' }) }}>{n}</span>
          <span style={ty('button', { color: 'var(--ff-text)' })}>Restore</span>
        </div>
      ))}
    </Screen>
  );

  // 22 · Shizuku status
  window.SettingsShizuku = ({ theme, fontScale }) => (
    <Screen theme={theme} fontScale={fontScale}>
      <TopBar title="Shizuku" />
      <div style={{ padding: `12px ${SPACE.screenPad}px 0` }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 13, padding: '16px', borderRadius: SPACE.radiusBanner, background: 'var(--ff-surface)', border: '1px solid var(--ff-line)' }}>
          <Dot kind="ready" size={9} />
          <span style={ty('body', { fontWeight: 500 })}>Shizuku ready</span>
        </div>
      </div>
      <div style={{ paddingTop: 14 }}>
        <SettingsRow label="Installed" value="Yes" />
        <SettingsRow label="Running" value="Yes" />
        <SettingsRow label="Permission" value="Granted" />
        <SettingsRow label="API version" value="13" />
      </div>
      <div style={{ padding: `14px ${SPACE.screenPad}px` }}>
        <span style={ty('caption', { color: 'var(--ff-text2)' })}>After a reboot, open Shizuku and start it again via Wireless Debugging.</span>
      </div>
      <div style={{ padding: `0 ${SPACE.screenPad}px` }}><Btn onTop kind="primary">Open Shizuku</Btn></div>
    </Screen>
  );

  // 23 · Debug (quiet utilitarian)
  window.SettingsDebug = ({ theme, fontScale }) => (
    <Screen theme={theme} fontScale={fontScale}>
      <TopBar title="Debug" />
      <div style={{ paddingTop: 6 }}>
        <SettingsRow label="Shizuku status" value="Ready" />
        <SettingsRow label="Exact alarm permission" value="Granted" />
        <SettingsRow label="Active sessions" value="1" />
      </div>
      <div style={{ padding: `16px ${SPACE.screenPad}px 0` }}>
        <SectionLabel>Last command</SectionLabel>
        <div style={{ height: 8 }} />
        <CodeBlock rows={[
          { k: 'Command', v: 'pm disable-user --user 0 com.google.android.youtube' },
          { k: 'Exit code', v: '0' },
          { k: 'Stdout', v: 'Package com.google.android.youtube new state: disabled-user' },
          { k: 'Stderr', v: '—' },
        ]} />
      </div>
    </Screen>
  );

  // ── Dialogs ──────────────────────────────────────────────────
  const dialogBg = (theme, fontScale, label) => (
    <Screen theme={theme} fontScale={fontScale} gesture={false}>
      <div style={{ opacity: 0.4 }}><TopBar title={label || 'Dopamine'} /></div>
    </Screen>
  );

  window.DialogRename = ({ theme, fontScale }) => (
    <Screen theme={theme} fontScale={fontScale} gesture={false}>
      <div style={{ paddingTop: 22, opacity: 0.4 }}><AppRow name="YouTube" /><AppRow name="YouTube Music" /></div>
      <Dialog title="Rename app" input="YouTube"
        actions={<React.Fragment><DialogBtn kind="muted">Cancel</DialogBtn><DialogBtn>Save</DialogBtn></React.Fragment>} />
    </Screen>
  );

  window.DialogRenameCat = ({ theme, fontScale }) => (
    <Screen theme={theme} fontScale={fontScale} gesture={false}>
      <div style={{ opacity: 0.4 }}><TopBar title="Dopamine" /></div>
      <Dialog title="Rename category" input="Dopamine"
        actions={<React.Fragment><DialogBtn kind="muted">Cancel</DialogBtn><DialogBtn>Save</DialogBtn></React.Fragment>} />
    </Screen>
  );

  window.DialogCreateCat = ({ theme, fontScale }) => (
    <Screen theme={theme} fontScale={fontScale} gesture={false}>
      <div style={{ opacity: 0.4 }}><TopBar title="Pause categories" /></div>
      <Dialog title="New category" input="Untitled"
        actions={<React.Fragment><DialogBtn kind="muted">Cancel</DialogBtn><DialogBtn>Create</DialogBtn></React.Fragment>} />
    </Screen>
  );

  window.DialogConfirmUnpause = ({ theme, fontScale }) => (
    <Screen theme={theme} fontScale={fontScale} gesture={false}>
      <div style={{ opacity: 0.4 }}><TopBar title="Dopamine" /></div>
      <Dialog title="Unpause Dopamine?" body="7 apps will be available again before midnight."
        actions={<React.Fragment><DialogBtn kind="muted">Keep paused</DialogBtn><DialogBtn>Unpause</DialogBtn></React.Fragment>} />
    </Screen>
  );

  window.DialogPermission = ({ theme, fontScale }) => (
    <Screen theme={theme} fontScale={fontScale} gesture={false}>
      <div style={{ opacity: 0.4 }}><TopBar title="Dopamine" /></div>
      <Dialog title="Allow exact alarms" body="FocusFloat needs the exact alarm permission to automatically unpause your apps at midnight."
        actions={<React.Fragment><DialogBtn kind="muted">Not now</DialogBtn><DialogBtn>Open settings</DialogBtn></React.Fragment>} />
    </Screen>
  );

  // Snackbar / toast demo (over home)
  window.SnackbarDemo = ({ theme, fontScale }) => (
    <Screen theme={theme} fontScale={fontScale}>
      <div style={{ paddingTop: 22, opacity: 0.55 }}><AppRow name="YouTube" state="paused" /><AppRow name="Instagram" state="paused" /></div>
      <Snackbar action="Undo">Dopamine paused until 00:00</Snackbar>
    </Screen>
  );

})();
