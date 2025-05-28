package com.example

import app.TicketDispatcherAgent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service

@Service
class AgentServiceAdapter {
    private val agent = TicketDispatcherAgent(
        prodToken = System.getenv("GRAZIE_PROD_TOKEN") ?: error("GRAZIE_PROD_TOKEN environment variable is not set"),
        stgnToken = System.getenv("GRAZIE_STGN_TOKEN") ?: error("GRAZIE_STGN_TOKEN environment variable is not set"),
        githubToken = System.getenv("GITHUB_TOKEN") ?: error("GITHUB_TOKEN missing")
    )

    init {
        runBlocking {
            agent.initialize()
        }
    }

    fun getLogFlow() = agent.logs

    suspend fun dispatchTicket(description: String): String =
        agent.recommendAssignees(description)

    // Expose the logs flow from the agent
    val agentLogs: Flow<String> get() = agent.logs
}
