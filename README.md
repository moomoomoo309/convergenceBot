# Convergence Bot

Convergence Bot is a Kotlin chat bot that **bridges messages between different chat
protocols** and runs a single, shared command system across all of them. A message
arriving on one protocol (e.g. Discord) can be relayed to linked chats on others
(e.g. a local Console session), and commands behave identically no matter which
protocol they came from.

- **Language:** Kotlin 2.3.x on the JVM (Java toolchain 17)
- **Entry point:** `convergence.ConvergenceBot.main` (`src/main/kotlin/convergence/ConvergenceBot.kt`)
- **Build:** Gradle (Kotlin DSL) with the Shadow plugin (fat jar) and ANTLR (command grammar)

## Features

- **Cross-protocol bridging** — link chats across protocols so messages relay between them.
- **Unified command system** — commands and aliases resolve uniformly across every protocol,
  scoped per-chat or per-server.
- **Pluggable protocols** — Discord (via JDA) and a Console protocol for local testing ship
  in-tree; new protocols extend a common `Protocol` base.
- **Capability detection** — protocols opt into features (nicknames, reactions, images,
  mentions, message history, roles, …) via interfaces, so the core never assumes a backend
  can do something it can't.
- **Scheduled & timed commands**, **alias variables** (`%sender`, `%nick`, `%botname`,
  `%chatname`), **CalDAV calendar sync**, and more.

## Requirements

- JDK 17 or newer
- A Discord bot token (only if you want the Discord protocol; the Console protocol needs nothing)

## Build, test, run

All commands use the Gradle wrapper:

```bash
./gradlew build              # compile + test
./gradlew test               # run tests only
./gradlew run                # run locally with stdin wired in (Console protocol works)
./gradlew shadowJar          # build the fat jar -> build/libs/convergence.bot-1.0-SNAPSHOT-all.jar
./gradlew copyBot            # build the fat jar and copy it to ~/.convergence/
```

The ANTLR command grammar (`Command.g4`) is regenerated automatically as part of
`compileKotlin` — there's no need to run ANTLR by hand. After editing the grammar,
just recompile.

To run the standalone jar:

```bash
java -jar build/libs/convergence.bot-1.0-SNAPSHOT-all.jar
```

## Configuration

The bot reads its config from `~/.convergence/` by default (override with
`-c` / `--convergence-path`):

| File | Purpose |
| --- | --- |
| `~/.convergence/discordToken` | Discord bot token (plain text). Required for the Discord protocol. |
| `~/.convergence/settings.json` | Persisted settings — aliases, linked chats, scheduled commands, synced calendars, etc. Created automatically if absent. |

For local development you can exercise the **Console protocol** without any Discord
token via `./gradlew run`.

> Some features under `discord/frat/` read hardcoded paths (`/opt/bots/...`) specific
> to the maintainer's deployment; they're harmless to ignore for general use.

## Project layout

```
src/main/kotlin/convergence/
  ConvergenceBot.kt     Main entry point: registers protocols, loads settings, starts the scheduler.
  Interfaces.kt         Core domain model (Protocol, Chat, Server, User, CommandScope) + capability interfaces.
  Command.kt            Command / ArgumentSpec / Alias model.
  CommandRegistry.kt    registerCommand / registerAlias / runCommand.
  CommandParser.kt      Parses incoming strings into commands (uses the ANTLR grammar).
  Command.g4            ANTLR4 grammar for the command syntax.
  DefaultCommands.kt    Built-in commands (help, echo, scheduling, aliases, ...).
  CommandScheduler.kt   Persisted scheduled / timed commands.
  Configuration.kt      Settings model + load/save.
  Serialization.kt      Jackson (de)serializers for domain objects via stable string keys.
  discord/              Discord protocol (JDA): commands, frat roster/roles, CalDAV calendar sync.
  console/Console.kt    Console protocol — stdin/stdout, for local testing.
src/main/java/convergence/   ANTLR-generated lexer/parser/visitor (do not hand-edit).
src/test/kotlin/             Tests (kotlin.test + MockK).
```

## Core concepts

- **Protocol** — abstract base each chat backend extends; handles sending/receiving messages,
  listing chats/users, and rebuilding objects from string keys.
- **Capability interfaces** — optional features (`HasNicknames`, `HasReactions`, `HasImages`,
  `CanMentionUsers`, `HasMessageHistory`, `HasRoles`, `HasServer`, …). Code feature-detects
  with `if (protocol is HasNicknames)` rather than assuming.
- **CommandScope** — a `Chat` or `Server` that a command/alias is bound to. Commands resolve
  in order: chat alias → server alias → protocol command → universal command.
- **Serialization contract** — domain objects wrap live protocol state and can't be serialized
  directly, so each exposes a stable, self-describing `toKey()` string (it starts with the
  protocol name), and each protocol rebuilds objects from a key via `commandScopeFromKey()` /
  `userFromKey()`. See `Serialization.kt`.

## Adding a new protocol

1. Extend `Protocol` and implement its abstract members.
2. Implement any capability interfaces the backend supports.
3. Add the protocol to the `protocols` list in `ConvergenceBot.main`.
4. Make sure `toKey()` / `...fromKey()` round-trip for any persisted scopes and users.

## Contributing

See [AGENTS.md](AGENTS.md) for detailed conventions, gotchas, and guidance (it's written for
both AI agents and humans). In short: match the surrounding compact Kotlin style, edit
`Command.g4` rather than the generated Java, and use `defaultLogger` / `messageLogger` instead
of `println`.

## Key dependencies

JDA (Discord) · Jackson (settings serialization) · ANTLR4 (command grammar) · Natty
(natural-language dates) · caldav4j / sardine (CalDAV sync) · Apache POI (spreadsheets) ·
GraalVM polyglot + graaljs (JS scripting) · argparse4j (CLI args) · logback (logging) ·
MockK + kotlin.test (testing). Versions are centralized in `gradle/libs.versions.toml`.
