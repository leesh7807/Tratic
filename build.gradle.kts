import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Sync

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
	implementation("org.springframework.boot:spring-boot-starter-validation")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// security
	implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	testImplementation("org.springframework.security:spring-security-test")

	// db
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql") 
	
	// lombok
	compileOnly("org.projectlombok:lombok")
	compileOnly("org.springframework.boot:spring-boot-configuration-processor")
	annotationProcessor("org.projectlombok:lombok")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	
	testCompileOnly("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok")

	testImplementation("org.mockito:mockito-core")
	mockitoAgent("org.mockito:mockito-core") {
		isTransitive = false
	}
}

val frontendDir = layout.projectDirectory.dir("frontend")
val nodeVersionFile = frontendDir.file(".node-version")
val frontendNodeModulesDir = frontendDir.dir("node_modules")
val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
val npmExecutable = if (isWindows) "npm.cmd" else "npm"
val fnmExecutable = if (isWindows) "fnm.exe" else "fnm"

val installFrontend by tasks.registering(Exec::class) {
	group = "frontend"
	description = "Installs frontend dependencies with fnm-managed Node.js."
	workingDir(frontendDir.asFile)

	inputs.files(
		frontendDir.file("package.json"),
		frontendDir.file("package-lock.json"),
		nodeVersionFile
	)
	outputs.dir(frontendNodeModulesDir)

	commandLine(
		fnmExecutable,
		"exec",
		"--using=${nodeVersionFile.asFile.absolutePath}",
		npmExecutable,
		"ci"
	)
}

val frontendDistDir = frontendDir.dir("dist")

val buildFrontend by tasks.registering(Exec::class) {
	group = "frontend"
	description = "Builds the React frontend."
	dependsOn(installFrontend)
	workingDir(frontendDir.asFile)

	inputs.files(
		frontendDir.file("package.json"),
		frontendDir.file("package-lock.json"),
		nodeVersionFile,
		frontendDir.file("index.html"),
		frontendDir.file("vite.config.js")
	)
	inputs.dir(frontendDir.dir("src"))
	outputs.dir(frontendDistDir)

	commandLine(
		fnmExecutable,
		"exec",
		"--using=${nodeVersionFile.asFile.absolutePath}",
		npmExecutable,
		"run",
		"build"
	)
}

val frontendGeneratedResourcesDir = layout.buildDirectory.dir("generated/frontend-resources/main")
val frontendStaticOutputDir = layout.buildDirectory.dir("generated/frontend-resources/main/static")

val syncFrontendAssets by tasks.registering(Sync::class) {
	group = "frontend"
	description = "Copies the frontend build output into generated Spring static resources."
	dependsOn(buildFrontend)
	from(frontendDistDir)
	into(frontendStaticOutputDir)
}

sourceSets.main {
	resources.srcDir(frontendGeneratedResourcesDir)
}

tasks.named("processResources") {
	dependsOn(syncFrontendAssets)
}

tasks.named("clean") {
	doLast {
		delete(frontendDistDir)
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
