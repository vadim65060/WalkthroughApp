package com.example.walkthrough.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.walkthrough.presentation.screens.addhouse.AddHouseScreen
import com.example.walkthrough.presentation.screens.apartments.ApartmentsListScreen
import com.example.walkthrough.presentation.screens.export.ExportScreen
import com.example.walkthrough.presentation.screens.homes.HomesScreen
import com.example.walkthrough.presentation.screens.walkthrough.WalkthroughScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "homes"
    ) {
        composable("homes") {
            HomesScreen(navController = navController)
        }

        composable("add_house") {
            AddHouseScreen(navController = navController)
        }

        composable(
            "walkthrough/{houseId}",
            arguments = listOf(navArgument("houseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val houseId = backStackEntry.arguments?.getLong("houseId") ?: 0L
            WalkthroughScreen(
                navController = navController,
                houseId = houseId
            )
        }

        composable(
            "apartments/{houseId}",
            arguments = listOf(navArgument("houseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val houseId = backStackEntry.arguments?.getLong("houseId") ?: 0L
            ApartmentsListScreen(
                navController = navController,
                houseId = houseId
            )
        }

        composable("export") {
            ExportScreen(navController = navController)
        }
    }
}