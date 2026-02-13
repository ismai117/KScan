package org.ncgroup.kscan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import org.ncgroup.kscan.permissions.PermissionsViewModel

@Composable
fun MainView() {
    val factory = rememberPermissionsControllerFactory()
    val controller = remember(factory) { factory.createPermissionsController() }
    BindEffect(controller)

    val viewModel =
        viewModel {
            PermissionsViewModel(controller = controller)
        }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        when (viewModel.state) {
            PermissionState.DeniedAlways -> {
                Column(modifier = Modifier.align(Alignment.Center)) {
                    Text(text = "Permission always denied")
                    Button(
                        onClick = {
                            controller.openAppSettings()
                        },
                    ) {
                        Text(text = "Open settings")
                    }
                }
            }
            PermissionState.Granted -> {
                App()
            }
            else -> {
                Button(
                    onClick = {
                        viewModel.provideOrRequestPermission()
                    },
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    Text(text = "Request permission")
                }
            }
        }
    }
}
