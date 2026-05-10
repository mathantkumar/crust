plugins {
	kotlin("jvm") version "2.1.0"
	kotlin("plugin.spring") version "2.1.0"
	id("org.springframework.boot") version "3.4.0"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.1.0"
}

group = "com.crust"
version = "0.0.1-SNAPSHOT"
description = "Menu Graph"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.flywaydb:flyway-core")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	runtimeOnly("org.postgresql:postgresql")
	implementation("com.netflix.graphql.dgs:graphql-dgs-spring-graphql-starter:9.0.4")
	implementation("com.graphql-java:graphql-java-extended-scalars:21.0")
	implementation("org.springframework.kafka:spring-kafka")
	implementation("dev.langchain4j:langchain4j-spring-boot-starter:0.35.0")
	implementation("dev.langchain4j:langchain4j-google-ai-gemini:0.35.0")
	implementation("org.springframework.retry:spring-retry:2.0.11")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-web-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
