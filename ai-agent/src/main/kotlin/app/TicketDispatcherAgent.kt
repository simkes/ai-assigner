package app

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.ext.agent.simpleSingleRunAgent
import ai.koog.prompt.executor.clients.grazie.JetBrainsAIModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.gitTools.GitHubToolSet

class TicketDispatcherAgent(
    prodToken: String,
    stgnToken: String,
    githubToken: String
) {
    // initialize the LLM agent once
    private val agent = simpleSingleRunAgent(
        executor     = AgentExecutor(prodToken).getLLMExecutor(),
        llmModel     = JetBrainsAIModels.OpenAI.GPT4oMini,
        systemPrompt = """
            You are **Ticket-Dispatcher AI**.  
            Your goal is to recommend the best person(s) to assign a ticket to, based on both the relevance of code ownership and the deeper context of each contributor’s work.
            
            **Available tools**  
            1. **search(query: String)** → returns file paths matching text query  
            2. **git_blame(input: GitBlameInput)** → returns ranges of lines and their last-author commits for a file  
            3. **git_log(input: GitLogInput)** → returns recent commits for a branch or path
            
            **Workflow**  
            1. Carefully read the ticket description and extract key technical terms, error messages, class or function names.  
            2. Call **search** with those terms to identify the most relevant source files.  
            3. For each file path:  
               - Call **git_blame** to see who last touched each logical block.  
               - Optionally call **git_log** to understand each author’s recent commit messages (recency, scope, complexity).  
            4. Reflect on each contributor’s pattern of work in those files—consider not just raw line counts but also:  
               1. Recency of changes (who’s been active lately)  
               2. Breadth and depth of changes (major refactors vs. small tweaks)  
               3. Contextual fit (did they implement the feature or fix similar bugs?)  
            5. Synthesize your findings into a **ranked list** of assignees (best fit first).  
            
            **Output**  
            When done, return exactly this plain JSON structure and stop calling any more tools:
            
            {
              "summary":   "<concise rationale for your recommendations>",
              "assignees": [
                { "login": "alice.johnson", "name": "Alice Johnson", "reason": "<why alice is top choice>" },
                { "login": "bob.bobby.bob", "name": "Bob Williams", "reason": "<why bob is next best>" },
                { "login": "charlie445", "name": "Charlie Smith", "reason": "<next>" }
              ]
            }
        """.trimIndent(),
        toolRegistry = ToolRegistry {
            tools(
                SearchTools(stgnToken).asTools() +
                        GitHubToolSet(githubToken).asTools()
            )
        },
        maxIterations = 100
    )

    suspend fun recommendAssignees(description: String): String =
        withContext(Dispatchers.IO) {
            agent.runAndGetResult(description).toString()
        }
}
