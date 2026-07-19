package com.safenet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SafeNetApplication {
    public static void main(String[] args) {
        SpringApplication.run(SafeNetApplication.class, args);
        System.out.println("SafeNet API running on http://localhost:8080");
    }
}
