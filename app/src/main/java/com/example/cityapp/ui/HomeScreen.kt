import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cityapp.ui.ChatboxScreen
import com.example.cityapp.ui.CityScreen
import com.example.cityapp.ui.MapScreen
import com.example.cityapp.ui.ProfileScreen
import com.example.cityapp.ui.UnreadMessagesViewModel
import com.google.firebase.auth.FirebaseAuth


data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)

@Composable
fun HomeScreen(
    navController: NavController,
    auth: FirebaseAuth,
    unreadViewModel: UnreadMessagesViewModel = viewModel()
) {
    val hasUnreadMessages by unreadViewModel.hasUnreadMessages.collectAsState()

    val items = listOf(
        BottomNavItem(
            label = "Steden",
            selectedIcon = Icons.Filled.Apartment,
            unselectedIcon = Icons.Outlined.Apartment,
            route = "cities"
        ),
        BottomNavItem(
            label = "Kaart",
            selectedIcon = Icons.Filled.Map,
            unselectedIcon = Icons.Outlined.Map,
            route = "map"
        ),
        BottomNavItem(
            label = "Inbox",
            selectedIcon = Icons.Filled.ChatBubbleOutline,
            unselectedIcon = Icons.Outlined.ChatBubbleOutline,
            route = "inbox"
        ),
        BottomNavItem(
            label = "Profiel",
            selectedIcon = Icons.Filled.AccountCircle,
            unselectedIcon = Icons.Outlined.AccountCircle,
            route = "profile"
        )
    )

    var selectedItemIndex by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar (
                containerColor = androidx.compose.ui.graphics.Color.White
            ){
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedItemIndex == index,
                        onClick = { selectedItemIndex = index },
                        label = { Text(item.label) },
                        icon = {
                            if (item.route == "inbox") {
                                BadgedBox(
                                    badge = {
                                        if (hasUnreadMessages) {

                                            Badge(

                                                containerColor = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                                    .offset(x = 4.dp, y = (-4).dp)
                                            )

                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selectedItemIndex == index) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (selectedItemIndex == index) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedItemIndex) {
            0 -> CityScreen(modifier = Modifier.padding(innerPadding))
            1 -> MapScreen(modifier = Modifier.padding(innerPadding))
            2 -> ChatboxScreen(modifier = Modifier.padding(innerPadding))
            3 -> ProfileScreen(navController = navController, auth = auth, modifier = Modifier.padding(innerPadding))
        }
    }
}



