package app

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.mcp.McpToolRegistryProvider

suspend fun getYoutrackReadTools(): List<Tool<*, *>> {
    val command = arrayOf(
        "docker", "run", "--rm", "-i", "-p", "8080:8080",
        "-v", "C:\\Projects\\mcp-studio\\data:/app/data",
        "-e", "OPENAI_API_KEY=${System.getenv("OPENAI_API_KEY") ?: ""}",
        "-e", "YOUTRACK_BASE_URL=https://youtrack-staging.labs.intellij.net/",
        "-e", "YOUTRACK_TOKEN=Bearer ${System.getenv("YOUTRACK_TOKEN") ?: ""}",
        "mcp-studio"
    )
    val process = ProcessBuilder(*command).start()

    val toolRegistry = McpToolRegistryProvider.fromTransport(
        McpToolRegistryProvider.defaultStdioTransport(process)
    )

    return toolRegistry.tools.filter {
        it.name.startsWith("search") ||
                it.name.startsWith("get") ||
                it.name.startsWith("read")
    }
}