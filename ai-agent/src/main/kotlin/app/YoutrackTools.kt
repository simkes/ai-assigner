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

class YoutrackToolSet(private val token: String) : ToolSet {
    private val client = OkHttpClient()
    private val baseUrl = "https://youtrack-staging.labs.intellij.net/api"

    @ai.koog.agents.core.tools.annotations.Tool(customName = "search_issues")
    @LLMDescription("Searches for issues using query syntax")
    suspend fun searchIssues(
        @LLMDescription("Query string to search for issues") query: String,
        @LLMDescription("Maximum number of results to return") limit: Int = 10,
        @LLMDescription("Number of results to skip") offset: Int = 0,
        @LLMDescription("Fields to include in the response") fields: String = "id,idReadable,summary,customFields(name,value(id,login,fullName))"
    ): String = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            error("search_issues failed: Token is empty or blank")
        }

        val authHeader = if (token.startsWith("Bearer")) token else "Bearer $token"

        val request = Request.Builder()
            .url("$baseUrl/issues?fields=$fields&query=$query&\$top=$limit&\$skip=$offset")
            .addHeader("Authorization", authHeader)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("search_issues failed: HTTP ${response.code}")
            response.body?.string() ?: "[]"
        }
    }
}

suspend fun getMcpYoutrackTools(token: String = System.getenv("YOUTRACK_TOKEN") ?: ""): List<Tool<*, *>> {
    if (token.isBlank()) {
        error("getMcpYoutrackTools failed: Token is empty or blank")
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

    return toolRegistry.tools.filter {
        it.name == "get_issue_details"
    }
}

suspend fun getYoutrackReadTools(token: String = System.getenv("YOUTRACK_TOKEN") ?: ""): List<Tool<*, *>> {
    val directTools = YoutrackToolSet(token).asTools()
    val mcpTools = getMcpYoutrackTools(token)

    return directTools + mcpTools
}
