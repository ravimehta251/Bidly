package com.bidflare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BidFlareApplication {

    public static void main(String[] args) {
        SpringApplication.run(BidFlareApplication.class, args);
    }
}
