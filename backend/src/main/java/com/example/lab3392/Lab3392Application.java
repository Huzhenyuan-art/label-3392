package com.example.lab3392;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableCaching
@SpringBootApplication
public class Lab3392Application {
    public static void main(String[] args) {
        SpringApplication.run(Lab3392Application.class, args);
    }
}

