package com.focusfloat.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.focusfloat.app.core.model.AppEntry
import com.focusfloat.app.core.time.formatTime
import com.focusfloat.app.design.ActionLine
import com.focusfloat.app.design.AppRowState
import com.focusfloat.app.design.AppTextRow
import com.focusfloat.app.design.BannerKind
import com.focusfloat.app.design.BatteryArc
import com.focusfloat.app.design.DotKind
import com.focusfloat.app.design.FocusFloatTheme
import com.focusfloat.app.design.FocusScreen
import com.focusfloat.app.design.FocusTheme
import com.focusfloat.app.design.FocusTextScale
import com.focusfloat.app.design.FocusThemeMode
import com.focusfloat.app.design.SectionLabel
import com.focusfloat.app.design.SettingsRow
import com.focusfloat.app.design.StatusBanner
import com.focusfloat.app.design.StatusDot
import com.focusfloat.app.design.TopBar
import com.focusfloat.app.digitalwellbeing.DigitalWellbeingAutomationState
import com.focusfloat.app.digitalwellbeing.DigitalWellbeingAutomationStatus
import com.focusfloat.app.pause.model.PauseSessionStatus
import java.time.Instant
import kotlin.math.abs

private enum class Route {
    Onboarding,
    Home,
    Apps,
    Favorites,
    Category,
    Distracting,
    Settings,
    Hidden,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusFloatRoot(
    state: MainUiState,
    actions: MainViewModel,
    onRequestNotifications: () -> Unit,
    onSetAsHome: () -> Unit,
) {
    var route by rememberSaveable { mutableStateOf(Route.Home) }
    var selectedApp by remember { mutableStateOf<AppEntry?>(null) }
    var renameTarget by remember { mutableStateOf<AppEntry?>(null) }
    var distractingGateApp by remember { mutableStateOf<AppEntry?>(null) }
    var addAppsReturnRoute by rememberSaveable { mutableStateOf<Route?>(null) }
    var listReturnRoute by rememberSaveable { mutableStateOf(Route.Home) }
    val settings = state.settings
    val onboardingRequired = settings?.onboardingCompleted == false

    fun openAppsRoute() {
        addAppsReturnRoute = null
        route = Route.Apps
    }

    fun openFavoritesRoute(returnRoute: Route = Route.Home) {
        listReturnRoute = returnRoute
        route = Route.Favorites
    }

    fun openDistractingRoute(returnRoute: Route = Route.Home) {
        listReturnRoute = returnRoute
        route = Route.Distracting
    }

    fun requestOpenApp(app: AppEntry) {
        if (app.isPaused) {
            selectedApp = app
        } else if (app.key.ref in state.distractingPackages) {
            onRequestNotifications()
            distractingGateApp = app
        } else {
            actions.launchApp(app)
        }
    }

    fun navigateBack() {
        when (route) {
            Route.Home -> Unit
            Route.Apps -> {
                route = addAppsReturnRoute ?: Route.Home
                addAppsReturnRoute = null
            }
            Route.Category -> route = Route.Home
            Route.Favorites,
            Route.Distracting -> {
                route = listReturnRoute
                listReturnRoute = Route.Home
            }
            Route.Hidden,
            Route.Onboarding -> route = Route.Settings
            Route.Settings -> route = Route.Home
        }
    }

    BackHandler(enabled = !onboardingRequired || distractingGateApp != null || renameTarget != null || selectedApp != null) {
        when {
            distractingGateApp != null -> distractingGateApp = null
            renameTarget != null -> renameTarget = null
            selectedApp != null -> selectedApp = null
            else -> navigateBack()
        }
    }

    FocusFloatTheme(
        mode = settings?.themeMode ?: FocusThemeMode.Amoled,
        textScale = settings?.textScale ?: FocusTextScale.Medium,
    ) {
        FocusScreen(modifier = Modifier.fillMaxSize()) {
            val swipeEnabled = !onboardingRequired &&
                selectedApp == null &&
                renameTarget == null &&
                distractingGateApp == null
            val visibleRoute = if (onboardingRequired) Route.Onboarding else route
            Box(
                Modifier
                    .fillMaxSize()
                    .horizontalRouteSwipe(
                        enabled = swipeEnabled,
                        routeKey = route,
                        onSwipe = { deltaX ->
                            when {
                                route == Route.Home && deltaX < 0f -> openAppsRoute()
                                route == Route.Apps && deltaX > 0f -> navigateBack()
                            }
                        },
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                AnimatedContent(
                    targetState = visibleRoute,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        val direction = routeSlideDirection(initialState, targetState)
                        val animation = tween<IntOffset>(ROUTE_ANIMATION_MS)
                        val fadeAnimation = tween<Float>(ROUTE_ANIMATION_MS)
                        (
                            slideInHorizontally(animationSpec = animation) { fullWidth -> direction * fullWidth } +
                                fadeIn(animationSpec = fadeAnimation)
                            ) togetherWith (
                            slideOutHorizontally(animationSpec = animation) { fullWidth -> -direction * fullWidth } +
                                fadeOut(animationSpec = fadeAnimation)
                            )
                    },
                    label = "FocusFloat route",
                ) { targetRoute ->
                    when {
                        state.loading -> LoadingScreen()
                        targetRoute == Route.Onboarding -> OnboardingScreen(
                            state = state,
                            onBack = if (onboardingRequired) null else ({ route = Route.Settings }),
                            onSetAsHome = onSetAsHome,
                            onOpenAccessibilitySettings = actions::openAccessibilitySettings,
                            onRefresh = actions::refresh,
                            onFinish = {
                                if (state.homeRoleHeld) {
                                    actions.completeOnboarding()
                                    route = Route.Home
                                } else {
                                    onSetAsHome()
                                }
                            },
                            onSkip = {
                                actions.completeOnboarding()
                                route = Route.Home
                            },
                        )
                        targetRoute == Route.Home -> HomeScreen(
                            state = state,
                            onOpenApps = ::openAppsRoute,
                            onOpenFavorites = { openFavoritesRoute() },
                            onOpenSettings = { route = Route.Settings },
                            onOpenCategory = { route = Route.Category },
                            onOpenDistracting = { openDistractingRoute() },
                            onUnpause = actions::unpausePrimaryCategory,
                        )
                        targetRoute == Route.Apps -> AppsScreen(
                            title = "All apps",
                            apps = state.visibleApps,
                            onBack = ::navigateBack,
                            onAppClick = { selectedApp = it },
                            onOpenSettings = { route = Route.Settings },
                        )
                        targetRoute == Route.Favorites -> FavoritesScreen(
                            state = state,
                            onBack = ::navigateBack,
                            onAppClick = { selectedApp = it },
                            onAddApps = {
                                addAppsReturnRoute = Route.Favorites
                                route = Route.Apps
                            },
                        )
                        targetRoute == Route.Category -> CategoryScreen(
                            state = state,
                            onBack = ::navigateBack,
                            onPause = {
                                onRequestNotifications()
                                actions.pausePrimaryCategory()
                            },
                            onUnpause = actions::unpausePrimaryCategory,
                            onRefresh = actions::refresh,
                            onAppClick = { selectedApp = it },
                            onAddApps = {
                                addAppsReturnRoute = Route.Category
                                route = Route.Apps
                            },
                        )
                        targetRoute == Route.Distracting -> DistractingScreen(
                            state = state,
                            onBack = ::navigateBack,
                            onAppClick = { selectedApp = it },
                            onAddApps = {
                                addAppsReturnRoute = Route.Distracting
                                route = Route.Apps
                            },
                            onOpenAccessibilitySettings = actions::openAccessibilitySettings,
                        )
                        targetRoute == Route.Settings -> SettingsScreen(
                            state = state,
                            onBack = ::navigateBack,
                            onSetAsHome = onSetAsHome,
                            onOpenFavorites = { openFavoritesRoute(Route.Settings) },
                            onOpenHidden = { route = Route.Hidden },
                            onOpenOnboarding = { route = Route.Onboarding },
                            onOpenDistracting = { openDistractingRoute(Route.Settings) },
                            onOpenAccessibilitySettings = actions::openAccessibilitySettings,
                        )
                        targetRoute == Route.Hidden -> HiddenAppsScreen(
                            apps = state.hiddenApps,
                            onBack = ::navigateBack,
                            onRestore = { actions.hideApp(it, hidden = false) },
                        )
                    }
                }
            }
        }

        selectedApp?.let { app ->
            ModalBottomSheet(
                onDismissRequest = { selectedApp = null },
                containerColor = FocusTheme.colors.surface,
                contentColor = FocusTheme.colors.text,
            ) {
                AppActionsSheet(
                    app = app,
                    inPrimaryCategory = app.key.ref in state.primaryCategoryPackages,
                    inDistractingCategory = app.key.ref in state.distractingPackages,
                    onOpen = {
                        selectedApp = null
                        if (app.isPaused) actions.openPixelFocusModeSettings() else requestOpenApp(app)
                    },
                    onFavorite = {
                        selectedApp = null
                        actions.toggleFavorite(app)
                    },
                    onRename = {
                        selectedApp = null
                        renameTarget = app
                    },
                    onHide = {
                        selectedApp = null
                        actions.hideApp(app)
                    },
                    onToggleCategory = {
                        selectedApp = null
                        if (app.key.ref in state.primaryCategoryPackages) {
                            actions.removeFromPrimaryPauseCategory(app)
                        } else {
                            actions.addToPrimaryPauseCategory(app)
                        }
                    },
                    onToggleDistracting = {
                        selectedApp = null
                        if (app.key.ref in state.distractingPackages) {
                            actions.removeFromDistractingCategory(app)
                        } else {
                            actions.addToDistractingCategory(app)
                        }
                    },
                    onAppInfo = {
                        selectedApp = null
                        actions.openAppInfo(app)
                    },
                )
            }
        }

        renameTarget?.let { app ->
            RenameDialog(
                app = app,
                onDismiss = { renameTarget = null },
                onSave = { label ->
                    actions.renameApp(app, label)
                    renameTarget = null
                },
            )
        }

        distractingGateApp?.let { app ->
            DistractingGateDialog(
                app = app,
                onDismiss = { distractingGateApp = null },
                onConfirm = {
                    distractingGateApp = null
                    actions.launchConfirmedDistractingApp(app)
                },
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 54.dp, bottom = 28.dp),
    ) {
        Text(
            text = "FocusFloat",
            color = FocusTheme.colors.text,
            style = FocusTheme.type.sheetTitle,
            modifier = Modifier.padding(horizontal = FocusTheme.spacing.screenPad),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Loading setup",
            color = FocusTheme.colors.text2,
            style = FocusTheme.type.caption,
            modifier = Modifier.padding(horizontal = FocusTheme.spacing.screenPad),
        )
    }
}

@Composable
private fun HomeScreen(
    state: MainUiState,
    onOpenApps: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCategory: () -> Unit,
    onOpenDistracting: () -> Unit,
    onUnpause: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 54.dp, bottom = 28.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FocusTheme.spacing.screenPad),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = state.clockText, color = FocusTheme.colors.text, style = FocusTheme.type.clock)
                Text(text = state.dateText, color = FocusTheme.colors.text2, style = FocusTheme.type.date)
            }
            BatteryArc(pct = state.batteryPct ?: 0f, modifier = Modifier.clickable(onClick = onOpenSettings))
        }

        Spacer(Modifier.weight(1f))

        PauseHomeBlock(state, onUnpause)

        if (!state.hasHomePauseStatus()) {
            DigitalWellbeingAutomationBanner(state.digitalWellbeingAutomationStatus)
        }

        state.banner?.let { banner ->
            Spacer(Modifier.height(14.dp))
            StatusBanner(
                title = banner.title,
                sub = banner.body,
                subMaxLines = 3,
                kind = if (banner.failed) BannerKind.Failed else BannerKind.Info,
            )
        }

        Spacer(Modifier.height(16.dp))
        ActionLine(text = "All apps", sub = "${state.visibleApps.size} apps", onClick = onOpenApps)
        ActionLine(text = "Favorite apps", sub = "${state.favorites.size} apps", onClick = onOpenFavorites)
        ActionLine(text = "Paused apps", sub = state.pauseListSubtitle(), onClick = onOpenCategory)
        ActionLine(text = "Distracting apps", sub = "${state.distractingApps.size} apps", onClick = onOpenDistracting)
    }
}

