package com.spaceexplorer.presentation.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.spaceexplorer.presentation.ui.screens.DetailScreen
import com.spaceexplorer.presentation.ui.screens.FavoritesScreen
import com.spaceexplorer.presentation.ui.screens.HistoryScreen
import com.spaceexplorer.presentation.ui.screens.HomeScreen

internal object Routes {
    const val HOME = "home"
    const val FAVORITES = "favorites"
    const val HISTORY = "history"
    const val DETAIL = "detail/{date}"

    fun detail(date: String): String = "detail/${Uri.encode(date)}"
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToDetail = { date ->
                    navController.navigate(Routes.detail(date))
                },
                onNavigateToFavorites = {
                    navController.navigate(Routes.FAVORITES)
                },
                onNavigateToHistory = {
                    navController.navigate(Routes.HISTORY)
                }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                onNavigateToDetail = { date ->
                    navController.navigate(Routes.detail(date))
                },
                onNavigateUp = { navController.navigateUp() }
            )
        }

        composable(Routes.FAVORITES) {
            FavoritesScreen(
                onNavigateToDetail = { date ->
                    navController.navigate(Routes.detail(date))
                },
                onNavigateUp = { navController.navigateUp() }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date").orEmpty()
            DetailScreen(
                date = date,
                onNavigateUp = { navController.navigateUp() }
            )
        }
    }
}
