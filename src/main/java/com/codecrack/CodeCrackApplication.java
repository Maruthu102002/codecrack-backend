package com.codecrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class CodeCrackApplication {
    public static void main(String[] args) {
        SpringApplication.run(CodeCrackApplication.class, args);

        System.out.println("\n" +
                "╔═══════════════════════════════════════════════════════╗\n" +
                "║                                                       ║\n" +
                "║   CodeCrack - Online Judge Platform                   ║\n" +
                "║                                                       ║\n" +
                "║   Backend:  http://localhost:8080                     ║\n" +
                "║   Health:   http://localhost:8080/actuator/health     ║\n" +
                "║   H2 DB:    http://localhost:8080/h2-console          ║\n" +
                "║                                                       ║\n" +
                "╚═══════════════════════════════════════════════════════╝\n");
    }
}