@Composable
private fun OnboardingScreen(
    state: MainUiState,
    onBack: (() -> Unit)?,
    onSetAsHome: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onRefresh: () -> Unit,
    onFinish: () -> Unit,
    onSkip: () -> Unit,
) {
    var step by rememberSaveable { mutableStateOf(0) }
    val maxStep = 3
    val categoryName = state.primaryCategory?.name ?: "Paused apps"
    val pauseEngineReady = state.digitalWellbeingAssistantEnabled

    Column(Modifier.fillMaxSize().padding(top = 28.dp, bottom = 22.dp)) {
        TopBar(title = "FocusFloat setup", onBack = onBack)
        SectionLabel("Step ${step + 1} of ${maxStep + 1}")
        Spacer(Modifier.height(28.dp))
        when (step) {
            0 -> OnboardingIntroStep()
            1 -> OnboardingHomeStep(homeRoleHeld = state.homeRoleHeld, onSetAsHome = onSetAsHome)
            2 -> OnboardingAssistantStep(
                enabled = state.digitalWellbeingAssistantEnabled,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                onRefresh = onRefresh,
            )
            else -> OnboardingCategoryStep(
                categoryName = categoryName,
                appCount = state.primaryCategoryPackages.size,
                pauseEngineReady = pauseEngineReady,
            )
        }
        Spacer(Modifier.weight(1f))
        if (step > 0) {
            ActionLine(text = "Back", onClick = { step -= 1 })
        }
        if (step < maxStep) {
            ActionLine(
                text = if (step == 0) "Start setup" else "Continue",
                onClick = { step += 1 },
            )
        } else {
            ActionLine(
                text = if (state.homeRoleHeld) "Finish" else "Set as home screen",
                sub = if (state.homeRoleHeld) null else "Required before completing setup.",
                onClick = onFinish,
            )
        }
        ActionLine(text = "Skip for now", sub = "You can reopen this guide from Settings.", onClick = onSkip)
    }
}

