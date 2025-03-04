package spring.circuit.application

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
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
) {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
    var successCount = AtomicInteger(0)
    var fourxxCount = AtomicInteger(0)
    var fivexxCount = AtomicInteger(0)
    var callNotPermittedCount = AtomicInteger(0)


    @CircuitBreaker(name = "circuitBreaker", fallbackMethod = "fallback")
    fun call(): Mono<String> {
        return webClient.get()
            .uri("/api/random-error")
            .retrieve()
            .onStatus({ it.is2xxSuccessful }) {
                successCount.incrementAndGet()
                logger.info("정상 응답")
                Mono.empty()
            }
            .onStatus({ it.is4xxClientError }) { response ->
                Mono.error(HttpClientErrorException(response.statusCode()))
            }
            .onStatus({ it.is5xxServerError }) { response ->
                Mono.error(HttpServerErrorException(response.statusCode()))
            }
            .bodyToMono(String::class.java)
            .defaultIfEmpty("Request가 정상적으로 처리되었습니다.")
    }

    fun fallback(e: Throwable): Mono<String> {
        return when (e) {
            is CallNotPermittedException -> {
                callNotPermittedCount.incrementAndGet()
                logger.info("CircuitBreaker가 열렸습니다.")
                Mono.just("CircuitBreaker가 열렸습니다.")
            }
            is HttpClientErrorException -> {
                fourxxCount.incrementAndGet()
                logger.warn("4xx 에러")
                Mono.just("4xx 에러")
            }
            is HttpServerErrorException -> {
                fivexxCount.incrementAndGet()
                logger.error("5xx 에러")
                Mono.just("5xx 에러")
            }
            else -> Mono.just("기타 에러")
        }
    }
}
