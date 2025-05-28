package app

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.mcp.McpToolRegistryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

suspend fun getYoutrackReadTools(token: String = System.getenv("YOUTRACK_TOKEN") ?: ""): List<Tool<*, *>> {
    if (token.isBlank()) {
        error("getYoutrackReadTools failed: Token is empty or blank")
    }

    val bearerToken = if (token.startsWith("Bearer")) token else "Bearer $token"
    val command = arrayOf(
        "docker", "run", "--rm", "-i", "-p", "8080:8080",
        "-v", "C:\\Projects\\mcp-studio\\data:/app/data",
        "-e", "OPENAI_API_KEY=${System.getenv("OPENAI_API_KEY") ?: ""}",
        "-e", "YOUTRACK_BASE_URL=https://youtrack-staging.labs.intellij.net/",
        "-e", "YOUTRACK_TOKEN=$bearerToken",
        "mcp-studio"
    )
    val process = ProcessBuilder(*command).start()

    val toolRegistry = McpToolRegistryProvider.fromTransport(
        McpToolRegistryProvider.defaultStdioTransport(process)
    )

    return toolRegistry.tools
}