@Composable
private fun OnboardingIntroStep() {
    OnboardingCopy(
        title = "A quieter home screen",
        body = "FocusFloat keeps the launcher plain, searchable, and focused on the apps you choose.",
    )
    Spacer(Modifier.height(18.dp))
    SettingsRow(label = "Minimal launcher", sub = "Text-first home screen with favorites and app search.")
    SettingsRow(label = "Pause list", sub = "Use Pixel Focus Mode with guided screen automation.")
    SettingsRow(label = "Local control", sub = "No account, servers, tracking, or internet access.")
}

@Composable
private fun OnboardingHomeStep(homeRoleHeld: Boolean, onSetAsHome: () -> Unit) {
    OnboardingCopy(
        title = "Set FocusFloat as Home",
        body = "Android needs FocusFloat selected as your Home app before the launcher can replace the Pixel launcher.",
    )
    Spacer(Modifier.height(18.dp))
    StatusBanner(
        title = if (homeRoleHeld) "FocusFloat is your Home app" else "Home app required",
        sub = if (homeRoleHeld) {
            "Pixel Home gestures will open FocusFloat."
        } else {
            "Choose FocusFloat in the Android prompt, then return here."
        },
        action = if (homeRoleHeld) null else "Set as home screen",
        onAction = onSetAsHome,
    )
}

