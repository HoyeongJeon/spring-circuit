package spring.circuit.presentation

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import spring.circuit.application.CircuitBreakerService

@RestController
@RequestMapping("/api")
class CircuitController(
    private val circuitBreakerService: CircuitBreakerService
) {

    @GetMapping("/call")
    fun triggerCircuitBreaker(): Mono<String> {
        return circuitBreakerService.call()
            .onErrorResume { Mono.just("Fallback triggered: ${it.message}") }
    }

    @GetMapping("/stats")
    fun getCircuitStats(): Map<String, Int> {
        return mapOf(
            "성공" to circuitBreakerService.successCount.get(),
            "4xx 에러" to circuitBreakerService.fourxxCount.get(),
            "5xx 에러" to circuitBreakerService.fivexxCount.get(),
            "circuit breaker가 열림" to circuitBreakerService.callNotPermittedCount.get()
        )
    }
}
