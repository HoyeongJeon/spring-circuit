package spring.circuit

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy

@SpringBootApplication
@EnableAspectJAutoProxy
class CircuitApplication

fun main(args: Array<String>) {
	runApplication<CircuitApplication>(*args)
}
