package convergence.discord.frat

import convergence.*
import convergence.discord.*
import guru.nidi.graphviz.attribute.Color
import guru.nidi.graphviz.attribute.Style
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.model.Factory.mutGraph
import guru.nidi.graphviz.model.Factory.mutNode
import guru.nidi.graphviz.model.MutableGraph
import guru.nidi.graphviz.model.MutableNode
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.time.LocalTime
import java.time.OffsetDateTime

val englishToGreek = mapOf(
    'A' to 'Α',
    'B' to 'Β',
    'G' to 'Γ',
    'D' to 'Δ',
    'E' to 'Ε',
    'Z' to 'Ζ',
    'H' to 'Η',
    'Q' to 'Θ',
    'I' to 'Ι',
    'K' to 'Κ',
    'L' to 'Λ',
    'M' to 'Μ',
    'N' to 'Ν',
    'X' to 'Ξ',
    'O' to 'Ο',
    'P' to 'Π',
    'R' to 'Ρ',
    'S' to 'Σ',
    'T' to 'Τ',
    'U' to 'Υ',
    'F' to 'Φ',
    'C' to 'Χ',
    'Y' to 'Ψ',
)

fun getBrotherInfo(name: String, searchCriteria: (BrotherInfo) -> String?): BrotherInfo? {
    val lowerName = name.lowercase()
    return brotherInfo.firstOrNull { searchCriteria(it)?.lowercase() == lowerName }
        ?: brotherInfo.firstOrNull { searchCriteria(it)?.lowercase()?.startsWith(lowerName) == true }
        ?: brotherInfo.firstOrNull { searchCriteria(it)?.lowercase()?.contains(lowerName) == true }
}

fun generateGraphGoingDown(graph: MutableGraph, node: BrotherTreeNode, mutNode: MutableNode?) =
    generateGraphGoingDown(graph, node, mutNode) { _, _, _ -> }

fun generateGraphGoingDown(
    graph: MutableGraph,
    node: BrotherTreeNode,
    mutNode: MutableNode?,
    callback: (BrotherTreeNode, MutableNode?, MutableNode) -> Unit
) {
    for (little in node.littles) {
        val name = little.brother.getNodeName()
        val newNode = mutNode(name)
        callback(node, mutNode, newNode)
        mutNode?.addLink(newNode)
        graph.add(newNode)
        generateGraphGoingDown(graph, little, newNode, callback)
    }
}

private fun resolveNode(args: List<String>): Pair<String, BrotherTreeNode>? {
    val name = args.joinToString(" ").lowercase()
    val startInfo = getBrotherInfo(name) { it.getName() } ?: return null
    val node = brotherMap["${startInfo.firstName} ${startInfo.lastName}".lowercase()] ?: return null
    return name to node
}

private fun buildDownGraph(name: String, node: BrotherTreeNode): MutableGraph {
    val graph = mutGraph("$name's line").setDirected(true)
    val rootNode = mutNode(node.brother.getNodeName())
    graph.add(rootNode)
    generateGraphGoingDown(graph, node, rootNode)
    return graph
}

private fun graphToMessage(graph: MutableGraph, name: String): DiscordOutgoingMessage {
    val stream = ByteArrayOutputStream()
    Graphviz.fromGraph(graph).render(Format.PNG).toOutputStream(stream)
    return DiscordOutgoingMessage(
        MessageCreateBuilder()
            .addFiles(FileUpload.fromData(stream.toByteArray(), "$name line.png"))
            .build()
    )
}

fun brotherLine(args: List<String>): OutgoingMessage {
    val (name, node) = resolveNode(args)
        ?: return SimpleOutgoingMessage("No brothers found searching for \"${args.joinToString(" ")}\".")
    return graphToMessage(buildDownGraph(name, node), name)
}

