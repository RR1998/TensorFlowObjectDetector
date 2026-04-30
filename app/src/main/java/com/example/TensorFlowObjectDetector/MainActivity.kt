package com.example.TensorFlowObjectDetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.TensorFlowObjectDetector.navigator.AppNavHost
import com.example.TensorFlowObjectDetector.navigator.Screen
import com.example.TensorFlowObjectDetector.ui.theme.MyApplicationTheme
import com.example.TensorFlowObjectDetector.utils.navigationBarAssets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainActivityContent()
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MainActivityContent() {
    MyApplicationTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route.orEmpty()
        val items = listOf(
            Screen.Camera,
            Screen.Details,
            Screen.Chat
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                val title = when {
                    currentRoute.startsWith("details/") || currentRoute == Screen.Details.route ->
                        stringResource(R.string.screen_detail_result)

                    currentRoute == Screen.Chat.route -> "Chat"
                    else -> ""
                }
                if (currentRoute != Screen.Camera.route) {
                    TopAppBar(
                        title = {
                            Text(
                                title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        },
                        navigationIcon = {
                            if (currentRoute != Screen.Camera.route) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_arrow_back),
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            },
            bottomBar = {
                NavigationBar {
                    items.forEach { screen ->
                        val assets = navigationBarAssets(screen.route)
                        val isSelected = when (screen) {
                            Screen.Camera -> currentRoute == Screen.Camera.route
                            Screen.Details -> currentRoute.startsWith("details/")
                                    || currentRoute == Screen.Details.route

                            Screen.Chat -> currentRoute == Screen.Chat.route
                        }
                        NavigationBarItem(
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            icon = {
                                Icon(
                                    painter = painterResource(assets.first),
                                    contentDescription = screen.route,
                                )
                            },
                            label = { Text(assets.second) },
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                AppNavHost(
                    navController = navController,
                    startDestination = Screen.Camera.route
                )
            }
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainActivityPreview() {
    MainActivityContent()
}
