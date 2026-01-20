plugins {
	java
	id("org.springframework.boot") version "3.3.5"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.google.cloud.tools.jib") version "3.4.3"
}

jib {
	from { image = "eclipse-temurin:21-jre" }
	to {
		image = "ghcr.io/chairhair/monday-memory"
		tags = setOf("latest")
	}
	container {
		ports = listOf("8080")
		jvmFlags = listOf("-Xms256m", "-Xmx512m")
		creationTime = "USE_CURRENT_TIMESTAMP"
	}
}


group = "com.monday"
version = "0.1.4"

java {
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

dependencies {
	implementation("com.monday:shared-monday-memory-be-library:0.1.4")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	//implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
	implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")
	implementation("io.github.resilience4j:resilience4j-micrometer:2.2.0")
	implementation("io.github.resilience4j:resilience4j-retry:2.2.0")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("io.jsonwebtoken:jjwt-api:0.11.5")
	implementation("io.jsonwebtoken:jjwt-impl:0.11.5")
	implementation("io.jsonwebtoken:jjwt-jackson:0.11.5")
	implementation("me.paulschwarz:spring-dotenv:4.0.0")
	implementation("com.stripe:stripe-java:30.0.0")
	implementation("com.openai:openai-java-spring-boot-starter:4.8.0")
	implementation("com.knuddels:jtokkit:1.1.0")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("com.h2database:h2")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
	testImplementation("org.jetbrains.kotlin:kotlin-reflect:1.9.+")
	testImplementation("com.nimbusds:nimbus-jose-jwt:9.37")

}

tasks.withType<Test> {
	useJUnitPlatform()
}