package com.example

import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class TicketRequest(val description: String)

@RestController
@RequestMapping("/api/tickets")
class TicketController(private val agentService: AgentServiceAdapter) {

    @PostMapping("/dispatch")
    fun dispatch(@RequestBody req: TicketRequest): ResponseEntity<String> {
        val result = runBlocking {
            agentService.dispatchTicket(req.description)
        }
        return ResponseEntity.ok(result)
    }
}