@Composable
private fun OnboardingAssistantStep(
    enabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    OnboardingCopy(
        title = "Enable Focus Mode assistant",
        body = "FocusFloat can use Accessibility to select your pause-list apps inside Pixel Focus Mode.",
    )
    Spacer(Modifier.height(18.dp))
    StatusBanner(
        title = if (enabled) "Assistant enabled" else "Accessibility permission required",
        sub = if (enabled) {
            "FocusFloat can run the guided Focus Mode setup."
        } else {
            "Enable FocusFloat Focus Mode assistant. It is separate from the distracting app guard."
        },
        action = if (enabled) null else "Open Accessibility",
        kind = if (enabled) BannerKind.Info else BannerKind.Setup,
        onAction = onOpenAccessibilitySettings,
    )
    Spacer(Modifier.height(12.dp))
    ActionLine(text = "Refresh status", onClick = onRefresh)
}

@Composable
private fun OnboardingCategoryStep(
    categoryName: String,
    appCount: Int,
    pauseEngineReady: Boolean,
) {
    OnboardingCopy(
        title = "Prepare your pause list",
        body = "Add apps to the pause list, then let FocusFloat select them inside Pixel Focus Mode.",
    )
    Spacer(Modifier.height(18.dp))
    SettingsRow(label = categoryName, sub = "$appCount apps selected")
    SettingsRow(label = "Focus Mode assistant", value = if (pauseEngineReady) "Ready" else "Required")
    SettingsRow(label = "Next", sub = "Finish setup, open $categoryName, and add the first apps.")
}

@Composable
private fun OnboardingCopy(title: String, body: String) {
    Column(Modifier.padding(horizontal = FocusTheme.spacing.screenPad)) {
        Text(text = title, color = FocusTheme.colors.text, style = FocusTheme.type.sheetTitle)
        Spacer(Modifier.height(10.dp))
        Text(text = body, color = FocusTheme.colors.text2, style = FocusTheme.type.body)
    }
}

