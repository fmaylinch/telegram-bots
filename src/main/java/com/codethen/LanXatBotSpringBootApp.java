package com.codethen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;

/**
 * Exclude is to prevent SpringBoot from trying to connect to localhost.
 * https://stackoverflow.com/questions/68121079/spring-boot-mongo#comment133330046_68121079
 */
@SpringBootApplication(exclude=MongoReactiveAutoConfiguration.class)
public class LanXatBotSpringBootApp {

    public static void main(String... args) {
        SpringApplication.run(LanXatBotSpringBootApp.class, args);
    }
}
