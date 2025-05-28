package com.example

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.Executors

data class TicketRequest(val description: String)

@RestController
@RequestMapping("/api/tickets")
class TicketController(private val agentService: AgentServiceAdapter) {

    private val executor = Executors.newSingleThreadExecutor()

    @PostMapping("/dispatch")
    fun dispatch(@RequestBody req: TicketRequest): ResponseEntity<String> {
        val result = runBlocking {
            agentService.dispatchTicket(req.description)
        }
        return ResponseEntity.ok(result)
    }

    @GetMapping("/logs", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getLogs(): SseEmitter {
        val emitter = SseEmitter(Long.MAX_VALUE)

        executor.execute {
            runBlocking {
                try {
                    agentService.agentLogs
                        .onEach { logMessage ->
                            emitter.send(SseEmitter.event().data(logMessage))
                        }
                        .collect()
                } catch (e: Exception) {
                    emitter.completeWithError(e)
                } finally {
                    emitter.complete()
                }
            }
        }

        emitter.onCompletion { executor.shutdown() }
        emitter.onError { executor.shutdown() }

        return emitter
    }
}
