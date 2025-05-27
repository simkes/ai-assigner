package app

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.ext.agent.simpleSingleRunAgent
import ai.koog.prompt.executor.clients.grazie.JetBrainsAIModels
import kotlinx.coroutines.runBlocking
import app.AgentExecutor
import app.SearchTools

fun main() = runBlocking {
    val prodToken = System.getenv("GRAZIE_PROD_TOKEN") ?: error("GRAZIE_PROD_TOKEN environment variable is not set")
    val stgnToken = System.getenv("GRAZIE_STGN_TOKEN") ?: error("GRAZIE_STGN_TOKEN environment variable is not set")

    val toolRegistry = ToolRegistry {
        tools(SearchTools(stgnToken).asTools())
    }

    val agent = simpleSingleRunAgent(
        executor = AgentExecutor(prodToken).getLLMExecutor(),
        llmModel = JetBrainsAIModels.OpenAI.GPT4oMini,
        systemPrompt = """
            You are a ticket dispatcher agent that helps developers find relevant code for issue/ticket/bug descriptions.

            When given an issue description:
            1. Analyze the description to understand the problem
            2. Extract key technical terms and concepts that would be useful for code search
            3. Use the search tool to find relevant code snippets in the repository
            4. For each search, use precise technical terms that are likely to appear in code
            5. If the initial search doesn't yield useful results, try alternative search terms
            6. Summarize your findings, highlighting the most relevant code locations

            Your goal is to help developers quickly locate the code they need to work on based on issue descriptions.
        """.trimIndent(),
        toolRegistry = toolRegistry
    )

    println("Enter an issue/ticket/bug description:")
    val userInput = readln()

    val result = agent.runAndGetResult(userInput)

    println("Result: $result")
}