@Composable
private fun PauseHomeBlock(
    state: MainUiState,
    onUnpause: () -> Unit,
) {
    val session = state.activeSession
    if (session == null) return
    val failed = session.status == PauseSessionStatus.PartiallyFailed ||
        session.status == PauseSessionStatus.FailedToUnpause
    StatusBanner(
        title = pausedAppsTitle(session.packageNames.size, session.pausedUntil, failed),
        sub = session.failureSummary?.compactFailureSummary(),
        subMaxLines = 3,
        action = "Turn off Pixel Focus Mode",
        kind = if (failed) BannerKind.Failed else BannerKind.Info,
        onAction = onUnpause,
    )
}

@Composable
private fun DigitalWellbeingAutomationBanner(status: DigitalWellbeingAutomationStatus?) {
    if (status == null || status.state == DigitalWellbeingAutomationState.Idle) return
    Spacer(Modifier.height(14.dp))
    StatusBanner(
        title = digitalWellbeingAutomationTitle(status),
        sub = digitalWellbeingAutomationSub(status),
        subMaxLines = 3,
        kind = if (status.state == DigitalWellbeingAutomationState.Failed) BannerKind.Failed else BannerKind.Info,
    )
}

@Composable
private fun AppsScreen(
    title: String,
    apps: List<AppEntry>,
    onBack: () -> Unit,
    onAppClick: (AppEntry) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    val visibleApps = remember(apps, query.text) {
        val needle = query.text.trim().lowercase()
        if (needle.isBlank()) {
            apps
        } else {
            apps.filter {
                it.displayLabel.lowercase().contains(needle) ||
                    it.key.packageName.lowercase().contains(needle)
            }
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(top = 28.dp, bottom = 22.dp)
            .imePadding(),
    ) {
        TopBar(title = title, onBack = onBack, trailing = {
            Text(
                text = "Settings",
                color = FocusTheme.colors.text2,
                style = FocusTheme.type.caption,
                modifier = Modifier.clickable(onClick = onOpenSettings),
            )
        })
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = FocusTheme.spacing.screenPad)
                .height(56.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (query.text.isBlank()) {
                Text("Search apps", color = FocusTheme.colors.text3, style = FocusTheme.type.appRow)
            }
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                textStyle = FocusTheme.type.appRow.copy(color = FocusTheme.colors.text),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(FocusTheme.colors.line))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(visibleApps, key = { "${it.key.packageName}/${it.key.className}/${it.key.userSerial}" }) { app ->
                AppTextRow(
                    name = app.displayLabel,
                    state = app.rowState(),
                    meta = app.meta(),
                    onClick = { onAppClick(app) },
                )
            }
            if (visibleApps.isEmpty()) {
                item { EmptyLine("No results") }
            }
        }
    }
}

@Composable
private fun FavoritesScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onAppClick: (AppEntry) -> Unit,
    onAddApps: () -> Unit,
) {
    val apps = state.favorites
    Column(Modifier.fillMaxSize().padding(top = 28.dp, bottom = 22.dp)) {
        TopBar(title = "Favorite apps", onBack = onBack)
        Text(
            text = "${apps.size} apps",
            color = FocusTheme.colors.text2,
            style = FocusTheme.type.body,
            modifier = Modifier.padding(horizontal = FocusTheme.spacing.screenPad),
        )
        Spacer(Modifier.height(20.dp))
        if (apps.isEmpty()) {
            Box(modifier = Modifier.weight(1f)) {
                EmptyLine("No favorite apps")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(apps, key = { it.lazyListKey() }) { app ->
                    AppTextRow(
                        name = app.displayLabel,
                        state = app.rowState(),
                        meta = app.meta(),
                        favorite = true,
                        onClick = { onAppClick(app) },
                    )
                }
            }
        }
        ActionLine(text = "Add apps", onClick = onAddApps)
    }
}

