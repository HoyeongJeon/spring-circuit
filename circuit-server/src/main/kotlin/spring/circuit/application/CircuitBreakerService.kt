package spring.circuit.application

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger

@Service
class CircuitBreakerService(
    private val webClient: WebClient,
    private val circuitBreakerRegistry: CircuitBreakerRegistry
) {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    var successCount = AtomicInteger(0)
    var fourxxCount = AtomicInteger(0)
    var fivexxCount = AtomicInteger(0)
    var callNotPermittedCount = AtomicInteger(0)

    fun call(): Mono<String> {
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker("circuit-breaker")
        return webClient.get()
            .uri("/api/random-error")
            .retrieve()
            .onStatus({ it.is2xxSuccessful }) {
                successCount.incrementAndGet()
                logger.info("정상 응답")
                Mono.empty()
            }
            .onStatus({ it.is4xxClientError }) { response ->
                fourxxCount.incrementAndGet()
                logger.warn("4xx 에러")
                Mono.error(HttpClientErrorException(response.statusCode()))
            }
            .onStatus({ it.is5xxServerError }) { response ->
                fivexxCount.incrementAndGet()
                logger.error("5xx 에러")
                Mono.error(HttpServerErrorException(response.statusCode()))
            }
            .bodyToMono(String::class.java)
            .defaultIfEmpty("Request가 정상적으로 처리되었습니다.")
            .transform(CircuitBreakerOperator.of(circuitBreaker))
            .onErrorResume(this::fallback)
    }

    fun fallback(e: Throwable): Mono<String> {
        return when (e) {
            is CallNotPermittedException -> {
                callNotPermittedCount.incrementAndGet()
                logger.info("CircuitBreaker가 열렸습니다.")
                Mono.just("CircuitBreaker가 열렸습니다.")
            }

            is HttpServerErrorException -> Mono.just("5xx 에러")
            is HttpClientErrorException -> Mono.just("4xx 에러")
            else -> Mono.just("기타 에러")
        }
    }
}
