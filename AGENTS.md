# AGENTS.md

Guidance for AI agents (and humans) working in the Convergence Bot repo.

## What this is

Convergence Bot is a Kotlin chatbot designed to **bridge messages between
different chat protocols** (e.g. Discord ↔ Console) and to run a shared command
system across all of them. A message arriving on one protocol can be relayed to
linked chats on others, and commands work uniformly regardless of which protocol
they came from.

- Language: Kotlin 2.3.x on the JVM (Java toolchain 17).
- Entry point: `convergence.ConvergenceBot.main` (`src/main/kotlin/convergence/ConvergenceBot.kt`).
- Build: Gradle (Kotlin DSL), with the Gradle Shadow plugin for a fat jar and
  ANTLR for the command grammar.

## Layout

```
src/main/kotlin/convergence/
  ConvergenceBot.kt     Main entry point; registers protocols, loads settings, starts scheduler.
  Interfaces.kt         Core domain model: Protocol, Chat, Server, User, CommandScope,
                        and the capability interfaces (HasNicknames, HasReactions, CanMentionUsers, ...).
  BotState.kt           Holds mutable runtime state (linked chats, registered commands, etc.).
  Command.kt            Command / ArgumentSpec / CommandLike model.
  CommandRegistryService.kt  registerCommand / registerAlias / runCommand, getCommand.
  CommandParser.kt      Parses an incoming string into a CommandWithArgs (uses the ANTLR grammar).
                        Also contains CommandParserService (DI-injected wrapper around ANTLR).
  Command.g4            ANTLR4 grammar for the command syntax. Generated sources land in src/main/java/convergence.
  DefaultCommands.kt    Built-in commands (help, echo, scheduling, aliases, etc.).
  Scheduler.kt          SchedulerThread + ScheduledCommand/ScheduledTask — runs scheduled commands
                        and timed tasks (e.g. calendar sync, notifications).
  ScheduleCommands.kt   Commands related to scheduling and event management.
  Configuration.kt      Settings model (SettingsData) + load/save; convergencePath, settingsPath.
  Serialization.kt      Jackson (de)serializers for domain objects via stable string keys (toKey()).
  Callbacks.kt          Message/chat event callback machinery.
  MessagingService.kt   Message sending helpers.
  DI.kt                 Koin dependency injection module and getKoinService helper.
  Extensions.kt         Kotlin extension helpers (e.g. substringBetween).
  Logging.kt            defaultLogger / messageLogger.
  discord/              Discord protocol (JDA-based): Discord.kt, DiscordCommands.kt,
                        DiscordConfig.kt, FratHooks.kt,
                        frat/ (a specific Discord guild's roster/role features),
                        calendar/ (CalDAV sync): CalendarProcessorService, CalendarNotificationProcessorService.
  console/Console.kt    Console protocol — stdin/stdout, useful for local testing without Discord.
src/main/java/convergence/   ANTLR-generated lexer/parser/visitor (do not hand-edit).
src/test/kotlin/             Tests (kotlin.test + MockK).
```

## Core concepts

- **Protocol** (`Interfaces.kt`): abstract base each chat backend extends
  (`UniversalProtocol`, `ConsoleProtocol`, `DiscordProtocol`). Implements
  sending/receiving messages, listing chats/users, and resolving objects from
  string keys.
- **Capability interfaces**: optional features a protocol may implement —
  `HasNicknames`, `HasReactions`, `HasImages`, `CanMentionUsers`,
  `HasMessageHistory`, `HasRoles`, `HasServer`, etc. Code feature-detects with
  `if (protocol is HasNicknames)` rather than assuming capabilities.
- **CommandScope**: a `Chat` or `Server` — the scope a command/alias is bound to.
  Commands resolve in order: chat alias → server alias → protocol command →
  universal command (`getCommand` in `CommandRegistryService.kt`).
- **Serialization contract**: domain objects can't be serialized directly (they
  wrap live protocol state). Each exposes a stable `toKey()` string, and each
  protocol rebuilds objects from a key via `commandScopeFromKey()` / `userFromKey()`.
  Keys are self-describing — they start with the protocol name (e.g.
  `DiscordChat(...)`), which is how `scopeStrToProtocol()` finds the owner. When
  adding new persisted domain types, follow this pattern in `Serialization.kt`
  and register in `convergenceModule`; avoid handwritten DTO mirrors.

