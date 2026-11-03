package com.labtbd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LabTbdApplication {

    public static void main(String[] args) {
        SpringApplication.run(LabTbdApplication.class, args);
    }
}
