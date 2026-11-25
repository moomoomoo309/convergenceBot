package convergence.command

import com.fasterxml.jackson.annotation.JsonIgnore
import convergence.Protocol
import convergence.ProtocolRef
import convergence.defaultLogger
import convergence.model.*
import convergence.replaceAliasVars

enum class ArgumentType {
    NUMBER,
    STRING,
    BOOLEAN,
    INTEGER
}

data class ArgumentSpec(val name: String, val type: ArgumentType, val optional: Boolean = false)

sealed class CommandLike(
    open val protocol: ProtocolRef,
    open val name: String,
): Comparable<CommandLike> {
    override fun compareTo(other: CommandLike) = "$protocol-$name".compareTo("${other.protocol}-${other.name}")
}

typealias CommandFunction = (List<String>, Chat<*>, User<*>) -> OutgoingMessage?

// A permission gate that runs before the command body: return a message to block the
// invocation with it, null to allow it. Declared as a fun interface (SAM) rather than a
// bare function typealias so the type is nominal — not interchangeable with any other
// four-argument function shape — and so lambdas convert directly at every call site.
fun interface PermissionFunction {
    fun check(command: Command, args: List<String>, chat: Chat<*>, sender: User<*>): OutgoingMessage?
}

data class Command(
    override val protocol: ProtocolRef,
    override val name: String,
    @JsonIgnore val argSpecs: List<ArgumentSpec>,
    @JsonIgnore val function: CommandFunction,
    @JsonIgnore val helpText: String,
    @JsonIgnore val syntaxText: String,
    @JsonIgnore val permissions: PermissionFunction
): CommandLike(protocol, name) {
    operator fun invoke(args: List<String>, chat: Chat<*>, sender: User<*>): OutgoingMessage? {
        val errorMessage = permissions.check(this, args, chat, sender)
        if (errorMessage != null) {
            return errorMessage
        }
        return replaceAliasVars(chat, sender, function(args, chat, sender))
    }

    constructor(
        protocol: ProtocolRef,
        name: String,
        argSpecs: List<ArgumentSpec>,
        function: () -> OutgoingMessage?,
        helpText: String,
        syntaxText: String,
        permissions: PermissionFunction = { _,_,_,_ -> null }
    ): this(protocol, name, argSpecs, { _, _, _ -> function() }, helpText, syntaxText, permissions)

    constructor(
        protocol: ProtocolRef,
        name: String,
        argSpecs: List<ArgumentSpec>,
        function: (args: List<String>) -> OutgoingMessage?,
        helpText: String,
        syntaxText: String,
        permissions: PermissionFunction = { _,_,_,_ -> null }
    ): this(protocol, name, argSpecs, { args: List<String>, _, _ -> function(args) }, helpText, syntaxText, permissions)

    constructor(
        protocol: ProtocolRef,
        name: String,
        argSpecs: List<ArgumentSpec>,
        function: (args: List<String>, chat: Chat<*>) -> OutgoingMessage?,
        helpText: String,
        syntaxText: String,
        permissions: PermissionFunction = { _,_,_,_ -> null }
    ): this(
        protocol,
        name,
        argSpecs,
        { args: List<String>, chat: Chat<*>, _ -> function(args, chat) },
        helpText,
        syntaxText,
        permissions
    )


    companion object {
        fun of(
            protocol: ProtocolRef,
            name: String,
            argSpecs: List<ArgumentSpec>,
            function: (args: List<String>, chat: Chat<*>, sender: User<*>) -> String?,
            helpText: String,
            syntaxText: String,
            permissions: PermissionFunction = { _,_,_,_ -> null }
        ) = Command(
            protocol,
            name,
            argSpecs,
            { args: List<String>, chat: Chat<*>, sender: User<*> ->
                function(args, chat, sender)?.let {
                    SimpleOutgoingMessage(
                        it
                    )
                }
            },
            helpText,
            syntaxText,
            permissions
        )

        fun of(
            protocol: ProtocolRef,
            name: String,
            argSpecs: List<ArgumentSpec>,
            function: (args: List<String>, chat: Chat<*>) -> String?,
            helpText: String,
            syntaxText: String,
            permissions: PermissionFunction = { _,_,_,_ -> null }
        ) = Command(
            protocol,
            name,
            argSpecs,
            { args: List<String>, chat: Chat<*> -> function(args, chat)?.let { SimpleOutgoingMessage(it) } },
            helpText,
            syntaxText,
            permissions
        )

        fun of(
            protocol: ProtocolRef,
            name: String,
            argSpecs: List<ArgumentSpec>,
            function: (args: List<String>) -> String?,
            helpText: String,
            syntaxText: String,
            permissions: PermissionFunction = { _,_,_,_ -> null }
        ) = Command(
            protocol,
            name,
            argSpecs,
            { args: List<String> -> function(args)?.let { SimpleOutgoingMessage(it) } },
            helpText,
            syntaxText,
            permissions
        )

        fun of(
            protocol: ProtocolRef,
            name: String,
            argSpecs: List<ArgumentSpec>,
            function: () -> String?,
            helpText: String,
            syntaxText: String,
            permissions: PermissionFunction = { _,_,_,_ -> null }
        ) = Command(
            protocol,
            name,
            argSpecs,
            { -> function()?.let { SimpleOutgoingMessage(it) } },
            helpText,
            syntaxText,
            permissions
        )
    }
}