fun fullTree(args: List<String>): OutgoingMessage {
    val (name, node) = resolveNode(args)
        ?: return SimpleOutgoingMessage("No brothers found searching for \"${args.joinToString(" ")}\".")
    val brotherLine = mutableSetOf(node.brother.getName())

    fun recurse(node: BrotherTreeNode) {
        brotherLine.add(node.brother.getName())
        for (little in node.littles) recurse(little)
    }
    recurse(node)
    var big = node.big
    while (big != null) {
        brotherLine.add(big.brother.getNodeName())
        big = big.big
    }

    val graph = mutGraph("$name's line in full tree").setDirected(true)
    generateGraphGoingDown(graph, brotherRoot, null) { _, _, newNode ->
        if (newNode.name().value() in brotherLine)
            newNode.add(Style.FILLED, Color.GRAY).add(Color.RED.font())
    }
    return graphToMessage(graph, name)
}

fun fullLine(args: List<String>): OutgoingMessage {
    val (name, node) = resolveNode(args)
        ?: return SimpleOutgoingMessage("No brothers found searching for \"${args.joinToString(" ")}\".")
    val graph = buildDownGraph(name, node)
    val rootNode = mutNode(node.brother.getNodeName())
    var big = node.big
    var previousNode = rootNode
    while (big != null) {
        val newNode = mutNode(big.brother.getNodeName())
        newNode.addLink(previousNode)
        graph.add(newNode)
        previousNode = newNode
        big = big.big
    }
    return graphToMessage(graph, name)
}


fun brotherBigs(args: List<String>): OutgoingMessage {
    val name = args.joinToString(" ").lowercase()
    val startInfo = getBrotherInfo(name) { it.getName() }
        ?: return SimpleOutgoingMessage("No brothers found searching for \"$name\".")

    var node: BrotherTreeNode? = brotherMap[startInfo.getName().lowercase()]
        ?: return SimpleOutgoingMessage(
            "No brothers found searching for " +
                    "\"${startInfo.firstName} ${startInfo.lastName}\"."
        )
    // Add the line going up
    val line = mutableListOf(node!!.brother)
    node = node.big
    @Suppress("unused")
    for (unused in 0..<25) { // Discord has a 25 field limit
        if (node == null)
            break
        line.add(node.brother)
        node = node.big
    }

    val msg = MessageCreateBuilder()
    val embeds = EmbedBuilder()
        .setTitle("Line for Brother #${startInfo.rosterNumber} ${startInfo.getName()}")

    for (brother in line) {
        embeds.addField(
            "#" + brother.rosterNumber,
            "${brother.firstName} \"${brother.nickName}\" ${brother.lastName}",
            true
        )
    }
    return DiscordOutgoingMessage(msg.addEmbeds(embeds.build()).build())
}

fun brotherInfo(args: List<String>, searchCriteria: (BrotherInfo) -> String?): DiscordOutgoingMessage {
    val name = args.joinToString(" ").lowercase()
    val info = getBrotherInfo(name, searchCriteria)
        ?: return DiscordOutgoingMessage("No brothers found searching for \"$name\".")

    return DiscordOutgoingMessage(
        MessageCreateBuilder()
            .addEmbeds(
                EmbedBuilder()
                    .setTitle("Info for Brother #${info.rosterNumber} ${info.firstName} ${info.lastName}")
                    .addField(
                        "Pledge Class",
                        info.pledgeClass.map { englishToGreek[it] }.joinToString("").ifBlank { "not listed" }, true
                    )
                    .addField("Crossing date", info.crossingDate.ifBlank { "not listed" }, true)
                    .addField("Big brother", info.bigBrother.ifBlank { "not listed" }, true)
                    .addField(
                        "Nickname",
                        if (info.nickName.isNotBlank()) "\"${info.nickName}\"" else "not listed",
                        true
                    )
                    .addField("Major", info.major.ifBlank { "not listed" }, true)
                    .build()
            ).build()
    )
}

private fun setAlumnusNickname(args: List<String>, chat: Chat, sender: User): String {
    if (args.size != 1)
        return "Syntax: setAlumnusNickname (roster number)"
    if (sender !is DiscordUser || chat !is DiscordChat)
        return "This command only works on Discord."
    if (chat.server.guild.idLong != fratConfig.aaServerID)
        return "You can only run this command on the alumni association server."
    val brotherInfo = getBrotherInfo(args[0]) { it.rosterNumber }
        ?: return "No brother with roster number ${args[0]} found."
    val nickname = "${brotherInfo.rosterNumber} - ${brotherInfo.getName()} (${brotherInfo.realPledgeClass})"
    DiscordProtocol.setUserNickname(chat, sender, nickname)
    return "Nickname updated."
}

