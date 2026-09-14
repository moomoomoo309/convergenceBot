package convergence.model

import convergence.Protocol

abstract class Server(val name: String, override val protocol: Protocol): Comparable<Server>, CommandScope
