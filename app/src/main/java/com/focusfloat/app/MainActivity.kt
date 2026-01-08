package com.focusfloat.app

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfloat.app.ui.FocusFloatRoot
import com.focusfloat.app.ui.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory((application as FocusFloatApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FocusFloatActivityContent(viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setHomeRoleHeld(focusFloatHasHomeRole())
        viewModel.reconcileAndRefresh()
    }

    private fun focusFloatHasHomeRole(): Boolean = applicationContext.focusFloatHasHomeRole()
}

@Composable
private fun FocusFloatActivityContent(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.setHomeRoleHeld(context.focusFloatHasHomeRole())
        viewModel.reconcileAndRefresh()
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    FocusFloatRoot(
        state = state,
        actions = viewModel,
        onRequestNotifications = ::requestNotificationsIfNeeded,
        onSetAsHome = {
            if (Build.VERSION.SDK_INT >= 29) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                    !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
                ) {
                    launcher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
                } else {
                    context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                }
            } else {
                context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            }
        },
    )
}

private fun Context.focusFloatHasHomeRole(): Boolean {
    return if (Build.VERSION.SDK_INT >= 29) {
        val roleManager = getSystemService(RoleManager::class.java)
        roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && roleManager.isRoleHeld(RoleManager.ROLE_HOME)
    } else {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName == packageName
    }
}
