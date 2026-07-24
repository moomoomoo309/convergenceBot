package convergence

import convergence.discord.calendar.CalendarNotificationProcessorService
import convergence.discord.calendar.CalendarProcessorService
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent

/**
 * Reified inline helper — allows [getKoinService]<SomeService>() instead of
 * the verbose KoinJavaComponent.get(SomeService::class.java).
 */
inline fun <reified T : Any> getKoinService(): T = KoinJavaComponent.get(T::class.java)

/**
 * Core module providing the fundamental application state and services.
 * The existing globals (bot, settings) are registered here so that new code
 * can use Koin injection while old code continues using the top-level vals.
 */
val coreModule = module {
    single { bot }
    single { settings }
    single { objectMapper }
    single { callbacks }
    single { MessagingService(get(), get()) }
    single { ScheduleCommands(get(), get(), get(), get(), get()) }
    single { CommandParserService(get(), get()) }
    single { CommandRegistryService(get(), get(), get(), get()) }
    single { CalendarNotificationProcessorService(get()) }
    single { CalendarProcessorService(get(), get(), get()) }
    single { SchedulerThread(get(), get(), get(), get()) }
}

/**
 * All application modules combined.
 */
val appModules = coreModule
