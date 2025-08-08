package com.monday.monday_backend.config.repository;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.monday.monday_backend")  // JPA repos live here
//@EnableElasticsearchRepositories(basePackages =  "com.monday.query")
public class RepositoryConfig {
}