data class Alias(
    val scope: CommandScope,
    override val name: String,
    val command: Command,
    val args: List<String>
): CommandLike(scope.protocol, name) {
    fun commandText() = "${command.name} ${args.joinToString(" ")}"
}

// ─── Typed command registration ───────────────────────────────────────────────
//
// commandOf mirrors Command.of but takes the protocol as the extension receiver, so the
// handler's chat/sender parameters are typed as the protocol's own C/U rather than the
// base Chat<*>/User<*>. The wrapper re-checks protocol identity before the (erased)
// cast: a command reached from a chat of some other protocol would otherwise throw
// ClassCastException deep inside the handler. A mismatch fails soft — warn, no output.
//
// Only for protocol-locked commands. Universal / protocol-agnostic commands must keep
// using Command.of with Chat<*> parameters: they are legitimately invoked with chats
// from every protocol (via the UniversalProtocol fallback in getCommand), which the
// identity guard would reject.

@Suppress("UNCHECKED_CAST")
fun <C : Chat<*>, U : User<*>> Protocol<C, U, *, *>.commandOf(
    name: String,
    argSpecs: List<ArgumentSpec>,
    function: (args: List<String>, chat: C, sender: U) -> String?,
    helpText: String,
    syntaxText: String,
    permissions: PermissionFunction = { _, _, _, _ -> null }
): Command {
    val proto = this
    return Command.of(
        proto, name, argSpecs,
        { args: List<String>, chat: Chat<*>, sender: User<*> ->
            if (chat.protocol !== proto || sender.protocol !== proto) {
                defaultLogger.warn("Command \"$name\" invoked outside ${proto.name}; ignoring.")
                null
            } else
                function(args, chat as C, sender as U)
        },
        helpText, syntaxText, permissions
    )
}

@Suppress("UNCHECKED_CAST")
fun <C : Chat<*>, U : User<*>> Protocol<C, U, *, *>.commandOf(
    name: String,
    argSpecs: List<ArgumentSpec>,
    function: (args: List<String>, chat: C) -> String?,
    helpText: String,
    syntaxText: String,
    permissions: PermissionFunction = { _, _, _, _ -> null }
): Command {
    val proto = this
    return Command.of(
        proto, name, argSpecs,
        { args: List<String>, chat: Chat<*> ->
            if (chat.protocol !== proto) {
                defaultLogger.warn("Command \"$name\" invoked outside ${proto.name}; ignoring.")
                null
            } else
                function(args, chat as C)
        },
        helpText, syntaxText, permissions
    )
}

fun <C : Chat<*>, U : User<*>> Protocol<C, U, *, *>.commandOf(
    name: String,
    argSpecs: List<ArgumentSpec>,
    function: (args: List<String>) -> String?,
    helpText: String,
    syntaxText: String,
    permissions: PermissionFunction = { _, _, _, _ -> null }
): Command = Command.of(this, name, argSpecs, function, helpText, syntaxText, permissions)

fun <C : Chat<*>, U : User<*>> Protocol<C, U, *, *>.commandOf(
    name: String,
    argSpecs: List<ArgumentSpec>,
    function: () -> String?,
    helpText: String,
    syntaxText: String,
    permissions: PermissionFunction = { _, _, _, _ -> null }
): Command = Command.of(this, name, argSpecs, function, helpText, syntaxText, permissions)

// ─── Typed permission registration ────────────────────────────────────────────
//
// permissionOf mirrors commandOf: the receiver protocol fixes [C]/[U] so the check
// receives its own chat/sender types instead of the base Chat<*>/User<*>, and the
// wrapper re-checks protocol identity before the (erased) cast.
//
// The failure mode deliberately differs from commandOf. A command that can't verify its
// chat simply produces no output; a permission that can't verify its chat must DENY,
// because returning null from a permission means "allowed" — failing open would silently
// skip the check.
//
// Same restriction as commandOf: only for protocol-locked permissions. Universal /
// protocol-agnostic commands are invoked with chats from every protocol (via the
// UniversalProtocol fallback in getCommand) and must keep a plain PermissionFunction
// lambda over Chat<*>.

@Suppress("UNCHECKED_CAST")
fun <C : Chat<*>, U : User<*>> Protocol<C, U, *, *>.permissionOf(
    function: (command: Command, args: List<String>, chat: C, sender: U) -> OutgoingMessage?
): PermissionFunction {
    val proto = this
    return PermissionFunction { command, args, chat, sender ->
        if (chat.protocol !== proto || sender.protocol !== proto) {
            defaultLogger.warn("Permission check for \"${command.name}\" invoked outside ${proto.name}; denying.")
            SimpleOutgoingMessage("This command can't be run here.")
        } else
            function(command, args, chat as C, sender as U)
    }
}
