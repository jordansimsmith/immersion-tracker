package com.jordansimsmith.immersion.tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
public class ImmersionTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImmersionTrackerApplication.class, args);
    }
}
