# TODO
## Add the concept of servers
- Currently, only chats (in discord, channels) and protocols (Discord, Messenger, etc.) exist
- Add the ability to register commands per-protocol and per-server
    - This also means using the most local version of a command, and allowing duplicates.
        - Easy way to implement this is to add some stuff into [getCommand]'s when statement
- Add the ability to change the command delimiter per-protocol and per-server
    - Could be implemented similarly to above, by adding more steps to the command delimiter lookup rather than just a DefaultMap.
- Lazy way to do this is to make a "server" a Chat object, which will mean not having to write extra code
    - This will mean most chats will need to have a property to get the server they come from (which may return itself if it's a server already)
    - It could also be a super class, which could make that simpler.
## Some kind of logging system?
- Should probably say the thread it came from.
- log and logErr methods exist for ease of switching the logging system.
## Serialization of Users, Chats, Aliases, ScheduledCommands
- Code written, needs testing.
## Discord Bot rich integration
- Make help command embed its contents?
- Make it actually handle emojis and formatted text correctly (might need some kind of parser?)
## Messenger Bot
- Implement all of the methods with a TODO. See Discord plugin for an example.
- <https://github.com/BotMill/fb-botmill> is a good looking library for FB Messenger bots
## Testing and bug fixing of existing commands
- Help definitely has a bug in it
## New commands to be implemented
- Mimic: Runs a command as another user.
- At: Says who went where. (It's the companion command to goingto)