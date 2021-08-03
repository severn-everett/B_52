package com.severett.b52

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
open class B52

fun main(args: Array<String>) {
    SpringApplication.run(B52::class.java, *args)
}
