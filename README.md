# Convergence Bot

Convergence Bot is a multi-protocol chat bridge written in Kotlin that connects different communication platforms. It enables commands and messages to flow between Discord, console, and other protocols, with built-in support for calendar synchronization, image archiving, and fraternal organization management.

## Features

- **Multi-protocol bridging** - Connect Discord servers with console and other protocols
- **Customizable commands** - Configurable command delimiters per chat (default: `!`)
- **Alias system** - Create custom command shortcuts at chat or server scope
- **Scheduled events** - Schedule commands to run at specific times
- **Chat linking** - Link chats across protocols for message forwarding
- **Image handling** - Upload images to WebDAV/Nextcloud servers automatically
- **Calendar sync** - Sync CalDAV calendars to Discord scheduled events with automatic updates
- **Reaction forwarding** - Forward messages to channels based on reaction count thresholds
- **Timers** - Create and track named timers
- **Rich formatting** - Support for bold, italic, code, spoilers, and more
- **Fraternal organization support** - Roster management, brother lookups, and lineage tracking

## Supported Protocols

- **Discord** - Full-featured Discord integration via JDA
- **Console** - Terminal interface for local testing and administration
- **Universal** - Meta-protocol for cross-protocol commands

## Prerequisites

- Java 17 or higher
- Gradle (wrapper included)

## Building

```bash
# Build and install to ~/.convergence/
gradle copyBot

# Or just build the shadow JAR
gradle shadowJar
```

## Running

```bash
# Run directly with Gradle
gradle run

# Or run the installed JAR
java -jar ~/.convergence/convergence.bot-1.0-SNAPSHOT-all.jar
```

### Command-line Options

- `-c, --convergence-path <path>` - Set the configuration directory (default: `~/.convergence/`)

## Configuration

On first run, the bot creates a `~/.convergence/` directory with:

- `settings.json` - Bot configuration (aliases, linked chats, scheduled events, calendar syncs)
- `discordToken` - Discord bot token (required for Discord protocol)

### Discord Setup

1. Create a Discord bot at [Discord Developer Portal](https://discord.com/developers/applications)
2. Place the bot token in `~/.convergence/discordToken`
3. Invite the bot to your server with appropriate permissions

### Fraternal Organization Configuration

For organizations using the roster features, a config file at `/opt/bots/config.json` provides:

- Roster spreadsheet URL (Excel source for brother data)
- Nextcloud/WebDAV credentials for image uploads
- Discord channel URLs for agendas, minutes, and attendance
- House duty schedule paths

## Commands

### Universal Commands

| Command                                        | Description                           |
|------------------------------------------------|---------------------------------------|
| `!help [command\|page]`                        | Get help on commands                  |
| `!echo <message>`                              | Repeat a message                      |
| `!ping`                                        | Check bot responsiveness              |
| `!me <action>`                                 | Perform an action (*username action*) |
| `!alias <name> <command>`                      | Create a chat-scoped alias            |
| `!serverAlias <name> <command>`                | Create a server-scoped alias          |
| `!removeAlias <name>`                          | Remove an alias                       |
| `!chats`                                       | List all known chats                  |
| `!commands`                                    | List available commands               |
| `!aliases`                                     | List active aliases                   |
| `!link <id>`                                   | Link another chat to this one         |
| `!unlink <id>`                                 | Remove a chat link                    |
| `!links`                                       | Show linked chats                     |
| `!setdelimiter <char>`                         | Change the command delimiter          |
| `!goingto <location> [for duration] [at time]` | Announce you're going somewhere       |
| `!schedule <time> <command>`                   | Schedule a command                    |
| `!unschedule <id>`                             | Cancel a scheduled command            |
| `!events`                                      | Show your scheduled events            |
| `!allevents`                                   | Show all scheduled events             |
| `!createTimer <name>`                          | Create a named timer                  |
| `!resetTimer <name>`                           | Reset a timer and see elapsed time    |
| `!checkTimer <name>`                           | Check timer duration                  |
| `!target <message> <user>`                     | Target a user for alias variables     |
| `!targetnick <message> <user>`                 | Target using nickname                 |
| `!exit`                                        | Shut down the bot                     |

### Discord-Specific Commands

| Command                                     | Description                                 |
|---------------------------------------------|---------------------------------------------|
| `!syncCalendar <url>`                       | Sync a CalDAV calendar to Discord events    |
| `!resyncCalendar`                           | Resync all calendars                        |
| `!unsyncCalendar <url>`                     | Remove a synced calendar                    |
| `!syncedCalendars`                          | List synced calendars                       |
| `!uploadImagesTo <url>`                     | Auto-upload images to WebDAV/Nextcloud      |
| `!stopUploadingImages`                      | Stop image uploads                          |
| `!registerReactChannel <emoji> <threshold>` | Forward messages by reaction count          |
| `!removeReactChannel`                       | Stop reaction forwarding                    |
| `!reactChannels`                            | List reaction forwarding rules              |
| `!brotherbyname <name>`                     | Look up brother by first/last name          |
| `!brotherbyroster <number>`                 | Look up brother by roster number            |
| `!brotherbynickname <nickname>`             | Look up brother by nickname                 |
| `!brothergetline <name>`                    | Get brother's lineage (big brother chain)   |
| `!updateRoster`                             | Refresh roster data from source spreadsheet |

## License

MIT License - see [LICENSE](LICENSE) for details.
