package app

import ai.grazie.api.gateway.client.SuspendableAPIGatewayClient
import ai.grazie.client.common.SuspendableClientWithBackoff
import ai.grazie.client.common.SuspendableHTTPClient
import ai.grazie.client.ktor.GrazieKtorHTTPClient
import ai.grazie.model.auth.GrazieAgent
import ai.grazie.model.auth.v5.AuthData
import ai.koog.prompt.executor.clients.grazie.GrazieEnvironment
import ai.koog.prompt.executor.clients.grazie.GrazieLLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor

class AgentExecutor(private val token: String) {

    fun getLLMExecutor(): SingleLLMPromptExecutor {
        val client = SuspendableAPIGatewayClient(
            GrazieEnvironment.Production.url,
            SuspendableHTTPClient.WithV5(
                SuspendableClientWithBackoff(
                    GrazieKtorHTTPClient.Client.Default,
                ), AuthData(
                    token,
                    grazieAgent = GrazieAgent("test", "dev")
                )
            )
        )

        val executor = SingleLLMPromptExecutor(GrazieLLMClient(client))
        return executor
    }
}