@Composable
private fun CategoryScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onPause: () -> Unit,
    onUnpause: () -> Unit,
    onRefresh: () -> Unit,
    onAppClick: (AppEntry) -> Unit,
    onAddApps: () -> Unit,
) {
    val categoryName = state.primaryCategory?.name ?: "Paused apps"
    val session = state.activeSession
    val androidPausedApps = state.primaryCategoryAndroidPausedCount()
    Column(Modifier.fillMaxSize().padding(top = 28.dp, bottom = 22.dp)) {
        TopBar(title = categoryName, onBack = onBack)
        Text(
            text = state.pauseListCountLine(),
            color = FocusTheme.colors.text2,
            style = FocusTheme.type.body,
            modifier = Modifier.padding(horizontal = FocusTheme.spacing.screenPad),
        )
        Spacer(Modifier.height(20.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.primaryCategoryApps, key = { it.lazyListKey() }) { app ->
                AppTextRow(
                    name = app.displayLabel,
                    state = app.rowState(),
                    meta = app.meta(),
                    onClick = { onAppClick(app) },
                )
            }
        }
        DigitalWellbeingAutomationBanner(state.digitalWellbeingAutomationStatus)
        if (session != null) {
            ActionLine(
                text = "Turn off Pixel Focus Mode",
                sub = "Affects every app selected in Pixel Focus Mode.",
                onClick = onUnpause,
            )
        } else {
            if (androidPausedApps > 0) {
                ActionLine(text = "Refresh status", sub = "Re-check Android pause state.", onClick = onRefresh)
            }
            ActionLine(text = "Add apps", onClick = onAddApps)
            ActionLine(
                text = if (state.digitalWellbeingAssistantEnabled) {
                    "Start Focus Mode assistant"
                } else {
                    "Enable Focus Mode assistant"
                },
                sub = if (androidPausedApps > 0) {
                    "Select this pause list in Pixel Focus Mode."
                } else {
                    null
                },
                enabled = state.primaryCategoryApps.isNotEmpty(),
                onClick = onPause,
            )
        }
        Row(
            modifier = Modifier.padding(horizontal = FocusTheme.spacing.screenPad, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(if (state.digitalWellbeingAssistantEnabled) DotKind.Ready else DotKind.Hollow)
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (state.digitalWellbeingAssistantEnabled) "Assistant ready" else "Accessibility required",
                color = FocusTheme.colors.text2,
                style = FocusTheme.type.caption,
            )
        }
    }
}

@Composable
private fun DistractingScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onAppClick: (AppEntry) -> Unit,
    onAddApps: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
) {
    val apps = state.distractingApps
    Column(Modifier.fillMaxSize().padding(top = 28.dp, bottom = 22.dp)) {
        TopBar(title = "Distracting apps", onBack = onBack)
        Text(
            text = "${apps.size} apps",
            color = FocusTheme.colors.text2,
            style = FocusTheme.type.body,
            modifier = Modifier.padding(horizontal = FocusTheme.spacing.screenPad),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "These apps are not disabled. FocusFloat asks for a typed confirmation before opening them. Enable the external launch guard to cover notification opens and usage reminders.",
            color = FocusTheme.colors.text2,
            style = FocusTheme.type.caption,
            modifier = Modifier.padding(horizontal = FocusTheme.spacing.screenPad),
        )
        Spacer(Modifier.height(20.dp))
        if (apps.isEmpty()) {
            Box(modifier = Modifier.weight(1f)) {
                EmptyLine("No distracting apps")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(apps, key = { it.lazyListKey() }) { app ->
                    AppTextRow(
                        name = app.displayLabel,
                        meta = "requires confirmation",
                        onClick = { onAppClick(app) },
                    )
                }
            }
        }
        ActionLine(text = "External launch guard", sub = "Required for notification opens and accurate reminders.", onClick = onOpenAccessibilitySettings)
        ActionLine(text = "Add apps", onClick = onAddApps)
    }
}

@Composable
private fun SettingsScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onSetAsHome: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenHidden: () -> Unit,
    onOpenOnboarding: () -> Unit,
    onOpenDistracting: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(top = 28.dp)) {
        TopBar(title = "Settings", onBack = onBack)
        SettingsRow(label = "Set as home screen", onClick = onSetAsHome)
        SettingsRow(label = "Setup guide", sub = "Home, assistant, and pause list", onClick = onOpenOnboarding)
        SettingsRow(label = "Favorites", sub = "${state.favorites.size} apps", onClick = onOpenFavorites)
        SettingsRow(label = "Hidden apps", sub = "${state.hiddenApps.size} apps", onClick = onOpenHidden)
        SettingsRow(label = "Pause list", sub = state.primaryCategory?.name ?: "Paused apps")
        SettingsRow(label = "Distracting apps", sub = "${state.distractingApps.size} apps", onClick = onOpenDistracting)
        SettingsRow(label = "External launch guard", sub = "Accessibility permission", onClick = onOpenAccessibilitySettings)
        SettingsRow(label = "Appearance", sub = "${state.settings?.themeMode ?: FocusThemeMode.Amoled} - ${state.settings?.textScale ?: FocusTextScale.Medium}")
        SettingsRow(
            label = "Focus Mode assistant",
            sub = if (state.digitalWellbeingAssistantEnabled) "Enabled" else "Accessibility permission",
            onClick = onOpenAccessibilitySettings,
        )
        SettingsRow(label = "Debug", sub = "Active sessions: ${if (state.activeSession == null) 0 else 1}")
    }
}

