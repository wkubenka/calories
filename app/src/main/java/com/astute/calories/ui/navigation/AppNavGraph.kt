package com.astute.calories.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.astute.calories.ui.home.HomeScreen
import com.astute.calories.ui.meals.SavedMealsScreen
import com.astute.calories.ui.scanner.BarcodeScannerScreen
import com.astute.calories.ui.search.FoodSearchScreen
import com.astute.calories.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val FOOD_SEARCH = "food_search"
    const val BARCODE_SCANNER = "barcode_scanner"
    const val SAVED_MEALS = "saved_meals"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToSearch = { navController.navigate(Routes.FOOD_SEARCH) },
                onNavigateToScanner = { navController.navigate(Routes.BARCODE_SCANNER) },
                onNavigateToSavedMeals = { navController.navigate(Routes.SAVED_MEALS) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.FOOD_SEARCH) {
            FoodSearchScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.BARCODE_SCANNER) {
            BarcodeScannerScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.SAVED_MEALS) {
            SavedMealsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
