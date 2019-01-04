# Command Registration
- Protocols need to register themselves with the Command system **DONE**
    - ~~In doing so, they set the Command delimiter (is it !help or |help or what?)~~
        - The Command delimiter should be set per chat.
    - When they do that, it'll give them their instance (or _the_ instance, if you want it to be a singleton) of the Command system.
    - Through this, they can register their own commands.
    - registerProtocol(baseInterface, CommandDelimiter) -> commandHandler
- Command registration **DONE**
    - registerCommand(name, function, helpText, syntaxText, protocol (optional))
        - ~~Will call registerUniversalCommand if the protocol is not specified.~~ Replaced with UniversalChat
    - ~~registerUniversalCommand(name, function, helpText, syntaxText)~~ Replaced with UniversalChat
        - ~~Will be static, since it can be called without registering through a protocol, or will be accessible through a universal registration object.~~ Replaced with UniversalChat
    - ~~Conflicts need to be dealt with, but commands limited by protocol shouldn't conflict with commands limited by a different protocol.~~ Replaced with UniversalChat
        - ~~You basically need two sets of Command registries to handle this, one for universal ones, one for each protocol's.~~ Replaced with UniversalChat
    - Argument parsing needs to be handled **DONE**
        - ~~There is most likely a class built in for this. If not, look at how OpenJDK does Command line arguments.~~
- Aliases **DONE**
    - ~~An alias is a Command which runs other commands.~~ **DONE**
        - Ex: "!alias !hi !echo hi" will run "!echo hi" when you put in "!hi".
            - This doesn't have to be the syntax, it's just an example.
            - Then, if you run "!hi mom", it'll run "!echo hi mom".
        - ~~If you _really_ want to go crazy, a shell-type language can be made for this, but that's not necessary.~~
            - This can be emulated through other commands.
    - ~~Aliases have to be designed per-_chat_.~~ **DONE**
        - registerAlias(Protocol, chat, name, commandToRun, helpText, syntaxText)