@Composable
private fun HiddenAppsScreen(
    apps: List<AppEntry>,
    onBack: () -> Unit,
    onRestore: (AppEntry) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(top = 28.dp)) {
        TopBar(title = "Hidden apps", onBack = onBack)
        if (apps.isEmpty()) {
            EmptyLine("No hidden apps")
        } else {
            LazyColumn {
                items(apps, key = { it.lazyListKey() }) { app ->
                    SettingsRow(label = app.displayLabel, value = "Restore", onClick = { onRestore(app) })
                }
            }
        }
    }
}

@Composable
private fun AppActionsSheet(
    app: AppEntry,
    inPrimaryCategory: Boolean,
    inDistractingCategory: Boolean,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onRename: () -> Unit,
    onHide: () -> Unit,
    onToggleCategory: () -> Unit,
    onToggleDistracting: () -> Unit,
    onAppInfo: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Text(
            text = app.displayLabel,
            color = FocusTheme.colors.text,
            style = FocusTheme.type.sheetTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = FocusTheme.spacing.sheetPad, vertical = 12.dp),
        )
        SheetAction(if (app.isPaused) "Open Pixel Focus Mode settings" else "Open", onOpen)
        SheetAction(if (app.isFavorite) "Remove from favorites" else "Add to favorites", onFavorite)
        SheetAction("Rename", onRename)
        SheetAction("Hide from app list", onHide, muted = app.isProtected)
        if (app.isProtected) {
            SheetAction("Protected", {}, muted = true)
        } else {
            SheetAction(
                if (inPrimaryCategory) "Remove from pause list" else "Add to pause list",
                onToggleCategory,
            )
            if (inDistractingCategory) {
                SheetAction("Remove from Distracting apps", onToggleDistracting)
            } else if (!app.isPaused) {
                SheetAction("Add to Distracting apps", onToggleDistracting)
            }
        }
        SheetAction("App info", onAppInfo)
    }
}

@Composable
private fun SheetAction(text: String, onClick: () -> Unit, muted: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(FocusTheme.spacing.sheetRowH)
            .clickable(enabled = !muted, onClick = onClick)
            .padding(horizontal = FocusTheme.spacing.sheetPad),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            color = if (muted) FocusTheme.colors.text3 else FocusTheme.colors.text,
            style = FocusTheme.type.body,
        )
    }
}

@Composable
private fun RenameDialog(
    app: AppEntry,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
) {
    var value by remember { mutableStateOf(app.customLabel ?: app.label) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FocusTheme.colors.surface,
        titleContentColor = FocusTheme.colors.text,
        textContentColor = FocusTheme.colors.text2,
        title = { Text("Rename app", style = FocusTheme.type.sheetTitle) },
        text = {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                textStyle = FocusTheme.type.body.copy(color = FocusTheme.colors.text),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FocusTheme.colors.surface2, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }) {
                Text("Save", color = FocusTheme.colors.text, style = FocusTheme.type.button)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = FocusTheme.colors.text2, style = FocusTheme.type.button)
            }
        },
    )
}

@Composable
private fun DistractingGateDialog(
    app: AppEntry,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var value by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val confirmed = value.trim().equals("confirmed", ignoreCase = true)
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FocusTheme.colors.surface,
        titleContentColor = FocusTheme.colors.text,
        textContentColor = FocusTheme.colors.text2,
        title = { Text(app.displayLabel, style = FocusTheme.type.sheetTitle) },
        text = {
            Column {
                Text(
                    text = "By opening this app, I am approving that it might ruin today's plans and productivity.",
                    color = FocusTheme.colors.text2,
                    style = FocusTheme.type.body,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Type confirmed to continue.",
                    color = FocusTheme.colors.text3,
                    style = FocusTheme.type.caption,
                )
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    textStyle = FocusTheme.type.body.copy(color = FocusTheme.colors.text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .background(FocusTheme.colors.surface2, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = confirmed, onClick = onConfirm) {
                Text(
                    "Open app",
                    color = if (confirmed) FocusTheme.colors.text else FocusTheme.colors.text3,
                    style = FocusTheme.type.button,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = FocusTheme.colors.text2, style = FocusTheme.type.button)
            }
        },
    )
}

@Composable
private fun EmptyLine(text: String) {
    Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
        Text(text, color = FocusTheme.colors.text2, style = FocusTheme.type.body)
    }
}

private fun AppEntry.rowState(): AppRowState {
    return when {
        isHidden -> AppRowState.Hidden
        isProtected -> AppRowState.Protected
        isPaused -> AppRowState.Paused
        else -> AppRowState.Normal
    }
}

