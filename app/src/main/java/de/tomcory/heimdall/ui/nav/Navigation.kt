package de.tomcory.heimdall.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import de.tomcory.heimdall.R
import de.tomcory.heimdall.ui.apps.AppsScreen
import de.tomcory.heimdall.ui.apps.PermissionsScreen
import de.tomcory.heimdall.ui.database.DatabaseScreen
import de.tomcory.heimdall.ui.telemetry.TelemetryScreen
import de.tomcory.heimdall.ui.traffic.TrafficScreen

/**
 * How to add a new screen to the bottom navigation bar:
 *
 * 1) add a new data object to the NavigationItem class. Define a route (select a unique string), icons and title.
 * 2) add the new navigation item to the navigationItems list below
 * 3) add a new composable to the Navigation NavHost below that maps your defined route to your screen.
 */
sealed class NavigationItem(var route: String, var unselectedIcon: Int, var selectedIcon: Int, var title: String) {
    data object Traffic : NavigationItem("traffic", R.drawable.ic_m3_traffic_24px, R.drawable.ic_m3_traffic_filled_24px, "Traffic")
    data object Apps : NavigationItem("apps", R.drawable.ic_m3_apps_24px, R.drawable.ic_m3_apps_24px, "Apps")
    data object Database : NavigationItem("database", R.drawable.ic_m3_database_24px, R.drawable.ic_m3_database_24px, "Database")
    data object Telemetry : NavigationItem("telemetry", R.drawable.ic_m3_database_24px, R.drawable.ic_m3_database_24px, "Telemetry")
}

/**
 * List of navigation items used by the BottomNavigationBar.
 */
val navigationItems = listOf(
    NavigationItem.Traffic,
    NavigationItem.Apps,
    NavigationItem.Database,
    NavigationItem.Telemetry,

)

@Composable
fun Navigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = NavigationItem.Traffic.route) {

        /*
         * Map navigation items for the BottomNavigationBar to their destination screen here.
         */
        composable(NavigationItem.Traffic.route) {
            TrafficScreen()
        }
        composable(NavigationItem.Apps.route) {
            AppsScreen(navController)
        }
        composable(NavigationItem.Database.route) {
            DatabaseScreen()
        }

        composable(NavigationItem.Telemetry.route) {
            TelemetryScreen()
        }


        /*
         * Add other destinations, e.g. nested screens, here.
         */
        composable("permissions/{packageName}") {
            PermissionsScreen(navController, it.arguments?.getString("packageName"))
        }
    }
}

