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

- 브랜치 registry 에서 Registry Pattern을 사용하여 Circuit Breaker 패턴을 구현한 코드를 확인할 수 있다.
- 브랜치 aop 에서 AOP를 사용하여 Circuit Breaker 패턴을 구현한 코드를 확인할 수 있다.
