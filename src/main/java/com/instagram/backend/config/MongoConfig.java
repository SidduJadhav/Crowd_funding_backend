package com.instagram.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.instagram.backend.repository.mongo")
@EnableMongoAuditing
public class MongoConfig {
    // MongoDB configuration is in application.properties
    // This class just enables MongoDB repositories
}