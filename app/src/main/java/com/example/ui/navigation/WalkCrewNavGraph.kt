package com.example.ui.navigation

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.auth.AuthUiState
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.ProfileSetupScreen
import com.example.ui.auth.WelcomeSignInScreen
import com.example.ui.components.FloatingPillNavBar
import com.example.ui.components.NavigationTab
import com.example.ui.crews.CrewDetailScreen
import com.example.ui.crews.CrewDetailViewModel
import com.example.ui.crews.CrewViewModel
import com.example.ui.crews.CrewsScreen
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.profile.ProfileScreen
import com.example.ui.theme.DeepForest
import com.example.ui.theme.LimeAccent

object WalkCrewDestinations {
    const val HOME = "home"
    const val CREWS = "crews"
    const val PROFILE = "profile"
    const val CREW_DETAIL = "crew_detail/{crewId}"
    const val PAYWALL = "paywall"
}

@Composable
fun WalkCrewApp(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
    crewViewModel: CrewViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()

    when (val state = authState) {
        is AuthUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepForest),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = LimeAccent,
                    strokeWidth = 3.dp
                )
            }
        }

        is AuthUiState.Unauthenticated -> {
            WelcomeSignInScreen(viewModel = authViewModel)
        }

        is AuthUiState.NeedsProfileSetup -> {
            ProfileSetupScreen(
                initialName = state.defaultName,
                viewModel = authViewModel
            )
        }

        is AuthUiState.Authenticated -> {
            MainAppContent(
                navController = navController,
                homeViewModel = homeViewModel,
                crewViewModel = crewViewModel,
                onSignOut = { authViewModel.signOut() }
            )
        }
    }
}

@Composable
private fun MainAppContent(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    crewViewModel: CrewViewModel,
    onSignOut: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: WalkCrewDestinations.HOME

    val currentTab = when (currentRoute) {
        WalkCrewDestinations.CREWS -> NavigationTab.CREWS
        WalkCrewDestinations.PROFILE -> NavigationTab.PROFILE
        else -> NavigationTab.HOME
    }

    val isTopLevelDestination = currentRoute in listOf(
        WalkCrewDestinations.HOME,
        WalkCrewDestinations.CREWS,
        WalkCrewDestinations.PROFILE
    )

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = WalkCrewDestinations.HOME,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(WalkCrewDestinations.HOME) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToCrews = {
                        navController.navigate(WalkCrewDestinations.CREWS) {
                            popUpTo(WalkCrewDestinations.HOME)
                            launchSingleTop = true
                        }
                    },
                    onNavigateToProfile = {
                        navController.navigate(WalkCrewDestinations.PROFILE) {
                            popUpTo(WalkCrewDestinations.HOME)
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(WalkCrewDestinations.CREWS) {
                CrewsScreen(
                    viewModel = crewViewModel,
                    onCrewClick = { crewId ->
                        navController.navigate("crew_detail/$crewId")
                    }
                )
            }

            composable(
                route = WalkCrewDestinations.CREW_DETAIL,
                arguments = listOf(navArgument("crewId") { type = NavType.StringType })
            ) { backStackEntry ->
                val crewId = backStackEntry.arguments?.getString("crewId") ?: ""
                val context = LocalContext.current
                val detailViewModel: CrewDetailViewModel = viewModel(
                    key = "crew_detail_$crewId",
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return CrewDetailViewModel(context.applicationContext as Application, crewId) as T
                        }
                    }
                )
                CrewDetailScreen(
                    viewModel = detailViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(WalkCrewDestinations.PROFILE) {
                ProfileScreen(
                    viewModel = homeViewModel,
                    onSignOut = onSignOut,
                    onNavigateToPaywall = {
                        navController.navigate(WalkCrewDestinations.PAYWALL)
                    }
                )
            }

            composable(WalkCrewDestinations.PAYWALL) {
                com.example.ui.paywall.PaywallScreen(
                    onDismiss = { navController.popBackStack() }
                )
            }
        }

        // Floating Pill Navigation Bar (shown on top-level tabs)
        if (isTopLevelDestination) {
            FloatingPillNavBar(
                selectedTab = currentTab,
                onTabSelected = { tab ->
                    val targetRoute = when (tab) {
                        NavigationTab.HOME -> WalkCrewDestinations.HOME
                        NavigationTab.CREWS -> WalkCrewDestinations.CREWS
                        NavigationTab.PROFILE -> WalkCrewDestinations.PROFILE
                    }
                    if (currentRoute != targetRoute) {
                        navController.navigate(targetRoute) {
                            popUpTo(WalkCrewDestinations.HOME) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
