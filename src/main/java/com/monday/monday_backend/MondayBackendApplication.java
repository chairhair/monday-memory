package com.monday.monday_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.monday.monday_backend")
public class MondayBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MondayBackendApplication.class, args);
	}

}