private val pledgeRole: DiscordRole? by lazy {
    jda.getRoleById(fratConfig.pledgeRoleID)?.let { DiscordRole(it) }
}

val isNotPledge = { _: List<String>, chat: Chat, sender: User ->
    val server = (chat as? DiscordChat)?.server
    val role = pledgeRole
    if (server != null && role != null && DiscordProtocol.userHasRole(server, sender, role))
        SimpleOutgoingMessage("Nice try, pledge.")
    else
        null
}

@Suppress("LongMethod")
fun registerFratCommands() {
    registerCommand(
        Command(
            DiscordProtocol,
            "brotherbyroster",
            listOf(ArgumentSpec("Roster", ArgumentType.STRING)),
            { args: List<String> -> brotherInfo(args) { it.rosterNumber } },
            "Gets information about a particular brother based on their roster number.",
            "brotherbyroster (roster number)",
            isNotPledge
        )
    )
    registerCommand(
        Command(
            DiscordProtocol,
            "brotherbyname",
            listOf(ArgumentSpec("Name", ArgumentType.STRING)),
            { args -> brotherInfo(args) { it.getName() } },
            "Gets information about a particular brother based on their first and last name.",
            "brotherbyname (name)",
            isNotPledge
        )
    )
    registerCommand(
        Command(
            DiscordProtocol,
            "brotherbynickname",
            listOf(ArgumentSpec("Nickname", ArgumentType.STRING)),
            { args -> brotherInfo(args) { it.nickName } },
            "Gets information about a particular brother based on their nickname.",
            "brotherbynickname (nickname)",
            isNotPledge
        )
    )
    registerCommand(
        Command(
            DiscordProtocol,
            "brotherline",
            listOf(ArgumentSpec("Name", ArgumentType.STRING)),
            { args -> brotherLine(args) },
            "Gets information about a particular brother's line going down.",
            "brotherLine (name)",
            isNotPledge
        )
    )
    registerCommand(
        Command(
            DiscordProtocol,
            "brotherbigs",
            listOf(ArgumentSpec("Name", ArgumentType.STRING)),
            { args -> brotherBigs(args) },
            "Gets information about a particular brother's line going up.",
            "brotherBigs (name)",
            isNotPledge
        )
    )
    registerCommand(
        Command(
            DiscordProtocol,
            "fullline",
            listOf(ArgumentSpec("Name", ArgumentType.STRING)),
            { args -> fullLine(args) },
            "Gets information about a particular brother's line going up and down.",
            "fullLine (name)",
            isNotPledge
        )
    )
    registerCommand(
        Command(
            DiscordProtocol,
            "fulltree",
            listOf(ArgumentSpec("Name", ArgumentType.STRING)),
            { args -> fullTree(args) },
            "Shows the full tree, with a particular brother's line going up and down highlighted.",
            "fullTree (name)",
            isNotPledge
        )
    )
    registerCommand(
        Command.of(
            DiscordProtocol,
            "updateRoster",
            listOf(),
            { ->
                val newRoster = getNewRoster()
                brotherInfo.clear()
                brotherInfo.addAll(newRoster)
                Files.write(brotherInfoPath, objectMapper.writeValueAsBytes(newRoster))
                "Roster updated."
            },
            "Updates the brother roster list.",
            "updateRoster (takes no arguments)"
        )
    )
    registerCommand(
        Command.of(
            DiscordProtocol,
            "registerMentionChat",
            listOf(ArgumentSpec("user", ArgumentType.STRING)),
            fct@{ args, chat, _ ->
                val name = args.joinToString(" ")
                val target = getUserFromName(chat, name)
                    ?: return@fct "No user found with name \"$name\"."
                mentionChats
                    .getOrPut(chat) { mutableMapOf() }
                    .putIfAbsent(target, mutableMapOf())
                Settings.update()
                "Chat registered to mention ${getUserName(chat, target)}."
            },
            "Registers this chat with the given user as a mention chat.",
            "registerMentionChat (user)",
            isNotPledge
        )
    )
    registerCommand(
        Command.of(
            DiscordProtocol,
            "removeMentionChats",
            listOf(),
            { _, chat, _ ->
                mentionChats
                    .getOrDefault(chat, mutableMapOf())
                    .clear()
                Settings.update()
                "Mention users cleared from this chat."
            },
            "Removes all mention users from this chat.",
            "removeMentionChat (takes no arguments)",
            isNotPledge
        )
    )
    registerCommand(
        Command.of(
            DiscordProtocol,
            "mentionChatStats",
            listOf(ArgumentSpec("user", ArgumentType.STRING)),
            { _, chat, _ -> "Mention chat stats for this channel:\n${mentionStats(chat)}" },
            "Lists the stats for this mention chat.",
            "mentionChatStats (takes no arguments)"
        )
    )
    registerCommand(
        Command.of(
            DiscordProtocol,
            "setAlumnusNickname",
            listOf(ArgumentSpec("rosternumber", ArgumentType.NUMBER)),
            ::setAlumnusNickname,
            "Sets an alumnus's nickname for the alumni association server.",
            "setAlumnusNickname (roster number)"
        )
    )
    callbacks.getOrPut(MentionedUser::class) { mutableListOf() }.add(
        MentionedUser { chat: Chat, msg: IncomingMessage, sender: User, users: List<User> ->
            if (msg is DiscordIncomingMessage) {
                if (sender !is DiscordUser)
                    return@MentionedUser true
                val newMentions = mutableMapOf<DiscordUser, Int>()
                for (user in users) {
                    user as? DiscordUser ?: continue
                    val mentions = (mentionChats[chat] ?: return@MentionedUser true)[user] ?: return@MentionedUser true
                    val mentionCount = mentions.getOrDefault(sender, 0) + 1
                    mentions[sender] = mentionCount
                    newMentions[user] = mentionCount
                }
                Settings.update()
                val parts = newMentions.toList().map { (user, count) ->
                    "${user.name} $count time${if (count > 1) "s" else ""}"
                }
                val mentionStr = when (parts.size) {
                    1 -> parts[0]
                    2 -> "${parts[0]} and ${parts[1]}"
                    else -> "${parts.dropLast(1).joinToString(", ")}, and ${parts.last()}"
                }
                sendMessage(chat, "You mentioned $mentionStr.")
            }
            true
        }
    )
    CommandScheduler.taskList.add(ScheduledTask(nextMonth(), ::mentionStatsFct))
}

private fun nextMonth(): OffsetDateTime {
    // If it's the first of the month, we should schedule it for Noon today
    val now = OffsetDateTime.now()
    val thisMonth = now
        .withDayOfMonth(1)
        .with(LocalTime.MIN)
        .plusHours(12)
    // But if it isn't, it should be scheduled for next month
    if (thisMonth < now)
        return thisMonth.plusMonths(1)

    return thisMonth
}

private fun mentionStatsFct() {
    for ((chat, _) in mentionChats) {
        sendMessage(chat, "Monthly mention stats:\n${mentionStats(chat)}")
    }
    for ((_, stats) in mentionChats)
        for ((_, mentioners) in stats)
            mentioners.clear()
    Settings.update()
    // Schedule it again for next month
    CommandScheduler.taskList.add(ScheduledTask(nextMonth(), ::mentionStatsFct))
}

fun mentionStats(chat: Chat) = mentionChats
    .getOrDefault(chat, mutableMapOf())
    .map { (target, mentions) ->
        "${DiscordProtocol.getUserName(chat, target)}:\n\t${
            mentions.toList().joinToString(
                "\n\t",
                transform = { (user, count) ->
                    "${getUserName(chat, user)}: $count"
                })
        }"
    }.joinToString()
