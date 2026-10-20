package com.shofe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShofeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShofeApplication.class, args);
    }
}
