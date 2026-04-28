package com.crust.menu

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class MenuGraphApplication

fun main(args: Array<String>) {
	runApplication<MenuGraphApplication>(*args)
}
