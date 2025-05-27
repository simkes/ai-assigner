package com.example

import app.TicketDispatcherAgent
import org.springframework.stereotype.Service

@Service
class AgentServiceAdapter {
    private val agent = TicketDispatcherAgent(
        prodToken  = System.getenv("GRAZIE_PROD_TOKEN") ?: error("GRAZIE_PROD_TOKEN environment variable is not set"),
        stgnToken  = System.getenv("GRAZIE_STGN_TOKEN") ?: error("GRAZIE_STGN_TOKEN environment variable is not set"),
        githubToken= System.getenv("GITHUB_TOKEN") ?: error("GITHUB_TOKEN missing")
    )

    suspend fun dispatchTicket(description: String): String =
        agent.recommendAssignees(description)
}