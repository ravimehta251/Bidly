package com.bidflare.config;

import org.redisson.api.RedissonClient;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the Redisson client as a bean.
 * RedissonAutoConfiguration from the starter already creates the RedissonClient bean
 * via the redisson.yml config file — this class simply documents that and can be used
 * to add additional Redisson-related beans if needed.
 */
@Configuration
public class RedissonConfig {
    // RedissonClient bean is auto-configured by RedissonAutoConfiguration
    // using src/main/resources/redisson.yml (referenced in application.yml)
}
