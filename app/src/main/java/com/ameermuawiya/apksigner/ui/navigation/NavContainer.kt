package com.ameermuawiya.apksigner.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ameermuawiya.apksigner.R
import com.ameermuawiya.apksigner.ui.screens.HistoryScreen
import com.ameermuawiya.apksigner.ui.screens.HomeScreen
import com.ameermuawiya.apksigner.ui.screens.SettingsScreen
import com.ameermuawiya.apksigner.ui.screens.SignPackageScreen
import com.ameermuawiya.apksigner.ui.theme.ApkSignerTheme
import com.ameermuawiya.apksigner.ui.viewmodel.MainViewModel

/**
 * Data model representing bottom navigation item properties.
 */
data class NavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Main application navigation container managing tab switching and persistent screen states.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NavContainer(viewModel: MainViewModel) {
    val selectedDetails by viewModel.selectedAppDetails.collectAsState()
    val availableUpdate by viewModel.availableUpdate.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val navItems = listOf(
        NavItem(
            title = stringResource(R.string.nav_container_home),
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        NavItem(
            title = stringResource(R.string.nav_container_history),
            selectedIcon = Icons.Filled.History,
            unselectedIcon = Icons.Outlined.History
        ),
        NavItem(
            title = stringResource(R.string.nav_container_settings),
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            label = { Text(text = item.title) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                when (selectedTab) {
                    0 -> HomeScreen(viewModel = viewModel)
                    1 -> HistoryScreen(viewModel = viewModel)
                    2 -> SettingsScreen(viewModel = viewModel)
                }
            }
        }

        AnimatedVisibility(
            visible = selectedDetails != null,
            enter = fadeIn(animationSpec = tween(240)) + slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(240)
            ),
            exit = fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(200)
            )
        ) {
            selectedDetails?.let { details ->
                SignPackageScreen(
                    viewModel = viewModel,
                    details = details,
                    onBack = { viewModel.clearSelectedTarget() }
                )
            }
        }

        availableUpdate?.let { updateInfo ->
            com.ameermuawiya.apksigner.ui.components.AppUpdateBottomSheet(
                updateInfo = updateInfo,
                onDismiss = { viewModel.dismissUpdateSheet() }
            )
        }
    }
}

/**
 * Preview composable for NavContainer in light and dark themes.
 */
@Preview(showBackground = true)
@Composable
fun NavContainerPreview() {
    ApkSignerTheme {
        Text("NavContainer Preview")
    }
}
