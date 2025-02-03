# TODO
## Replace CalDav4j with dav4jvm
## Add the concept of servers
- Currently, only chats (in discord, channels) and protocols (Discord, Messenger, etc.) exist
- Add the ability to register commands per-server
    - This also means using the most local version of a command, and allowing duplicates.
        - Easy way to implement this is to add some stuff into [getCommand]'s when statement
    - Add the ability to change the command delimiter per-protocol and per-server
        - Could be implemented similarly to above, by adding more steps to the command delimiter lookup rather than just
          a DefaultMap.

## New commands to be implemented
- Mimic: Runs a command as another user. Should be able to mimic any user from any chat linked to the current one.
- At: Says who went where. (It's the companion command to goingto)
