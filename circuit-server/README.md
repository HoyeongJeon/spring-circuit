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
- 본 브랜치는 AOP 를 사용하여 Circuit Breaker 패턴을 구현한 브랜치이다.

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

**application.yml**
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        registerHealthIndicator: true
        slidingWindowType: COUNT_BASED
        failureRateThreshold: 15 // Circuit Breaker 가 열리는 기준
        minimumNumberOfCalls: 5 // Circuit Breaker 가 열리기 전 최소 호출 횟수
        slidingWindowSize: 50 // Circuit Breaker 가 열리기 전 호출 횟수
        permittedNumberOfCallsInHalfOpenState: 5 
        recordExceptions:
          - org.springframework.web.client.HttpServerErrorException // Circuit Breaker 가 열리는 예외(500 에러)
        ignoreExceptions:
          - org.springframework.web.client.HttpClientErrorException // Circuit Breaker 가 열리지 않는 예외(400 에러)
    instances:
      circuitBreaker:
        baseConfig: default
```

**CircuitBreakerService.kt**
```kotlin
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

```

### failureRateThreshold에 따른 Circuit Breaker 동작
(k6를 사용해 테스트 진행)

**15%**

<img width="241" alt="Image" src="https://github.com/user-attachments/assets/6f6ff876-1a13-4bf8-b7c1-c58c6a1ebb8a" />

<img width="1147" alt="Image" src="https://github.com/user-attachments/assets/d2665fcd-3602-4a06-9dfc-ca4291e57f69" />

(Circuit Breaker 가 열리기 전 50개의 로그를 검사한 결과, 8번의 500 에러가 발생했으므로 실패율은 16%이다.) 임계점을 넘기니 Circuit Breaker가 열리고, 응답이 Circuit Breaker가 열렸다는 메시지로 변경된다.(CallNotPermittedException이 발생)

**20%**

<img width="243" alt="Image" src="https://github.com/user-attachments/assets/adb0ddd4-014f-4d32-9131-cbd84d882d97" />


**25%**

<img width="248" alt="Image" src="https://github.com/user-attachments/assets/419abb12-ff7a-42d3-8a0c-0c1996672e29" />
