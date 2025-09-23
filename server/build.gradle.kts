plugins {
	application
	kotlin("jvm")
	id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
	implementation("io.ktor:ktor-server-core-jvm:2.3.12")
	implementation("io.ktor:ktor-server-netty-jvm:2.3.12")
	implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.12")
	implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.12")
	implementation("io.ktor:ktor-server-call-logging-jvm:2.3.12")
	implementation("io.ktor:ktor-server-cors-jvm:2.3.12")
	implementation("io.ktor:ktor-server-auth-jvm:2.3.12")
	implementation("io.ktor:ktor-server-auth-jwt-jvm:2.3.12")
	implementation("io.ktor:ktor-server-status-pages-jvm:2.3.12")
	implementation("com.auth0:java-jwt:4.4.0")
	implementation("org.jetbrains.exposed:exposed-core:0.52.0")
	implementation("org.jetbrains.exposed:exposed-dao:0.52.0")
	implementation("org.jetbrains.exposed:exposed-jdbc:0.52.0")
	implementation("org.jetbrains.exposed:exposed-java-time:0.52.0")
	implementation("org.postgresql:postgresql:42.7.3")
	implementation("com.h2database:h2:2.2.224")
	implementation("org.mindrot:jbcrypt:0.4")
	implementation("ch.qos.logback:logback-classic:1.5.6")
	testImplementation(kotlin("test"))
	
	// HTTP клиент для работы с 1000 Genomes
	implementation("io.ktor:ktor-client-cio:2.3.12")
	implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
	implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
	
	// Для работы с CSV/TSV файлами
	implementation("com.opencsv:opencsv:5.9")
	
	// Для работы с VCF файлами (упрощенная версия без htsjdk)
	// implementation("com.github.samtools:htsjdk:3.0.10")
}

application { mainClass.set("com.origin.server.ApplicationKt") }

kotlin { jvmToolchain(17) }

tasks.register<Jar>("fatJar") {
	archiveClassifier.set("all")
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	manifest { attributes["Main-Class"] = "com.origin.server.ApplicationKt" }
	from(sourceSets.main.get().output)
	dependsOn(configurations.runtimeClasspath)
	from({ configurations.runtimeClasspath.get().filter { it.name.endsWith(".jar") }.map { zipTree(it) } })
}