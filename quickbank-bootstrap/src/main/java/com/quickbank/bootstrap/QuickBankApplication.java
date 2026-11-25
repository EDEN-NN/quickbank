package com.quickbank.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.quickbank")
@EntityScan("com.quickbank")
@EnableJpaRepositories("com.quickbank")
@EnableScheduling
public class QuickBankApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuickBankApplication.class, args);
    }
}
