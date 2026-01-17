plugins {
	java
	id("org.springframework.boot") version "3.5.8"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "app.leesh"
version = "0.0.1-SNAPSHOT"
description = "trading log app"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

val mockitoAgent = configurations.create("mockitoAgent")

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	testImplementation("org.mockito:mockito-core")
	mockitoAgent("org.mockito:mockito-core") {
		isTransitive = false
	}
}

tasks.withType<Test> {
	useJUnitPlatform() {
		if (!project.hasProperty("includeExternal")) {
			excludeTags("external")
		}
	}

	jvmArgs("-javaagent:${configurations["mockitoAgent"].asPath}")
}
