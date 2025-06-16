/* FocusFloat screens — Home, All Apps, Search, Bottom Sheets, Category, Errors */
(function () {
  const { SPACE } = window.FF;
  const ty = window.FFty;
  const { FFScreen: Screen, FFClock: Clock, FFAppRow: AppRow, FFActionLine: ActionLine,
    FFStatusBanner: StatusBanner, FFSearchField: SearchField, FFScrubber: Scrubber,
    FFGearButton: GearButton, FFIcon: Icon, FFDot: Dot, FFBackdrop: Backdrop, FFSheet: Sheet,
    FFSheetRow: SheetRow, FFBtn: Btn, FFCodeBlock: CodeBlock } = window;

  const FAVS = ['Phone', 'Messages', 'Calendar', 'Maps', 'Notes'];

  // bottom phone + camera corner glyphs
  function Corners() {
    return (
      <React.Fragment>
        <div style={{ position: 'absolute', left: 26, bottom: 30, color: 'var(--ff-text)' }}><Icon.phone size={26} /></div>
        <div style={{ position: 'absolute', right: 26, bottom: 30, color: 'var(--ff-text)' }}><Icon.camera size={26} /></div>
      </React.Fragment>
    );
  }

  // shared home shell: clock on top, favourites + pauseArea pinned lower
  function HomeShell({ theme, fontScale, pauseArea, searchLine = true }) {
    return (
      <Screen theme={theme} fontScale={fontScale}>
        <div style={{ height: '100%', display: 'flex', flexDirection: 'column', paddingTop: 58 }}>
          <div style={{ flexShrink: 0 }}><Clock /></div>
          <div style={{ flex: 1, minHeight: 20 }} />
          <div style={{ flexShrink: 0, paddingBottom: 4 }}>
            {FAVS.map((n) => <AppRow key={n} name={n} height={SPACE.favRowH} />)}
          </div>
          <div style={{ flexShrink: 0 }}>{pauseArea}</div>
          {searchLine && (
            <div style={{ flexShrink: 0, padding: `16px ${SPACE.screenPad}px 0`, display: 'flex', alignItems: 'center', gap: 12 }}>
              <Icon.search size={20} style={{ color: 'var(--ff-text3)' }} />
              <span style={ty('appRow', { color: 'var(--ff-text3)', whiteSpace: 'nowrap' })}>Search apps</span>
            </div>
          )}
          <div style={{ height: 86, flexShrink: 0 }} />
        </div>
        <Corners />
      </Screen>
    );
  }

  // 1 · Home / Ready
  window.HomeReady = ({ theme, fontScale }) => (
    <HomeShell theme={theme} fontScale={fontScale}
      pauseArea={<ActionLine onTop>Pause Dopamine until midnight</ActionLine>} />
  );

  // 2 · Home / Category Paused
  window.HomePaused = ({ theme, fontScale }) => (
    <HomeShell theme={theme} fontScale={fontScale} pauseArea={
      <div style={{ borderTop: '1px solid var(--ff-line)', padding: `16px ${SPACE.screenPad}px 4px` }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 11 }}>
          <Dot kind="paused" size={8} />
          <span style={ty('body', { color: 'var(--ff-text2)', fontSize: `calc(18px * var(--ff-fs,1))`, whiteSpace: 'nowrap' })}>Dopamine paused until 00:00</span>
        </div>
        <div style={ty('button', { color: 'var(--ff-text)', paddingTop: 14, paddingBottom: 4 })}>Unpause now</div>
      </div>
    } />
  );

  // 3 · Home / Shizuku Setup Required
  window.HomeSetup = ({ theme, fontScale }) => (
    <HomeShell theme={theme} fontScale={fontScale} pauseArea={
      <div style={{ paddingTop: 14 }}>
        <StatusBanner kind="setup" title="Shizuku setup required" sub="Pausing is unavailable until Shizuku is running." action="Open Shizuku" />
      </div>
    } />
  );

  // 4 · Home / Partial Failure
  window.HomePartial = ({ theme, fontScale }) => (
    <HomeShell theme={theme} fontScale={fontScale} pauseArea={
      <div style={{ margin: `14px ${SPACE.screenPad}px 0`, padding: '15px 16px', borderRadius: SPACE.radiusBanner, background: 'var(--ff-err-dim)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <Dot kind="failed" size={9} />
          <div style={{ flex: 1 }}>
            <div style={ty('body', { color: 'var(--ff-err)', fontWeight: 500 })}>Dopamine partially paused</div>
            <div style={ty('caption', { color: 'var(--ff-text2)', marginTop: 3 })}>5 apps paused · 1 failed</div>
          </div>
        </div>
        <div style={{ display: 'flex', gap: 24, paddingLeft: 21, marginTop: 14 }}>
          <span style={ty('button', { color: 'var(--ff-text)', whiteSpace: 'nowrap' })}>View details</span>
          <span style={ty('button', { color: 'var(--ff-text2)', whiteSpace: 'nowrap' })}>Unpause now</span>
        </div>
      </div>
    } />
  );

  // ── All Apps list ────────────────────────────────────────────
  const APP_LIST = [
    { name: 'Amazon Shopping', state: 'paused' }, { name: 'Authenticator' }, { name: 'Books' },
    { name: 'Calculator' }, { name: 'Calendar' }, { name: 'Camera' }, { name: 'Chrome' },
    { name: 'Clock' }, { name: 'Contacts' }, { name: 'Drive' }, { name: 'Files' },
    { name: 'Gmail' }, { name: 'Keep Notes' },
  ];

  window.AllApps = ({ theme, fontScale, icons = false }) => (
    <Screen theme={theme} fontScale={fontScale}>
      <div style={{ paddingTop: 22 }}>
        <SearchField icon />
        <div style={{ height: 10 }} />
        {APP_LIST.map((a) => <AppRow key={a.name} name={a.name} state={a.state} icons={icons} />)}
      </div>
      <Scrubber active="C" />
      <GearButton />
    </Screen>
  );

  // All Apps — row-state showcase (normal / paused / protected / hidden)
  window.AllAppsStates = ({ theme, fontScale }) => (
    <Screen theme={theme} fontScale={fontScale}>
      <div style={{ paddingTop: 22 }}>
        <SearchField icon />
        <div style={{ height: 10 }} />
        <AppRow name="YouTube" />
        <AppRow name="Instagram" state="paused" />
        <AppRow name="Phone" state="protected" />
        <AppRow name="Reddit" state="paused" />
        <AppRow name="System UI" state="hidden" />
        <AppRow name="Telegram" />
      </div>
      <Scrubber active="I" />
      <GearButton />
    </Screen>
  );

  // ── Search ───────────────────────────────────────────────────
  window.SearchResults = ({ theme, fontScale }) => (
    <Screen theme={theme} fontScale={fontScale}>
      <div style={{ paddingTop: 22 }}>
        <SearchField value="youtube" autofocus />
        <div style={{ height: 14 }} />
        <AppRow name="YouTube" state="paused" />
        <AppRow name="YouTube Music" />
      </div>
    </Screen>
  );

  window.SearchEmpty = ({ theme, fontScale }) => (
    <Screen theme={theme} fontScale={fontScale}>
      <div style={{ paddingTop: 22 }}>
        <SearchField value="" autofocus />
        <div style={{ height: 120 }} />
        <div style={{ textAlign: 'center', padding: '0 40px' }}>
          <div style={ty('body', { color: 'var(--ff-text2)' })}>Type to search your apps</div>
        </div>
      </div>
    </Screen>
  );

  window.SearchNoResults = ({ theme, fontScale }) => (
    <Screen theme={theme} fontScale={fontScale}>
      <div style={{ paddingTop: 22 }}>
        <SearchField value="qwxz" autofocus />
        <div style={{ height: 120 }} />
        <div style={{ textAlign: 'center', padding: '0 40px' }}>
          <div style={ty('body', { color: 'var(--ff-text2)' })}>No apps match “qwxz”</div>
        </div>
      </div>
    </Screen>
  );

  // ── Bottom sheets ────────────────────────────────────────────
  // 9 · App action sheet (normal app) — shown over All Apps
  window.SheetAppActions = ({ theme, fontScale }) => (
    <Screen theme={theme} fontScale={fontScale} gesture={false}>
      <div style={{ paddingTop: 22, opacity: 0.5 }}>
        <SearchField icon />
        <div style={{ height: 10 }} />
        {APP_LIST.slice(0, 6).map((a) => <AppRow key={a.name} name={a.name} state={a.state} />)}
      </div>
      <Backdrop>
        <Sheet title="YouTube">
          <SheetRow label="Open" />
          <SheetRow label="Add to favorites" />
          <SheetRow label="Rename" />
          <SheetRow label="Hide from app list" />
          <SheetRow label="Add to pause category" />
          <SheetRow label="App info" kind="muted" />
        </Sheet>
      </Backdrop>
    </Screen>
  );

  // 10 · App action sheet (protected app)
  window.SheetProtected = ({ theme, fontScale }) => (
    <Screen theme={theme} fontScale={fontScale} gesture={false}>
      <div style={{ paddingTop: 22, opacity: 0.5 }}>
        <SearchField icon />
        <div style={{ height: 10 }} />
        {APP_LIST.slice(0, 6).map((a) => <AppRow key={a.name} name={a.name} state={a.state} />)}
      </div>
      <Backdrop>
        <Sheet title="Phone" sub="Protected — can’t be paused or hidden">
          <SheetRow label="Open" />
          <SheetRow label="Add to favorites" />
          <SheetRow label="Rename" />
          <SheetRow label="App info" kind="muted" />
        </Sheet>
      </Backdrop>
    </Screen>
  );

  // 11 · Add to pause category sheet
  window.SheetAddCategory = ({ theme, fontScale }) => (
    <Screen theme={theme} fontScale={fontScale} gesture={false}>
      <div style={{ paddingTop: 22, opacity: 0.5 }}>
        <SearchField icon />
        <div style={{ height: 10 }} />
        {APP_LIST.slice(0, 6).map((a) => <AppRow key={a.name} name={a.name} state={a.state} />)}
      </div>
      <Backdrop>
        <Sheet title="Add to category" sub="YouTube">
          <SheetRow label="Dopamine" dot="ready" sub="7 apps" />
          <SheetRow label="Work Distractions" dot="ready" sub="4 apps" />
          <div style={{ height: 6 }} />
          <div style={{ borderTop: '1px solid var(--ff-line)' }} />
          <SheetRow label="Create new category" />
        </Sheet>
      </Backdrop>
    </Screen>
  );

  // 12 · Paused app sheet (tap on paused app)
  window.SheetPausedApp = ({ theme, fontScale }) => (
    <Screen theme={theme} fontScale={fontScale} gesture={false}>
      <div style={{ paddingTop: 22, opacity: 0.5 }}>
        <SearchField icon />
        <div style={{ height: 10 }} />
        {APP_LIST.slice(0, 6).map((a) => <AppRow key={a.name} name={a.name} state={a.state} />)}
      </div>
      <Backdrop>
        <Sheet>
          <div style={{ padding: `4px ${SPACE.sheetPad}px 10px`, display: 'flex', alignItems: 'center', gap: 11 }}>
            <Dot kind="paused" size={9} />
            <span style={ty('sheetTitle', { color: 'var(--ff-text)' })}>YouTube is paused until 00:00</span>
          </div>
          <SheetRow label="Unpause category" />
          <SheetRow label="App info" kind="muted" />
          <SheetRow label="Cancel" kind="muted" />
        </Sheet>
      </Backdrop>
    </Screen>
  );

  // ── Pause Category detail ────────────────────────────────────
  const CAT_APPS = [
    { name: 'YouTube' }, { name: 'Instagram' }, { name: 'Reddit' },
    { name: 'X' }, { name: 'Telegram' }, { name: 'Chrome' }, { name: 'TikTok' },
  ];

  function CategoryShell({ theme, fontScale, rowState, statusLabel, statusKind, footer }) {
    return (
      <Screen theme={theme} fontScale={fontScale}>
        <window.FFTopBar title="Dopamine" trailing={<span style={ty('caption', { color: 'var(--ff-text2)' })}>7 apps</span>} />
        <div style={{ paddingTop: 4 }}>
          {CAT_APPS.map((a) => <AppRow key={a.name} name={a.name} state={rowState ? rowState(a.name) : 'normal'} height={SPACE.rowH} />)}
        </div>
        <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0 }}>
          {footer}
          <div style={{ borderTop: '1px solid var(--ff-line)', padding: `13px ${SPACE.screenPad}px`, display: 'flex', alignItems: 'center', gap: 11 }}>
            <Dot kind={statusKind} size={8} />
            <span style={ty('section', { color: 'var(--ff-text2)' })}>Status</span>
            <span style={ty('caption', { color: statusKind === 'failed' ? 'var(--ff-err)' : 'var(--ff-text2)', marginLeft: 'auto' })}>{statusLabel}</span>
          </div>
        </div>
      </Screen>
    );
  }

  // 13 · ready
  window.CategoryReady = ({ theme, fontScale }) => (
    <CategoryShell theme={theme} fontScale={fontScale} statusKind="ready" statusLabel="Shizuku ready"
      footer={<div style={{ padding: `0 ${SPACE.screenPad}px 6px` }}><Btn full onTop kind="primary">Pause until midnight</Btn></div>} />
  );

  // 14 · paused
  window.CategoryPaused = ({ theme, fontScale }) => (
    <CategoryShell theme={theme} fontScale={fontScale} statusKind="paused" statusLabel="Paused until 00:00"
      rowState={(n) => (['YouTube', 'Instagram', 'Reddit'].includes(n) ? 'paused' : 'normal')}
      footer={<div style={{ padding: `0 ${SPACE.screenPad}px 6px` }}><Btn full onTop kind="primary">Unpause now</Btn></div>} />
  );

  // 15 · setup required
  window.CategorySetup = ({ theme, fontScale }) => (
    <CategoryShell theme={theme} fontScale={fontScale} statusKind="paused" statusLabel="Setup required"
      footer={
        <div style={{ padding: `0 ${SPACE.screenPad}px 6px` }}>
          <div style={{ paddingBottom: 10 }}>
            <StatusBanner kind="setup" title="Shizuku setup required" action="Open Shizuku" />
          </div>
          <Btn full onTop kind="primary" disabled>Pause until midnight</Btn>
        </div>
      } />
  );

  // 16 · failed to unpause
  window.CategoryFailed = ({ theme, fontScale }) => (
    <CategoryShell theme={theme} fontScale={fontScale} statusKind="failed" statusLabel="Failed to unpause"
      rowState={(n) => (['YouTube', 'Instagram', 'Reddit'].includes(n) ? 'paused' : 'normal')}
      footer={
        <div style={{ padding: `0 ${SPACE.screenPad}px 6px` }}>
          <div style={{ paddingBottom: 10 }}>
            <StatusBanner kind="failed" title="Couldn’t unpause 1 app" sub="Instagram — shell command failed" action="Details" />
          </div>
          <Btn full onTop kind="primary">Try again</Btn>
        </div>
      } />
  );

  // 17 · Pause Result / Error details (sheet over category)
  window.PauseResult = ({ theme, fontScale }) => (
    <Screen theme={theme} fontScale={fontScale} gesture={false}>
      <div style={{ opacity: 0.45 }}>
        <window.FFTopBar title="Dopamine" />
      </div>
      <Backdrop>
        <Sheet>
          <div style={{ padding: `2px ${SPACE.sheetPad}px 14px` }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 11 }}>
              <Dot kind="paused" size={9} />
              <span style={ty('sheetTitle', { color: 'var(--ff-text)' })}>Paused until 00:00</span>
            </div>
            <div style={ty('caption', { color: 'var(--ff-text2)', marginTop: 6, paddingLeft: 20 })}>5 apps paused · 1 failed</div>
          </div>
          <div style={{ borderTop: '1px solid var(--ff-line)', padding: `14px ${SPACE.sheetPad}px 4px` }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 11, marginBottom: 12 }}>
              <Dot kind="failed" size={8} />
              <span style={ty('body', { color: 'var(--ff-err)', fontWeight: 500 })}>Couldn’t pause Instagram</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10, color: 'var(--ff-text2)' }}>
              <span style={{ transform: 'rotate(90deg)', display: 'flex' }}><Icon.chevRight size={15} /></span>
              <span style={ty('caption', { color: 'var(--ff-text2)' })}>Technical details</span>
            </div>
            <CodeBlock rows={[
              { k: 'Command', v: 'pm disable-user --user 0 com.instagram.android' },
              { k: 'Exit code', v: '1', err: true },
              { k: 'Stderr', v: 'Security exception: shell does not have permission', err: true },
            ]} />
          </div>
          <div style={{ padding: `4px ${SPACE.sheetPad}px 0` }}>
            <SheetRow label="Retry Instagram" />
            <SheetRow label="Copy details" kind="muted" />
            <SheetRow label="Done" kind="muted" />
          </div>
        </Sheet>
      </Backdrop>
    </Screen>
  );

})();
