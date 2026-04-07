# Navigation in Jetpack Compose

> **Source:** [aldefy/compose-skill](https://github.com/aldefy/compose-skill) — MIT License

Reference: `androidx/navigation/navigation-compose/src/commonMain/kotlin/androidx/navigation/compose/`

## Setup

### Basic NavHost and NavController

```kotlin
val navController = rememberNavController()

NavHost(
    navController = navController,
    startDestination = "home"
) {
    composable<Home> {
        HomeScreen(onNavigate = { navController.navigate(Details()) })
    }
}
```

## Type-Safe Navigation (Navigation 2.8+)

Use `@Serializable` route classes instead of string routes.

```kotlin
@Serializable
data class Home(val userId: String? = null)

@Serializable
data class Details(val itemId: Int)

NavHost(navController, startDestination = Home()) {
    composable<Home> { backStackEntry ->
        val args = backStackEntry.toRoute<Home>()
        HomeScreen(userId = args.userId)
    }
    composable<Details> { backStackEntry ->
        val args = backStackEntry.toRoute<Details>()
        DetailsScreen(itemId = args.itemId)
    }
}
```

## Navigating

```kotlin
// Type-safe
navController.navigate(Details(itemId = 42))

// Pop back stack
navController.popBackStack()

// popUpTo — Clear back stack
navController.navigate(Details(itemId = 42), navOptions = navOptions {
    popUpTo(Home::class) { inclusive = false }
})
```

## Nested Navigation Graphs

```kotlin
navigation<FeatureRoot>(startDestination = FeatureHome()) {
    composable<FeatureHome> { FeatureHomeScreen() }
    composable<FeatureDetail> { FeatureDetailScreen() }
}
```

## Deep Links

```kotlin
composable<Details>(
    deepLinks = listOf(
        navDeepLink<Details>(uriPattern = "https://example.com/details/{itemId}")
    )
) { backStackEntry ->
    val args = backStackEntry.toRoute<Details>()
    DetailsScreen(itemId = args.itemId)
}
```

## Bottom Navigation Integration

```kotlin
Scaffold(
    bottomBar = {
        NavigationBar {
            NavigationBarItem(
                selected = selectedItem == "home",
                onClick = {
                    navController.navigate(Home()) {
                        popUpTo(Home::class) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                icon = { Icon(Icons.Default.Home, null) },
                label = { Text("Home") }
            )
        }
    }
) {
    NavHost(navController, startDestination = Home()) { /* ... */ }
}
```

## ViewModel Scoping with Navigation

```kotlin
composable<Details> { backStackEntry ->
    val viewModel: DetailsViewModel = hiltViewModel()
    DetailsScreen(viewModel = viewModel)
}
```

## Testing Navigation

```kotlin
@get:Rule val composeTestRule = createComposeRule()

@Test
fun navigateToDetails() {
    val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
    navController.navigatorProvider.addNavigator(ComposeNavigator())

    composeTestRule.setContent {
        NavHost(navController, startDestination = Home()) {
            composable<Home> { HomeScreen(onNavigate = { navController.navigate(Details()) }) }
            composable<Details> { DetailsScreen() }
        }
    }

    composeTestRule.onNodeWithTag("detail_button").performClick()
    assertEquals(Details::class.serializer().descriptor.serialName,
        navController.currentBackStackEntry?.destination?.route)
}
```

## Anti-Patterns

- ❌ **Don't** use string-based routes — use `@Serializable` route classes
- ❌ **Don't** create NavController in ViewModel — it lives in NavHost
- ❌ **Don't** navigate in composition — use `LaunchedEffect`
- ❌ **Don't** mix string and type-safe navigation approaches
