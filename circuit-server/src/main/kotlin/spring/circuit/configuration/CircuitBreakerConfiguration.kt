package spring.circuit.configuration

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException

@Configuration
class CircuitBreakerConfiguration() {

    private fun circuitBreakerConfig(): CircuitBreakerConfig {
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(15.0f) // circuit 작동 임계값 : 15%, 20%, 25%
            .minimumNumberOfCalls(5)
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(50)
            .recordExceptions(HttpServerErrorException::class.java)
            .ignoreExceptions(
                HttpClientErrorException::class.java,
            )
            .build()
    }

    @Bean
    fun circuitBreakerRegistry(): CircuitBreakerRegistry {
        return CircuitBreakerRegistry.of(circuitBreakerConfig()).also {
            it.circuitBreaker("circuit-breaker").reset()
        }
    }
}
