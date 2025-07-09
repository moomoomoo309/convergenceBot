package convergence.discord.frat

import convergence.*
import convergence.discord.DiscordOutgoingMessage
import convergence.discord.DiscordProtocol
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files

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
    return brotherInfo.firstOrNull { searchCriteria(it)?.lowercase() == name }
        ?: brotherInfo.firstOrNull { searchCriteria(it)?.lowercase()?.startsWith(name) == true }
        ?: brotherInfo.firstOrNull { searchCriteria(it)?.lowercase()?.contains(name) == true }
}

fun generateGraphGoingDown(graph: MutableGraph, node: BrotherTreeNode, mutNode: MutableNode?) = generateGraphGoingDown(graph, node, mutNode) { _,_,_ -> }
fun generateGraphGoingDown(graph: MutableGraph, node: BrotherTreeNode, mutNode: MutableNode?, callback: (BrotherTreeNode, MutableNode?, MutableNode) -> Unit) {
    for (little in node.littles) {
        val name = little.brother.getName()
        val newNode = mutNode(name)
        callback(node, mutNode, newNode)
        mutNode?.addLink(newNode)
        graph.add(newNode)
        generateGraphGoingDown(graph, little, newNode, callback)
    }
}

fun brotherLine(args: List<String>): OutgoingMessage {
    val name = args.joinToString(" ").lowercase()
    val startInfo = getBrotherInfo(name) { it.getName() }
        ?: return SimpleOutgoingMessage("No brothers found searching for \"$name\".")

    val node = brotherMap["${startInfo.firstName} ${startInfo.lastName}".lowercase()]
        ?: return SimpleOutgoingMessage("No brothers found searching for \"${startInfo.firstName} ${startInfo.lastName}\".")
    // Convert the brother tree into graphviz nodes
    val graph = mutGraph("$name's line")
        .setDirected(true)
    val rootNode = mutNode(node.brother.getName())
    graph.add(rootNode)
    generateGraphGoingDown(graph, node, rootNode)

    val msg = MessageCreateBuilder()
    // Render the graph to an image, get it as a byte array
    val rendered = Graphviz.fromGraph(graph).render(Format.PNG)
    val stream = ByteArrayOutputStream()

    // Convert the outputstream to an inputstream so it can be uploaded to discord
    rendered.toOutputStream(stream)
    msg.addFiles(FileUpload.fromData(ByteArrayInputStream(stream.toByteArray()), "$name line.png"))
    return DiscordOutgoingMessage(msg.build())
}

fun fullTree(args: List<String>): OutgoingMessage {
    val name = args.joinToString(" ").lowercase()
    val startInfo = getBrotherInfo(name) { it.getName() }
        ?: return SimpleOutgoingMessage("No brothers found searching for \"$name\".")

    val node = brotherMap["${startInfo.firstName} ${startInfo.lastName}".lowercase()]
        ?: return SimpleOutgoingMessage("No brothers found searching for \"${startInfo.firstName} ${startInfo.lastName}\".")
    val brotherLine = mutableSetOf<String>()

    // Add the line going down
    fun recurse(node: BrotherTreeNode) {
        brotherLine.add(node.brother.getName())
        for (little in node.littles) {
            recurse(little)
        }
    }
    recurse(node)
    var big = node.big
    // Add the line going up
    while (big != null) {
        brotherLine.add(big.brother.getName())
        big = big.big
    }

    // Convert the brother tree into graphviz nodes
    val graph = mutGraph("$name's line in full tree")
        .setDirected(true)
    generateGraphGoingDown(graph, brotherRoot, null) { _, _, newNode ->
        // Make your line red and gray
        if (name in brotherLine)
            newNode.add(Style.FILLED, Color.GRAY).add(Color.RED.font())
    }

    val msg = MessageCreateBuilder()
    // Render the graph to an image, get it as a byte array
    val rendered = Graphviz.fromGraph(graph).render(Format.PNG)
    val stream = ByteArrayOutputStream()

    // Convert the outputstream to an inputstream so it can be uploaded to discord
    rendered.toOutputStream(stream)
    msg.addFiles(FileUpload.fromData(ByteArrayInputStream(stream.toByteArray()), "$name line.png"))
    return DiscordOutgoingMessage(msg.build())
}