private fun AppEntry.meta(): String? {
    return when {
        isPaused && pausedUntil != null -> "paused until ${formatTime(pausedUntil)}"
        isPaused -> "paused by Android"
        isProtected -> "protected"
        isHidden -> "hidden"
        else -> null
    }
}

private fun AppEntry.lazyListKey(): String = "${key.packageName}/${key.className}/${key.userSerial}"

private fun MainUiState.pauseListSubtitle(): String {
    val session = activeSession
    return if (session != null) {
        "${session.packageNames.size} ${appCountLabel(session.packageNames.size)} paused"
    } else if (primaryCategoryPackages.isEmpty()) {
        "No apps in list"
    } else {
        "${primaryCategoryPackages.size} ${appCountLabel(primaryCategoryPackages.size)} in list"
    }
}

private fun MainUiState.primaryCategoryAndroidPausedCount(): Int {
    if (activeSession != null) return 0
    return primaryCategoryApps.count { it.isPaused }
}

private fun MainUiState.hasHomePauseStatus(): Boolean {
    return activeSession != null
}

private fun MainUiState.pauseListCountLine(): String {
    val androidPausedApps = primaryCategoryAndroidPausedCount()
    return if (androidPausedApps > 0) {
        "${primaryCategoryApps.size} ${appCountLabel(primaryCategoryApps.size)} in list; " +
            "$androidPausedApps ${appCountLabel(androidPausedApps)} paused by Android"
    } else {
        "${primaryCategoryApps.size} ${appCountLabel(primaryCategoryApps.size)} in list"
    }
}

private fun pausedAppsTitle(count: Int, pausedUntil: Instant, failed: Boolean): String {
    val paused = "$count ${appCountLabel(count)} paused"
    return if (failed) {
        "$paused; some failed"
    } else {
        "$paused until ${formatTime(pausedUntil)}"
    }
}

private fun digitalWellbeingAutomationTitle(status: DigitalWellbeingAutomationStatus): String {
    return when (status.state) {
        DigitalWellbeingAutomationState.Running -> "Focus Mode assistant running"
        DigitalWellbeingAutomationState.Completed -> "Focus Mode assistant completed"
        DigitalWellbeingAutomationState.Failed -> "Focus Mode assistant failed"
        DigitalWellbeingAutomationState.Idle -> "Focus Mode assistant"
    }
}

private fun digitalWellbeingAutomationSub(status: DigitalWellbeingAutomationStatus): String? {
    val progress = if (status.total > 0 && status.state == DigitalWellbeingAutomationState.Running) {
        "${status.completed.coerceIn(0, status.total)} of ${status.total} apps"
    } else {
        null
    }
    return listOfNotNull(status.message, progress)
        .distinct()
        .joinToString("\n")
        .ifBlank { null }
}

private fun appCountLabel(count: Int): String = if (count == 1) "app" else "apps"

private fun routeSlideDirection(initial: Route, target: Route): Int {
    return if (target.horizontalPosition() >= initial.horizontalPosition()) 1 else -1
}

private fun Route.horizontalPosition(): Int {
    return when (this) {
        Route.Home -> 0
        Route.Apps -> 1
        Route.Onboarding,
        Route.Favorites,
        Route.Category,
        Route.Distracting,
        Route.Settings,
        Route.Hidden -> 1
    }
}

private fun Modifier.horizontalRouteSwipe(
    enabled: Boolean,
    routeKey: Any?,
    onSwipe: (deltaX: Float) -> Unit,
): Modifier {
    if (!enabled) return this
    return pointerInput(enabled, routeKey) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var total = Offset.Zero
            var triggered = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                if (change != null) {
                    total += change.position - change.previousPosition
                    if (!triggered && abs(total.x) >= HORIZONTAL_SWIPE_THRESHOLD_PX && abs(total.x) > abs(total.y) * 1.2f) {
                        triggered = true
                        onSwipe(total.x)
                    }
                }
            } while (!triggered && event.changes.any { it.pressed })
        }
    }
}

private const val ROUTE_ANIMATION_MS = 220
private const val HORIZONTAL_SWIPE_THRESHOLD_PX = 80f

private fun String.compactFailureSummary(maxLines: Int = 2): String {
    val lines = lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .toList()
    if (lines.size <= maxLines) return lines.joinToString("\n")
    return lines.take(maxLines).joinToString("\n") + "\n+ ${lines.size - maxLines} more"
}
