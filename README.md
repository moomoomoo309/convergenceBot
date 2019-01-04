# Convergence Bot
Convergence bot is a bot written in Kotlin designed to bridge the gap
between communication in different chat protocols.

Currently implemented protocols:
- None (console interface is implemented for testing)

Currently planned protocols:
- Facebook Messenger
- Discord

Protocols will be implemented as plugins for the bot, thus allowing for
additional protocols to be added. Command registration will allow for
commands to be specific to a protocol, or generic across all protocols.

The bot supports a variety of commands, and aliases to make custom ones.

## Building
### Core
- `gradle jar` will build the core project as a jar and install it.

### Plugins
- `gradle jar` on the given plugin will build the plugin as a jar and install it.

This will likely be changed in the future so all of the plugins bundled with the bot can be built at once.

