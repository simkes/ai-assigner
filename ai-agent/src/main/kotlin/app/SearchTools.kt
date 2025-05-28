package app

import ai.grazie.api.gateway.client.SuspendableAPIGatewayClient
import ai.grazie.client.common.SuspendableHTTPClient
import ai.grazie.client.ktor.GrazieKtorHTTPClient
import ai.grazie.code.indexing.model.ProductionIndices
import ai.grazie.model.auth.v5.AuthData
import ai.grazie.model.cloud.AuthType
import ai.grazie.utils.annotations.ExperimentalAPI
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet

class SearchTools(private val token: String) : ToolSet {
    @OptIn(ExperimentalAPI::class)
    @Tool
    @LLMDescription("Searches for code snippets related to the query in the specified repository")
    suspend fun search(text: String, repository: String = "JetBrains/compose-multiplatform", maxResults: Int = 5): String {
        val client = SuspendableAPIGatewayClient(
            serverUrl = "https://api.app.stgn.grazie.aws.intellij.net",
            authType = AuthType.User,
            httpClient = SuspendableHTTPClient.WithV5(
                GrazieKtorHTTPClient.Client.Default,
                authData = AuthData(token)
            ),
        )

        // Create an instance of the search client
        val searchClient = client.indexing().search()

        // Perform search
        val response = searchClient.search(
            indexAlias = ProductionIndices.CodeBlocks,
            text = text,
            repository = repository,
            maxResults = maxResults,
            minScore = 0.2
        )

        val results = StringBuilder()
        response.res.forEachIndexed { index, item ->
            results.append("${index + 1}. File=${item.sourcePosition.relativePath}, ")
            results.append("offset=${item.sourcePosition.startOffset}:${item.sourcePosition.endOffset}, ")
            results.append("similarity=${item.scoredText.similarity}\n")
            // Just include the file and position information without the actual code snippet
            results.append("\n")
        }

        return if (results.isEmpty()) "No results found for query: $text" else results.toString()
    }
}