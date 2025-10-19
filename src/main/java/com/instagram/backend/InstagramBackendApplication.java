package com.instagram.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EntityScan("com.instagram.backend.model.entity")
@EnableJpaRepositories("com.instagram.backend.repository.jpa")
@EnableMongoRepositories("com.instagram.backend.repository.mongo")

public class InstagramBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(InstagramBackendApplication.class, args);
    }

}