fun fullLine(args: List<String>): OutgoingMessage {
    val name = args.joinToString(" ").lowercase()
    val startInfo = getBrotherInfo(name) { it.getName() }
        ?: return SimpleOutgoingMessage("No brothers found searching for \"$name\".")

    val node = brotherMap["${startInfo.firstName} ${startInfo.lastName}".lowercase()]
        ?: return SimpleOutgoingMessage("No brothers found searching for \"${startInfo.firstName} ${startInfo.lastName}\".")
    // Convert the brother tree into graphviz nodes
    val graph = mutGraph("$name's line")
        .setDirected(true)
    val rootNode = mutNode(node.brother.getName())
    graph.add(rootNode)
    generateGraphGoingDown(graph, node, rootNode)
    var big = node.big
    var previousNode = rootNode
    // Add the line going all the way up
    while (big != null) {
        val newNode = mutNode(big.brother.getName())
        newNode.addLink(previousNode)
        graph.add(newNode)
        previousNode = newNode
        big = big.big
    }

    val msg = MessageCreateBuilder()
    // Render the graph to an image, get it as a byte array
    val rendered = Graphviz.fromGraph(graph).render(Format.PNG)
    val stream = ByteArrayOutputStream()

    // Convert the outputstream to an inputstream so it can be uploaded to discord
    rendered.toOutputStream(stream)
    msg.addFiles(FileUpload.fromData(ByteArrayInputStream(stream.toByteArray()), "$name line.png"))
    return DiscordOutgoingMessage(msg.build())
}


fun brotherBigs(args: List<String>): OutgoingMessage {
    val name = args.joinToString(" ").lowercase()
    val startInfo = getBrotherInfo(name) { it.firstName + " " + it.lastName }
        ?: return SimpleOutgoingMessage("No brothers found searching for \"$name\".")

    var node: BrotherTreeNode? = brotherMap["${startInfo.firstName} ${startInfo.lastName}".lowercase()]
        ?: return SimpleOutgoingMessage("No brothers found searching for \"${startInfo.firstName} ${startInfo.lastName}\".")
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
        .setTitle("Line for Brother #${startInfo.rosterNumber} ${startInfo.firstName} ${startInfo.lastName}")

    for (brother in line) {
        embeds.addField("#" + brother.rosterNumber, "${brother.firstName} \"${brother.nickName}\" ${brother.lastName}", true)
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

fun registerFratCommands() {
    registerCommand(
        Command(
            DiscordProtocol,
            "brotherbyroster",
            listOf(ArgumentSpec("Roster", ArgumentType.STRING)),
            { args: List<String> -> brotherInfo(args) { it.rosterNumber } },
            "Gets information about a particular brother based on their roster number.",
            "brotherbyroster (roster number)"
        )
    )
    registerCommand(
        Command(
            DiscordProtocol,
            "brotherbyname",
            listOf(ArgumentSpec("Name", ArgumentType.STRING)),
            { args -> brotherInfo(args) { it.getName() } },
            "Gets information about a particular brother based on their first and last name.",
            "brotherbyname (name)"
        )
    )
    registerCommand(
        Command(
            DiscordProtocol,
            "brotherbynickname",
            listOf(ArgumentSpec("Nickname", ArgumentType.STRING)),
            { args -> brotherInfo(args) { it.nickName } },
            "Gets information about a particular brother based on their nickname.",
            "brotherbynickname (nickname)"
        )
    )
    registerCommand(
        Command(
            DiscordProtocol,
            "brotherline",
            listOf(ArgumentSpec("Name", ArgumentType.STRING)),
            { args -> brotherLine(args) },
            "Gets information about a particular brother's line going down.",
            "brotherLine (name)"
        )
    )
    registerCommand(
        Command(
            DiscordProtocol,
            "brotherbigs",
            listOf(ArgumentSpec("Name", ArgumentType.STRING)),
            { args -> brotherBigs(args) },
            "Gets information about a particular brother's line going up.",
            "brotherBigs (name)"
        )
    )
    registerCommand(
        Command(
            DiscordProtocol,
            "fullline",
            listOf(ArgumentSpec("Name", ArgumentType.STRING)),
            { args -> fullLine(args) },
            "Gets information about a particular brother's line going up and down.",
            "fullLine (name)"
        )
    )
    registerCommand(
        Command(
            DiscordProtocol,
            "fulltree",
            listOf(ArgumentSpec("Name", ArgumentType.STRING)),
            { args -> fullTree(args) },
            "Shows the full tree, with a particular brother's line going up and down highlighted.",
            "fullTree (name)"
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
}
