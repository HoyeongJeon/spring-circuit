# MSA 환경에서 장애 전파를 막기 위한 Circuit Breaker 패턴
- MSA 환경에서 장애 전파를 막기 위한 방법으로 크게 다음 3가지가 존재한다.
  - Circuit Breaker
  - Tracing
  - RateLimit
- 이번 실습은 위 방법 중 Circuit Breaker 패턴을 사용하여 장애 전파를 막는 방법에 대해 알아보도록 한다.
## 1. Circuit Breaker 패턴이란?
- Circuit Breaker 패턴은 장애 전파를 막기 위한 패턴 중 하나로, 장애가 발생한 서비스에 대한 요청을 일정 시간 동안 차단하는 방식을 말한다.
- Circuit Breaker 패턴은 다음과 같은 3가지 상태를 가진다.
  - Closed: 서비스가 정상적으로 동작하는 상태
  - Open: Circuit Breaker가 열린 상태로, 서비스가 장애 상태인 경우. 이 상태에서는 요청을 차단한다.
  - Half-Open: Circuit Breaker가 열린 상태에서 일정 시간이 지나면 Half-Open 상태로 전환된다. 이 상태에서는 일부 요청을 허용하여 서비스가 정상적으로 동작하는지 확인한다.

## 2. Circuit Breaker 패턴 구현하기
- Circuit Breaker 패턴을 구현하기 위해 다음과 같은 라이브러리를 사용했다.
  - Spring Cloud Circuit Breaker
  - Resilience4j
- Circuit Breaker 패턴을 구현하는 방법으로 AOP, Registry Pattern, Proxy Pattern 등이 있다. 이번 실습에서는 AOP와 Registry Pattern을 사용하여 Circuit Breaker 패턴을 구현해보도록 한다.
- 본 브랜치는 Registry Pattern을 사용하여 Circuit Breaker 패턴을 구현한 브랜치이다.

## 3. 실습
### 불안정한 서버 가정
`error-server 참조`
```javascript
     if (random < 0.2) {
        // 20% 확률로 500 에러
        res.writeHead(500, { "Content-Type": "text/plain" });
        return res.end("Internal Server Error");
      } else if (random < 0.3) {
        // 10% 확률로 400 에러
        res.writeHead(400, { "Content-Type": "text/plain" });
        return res.end("Bad Request");
      }
```
### Circuit Breaker 패턴 구현
`circuit-server 참조`

**CircuitBreakerConfiguration.kt**
```kotlin
@Configuration
class CircuitBreakerConfiguration() {

    private fun circuitBreakerConfig(): CircuitBreakerConfig {
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(15.0f) // circuit 작동 임계값 : 15%, 20%, 25%
            .minimumNumberOfCalls(10) // circuit 작동 최소 호출 횟수
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED) // circuit 작동 윈도우 타입
            .slidingWindowSize(1000) // circuit 작동 윈도우 크기
            .recordExceptions(HttpServerErrorException::class.java) // 500 에러는 Circuit Breaker로 처리
            .ignoreExceptions(
                HttpClientErrorException::class.java, // 400 에러는 Circuit Breaker로 처리하지 않음
            )
            .build()
    }

    @Bean
    fun circuitBreakerRegistry(): CircuitBreakerRegistry {
        return CircuitBreakerRegistry.of(circuitBreakerConfig())
    }
}

```

**CircuitBreakerService.kt**
```kotlin
@Service
class CircuitBreakerService(
    private val webClient: WebClient,
    private val circuitBreakerRegistry: CircuitBreakerRegistry
) {
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
                Mono.empty()
            }
            .onStatus({ it.is4xxClientError }) { response ->
                fourxxCount.incrementAndGet()
                Mono.error(HttpClientErrorException(response.statusCode()))
            }
            .onStatus({ it.is5xxServerError }) { response ->
                fivexxCount.incrementAndGet()
                Mono.error(HttpServerErrorException(response.statusCode()))
            }
            .bodyToMono(String::class.java)
            .transform(CircuitBreakerOperator.of(circuitBreaker))
            .onErrorResume(this::fallback)
    }

    fun fallback(e: Throwable): Mono<String> {
        return when (e) {
            is CallNotPermittedException -> {
                callNotPermittedCount.incrementAndGet()
                Mono.just("CircuitBreaker가 열렸습니다.")
            }

            is HttpServerErrorException -> Mono.just("5xx 에러")
            is HttpClientErrorException -> Mono.just("4xx 에러")
            else -> Mono.just("기타 에러")
        }
    }
}
```

### failureRateThreshold에 따른 Circuit Breaker 동작
(k6를 사용해 테스트 진행)

**15%**

![Image](https://github.com/user-attachments/assets/f497cef8-3ded-40ba-865a-cdfb69173a23)

![Image](https://github.com/user-attachments/assets/fdd36612-5e44-41c0-9798-282f84fd13af)

(Circuit Breaker 가 열리기 전 52개의 로그를 검사한 결과, 8번의 500 에러가 발생했으므로 실패율은 15.38%이다.)
임계점을 넘기니 Circuit Breaker가 열리고, 응답이 Circuit Breaker가 열렸다는 메시지로 변경된다.(CallNotPermittedException이 발생)


**20%**

![Image](https://github.com/user-attachments/assets/85a513bb-4f2a-4ae9-87fe-9f4cfc69932b)

실패율이 20%를 넘기지 않아 Circuit Breaker가 열리지 않았다.


**25%**

![Image](https://github.com/user-attachments/assets/51aa1028-09b9-4a67-9c41-234077d483d6)

실패율이 25%를 넘겨 Circuit Breaker가 열렸다.
