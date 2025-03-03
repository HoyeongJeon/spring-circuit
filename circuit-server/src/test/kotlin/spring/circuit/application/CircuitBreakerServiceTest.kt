package spring.circuit.application

import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test

@SpringBootTest
class CircuitBreakerServiceTest {
    @Autowired
    private lateinit var circuitBreakerService: CircuitBreakerService

    @Test
    @DisplayName("서킷브레이커 테스트")
    fun `1000번 호출해서 카운트`() {
        val result = (1..1000).map {
            circuitBreakerService.call().block()
        }
        println("성공 횟수: ${circuitBreakerService.successCount.get()}")
        println("4xx대 에러 횟수: ${circuitBreakerService.fourxxCount.get()}")
        println("5xx대 에러 횟수: ${circuitBreakerService.fivexxCount.get()}")
        println("callNotPermittedException 횟수: ${circuitBreakerService.callNotPermittedCount.get()}")
    }

}

