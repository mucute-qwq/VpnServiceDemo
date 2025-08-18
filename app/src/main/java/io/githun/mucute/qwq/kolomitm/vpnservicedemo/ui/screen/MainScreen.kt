package io.githun.mucute.qwq.kolomitm.vpnservicedemo.ui.screen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.githun.mucute.qwq.kolomitm.vpnservicedemo.R
import io.githun.mucute.qwq.kolomitm.vpnservicedemo.service.AppService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val toggleVpn = {
        when (AppService.state) {
            AppService.State.Active -> context.startForegroundService(Intent(AppService.ACTION_STOP).apply {
                `package` = context.packageName
            })

            AppService.State.Inactive -> context.startForegroundService(Intent(AppService.ACTION_START).apply {
                `package` = context.packageName
            })

            AppService.State.Loading -> {}
        }
    }
    val vpnLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            toggleVpn()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("VPN permission denied")
            }
        }
    }
    val requestVpn = requestVpn@{
        val intent = VpnService.prepare(context) ?: return@requestVpn toggleVpn()
        vpnLauncher.launch(intent)
    }
    val postNotificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            requestVpn()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Notification permission denied")
            }
        }
    }

    var previousState by remember { mutableStateOf(AppService.state) }
    LaunchedEffect(AppService.state) {
        if (AppService.state === previousState) {
            return@LaunchedEffect
        }

        previousState = AppService.state
        when (AppService.state) {
            AppService.State.Active -> snackbarHostState.showSnackbar("AppService is active")
            AppService.State.Inactive -> snackbarHostState.showSnackbar("AppService is inactive")
            AppService.State.Loading -> {}
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.app_name))
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        postNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        requestVpn()
                    }
                }
            ) {
                Text("Start/Stop")
            }
        }
    }
}