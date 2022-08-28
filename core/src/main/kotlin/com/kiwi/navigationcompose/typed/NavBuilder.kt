package com.kiwi.navigationcompose.typed

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.kiwi.navigationcompose.typed.internal.UriBundleDecoder
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

/**
 * Add the Composable to the NavGraphBuilder with type-safe arguments and route.
 *
 * Pass the unique destination as generic argument T. This function is inlined, you can provide the
 * destination serialization manually using the function variant with serializer argument.
 *
 * Arguments (a Destination instance) are available as a composable lambda's receiver. You can read those
 * values directly in the lambda.
 *
 * ```
 * sealed interface Destinations : Destination {
 *      @Serializable object Home : Destinations
 *      @Serializable data class Article(val id: Int) : Destinations
 * }
 *
 * NavGraph(...) {
 *     composable<Destinations.Home> { Home() }
 *     composable<Destinations.Article> { Article(id) }
 * }
 * ```
 */
@ExperimentalSerializationApi
public inline fun <reified T : Destination> NavGraphBuilder.composable(
	deepLinks: List<NavDeepLink> = emptyList(),
	noinline content: @Composable T.(NavBackStackEntry) -> Unit,
) {
	composable(
		serializer = serializer(),
		deepLinks = deepLinks,
		content = content,
	)
}

/**
 * Add the Composable to the NavGraphBuilder with type-safe arguments and route.
 *
 * Pass the unique destination's serializer as an argument. This is a semi-internal implementation,
 * prefer using the generic function variant.
 */
@ExperimentalSerializationApi
public fun <T : Destination> NavGraphBuilder.composable(
	serializer: KSerializer<T>,
	deepLinks: List<NavDeepLink> = emptyList(),
	content: @Composable T.(NavBackStackEntry) -> Unit,
) {
	composable(
		route = createRoutePattern(serializer),
		arguments = createNavArguments(serializer),
		deepLinks = deepLinks,
	) { navBackStackEntry ->
		decodeArguments(serializer, navBackStackEntry).content(navBackStackEntry)
	}
}

/**
 * Add the Dialog to the NavGraphBuilder with type-safe arguments and route.
 *
 * Pass the unique destination as generic argument T. This function is inlined, you can provide the
 * destination serialization manually using the function variant with serializer argument.
 *
 * Arguments (a Destination instance) are available as a composable lambda's receiver. You can read those
 * values directly in the lambda.
 *
 * ```
 * sealed interface Destinations : Destination {
 *      @Serializable data class DeleteArticleConfirmation(val id: Int) : Destinations
 * }
 *
 * NavGraph(...) {
 *     dialog<Destinations.DeleteArticleConfirmation> { DeleteArticleConfirmation(id) }
 * }
 * ```
 */
@ExperimentalSerializationApi
public inline fun <reified T : Destination> NavGraphBuilder.dialog(
	deepLinks: List<NavDeepLink> = emptyList(),
	noinline content: @Composable T.(NavBackStackEntry) -> Unit,
) {
	dialog(
		serializer = serializer(),
		deepLinks = deepLinks,
		content = content,
	)
}

/**
 * Add the Dialog to the NavGraphBuilder with type-safe arguments and route.
 *
 * Pass the unique destination's serializer as an argument. This is a semi-internal implementation,
 * prefer using the generic function variant.
 */
@ExperimentalSerializationApi
public fun <T : Destination> NavGraphBuilder.dialog(
	serializer: KSerializer<T>,
	deepLinks: List<NavDeepLink> = emptyList(),
	content: @Composable T.(NavBackStackEntry) -> Unit,
) {
	dialog(
		route = createRoutePattern(serializer),
		arguments = createNavArguments(serializer),
		deepLinks = deepLinks,
	) { navBackStackEntry ->
		decodeArguments(serializer, navBackStackEntry).content(navBackStackEntry)
	}
}

/**
 * Construct a nested NavGraph with type-safe arguments and route.
 *
 * Pass the unique destination as generic argument T. This function is inlined, you can provide the
 * destination serialization manually using the function variant with serializer argument.
 *
 * The start destination is passed as a RoutePattern obtained from a relevant Destination instance.
 *
 * ```
 * sealed interface LoginDestinations : Destination {
 *     @Serializable object Home : LoginDestinations
 *     @Serializable object PasswordReset : LoginDestinations
 * }
 *
 * inline fun <reified T : Destination> loginNavigation(navController: NavController) {
 *     navigation<T>(
 *         startDestination = createRoutePattern<LoginDestinations.Home>(),
 *     ) {
 *         composable<LoginDestinations.Home> { LoginHome() }
 *         composable<LoginDestinations.PasswordReset> { PasswordReset() }
 *     }
 * }
 * ```
 */
@ExperimentalSerializationApi
public inline fun <reified T : Destination> NavGraphBuilder.navigation(
	startDestination: String,
	deepLinks: List<NavDeepLink> = emptyList(),
	noinline builder: NavGraphBuilder.() -> Unit,
) {
	navigation(
		serializer = serializer<T>(),
		startDestination = startDestination,
		deepLinks = deepLinks,
		builder = builder,
	)
}

/**
 * Construct a nested NavGraph with type-safe arguments and route.
 *
 * Pass the unique destination's serializer as an argument. This is a semi-internal implementation,
 * prefer using the generic function variant.
 *
 * The start destination is passed as a RoutePattern obtained from a relevant Destination instance.
 */
@ExperimentalSerializationApi
public fun <T : Destination> NavGraphBuilder.navigation(
	serializer: KSerializer<T>,
	startDestination: String,
	deepLinks: List<NavDeepLink> = emptyList(),
	builder: NavGraphBuilder.() -> Unit,
) {
	navigation(
		startDestination = startDestination,
		route = createRoutePattern(serializer),
		arguments = createNavArguments(serializer),
		deepLinks = deepLinks,
		builder = builder,
	)
}

/**
 * Creates navigation arguments definition based on the Destination type.
 */
@ExperimentalSerializationApi
public fun createNavArguments(serializer: KSerializer<*>): List<NamedNavArgument> =
	List(serializer.descriptor.elementsCount) { i ->
		val name = serializer.descriptor.getElementName(i)
		val descriptor = serializer.descriptor.getElementDescriptor(i)
		navArgument(name) {
			// Use StringType for all types to support nullability for all of them.
			type = NavType.StringType
			// Null is modelled as missing uri parameter, so it has to be optional as well.
			val isOptional = serializer.descriptor.isElementOptional(i) || descriptor.isNullable
			nullable = isOptional
			// If something is optional, default value is required.
			if (isOptional) {
				defaultValue = null
			}
		}
	}

/**
 * Decodes arguments from a Bundle in NavBackStackEntry instance.
 */
@ExperimentalSerializationApi
public fun <T : Destination> decodeArguments(
	serializer: KSerializer<T>,
	navBackStackEntry: NavBackStackEntry,
): T {
	// Arguments may be empty if the destination does not have any parameters
	// and it is a start destination.
	val decoder = UriBundleDecoder(navBackStackEntry.arguments ?: Bundle())
	return decoder.decodeSerializableValue(serializer)
}
