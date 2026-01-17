package com.focusfloat.app.distracting

import android.app.Activity
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.UserManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.focusfloat.app.FocusFloatApplication
import com.focusfloat.app.MainActivity
import com.focusfloat.app.design.ActionLine
import com.focusfloat.app.design.FocusFloatTheme
import com.focusfloat.app.design.FocusScreen
import com.focusfloat.app.design.FocusTheme
import com.focusfloat.app.design.TopBar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DistractingGateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val appLabel = intent.getStringExtra(EXTRA_APP_LABEL)?.takeIf { it.isNotBlank() } ?: packageName
        val userSerial = intent.getLongExtra(EXTRA_USER_SERIAL, 0L)
        val className = intent.getStringExtra(EXTRA_CLASS_NAME)?.takeIf { it.isNotBlank() }

        if (packageName.isBlank()) {
            finish()
            return
        }

        val container = (application as FocusFloatApplication).container
        lifecycleScope.launch {
            val settings = container.settingsRepository.settings.first()
            setContent {
                FocusFloatTheme(mode = settings.themeMode, textScale = settings.textScale) {
                    FocusScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .imePadding(),
                    ) {
                        var value by remember { mutableStateOf("") }
                        val focusRequester = remember { FocusRequester() }
                        val keyboard = LocalSoftwareKeyboardController.current
                        val confirmed = value.trim().equals("confirmed", ignoreCase = true)

                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                            keyboard?.show()
                        }

                        Column(Modifier.fillMaxSize().padding(top = 28.dp, bottom = 22.dp)) {
                            TopBar(title = appLabel)
                            Spacer(Modifier.height(42.dp))
                            Text(
                                text = "By opening this app, I am approving that it might ruin today's plans and productivity.",
                                color = FocusTheme.colors.text,
                                style = FocusTheme.type.sheetTitle,
                                modifier = Modifier.padding(horizontal = FocusTheme.spacing.screenPad),
                            )
                            Spacer(Modifier.height(18.dp))
                            Text(
                                text = "Type confirmed to continue.",
                                color = FocusTheme.colors.text2,
                                style = FocusTheme.type.body,
                                modifier = Modifier.padding(horizontal = FocusTheme.spacing.screenPad),
                            )
                            Spacer(Modifier.height(14.dp))
                            BasicTextField(
                                value = value,
                                onValueChange = { value = it },
                                textStyle = FocusTheme.type.body.copy(color = FocusTheme.colors.text),
                                modifier = Modifier
                                    .padding(horizontal = FocusTheme.spacing.screenPad)
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .background(FocusTheme.colors.surface2, RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                singleLine = true,
                            )
                            Spacer(Modifier.weight(1f))
                            ActionLine(text = "Cancel", onClick = ::returnToFocusFloat)
                            ActionLine(
                                text = "Open app",
                                enabled = confirmed,
                                onClick = {
                                    DistractingGateApprovals.approve(packageName)
                                    setResult(Activity.RESULT_OK)
                                    if (launchTargetApp(packageName, className, userSerial)) {
                                        finish()
                                    } else {
                                        returnToFocusFloat()
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun returnToFocusFloat() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
        finish()
    }

    private fun launchTargetApp(packageName: String, className: String?, userSerial: Long): Boolean {
        val launcherApps = getSystemService(LauncherApps::class.java) ?: return false
        val userManager = getSystemService(UserManager::class.java) ?: return false
        val userHandle = launcherApps.profiles.firstOrNull { userManager.getSerialNumberForUser(it) == userSerial }
            ?: return false
        val activities = launcherApps.getActivityList(packageName, userHandle)
        val activity = className
            ?.let { targetClassName -> activities.firstOrNull { it.componentName.className == targetClassName } }
            ?: activities.firstOrNull()
            ?: return false
        return runCatching {
            launcherApps.startMainActivity(activity.componentName, userHandle, null, null)
        }.isSuccess
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_LABEL = "app_label"
        const val EXTRA_USER_SERIAL = "user_serial"
        const val EXTRA_CLASS_NAME = "class_name"
    }
}
