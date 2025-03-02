package spring.circuit

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CircuitApplication

fun main(args: Array<String>) {
	runApplication<CircuitApplication>(*args)
}
