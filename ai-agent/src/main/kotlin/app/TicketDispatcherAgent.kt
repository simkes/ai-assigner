package app

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.ext.agent.simpleSingleRunAgent
import ai.koog.agents.local.features.eventHandler.feature.EventHandler
import ai.koog.agents.local.features.eventHandler.feature.EventHandlerConfig
import ai.koog.prompt.executor.clients.grazie.JetBrainsAIModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.gitTools.GitHubToolSet
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.slf4j.LoggerFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
sealed class LogMessage {
    abstract val type: String
    abstract val toolName: String
    abstract val timestamp: Long
}

@Serializable
data class ToolCallLogMessage(
    override val type: String = "CALL",
    override val toolName: String,
    override val timestamp: Long = System.currentTimeMillis(),
    val args: String
) : LogMessage()

@Serializable
data class ToolResultLogMessage(
    override val type: String = "OK",
    override val toolName: String,
    override val timestamp: Long = System.currentTimeMillis(),
    val result: String
) : LogMessage()

@Serializable
data class ToolErrorLogMessage(
    override val type: String = "ERROR",
    override val toolName: String,
    override val timestamp: Long = System.currentTimeMillis(),
    val error: String
) : LogMessage()

class TicketDispatcherAgent(
    private val prodToken: String,
    private val stgnToken: String,
    private val githubToken: String
) {
    private val logger = LoggerFactory.getLogger(TicketDispatcherAgent::class.java)
    private lateinit var processingAgent: AIAgent
    private lateinit var formattingAgent: AIAgent
    private val json = Json { prettyPrint = false; encodeDefaults = true }

    private val _logs = MutableSharedFlow<String>(extraBufferCapacity = 100)
    val logs = _logs.asSharedFlow()

    suspend fun initialize() {
        val youtrackTools = getYoutrackReadTools()
        val toolRegistry = ToolRegistry {
            tools(
                SearchTools(stgnToken).asTools() +
                        GitHubToolSet(githubToken).asTools()
                       + youtrackTools
            )
        }

        val eventHandlerConfig: EventHandlerConfig.() -> Unit = {
            onToolCall = { tool, toolArgs ->
                logger.info("Tool called: ${tool.name} with args: $toolArgs")

                val logMessage = ToolCallLogMessage(
                    toolName = tool.name,
                    args = toolArgs.toString()
                )
                _logs.tryEmit(json.encodeToString(logMessage))
            }

            // Log when a tool call completes successfully
            onToolCallResult = { tool, toolArgs, result ->
                logger.info("Tool ${tool.name} completed with result: ${result?.toStringDefault()}")

                val logMessage = ToolResultLogMessage(
                    toolName = tool.name,
                    result = result?.toStringDefault() ?: "null"
                )
                _logs.tryEmit(json.encodeToString(logMessage))
            }

            // Log when a tool call fails
            onToolCallFailure = { tool, toolArgs, throwable ->
                logger.error("Tool ${tool.name} failed with error: ${throwable.message}")

                val logMessage = ToolErrorLogMessage(
                    toolName = tool.name,
                    error = throwable.message ?: "Unknown error"
                )
                _logs.tryEmit(json.encodeToString(logMessage))
            }

            // Log when a validation error occurs during a tool call
            onToolValidationError = { tool, toolArgs, value ->
                logger.warn("Tool ${tool.name} validation error with value: $value")

                val logMessage = ToolErrorLogMessage(
                    toolName = tool.name,
                    error = "Validation error: $value"
                )
                _logs.tryEmit(json.encodeToString(logMessage))
            }
        }

        // Initialize the processing agent that does all the work
        processingAgent = simpleSingleRunAgent(
            executor = AgentExecutor(prodToken).getLLMExecutor(),
            llmModel = JetBrainsAIModels.OpenAI.GPT4oMini,
            systemPrompt = """
                You are **Ticket-Dispatcher AI**.  
                Your goal is to recommend the best person(s) to assign a ticket to, based on both the relevance of code ownership and the deeper context of each contributor's work.

                **Available tools**  
                1. **search(query: String)** → returns file paths matching text query  
                2. **git_blame(input: GitBlameInput)** → returns ranges of lines and their last-author commits for a file  
                3. **git_log(input: GitLogInput)** → returns recent commits for a branch or path
                4. **get_issue_details(issueId: String, fields: String)** → retrieves complete details for a specified issue
                5. **search_issues(query: String, limit: Integer, offset: Integer)** → searches for issues using query syntax

                **Workflow**  
                If the user input is just an issue number or ID (e.g., XYZ-4321):
                1. Call **get_issue_details** to retrieve complete information about the issue.

                For regular ticket assignment:
                1. If the input contains an issue ID, call **get_issue_details** to get complete information.
                2. Optionally use **search_issues** with a limited number of results to find related tickets.
                3. Carefully read the ticket description and extract key technical terms, error messages, class or function names.  
                4. Call **search** with those terms to identify the most relevant source files.  
                5. For each file path:  
                   - Call **git_blame** to see who last touched each logical block.  
                   - Optionally call **git_log** to understand each author's recent commit messages (recency, scope, complexity).  
                6. Reflect on each contributor's pattern of work in those files—consider not just raw line counts but also:  
                   1. Recency of changes (who's been active lately)  
                   2. Breadth and depth of changes (major refactors vs. small tweaks)  
                   3. Contextual fit (did they implement the feature or fix similar bugs?)  
                7. Synthesize your findings into a detailed analysis of potential assignees.

                **Output**  
                When done, provide a detailed analysis of potential assignees, including their contributions, expertise, and relevance to the ticket.
                Include all the information you've gathered about each potential assignee, such as their recent commits, areas of expertise, and why they might be suitable for this ticket.
            """.trimIndent(),
            toolRegistry = toolRegistry,
            maxIterations = 100,
            installFeatures = {
                install(EventHandler, eventHandlerConfig)
            }
        )

        // Initialize the formatting agent that transforms the output
        formattingAgent = simpleSingleRunAgent(
            executor = AgentExecutor(prodToken).getLLMExecutor(),
            llmModel = JetBrainsAIModels.OpenAI.GPT4oMini,
            systemPrompt = """
                You are a formatting agent for the Ticket-Dispatcher AI.
                Your job is to take the detailed analysis provided by the processing agent and transform it into a structured JSON format.

                You will receive a detailed analysis of potential assignees for a ticket, including their contributions, expertise, and relevance.
                Your task is to extract the most relevant information and format it according to the required JSON structure.

                **Output**  
                When done, return exactly this JSON structure and stop:

                {
                  "summary": "<concise rationale for your recommendations>",
                  "assignees": [
                    { "login": "alice.johnson", "name": "Alice Johnson", "reason": "<why alice is top choice>" },
                    { "login": "bob.bobby.bob", "name": "Bob Williams", "reason": "<why bob is next best>" },
                    { "login": "charlie445", "name": "Charlie Smith", "reason": "<next>" }
                  ]
                }

                Make sure to:
                1. Extract the most relevant assignees from the analysis
                2. Provide a concise summary of the rationale
                3. For each assignee, include their login, full name, and a brief reason why they are recommended
                4. List assignees in order of relevance (best fit first)
                5. Return valid JSON that exactly matches the structure above
            """.trimIndent(),
            toolRegistry = ToolRegistry { },  // No tools needed for formatting agent
            maxIterations = 10,
            installFeatures = {
                install(EventHandler, eventHandlerConfig)
            }
        )
    }

    suspend fun recommendAssignees(description: String): String =
        withContext(Dispatchers.IO) {
            // First, run the processing agent to analyze the ticket and identify potential assignees
            val processingResult = processingAgent.runAndGetResult(description).toString()

            // Then, run the formatting agent to transform the processing result into the required JSON format
            formattingAgent.runAndGetResult(processingResult).toString()
        }
}
