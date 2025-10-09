package convergence

import com.fasterxml.jackson.annotation.JsonIgnore


enum class ArgumentType {
    NUMBER,
    STRING,
    BOOLEAN,
    INTEGER
}

data class ArgumentSpec(val name: String, val type: ArgumentType, val optional: Boolean = false)

sealed class CommandLike(
    open val protocol: Protocol,
    open val name: String,
): Comparable<CommandLike> {
    override fun compareTo(other: CommandLike) = "$protocol-$name".compareTo("${other.protocol}-${other.name}")
}

data class Command(
    override val protocol: Protocol,
    override val name: String,
    @JsonIgnore val argSpecs: List<ArgumentSpec>,
    @JsonIgnore val function: (List<String>, Chat, User) -> OutgoingMessage?,
    @JsonIgnore val helpText: String,
    @JsonIgnore val syntaxText: String
): CommandLike(protocol, name) {
    constructor(
        protocol: Protocol,
        name: String,
        argSpecs: List<ArgumentSpec>,
        function: () -> OutgoingMessage?,
        helpText: String,
        syntaxText: String
    ): this(protocol, name, argSpecs, { _, _, _ -> function() }, helpText, syntaxText)

    constructor(
        protocol: Protocol,
        name: String,
        argSpecs: List<ArgumentSpec>,
        function: (args: List<String>) -> OutgoingMessage?,
        helpText: String,
        syntaxText: String
    ): this(protocol, name, argSpecs, { args: List<String>, _, _ -> function(args) }, helpText, syntaxText)

    constructor(
        protocol: Protocol,
        name: String,
        argSpecs: List<ArgumentSpec>,
        function: (args: List<String>, chat: Chat) -> OutgoingMessage?,
        helpText: String,
        syntaxText: String
    ): this(
        protocol,
        name,
        argSpecs,
        { args: List<String>, chat: Chat, _ -> function(args, chat) },
        helpText,
        syntaxText
    )


    companion object {
        fun of(
            protocol: Protocol,
            name: String,
            argSpecs: List<ArgumentSpec>,
            function: (args: List<String>, chat: Chat, sender: User) -> String?,
            helpText: String,
            syntaxText: String
        ) = Command(
            protocol,
            name,
            argSpecs,
            { args: List<String>, chat: Chat, sender: User ->
                function(args, chat, sender)?.let {
                    SimpleOutgoingMessage(
                        it
                    )
                }
            },
            helpText,
            syntaxText
        )

        fun of(
            protocol: Protocol,
            name: String,
            argSpecs: List<ArgumentSpec>,
            function: (args: List<String>, chat: Chat) -> String?,
            helpText: String,
            syntaxText: String
        ) = Command(
            protocol,
            name,
            argSpecs,
            { args: List<String>, chat: Chat -> function(args, chat)?.let { SimpleOutgoingMessage(it) } },
            helpText,
            syntaxText
        )

        fun of(
            protocol: Protocol,
            name: String,
            argSpecs: List<ArgumentSpec>,
            function: (args: List<String>) -> String?,
            helpText: String,
            syntaxText: String
        ) = Command(
            protocol,
            name,
            argSpecs,
            { args: List<String> -> function(args)?.let { SimpleOutgoingMessage(it) } },
            helpText,
            syntaxText
        )

        fun of(
            protocol: Protocol,
            name: String,
            argSpecs: List<ArgumentSpec>,
            function: () -> String?,
            helpText: String,
            syntaxText: String
        ) = Command(
            protocol,
            name,
            argSpecs,
            { -> function()?.let { SimpleOutgoingMessage(it) } },
            helpText,
            syntaxText
        )
    }
}

data class Alias(
    val scope: CommandScope,
    override val name: String,
    val command: Command,
    val args: List<String>
): CommandLike(scope.protocol, name) {
    fun toDTO() = AliasDTO(
        scope.toKey(),
        name,
        command.name,
        scope.protocol.name,
        args
    )
}
