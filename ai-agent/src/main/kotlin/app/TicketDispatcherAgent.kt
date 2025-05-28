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
import org.slf4j.LoggerFactory

class TicketDispatcherAgent(
    private val prodToken: String,
    private val stgnToken: String,
    private val githubToken: String
) {
    private val logger = LoggerFactory.getLogger(TicketDispatcherAgent::class.java)
    private lateinit var agent: AIAgent

    suspend fun initialize() {

        val youtrackTools = getYoutrackReadTools()
        val toolRegistry = ToolRegistry {
            tools(
                SearchTools(stgnToken).asTools() +
                        GitHubToolSet(githubToken).asTools() +
                        youtrackTools
            )
        }

        val eventHandlerConfig: EventHandlerConfig.() -> Unit = {
            onToolCall = { tool, toolArgs ->
                logger.info("Tool called: ${tool.name} with args: $toolArgs")
            }

            // Log when a tool call completes successfully
            onToolCallResult = { tool, toolArgs, result ->
                logger.info("Tool ${tool.name} completed with result: ${result?.toStringDefault()}")
            }

            // Log when a tool call fails
            onToolCallFailure = { tool, toolArgs, throwable ->
                logger.error("Tool ${tool.name} failed with error: ${throwable.message}")
            }

            // Log when a validation error occurs during a tool call
            onToolValidationError = { tool, toolArgs, value ->
                logger.warn("Tool ${tool.name} validation error with value: $value")
            }

        }

        // Initialize the agent
        agent = simpleSingleRunAgent(
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
                6. **read_issue_links(id: String, issueLinkId: String)** → reads links of the issue with specific ID
                7. **read_issueLink(id: String)** → gets data for specific link of the issue
                
                **Workflow**  
                If the user input is just an issue number or ID (e.g., XYZ-4321):
                1. Call **get_issue_details** to retrieve complete information about the issue.
                2. If needed, use **read_issueLink** or **read_issue_links** to get additional context from linked issues.
                
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
                7. Synthesize your findings into a **ranked list** of assignees (best fit first).  
                
                **Output**  
                When done, return exactly this JSON structure and stop calling any more tools:
                
                {
                  "summary":   "<concise rationale for your recommendations>",
                  "assignees": [
                    { "login": "alice.johnson", "name": "Alice Johnson", "reason": "<why alice is top choice>" },
                    { "login": "bob.bobby.bob", "name": "Bob Williams", "reason": "<why bob is next best>" },
                    { "login": "charlie445", "name": "Charlie Smith", "reason": "<next>" }
                  ]
                }
            """.trimIndent(),
            toolRegistry = toolRegistry,
            maxIterations = 100,
            installFeatures = {
                install(EventHandler, eventHandlerConfig)
            }
        )
    }

    suspend fun recommendAssignees(description: String): String =
        withContext(Dispatchers.IO) {
            agent.runAndGetResult(description).toString()
        }
}