## Build / test / run

All commands use the Gradle wrapper (`./gradlew`). Java 17+ required.

```bash
./gradlew build              # compile + test
./gradlew test               # run tests only
./gradlew compileKotlin -q   # quick compile check
./gradlew shadowJar          # produce the fat jar in build/libs/convergence.bot-1.0-SNAPSHOT-all.jar
./gradlew copyBot            # build shadowJar and copy it to ~/.convergence/
./gradlew run                # run locally (stdin wired in, so the Console protocol works)
```

Note: `compileKotlin` depends on `generateGrammarSource`, so the ANTLR sources
are regenerated automatically — you don't need to run ANTLR by hand. After
editing `Command.g4`, just recompile.

### Runtime configuration (not in the repo)

The bot reads config from `convergencePath`, default `~/.convergence/`:

- `~/.convergence/discordToken` — Discord bot token (plain text). Without it,
  the Discord protocol logs an error and the bot can't connect to Discord.
- `~/.convergence/settings.json` — persisted settings (aliases, linked chats,
  scheduled commands, synced calendars, etc.). Created automatically if absent.
- `/opt/bots/config.json` and `/opt/bots/convergence/brotherInfo.json` — used
  only by the `discord/frat` features (hardcoded absolute paths in
  `FratConfig.kt`); harmless to ignore for general work.

Pass `-c/--convergence-path` to override the config directory.

For local development you can exercise the **Console protocol** without any
Discord token via `./gradlew run`.

### Deployment

`copyBot.sh` builds the shadow jar and `scp`s it to a remote host, then restarts
the `ConvergenceBot.service` systemd unit. This is environment-specific to the
maintainer's server — don't run it unless you are that maintainer.

## Conventions

- **Code style**: match the surrounding code. This repo uses a compact Kotlin
  style — `if(...)`, `catch(e: ...)`, `when(...)` with no space after the
  keyword. Expression bodies and trailing-lambda style are common.
- **detekt**: `detekt.yml` is present as config. There's currently no detekt
  Gradle plugin wired in, so it's run via the IDE/standalone if at all — don't
  assume `./gradlew detekt` exists.
- **Generated code**: anything in `src/main/java/convergence/` (CommandParser,
  CommandLexer, visitors, `.tokens`, `.interp`) is ANTLR output. Edit
  `Command.g4` instead and recompile.
- **Logging**: use `defaultLogger` / `messageLogger` (logback, see
  `src/main/resources/logback.xml`); don't add `println` outside the Console
  protocol.
- **Commits**: history uses short imperative subjects; commits authored by an
  agent in this repo are prefixed `CLAUDE:`.

## Gotchas

- The Shadow jar config in `build.gradle.kts` is deliberately non-default: it
  filters the classpath to real JARs because GraalVM's `js-community` is
  POM-only and breaks Shadow's default zip-everything behavior. Don't "simplify"
  that block without understanding the comment there.
- `commands` is a global map keyed by protocol; `aliases` keyed by scope. Adding
  a command means calling `registerCommand` (typically from a protocol's
  `init()` / `configLoaded()` or from `registerDefaultCommands`).
- Adding a new protocol: extend `Protocol`, implement the required abstract
  members plus any capability interfaces it supports, add it to the `protocols`
  list in `ConvergenceBot.main`, and ensure `toKey()`/`...fromKey()` round-trip
  for any persisted scopes/users.

## Key dependencies

JDA (Discord), Jackson (+ kotlin & jsr310 modules) for settings (de)serialization,
ANTLR4 (command grammar), Natty (natural-language date parsing), caldav4j/sardine
(CalDAV calendar sync), Apache POI (spreadsheets), GraalVM polyglot + graaljs
(JS scripting), graphviz-kotlin, argparse4j (CLI args), logback (logging),
Koin (dependency injection), MockK + kotlin.test (testing). Versions are
centralized in `gradle/libs.versions.toml`.
